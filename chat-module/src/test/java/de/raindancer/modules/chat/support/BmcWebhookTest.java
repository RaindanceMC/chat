package de.raindancer.modules.chat.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BmcWebhookTest {

    private static final String SECRET = "test-secret-not-the-real-one";

    private static String sign(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private static final String CURRENT = """
            {"type":"donation.created","live_mode":true,"attempt":1,"created":1761472800,"event_id":991,
             "data":{"id":5521,"amount":5,"object":"payment","status":"succeeded","message":"Love the server! bean-k7m4p",
                     "currency":"EUR","refunded":"false","created_at":1761472800,"note_hidden":"false",
                     "supporter_name":"Alex","supporter_id":77,"supporter_email":"alex@example.com","coffee_count":2}}
            """;

    private static final String CLASSIC = """
            {"response":{"supporter_email":"sam@example.com","supporter_name":"Sam","number_of_coffees":"3",
                         "total_amount":"15","support_created_on":"2021-08-01 16:00:00","support_note":"for the server",
                         "support_id":42}}
            """;

    @Test
    @DisplayName("a body signed with the secret is accepted, in either header Buy Me a Coffee has used")
    void signed() throws Exception {
        assertThat(BmcWebhook.verify(CURRENT.getBytes(StandardCharsets.UTF_8), sign(CURRENT, SECRET), SECRET)).isTrue();
        assertThat(BmcWebhook.verify(CURRENT.getBytes(StandardCharsets.UTF_8), sign(CURRENT, SECRET).toUpperCase(),
                SECRET)).as("hex in capitals").isTrue();
        assertThat(BmcWebhook.verify(CURRENT.getBytes(StandardCharsets.UTF_8), "sha256=" + sign(CURRENT, SECRET),
                SECRET)).as("with a scheme in front").isTrue();
    }

    @Test
    @DisplayName("a wrong, missing or empty signature, or no secret at all, is refused")
    void unsigned() throws Exception {
        byte[] body = CURRENT.getBytes(StandardCharsets.UTF_8);
        assertThat(BmcWebhook.verify(body, sign(CURRENT, "another-secret"), SECRET)).isFalse();
        assertThat(BmcWebhook.verify(body, sign(CURRENT + " ", SECRET), SECRET)).as("body changed").isFalse();
        assertThat(BmcWebhook.verify(body, null, SECRET)).isFalse();
        assertThat(BmcWebhook.verify(body, "", SECRET)).isFalse();
        assertThat(BmcWebhook.verify(body, sign(CURRENT, "x"), "")).as("no secret set up").isFalse();
        assertThat(BmcWebhook.verify(body, sign(CURRENT, SECRET), null)).isFalse();
    }

    @Test
    @DisplayName("today's payload: who, how many coffees, the message and its code")
    void current() {
        BmcWebhook.Donation donation = BmcWebhook.read(CURRENT, null).orElseThrow();

        assertThat(donation.kind()).isEqualTo(BmcWebhook.Kind.SUPPORT);
        assertThat(donation.eventId()).isEqualTo("991");
        assertThat(donation.supporterName()).isEqualTo("Alex");
        assertThat(donation.coffees()).isEqualTo(2);
        assertThat(donation.amount()).isEqualTo("5 EUR");
        assertThat(donation.cents()).isEqualTo(500);
        assertThat(donation.code()).contains("BEAN-K7M4P");
    }

    @Test
    @DisplayName("the older payload under \"response\", its event named in a header")
    void classic() {
        BmcWebhook.Donation donation = BmcWebhook.read(CLASSIC, "coffee_purchase").orElseThrow();

        assertThat(donation.kind()).isEqualTo(BmcWebhook.Kind.SUPPORT);
        assertThat(donation.eventId()).isEqualTo("42");
        assertThat(donation.supporterName()).isEqualTo("Sam");
        assertThat(donation.coffees()).isEqualTo(3);
        assertThat(donation.amount()).isEqualTo("15");
        assertThat(donation.cents()).isEqualTo(1500);
        assertThat(donation.code()).isEmpty();
    }

    @Test
    @DisplayName("memberships and extras count as support; a membership's end is told apart; refunds are not support")
    void kinds() {
        assertThat(kindOf("membership.started")).isEqualTo(BmcWebhook.Kind.SUPPORT);
        assertThat(kindOf("extra_purchase.created")).isEqualTo(BmcWebhook.Kind.SUPPORT);
        assertThat(kindOf("donation.refunded")).isEqualTo(BmcWebhook.Kind.REFUND);
        assertThat(kindOf("membership.cancelled")).isEqualTo(BmcWebhook.Kind.MEMBERSHIP_ENDED);
        assertThat(kindOf("membership.ended")).isEqualTo(BmcWebhook.Kind.MEMBERSHIP_ENDED);
        assertThat(kindOf("membership.updated")).isEqualTo(BmcWebhook.Kind.OTHER);
        assertThat(BmcWebhook.read(CURRENT.replace("donation.created", "membership.started"), null).orElseThrow()
                .membership()).isTrue();
        assertThat(BmcWebhook.read(CURRENT, null).orElseThrow().membership()).isFalse();
        assertThat(BmcWebhook.read(CURRENT, null).orElseThrow().supporterId()).isEqualTo("77");
    }

    private static BmcWebhook.Kind kindOf(String type) {
        return BmcWebhook.read(CURRENT.replace("donation.created", type), null).orElseThrow().kind();
    }

    @Test
    @DisplayName("the code is found anywhere in the message, in any case, and only a real-looking one")
    void codes() {
        assertThat(BmcWebhook.codeIn("keep going! BEAN-ACD34")).contains("BEAN-ACD34");
        assertThat(BmcWebhook.codeIn("(bean-acd34)")).contains("BEAN-ACD34");
        assertThat(BmcWebhook.codeIn("Bean - ACD34")).as("spaces typed around the dash").contains("BEAN-ACD34");
        assertThat(BmcWebhook.codeIn("BEAN-ACD3")).isEmpty();
        assertThat(BmcWebhook.codeIn("JELLYBEAN-ACD345")).isEmpty();
        assertThat(BmcWebhook.codeIn(null)).isEmpty();
    }

    @Test
    @DisplayName("amounts with cents, as text or as numbers, become whole cents; nonsense becomes none")
    void cents() {
        assertThat(BmcWebhook.centsOf("4.5")).isEqualTo(450);
        assertThat(BmcWebhook.centsOf("12.99")).isEqualTo(1299);
        assertThat(BmcWebhook.centsOf("3")).isEqualTo(300);
        assertThat(BmcWebhook.centsOf("")).isZero();
        assertThat(BmcWebhook.centsOf("free")).isZero();
        assertThat(BmcWebhook.centsOf("-5")).isZero();
    }

    @Test
    @DisplayName("an event without an id gets one from its body, so a retry is still recognised")
    void idFromBody() {
        String noId = CURRENT.replace("\"event_id\":991,", "").replace("\"id\":5521,", "");

        Optional<BmcWebhook.Donation> once = BmcWebhook.read(noId, null);
        Optional<BmcWebhook.Donation> again = BmcWebhook.read(noId, null);

        assertThat(once.orElseThrow().eventId()).isNotBlank().isEqualTo(again.orElseThrow().eventId());
    }

    @Test
    @DisplayName("something that is not JSON, or not a Buy Me a Coffee event, is nothing")
    void garbage() {
        assertThat(BmcWebhook.read("not json", null)).isEmpty();
        assertThat(BmcWebhook.read("[1,2]", null)).isEmpty();
        assertThat(BmcWebhook.read("{\"hello\":1}", null)).isEmpty();
    }
}
