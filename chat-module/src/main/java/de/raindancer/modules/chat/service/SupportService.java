package de.raindancer.modules.chat.service;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.chat.SupportSettings;
import de.raindancer.modules.chat.store.SupportReminders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Who is due the support note: each player once they have played a while, never everybody at once. Who is online
 * and how long they have played are the module's.
 */
public final class SupportService {

    private static final Pattern WEB_LINK = Pattern.compile("https?://\\S+");

    private final SupportReminders reminders;
    private volatile SupportSettings settings;

    public SupportService(SupportSettings settings, SupportReminders reminders) {
        this.settings = settings;
        this.reminders = reminders;
    }

    public void settings(SupportSettings fresh) {
        this.settings = fresh;
    }

    /** The owner's link, when it is a web address. */
    public Optional<String> link() {
        String written = settings.link();
        if (written == null) {
            return Optional.empty();
        }
        String link = written.strip();
        return WEB_LINK.matcher(link).matches() ? Optional.of(link) : Optional.empty();
    }

    public String buttonText() {
        String written = settings.button();
        return written == null || written.isBlank() ? SupportSettings.BUTTON : written.strip();
    }

    public SupportSettings current() {
        return settings;
    }

    public String signed() {
        return settings.signed();
    }

    public boolean isDue(UUID who, long playedMinutes) {
        SupportSettings live = settings;
        if (!live.enabled() || link().isEmpty() || playedMinutes < live.remindAfterMinutes()) {
            return false;
        }
        OptionalLong before = reminders.remindedAt(who);
        if (before.isEmpty()) {
            return true;
        }
        long again = live.remindAgainMinutes();
        return again > 0 && playedMinutes - before.getAsLong() >= again;
    }

    public void reminded(UUID who, long playedMinutes) {
        reminders.set(who, playedMinutes);
        reminders.followUpFrom(who, playedMinutes);
    }

    /** Whether their note has gone unread long enough for the whole letter to follow in chat. */
    public boolean isFollowUpDue(UUID who, long playedMinutes) {
        SupportSettings live = settings;
        long after = live.followUpAfterMinutes();
        if (!live.enabled() || link().isEmpty() || after == 0) {
            return false;
        }
        OptionalLong from = reminders.followUpFrom(who);
        return from.isPresent() && playedMinutes - from.getAsLong() >= after;
    }

    public void opened(UUID who) {
        reminders.followUpDone(who);
    }

    public void followedUp(UUID who) {
        reminders.followUpDone(who);
    }

    /**
     * {@code key} with its {@code <button>} filled in; empty while there is no usable link. The button is built
     * here rather than written as a click tag in messages.yml: a placeholder inside a tag's argument leaks as raw
     * markup wherever the message is rendered without it.
     */
    public Optional<Component> render(Messages messages, String key, Object... more) {
        return link().map(link -> {
            Object[] values = new Object[more.length + 4];
            values[0] = "button";
            values[1] = button(link);
            values[2] = "link";
            values[3] = link;
            System.arraycopy(more, 0, values, 4, more.length);
            return messages.get(key, values);
        });
    }

    private Component button(String link) {
        return Component.text(buttonText(), NamedTextColor.AQUA)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(link))
                .hoverEvent(HoverEvent.showText(Component.text("Opens " + link, NamedTextColor.GRAY)));
    }
}
