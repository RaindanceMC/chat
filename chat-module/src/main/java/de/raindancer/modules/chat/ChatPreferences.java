package de.raindancer.modules.chat;

import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.chat.model.ChatStyle;
import de.raindancer.modules.chat.model.MentionPing;
import de.raindancer.modules.chat.service.ChatStyleService;
import de.raindancer.modules.chat.util.PermissionNodes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

import java.util.Arrays;
import java.util.function.Supplier;

/** What /preferences offers for chat: the mention sound, kept in the chat style like /chatstyle does. */
public final class ChatPreferences {

    private static final String GROUP = "Chat";

    private ChatPreferences() {
    }

    public static Preference mentionPing(Supplier<ChatStyleService> styles) {
        return Preference.choice("chat:mention-ping", GROUP, "Sound when you are mentioned",
                        "What you hear when somebody @-mentions you.",
                        Arrays.stream(MentionPing.values()).map(MentionPing::label).toList(),
                        player -> styles.get().styleOf(player.getUniqueId()).ping().ordinal(),
                        (player, index) -> {
                            MentionPing picked = MentionPing.values()[index];
                            ChatStyleService live = styles.get();
                            ChatStyle style = live.styleOf(player.getUniqueId());
                            player.playSound(Sound.sound(Key.key(picked.sound()), Sound.Source.PLAYER,
                                    picked.volume(), picked.pitch()));
                            live.set(player.getUniqueId(), style.withPing(picked));
                        })
                .needing(PermissionNodes.STYLE);
    }
}
