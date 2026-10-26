package de.raindancer.modules.chat.support;

import de.raindancer.core.RainsCore;
import de.raindancer.core.platform.command.PlayerTargets;
import de.raindancer.core.platform.util.Scheduling;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * {@code /support admin …}: making somebody a supporter by hand, taking it away, and giving out donations that
 * came without a code. Writing is disk work, so each change is saved off the server's threads.
 */
public final class SupportStaff {

    public static final List<String> SUBCOMMANDS = List.of("inbox", "give", "take", "assign", "dismiss");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault());

    private final Plugin plugin;
    private final Server server;
    private final RainsCore core;
    /** Only on the server that receives the webhook; empty elsewhere. */
    private final Supplier<Optional<SupportDesk>> desk;

    public SupportStaff(Plugin plugin, Server server, RainsCore core, Supplier<Optional<SupportDesk>> desk) {
        this.plugin = plugin;
        this.server = server;
        this.core = core;
        this.desk = desk;
    }

    /** @param args everything after {@code admin} */
    public void run(CommandSender sender, String[] args) {
        String what = args.length == 0 ? "" : args[0].toLowerCase(java.util.Locale.ROOT);
        switch (what) {
            case "inbox" -> inbox(sender);
            case "give" -> give(sender, args);
            case "take" -> take(sender, args);
            case "assign" -> assign(sender, args);
            case "dismiss" -> dismiss(sender, args);
            default -> say(sender, "chat.support.admin.usage");
        }
    }

    private void inbox(CommandSender sender) {
        Optional<SupportDesk> here = desk.get();
        if (here.isEmpty()) {
            say(sender, "chat.support.admin.not-here");
            return;
        }
        List<SupportInbox.Waiting> waiting = here.get().waiting();
        if (waiting.isEmpty()) {
            say(sender, "chat.support.admin.inbox-empty");
            return;
        }
        say(sender, "chat.support.admin.inbox-head");
        for (int i = 0; i < waiting.size(); i++) {
            SupportInbox.Waiting one = waiting.get(i);
            say(sender, "chat.support.admin.inbox-line", "number", String.valueOf(i + 1),
                    "name", one.supporterName().isBlank() ? "?" : one.supporterName(),
                    "amount", one.amount().isBlank() ? "?" : one.amount(), "coffees", String.valueOf(one.coffees()),
                    "when", DAY.format(one.at()));
        }
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 2) {
            say(sender, "chat.support.admin.usage");
            return;
        }
        Optional<UUID> who = PlayerTargets.idOf(server, args[1]);
        if (who.isEmpty()) {
            say(sender, "chat.support.admin.unknown-player", "player", args[1]);
            return;
        }
        // An amount given by hand is what the owner says reached them: no fees taken off.
        long cents = args.length > 2 ? BmcWebhook.centsOf(args[2]) : 0;
        var now = core.supporters().credit(who.get(), 1, cents, cents, Instant.now());
        Scheduling.async(plugin, core.supporters()::save);
        say(sender, "chat.support.admin.given", "player", args[1],
                "amount", String.format(java.util.Locale.ROOT, "%.2f", now.cents() / 100.0));
    }

    private void take(CommandSender sender, String[] args) {
        if (args.length < 2) {
            say(sender, "chat.support.admin.usage");
            return;
        }
        Optional<UUID> who = PlayerTargets.idOf(server, args[1]);
        if (who.isEmpty() || !core.supporters().revoke(who.get())) {
            say(sender, "chat.support.admin.not-one", "player", args[1]);
            return;
        }
        Scheduling.async(plugin, core.supporters()::save);
        say(sender, "chat.support.admin.taken", "player", args[1]);
    }

    private void assign(CommandSender sender, String[] args) {
        Optional<SupportDesk> here = desk.get();
        if (here.isEmpty()) {
            say(sender, "chat.support.admin.not-here");
            return;
        }
        Optional<Integer> number = args.length > 2 ? number(args[1]) : Optional.empty();
        if (number.isEmpty()) {
            say(sender, "chat.support.admin.usage");
            return;
        }
        Optional<UUID> who = PlayerTargets.idOf(server, args[2]);
        if (who.isEmpty()) {
            say(sender, "chat.support.admin.unknown-player", "player", args[2]);
            return;
        }
        Scheduling.async(plugin, () -> here.get().assign(number.get(), who.get()).ifPresentOrElse(
                done -> say(sender, "chat.support.admin.assigned", "number", args[1], "player", args[2]),
                () -> say(sender, "chat.support.admin.no-such", "number", args[1])));
    }

    private void dismiss(CommandSender sender, String[] args) {
        Optional<SupportDesk> here = desk.get();
        if (here.isEmpty()) {
            say(sender, "chat.support.admin.not-here");
            return;
        }
        Optional<Integer> number = args.length > 1 ? number(args[1]) : Optional.empty();
        if (number.isEmpty()) {
            say(sender, "chat.support.admin.usage");
            return;
        }
        Scheduling.async(plugin, () -> here.get().dismiss(number.get()).ifPresentOrElse(
                done -> say(sender, "chat.support.admin.dismissed", "number", args[1]),
                () -> say(sender, "chat.support.admin.no-such", "number", args[1])));
    }

    private void say(CommandSender sender, String key, Object... values) {
        core.messages().send(sender, key, values);
    }

    private static Optional<Integer> number(String text) {
        try {
            int value = Integer.parseInt(text.strip());
            return value > 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
    }
}
