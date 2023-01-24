package com.gmail.necnionch.myplugin.itemlistdumper.bukkit;

import com.google.common.collect.Maps;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleCommand implements TabExecutor {
    private final Map<String, CommandExecutor> subCommands = Maps.newHashMap();

    public SimpleCommand() {
    }

    protected Map<String, CommandExecutor> subcommands() {
        return subCommands;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && subCommands.containsKey(args[0].toLowerCase(Locale.ROOT))) {
            return subCommands.get(args[0].toLowerCase(Locale.ROOT)).onCommand(sender, command, label, args);
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

}
