package net.connorcpu.plugscript;

import org.bukkit.command.CommandSender;

/**
 * Created by Connor on 12/12/13.
 */
public interface ScriptCommand {
    public Boolean execute(CommandSender sender, String commandLabel, String[] args);
}
