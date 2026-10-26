package de.raindancer.modules.chat.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/** The listener over real HTTP, on a free port: only a signed POST to the path reaches the handler. */
class SupportWebhookTest {

    private static final String SECRET = "another-test-secret";
    private static final String BODY = "{\"type\":\"donation.created\",\"event_id\":7,"
            + "\"data\":{\"supporter_name\":\"Alex\",\"coffee_count\":1,\"message\":\"BEAN-ACD34\"}}";

    private final List<BmcWebhook.Donation> received = new CopyOnWriteArrayList<>();
    private final HttpClient http = HttpClient.newHttpClient();
    private SupportWebhook webhook;

    @AfterEach
    void stop() {
        if (webhook != null) {
            webhook.stop();
        }
    }

    private int start(String secret) throws Exception {
        webhook = SupportWebhook.start("127.0.0.1", 0, () -> secret, received::add);
        return webhook.port();
    }

    private static String sign(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private HttpResponse<String> post(int port, String path, String body, String header, String signature)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (signature != null) {
            request.header(header, signature);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("a signed donation is answered 200 and handed on")
    void signed() throws Exception {
        int port = start(SECRET);

        assertThat(post(port, "/bmc", BODY, "x-signature-sha256", sign(BODY)).statusCode()).isEqualTo(200);
        assertThat(post(port, "/bmc", BODY, "X-Bmc-Signature", sign(BODY)).statusCode()).isEqualTo(200);

        assertThat(received).hasSize(2);
        assertThat(received.getFirst().code()).contains("BEAN-ACD34");
    }

    @Test
    @DisplayName("a wrong or missing signature is 401, and nothing is handed on")
    void unsigned() throws Exception {
        int port = start(SECRET);

        assertThat(post(port, "/bmc", BODY, "x-signature-sha256", sign(BODY + "x")).statusCode()).isEqualTo(401);
        assertThat(post(port, "/bmc", BODY, "x-signature-sha256", null).statusCode()).isEqualTo(401);
        assertThat(received).isEmpty();
    }

    @Test
    @DisplayName("without a secret set up every request is refused, signed or not")
    void noSecret() throws Exception {
        int port = start("");

        assertThat(post(port, "/bmc", BODY, "x-signature-sha256", sign(BODY)).statusCode()).isEqualTo(503);
        assertThat(received).isEmpty();
    }

    @Test
    @DisplayName("only POST to /bmc: anything else is 404 or 405; a huge body is 413")
    void onlyThePath() throws Exception {
        int port = start(SECRET);

        assertThat(post(port, "/other", BODY, "x-signature-sha256", sign(BODY)).statusCode()).isEqualTo(404);
        HttpResponse<String> get = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/bmc"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(get.statusCode()).isEqualTo(405);
        String huge = "{\"x\":\"" + "a".repeat(300_000) + "\"}";
        assertThat(post(port, "/bmc", huge, "x-signature-sha256", sign(huge)).statusCode()).isEqualTo(413);
        assertThat(received).isEmpty();
    }

    @Test
    @DisplayName("signed but not a Buy Me a Coffee event: 200, so it is not retried, and nothing handed on")
    void notAnEvent() throws Exception {
        int port = start(SECRET);
        String other = "{\"hello\":1}";

        assertThat(post(port, "/bmc", other, "x-signature-sha256", sign(other)).statusCode()).isEqualTo(200);
        assertThat(received).isEmpty();
    }
}
