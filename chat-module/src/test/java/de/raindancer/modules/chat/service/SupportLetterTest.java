package de.raindancer.modules.chat.service;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.chat.ChatModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The support window's words and the chat line pointing at it, from the bundled wording, in both tones. */
class SupportLetterTest {

    private static final String LINK = "https://buymeacoffee.com/Raindancer118";

    @TempDir
    Path folder;

    private Messages messages(Messages.Tone tone) {
        Messages messages = new Messages(folder.resolve("messages.yml"));
        messages.defineFrom(ChatModule.class.getResourceAsStream("messages.yml"));
        messages.seriousFrom(ChatModule.class.getResourceAsStream("messages-serious.yml"));
        messages.tone(tone);
        return messages;
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    @DisplayName("every part of the window renders in both tones, with the player's name and no leftover markup")
    void windowRenders() {
        for (Messages.Tone tone : Messages.Tone.values()) {
            SupportLetter letter = SupportLetter.of(messages(tone), "Steve", "Buy me a coffee", LINK, "Raindancer118", false);

            String all = String.join("\n", plain(letter.title()), plain(letter.greeting()), plain(letter.text()),
                    plain(letter.signature().orElseThrow()), plain(letter.footer()), plain(letter.buy()), plain(letter.buyTooltip()), plain(letter.later()));
            assertThat(all).as(tone.name()).contains("Steve", "Buy me a coffee", LINK, "Raindancer118")
                    .doesNotContain("<player>", "<button>", "<link>", "<#", "<gradient", "</");
        }
    }

    @Test
    @DisplayName("the painted header is only there for a player wearing the server's pack — anybody else would see a box")
    void headerOnlyWithThePack() {
        assertThat(SupportLetter.of(messages(Messages.Tone.values()[0]), "Steve", "x", LINK, "", false).header())
                .isEmpty();

        Component header = SupportLetter.of(messages(Messages.Tone.values()[0]), "Steve", "x", LINK, "", true)
                .header().orElseThrow();
        assertThat(plain(header)).isEqualTo(SupportLetter.HEADER_GLYPH);
        assertThat(header.font()).isEqualTo(SupportLetter.FONT);
    }

    @Test
    @DisplayName("a button text with markup in it is shown as typed, not parsed")
    void buttonTextIsText() {
        SupportLetter letter = SupportLetter.of(messages(Messages.Tone.values()[0]), "Steve",
                "<click:run_command:'/op me'>x", LINK, "", false);

        assertThat(plain(letter.buy())).contains("<click:run_command:'/op me'>x");
        assertThat(clicks(letter.buy())).isEmpty();
    }

    @Test
    @DisplayName("a player's name cannot smuggle markup into the window")
    void nameIsText() {
        SupportLetter letter = SupportLetter.of(messages(Messages.Tone.values()[0]), "<red>Evil", "x", LINK, "", false);

        assertThat(plain(letter.greeting())).contains("<red>Evil");
    }

    @Test
    @DisplayName("a player who is not a supporter yet sees their code and a copy button; a supporter a thank-you")
    void codeOrThanks() {
        for (Messages.Tone tone : Messages.Tone.values()) {
            SupportLetter asking = SupportLetter.of(messages(tone), "Steve", "x", LINK, "", false)
                    .forCode("BEAN-K7M4P");
            assertThat(plain(asking.reward().orElseThrow())).as(tone.name()).contains("BEAN-K7M4P")
                    .doesNotContain("<code>", "<#");
            assertThat(plain(asking.copy().orElseThrow())).contains("BEAN-K7M4P");

            SupportLetter thanking = SupportLetter.of(messages(tone), "Steve", "x", LINK, "", false).forSupporter();
            assertThat(plain(thanking.reward().orElseThrow())).as(tone.name()).isNotBlank().doesNotContain("<#");
            assertThat(thanking.copy()).isEmpty();
        }
        assertThat(SupportLetter.of(messages(Messages.Tone.values()[0]), "Steve", "x", LINK, "", false).reward())
                .as("nothing about rewards until told which").isEmpty();
    }

    @Test
    @DisplayName("the thank-you after a coffee renders in both tones with the player's name")
    void thanks() {
        for (Messages.Tone tone : Messages.Tone.values()) {
            assertThat(plain(SupportLetter.thanks(messages(tone), "Steve"))).as(tone.name()).contains("Steve")
                    .doesNotContain("<player>", "<#");
        }
    }

    @Test
    @DisplayName("signed with the owner's name when one is set, as typed; unsigned when it is empty")
    void signature() {
        Messages messages = messages(Messages.Tone.values()[0]);

        assertThat(plain(SupportLetter.of(messages, "Steve", "x", LINK, "  Rain<b>  ", false).signature()
                .orElseThrow())).contains("Rain<b>").doesNotContain("  Rain");
        assertThat(SupportLetter.of(messages, "Steve", "x", LINK, " ", false).signature()).isEmpty();
        assertThat(SupportLetter.of(messages, "Steve", "x", LINK, null, false).signature()).isEmpty();
    }

    @Test
    @DisplayName("the chat line only says there is a message, and carries the button that opens it")
    void notice() {
        Component open = Component.text("[Read it]").clickEvent(ClickEvent.runCommand("/callback abc"));

        for (Messages.Tone tone : Messages.Tone.values()) {
            Component line = SupportLetter.notice(messages(tone), open);

            assertThat(clicks(line)).as(tone.name()).containsExactly(ClickEvent.runCommand("/callback abc"));
            assertThat(plain(line)).contains("[Read it]").doesNotContain("buymeacoffee", "<open>");
        }
    }

    private static List<ClickEvent> clicks(Component component) {
        List<ClickEvent> found = new ArrayList<>();
        collect(component, found);
        return found.stream().distinct().toList();
    }

    private static void collect(Component component, List<ClickEvent> found) {
        if (component.clickEvent() != null) {
            found.add(component.clickEvent());
        }
        component.children().forEach(child -> collect(child, found));
    }

    @Test
    @DisplayName("the follow-up in chat is the whole letter, code and signature included, with a button to the link")
    void inChat() {
        for (Messages.Tone tone : Messages.Tone.values()) {
            Component chat = SupportLetter.of(messages(tone), "Steve", "Buy me a coffee", LINK, "Raindancer118", true)
                    .forCode("BEAN-ACDEF").inChat(LINK);

            assertThat(plain(chat)).as(tone.name()).contains("Steve", "Buy me a coffee", "BEAN-ACDEF", "Raindancer118")
                    .doesNotContain(SupportLetter.HEADER_GLYPH, "<player>", "<button>", "<code>", "<#", "</");
            assertThat(clicks(chat)).as(tone.name()).containsExactly(ClickEvent.openUrl(LINK));
        }
    }

}
