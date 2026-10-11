package com.tilkicraft.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FrameGeometryTest {
    @Test void tabFrameHasItsOwnSpaceAboveEveryLabel() {
        String header=FrameGeometry.HEADER;
        assertTrue(header.startsWith("{tab-frame}"));
        int titleLine=header.substring(0,header.indexOf("TILKICRAFT")).split("\n",-1).length-1;
        assertTrue(titleLine*FrameGeometry.TAB_LINE_HEIGHT>=FrameGeometry.TAB_LOGO_HEIGHT);
        assertTrue(FrameGeometry.FOOTER.isEmpty());
        assertEquals(1,FrameGeometry.columns(20));assertEquals(20,FrameGeometry.rows(20));
        assertEquals(2,FrameGeometry.columns(21));assertEquals(11,FrameGeometry.rows(21));
        assertEquals(4,FrameGeometry.columns(80));assertEquals(20,FrameGeometry.rows(80));
    }
}
