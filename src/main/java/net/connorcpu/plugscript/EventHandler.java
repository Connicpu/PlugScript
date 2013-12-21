package net.connorcpu.plugscript;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

/**
 * Created with IntelliJ IDEA.
 * User: Connor
 * Date: 12/10/13
 * Time: 11:08 AM
 * To change this template use File | Settings | File Templates.
 */
public interface EventHandler {
    void execute(Event event);
    EventPriority priority();
    String handlerId();
    Class<? extends Event> eventType();
    boolean ignoreCancelled();
}
