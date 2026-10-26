package de.raindancer.modules.chat.support;

import de.raindancer.core.RainsCore;
import de.raindancer.core.platform.log.LogChannel;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.ui.identity.NameBadges;
import de.raindancer.modules.api.ModuleContext;
import de.raindancer.modules.chat.SupportSettings;
import de.raindancer.modules.chat.service.SupportNotes;
import de.raindancer.modules.chat.service.SupportPack;
import de.raindancer.modules.chat.service.SupportService;
import de.raindancer.modules.chat.store.SupportReminders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

/**
 * The support link as one piece: the note after some playtime, the window, the codes, the webhook on the one server
 * that receives it, the thank-you, and the ☕ badge. The module only starts it.
 */
public final class SupportFeature {

    public static final String SECRET_FILE = "bmc-webhook.secret";
    private static final String BADGE_OWNER = "chat-supporter";
    private static final Component BADGE = Component.text("☕", TextColor.color(0xC8A27A));

    private final SupportService support;
    private final SupportNotes notes;
    private final SupportStaff staff;
    private final SupportDesk desk;
    private final LogChannel log;
    private final Path secretFile;
    private volatile SupportWebhook webhook;
    private volatile String listening = "";

    private SupportFeature(SupportService support, SupportNotes notes, SupportStaff staff, SupportDesk desk,
                           LogChannel log, Path secretFile) {
        this.support = support;
        this.notes = notes;
        this.staff = staff;
        this.desk = desk;
        this.log = log;
        this.secretFile = secretFile;
    }

    public static SupportFeature start(ModuleContext context, Server server, RainsCore core) {
        var plugin = context.plugin();
        var settings = context.settings(SupportSettings.class, SupportSettings.DEFAULTS);

        var reminders = new SupportReminders(context.dataFolder());
        reminders.load();
        var remindersWatch = reminders.watch();
        context.closeWith(remindersWatch::close);
        var codes = new SupportCodes(context.dataFolder(), new SecureRandom());
        codes.load();
        var codesWatch = codes.watch();
        context.closeWith(codesWatch::close);
        var inbox = new SupportInbox(context.dataFolder());
        inbox.load();

        var support = new SupportService(settings.current(), reminders);
        var desk = new SupportDesk(core.supporters(), codes, inbox, Instant::now,
                cents -> settings.current().net(cents));
        var notes = new SupportNotes(plugin, server, core, support, codes);
        var feature = new SupportFeature[1];
        var staff = new SupportStaff(plugin, server, core,
                () -> feature[0] != null && feature[0].webhook != null ? Optional.of(desk) : Optional.empty());
        feature[0] = new SupportFeature(support, notes, staff, desk, context.log(),
                context.dataFolder().resolve(SECRET_FILE));

        SupportPack.contribute(plugin, core);
        NameBadges.register(BADGE_OWNER, "Supporter",
                player -> core.supporters().isSupporter(player) ? Optional.of(BADGE) : Optional.empty());
        context.closeWith(() -> NameBadges.unregister(BADGE_OWNER));

        feature[0].listen(settings.current());
        settings.onChange(fresh -> {
            support.settings(fresh);
            feature[0].listen(fresh);
        });
        context.closeWith(feature[0]::stopListening);

        // Once a minute, the same beat Core counts playtime on.
        var timer = Scheduling.globalTimer(plugin, 1200, 1200, task -> {
            if (notes.tick()) {
                Scheduling.async(plugin, () -> {
                    reminders.save();
                    core.supporters().save();
                });
            }
        });
        if (timer != null) {
            context.closeWith(timer::cancel);
        }
        var tidying = Scheduling.asyncTimer(plugin, 3600, 3600, task -> desk.tidy());
        if (tidying != null) {
            context.closeWith(tidying::cancel);
        }
        context.closeWith(reminders::save);
        context.closeWith(codes::save);
        return feature[0];
    }

    public SupportService support() {
        return support;
    }

    public SupportNotes notes() {
        return notes;
    }

    public SupportStaff staff() {
        return staff;
    }

    /** Opens, moves or closes the webhook port to match the settings. */
    private synchronized void listen(SupportSettings settings) {
        String wanted = settings.webhookPort() > 0 ? settings.listenOn() + ":" + settings.webhookPort() : "";
        if (wanted.equals(listening)) {
            return;
        }
        stopListening();
        if (wanted.isEmpty()) {
            return;
        }
        try {
            webhook = SupportWebhook.start(settings.listenOn(), settings.webhookPort(), this::secret, this::received);
            listening = wanted;
            log.info("Listening for Buy Me a Coffee on {}{}{}", wanted, SupportWebhook.PATH,
                    secret().isBlank() ? " — but " + secretFile + " is missing or empty, so every delivery is refused"
                            : ".");
        } catch (IOException taken) {
            log.warn("Could not listen for Buy Me a Coffee on {} ({}). Donations are not received until the port "
                    + "is free and the setting is saved again.", wanted, taken.getMessage());
        }
    }

    private synchronized void stopListening() {
        if (webhook != null) {
            webhook.stop();
            webhook = null;
        }
        listening = "";
    }

    private void received(BmcWebhook.Donation donation) {
        try {
            SupportDesk.Outcome outcome = desk.receive(donation);
            log.info("Buy Me a Coffee event {}: {}", donation.eventId(), switch (outcome) {
                case CREDITED -> "credited to the player whose code it carried";
                case WAITING -> "no known code — waiting in /support admin inbox";
                case ALREADY_HANDLED -> "a repeat, already handled";
                case MEMBERSHIP_ENDED -> "a membership ended";
                case IGNORED -> "not a support (" + donation.kind() + "), nothing changed";
            });
        } catch (RuntimeException failure) {
            log.warn("A Buy Me a Coffee event could not be handled ({}).", failure.toString());
        }
    }

    /** Read every time, so a new secret needs no restart. The environment wins, for a panel that sets it there. */
    private String secret() {
        String fromEnvironment = System.getenv("BMC_WEBHOOK_SECRET");
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment.strip();
        }
        try {
            return Files.isRegularFile(secretFile) ? Files.readString(secretFile, StandardCharsets.UTF_8).strip() : "";
        } catch (IOException unreadable) {
            return "";
        }
    }
}
