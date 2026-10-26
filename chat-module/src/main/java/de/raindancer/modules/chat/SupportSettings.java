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
        String signed) {

    /** Every setting from before the signature, unsigned. */
    public SupportSettings(boolean enabled, String link, String button, int afterMinutes, int againAfterHours) {
        this(enabled, link, button, afterMinutes, againAfterHours, "");
    }

    public static final String BUTTON = "Buy me a coffee";

    public static final SupportSettings DEFAULTS = new SupportSettings(false, "", BUTTON, 180, 0, "");

    public long remindAfterMinutes() {
        return Math.max(10, Math.min(100000, afterMinutes));
    }

    /** Zero: never again. */
    public long remindAgainMinutes() {
        return Math.max(0, Math.min(10000, againAfterHours)) * 60L;
    }
}
