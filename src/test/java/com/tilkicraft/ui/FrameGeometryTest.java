package com.tilkicraft.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FrameGeometryTest {
    @Test void tabLogoHasItsOwnSpaceAboveEveryLabel() {
        String header=FrameGeometry.HEADER;
        assertFalse(header.contains("tab-frame"));
        assertFalse(header.contains("tab-bottom"));
        int logoLine=header.substring(0,header.indexOf("{tab-logo}")).split("\n",-1).length-1;
        int titleLine=header.substring(0,header.indexOf("TILKICRAFT")).split("\n",-1).length-1;
        assertTrue(logoLine*FrameGeometry.TAB_LINE_HEIGHT>=FrameGeometry.TAB_LOGO_HEIGHT);
        assertTrue(titleLine>logoLine);
        assertFalse(FrameGeometry.FOOTER.contains("tab-frame"));
    }
}
