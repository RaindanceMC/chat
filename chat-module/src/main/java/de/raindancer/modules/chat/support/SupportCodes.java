package de.raindancer.modules.chat.support;

import de.raindancer.core.data.store.YamlStore;
import de.raindancer.core.platform.network.NetworkBus;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Each player's code for their coffee's message, {@code BEAN-K7M4P}: how a donation finds its player. Made the
 * first time it is shown, the same ever after. One network document, so the server that receives the webhook
 * knows codes shown on every other.
 */
public final class SupportCodes {

    private final YamlStore store;
    private final Random random;
    private final Map<UUID, String> codes = new ConcurrentHashMap<>();
    private final Map<String, UUID> owners = new ConcurrentHashMap<>();
    private final Map<UUID, String> unwritten = new ConcurrentHashMap<>();
    private volatile boolean readable = true;

    public SupportCodes(Path dataFolder, Random random) {
        this(YamlStore.network(dataFolder.resolve("support-codes.yml")), random);
    }

    public SupportCodes(YamlStore store, Random random) {
        this.store = store;
        this.random = random;
    }

    public synchronized void load() {
        Optional<YamlConfiguration> read = store.readIfReadable();
        if (read.isEmpty()) {
            readable = false;
            return;
        }
        readable = true;
        Map<UUID, String> fresh = new HashMap<>();
        ConfigurationSection players = read.get().getConfigurationSection("players");
        if (players != null) {
            for (String rawId : players.getKeys(false)) {
                try {
                    String code = players.getString(rawId, "");
                    if (BmcWebhook.codeIn(code).filter(code::equals).isPresent()) {
                        fresh.put(UUID.fromString(rawId), code);
                    }
                } catch (IllegalArgumentException handEdited) {
                    // one broken line must not cost everybody else theirs
                }
            }
        }
        fresh.putAll(unwritten);
        codes.clear();
        codes.putAll(fresh);
        owners.clear();
        fresh.forEach((who, code) -> owners.put(code, who));
    }

    /** Their code, made now if they have none. Held until {@link #save}. */
    public synchronized String codeOf(UUID player) {
        String known = codes.get(player);
        if (known != null) {
            return known;
        }
        String made;
        do {
            StringBuilder code = new StringBuilder("BEAN-");
            for (int i = 0; i < 5; i++) {
                code.append(BmcWebhook.ALPHABET.charAt(random.nextInt(BmcWebhook.ALPHABET.length())));
            }
            made = code.toString();
        } while (owners.containsKey(made));
        codes.put(player, made);
        owners.put(made, player);
        unwritten.put(player, made);
        return made;
    }

    public Optional<UUID> playerOf(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(owners.get(code.strip().toUpperCase(Locale.ROOT)));
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
        Map<UUID, String> sent = Map.copyOf(unwritten);
        boolean written = store.write(yaml -> codes.forEach((who, code) -> yaml.set("players." + who, code)));
        if (written) {
            sent.forEach(unwritten::remove);
            load();
        }
        return written;
    }

    public boolean isDirty() {
        return !unwritten.isEmpty();
    }

    public NetworkBus.Subscription watch() {
        return store.onChange(() -> {
            synchronized (this) {
                save();
                load();
            }
        });
    }
}
