package de.raindancer.modules.chat.support;

import de.raindancer.core.data.store.YamlStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * The webhook's own book, kept by the one server that receives it: which events were already handled (so a retry
 * is not counted twice), and donations that came without a code, for staff to give to the right player.
 *
 * <p>A supporter's name beside an amount is personal data, so a donation nobody assigns is forgotten after
 * {@link #KEPT}; only its event id stays, which says nothing about anybody.
 */
public final class SupportInbox {

    public static final Duration KEPT = Duration.ofDays(90);
    private static final int REMEMBERED_EVENTS = 1000;

    public record Waiting(String eventId, Instant at, String supporterName, int coffees, String amount) {
    }

    private final YamlStore store;
    private final LinkedHashSet<String> seen = new LinkedHashSet<>();
    private final List<Waiting> waiting = new ArrayList<>();

    public SupportInbox(Path dataFolder) {
        this(new YamlStore(dataFolder.resolve("support-inbox.yml")));
    }

    public SupportInbox(YamlStore store) {
        this.store = store;
    }

    public synchronized void load() {
        Optional<YamlConfiguration> read = store.readIfReadable();
        if (read.isEmpty()) {
            return;
        }
        seen.clear();
        seen.addAll(read.get().getStringList("seen"));
        waiting.clear();
        ConfigurationSection list = read.get().getConfigurationSection("waiting");
        if (list != null) {
            for (String id : list.getKeys(false)) {
                ConfigurationSection one = list.getConfigurationSection(id);
                if (one == null) {
                    continue;
                }
                try {
                    waiting.add(new Waiting(one.getString("event", id), Instant.parse(one.getString("at", "")),
                            one.getString("name", ""), one.getInt("coffees", 1), one.getString("amount", "")));
                } catch (DateTimeParseException handEdited) {
                    // skipped, not fatal
                }
            }
        }
        waiting.sort(Comparator.comparing(Waiting::at));
    }

    /** True the first time an event id is seen, false for a retry of it. */
    public synchronized boolean firstTime(String eventId) {
        if (!seen.add(eventId)) {
            return false;
        }
        while (seen.size() > REMEMBERED_EVENTS) {
            seen.remove(seen.iterator().next());
        }
        return true;
    }

    public synchronized void add(BmcWebhook.Donation donation, Instant at) {
        waiting.add(new Waiting(donation.eventId(), at, donation.supporterName(), donation.coffees(),
                donation.amount()));
    }

    public synchronized List<Waiting> waiting() {
        return List.copyOf(waiting);
    }

    /** Takes the {@code number}-th waiting donation (from 1) out of the inbox. */
    public synchronized Optional<Waiting> take(int number) {
        if (number < 1 || number > waiting.size()) {
            return Optional.empty();
        }
        return Optional.of(waiting.remove(number - 1));
    }

    /** @return how many were forgotten */
    public synchronized int forgetOlderThan(Instant now) {
        int before = waiting.size();
        waiting.removeIf(one -> one.at().plus(KEPT).isBefore(now));
        return before - waiting.size();
    }

    public synchronized boolean save() {
        return store.write(yaml -> {
            yaml.set("seen", List.copyOf(seen));
            yaml.set("waiting", null);
            for (int i = 0; i < waiting.size(); i++) {
                Waiting one = waiting.get(i);
                String base = "waiting." + i;
                yaml.set(base + ".event", one.eventId());
                yaml.set(base + ".at", one.at().toString());
                yaml.set(base + ".name", one.supporterName());
                yaml.set(base + ".coffees", one.coffees());
                yaml.set(base + ".amount", one.amount());
            }
        });
    }
}
