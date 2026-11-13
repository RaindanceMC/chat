package de.raindancer.modules.chat.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MentionPingTest {

    @Test
    @DisplayName("the classic orb ping is the default, so nobody hears anything new without choosing")
    void classicByDefault() {
        assertThat(ChatStyle.DEFAULT.ping()).isEqualTo(MentionPing.CLASSIC);
        assertThat(MentionPing.CLASSIC.sound()).isEqualTo("entity.experience_orb.pickup");
    }

    @Test
    @DisplayName("clicking through goes round every sound and back to the start")
    void cycles() {
        MentionPing ping = MentionPing.CLASSIC;
        for (int i = 0; i < MentionPing.values().length; i++) {
            ping = ping.next();
        }
        assertThat(ping).isEqualTo(MentionPing.CLASSIC);
        assertThat(MentionPing.values()).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("a name nobody knows reads as the classic ping")
    void unknownName() {
        assertThat(MentionPing.named("trumpet")).isEqualTo(MentionPing.CLASSIC);
        assertThat(MentionPing.named("amethyst")).isEqualTo(MentionPing.AMETHYST);
    }

    @Test
    @DisplayName("a chosen ping alone is a choice worth keeping")
    void pingIsNotDefault() {
        assertThat(ChatStyle.DEFAULT.withPing(MentionPing.BELL).isDefault()).isFalse();
    }
}
