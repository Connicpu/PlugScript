package net.connorcpu.plugscript;

import lombok.Data;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.EventExecutor;

/**
 * Created with IntelliJ IDEA.
 * User: Connor
 * Date: 12/10/13
 * Time: 11:08 AM
 * To change this template use File | Settings | File Templates.
 */
@Data public class EventHandler {
    private EventExecutor executor;
    private String handlerId;
    private EventPriority priority;
    private Class<? extends Event> eventType;
    private boolean ignoreCancelled;
}
