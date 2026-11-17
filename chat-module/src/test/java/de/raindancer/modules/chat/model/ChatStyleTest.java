package de.raindancer.modules.chat.model;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatStyleTest {

    @Test
    @DisplayName("DEFAULT is its own default")
    void defaultIsDefault() {
        assertThat(ChatStyle.DEFAULT.isDefault()).isTrue();
        assertThat(ChatStyle.DEFAULT.color()).isNull();
    }

    @Test
    @DisplayName("choosing a colour is no longer the default")
    void withColorIsNotDefault() {
        ChatStyle gold = ChatStyle.DEFAULT.withColor(NamedTextColor.GOLD);

        assertThat(gold.isDefault()).isFalse();
        assertThat(gold.color()).isEqualTo(NamedTextColor.GOLD);
    }

    @Test
    @DisplayName("colour null clears it back to unchosen")
    void withNullColorClears() {
        ChatStyle cleared = ChatStyle.DEFAULT.withColor(NamedTextColor.GOLD).withColor(null);

        assertThat(cleared.isDefault()).isTrue();
    }

    @Test
    @DisplayName("withDecoration flips exactly the one asked for")
    void withDecorationFlipsOnlyThatOne() {
        ChatStyle bold = ChatStyle.DEFAULT.withDecoration(TextDecoration.BOLD, true);

        assertThat(bold.bold()).isTrue();
        assertThat(bold.italic()).isFalse();
        assertThat(bold.underlined()).isFalse();
        assertThat(bold.strikethrough()).isFalse();
        assertThat(bold.has(TextDecoration.BOLD)).isTrue();
        assertThat(bold.has(TextDecoration.ITALIC)).isFalse();
    }

    @Test
    @DisplayName("obfuscated is not offered — asking for it changes nothing")
    void obfuscatedIsIgnored() {
        ChatStyle unchanged = ChatStyle.DEFAULT.withDecoration(TextDecoration.OBFUSCATED, true);

        assertThat(unchanged).isEqualTo(ChatStyle.DEFAULT);
        assertThat(unchanged.has(TextDecoration.OBFUSCATED)).isFalse();
    }

    @Test
    @DisplayName("asStyle only ever sets decorations to true, never to false")
    void styleNeverSetsFalse() {
        ChatStyle bold = ChatStyle.DEFAULT.withDecoration(TextDecoration.BOLD, true);

        assertThat(bold.asStyle().decoration(TextDecoration.BOLD))
                .isEqualTo(TextDecoration.State.TRUE);
        assertThat(bold.asStyle().decoration(TextDecoration.ITALIC))
                .as("unset, not explicitly false — a child's own italic must be free to win")
                .isEqualTo(TextDecoration.State.NOT_SET);
    }

    @Test
    @DisplayName("asStyle carries the chosen colour")
    void styleCarriesColor() {
        ChatStyle gold = ChatStyle.DEFAULT.withColor(NamedTextColor.GOLD);

        assertThat(gold.asStyle().color()).isEqualTo(NamedTextColor.GOLD);
    }

    @Test
    @DisplayName("DEFAULT's style sets nothing at all")
    void defaultStyleIsEmpty() {
        assertThat(ChatStyle.DEFAULT.asStyle().color()).isNull();
        assertThat(ChatStyle.DEFAULT.asStyle().decorations().values())
                .allMatch(state -> state == TextDecoration.State.NOT_SET);
    }

    @Test
    @DisplayName("plain means no colour and no decoration; the mention ping is a preference, not a look")
    void plainIgnoresPing() {
        assertThat(ChatStyle.DEFAULT.looksPlain()).isTrue();
        assertThat(ChatStyle.DEFAULT.withPing(MentionPing.BELL).looksPlain()).isTrue();
        assertThat(ChatStyle.DEFAULT.withColor(NamedTextColor.GOLD).looksPlain()).isFalse();
        assertThat(ChatStyle.DEFAULT.withDecoration(TextDecoration.ITALIC, true).looksPlain()).isFalse();
    }

    @Test
    @DisplayName("taking the look off keeps the mention ping")
    void plainKeepsPing() {
        ChatStyle worn = new ChatStyle(NamedTextColor.GOLD, true, true, true, true, MentionPing.BELL);
        ChatStyle plain = worn.plain();
        assertThat(plain.looksPlain()).isTrue();
        assertThat(plain.ping()).isEqualTo(MentionPing.BELL);
    }
}
