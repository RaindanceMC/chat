package de.raindancer.modules.chat.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SupportCodesTest {

    @TempDir
    Path folder;

    private SupportCodes codes(Random random) {
        SupportCodes codes = new SupportCodes(folder, random);
        codes.load();
        return codes;
    }

    @Test
    @DisplayName("a player's code is made once and then always the same, also after a restart")
    void stable() {
        UUID player = UUID.randomUUID();
        SupportCodes codes = codes(new Random(1));

        String code = codes.codeOf(player);
        codes.save();

        assertThat(code).matches("BEAN-[ACDEFHJKLMNPRTUVWXY34679]{5}");
        assertThat(codes.codeOf(player)).isEqualTo(code);
        assertThat(codes(new Random(2)).codeOf(player)).isEqualTo(code);
    }

    @Test
    @DisplayName("a code leads back to its player, in any case; an unknown code to nobody")
    void lookup() {
        UUID player = UUID.randomUUID();
        SupportCodes codes = codes(new Random(3));
        String code = codes.codeOf(player);

        assertThat(codes.playerOf(code)).contains(player);
        assertThat(codes.playerOf(code.toLowerCase())).contains(player);
        assertThat(codes.playerOf("BEAN-AAAAA")).isEmpty();
    }

    @Test
    @DisplayName("no two players get the same code, even with a random source that repeats itself")
    void unique() {
        SupportCodes codes = codes(new Random(4) {
            private int calls;

            @Override
            public int nextInt(int bound) {
                // the first player's whole code comes up again for the second before anything new
                return calls++ < 10 ? 0 : super.nextInt(bound);
            }
        });
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            assertThat(seen.add(codes.codeOf(UUID.randomUUID()))).isTrue();
        }
    }
}
