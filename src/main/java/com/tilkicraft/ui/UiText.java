package com.tilkicraft.ui;
import java.util.Locale;

public final class UiText {
    private UiText() {}
    public static int tabNameWidth(String name) {
        name=org.bukkit.ChatColor.stripColor(name); int width=0;
        for(char c:name.toCharArray()) {
            if(c>=0xeb00 && c<=0xeb10) { width+=new int[]{55,53,37,49,53,54,36,48,48,47,48,73,35,41,61,67,38}[c-0xeb00]; continue; }
            if(c>=0xea20 && c<=0xea26) { width+=9; continue; }
            width+=switch(c) {case ' ', 'I', 't' -> 4;case 'i','!', '.',',',':',';' -> 2;case 'l','\'' -> 3;default -> 6;};
        }
        return width;
    }
    public static String compact(double value) {
        if (!Double.isFinite(value)) return "—";
        double amount = Math.abs(value);
        if (amount >= 1_000_000_000) return String.format(Locale.ROOT,"%.1fB",value/1_000_000_000);
        if (amount >= 1_000_000) return String.format(Locale.ROOT,"%.1fM",value/1_000_000);
        if (amount >= 1_000) return String.format(Locale.ROOT,"%.1fK",value/1_000);
        return String.format(Locale.ROOT,"%.0f",value);
    }
    public static String duration(long ticks) {
        long minutes = Math.max(0,ticks)/1200;
        return minutes/60 + "sa " + minutes%60 + "dk";
    }
    public static boolean unresolved(String value) {
        return value == null || value.isBlank() || value.matches(".*%[^%]+%.*");
    }
    public static String rankBadge(Object provider,org.bukkit.entity.Player player) {
        if(provider==null) return null;
        try {
            Object result=provider.getClass().getMethod("getRankBadge",org.bukkit.entity.Player.class,org.bukkit.entity.Player.class).invoke(provider,player,player);
            return result instanceof String badge && !badge.isBlank()?badge:null;
        } catch (ReflectiveOperationException ignored) { return null; }
    }
}
