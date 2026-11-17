package de.raindancer.modules.chat.store;

import de.raindancer.modules.chat.model.ChatStyle;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChatStyleStoreWriteTest {

    @TempDir
    Path folder;

    @Test
    @DisplayName("a choice is worn at once but written by the writer it was given, not on the clicking thread")
    void writtenByTheWriter() {
        List<Runnable> queued = new ArrayList<>();
        ChatStyleStore store = new ChatStyleStore(folder);
        store.load();
        store.writer(queued::add);
        UUID who = UUID.randomUUID();
        ChatStyle gold = ChatStyle.DEFAULT.withColor(NamedTextColor.GOLD);

        store.set(who, gold);

        assertThat(store.of(who)).isEqualTo(gold);
        assertThat(fresh().of(who)).as("nothing written on the clicking thread").isEqualTo(ChatStyle.DEFAULT);
        assertThat(queued).hasSize(1);

        queued.forEach(Runnable::run);
        assertThat(fresh().of(who)).isEqualTo(gold);
    }

    private ChatStyleStore fresh() {
        ChatStyleStore again = new ChatStyleStore(folder);
        again.load();
        return again;
    }
}
