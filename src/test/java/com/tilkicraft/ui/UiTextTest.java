package com.tilkicraft.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiTextTest {
    public static class BadgeProvider {
        public String getRankBadge(org.bukkit.entity.Player subject,org.bukkit.entity.Player viewer) { return "\uEB10"; }
    }
    @Test void usesTagPluginBadgeAndFallsBackForOlderOrMissingProviders() {
        assertEquals("\uEB10",UiText.rankBadge(new BadgeProvider(),null));
        assertNull(UiText.rankBadge(new Object(),null));
        assertNull(UiText.rankBadge(null,null));
    }
    @Test void formatsEconomyValuesWithoutLocaleDependence() {
        assertEquals("1.4M",UiText.compact(1_400_000));
        assertEquals("-1.2K",UiText.compact(-1200));
        assertEquals("—",UiText.compact(Double.NaN));
        assertEquals("99",UiText.compact(99));
    }
    @Test void convertsMinecraftPlayTimeTicksToHoursAndMinutes() {
        assertEquals("1sa 1dk",UiText.duration(61*1200));
        assertEquals("0sa 0dk",UiText.duration(-1));
    }
    @Test void detectsUninstalledPlaceholderExpansions() {
        assertTrue(UiText.unresolved("%luckperms_prefix%"));
        assertTrue(UiText.unresolved(""));
        assertFalse(UiText.unresolved("VIP"));
    }
    @Test void tabFrameWidthAccountsForBadgesAndIgnoresColorCodes() {
        assertEquals(UiText.tabNameWidth("Arda"),UiText.tabNameWidth("§fArda"));
        assertTrue(UiText.tabNameWidth("\uEB10 Arda")>UiText.tabNameWidth("Arda"));
        assertTrue(UiText.tabNameWidth("WWWW")>UiText.tabNameWidth("iiii"));
    }
}
