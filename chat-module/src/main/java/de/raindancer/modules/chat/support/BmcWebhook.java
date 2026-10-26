package de.raindancer.modules.chat.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reading what Buy Me a Coffee posts: checking it was signed with the webhook's secret, and pulling out who
 * supported, how much, and the code from {@code /support} if they wrote it in their message.
 *
 * <p>Two payload shapes are read, because both are out there: today's ({@code type} with the event and the support
 * under {@code data}) and the older one (everything under {@code response}, the event named in a header).
 */
public final class BmcWebhook {

    public enum Kind { SUPPORT, REFUND, OTHER }

    /** @param code the player's code from the message, normalised to {@code BEAN-XXXXX} */
    public record Donation(Kind kind, String eventId, String supporterName, int coffees, String amount,
                           String message, Optional<String> code) {
    }

    /** Letters and digits nobody misreads: no B/8, O/0, I/1, S/5, G, Q, Z/2. */
    public static final String ALPHABET = "ACDEFHJKLMNPRTUVWXY34679";
    private static final Pattern CODE = Pattern.compile(
            "(?<![A-Za-z0-9])BEAN\\s*-\\s*([" + ALPHABET + "]{5})(?![A-Za-z0-9])", Pattern.CASE_INSENSITIVE);

    private BmcWebhook() {
    }

    /** Whether {@code signature} (hex HMAC-SHA256 of the raw body, maybe {@code sha256=} in front) matches. */
    public static boolean verify(byte[] body, String signature, String secret) {
        if (body == null || signature == null || signature.isBlank() || secret == null || secret.isBlank()) {
            return false;
        }
        String given = signature.strip();
        int equals = given.indexOf('=');
        if (equals >= 0) {
            given = given.substring(equals + 1);
        }
        byte[] expected;
        byte[] received;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.strip().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            expected = mac.doFinal(body);
            received = HexFormat.of().parseHex(given.toLowerCase(Locale.ROOT));
        } catch (GeneralSecurityException | IllegalArgumentException unreadable) {
            return false;
        }
        return MessageDigest.isEqual(expected, received);
    }

    /** @param headerEvent the event header of the older shape, when there is one */
    public static Optional<Donation> read(String body, String headerEvent) {
        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (!parsed.isJsonObject()) {
                return Optional.empty();
            }
            root = parsed.getAsJsonObject();
        } catch (RuntimeException notJson) {
            return Optional.empty();
        }
        JsonObject data;
        String type;
        if (root.has("data") && root.get("data").isJsonObject()) {
            data = root.getAsJsonObject("data");
            type = text(root, "type");
        } else if (root.has("response") && root.get("response").isJsonObject()) {
            data = root.getAsJsonObject("response");
            type = headerEvent == null ? "coffee_purchase" : headerEvent;
        } else {
            return Optional.empty();
        }
        String message = first(data, "message", "support_note", "note");
        String id = first(root, "event_id");
        if (id.isEmpty()) {
            id = first(data, "id", "support_id", "purchase_id", "membership_id");
        }
        if (id.isEmpty()) {
            id = "body-" + sha256(body);
        }
        String currency = first(data, "currency");
        String amount = first(data, "amount", "total_amount", "support_coffee_price");
        return Optional.of(new Donation(kindOf(type), id, first(data, "supporter_name", "payer_name", "name"),
                number(first(data, "coffee_count", "number_of_coffees", "support_coffees")),
                currency.isEmpty() ? amount : (amount + " " + currency).strip(), message, codeIn(message)));
    }

    public static Optional<String> codeIn(String message) {
        if (message == null) {
            return Optional.empty();
        }
        Matcher matcher = CODE.matcher(message);
        return matcher.find() ? Optional.of("BEAN-" + matcher.group(1).toUpperCase(Locale.ROOT)) : Optional.empty();
    }

    static Kind kindOf(String type) {
        String event = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (event.contains("refund")) {
            return Kind.REFUND;
        }
        if (event.endsWith(".created") || event.endsWith(".started") || event.endsWith(".renewed")
                || event.contains("purchase") || event.contains("coffee")) {
            return Kind.SUPPORT;
        }
        return Kind.OTHER;
    }

    private static String first(JsonObject object, String... keys) {
        for (String key : keys) {
            String value = text(object, key);
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static String text(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return "";
        }
        return value.getAsString().strip();
    }

    private static int number(String text) {
        try {
            return Math.max(1, (int) Math.round(Double.parseDouble(text)));
        } catch (NumberFormatException missing) {
            return 1;
        }
    }

    private static String sha256(String body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8))).substring(0, 32);
        } catch (GeneralSecurityException impossible) {
            return Integer.toHexString(body.hashCode());
        }
    }
}
