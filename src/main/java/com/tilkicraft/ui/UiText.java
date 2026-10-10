package com.tilkicraft.ui;
import java.util.Locale;

public final class UiText {
    private static final java.awt.font.FontRenderContext CONTEXT=new java.awt.font.FontRenderContext(null,true,true);
    private static java.awt.Font font;
    private static String currentStyle;
    private UiText() {}
    public static void fontStyle(String style) {
        if(style.equals(currentStyle))return;
        if(!java.util.Set.of("modern","classic","fantasy").contains(style))style="modern";
        try(var input=UiText.class.getResourceAsStream("/fonts/"+style+".ttf")) {
            font=java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT,input).deriveFont(9f);currentStyle=style;
        } catch(Exception ex) { font=new java.awt.Font("Dialog",java.awt.Font.PLAIN,9);currentStyle=style; }
    }
    public static int tabNameWidth(String name) {
        if(font==null)fontStyle("modern");
        name=org.bukkit.ChatColor.stripColor(name); double width=0;
        for(int c:name.codePoints().toArray()) {
            if(c>=0xeb00 && c<=0xeb10) { width+=new int[]{55,51,40,48,54,52,34,48,46,43,46,64,33,37,56,61,36}[c-0xeb00]; continue; }
            if(c>=0xea20 && c<=0xea26) { width+=9; continue; }
            width+=c==' '?4:font.getStringBounds(new String(Character.toChars(c)),CONTEXT).getWidth();
        }
        return (int)Math.ceil(width);
    }
    public static String pixelSpace(int pixels) { return String.valueOf((char)(0xee40+Math.max(0,Math.min(192,pixels)))); }
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
