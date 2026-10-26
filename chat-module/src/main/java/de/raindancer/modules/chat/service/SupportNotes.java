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
    private final de.raindancer.modules.chat.support.SupportCodes codes;

    public SupportNotes(Plugin plugin, Server server, RainsCore core, SupportService support,
                        de.raindancer.modules.chat.support.SupportCodes codes) {
        this.plugin = plugin;
        this.server = server;
        this.core = core;
        this.support = support;
        this.codes = codes;
    }

    /** @return whether anybody was told, so there is something to save */
    public boolean tick() {
        boolean told = false;
        for (Player player : server.getOnlinePlayers()) {
            UUID who = player.getUniqueId();
            var supporter = core.supporters().of(who);
            if (supporter.isPresent() && !supporter.get().thanked()) {
                core.supporters().thanked(who);
                player.sendMessage(SupportLetter.thanks(core.messages(), player.getName()));
                core.effects().play(who, de.raindancer.core.ui.effect.Cues.NOTIFY);
                told = true;
            }
            if (!core.playtime().isKnown(who) || Away.isAway(who)) {
                continue;
            }
            long played = core.playtime().played(who).toMinutes();
            if (support.isFollowUpDue(who, played)) {
                Optional<String> link = support.link();
                if (link.isPresent()) {
                    support.followedUp(who);
                    player.sendMessage(letterFor(player, false).inChat(link.get()));
                    core.effects().play(who, de.raindancer.core.ui.effect.Cues.NOTIFY);
                    told = true;
                }
                continue;
            }
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
        support.opened(player.getUniqueId());
        SupportWindow.show(player, letterFor(player, wearing), link.get(), wearing);
        return true;
    }

    private SupportLetter letterFor(Player player, boolean wearing) {
        UUID who = player.getUniqueId();
        SupportLetter letter = SupportLetter.of(core.messages(), player.getName(), support.buttonText(),
                support.link().orElse(""), support.signed(), wearing);
        if (core.supporters().isSupporter(who)) {
            return letter.forSupporter();
        }
        if (support.current().rewards()) {
            letter = letter.forCode(codes.codeOf(who));
            if (codes.isDirty()) {
                Scheduling.async(plugin, codes::save);
            }
        }
        return letter;
    }
}
