package io.github.thebusybiscuit.slimefun4.api;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.Logger;
import org.spongepowered.plugin.PluginContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This is a very basic interface that will be used to identify
 * the {@link PluginContainer} that registered a {@link SlimefunItem}.
 * <p>
 * It will also contain some utility methods such as {@link SlimefunAddon#getBugTrackerURL()}
 * to provide some context when bugs arise.
 * <p>
 * It is recommended to implement this interface if you are developing
 * an Addon.
 *
 * @author TheBusyBiscuit
 */
public interface SlimefunAddon {

    /**
     * This method returns the instance of {@link PluginContainer} that this
     * {@link SlimefunAddon} refers to.
     *
     * @return The instance of your {@link PluginContainer}
     */
    @Nonnull
    PluginContainer getContainer();

    /**
     * This method returns a link to the Bug Tracker of this {@link SlimefunAddon}
     *
     * @return The URL for this Plugin's Bug Tracker, or null
     */
    @Nullable
    String getBugTrackerURL();

    /**
     * This method returns the name of this addon, it defaults to the name
     * of the {@link PluginContainer} provided by {@link SlimefunAddon#getContainer()} ()}
     *
     * @return The Name of this {@link SlimefunAddon}
     */
    default @Nonnull String getName() {
        return getContainer().metadata().name().orElse(getContainer().metadata().id());
    }

    /**
     * This method returns the version of this addon, it defaults to the version
     * of the {@link PluginContainer} provided by {@link SlimefunAddon#getContainer()} ()}
     *
     * @return The version of this {@link SlimefunAddon}
     */
    default @Nonnull String getPluginVersion() {
        return getContainer().metadata().version().toString();
    }

    /**
     * This method returns the {@link Logger} of this addon, it defaults to the {@link Logger}
     * of the {@link PluginContainer} provided by {@link SlimefunAddon#getContainer()}
     *
     * @return The {@link Logger} of this {@link SlimefunAddon}
     */
    @Nonnull
    Logger getLogger();

    /**
     * This method checks whether the given String is the name of a dependency of this
     * {@link SlimefunAddon}.
     * It specifically checks whether the given String can be found in {@link org.spongepowered.plugin.metadata.model.PluginDependency()}
     *
     * @param dependency The dependency to check for
     * @return Whether this {@link SlimefunAddon} depends on the given {@link org.spongepowered.plugin.metadata.model.PluginDependency}
     */
    default boolean hasDependency(@Nonnull String dependency) {
        Validate.notNull(dependency, "The dependency cannot be null");

        // Well... it cannot depend on itself but you get the idea.
        if (getContainer().metadata().id().equalsIgnoreCase(dependency)) {
            return true;
        }
        return getContainer().metadata().dependency(dependency).isPresent();
    }

}
