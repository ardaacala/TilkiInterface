package com.tilkicraft.ui;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Optional bridge to the single server pack sender. */
public final class SharedPack {
    public static final UUID ID=UUID.fromString("2c17449c-7006-4595-ae41-bef76e22ea31");
    private SharedPack() {}
    public static String fontStyle() {
        var plugin=Bukkit.getPluginManager().getPlugin("TilkiResourcePack");
        if(plugin!=null && plugin.isEnabled()) try { return (String)plugin.getClass().getMethod("getFontStyle").invoke(plugin); }
        catch(ReflectiveOperationException ignored) { }
        return "modern";
    }
    public static boolean active() { return Bukkit.getPluginManager().isPluginEnabled("TilkiResourcePack"); }
    public static boolean matches(UUID own,UUID incoming) { return (active()?ID:own).equals(incoming); }
    public static boolean loaded(Player player) {
        var plugin=Bukkit.getPluginManager().getPlugin("TilkiResourcePack");
        if(plugin==null || !plugin.isEnabled()) return false;
        try { return (boolean)plugin.getClass().getMethod("isLoaded",UUID.class).invoke(plugin,player.getUniqueId()); }
        catch(ReflectiveOperationException ignored) { return false; }
    }
}

