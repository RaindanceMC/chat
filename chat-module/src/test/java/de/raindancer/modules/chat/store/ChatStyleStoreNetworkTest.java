package de.raindancer.modules.chat.store;

import de.raindancer.core.data.sql.Database;
import de.raindancer.core.data.store.Documents;
import de.raindancer.core.data.store.YamlStore;
import de.raindancer.core.testkit.TestDatabases;
import de.raindancer.modules.chat.model.ChatStyle;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** A player's chat colour is theirs on every server of the network. */
@Tag("database")
class ChatStyleStoreNetworkTest {

    private static final ChatStyle GOLD = new ChatStyle(NamedTextColor.GOLD, true, false, false, false);
    private static final ChatStyle RED = new ChatStyle(NamedTextColor.RED, false, true, false, false);

    @TempDir
    Path plugins;

    private Servers network;
    private final UUID tom = UUID.randomUUID();

    private ChatStyleStore on(String id) {
        if (network == null) {
            network = new Servers(plugins);
        }
        ChatStyleStore store = new ChatStyleStore(network.network(network.server(id), "chatstyles.yml"));
        store.load();
        store.watch();
        return store;
    }

    @AfterEach
    void close() {
        if (network != null) {
            network.close();
        }
    }

    @Test
    @DisplayName("a colour chosen on one server is worn on the other, without anything being carried over")
    void styleFollowsThePlayer() {
        ChatStyleStore smp = on("smp");
        ChatStyleStore arena = on("arena");

        smp.set(tom, GOLD);
        network.heard();

        assertThat(arena.of(tom)).isEqualTo(GOLD);
    }

    @Test
    @DisplayName("choosing none on one server is none on the other")
    void clearingTravels() {
        ChatStyleStore smp = on("smp");
        ChatStyleStore arena = on("arena");
        smp.set(tom, GOLD);
        network.heard();

        arena.set(tom, ChatStyle.DEFAULT);
        network.heard();

        assertThat(smp.of(tom)).isEqualTo(ChatStyle.DEFAULT);
    }

    @Test
    @DisplayName("two players choosing at once on two servers both keep their choice")
    void bothChoicesKept() {
        ChatStyleStore smp = on("smp");
        ChatStyleStore arena = on("arena");
        UUID bea = UUID.randomUUID();

        smp.set(tom, GOLD);
        arena.set(bea, RED);
        network.heard();

        assertThat(smp.of(bea)).isEqualTo(RED);
        assertThat(arena.of(tom)).isEqualTo(GOLD);
    }

    @Test
    @DisplayName("two servers whose players choose at the very same time lose nobody's choice")
    void concurrentChoicesKept() throws Exception {
        ChatStyleStore smp = on("smp");
        ChatStyleStore arena = on("arena");
        java.util.List<UUID> onSmp = java.util.stream.Stream.generate(UUID::randomUUID).limit(40).toList();
        java.util.List<UUID> onArena = java.util.stream.Stream.generate(UUID::randomUUID).limit(40).toList();
        java.util.concurrent.CountDownLatch go = new java.util.concurrent.CountDownLatch(1);
        Thread first = new Thread(() -> choose(smp, onSmp, GOLD, go));
        Thread second = new Thread(() -> choose(arena, onArena, RED, go));
        first.start();
        second.start();
        go.countDown();
        first.join();
        second.join();
        network.heard();

        for (ChatStyleStore store : java.util.List.of(smp, arena)) {
            assertThat(onSmp.stream().filter(who -> !store.of(who).equals(GOLD)).toList()).isEmpty();
            assertThat(onArena.stream().filter(who -> !store.of(who).equals(RED)).toList()).isEmpty();
        }
    }

    private static void choose(ChatStyleStore store, java.util.List<UUID> players, ChatStyle style,
                               java.util.concurrent.CountDownLatch go) {
        try {
            go.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return;
        }
        players.forEach(who -> store.set(who, style));
    }

    @Test
    @DisplayName("a document that cannot be read leaves what is held alone and refuses to write over it")
    void unreadableIsNotWrittenOver() throws Exception {
        Database broken = TestDatabases.unusable(plugins.resolve("broken.db"), Documents.SCHEMA);
        try {
            Documents documents = new Documents(broken, null, plugins, "smp", System::currentTimeMillis);
            ChatStyleStore store = new ChatStyleStore(
                    new YamlStore(plugins.resolve("RainsChat").resolve("chatstyles.yml"), documents,
                            Documents.Scope.NETWORK));
            store.load();

            store.set(tom, GOLD);

            assertThat(store.isReadable()).isFalse();
            assertThat(store.of(tom)).isEqualTo(GOLD);
        } finally {
            broken.close();
        }
    }
}
