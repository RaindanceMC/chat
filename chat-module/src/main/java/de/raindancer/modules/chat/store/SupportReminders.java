package de.raindancer.modules.chat.store;

import de.raindancer.core.data.store.YamlStore;
import de.raindancer.core.platform.network.NetworkBus;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who has had the support note, and at how many minutes of playtime.
 *
 * <pre>
 * players:
 *   &lt;uuid&gt;: 180
 * </pre>
 * One network document, like {@link ChatStyleStore} and for the same reasons: had on one server is had on all, a
 * reload keeps what this server has not written yet, and a document that cannot be read is never written over.
 */
public final class SupportReminders {

    private final YamlStore store;
    private final Map<UUID, Long> remindedAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> unwritten = new ConcurrentHashMap<>();
    private volatile boolean readable = true;

    public SupportReminders(Path dataFolder) {
        this(YamlStore.network(dataFolder.resolve("support-reminded.yml")));
    }

    public SupportReminders(YamlStore store) {
        this.store = store;
    }

    public synchronized void load() {
        Optional<YamlConfiguration> read = store.readIfReadable();
        if (read.isEmpty()) {
            readable = false;
            return;
        }
        readable = true;
        Map<UUID, Long> fresh = new HashMap<>();
        ConfigurationSection players = read.get().getConfigurationSection("players");
        if (players != null) {
            for (String rawId : players.getKeys(false)) {
                try {
                    fresh.put(UUID.fromString(rawId), players.getLong(rawId));
                } catch (IllegalArgumentException notAnId) {
                    // one hand-edited line must not cost everybody else theirs
                }
            }
        }
        fresh.putAll(unwritten);
        remindedAt.keySet().retainAll(fresh.keySet());
        remindedAt.putAll(fresh);
    }

    /** The playtime minute they last had the note at; empty if they never have. */
    public OptionalLong remindedAt(UUID who) {
        Long at = remindedAt.get(who);
        return at == null ? OptionalLong.empty() : OptionalLong.of(at);
    }

    /** Held until {@link #save}, which is disk work and belongs off the server's threads. */
    public void set(UUID who, long playedMinutes) {
        remindedAt.put(who, playedMinutes);
        unwritten.put(who, playedMinutes);
    }

    public synchronized boolean save() {
        if (!readable) {
            load();
            if (!readable) {
                return false;
            }
        }
        if (unwritten.isEmpty()) {
            return true;
        }
        Map<UUID, Long> sent = Map.copyOf(unwritten);
        boolean written = store.write(yaml -> remindedAt.forEach((who, at) -> yaml.set("players." + who, at)));
        if (written) {
            sent.forEach(unwritten::remove);
            load();
        }
        return written;
    }

    /** Reloads when another server writes the document, after writing what this one holds. Close to stop. */
    public NetworkBus.Subscription watch() {
        return store.onChange(() -> {
            synchronized (this) {
                save();
                load();
            }
        });
    }
}
