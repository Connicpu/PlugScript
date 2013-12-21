package net.connorcpu.plugscript;

import lombok.Data;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Created by Connor on 12/12/13.
 */
@Data public class ScriptCommandExecutor extends Command {
    ScriptCommand command;

    public ScriptCommandExecutor(String name, String description, String usage,
                                 List<String> aliases, ScriptCommand command) {
        super(name, description, usage, aliases);

        this.command = command;
    }

    @Override public boolean execute(CommandSender sender, String label, String[] args) {
        return command.execute(sender, label, args);
    }
}
