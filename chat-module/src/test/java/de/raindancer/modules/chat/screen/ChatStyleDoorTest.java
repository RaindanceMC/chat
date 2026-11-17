package de.raindancer.modules.chat.screen;

import de.raindancer.modules.chat.model.ChatStyle;
import de.raindancer.modules.chat.model.MentionPing;
import de.raindancer.modules.chat.service.ChatStyleService;
import de.raindancer.modules.chat.store.ChatStyleStore;
import de.raindancer.modules.chat.util.PermissionNodes;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatStyleDoorTest {

    @TempDir
    Path folder;

    private Player player(boolean mayStyle) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.hasPermission(PermissionNodes.STYLE)).thenReturn(mayStyle);
        return player;
    }

    private ChatStyleService styles() {
        ChatStyleStore store = new ChatStyleStore(folder);
        store.load();
        return new ChatStyleService(store);
    }

    @Test
    @DisplayName("a coloured chat is worn; taking it off leaves plain text and the chosen ping")
    void takeOff() {
        ChatStyleService styles = styles();
        ChatStyleDoor door = new ChatStyleDoor(() -> null, styles);
        Player me = player(true);
        assertThat(door.wearing(me)).isFalse();

        styles.set(me.getUniqueId(), new ChatStyle(NamedTextColor.AQUA, true, false, false, false, MentionPing.BELL));
        assertThat(door.wearing(me)).isTrue();

        door.takeOff(me);
        assertThat(door.wearing(me)).isFalse();
        assertThat(styles.styleOf(me.getUniqueId()).ping()).isEqualTo(MentionPing.BELL);
    }

    @Test
    @DisplayName("without the style node the door is locked and says why")
    void locked() {
        ChatStyleDoor door = new ChatStyleDoor(() -> null, styles());
        assertThat(door.locked(player(true))).isEmpty();
        assertThat(door.locked(player(false))).isPresent();
        assertThat(door.id()).isEqualTo("chat:colour");
    }
}
