package de.raindancer.modules.chat.service;

import de.raindancer.core.ui.chat.Chat;
import de.raindancer.core.ui.effect.Effect;
import de.raindancer.core.ui.effect.Effects;
import de.raindancer.core.ui.effect.SoundCue;
import de.raindancer.core.ui.effect.SoundSequence;
import de.raindancer.core.ui.messages.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** What announcements and the staff controls do on this server, whether typed here or heard from another server. */
public final class ChatEffects implements ChatNetwork.Receiver {

    private static final int BLANK_LINES = 100;

    /** What everybody hears when an ad goes out — its own cue, so an owner can rebind or silence just ads. */
    public static final String AD_CUE = "chat:ad";

    /** Two rising bell notes: brighter than the notify ping, and short. */
    private static final Effect AD_SOUND = Effect.of(new SoundSequence(List.of(
            SoundSequence.Step.now(new SoundCue("block.note_block.bell", 0.6f, 1.2f)),
            new SoundSequence.Step(new SoundCue("block.note_block.bell", 0.6f, 1.6f), 140L))));

    /** An ad's cue and its action bar line, for the players it is given. */
    @FunctionalInterface
    public interface AdCue {
        void play(Collection<UUID> players, Component bar);
    }

    private final Server server;
    private final Chat chat;
    private final Messages messages;
    private final FreezeService freeze;
    private final ChatQualityService quality;
    private final FormatService format;
    private final Consumer<Collection<UUID>> notifyAll;
    private final AdCue adCue;

    /** @param notifyAll plays Core's notify cue to the players it is given */
    public ChatEffects(Server server, Chat chat, Messages messages, FreezeService freeze, ChatQualityService quality,
                       FormatService format, Consumer<Collection<UUID>> notifyAll, AdCue adCue) {
        this.server = server;
        this.chat = chat;
        this.messages = messages;
        this.freeze = freeze;
        this.quality = quality;
        this.format = format;
        this.notifyAll = notifyAll;
        this.adCue = adCue;
    }

    /** Gives {@link #AD_CUE} its sound unless somebody already bound it. */
    public static void defineAdCue(Effects effects) {
        if (!effects.isDefined(AD_CUE)) {
            effects.define(AD_CUE, AD_SOUND);
        }
    }

    @Override
    public void banner(String text) {
        chat.broadcast(messages.raw("chat.announce.banner"), Chat.arg("text", text));
        notifyAll.accept(online());
    }

    @Override
    public void ad(UUID sender, String name, String text) {
        server.broadcast(format.renderAd(sender, name, text));
        adCue.play(online(), format.renderAdBar(sender, name, text));
    }

    @Override
    public void clear(String by) {
        for (Player online : server.getOnlinePlayers()) {
            for (int line = 0; line < BLANK_LINES; line++) {
                chat.blank(online);
            }
        }
        chat.broadcast(messages.raw("chat.admin.cleared"), Chat.arg("player", by));
    }

    @Override
    public void freeze(boolean frozen, String by) {
        if (frozen) {
            freeze.freeze();
        } else {
            freeze.unfreeze();
        }
        chat.broadcast(messages.raw(frozen ? "chat.admin.frozen" : "chat.admin.unfrozen"), Chat.arg("player", by));
    }

    @Override
    public void slowmode(int seconds, String by) {
        if (seconds < 0) {
            quality.clearSlowmodeOverride();
            chat.broadcast(messages.raw("chat.admin.slowmode-off"), Chat.arg("player", by));
            return;
        }
        quality.overrideSlowmode(seconds);
        chat.broadcast(messages.raw("chat.admin.slowmode-set"), Chat.arg("player", by), Chat.arg("seconds", seconds));
    }

    @Override
    public ChatNetwork.State state() {
        return new ChatNetwork.State(freeze.isFrozen(), quality.isSlowmodeOverridden() ? quality.effectiveSlowmode() : -1);
    }

    @Override
    public void adopt(ChatNetwork.State state) {
        if (state.frozen()) {
            freeze.freeze();
        } else {
            freeze.unfreeze();
        }
        if (state.slowmode() < 0) {
            quality.clearSlowmodeOverride();
        } else {
            quality.overrideSlowmode(state.slowmode());
        }
    }

    private Collection<UUID> online() {
        return server.getOnlinePlayers().stream().map(Player::getUniqueId).toList();
    }
}
