package com.tilkicraft.ui;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HdLayoutsTest {
    @Test void tiledHdFramesKeepTheirExactLogicalWidthWithoutAccumulatingGaps() throws Exception {
        Properties layouts=new Properties(),advances=new Properties();
        try(var input=getClass().getResourceAsStream("/frame-layouts.properties")) { layouts.load(input); }
        try(var input=getClass().getResourceAsStream("/frame-advances.properties")) { advances.load(input); }
        assertEquals(214,layouts.size());
        for(String key:layouts.stringPropertyNames()) {
            String[] parts=key.split("\\.");int expected=parts[0].equals("hud")?Integer.parseInt(parts[1]):192+64*Integer.parseInt(parts[1]);
            String value=layouts.getProperty(key);
            int width=value.codePoints().map(cp->Integer.parseInt(Objects.requireNonNull(advances.getProperty(Integer.toHexString(cp)),key))).sum();
            assertEquals(parts[0].equals("bottom")?0:expected+1,width,key);
            if(parts[0].equals("hud"))assertTrue(value.length()+12<=128,"Objective display name limit: "+key);
        }
    }
    @Test void allAlternativeFontsContainTurkishGlyphs() throws Exception {
        for(String style:List.of("modern","classic","fantasy")) try(var input=getClass().getResourceAsStream("/fonts/"+style+".ttf")) {
            var font=java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT,input);assertEquals(-1,font.canDisplayUpTo("İıŞşĞğÇçÖöÜü"),style);
        }
    }
}
