package com.tilkicraft.ui;

/** Header spacing reserves a separate line above every TAB label for the logo. */
final class FrameGeometry {
    static final int TAB_LOGO_HEIGHT=52, TAB_LINE_HEIGHT=9;
    static final String HEADER="\n\n\n\n\n\n{tab-logo}\n&6&lTILKICRAFT &8┃ &f{online}/{max}\n";
    static final String FOOTER="\n&7{player} &8┃ &f{ping} ms";
    private FrameGeometry() {}
}
