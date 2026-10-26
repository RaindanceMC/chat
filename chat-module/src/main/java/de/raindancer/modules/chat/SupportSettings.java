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
 * A support link — Buy Me a Coffee, Ko-fi, Patreon — dropped into chat every now and then, and {@code /support}
 * for anybody who wants it right away. A record of its own, like {@link NetworkChatSettings}: a separate decision.
 */
@Settings(id = "chat-support", topics = {
        @Topic(path = "chat/support", title = "Support link", icon = Material.EMERALD),
})
public record SupportSettings(

        @In("chat/support") @Title("Post a support link now and then")
        @Describe("Every so often, everybody online sees a short note with a button to the link below. /support "
                + "shows it any time. Nothing is posted while the link is empty.")
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

        @In("chat/support") @Title("Minutes between reminders") @Range(min = 5, max = 1440)
        @Describe("How long between two notes. The first comes this long after the server starts.")
        @Key("support.every-minutes")
        int everyMinutes,

        @In("chat/support") @Title("Fewest players online") @Range(min = 1, max = 100)
        @Describe("A note is held back until at least this many are online, then posted.")
        @Key("support.fewest-players")
        int fewestPlayers) {

    public static final String BUTTON = "Buy me a coffee";

    public static final SupportSettings DEFAULTS = new SupportSettings(false, "", BUTTON, 60, 1);

    public java.time.Duration interval() {
        return java.time.Duration.ofMinutes(Math.max(5, Math.min(1440, everyMinutes)));
    }

    public int fewest() {
        return Math.max(1, Math.min(100, fewestPlayers));
    }
}
