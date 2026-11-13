package de.raindancer.modules.chat.rules;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** {@code :heart:} and friends typed in chat, turned into their symbol — a Prime perk ({@code chat.emotes}). */
public final class Emotes {

    private static final Map<String, String> CODES = new LinkedHashMap<>();
    private static final Pattern CODE = Pattern.compile(":[a-zA-Z]+:");

    static {
        CODES.put(":heart:", "❤");
        CODES.put(":star:", "★");
        CODES.put(":sparkle:", "✦");
        CODES.put(":smile:", "☺");
        CODES.put(":sun:", "☀");
        CODES.put(":moon:", "☾");
        CODES.put(":cloud:", "☁");
        CODES.put(":snow:", "❄");
        CODES.put(":music:", "♫");
        CODES.put(":flower:", "✿");
        CODES.put(":crown:", "♛");
        CODES.put(":bolt:", "⚡");
        CODES.put(":check:", "✔");
        CODES.put(":cross:", "✘");
        CODES.put(":arrow:", "➜");
        CODES.put(":peace:", "☮");
        CODES.put(":shrug:", "¯\\_(ツ)_/¯");
        CODES.put(":flip:", "(╯°□°)╯︵ ┻━┻");
    }

    private Emotes() {
    }

    public static String apply(String text) {
        if (text == null || text.indexOf(':') < 0) {
            return text;
        }
        Matcher found = CODE.matcher(text);
        StringBuilder out = new StringBuilder();
        while (found.find()) {
            String symbol = CODES.get(found.group().toLowerCase(Locale.ROOT));
            found.appendReplacement(out, Matcher.quoteReplacement(symbol == null ? found.group() : symbol));
        }
        found.appendTail(out);
        return out.toString();
    }

    /** Every code and its symbol, in the order the help shows them. */
    public static Map<String, String> all() {
        return java.util.Collections.unmodifiableMap(CODES);
    }
}
