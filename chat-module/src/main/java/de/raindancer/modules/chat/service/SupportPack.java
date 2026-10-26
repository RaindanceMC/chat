package de.raindancer.modules.chat.service;

import de.raindancer.core.RainsCore;
import de.raindancer.core.content.pack.JarPack;
import de.raindancer.core.platform.log.Log;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** The support window's art — the painted header and the steaming mug — offered to Core's resource packs. */
public final class SupportPack {

    public static final String NAMESPACE = "rainschat";
    private static final String ROOT = "/de/raindancer/modules/chat/pack/";
    private static final String OWNER = "Chat";
    private static final String NAME = "support";

    private SupportPack() {
    }

    public static void contribute(Plugin plugin, RainsCore core) {
        try {
            JarPack.offer(plugin, core.resourcePacks(), SupportPack.class, ROOT, files(), OWNER, NAME,
                    "The support window's header and coffee mug");
        } catch (IOException failure) {
            Log.of("chat").warn("The support window's art could not be written ({}); the window still opens, "
                    + "without its header.", failure.getMessage());
        }
    }

    public static void withdraw(RainsCore core) {
        core.resourcePacks().withdraw(OWNER.toLowerCase(Locale.ROOT) + ":" + NAME);
    }

    /** The mug for a player wearing the pack; cocoa beans for anybody else, who would see a missing model. */
    public static ItemStack mug(boolean wearingPack) {
        ItemStack stack = new ItemStack(Material.COCOA_BEANS);
        if (wearingPack) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setItemModel(new NamespacedKey(NAMESPACE, "coffee"));
                stack.setItemMeta(meta);
            }
        }
        return stack;
    }

    static List<String> files() {
        try (InputStream in = SupportPack.class.getResourceAsStream(ROOT + "files.txt")) {
            if (in == null) {
                return List.of();
            }
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                    .map(String::trim).filter(line -> !line.isEmpty()).toList();
        } catch (IOException unreadable) {
            return List.of();
        }
    }
}
