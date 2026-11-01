package com.pixplaze.webapi;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * {@code /pixplaze login} — sign in to Pixplaze: a link to confirm the sign-in comes to chat. An operator on a
 * server that is not connected yet starts the server connection instead: nobody can sign in before it.
 *
 * <p>{@code /pixplaze connect} — operators and the console: connect the server again after a refusal or an
 * unconfirmed link, without a restart. The link to confirm goes to the server console.
 */
public final class PixplazeCommand implements CommandExecutor, TabCompleter {

    private static final String LOGIN = "login";
    private static final String CONNECT = "connect";

    private final Plugin plugin;
    private final PixplazeConnection connection;

    public PixplazeCommand(Plugin plugin, PixplazeConnection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            return false;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case LOGIN -> login(sender);
            case CONNECT -> connect(sender);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void login(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only a player can sign in to Pixplaze.");
            return;
        }
        connection.startLogin(player, reply(sender));
    }

    private void connect(CommandSender sender) {
        if (!mayConnect(sender)) {
            sender.sendMessage("Only an operator can connect the server to Pixplaze.");
            return;
        }
        connection.connect(reply(sender), sender instanceof Player player ? player.getUniqueId() : null);
    }

    private static boolean mayConnect(CommandSender sender) {
        return sender instanceof ConsoleCommandSender || sender.isOp();
    }

    /// Replies come from the network thread: Bukkit API is called on the main one.
    private Consumer<String> reply(CommandSender sender) {
        return message -> {
            if (plugin.isEnabled()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(message));
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        final var prefix = args[0].toLowerCase(Locale.ROOT);
        return (mayConnect(sender) ? List.of(LOGIN, CONNECT) : List.of(LOGIN)).stream()
                .filter(option -> option.startsWith(prefix))
                .toList();
    }
}
