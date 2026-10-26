package de.raindancer.modules.chat.support;

import de.raindancer.modules.chat.SupportSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupportFeesTest {

    @Test
    @DisplayName("what reaches the owner: the amount less the percentage and the fixed fee, never below nothing")
    void net() {
        assertThat(SupportSettings.netOf(500, "7.9", "0.30")).isEqualTo(430);
        assertThat(SupportSettings.netOf(1000, "7.9", "0.30")).isEqualTo(891);
        assertThat(SupportSettings.netOf(20, "7.9", "0.30")).isZero();
        assertThat(SupportSettings.netOf(500, "0", "0")).isEqualTo(500);
    }

    @Test
    @DisplayName("written the way people write money — a comma, a currency sign — or nonsense, which counts as no fee")
    void written() {
        assertThat(SupportSettings.netOf(1000, "5,5 %", "€0,25")).isEqualTo(920);
        assertThat(SupportSettings.netOf(1000, "lots", "")).isEqualTo(1000);
    }
}
