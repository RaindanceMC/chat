package de.raindancer.modules.chat;

import de.raindancer.core.data.settings.Describe;
import de.raindancer.core.data.settings.In;
import de.raindancer.core.data.settings.Key;
import de.raindancer.core.data.settings.Range;
import de.raindancer.core.data.settings.Settings;
import de.raindancer.core.data.settings.Title;
import de.raindancer.core.data.settings.Topic;
import org.bukkit.Material;

/**
 * A support link — Buy Me a Coffee, Ko-fi, Patreon — in a short note to each player once they have played a while,
 * and on {@code /support}. A record of its own, like
 * {@link NetworkChatSettings}: a separate decision.
 */
@Settings(id = "chat-support", topics = {
        @Topic(path = "chat/support", title = "Support link", icon = Material.EMERALD),
})
public record SupportSettings(

        @In("chat/support") @Title("Send players a support note")
        @Describe("Once somebody has played long enough, they alone get a short note with a button to the link "
                + "below — never everybody at once. /support shows it any time. Nothing is sent while the link is empty.")
        @Key("support.enabled")
        boolean enabled,

        @In("chat/support") @Title("Link")
        @Describe("Where the button goes, starting with https://, e.g. https://buymeacoffee.com/yourname.")
        @Key("support.link")
        String link,

        @In("chat/support") @Title("Button text")
        @Describe("What the button says, e.g. Buy me a coffee or Support on Ko-fi.")
        @Key("support.button")
        String button,

        @In("chat/support") @Title("Minutes played before the note") @Range(min = 10, max = 100000)
        @Describe("Playtime across the whole network, not since joining today. Somebody who is away gets it when "
                + "they are back.")
        @Key("support.after-minutes")
        int afterMinutes,

        @In("chat/support") @Title("Hours played before the next note") @Range(min = 0, max = 10000)
        @Describe("More playtime after the last note before another. Zero: everybody gets it once, ever.")
        @Key("support.again-after-hours")
        int againAfterHours,

        @In("chat/support") @Title("Signed by")
        @Describe("Your name under the note in the window, e.g. your Minecraft name. Empty: unsigned.")
        @Key("support.signed")
        String signed,

        @In("chat/support") @Title("Thank supporters with a badge and a trail")
        @Describe("The window shows each player a code for their coffee's message; a coffee with it gives them a "
                + "☕ badge by their name and the coffee-bean trail in /cosmetics. Needs one server receiving the "
                + "webhook (below). Only ever cosmetic: anything that helps in the game would break Minecraft's "
                + "usage guidelines.")
        @Key("support.rewards")
        boolean rewards,

        @In("chat/support") @Title("Receive Buy Me a Coffee webhooks on port") @Range(min = 0, max = 65535)
        @Describe("On ONE server of the network: the port Buy Me a Coffee's webhook is passed to, at /bmc. Zero: "
                + "this server does not listen. The secret goes in plugins/RainsChat/bmc-webhook.secret, never "
                + "here. A donation with a player's /support code in its message makes them a supporter "
                + "everywhere; one without waits for /support admin inbox. Stored: who supported, when and how "
                + "many coffees; a donation nobody claims is forgotten after 90 days. Say so in your privacy notice.")
        @Key("support.webhook-port")
        int webhookPort,

        @In("chat/support") @Title("Listen on address")
        @Describe("Which address the webhook port is opened on. 0.0.0.0 is every address; 127.0.0.1 only this "
                + "machine, for a proxy on the same host.")
        @Key("support.webhook-host")
        String webhookHost,

        @In("chat/support") @Title("Fees in percent")
        @Describe("What Buy Me a Coffee and the card company keep of every payment, in percent — 5 % platform and "
                + "2.9 % card by default. Only used to estimate what reached you, e.g. for skins per euro.")
        @Key("support.fee-percent")
        String feePercent,

        @In("chat/support") @Title("Fixed fee per payment")
        @Describe("Taken from every payment on top of the percentage, e.g. 0.30.")
        @Key("support.fee-fixed")
        String feeFixed) {

    public static final String FEE_PERCENT = "7.9";
    public static final String FEE_FIXED = "0.30";

    /** Every setting from before the fees: the default fees. */
    public SupportSettings(boolean enabled, String link, String button, int afterMinutes, int againAfterHours,
                           String signed, boolean rewards, int webhookPort, String webhookHost) {
        this(enabled, link, button, afterMinutes, againAfterHours, signed, rewards, webhookPort, webhookHost,
                FEE_PERCENT, FEE_FIXED);
    }

    /** What of {@code cents} reaches the owner. */
    public long net(long cents) {
        return netOf(cents, feePercent, feeFixed);
    }

    public static long netOf(long cents, String percent, String fixed) {
        double share = number(percent);
        long fixedCents = Math.round(number(fixed) * 100);
        return Math.max(0, cents - Math.round(cents * share / 100) - fixedCents);
    }

    private static double number(String written) {
        if (written == null) {
            return 0;
        }
        String cleaned = written.replace(',', '.').replaceAll("[^0-9.]", "");
        try {
            return cleaned.isEmpty() ? 0 : Math.max(0, Double.parseDouble(cleaned));
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }

    /** Every setting from before the webhook: not listening. */
    public SupportSettings(boolean enabled, String link, String button, int afterMinutes, int againAfterHours,
                           String signed) {
        this(enabled, link, button, afterMinutes, againAfterHours, signed, false, 0, "0.0.0.0");
    }

    /** Every setting from before the signature, unsigned. */
    public SupportSettings(boolean enabled, String link, String button, int afterMinutes, int againAfterHours) {
        this(enabled, link, button, afterMinutes, againAfterHours, "", false, 0, "0.0.0.0");
    }

    public static final String BUTTON = "Buy me a coffee";

    public static final SupportSettings DEFAULTS = new SupportSettings(false, "", BUTTON, 180, 0, "", false, 0, "0.0.0.0",
            FEE_PERCENT, FEE_FIXED);

    public long remindAfterMinutes() {
        return Math.max(10, Math.min(100000, afterMinutes));
    }

    /** Zero: never again. */
    public long remindAgainMinutes() {
        return Math.max(0, Math.min(10000, againAfterHours)) * 60L;
    }

    public String listenOn() {
        return webhookHost == null || webhookHost.isBlank() ? "0.0.0.0" : webhookHost.strip();
    }
}
