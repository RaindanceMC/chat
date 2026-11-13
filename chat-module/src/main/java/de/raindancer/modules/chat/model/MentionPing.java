package de.raindancer.modules.chat.model;

import java.util.Locale;

/** The sound a player hears when somebody @-mentions them — their own pick, a Prime perk ({@code chat.style}). */
public enum MentionPing {

    CLASSIC("Classic", "entity.experience_orb.pickup", 0.7f, 1.4f),
    AMETHYST("Amethyst", "block.amethyst_block.chime", 1.0f, 1.2f),
    BELL("Bell", "block.note_block.bell", 0.7f, 1.5f),
    HARP("Harp", "block.note_block.harp", 0.7f, 1.8f),
    CHIME("Chime", "block.note_block.chime", 0.7f, 1.4f),
    ALLAY("Allay", "entity.allay.item_given", 0.8f, 1.2f),
    BUBBLE("Bubble", "block.bubble_column.bubble_pop", 1.0f, 1.0f);

    private final String label;
    private final String sound;
    private final float volume;
    private final float pitch;

    MentionPing(String label, String sound, float volume, float pitch) {
        this.label = label;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public String label() {
        return label;
    }

    public String sound() {
        return sound;
    }

    public float volume() {
        return volume;
    }

    public float pitch() {
        return pitch;
    }

    /** The one after this, round to the first again — what a click on the menu button does. */
    public MentionPing next() {
        MentionPing[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /** By its name in the file; anything unknown (a hand-edit, a removed sound) is the classic ping. */
    public static MentionPing named(String name) {
        if (name == null) {
            return CLASSIC;
        }
        try {
            return valueOf(name.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return CLASSIC;
        }
    }
}
