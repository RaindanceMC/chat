package de.raindancer.modules.chat.util;

import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrimeNodesTest {

    @Test
    @DisplayName("chat colour and emotes are Prime's: children of rains.prime, and nobody else's by default")
    void primeHoldsThem() {
        Map<String, Permission> known = new HashMap<>();
        PluginManager manager = mock(PluginManager.class);
        when(manager.getPermission(anyString())).thenAnswer(call -> known.get(call.<String>getArgument(0)));
        doAnswer(call -> {
            Permission added = call.getArgument(0);
            known.put(added.getName(), added);
            return null;
        }).when(manager).addPermission(any(Permission.class));

        PermissionNodes.makePrime(manager);

        assertThat(known.get("rains.prime").getChildren())
                .containsEntry(PermissionNodes.STYLE, true)
                .containsEntry(PermissionNodes.EMOTES, true);
    }
}
