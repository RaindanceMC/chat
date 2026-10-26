package de.raindancer.modules.chat.command;

import de.raindancer.modules.chat.ChatServices;
import de.raindancer.modules.chat.service.SupportService;
import de.raindancer.modules.chat.util.PermissionNodes;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/** {@code /support} — the server's support link, whenever somebody wants it rather than waiting for the note. */
public final class SupportCommand implements IChatCommand {

    private final Supplier<ChatServices> services;
    private final Supplier<SupportService> support;

    public SupportCommand(Supplier<ChatServices> services, Supplier<SupportService> support) {
        this.services = services;
        this.support = support;
    }

    @Override
    public String describe() {
        return "shows the server's support link";
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        ChatServices live = services.get();
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
