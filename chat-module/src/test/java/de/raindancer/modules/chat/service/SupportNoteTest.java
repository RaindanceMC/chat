package de.raindancer.modules.chat.service;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.chat.ChatModule;
import de.raindancer.modules.chat.SupportSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The bundled wording, rendered: the button has to actually open the link, in both tones. */
class SupportNoteTest {

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

    private final SupportService support = new SupportService(
            new SupportSettings(true, LINK, "Buy me a coffee", 60, 1), System::currentTimeMillis);

    @ParameterizedTest
    @ValueSource(strings = {"chat.support.note", "chat.support.asked"})
    @DisplayName("the note and /support's answer carry a button that opens the link")
    void buttonOpensTheLink(String key) {
        for (Messages.Tone tone : Messages.Tone.values()) {
            Component note = support.render(messages(tone), key).orElseThrow();

            assertThat(clicks(note)).as(tone + " " + key)
                    .containsExactly(ClickEvent.openUrl(LINK));
            String plain = PlainTextComponentSerializer.plainText().serialize(note);
            assertThat(plain).contains("Buy me a coffee").doesNotContain("<link>", "<button>", "<click");
        }
    }

    @Test
    @DisplayName("a button text with markup in it is shown as typed, not run")
    void buttonTextIsText() {
        SupportService sneaky = new SupportService(
                new SupportSettings(true, LINK, "<click:run_command:'/op me'>x", 60, 1), System::currentTimeMillis);

        Component note = sneaky.render(messages(Messages.Tone.values()[0]), "chat.support.note").orElseThrow();

        assertThat(clicks(note)).containsExactly(ClickEvent.openUrl(LINK));
        assertThat(PlainTextComponentSerializer.plainText().serialize(note)).contains("<click:run_command");
    }

    @Test
    @DisplayName("without a usable link there is nothing to render")
    void noLink() {
        SupportService none = new SupportService(SupportSettings.DEFAULTS, System::currentTimeMillis);

        assertThat(none.render(messages(Messages.Tone.values()[0]), "chat.support.note")).isEmpty();
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
}
