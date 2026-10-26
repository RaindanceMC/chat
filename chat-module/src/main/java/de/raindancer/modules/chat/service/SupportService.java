package de.raindancer.modules.chat.service;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.chat.SupportSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * When the support link is due in chat again. Only the timing and the link — who it is sent to is the module's.
 */
public final class SupportService {

    private static final Pattern WEB_LINK = Pattern.compile("https?://\\S+");

    private final LongSupplier clock;
    private volatile SupportSettings settings;
    private volatile long lastPosted;

    public SupportService(SupportSettings settings, LongSupplier clock) {
        this.clock = clock;
        this.settings = settings;
        this.lastPosted = clock.getAsLong();
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

    /**
     * {@code key} with its {@code <button>} filled in; empty while there is no usable link. The button is built
     * here rather than written as a click tag in messages.yml: a placeholder inside a tag's argument leaks as raw
     * markup wherever the message is rendered without it.
     */
    public Optional<Component> render(Messages messages, String key) {
        return link().map(link -> messages.get(key, "button", button(link), "link", link));
    }

    private Component button(String link) {
        return Component.text(buttonText(), NamedTextColor.AQUA)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(link))
                .hoverEvent(HoverEvent.showText(Component.text("Opens " + link, NamedTextColor.GRAY)));
    }

    /** Whether to post now; a yes counts as posted, so the next comes one whole interval later. */
    public synchronized boolean due(int online) {
        SupportSettings live = settings;
        if (!live.enabled() || link().isEmpty() || online < live.fewest()) {
            return false;
        }
        long now = clock.getAsLong();
        if (now - lastPosted < live.interval().toMillis()) {
            return false;
        }
        lastPosted = now;
        return true;
    }
}
