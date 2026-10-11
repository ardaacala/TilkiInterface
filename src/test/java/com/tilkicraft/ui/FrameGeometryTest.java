package com.tilkicraft.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FrameGeometryTest {
    @Test void tabFrameHasItsOwnSpaceAboveEveryLabel() {
        String header=FrameGeometry.HEADER;
        assertTrue(header.startsWith("{tab-logo}"));
        int titleLine=header.substring(0,header.indexOf("TILKICRAFT")).split("\n",-1).length-1;
        assertTrue(titleLine*FrameGeometry.TAB_LINE_HEIGHT>=FrameGeometry.TAB_LOGO_HEIGHT);
        assertTrue(FrameGeometry.FOOTER.isEmpty());
    }
}
