package com.tilkicraft.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FrameGeometryTest {
    @Test void sidebarDateAndFooterStayBetweenTopAndBottomRails() {
        assertTrue(FrameGeometry.HUD_TOP+FrameGeometry.HUD_BORDER<9);
        assertTrue(13*9+9<FrameGeometry.HUD_TOP+FrameGeometry.HUD_HEIGHT-FrameGeometry.HUD_BORDER);
        assertTrue(FrameGeometry.HUD_INSET>FrameGeometry.HUD_BORDER);
        for(int width:new int[]{80,120,160,180})assertTrue(FrameGeometry.hudWidth(width)>=width+72);
    }
    @Test void tabTitleClearsTheTopRailAndHasOnlyOneCompleteFrame() {
        String fromAnchor=FrameGeometry.HEADER.substring(FrameGeometry.HEADER.indexOf("{tab-frame}"));
        String[] lines=fromAnchor.split("\n",-1);int title=-1;
        for(int i=0;i<lines.length;i++)if(lines[i].contains("TILKICRAFT"))title=i;
        assertTrue(title*9>=FrameGeometry.TAB_BORDER+9);
        assertFalse(FrameGeometry.FOOTER.contains("{tab-bottom}"));
        int footerText=lines.length*9+9+9;
        assertTrue(footerText+9<FrameGeometry.TAB_BASE_HEIGHT+9-FrameGeometry.TAB_BORDER);
        assertTrue(FrameGeometry.tabWidth(1,160,180)>=180+96);
    }
}
