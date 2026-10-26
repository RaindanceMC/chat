package de.raindancer.modules.chat.service;

import de.raindancer.modules.chat.SupportSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class SupportServiceTest {

    private static final String LINK = "https://buymeacoffee.com/Raindancer118";
    private static final long MINUTE = Duration.ofMinutes(1).toMillis();

    private final AtomicLong now = new AtomicLong(1_000_000);

    private SupportService service(SupportSettings settings) {
        return new SupportService(settings, now::get);
    }

    private static SupportSettings on(int everyMinutes, int fewestPlayers) {
        return new SupportSettings(true, LINK, "Buy me a coffee", everyMinutes, fewestPlayers);
    }

    @Test
    @DisplayName("with default settings nothing is ever posted and there is no link")
    void offByDefault() {
        SupportService support = service(SupportSettings.DEFAULTS);
        now.addAndGet(1000 * MINUTE);

        assertThat(support.link()).isEmpty();
        assertThat(support.due(50)).isFalse();
    }

    @Test
    @DisplayName("switched on without a link it stays quiet rather than posting an empty button")
    void onWithoutLink() {
        SupportService support = service(new SupportSettings(true, " ", "Buy me a coffee", 30, 1));
        now.addAndGet(60 * MINUTE);

        assertThat(support.due(10)).isFalse();
    }

    @Test
    @DisplayName("the first reminder comes one interval after start, not the moment the server boots")
    void notAtStart() {
        SupportService support = service(on(30, 1));

        assertThat(support.due(5)).isFalse();
        now.addAndGet(29 * MINUTE);
        assertThat(support.due(5)).isFalse();
        now.addAndGet(MINUTE);
        assertThat(support.due(5)).isTrue();
    }

    @Test
    @DisplayName("once posted, the next one waits a whole interval again")
    void waitsAfterPosting() {
        SupportService support = service(on(30, 1));
        now.addAndGet(30 * MINUTE);

        assertThat(support.due(5)).isTrue();
        assertThat(support.due(5)).as("asked twice in the same minute").isFalse();
        now.addAndGet(29 * MINUTE);
        assertThat(support.due(5)).isFalse();
        now.addAndGet(MINUTE);
        assertThat(support.due(5)).isTrue();
    }

    @Test
    @DisplayName("too few players online: skipped, and posted as soon as enough are on")
    void fewestPlayers() {
        SupportService support = service(on(30, 3));
        now.addAndGet(45 * MINUTE);

        assertThat(support.due(2)).isFalse();
        assertThat(support.due(0)).isFalse();
        assertThat(support.due(3)).isTrue();
    }

    @Test
    @DisplayName("an empty server does not count, even when the owner set the minimum to zero")
    void neverToNobody() {
        SupportService support = service(on(30, 0));
        now.addAndGet(45 * MINUTE);

        assertThat(support.due(0)).isFalse();
        assertThat(support.due(1)).isTrue();
    }

    @Test
    @DisplayName("only http and https addresses are used")
    void linkIsChecked() {
        assertThat(service(on(30, 1)).link()).contains(LINK);
        assertThat(service(new SupportSettings(true, "  " + LINK + " ", "x", 30, 1)).link()).contains(LINK);
        assertThat(service(new SupportSettings(true, "buymeacoffee.com/Raindancer118", "x", 30, 1)).link())
                .as("no scheme").isEmpty();
        assertThat(service(new SupportSettings(true, "javascript:alert(1)", "x", 30, 1)).link()).isEmpty();
        assertThat(service(new SupportSettings(true, "https://a.b/x y", "x", 30, 1)).link()).isEmpty();
    }

    @Test
    @DisplayName("an absurd interval is clamped, so a hand-written config cannot spam chat every tick")
    void intervalClamped() {
        SupportService support = service(on(0, 1));

        now.addAndGet(4 * MINUTE);
        assertThat(support.due(1)).isFalse();
        now.addAndGet(MINUTE);
        assertThat(support.due(1)).isTrue();
    }

    @Test
    @DisplayName("new settings take effect without restarting, and a lengthened interval is honoured")
    void settingsChange() {
        SupportService support = service(on(30, 1));
        now.addAndGet(30 * MINUTE);
        assertThat(support.due(1)).isTrue();

        support.settings(on(120, 1));
        now.addAndGet(60 * MINUTE);
        assertThat(support.due(1)).isFalse();
        now.addAndGet(60 * MINUTE);
        assertThat(support.due(1)).isTrue();

        support.settings(SupportSettings.DEFAULTS);
        now.addAndGet(1000 * MINUTE);
        assertThat(support.due(10)).isFalse();
    }

    @Test
    @DisplayName("the button text falls back to a default when the owner left it empty")
    void buttonText() {
        assertThat(service(on(30, 1)).buttonText()).isEqualTo("Buy me a coffee");
        assertThat(service(new SupportSettings(true, LINK, "  ", 30, 1)).buttonText())
                .isEqualTo(SupportSettings.BUTTON);
    }
}
