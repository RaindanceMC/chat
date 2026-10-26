package de.raindancer.modules.chat.service;

import de.raindancer.core.RainsCore;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.social.presence.Away;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * The server side of the support note: once a minute, a chat line to whoever is due, and the window it opens.
 * The deciding is {@link SupportService}'s, the words are {@link SupportLetter}'s.
 */
public final class SupportNotes {

    /** How long the [Read it] button in chat keeps working; /support opens the window any time after. */
    private static final Duration BUTTON_LIFETIME = Duration.ofDays(1);

    private final Plugin plugin;
    private final Server server;
    private final RainsCore core;
    private final SupportService support;

    public SupportNotes(Plugin plugin, Server server, RainsCore core, SupportService support) {
        this.plugin = plugin;
        this.server = server;
        this.core = core;
        this.support = support;
    }

    /** @return whether anybody was told, so there is something to save */
    public boolean tick() {
        boolean told = false;
        for (Player player : server.getOnlinePlayers()) {
            UUID who = player.getUniqueId();
            if (!core.playtime().isKnown(who) || Away.isAway(who)) {
                continue;
            }
            long played = core.playtime().played(who).toMinutes();
            if (!support.isDue(who, played)) {
                continue;
            }
            support.reminded(who, played);
            var open = core.buttons().label(core.messages().raw("chat.support.open"))
                    .tooltip(core.messages().raw("chat.support.open-tooltip"))
                    .forOnly(who).expiringIn(BUTTON_LIFETIME).repeatable()
                    .does(clicker -> Scheduling.entity(plugin, player, () -> {
                        if (player.isOnline()) {
                            open(player);
                        }
                    }));
            player.sendMessage(SupportLetter.notice(core.messages(), open.render()));
            told = true;
        }
        return told;
    }

    /** @return false when there is no link to show */
    public boolean open(Player player) {
        Optional<String> link = support.link();
        if (link.isEmpty()) {
            return false;
        }
        boolean wearing = core.resourcePacks().isWearing(player.getUniqueId());
        SupportWindow.show(player, SupportLetter.of(core.messages(), player.getName(), support.buttonText(),
                link.get(), support.signed(), wearing), link.get(), wearing);
        return true;
    }
}
