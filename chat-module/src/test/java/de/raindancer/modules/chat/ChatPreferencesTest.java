package de.raindancer.modules.chat;

import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.chat.model.ChatStyle;
import de.raindancer.modules.chat.model.MentionPing;
import de.raindancer.modules.chat.service.ChatStyleService;
import de.raindancer.modules.chat.util.PermissionNodes;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatPreferencesTest {

    private final UUID id = UUID.randomUUID();
    private final Player player = mock(Player.class);
    private final ChatStyleService styles = mock(ChatStyleService.class);

    @Test
    void theOptionsAreEveryPingByItsReadableName() {
        Preference ping = ChatPreferences.mentionPing(() -> styles);
        assertThat(ping.id()).isEqualTo("chat:mention-ping");
        assertThat(ping.title()).isEqualTo("Sound when you are mentioned");
        assertThat(ping.options()).isEqualTo(Arrays.stream(MentionPing.values()).map(MentionPing::label).toList());
    }

    @Test
    void theCurrentOptionIsTheStoredPing() {
        Preference ping = ChatPreferences.mentionPing(() -> styles);
        when(player.getUniqueId()).thenReturn(id);
        when(styles.styleOf(id)).thenReturn(ChatStyle.DEFAULT.withPing(MentionPing.BELL));
        assertThat(ping.state(player)).isEqualTo("Bell");
    }

    @Test
    void choosingStoresThePingInTheStyleKeepingTheLookAndPlaysIt() {
        Preference ping = ChatPreferences.mentionPing(() -> styles);
        when(player.getUniqueId()).thenReturn(id);
        ChatStyle before = ChatStyle.DEFAULT.withDecoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true);
        when(styles.styleOf(id)).thenReturn(before);

        ping.choose().accept(player, MentionPing.HARP.ordinal());

        verify(styles).set(id, before.withPing(MentionPing.HARP));
        ArgumentCaptor<Sound> heard = ArgumentCaptor.forClass(Sound.class);
        verify(player).playSound(heard.capture());
        assertThat(heard.getValue().name().asString()).isEqualTo("minecraft:" + MentionPing.HARP.sound());
    }

    @Test
    void onlyWhoHoldsTheStylePermissionIsOffered() {
        Preference ping = ChatPreferences.mentionPing(() -> styles);
        when(player.hasPermission(PermissionNodes.STYLE)).thenReturn(false);
        assertThat(ping.shown().test(player)).isFalse();
        when(player.hasPermission(PermissionNodes.STYLE)).thenReturn(true);
        assertThat(ping.shown().test(player)).isTrue();
        verify(player, org.mockito.Mockito.atLeastOnce()).hasPermission(any(String.class));
    }
}
