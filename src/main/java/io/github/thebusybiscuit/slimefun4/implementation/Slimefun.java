package io.github.thebusybiscuit.slimefun4.implementation;

import com.google.inject.Inject;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.exceptions.TagMisconfigurationException;
import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.gps.GPSNetwork;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.SlimefunRegistry;
import io.github.thebusybiscuit.slimefun4.core.commands.SlimefunCommand;
import io.github.thebusybiscuit.slimefun4.core.networks.NetworkManager;
import io.github.thebusybiscuit.slimefun4.core.services.*;
import io.github.thebusybiscuit.slimefun4.core.services.github.GitHubService;
import io.github.thebusybiscuit.slimefun4.core.services.holograms.HologramsService;
import io.github.thebusybiscuit.slimefun4.core.services.profiler.SlimefunProfiler;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundService;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientAltar;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientPedestal;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.Cooler;
import io.github.thebusybiscuit.slimefun4.implementation.items.magical.BeeWings;
import io.github.thebusybiscuit.slimefun4.implementation.items.tools.GrapplingHook;
import io.github.thebusybiscuit.slimefun4.implementation.items.weapons.SeismicAxe;
import io.github.thebusybiscuit.slimefun4.implementation.listeners.*;
import io.github.thebusybiscuit.slimefun4.implementation.listeners.crafting.*;
import io.github.thebusybiscuit.slimefun4.implementation.listeners.entity.*;
import io.github.thebusybiscuit.slimefun4.implementation.resources.GEOResourcesSetup;
import io.github.thebusybiscuit.slimefun4.implementation.setup.PostSetup;
import io.github.thebusybiscuit.slimefun4.implementation.setup.ResearchSetup;
import io.github.thebusybiscuit.slimefun4.implementation.setup.SlimefunItemSetup;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.SlimefunStartupTask;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.TickerTask;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.armor.RadiationTask;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.armor.RainbowArmorTask;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.armor.SlimefunArmorTask;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.armor.SolarHelmetTask;
import io.github.thebusybiscuit.slimefun4.integrations.IntegrationsManager;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;
import io.github.thebusybiscuit.slimefun4.utils.tags.SlimefunTag;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.MenuListener;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.UniversalBlockMenu;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.mose.remakes.Config;
import org.spongepowered.api.MinecraftVersion;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.item.recipe.Recipe;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is the main class of Slimefun.
 * This is where all the magic starts, take a look around.
 *
 * @author TheBusyBiscuit
 */
@Plugin("slimfun")
public final class Slimefun implements SlimefunAddon {

    /**
     * This is the Java version we recommend server owners to use.
     * This does not necessarily mean that it's the minimum version
     * required to run Slimefun.
     */
    private static final int RECOMMENDED_JAVA_VERSION = 17;

    /**
     * Our static instance of {@link Slimefun}.
     * Make sure to clean this up in {@link #onDisable(StoppingEngineEvent)}!
     */
    private static Slimefun instance;
    // Various things we need
    private final SlimefunRegistry registry = new SlimefunRegistry();
    private final SlimefunCommand command = new SlimefunCommand(this);
    private final TickerTask ticker = new TickerTask();
    // Services - Systems that fulfill certain tasks, treat them as a black box
    private final CustomItemDataService itemDataService = new CustomItemDataService(this, "slimefun_item");
    private final BlockDataService blockDataService = new BlockDataService(this, "slimefun_block");
    private final CustomTextureService textureService = new CustomTextureService(new Config(this, "item-models.yml"));
    private final GitHubService gitHubService = new GitHubService("Slimefun/Slimefun4");
    private final UpdaterService updaterService;
    private final MetricsService metricsService = new MetricsService(this);
    private final AutoSavingService autoSavingService = new AutoSavingService();
    private final BackupService backupService = new BackupService();
    private final PermissionsService permissionsService = new PermissionsService(this);
    private final PerWorldSettingsService worldSettingsService = new PerWorldSettingsService(this);
    private final MinecraftRecipeService recipeService = new MinecraftRecipeService(this);
    private final HologramsService hologramsService = new HologramsService(this);
    private final SoundService soundService = new SoundService(this);
    // Some other things we need
    private final IntegrationsManager integrations = new IntegrationsManager(this);
    private final SlimefunProfiler profiler = new SlimefunProfiler();
    private final GPSNetwork gpsNetwork = new GPSNetwork(this);
    // Listeners that need to be accessed elsewhere
    private final GrapplingHookListener grapplingHookListener = new GrapplingHookListener();
    private final BackpackListener backpackListener = new BackpackListener();
    private final SlimefunBowListener bowListener = new SlimefunBowListener();
    private final PluginContainer pluginContainer;
    // Important config files for Slimefun
    private final Config config;
    private final Config items;
    private final Config researches;
    private final Logger logger;
    /**
     * Keep track of which {@link MinecraftVersion} we are on.
     */
    private MinecraftVersion minecraftVersion;
    /**
     * Keep track of whether this is a fresh install or a regular boot up.
     */
    private boolean isNewlyInstalled = false;
    // Even more things we need
    private NetworkManager networkManager;
    private LocalizationService local;
    private Server server;

    /**
     * Our default constructor for {@link Slimefun}.
     */
    @Inject
    public Slimefun(PluginContainer container, Logger logger) {
        this.pluginContainer = container;
        this.logger = logger;

        this.config = new Config(pluginContainer);
        this.items = new Config(pluginContainer, "Items.yml");
        this.researches = new Config(pluginContainer, "Researches.yml");
        this.updaterService = new UpdaterService(this, pluginContainer.metadata().version(), config));
    }

    /**
     * This is a private internal method to set the de-facto instance of {@link Slimefun}.
     * Having this as a seperate method ensures the seperation between static and non-static fields.
     * It also makes sonarcloud happy :)
     *
     * @param pluginInstance Our instance of {@link Slimefun} or null
     */
    private static void setInstance(@Nullable Slimefun pluginInstance) {
        instance = pluginInstance;
    }

    /**
     * This returns the global instance of {@link Slimefun}.
     * This may return null if the {@link Plugin} was disabled.
     *
     * @return The {@link Slimefun} instance
     */
    public static @Nullable Slimefun instance() {
        return instance;
    }

    private static void validateInstance() {
        if (instance == null) {
            throw new IllegalStateException("Cannot invoke static method, Slimefun instance is null.");
        }
    }

    /**
     * This returns the {@link Logger} instance that Slimefun uses.
     * <p>
     * <strong>Any {@link SlimefunAddon} should use their own {@link Logger} instance!</strong>
     *
     * @return Our {@link Logger} instance
     */
    public static @Nonnull Logger logger() {
        validateInstance();
        return instance.logger;
    }

    /**
     * This returns the version of Slimefun that is currently installed.
     *
     * @return The currently installed version of Slimefun
     */
    public static @Nonnull String getVersion() {
        validateInstance();
        return instance.pluginContainer.metadata().version().toString();
    }

    public static @Nonnull Config getCfg() {
        validateInstance();
        return instance.config;
    }

    public static @Nonnull Config getResearchCfg() {
        validateInstance();
        return instance.researches;
    }

    public static @Nonnull Config getItemCfg() {
        validateInstance();
        return instance.items;
    }

    /**
     * This returns our {@link GPSNetwork} instance.
     * The {@link GPSNetwork} is responsible for handling any GPS-related
     * operations and for managing any {@link GEOResource}.
     *
     * @return Our {@link GPSNetwork} instance
     */
    public static @Nonnull GPSNetwork getGPSNetwork() {
        validateInstance();
        return instance.gpsNetwork;
    }

    public static @Nonnull TickerTask getTickerTask() {
        validateInstance();
        return instance.ticker;
    }

    /**
     * This returns the {@link LocalizationService} of Slimefun.
     *
     * @return The {@link LocalizationService} of Slimefun
     */
    public static @Nonnull LocalizationService getLocalization() {
        validateInstance();
        return instance.local;
    }

    /**
     * This method returns out {@link MinecraftRecipeService} for Slimefun.
     * This service is responsible for finding/identifying {@link Recipe Recipes}
     * from vanilla Minecraft.
     *
     * @return Slimefun's {@link MinecraftRecipeService} instance
     */
    public static @Nonnull MinecraftRecipeService getMinecraftRecipeService() {
        validateInstance();
        return instance.recipeService;
    }

    public static @Nonnull CustomItemDataService getItemDataService() {
        validateInstance();
        return instance.itemDataService;
    }

    public static @Nonnull CustomTextureService getItemTextureService() {
        validateInstance();
        return instance.textureService;
    }

    public static @Nonnull PermissionsService getPermissionsService() {
        validateInstance();
        return instance.permissionsService;
    }

    public static @Nonnull BlockDataService getBlockDataService() {
        validateInstance();
        return instance.blockDataService;
    }

    /**
     * This method returns out world settings service.
     * That service is responsible for managing item settings per
     * {@link ServerWorld}, such as disabling a {@link SlimefunItem} in a
     * specific {@link ServerWorld}.
     *
     * @return Our instance of {@link PerWorldSettingsService}
     */
    public static @Nonnull PerWorldSettingsService getWorldSettingsService() {
        validateInstance();
        return instance.worldSettingsService;
    }

    /**
     * This returns our {@link HologramsService} which handles the creation and
     * cleanup of any holograms.
     *
     * @return Our instance of {@link HologramsService}
     */
    public static @Nonnull HologramsService getHologramsService() {
        validateInstance();
        return instance.hologramsService;
    }

    /**
     * This returns our {@link  SoundService} which handles the configuration of all sounds used in Slimefun
     *
     * @return Our instance of {@link SoundService}
     */
    @Nonnull
    public static SoundService getSoundService() {
        validateInstance();
        return instance.soundService;
    }

    /**
     * This returns our instance of {@link IntegrationsManager}.
     * This is responsible for managing any integrations with third party {@link Plugin plugins}.
     *
     * @return Our instance of {@link IntegrationsManager}
     */
    public static @Nonnull IntegrationsManager getIntegrations() {
        validateInstance();
        return instance.integrations;
    }

    /**
     * This returns out instance of the {@link ProtectionManager}.
     * This bridge is used to hook into any third-party protection {@link Plugin}.
     *
     * @return Our instanceof of the {@link ProtectionManager}
     */
    /*public static @Nonnull ProtectionManager getProtectionManager() {
        return getIntegrations().getProtectionManager();
    }*/
    //No general protection manager (although, could make one, could be useful)

    /**
     * This method returns the {@link UpdaterService} of Slimefun.
     * It is used to handle automatic updates.
     *
     * @return The {@link UpdaterService} for Slimefun
     */
    public static @Nonnull UpdaterService getUpdater() {
        validateInstance();
        return instance.updaterService;
    }

    /**
     * This method returns the {@link MetricsService} of Slimefun.
     * It is used to handle sending metric information to bStats.
     *
     * @return The {@link MetricsService} for Slimefun
     */
    public static @Nonnull MetricsService getMetricsService() {
        validateInstance();
        return instance.metricsService;
    }

    /**
     * This method returns the {@link GitHubService} of Slimefun.
     * It is used to retrieve data from GitHub repositories.
     *
     * @return The {@link GitHubService} for Slimefun
     */
    public static @Nonnull GitHubService getGitHubService() {
        validateInstance();
        return instance.gitHubService;
    }

    /**
     * This returns our {@link NetworkManager} which is responsible
     * for handling the Cargo and Energy networks.
     *
     * @return Our {@link NetworkManager} instance
     */

    public static @Nonnull NetworkManager getNetworkManager() {
        validateInstance();
        return instance.networkManager;
    }

    public static @Nonnull SlimefunRegistry getRegistry() {
        validateInstance();
        return instance.registry;
    }

    public static @Nonnull GrapplingHookListener getGrapplingHookListener() {
        validateInstance();
        return instance.grapplingHookListener;
    }

    public static @Nonnull BackpackListener getBackpackListener() {
        validateInstance();
        return instance.backpackListener;
    }

    public static @Nonnull SlimefunBowListener getBowListener() {
        validateInstance();
        return instance.bowListener;
    }

    /**
     * The {@link Command} that was added by Slimefun.
     *
     * @return Slimefun's command
     */

    //I can see this being a problem
    public static @Nonnull SlimefunCommand getCommand() {
        validateInstance();
        return instance.command;
    }

    /**
     * This returns our instance of the {@link SlimefunProfiler}, a tool that is used
     * to analyse performance and lag.
     *
     * @return The {@link SlimefunProfiler}
     */
    public static @Nonnull SlimefunProfiler getProfiler() {
        validateInstance();
        return instance.profiler;
    }

    /**
     * This returns the currently installed version of Minecraft.
     *
     * @return The current version of Minecraft
     */
    public static @Nullable MinecraftVersion getMinecraftVersion() {
        validateInstance();
        return instance.minecraftVersion;
    }

    /**
     * This method returns whether this version of Slimefun was newly installed.
     * It will return true if this {@link Server} uses Slimefun for the very first time.
     *
     * @return Whether this is a new installation of Slimefun
     */
    public static boolean isNewlyInstalled() {
        validateInstance();
        return instance.isNewlyInstalled;
    }

    /**
     * This method returns a {@link Set} of every {@link Plugin} that lists Slimefun
     * as a required or optional dependency.
     * <p>
     * We will just assume this to be a list of our addons.
     *
     * @return A {@link Set} of every {@link Plugin} that is dependent on Slimefun
     */
    public static @Nonnull Set<PluginContainer> getInstalledAddons() {
        validateInstance();
        String pluginName = instance.getName();

        // @formatter:off - Collect any Plugin that (soft)-depends on Slimefun
        return Sponge
                .pluginManager()
                .plugins()
                .stream()
                .filter(plugin -> plugin
                        .metadata()
                        .dependencies()
                        .stream()
                        .anyMatch(depend -> depend
                                .id()
                                .equalsIgnoreCase(pluginName)))
                .collect(Collectors.toSet());
        // @formatter:on
    }

    /**
     * This method schedules a delayed synchronous task for Slimefun.
     * <strong>For Slimefun only, not for addons.</strong>
     * <p>
     * This method should only be invoked by Slimefun itself.
     * Addons must schedule their own tasks using their own {@link Plugin} instance.
     *
     * @param runnable The {@link Runnable} to run
     * @param delay    The delay for this task
     * @return The resulting {@link ScheduledTask} or null if Slimefun was disabled
     */
    public static @Nullable ScheduledTask runSync(@Nonnull Runnable runnable, long delay) {
        Validate.notNull(runnable, "Cannot run null");
        Validate.isTrue(delay >= 0, "The delay cannot be negative");

        // Run the task instantly within a Unit Test
        if (getMinecraftVersion() == null) {
            runnable.run();
            return null;
        }

        if (instance == null) {
            return null;
        }

        Task task = Task.builder().delay(Ticks.of(delay)).execute(runnable).plugin(instance.pluginContainer).build();
        return instance.getServer().scheduler().submit(task);
    }

    /**
     * This method schedules a synchronous task for Slimefun.
     * <strong>For Slimefun only, not for addons.</strong>
     * <p>
     * This method should only be invoked by Slimefun itself.
     * Addons must schedule their own tasks using their own {@link Plugin} instance.
     *
     * @param runnable The {@link Runnable} to run
     * @return The resulting {@link ScheduledTask} or null if Slimefun was disabled
     */
    public static void runSync(@Nonnull Runnable runnable) {
        Validate.notNull(runnable, "Cannot run null");

        // Run the task instantly within a Unit Test
        if (getMinecraftVersion() == null) {
            runnable.run();
            return;
        }

        if (instance == null) {
            return;
        }

        instance.server.scheduler().executor(instance.pluginContainer)
                .execute(runnable);
    }

    public Server getServer() {
        return this.server;
    }

    @Listener
    public void onStartingServer(StartingEngineEvent<Server> event) {
        this.server = event.engine();
        setInstance(this);

        if (isUnitTest()) {
            // We handle Unit Tests seperately.
            onUnitTestStart();
        } else {
            // The Environment has been validated.
            onPluginStart();
        }
    }

    @Listener
    public void onStartedServer(StartedEngineEvent<Server> event) {
        textureService.register(registry.getAllSlimefunItems(), true);
        permissionsService.register(registry.getAllSlimefunItems(), true);
        soundService.reload(true);

        // This try/catch should prevent buggy Spigot builds from blocking item loading
        try {
            recipeService.refresh();
        } catch (Exception | LinkageError x) {
            logger.error("An Exception occurred while iterating through the Recipe list on Minecraft Version " + minecraftVersion.name() + " (Slimefun v" + getVersion() + ")");
        }
    }

    /**
     * This is our start method for a Unit Test environment.
     */
    private void onUnitTestStart() {
        local = new LocalizationService(this, "", null);
        networkManager = new NetworkManager(200);
        command.register();
        registry.load(this, config);
        loadTags();
        soundService.reload(false);
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    /**
     * This is our start method for a correct Slimefun installation.
     */
    private void onPluginStart() {
        long timestamp = System.nanoTime();
        Logger logger = getLogger();

        // Encourage newer Java version
        if (NumberUtils.getJavaVersion() < RECOMMENDED_JAVA_VERSION) {
            StartupWarnings.oldJavaVersion(logger, RECOMMENDED_JAVA_VERSION);
        }

        // If the server has no "data-storage" folder, it's _probably_ a new install. So mark it for metrics.
        isNewlyInstalled = !new File("data-storage/Slimefun").exists();

        // Creating all necessary Folders
        logger.info("Creating directories...");
        createDirectories();

        // Load various config settings into our cache
        registry.load(this, config);

        // Set up localization
        logger.info("Loading language files...");
        String chatPrefix = config.getString("options", "chat-prefix");
        String serverDefaultLanguage = config.getString("options", "language");
        local = new LocalizationService(this, chatPrefix, serverDefaultLanguage);

        int networkSize = config.getInt("networks", "max-size");

        // Make sure that the network size is a valid input
        if (networkSize < 1) {
            logger.warn("Your 'networks.max-size' setting is misconfigured! It must be at least 1, it was set to: {0}", networkSize);
            networkSize = 1;
        }

        networkManager = new NetworkManager(networkSize, config.getBoolean("networks", "enable-visualizer"), config.getBoolean("networks", "delete-excess-items"));

        // Setting up bStats
        //new Thread(metricsService::start, "Slimefun Metrics").start(); <--- no .... bad

        // Starting the Auto-Updater
        if (config.getBoolean("options", "auto-update")) {
            logger.info("Starting Auto-Updater...");
            updaterService.start();
        } else {
            updaterService.disable();
        }

        // Registering all GEO Resources
        logger.info("Loading GEO-Resources...");
        GEOResourcesSetup.setup();

        logger.info("Loading Tags...");
        loadTags();

        logger.info("Loading items...");
        loadItems();

        logger.info("Loading researches...");
        loadResearches();

        registry.setResearchingEnabled(getResearchCfg().getBoolean("enable-researching"));
        PostSetup.setupWiki();

        logger.info("Registering listeners...");
        registerListeners();

        // Initiating various Stuff and all items with a slight delay (0ms after the Server finished loading)
        runSync(new SlimefunStartupTask(this, () -> {


        }), 0);

        // Setting up our commands
        try {
            command.register();
        } catch (Exception | LinkageError x) {
            logger.error("An Exception occurred while registering the /slimefun command", x);
        }

        // Armor Update Task
        if (config.getBoolean("options", "enable-armor-effects")) {
            new SlimefunArmorTask().schedule(this, config.getInt("options.armor-update-interval") * 20L);
            new RadiationTask().schedule(this, config.getInt("options.radiation-update-interval") * 20L);
            new RainbowArmorTask().schedule(this, config.getInt("options.rainbow-armor-update-interval") * 20L);
            new SolarHelmetTask().schedule(this, config.getInt("options.armor-update-interval"));
        }

        // Starting our tasks
        autoSavingService.start(this, config.getInt("options.auto-save-delay-in-minutes"));
        hologramsService.start();
        ticker.start(this);

        // Loading integrations
        logger.info("Loading Third-Party plugin integrations...");
        integrations.start();
        gitHubService.start(this);

        // Hooray!
        logger.info("Slimefun has finished loading in {0}", getStartupTime(timestamp));
    }

    @NotNull
    @Override
    public PluginContainer getContainer() {
        return pluginContainer;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/Slimefun/Slimefun4/issues";
    }

    /**
     * This method gets called when the {@link Plugin} gets disabled.
     * Most often it is called when the {@link Server} is shutting down or reloading.
     */
    @Listener
    public void onDisable(StoppingEngineEvent<Server> event) {
        // Slimefun never loaded successfully, so we don't even bother doing stuff here
        if (instance() == null || minecraftVersion == null) {
            return;
        }

        // Cancel all tasks from this plugin immediately
        Sponge.server().scheduler().tasks(this.pluginContainer).forEach(ScheduledTask::cancel);

        // Finishes all started movements/removals of block data
        try {
            ticker.halt();
            ticker.run();
        } catch (Exception x) {
            getLogger().error("Something went wrong while disabling the ticker task for Slimefun v" + this.pluginContainer.metadata().version().toString(), x);
        }

        // Kill our Profiler Threads
        profiler.kill();

        // Save all Player Profiles that are still in memory
        PlayerProfile.iterator().forEachRemaining(profile -> {
            if (profile.isDirty()) {
                profile.save();
            }
        });

        // Save all registered Worlds
        for (Map.Entry<String, BlockStorage> entry : getRegistry().getWorlds().entrySet()) {
            try {
                entry.getValue().saveAndRemove();
            } catch (Exception x) {
                getLogger().error("An Error occurred while saving Slimefun-Blocks in World '" + entry.getKey() + "' for Slimefun " + getVersion(), x);
            }
        }

        // Save all "universal" inventories (ender chests for example)
        for (UniversalBlockMenu menu : registry.getUniversalInventories().values()) {
            menu.save();
        }

        // Create a new backup zip
        if (config.getBoolean("options", "backup-data")) {
            backupService.run();
        }

        // Close and unload any resources from our Metrics Service
        metricsService.cleanUp();

        // Terminate our Plugin instance
        setInstance(null);

        /**
         * Close all inventories on the server to prevent item dupes
         * (Incase some idiot uses /reload)
         */
        for (ServerPlayer p : this.getServer().onlinePlayers()) {
            p.closeInventory();
        }
    }

    /**
     * This returns the time it took to load Slimefun (given a starting point).
     *
     * @param timestamp The time at which we started to load Slimefun.
     * @return The total time it took to load Slimefun (in ms or s)
     */
    private @Nonnull String getStartupTime(long timestamp) {
        long ms = (System.nanoTime() - timestamp) / 1000000;

        if (ms > 1000) {
            return NumberUtils.roundDecimalNumber(ms / 1000.0) + 's';
        } else {
            return NumberUtils.roundDecimalNumber(ms) + "ms";
        }
    }

    /**
     * This method checks if this is currently running in a unit test
     * environment.
     *
     * @return Whether we are inside a unit test
     */
    public boolean isUnitTest() {
        return minecraftVersion == null;
    }

    /**
     * This method checks for the {@link MinecraftVersion} of the {@link Server}.
     * If the version is unsupported, a warning will be printed to the console.
     *
     * @return Whether the {@link MinecraftVersion} is unsupported
     */
    private boolean isVersionUnsupported() {
        // Now check the actual Version of Minecraft
        this.minecraftVersion = Sponge.platform().minecraftVersion();
        return true;
    }

    /**
     * This method creates all necessary directories (and sub directories) for Slimefun.
     */
    private void createDirectories() {
        String[] storageFolders = {"Players", "blocks", "stored-blocks", "stored-inventories", "stored-chunks", "universal-inventories", "waypoints", "block-backups"};
        String[] pluginFolders = {"scripts", "error-reports", "cache/github", "world-settings"};

        for (String folder : storageFolders) {
            File file = new File("data-storage/Slimefun", folder);

            if (!file.exists()) {
                file.mkdirs();
            }
        }

        for (String folder : pluginFolders) {
            File file = new File("plugins/Slimefun", folder);

            if (!file.exists()) {
                file.mkdirs();
            }
        }
    }

    /**
     * This method registers all of our {@link Listener Listeners}.
     */
    private void registerListeners() {
        // Old deprecated CS-CoreLib Listener
        new MenuListener(this);

        new SlimefunBootsListener(this);
        new SlimefunItemInteractListener(this);
        new SlimefunItemConsumeListener(this);
        new BlockPhysicsListener(this);
        new CargoNodeListener(this);
        new MultiBlockListener(this);
        new GadgetsListener(this);
        new DispenserListener(this);
        new BlockListener(this);
        new EnhancedFurnaceListener(this);
        new ItemPickupListener(this);
        new ItemDropListener(this);
        new DeathpointListener(this);
        new ExplosionsListener(this);
        new DebugFishListener(this);
        new FireworksListener(this);
        new WitherListener(this);
        new IronGolemListener(this);
        new EntityInteractionListener(this);
        new MobDropListener(this);
        new VillagerTradingListener(this);
        new ElytraImpactListener(this);
        new CraftingTableListener(this);
        new AnvilListener(this);
        new BrewingStandListener(this);
        new CauldronListener(this);
        new GrindstoneListener(this);
        new CartographyTableListener(this);
        new ButcherAndroidListener(this);
        new MiningAndroidListener(this);
        new NetworkListener(this, networkManager);
        new HopperListener(this);
        new TalismanListener(this);
        new SoulboundListener(this);
        new AutoCrafterListener(this);
        new SlimefunItemHitListener(this);
        new MiddleClickListener(this);
        new BeeListener(this);
        new BeeWingsListener(this, (BeeWings) SlimefunItems.BEE_WINGS.getItem());
        new PiglinListener(this);
        new SmithingTableListener(this);

        // Item-specific Listeners
        new CoolerListener(this, (Cooler) SlimefunItems.COOLER.getItem());
        new SeismicAxeListener(this, (SeismicAxe) SlimefunItems.SEISMIC_AXE.getItem());
        new RadioactivityListener(this);
        new AncientAltarListener(this, (AncientAltar) SlimefunItems.ANCIENT_ALTAR.getItem(), (AncientPedestal) SlimefunItems.ANCIENT_PEDESTAL.getItem());
        grapplingHookListener.register(this, (GrapplingHook) SlimefunItems.GRAPPLING_HOOK.getItem());
        bowListener.register(this);
        backpackListener.register(this);

        // Handle Slimefun Guide being given on Join
        new SlimefunGuideListener(this, config.getBoolean("guide.receive-on-first-join"));

        // Clear the Slimefun Guide History upon Player Leaving
        new PlayerProfileListener(this);
    }

    /**
     * This (re)loads every {@link SlimefunTag}.
     */
    private void loadTags() {
        for (SlimefunTag tag : SlimefunTag.values()) {
            try {
                // Only reload "empty" (or unloaded) Tags
                if (tag.isEmpty()) {
                    tag.reload();
                }
            } catch (TagMisconfigurationException e) {
                getLogger().error("Failed to load Tag: " + tag.name(), e);
            }
        }
    }

    /**
     * This loads all of our items.
     */
    private void loadItems() {
        try {
            SlimefunItemSetup.setup(this);
        } catch (Exception | LinkageError x) {
            getLogger().error("An Error occurred while initializing SlimefunItems for Slimefun " + getVersion(), x);
        }
    }

    /**
     * This loads our researches.
     */
    private void loadResearches() {
        try {
            ResearchSetup.setupResearches();
        } catch (Exception | LinkageError x) {
            getLogger().error("An Error occurred while initializing Slimefun Researches for Slimefun " + getVersion(), x);
        }
    }

}
