package de.raindancer.modules.chat.service;

import de.raindancer.core.ui.messages.Messages;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.Optional;

/**
 * The words of the support window, rendered for one player — everything {@link SupportWindow} puts on screen, kept
 * apart from Paper's dialog API so it can be checked without a server.
 *
 * @param header    the painted header, only for a player wearing the server's pack; anybody else would see a box
 * @param signature the owner's name under the note, when they set one
 * @param reward    the thank-you on offer and the player's code for it — or, for a supporter, thanks for theirs
 * @param copy      the button copying the code; {@code code} is what it copies
 */
public record SupportLetter(Component title, Optional<Component> header, Component greeting, Component text,
                           Optional<Component> signature, Component footer, Component buy, Component buyTooltip,
                           Component later, Optional<Component> reward, Optional<Component> copy,
                           Optional<String> code, Messages messages) {

    /** The chat module's font in the pack; {@link #HEADER_GLYPH} is the painted header (art/support_art.py). */
    public static final Key FONT = Key.key(SupportPack.NAMESPACE, "support");
    public static final String HEADER_GLYPH = "";

    public static SupportLetter of(Messages messages, String player, String buttonText, String link, String signed,
                                   boolean wearingPack) {
        Optional<Component> header = wearingPack
                ? Optional.of(Component.text(HEADER_GLYPH).font(FONT))
                : Optional.empty();
        return new SupportLetter(
                messages.get("chat.support.window.title"),
                header,
                messages.get("chat.support.window.greeting", "player", player),
                messages.get("chat.support.window.text"),
                signed == null || signed.isBlank()
                        ? Optional.empty()
                        : Optional.of(messages.get("chat.support.window.signature", "name", signed.strip())),
                messages.get("chat.support.window.footer"),
                messages.get("chat.support.window.buy", "button", buttonText),
                messages.get("chat.support.window.buy-tooltip", "link", link),
                messages.get("chat.support.window.later"),
                Optional.empty(), Optional.empty(), Optional.empty(), messages);
    }

    public Component copyTooltip() {
        return messages.get("chat.support.window.copy-tooltip");
    }

    /** For somebody who has not supported yet: what a coffee with {@code code} in its message brings. */
    public SupportLetter forCode(String code) {
        return new SupportLetter(title, header, greeting, text, signature, footer, buy, buyTooltip, later,
                Optional.of(messages.get("chat.support.window.code", "code", code)),
                Optional.of(messages.get("chat.support.window.copy", "code", code)), Optional.of(code), messages);
    }

    /** For a supporter: thanks for theirs, and no code to copy. */
    public SupportLetter forSupporter() {
        return new SupportLetter(title, header, greeting, text, signature, footer, buy, buyTooltip, later,
                Optional.of(messages.get("chat.support.window.supporter")), Optional.empty(), Optional.empty(),
                messages);
    }

    /**
     * The whole letter as chat lines, for somebody who never opened the window: no painted header (chat has no room
     * for it) and the buy button as a link.
     */
    public Component inChat(String link) {
        Component nl = Component.newline();
        Component button = buy.clickEvent(ClickEvent.openUrl(link)).hoverEvent(HoverEvent.showText(buyTooltip));
        Component chat = Component.text().append(messages.get("chat.support.follow-up")).append(nl).append(nl)
                .append(greeting).append(nl).append(text).build();
        if (reward.isPresent()) {
            chat = chat.append(nl).append(reward.get());
        }
        chat = chat.append(nl).append(button);
        if (signature.isPresent()) {
            chat = chat.append(nl).append(signature.get());
        }
        return chat.append(nl).append(footer);
    }

    /** Said in chat once a coffee with their code has arrived. */
    public static Component thanks(Messages messages, String player) {
        return messages.get("chat.support.thanks", "player", player);
    }

    /** The one line in chat: there is a message, and {@code open} to read it. */
    public static Component notice(Messages messages, Component open) {
        return messages.get("chat.support.notice", "open", open);
    }
}
