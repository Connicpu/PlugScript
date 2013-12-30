package net.connorcpu.plugscript;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.RaiseException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Level;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

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
        System.setProperty("org.jruby.embed.localvariable.behavior", "persistent");
        reloadConfig();
        saveConfig();

        if (!this.registerJRuby()) {
            getLogger().severe("JRuby could not be located! Check your config!");
            return;
        }

        this.loadRubyEngines();
    }

    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }

        if (cmd.getName().equalsIgnoreCase("plugscripteval")) {
            return evalCommand(sender, args);
        }

        switch (args[0].toLowerCase()) {
            case "list":
                sender.sendMessage("\u00A7a===== JRuby scripts =====");
                for (ScriptedPlugin plugin : rubyEngines.values()) {
                    sender.sendMessage("\u00A7a- " + plugin.getContext().getEngineName());
                }
                sender.sendMessage("\u00A7a=========================");

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
                return true;
            case "eval":
                return evalCommand(sender, args);
            default:
                return false;
        }
    }

    private boolean evalCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return false;
        }

        String engineName = args[0];
        String command = joinCommandArgs(args);

        ScriptedPlugin plugin = findPluginCaseInsensitive(engineName);
        ScriptingContainer engine = plugin.getEngine();
        engine.put("$p", sender);
        if (sender instanceof Player) {
            Player p = (Player) sender;
            engine.put("$w", p.getWorld());
            engine.put("$l", p.getLocation());
        } else {
            engine.put("$w", getServer().getWorlds().get(0));
            engine.put("$l", getServer().getWorlds().get(0).getSpawnLocation());
        }

        try {
            sender.sendMessage("\u00A77" + plugin.getName() + "> " + command);
            Object result = engine.runScriptlet(command);
            if (result == null) {
                result = "nil";
            }
            sender.sendMessage("\u00A7a" + result.toString());
        } catch (Throwable ex) {
            sender.sendMessage("\u00A7c" + ex.getMessage());
        }

        return true;
    }

    private String joinCommandArgs(String[] args) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < args.length; ++i) {
            builder.append(args[i]);
            builder.append(' ');
        }
        return builder.toString();
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
            String engineName = null;
            try {
                String name = file.getName();
                if (!name.toLowerCase().endsWith(".rb"))
                    continue;

                engineName = name.substring(0, name.length() - 3).replace(" ", "_");

                if (engineName.isEmpty() || rubyEngines.containsKey(engineName)) {
                    // No files named ".rb" or duplicate engines
                    continue;
                }

                getLogger().info("Loading ruby script " + file.getName());

                ScriptedPlugin plugin = new ScriptedPlugin();
                PluginContext context = new PluginContext(engineName, "jruby");

                plugin.setScriptFile(file);
                plugin.setName(engineName);
                plugin.setContext(context);

                if (plugin.reloadScript()) {
                    getLogger().log(Level.INFO, "PlugScript " + engineName + " has been loaded successfully");
                }

                rubyEngines.put(engineName, plugin);
                newEngines.add(engineName);
            } catch (Throwable t) {
                getLogger().severe("PlugScript " + engineName + " could not be loaded");
                getLogger().severe(t.getMessage());
                t.printStackTrace();
            }
        }

        return newEngines;
    }

    boolean registerJRuby() {
        try {
            File jrbFolder = new File(getConfig().getString("jruby.path"));
            File jrbJar = new File(jrbFolder, "lib/jruby.jar");

            if (!jrbJar.exists()) {
                getLogger().log(SEVERE, "JRuby runtime not found: " + jrbJar.getPath());
                return false;
            }

            URL jrbURL = jrbJar.toURI().toURL();

            URLClassLoader syscl = (URLClassLoader) ClassLoader.getSystemClassLoader();
            URL[] urls = syscl.getURLs();
            for (URL url : urls)
                if (url.sameFile(jrbURL)) {
                    getLogger().log(INFO, "Using present JRuby.jar from the classpath.");
                    return true;
                }

            // URLClassLoader.addUrl is protected
            Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
            addURL.setAccessible(true);

            // add jruby.jar to Bukkit's class path
            addURL.invoke(syscl, new Object[]{jrbURL});

            getLogger().log(INFO, "Using JRuby runtime " + jrbJar.getPath());

            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override public void onDisable() {
        for (ScriptedPlugin plugin : rubyEngines.values()) {
            if (plugin.getContext().getDisableHandler() != null) {
                plugin.getContext().getDisableHandler().run();
            }
        }
    }

    @Data public class PluginContext {
        private final String engineName;
        private final String engineType;
        private ScriptedPlugin plugin;
        private List<String> registeredEvents = new LinkedList<>();

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
                getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
            }
        }

        public Server getServer() {
            return PlugScript.this.getServer();
        }

        public PlugScript getPlugin() {
            return PlugScript.this;
        }

        public ScriptingContainer getRubyEngine(String name) {
            return rubyEngines.get(name).getEngine();
        }

        public void registerEvent(EventHandler handler) {
            if (!registeredEvents.contains(handler.getHandlerId())) {
                registeredEvents.add(handler.getHandlerId());
            }
            PlugEvents.registerHandler(PlugScript.this, this.engineName, handler);
        }

        public void unregisterEvent(String handlerId) {
            if (registeredEvents.contains(handlerId)) {
                registeredEvents.remove(handlerId);
            }
            PlugEvents.unregisterHandler(this.engineName, handlerId);
        }

        public void unregisterAllEvents() {
            for (String handlerId : registeredEvents) {
                PlugEvents.unregisterHandler(this.engineName, handlerId);
            }
            registeredEvents.clear();
        }

        public void registerCommand(ScriptCommandExecutor command)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            command.setPluginName(engineName);

            Server server = getServer();
            SimpleCommandMap commandMap = (SimpleCommandMap) server
                .getClass().getMethod("getCommandMap").invoke(server);

            Command existingCommand = commandMap.getCommand(command.getName());
            if (existingCommand instanceof ScriptCommandExecutor) {
                commandMap.getCommands().remove(existingCommand);
            }

            commandMap.register("ps:" + getEngineName() + ":", command);
        }

        public void unregisterCommand(String name)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            Server server = getServer();
            SimpleCommandMap commandMap = (SimpleCommandMap) server
                .getClass().getMethod("getCommandMap").invoke(server);
            Command command = commandMap.getCommand(name);
            commandMap.getCommands().remove(command);
        }

        public void unregisterAllCommands()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            Server server = getServer();
            SimpleCommandMap commandMap = (SimpleCommandMap) server
                .getClass().getMethod("getCommandMap").invoke(server);
            List<Command> commandsCopy = new ArrayList<>(commandMap.getCommands());
            for (Command command : commandsCopy) {
                if (command instanceof ScriptCommandExecutor) {
                    ScriptCommandExecutor cmd = (ScriptCommandExecutor) command;
                    if (cmd.getPluginName().equals(engineName)) {
                        commandMap.getCommands().remove(command);
                    }
                }
            }
        }

        public boolean require(String scriptFile) throws FileNotFoundException, ScriptException {
            File directory = new File("./plugins/" + this.engineName);
            File file = new File(directory, scriptFile);
            return plugin.loadFile(plugin.getEngine(), file);
        }
    }
}








