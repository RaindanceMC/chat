package de.raindancer.modules.chat.service;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.chat.ChatModule;
import de.raindancer.modules.chat.SupportSettings;
import de.raindancer.modules.chat.store.SupportReminders;
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

    private SupportService support(SupportSettings settings) {
        SupportReminders reminders = new SupportReminders(folder);
        reminders.load();
        return new SupportService(settings, reminders);
    }

    @ParameterizedTest
    @ValueSource(strings = {"chat.support.asked"})
    @DisplayName("/support's answer at the console carries a button that opens the link")
    void buttonOpensTheLink(String key) {
        for (Messages.Tone tone : Messages.Tone.values()) {
            Component note = support(new SupportSettings(true, LINK, "Buy me a coffee", 180, 0))
                    .render(messages(tone), key, "player", "Steve").orElseThrow();

            assertThat(clicks(note)).as(tone + " " + key)
                    .containsExactly(ClickEvent.openUrl(LINK));
            String plain = PlainTextComponentSerializer.plainText().serialize(note);
            assertThat(plain).contains("Buy me a coffee").doesNotContain("<link>", "<button>", "<player>", "<click");
        }
    }

    @Test
    @DisplayName("a button text with markup in it is shown as typed, not run")
    void buttonTextIsText() {
        SupportService sneaky = support(new SupportSettings(true, LINK, "<click:run_command:'/op me'>x", 180, 0));

        Component note = sneaky.render(messages(Messages.Tone.values()[0]), "chat.support.asked", "player", "Steve").orElseThrow();

        assertThat(clicks(note)).containsExactly(ClickEvent.openUrl(LINK));
        assertThat(PlainTextComponentSerializer.plainText().serialize(note)).contains("<click:run_command");
    }

    @Test
    @DisplayName("without a usable link there is nothing to render")
    void noLink() {
        SupportService none = support(SupportSettings.DEFAULTS);

        assertThat(none.render(messages(Messages.Tone.values()[0]), "chat.support.asked")).isEmpty();
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
