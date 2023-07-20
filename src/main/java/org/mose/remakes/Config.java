package org.mose.remakes;

import org.spongepowered.api.Sponge;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.plugin.PluginContainer;

import java.io.File;
import java.util.function.Function;

public class Config {

    private final PluginContainer container;
    private final String fileName;


    private final File file;
    private final ConfigurationLoader<? extends ConfigurationNode> loader;
    private final ConfigurationNode rootNode;

    public Config(PluginContainer container) {
        this(container, "config.yml");
    }

    public Config(PluginContainer container, String fileName) {
        this.container = container;
        this.fileName = fileName;

        File folder = Sponge.configManager().pluginConfig(container).directory().toFile();
        this.file = new File(folder, fileName);
        if (fileName.endsWith(".yml")) {
            this.loader = YamlConfigurationLoader.builder().file(this.file).build();
        } else {
            throw new RuntimeException("Unknown filetype of " + fileName);
        }

        try {
            this.rootNode = this.loader.load();
        } catch (ConfigurateException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T get(Function<ConfigurationNode, T> func, Object... path) {
        return func.apply(this.rootNode.node(path));
    }

    public int getInt(Object... path) {
        return get(ConfigurationNode::getInt, path);
    }

    public String getString(Object... path) {
        return get(ConfigurationNode::getString, path);
    }

    public boolean getBoolean(Object... path) {
        return get(ConfigurationNode::getBoolean, path);
    }
}
