package de.raindancer.modules.chat.command;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.chat.ChatServices;
import de.raindancer.modules.chat.ChatSettings;
import de.raindancer.modules.chat.service.FreezeService;
import de.raindancer.modules.chat.service.PrivateChatService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A wrong /chat shows what this player may actually type; the console gets say's player names completed. */
class ChatCommandUsageTest {

    private final Server server = mock(Server.class);
    private final Messages messages = mock(Messages.class);
    private final ChatServices services = new ChatServices(null, server, null, null, messages, null, null,
            () -> ChatSettings.DEFAULTS, null, null, null, new FreezeService(), null, null,
            new PrivateChatService(), null, null, null, null);
    private final ChatCommand command = new ChatCommand(() -> services);

    private CommandSourceStack as(CommandSender who) {
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(who);
        return source;
    }

    private String usageShownTo(CommandSender who, String... args) {
        command.execute(as(who), args);
        ArgumentCaptor<Object> text = ArgumentCaptor.forClass(Object.class);
        verify(messages).send(eq(who), eq("chat.usage"), eq("usage"), text.capture());
        return String.valueOf(text.getValue());
    }

    @Test
    @DisplayName("a player without staff rights is not shown the staff subcommands")
    void playerUsageHidesStaff() {
        Player player = mock(Player.class);
        when(player.hasPermission(anyString())).thenReturn(false);

        String usage = usageShownTo(player, "nonsense");

        assertThat(usage).contains("/chat").doesNotContain("freeze").doesNotContain("slowmode").doesNotContain("say");
    }

    @Test
    @DisplayName("staff see the staff subcommands in the usage")
    void staffUsageShowsThem() {
        Player staff = mock(Player.class);
        when(staff.hasPermission(anyString())).thenReturn(true);

        assertThat(usageShownTo(staff, "nonsense")).contains("clear").contains("freeze").contains("slowmode <seconds|off>");
    }

    @Test
    @DisplayName("the console is shown say, and not the player-only channel switches")
    void consoleUsage() {
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        when(console.hasPermission(anyString())).thenReturn(true);

        assertThat(usageShownTo(console, "nonsense")).contains("say <player> <message>").doesNotContain("team");
    }

    @Test
    @DisplayName("/chat say <partial> completes the names of players online")
    void sayCompletesNames() {
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        Player tom = mock(Player.class);
        when(tom.getName()).thenReturn("Tom");
        Player tina = mock(Player.class);
        when(tina.getName()).thenReturn("Tina");
        Player max = mock(Player.class);
        when(max.getName()).thenReturn("Max");
        when(server.getOnlinePlayers()).thenAnswer(call -> List.of(tom, tina, max));

        assertThat(command.suggest(as(console), new String[]{"say", "t"})).containsExactlyInAnyOrder("Tom", "Tina");
        assertThat(command.suggest(as(console), new String[]{"say", "t", "x"})).isEmpty();
    }
}
