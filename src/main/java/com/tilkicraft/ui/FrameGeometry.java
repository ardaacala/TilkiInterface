package com.tilkicraft.ui;

/** Header spacing leaves the logo clear while the frame extends around player rows. */
final class FrameGeometry {
    static final int TAB_LOGO_HEIGHT=52, TAB_LINE_HEIGHT=9;
    static final String HEADER="{tab-frame}\n\n\n\n\n\n&6&lTILKICRAFT &8┃ &f{online}/{max}";
    static final String FOOTER="";
    static int columns(int players) { return Math.max(1,Math.min(4,(players+19)/20)); }
    static int rows(int players) { int bounded=Math.max(1,Math.min(80,players)),columns=columns(bounded);return (bounded+columns-1)/columns; }
    private FrameGeometry() {}
}
