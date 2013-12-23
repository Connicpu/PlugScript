package net.connorcpu.plugscript;

import lombok.Data;
import org.jruby.CompatVersion;
import org.jruby.RubyException;
import org.jruby.RubyInstanceConfig;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.RaiseException;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.*;
import java.util.*;
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
    private ScriptingContainer engine;
    private PlugScript.PluginContext context;
    private String name;
    private File scriptFile;
    private List<File> addedFiles = new ArrayList<>();

    public boolean reloadScript() {
        PlugScript plugin = context.getPlugin();
        engine = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT);
        engine.setCompileMode(RubyInstanceConfig.CompileMode.JIT);
        engine.setCompatVersion(rubyCompat());
        engine.setClassLoader(PlugScript.class.getClassLoader());
        engine.put("$context", context);

        File jrbFolder = new File(plugin.getConfig().getString("jruby.path"));
        File rubyFolder = new File(jrbFolder, "lib/ruby");
        String[] loadPaths = new String[]{
            new File("./plugins/" + name).getAbsolutePath(),
            new File(rubyFolder, "site_ruby/" + rubyVersion()).getAbsolutePath(),
            new File(rubyFolder, "site_ruby/shared").getAbsolutePath(),
            new File(rubyFolder, rubyVersion()).getAbsolutePath()
        };
        engine.setLoadPaths(Arrays.asList(loadPaths));


        return loadResource(engine, "/scripts/jruby/jruby_include.rb")
            && loadFile(engine, scriptFile);
    }

    private boolean loadFile(ScriptingContainer runtime, File file) {
        try {
            Reader input = new FileReader(file);

            try {
                runtime.runScriptlet(input, file.getPath());
            } catch (RaiseException e) {
                RubyException rbe = e.getException();
                System.err.println("An error loading script file " + file);
                System.err.println(e.getMessage());
                rbe.printBacktrace(System.err);
            } finally {
                input.close();
            }

            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean loadResource(ScriptingContainer runtime, String name) {
        try {
            Reader input = new InputStreamReader(PlugScript.class.getResourceAsStream(name));

            try {
                runtime.runScriptlet(input, "~!" + name);
            } catch (RaiseException e) {
                RubyException rbe = e.getException();
                System.err.println("An error loading script file ~!" + name);
                System.err.println(e.getMessage());
                rbe.printBacktrace(System.err);
            } finally {
                input.close();
            }

            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public String rubyVersion() {
        PlugScript plugin = context.getPlugin();
        return plugin.getConfig().getString("jruby.version");
    }

    public CompatVersion rubyCompat() {
        switch (rubyVersion()) {
            case "1.8":
                return CompatVersion.RUBY1_8;
            case "2.0":
                return CompatVersion.RUBY2_0;
            case "1.9":
            default:
                return CompatVersion.RUBY1_9;
        }
    }
}
