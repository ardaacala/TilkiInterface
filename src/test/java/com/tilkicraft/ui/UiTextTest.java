package com.tilkicraft.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiTextTest {
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
}
