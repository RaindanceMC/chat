package de.raindancer.modules.chat.support;

import de.raindancer.core.data.store.Documents;
import de.raindancer.core.data.store.YamlStore;
import de.raindancer.core.social.support.Supporters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SupportDeskTest {

    @TempDir
    Path folder;

    private final Instant now = Instant.parse("2026-10-26T12:00:00Z");
    private final UUID player = UUID.randomUUID();
    private Supporters supporters;
    private SupportCodes codes;
    private SupportInbox inbox;
    private SupportDesk desk;

    @BeforeEach
    void setUp() {
        supporters = new Supporters(new YamlStore(folder.resolve("supporters.yml"), null, Documents.Scope.NETWORK));
        supporters.load();
        codes = new SupportCodes(folder, new Random(9));
        codes.load();
        inbox = new SupportInbox(folder);
        inbox.load();
        desk = new SupportDesk(supporters, codes, inbox, () -> now);
    }

    private static BmcWebhook.Donation donation(String id, BmcWebhook.Kind kind, int coffees, String code) {
        return new BmcWebhook.Donation(kind, id, "Alex", coffees, "5 EUR", "msg", Optional.ofNullable(code));
    }

    @Test
    @DisplayName("a donation with a player's code makes them a supporter, with its coffees")
    void withCode() {
        String code = codes.codeOf(player);

        assertThat(desk.receive(donation("1", BmcWebhook.Kind.SUPPORT, 3, code)))
                .isEqualTo(SupportDesk.Outcome.CREDITED);

        assertThat(supporters.of(player).orElseThrow().coffees()).isEqualTo(3);
        assertThat(inbox.waiting()).isEmpty();
    }

    @Test
    @DisplayName("the same event delivered twice counts once")
    void retry() {
        String code = codes.codeOf(player);
        desk.receive(donation("1", BmcWebhook.Kind.SUPPORT, 1, code));

        assertThat(desk.receive(donation("1", BmcWebhook.Kind.SUPPORT, 1, code)))
                .isEqualTo(SupportDesk.Outcome.ALREADY_HANDLED);
        assertThat(supporters.of(player).orElseThrow().coffees()).isEqualTo(1);
    }

    @Test
    @DisplayName("without a code, or with one nobody has, it waits in the inbox for staff")
    void withoutCode() {
        assertThat(desk.receive(donation("1", BmcWebhook.Kind.SUPPORT, 1, null))).isEqualTo(SupportDesk.Outcome.WAITING);
        assertThat(desk.receive(donation("2", BmcWebhook.Kind.SUPPORT, 1, "BEAN-AAAAA")))
                .isEqualTo(SupportDesk.Outcome.WAITING);

        assertThat(inbox.waiting()).hasSize(2);
        assertThat(supporters.all()).isEmpty();
    }

    @Test
    @DisplayName("staff give a waiting donation to a player: credited with its coffees, out of the inbox")
    void assign() {
        desk.receive(donation("1", BmcWebhook.Kind.SUPPORT, 2, null));

        assertThat(desk.assign(1, player)).isPresent();

        assertThat(supporters.of(player).orElseThrow().coffees()).isEqualTo(2);
        assertThat(inbox.waiting()).isEmpty();
        assertThat(desk.assign(1, player)).isEmpty();
    }

    @Test
    @DisplayName("a refund or anything else changes nobody's standing — staff decide that")
    void refund() {
        String code = codes.codeOf(player);
        desk.receive(donation("1", BmcWebhook.Kind.SUPPORT, 1, code));

        assertThat(desk.receive(donation("2", BmcWebhook.Kind.REFUND, 1, code))).isEqualTo(SupportDesk.Outcome.IGNORED);
        assertThat(desk.receive(donation("3", BmcWebhook.Kind.OTHER, 1, null))).isEqualTo(SupportDesk.Outcome.IGNORED);
        assertThat(supporters.isSupporter(player)).isTrue();
        assertThat(inbox.waiting()).isEmpty();
    }

    @Test
    @DisplayName("what the desk did is written, so it survives a restart")
    void saved() {
        String code = codes.codeOf(player);
        codes.save();
        desk.receive(donation("1", BmcWebhook.Kind.SUPPORT, 1, code));

        Supporters reread = new Supporters(new YamlStore(folder.resolve("supporters.yml"), null,
                Documents.Scope.NETWORK));
        reread.load();
        SupportInbox inboxAgain = new SupportInbox(folder);
        inboxAgain.load();

        assertThat(reread.isSupporter(player)).isTrue();
        assertThat(inboxAgain.firstTime("1")).isFalse();
    }
}
