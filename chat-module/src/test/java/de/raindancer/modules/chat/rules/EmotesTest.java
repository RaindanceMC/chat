package de.raindancer.modules.chat.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmotesTest {

    @Test
    @DisplayName(":heart: and friends become their symbol, the rest of the line stays as typed")
    void replaces() {
        assertThat(Emotes.apply("gg :heart: see you :star:")).isEqualTo("gg ❤ see you ★");
        assertThat(Emotes.apply(":shrug:")).isEqualTo("¯\\_(ツ)_/¯");
    }

    @Test
    @DisplayName("an unknown or half-typed code is left alone")
    void unknownStays() {
        assertThat(Emotes.apply(":nope: :heart")).isEqualTo(":nope: :heart");
        assertThat(Emotes.apply("12:30 :) ok")).isEqualTo("12:30 :) ok");
    }

    @Test
    @DisplayName("capital letters count too: :HEART: is a heart")
    void ignoresCase() {
        assertThat(Emotes.apply(":HEART:")).isEqualTo("❤");
    }

    @Test
    @DisplayName("every code is listed for the help page, short enough for one chat line each")
    void listed() {
        assertThat(Emotes.all()).containsKey(":heart:").hasSizeGreaterThanOrEqualTo(12);
        assertThat(Emotes.all().values()).allSatisfy(symbol -> assertThat(symbol.length()).isLessThanOrEqualTo(12));
    }
}
