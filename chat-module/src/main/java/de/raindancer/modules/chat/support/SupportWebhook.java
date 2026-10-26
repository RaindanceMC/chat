package de.raindancer.modules.chat.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The address Buy Me a Coffee posts to: {@code POST /bmc}, signed with the webhook's secret. Everything else is
 * turned away before anything is read. A signed event is answered at once and handed to {@code handler} on this
 * listener's own thread — the handler hops onto the server's threads itself if it needs to.
 */
public final class SupportWebhook {

    public static final String PATH = "/bmc";
    private static final int MOST_BYTES = 256 * 1024;

    private final HttpServer server;
    private final ExecutorService threads;

    private SupportWebhook(HttpServer server, ExecutorService threads) {
        this.server = server;
        this.threads = threads;
    }

    /** @param secret read on every request, so a changed secret needs no restart; blank refuses everything */
    public static SupportWebhook start(String host, int port, Supplier<String> secret,
                                       Consumer<BmcWebhook.Donation> handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 16);
        ExecutorService threads = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "RainsChat support webhook");
            thread.setDaemon(true);
            return thread;
        });
        server.createContext("/", exchange -> handle(exchange, secret, handler));
        server.setExecutor(threads);
        server.start();
        return new SupportWebhook(server, threads);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public void stop() {
        server.stop(0);
        threads.shutdownNow();
    }

    private static void handle(HttpExchange exchange, Supplier<String> secret, Consumer<BmcWebhook.Donation> handler)
            throws IOException {
        try (exchange) {
            if (!PATH.equals(exchange.getRequestURI().getPath())) {
                answer(exchange, 404, "not here");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                answer(exchange, 405, "POST only");
                return;
            }
            String key = secret.get();
            if (key == null || key.isBlank()) {
                answer(exchange, 503, "no secret set up");
                return;
            }
            byte[] body;
            try (InputStream in = exchange.getRequestBody()) {
                body = in.readNBytes(MOST_BYTES + 1);
            }
            if (body.length > MOST_BYTES) {
                answer(exchange, 413, "too big");
                return;
            }
            String signature = exchange.getRequestHeaders().getFirst("x-signature-sha256");
            if (signature == null) {
                signature = exchange.getRequestHeaders().getFirst("x-bmc-signature");
            }
            if (!BmcWebhook.verify(body, signature, key)) {
                answer(exchange, 401, "bad signature");
                return;
            }
            var donation = BmcWebhook.read(new String(body, StandardCharsets.UTF_8),
                    exchange.getRequestHeaders().getFirst("x-bmc-event"));
            answer(exchange, 200, "thanks");
            donation.ifPresent(handler);
        }
    }

    private static void answer(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
