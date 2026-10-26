package de.raindancer.modules.chat.command;

import de.raindancer.modules.chat.ChatServices;
import de.raindancer.modules.chat.service.SupportNotes;
import de.raindancer.modules.chat.service.SupportService;
import de.raindancer.modules.chat.util.PermissionNodes;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/** {@code /support} — the support window for a player, the link as a line for the console. */
public final class SupportCommand implements IChatCommand {

    private final Supplier<ChatServices> services;
    private final Supplier<SupportService> support;
    private final Supplier<SupportNotes> window;

    public SupportCommand(Supplier<ChatServices> services, Supplier<SupportService> support,
                          Supplier<SupportNotes> window) {
        this.services = services;
        this.support = support;
        this.window = window;
    }

    @Override
    public String describe() {
        return "opens the support window";
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        ChatServices live = services.get();
        SupportNotes notes = window.get();
        if (source.getSender() instanceof Player player && notes != null) {
            if (!notes.open(player)) {
                live.messages().send(player, "chat.support.none");
            }
            return;
        }
        support.get().render(live.messages(), "chat.support.asked").ifPresentOrElse(
                line -> source.getSender().sendMessage(line),
                () -> live.messages().send(source.getSender(), "chat.support.none"));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        return List.of();
    }

    @Override
    public String permission() {
        return PermissionNodes.SUPPORT;
    }
}
