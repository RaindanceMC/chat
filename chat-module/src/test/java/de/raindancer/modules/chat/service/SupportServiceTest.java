package de.raindancer.modules.chat.service;

import de.raindancer.modules.chat.SupportSettings;
import de.raindancer.modules.chat.store.SupportReminders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SupportServiceTest {

    private static final String LINK = "https://buymeacoffee.com/Raindancer118";

    @TempDir
    Path folder;

    private SupportReminders reminders;
    private final UUID player = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reminders = new SupportReminders(folder);
        reminders.load();
    }

    private SupportService service(SupportSettings settings) {
        return new SupportService(settings, reminders);
    }

    private static SupportSettings on(int afterMinutes, int againHours) {
        return new SupportSettings(true, LINK, "Buy me a coffee", afterMinutes, againHours);
    }

    @Test
    @DisplayName("with default settings nobody ever gets the note and there is no link")
    void offByDefault() {
        SupportService support = service(SupportSettings.DEFAULTS);

        assertThat(support.link()).isEmpty();
        assertThat(support.isDue(player, 100_000)).isFalse();
    }

    @Test
    @DisplayName("switched on without a link nobody gets an empty note")
    void onWithoutLink() {
        SupportService support = service(new SupportSettings(true, " ", "x", 60, 0));

        assertThat(support.isDue(player, 600)).isFalse();
    }

    @Test
    @DisplayName("a player gets the note once they have played the set time, not before")
    void afterPlaytime() {
        SupportService support = service(on(180, 0));

        assertThat(support.isDue(player, 179)).isFalse();
        assertThat(support.isDue(player, 180)).isTrue();
    }

    @Test
    @DisplayName("once, and never again while repeating is off")
    void onlyOnce() {
        SupportService support = service(on(180, 0));

        support.reminded(player, 180);
        assertThat(support.isDue(player, 181)).isFalse();
        assertThat(support.isDue(player, 100_000)).isFalse();
    }

    @Test
    @DisplayName("each player gets their own note: one player's is not another's")
    void perPlayer() {
        SupportService support = service(on(180, 0));
        UUID other = UUID.randomUUID();

        support.reminded(player, 200);

        assertThat(support.isDue(other, 200)).isTrue();
        assertThat(support.isDue(other, 100)).isFalse();
    }

    @Test
    @DisplayName("with repeating on, the next note waits that much more playtime")
    void askAgain() {
        SupportService support = service(on(180, 10));

        support.reminded(player, 200);
        assertThat(support.isDue(player, 200 + 599)).isFalse();
        assertThat(support.isDue(player, 200 + 600)).isTrue();
    }

    @Test
    @DisplayName("who got it survives a restart, so nobody gets it twice")
    void survivesRestart() {
        service(on(180, 0)).reminded(player, 180);
        reminders.save();

        SupportReminders reread = new SupportReminders(folder);
        reread.load();

        assertThat(new SupportService(on(180, 0), reread).isDue(player, 5000)).isFalse();
        assertThat(reread.remindedAt(player)).hasValue(180);
    }

    @Test
    @DisplayName("only http and https addresses are used")
    void linkIsChecked() {
        assertThat(service(on(30, 0)).link()).contains(LINK);
        assertThat(service(new SupportSettings(true, "  " + LINK + " ", "x", 30, 0)).link()).contains(LINK);
        assertThat(service(new SupportSettings(true, "buymeacoffee.com/Raindancer118", "x", 30, 0)).link())
                .as("no scheme").isEmpty();
        assertThat(service(new SupportSettings(true, "javascript:alert(1)", "x", 30, 0)).link()).isEmpty();
        assertThat(service(new SupportSettings(true, "https://a.b/x y", "x", 30, 0)).link()).isEmpty();
    }

    @Test
    @DisplayName("hand-written numbers out of range are clamped")
    void clamped() {
        SupportService support = service(on(0, -5));

        assertThat(support.isDue(player, 9)).isFalse();
        assertThat(support.isDue(player, 10)).isTrue();
        support.reminded(player, 10);
        assertThat(support.isDue(player, 100_000)).as("negative means never again").isFalse();
    }

    @Test
    @DisplayName("new settings take effect without a restart")
    void settingsChange() {
        SupportService support = service(SupportSettings.DEFAULTS);
        assertThat(support.isDue(player, 500)).isFalse();

        support.settings(on(180, 0));
        assertThat(support.isDue(player, 500)).isTrue();
    }

    @Test
    @DisplayName("the button text falls back to a default when the owner left it empty")
    void buttonText() {
        assertThat(service(on(30, 0)).buttonText()).isEqualTo("Buy me a coffee");
        assertThat(service(new SupportSettings(true, LINK, "  ", 30, 0)).buttonText())
                .isEqualTo(SupportSettings.BUTTON);
    }
}
