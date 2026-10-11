package com.tilkicraft.ui;

/** Logical GUI pixels; blank lines reserve room for the actual artwork. */
final class FrameGeometry {
    static final int HUD_INSET=36, HUD_TOP=-28, HUD_HEIGHT=188, HUD_BORDER=32;
    static final int TAB_BORDER=48, TAB_BASE_HEIGHT=168;
    static final String HEADER="{tab-logo}\n{tab-frame}\n\n\n\n\n\n\n&6&lTILKICRAFT &8┃ &f{online}/{max}\n\n";
    static final String FOOTER="\n&7{player} &8┃ &f{ping} ms\n\n\n\n\n\n";
    static int hudWidth(int textWidth) {
        for(int width:new int[]{192,208,224,256}) if(textWidth+HUD_INSET*2<=width)return width;
        return 256;
    }
    static int tabWidth(int columns,int nameWidth,int infoWidth) {
        int content=Math.max(columns*(nameWidth+24),infoWidth);
        return Math.max(320,Math.min(768,((content+TAB_BORDER*2+63)/64)*64));
    }
    private FrameGeometry() {}
}
