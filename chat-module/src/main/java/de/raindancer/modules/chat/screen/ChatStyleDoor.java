package de.raindancer.modules.chat.screen;

import de.raindancer.core.ui.cosmetic.CosmeticDoor;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.modules.chat.ChatServices;
import de.raindancer.modules.chat.model.ChatStyle;
import de.raindancer.modules.chat.service.ChatStyleService;
import de.raindancer.modules.chat.util.PermissionNodes;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.function.Supplier;

/** The chat colour as a door on the server's cosmetics page, beside everything else worn. */
public final class ChatStyleDoor implements CosmeticDoor {

    private final Supplier<ChatServices> services;
    private final ChatStyleService styles;

    public ChatStyleDoor(Supplier<ChatServices> services, ChatStyleService styles) {
        this.services = services;
        this.styles = styles;
    }

    @Override
    public String id() {
        return "chat:colour";
    }

    @Override
    public ItemStack icon(Player viewer) {
        ChatStyle style = styles.styleOf(viewer.getUniqueId());
        return Icons.of(Material.WRITABLE_BOOK, "<white>Chat colour",
                "<gray>A colour and decorations for", "<gray>your own chat messages.",
                style.looksPlain() ? "<gray>Yours: plain." : "<gray>Yours: " + ChatStyleMenu.preview(style, "like this"),
                "", "<dark_gray>Click to pick one.");
    }

    @Override
    public Optional<String> locked(Player viewer) {
        return viewer.hasPermission(PermissionNodes.STYLE) ? Optional.empty() : Optional.of("Not open to you here");
    }

    @Override
    public void open(Player viewer, Menu parent) {
        new ChatStyleMenu(services.get(), viewer, parent).open();
    }

    @Override
    public boolean wearing(Player viewer) {
        return !styles.styleOf(viewer.getUniqueId()).looksPlain();
    }

    @Override
    public void takeOff(Player viewer) {
        styles.set(viewer.getUniqueId(), styles.styleOf(viewer.getUniqueId()).plain());
    }
}
