package de.raindancer.modules.chat.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SupportInboxTest {

    @TempDir
    Path folder;

    private final Instant now = Instant.parse("2026-10-26T12:00:00Z");

    private SupportInbox inbox() {
        SupportInbox inbox = new SupportInbox(folder);
        inbox.load();
        return inbox;
    }

    private static BmcWebhook.Donation donation(String id, String name) {
        return new BmcWebhook.Donation(BmcWebhook.Kind.SUPPORT, id, name, 1, "5 EUR", 500, "", java.util.Optional.empty(),
                "", false);
    }

    @Test
    @DisplayName("an event is handled once: a retry of the same one is recognised, also after a restart")
    void onceOnly() {
        SupportInbox inbox = inbox();

        assertThat(inbox.firstTime("991")).isTrue();
        assertThat(inbox.firstTime("991")).isFalse();
        inbox.save();
        assertThat(inbox().firstTime("991")).isFalse();
        assertThat(inbox().firstTime("992")).isTrue();
    }

    @Test
    @DisplayName("donations without a code wait in order, numbered, until assigned or dismissed")
    void waiting() {
        SupportInbox inbox = inbox();
        inbox.add(donation("1", "Alex"), now);
        inbox.add(donation("2", "Sam"), now.plusSeconds(60));

        assertThat(inbox.waiting()).extracting(SupportInbox.Waiting::supporterName).containsExactly("Alex", "Sam");
        var first = inbox.take(1).orElseThrow();
        assertThat(first.supporterName()).isEqualTo("Alex");
        assertThat(first.cents()).isEqualTo(500);
        assertThat(inbox.waiting()).extracting(SupportInbox.Waiting::supporterName).containsExactly("Sam");
        assertThat(inbox.take(5)).isEmpty();
        inbox.save();
        assertThat(inbox().waiting()).extracting(SupportInbox.Waiting::supporterName).containsExactly("Sam");
    }

    @Test
    @DisplayName("an unassigned donation is forgotten after 90 days — a supporter's name is personal data")
    void forgotten() {
        SupportInbox inbox = inbox();
        inbox.add(donation("1", "Alex"), now);
        inbox.add(donation("2", "Sam"), now.plus(Duration.ofDays(60)));

        assertThat(inbox.forgetOlderThan(now.plus(Duration.ofDays(91)))).isEqualTo(1);
        assertThat(inbox.waiting()).extracting(SupportInbox.Waiting::supporterName).containsExactly("Sam");
    }
}
