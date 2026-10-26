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
 * Who has had the support note, and at how many minutes of playtime; and whose note still waits to be read, since
 * which minute ({@value #DONE} once it was opened or followed up).
 *
 * <pre>
 * players:
 *   &lt;uuid&gt;: 180
 * follow-up:
 *   &lt;uuid&gt;: 180
 * </pre>
 * One network document, like {@link ChatStyleStore} and for the same reasons: had on one server is had on all, a
 * reload keeps what this server has not written yet, and a document that cannot be read is never written over.
 */
public final class SupportReminders {

    private final YamlStore store;
    private final Map<UUID, Long> remindedAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> unwritten = new ConcurrentHashMap<>();
    /** Never removed, only set to {@link #DONE}: a removal would come back from another server's older copy. */
    public static final long DONE = -1;
    private final Map<UUID, Long> followUps = new ConcurrentHashMap<>();
    private final Map<UUID, Long> unwrittenFollowUps = new ConcurrentHashMap<>();
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
        Map<UUID, Long> fresh = new HashMap<>(read(read.get().getConfigurationSection("players")));
        fresh.putAll(unwritten);
        remindedAt.keySet().retainAll(fresh.keySet());
        remindedAt.putAll(fresh);
        Map<UUID, Long> pending = new HashMap<>(read(read.get().getConfigurationSection("follow-up")));
        pending.putAll(unwrittenFollowUps);
        followUps.keySet().retainAll(pending.keySet());
        followUps.putAll(pending);
    }

    private static Map<UUID, Long> read(ConfigurationSection section) {
        Map<UUID, Long> read = new HashMap<>();
        if (section != null) {
            for (String rawId : section.getKeys(false)) {
                try {
                    read.put(UUID.fromString(rawId), section.getLong(rawId));
                } catch (IllegalArgumentException notAnId) {
                    // one hand-edited line must not cost everybody else theirs
                }
            }
        }
        return read;
    }

    /** Since which playtime minute their note waits to be read; empty when it does not (any more). */
    public OptionalLong followUpFrom(UUID who) {
        Long from = followUps.get(who);
        return from == null || from < 0 ? OptionalLong.empty() : OptionalLong.of(from);
    }

    public void followUpFrom(UUID who, long playedMinutes) {
        followUps.put(who, playedMinutes);
        unwrittenFollowUps.put(who, playedMinutes);
    }

    /** Read, or followed up: nothing more to send. */
    public void followUpDone(UUID who) {
        if (followUps.containsKey(who) && followUps.get(who) != DONE) {
            followUpFrom(who, DONE);
        }
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

    /** Something waits for {@link #save}. */
    public boolean isDirty() {
        return !unwritten.isEmpty() || !unwrittenFollowUps.isEmpty();
    }

    public synchronized boolean save() {
        if (!readable) {
            load();
            if (!readable) {
                return false;
            }
        }
        if (unwritten.isEmpty() && unwrittenFollowUps.isEmpty()) {
            return true;
        }
        Map<UUID, Long> sent = Map.copyOf(unwritten);
        Map<UUID, Long> sentFollowUps = Map.copyOf(unwrittenFollowUps);
        boolean written = store.write(yaml -> {
            remindedAt.forEach((who, at) -> yaml.set("players." + who, at));
            followUps.forEach((who, from) -> yaml.set("follow-up." + who, from));
        });
        if (written) {
            sent.forEach(unwritten::remove);
            sentFollowUps.forEach(unwrittenFollowUps::remove);
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
