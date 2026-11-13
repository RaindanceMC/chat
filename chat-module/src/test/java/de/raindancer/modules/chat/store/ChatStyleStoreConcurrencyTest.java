package de.raindancer.modules.chat.store;

import de.raindancer.modules.chat.model.ChatStyle;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** A choice is never undone by a reload that happens to run while it is being made. */
class ChatStyleStoreConcurrencyTest {

    private static final ChatStyle GOLD = new ChatStyle(NamedTextColor.GOLD, true, false, false, false);
    private static final ChatStyle RED = new ChatStyle(NamedTextColor.RED, false, true, false, false);

    @TempDir
    Path folder;

    /**
     * Another server writes while a click is half through: the change listener reloads right there, on
     * its own thread, exactly as the network bus does. The reload gets its chance as soon as it can run;
     * a store that lets it in mid-choice drops the choice, one that does not holds it back until the
     * choice is made.
     */
    private static void reloadWhileChoosing(ChatStyleStore store) {
        store.duringSet = () -> {
            Thread reload = new Thread(store::load, "network-reload");
            reload.start();
            try {
                reload.join(500);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        };
    }

    @Test
    @Timeout(30)
    @DisplayName("a choice is kept when a reload runs in the middle of making it")
    void reloadInTheMiddleOfAChoice() throws Exception {
        ChatStyleStore store = new ChatStyleStore(folder);
        UUID tom = UUID.randomUUID();
        reloadWhileChoosing(store);

        store.set(tom, GOLD);

        assertThat(store.of(tom)).as("in memory").isEqualTo(GOLD);
        ChatStyleStore reopened = new ChatStyleStore(folder);
        reopened.load();
        assertThat(reopened.of(tom)).as("in the document").isEqualTo(GOLD);
    }

    @Test
    @Timeout(30)
    @DisplayName("the next choice of a player stands, when a reload runs in the middle of making it")
    void reloadInTheMiddleOfAChange() throws Exception {
        ChatStyleStore store = new ChatStyleStore(folder);
        UUID tom = UUID.randomUUID();
        store.set(tom, GOLD);
        reloadWhileChoosing(store);

        store.set(tom, RED);

        assertThat(store.of(tom)).isEqualTo(RED);
        ChatStyleStore reopened = new ChatStyleStore(folder);
        reopened.load();
        assertThat(reopened.of(tom)).isEqualTo(RED);
    }

    @Test
    @Timeout(30)
    @DisplayName("clearing a choice stands, when a reload runs in the middle of it")
    void reloadInTheMiddleOfClearing() throws Exception {
        ChatStyleStore store = new ChatStyleStore(folder);
        UUID tom = UUID.randomUUID();
        store.set(tom, GOLD);
        reloadWhileChoosing(store);

        store.set(tom, ChatStyle.DEFAULT);

        assertThat(store.of(tom)).isEqualTo(ChatStyle.DEFAULT);
        ChatStyleStore reopened = new ChatStyleStore(folder);
        reopened.load();
        assertThat(reopened.of(tom)).isEqualTo(ChatStyle.DEFAULT);
    }
}
