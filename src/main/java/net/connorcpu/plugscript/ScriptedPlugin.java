package net.connorcpu.plugscript;

import lombok.Data;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: Connor
 * Date: 12/9/13
 * Time: 6:57 PM
 * To change this template use File | Settings | File Templates.
 */
@Data public class ScriptedPlugin {
    private Map<String, Object> persistence = new HashMap<>();
    private ScriptEngine engine;
    private PlugScript.PluginContext context;
    private String name;
    private File scriptFile;
    private List<File> addedFiles = new ArrayList<>();

    public boolean reloadScript() {
        PlugScript plugin = context.getPlugin();
        ScriptEngine oldEngine = engine;
        engine = plugin.scriptFactory.getEngineByName(context.getEngineType());
        engine.put("context", context);
        try {
            context.setEngine(engine);

            switch (context.getEngineType()) {
                case "jruby":
                    InputStream includeFile = PlugScript.class.getResourceAsStream("scripts/jruby/jruby_include.rb");
                    engine.eval(new BufferedReader(new InputStreamReader(includeFile)));
                    break;
            }

            engine.eval(new BufferedReader(new FileReader(scriptFile.getAbsolutePath())));
        } catch (ScriptException ex) {
            engine = oldEngine;
            context.setEngine(engine);
            context.getPlugin().getLogger().log(
                    Level.SEVERE,
                    "[PlugScript] An exception occurred initializing the script file " + scriptFile,
                    ex);
            return false;
        } catch (FileNotFoundException ex) {
            engine = oldEngine;
            context.setEngine(engine);
            context.getPlugin().getLogger().log(
                    Level.SEVERE,
                    "[PlugScript] Couldn't find the script file " + scriptFile,
                    ex);
            return false; //I'm looking up jruby docs
        }

        return true;
    }
}
