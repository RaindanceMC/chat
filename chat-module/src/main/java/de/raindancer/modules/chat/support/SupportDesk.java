package de.raindancer.modules.chat.support;

import de.raindancer.core.social.support.Supporters;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * What a donation does: with a player's code, they become a supporter; without one, it waits for staff. Each event
 * counts once. Called on the webhook's own thread, so it writes straight away — nothing here touches the world.
 */
public final class SupportDesk {

    public enum Outcome { CREDITED, WAITING, ALREADY_HANDLED, IGNORED }

    private final Supporters supporters;
    private final SupportCodes codes;
    private final SupportInbox inbox;
    private final Supplier<Instant> clock;

    public SupportDesk(Supporters supporters, SupportCodes codes, SupportInbox inbox, Supplier<Instant> clock) {
        this.supporters = supporters;
        this.codes = codes;
        this.inbox = inbox;
        this.clock = clock;
    }

    public synchronized Outcome receive(BmcWebhook.Donation donation) {
        if (!inbox.firstTime(donation.eventId())) {
            return Outcome.ALREADY_HANDLED;
        }
        Outcome outcome;
        if (donation.kind() != BmcWebhook.Kind.SUPPORT) {
            outcome = Outcome.IGNORED;
        } else {
            Optional<UUID> player = donation.code().flatMap(codes::playerOf);
            if (player.isPresent()) {
                supporters.credit(player.get(), donation.coffees(), clock.get());
                supporters.save();
                outcome = Outcome.CREDITED;
            } else {
                inbox.add(donation, clock.get());
                outcome = Outcome.WAITING;
            }
        }
        inbox.save();
        return outcome;
    }

    /** Gives the {@code number}-th waiting donation to {@code player}. Disk work: call off the server's threads. */
    public synchronized Optional<SupportInbox.Waiting> assign(int number, UUID player) {
        Optional<SupportInbox.Waiting> taken = inbox.take(number);
        taken.ifPresent(waiting -> {
            supporters.credit(player, waiting.coffees(), clock.get());
            supporters.save();
            inbox.save();
        });
        return taken;
    }

    public synchronized java.util.List<SupportInbox.Waiting> waiting() {
        return inbox.waiting();
    }

    public synchronized Optional<SupportInbox.Waiting> dismiss(int number) {
        Optional<SupportInbox.Waiting> taken = inbox.take(number);
        taken.ifPresent(waiting -> inbox.save());
        return taken;
    }

    /** Forgets unassigned donations past {@link SupportInbox#KEPT}. */
    public synchronized void tidy() {
        if (inbox.forgetOlderThan(clock.get()) > 0) {
            inbox.save();
        }
    }
}
