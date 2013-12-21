package net.connorcpu.plugscript;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: Connor
 * Date: 12/9/13
 * Time: 6:19 PM
 */
public class PlugScript extends JavaPlugin {
    protected ScriptEngineManager scriptFactory;
    public Map<String, ScriptedPlugin> rubyEngines;

    @Override public void onEnable() {
        loadRubyEngines();
    }

    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (args[0].toLowerCase()) {
            case "list":
                sender.sendMessage("\u00A7a===== JRuby scripts =====");
                for (ScriptedPlugin plugin : rubyEngines.values()) {
                    sender.sendMessage("\u00A7a- " + plugin.getContext().getEngineName());
                }

                return true;
            case "reload":
                if (args.length != 2) {
                    return false;
                }

                ScriptedPlugin plugin = findPluginCaseInsensitive(args[1]);
                if (plugin == null) {
                    sender.sendMessage("\u00A7cPlugin '" + args[1] + "' not found");
                    return true;
                }

                if (plugin.reloadScript()) {
                    sender.sendMessage("\u00A7aPlugin " + plugin.getName() + " reloaded successfully!");
                } else {
                    sender.sendMessage(
                            "\u00A7cAn error occurred reloading " +
                                    plugin.getName() +
                                    " (check console for details)");
                }

                return true;
            case "rescan":
                List<String> newEngines = loadRubyEngines();
                if (newEngines.size() > 0) {
                    StringBuilder message = new StringBuilder();
                    message.append("\u00A7aNew script plugins loaded: ");

                    for (String engineName : newEngines.subList(0, newEngines.size() - 1)) {
                        message.append(engineName);
                        message.append(", ");
                    }
                    message.append(newEngines.get(newEngines.size() - 1));
                } else {
                    sender.sendMessage("\u00A7aNo new script plugins found");
                }
            default:
                return false;
        }
    }

    private ScriptedPlugin findPluginCaseInsensitive(String name) {
        for (ScriptedPlugin plugin : rubyEngines.values()) {
            if (name.equalsIgnoreCase(plugin.getName())) {
                return plugin;
            }
        }

        return null;
    }

    private List<String> loadRubyEngines() {
        if (scriptFactory == null) {
            scriptFactory = new ScriptEngineManager();
        }
        if (rubyEngines == null) {
            rubyEngines = new HashMap<>();
        }

        File pluginDir = new File("./plugins/");
        File[] pluginFiles = pluginDir.listFiles();

        List<String> newEngines = new LinkedList<>();
        // Find ruby files
        for (File file : pluginFiles) {
            String name = file.getName();
            if (!name.toLowerCase().endsWith(".rb"))
                continue;

            String engineName = name.substring(0, name.length() - 3).replace(" ", "_");

            if (engineName.isEmpty() || rubyEngines.containsKey(engineName)) {
                // No files named ".rb" or duplicate engines
                continue;
            }

            getLogger().info("[PlugScript] Loading ruby script " + file.getName());

            ScriptedPlugin plugin = new ScriptedPlugin();
            PluginContext context = new PluginContext(engineName, "jruby");

            plugin.setName(engineName);
            plugin.setContext(context);

            plugin.reloadScript();

            rubyEngines.put(engineName, plugin);

            newEngines.add(engineName);
        }

        return newEngines;
    }

    @Override public void onDisable() {
        for (ScriptedPlugin plugin : rubyEngines.values()) {
            plugin.getContext().getDisableHandler().run();
        }
    }

    @Data public class PluginContext {
        private final String engineName;
        private final String engineType;
        private ScriptEngine engine;
        private ScriptedPlugin plugin;

        @Setter(AccessLevel.PRIVATE) private FileConfiguration config = null;
        @Setter(AccessLevel.PRIVATE) private File configFile = null;

        private Runnable disableHandler;

        public PluginContext(String engineName, String engineType) {
            this.engineName = engineName;
            this.engineType = engineType;
            this.reloadConfig();
        }

        public void reloadConfig() {
            if (configFile == null) {
                configFile = new File("./plugins/" + engineName, "config.yml");
            }
            config = YamlConfiguration.loadConfiguration(configFile);
        }

        public void saveConfig() {
            try {
                config.save(getConfigFile());
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "[PlugScript] Could not save config to " + configFile, ex);
            }
        }

        public Server getServer() {
            return PlugScript.this.getServer();
        }

        public PlugScript getPlugin() {
            return PlugScript.this;
        }

        public ScriptEngine getRubyEngine(String name) {
            return rubyEngines.get(name).getEngine();
        }

        public void registerEvent(EventHandler handler) {
            PlugEvents.registerHandler(PlugScript.this, this.engineName, handler);
        }

        public void unregisterEvent(String handlerId) {
            PlugEvents.unregisterHandler(this.engineName, handlerId);
        }

        public boolean require(String scriptFile) throws FileNotFoundException, ScriptException {
            File directory = new File("./plugins/" + this.engineName);
            if (!directory.exists()) {
                return false;
            }

            File file = new File(directory, scriptFile);
            if (!file.exists()) {
                return false;
            }

            for (File includedFile : this.plugin.getAddedFiles()) {
                if (includedFile.getAbsolutePath().equalsIgnoreCase(file.getAbsolutePath())) {
                    return true;
                }
            }

            engine.eval(new BufferedReader(new FileReader(file)));

            this.plugin.getAddedFiles().add(file);
            return true;
        }
    }
}








