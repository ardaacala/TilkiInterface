package com.tilkicraft.ui;
import java.util.Locale;

public final class UiText {
    private UiText() {}
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
