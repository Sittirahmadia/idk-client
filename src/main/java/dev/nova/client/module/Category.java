package dev.nova.client.module;

public enum Category {
    COMBAT   ("Combat",   0xFF_E74C3C),
    MOVEMENT ("Movement", 0xFF_2ECC71),
    RENDER   ("Render",   0xFF_3498DB),
    MISC     ("Misc",     0xFF_9B59B6);

    public final String name;
    public final int    color;

    Category(String name, int color) {
        this.name  = name;
        this.color = color;
    }

    /** ARGB alpha component (0-255) */
    public int alpha()  { return (color >> 24) & 0xFF; }
    /** Packed RGB without alpha */
    public int rgb()    { return color & 0x00FFFFFF; }
}
