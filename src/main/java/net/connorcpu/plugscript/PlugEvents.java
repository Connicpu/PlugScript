package net.connorcpu.plugscript;

import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Connor
 * Date: 12/10/13
 * Time: 11:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class PlugEvents {
    private static Map<String, List<CustomScriptEvent>> eventHandlers = new HashMap<>();

    protected static void registerHandler(PlugScript plugScript, String plugin, EventHandler handler) {
        List<CustomScriptEvent> handlers;
        if (!eventHandlers.containsKey(plugin)) {
            eventHandlers.put(plugin, handlers = new ArrayList<>());
        } else {
            handlers = eventHandlers.get(plugin);
        }
        CustomScriptEvent executor = getHandler(handler.handlerId(), handlers);
        if (executor != null) {
            executor.setHandler(handler);
            executor.setDisabled(false);
            return;
        }

        executor = new CustomScriptEvent(handler);
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        pluginManager.registerEvent(
                handler.eventType(),
                null,
                handler.priority(),
                executor,
                plugScript,
                handler.ignoreCancelled());

        handlers.add(executor);
    }

    private static CustomScriptEvent getHandler(String handlerId, List<CustomScriptEvent> eventList) {
        for (CustomScriptEvent event : eventList) {
            if (event.getHandler().handlerId().equals(handlerId)) {
                return event;
            }
        }
        return null;
    }

    protected static void unregisterHandler(String plugin, String handlerId) {
        List<CustomScriptEvent> handlers;
        if (!eventHandlers.containsKey(plugin)) {
            return;
        }

        handlers = eventHandlers.get(plugin);
        CustomScriptEvent executor = getHandler(handlerId, handlers);
        if (executor == null) {
            return;
        }

        executor.setDisabled(true);
    }

    @Data static class CustomScriptEvent implements EventExecutor {
        private EventHandler handler;
        private boolean disabled;

        public CustomScriptEvent(EventHandler handler) {
            this.disabled = true;
            this.handler = handler;
        }

        @Override public void execute(Listener listener, Event event) throws EventException {
            if (!disabled) {
                handler.execute(event);
            }
        }
    }
}
