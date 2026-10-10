package com.tilkicraft.ui;

import java.lang.reflect.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

public final class TilkiInterface extends JavaPlugin implements Listener, CommandExecutor {
    private static final UUID PACK = UUID.fromString("d7bef941-09ab-430c-a872-52b463159ca2");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm").withZone(ZoneId.of("Europe/Istanbul"));
    private final Map<UUID, Panel> panels = new HashMap<>();
    private final Set<UUID> ready = new HashSet<>(), hidden = new HashSet<>();
    private Object authApi, economy, chat;
    private Method auth, unrestricted, placeholder, balance, rankPrefix, primaryGroup;
    private String url;
    private byte[] hash;
    private boolean authRequired;
    private static final class Panel {
        final Scoreboard previous, board;
        final Objective objective;
        final List<Team> rows = new ArrayList<>();
        final BossBar bar;
        final String previousHeader, previousFooter;
        String lastHeader, lastFooter;
        Panel(Player player, boolean sidebar, int count) {
            previousHeader=player.getPlayerListHeader(); previousFooter=player.getPlayerListFooter();
            previous = player.getScoreboard();
            board = sidebar ? Bukkit.getScoreboardManager().getNewScoreboard() : null;
            objective = sidebar ? board.registerNewObjective("tilki_ui", Criteria.DUMMY, "TilkiCraft") : null;
            bar = Bukkit.createBossBar("TilkiCraft", BarColor.YELLOW, BarStyle.SOLID);
            if (sidebar) {
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                hideNumbers(objective);
                for (int i=0;i<count;i++) {
                    Team row = board.registerNewTeam("tilki_"+i);
                    String entry = ChatColor.values()[i].toString()+ChatColor.RESET;
                    row.addEntry(entry);
                    objective.getScore(entry).setScore(count-i);
                    rows.add(row);
                }
                player.setScoreboard(board);
            }
            bar.addPlayer(player);
        }
        // Spigot lacks a public number-format API. Use it when this server exposes it.
        static void hideNumbers(Objective objective) {
            try {
                Object handle = objective.getClass().getMethod("getHandle").invoke(objective);
                Class<?> blank = Class.forName("net.minecraft.network.chat.numbers.BlankFormat");
                Object format = null;
                for (Field field : blank.getFields()) if (Modifier.isStatic(field.getModifiers()) && blank.isAssignableFrom(field.getType())) { format=field.get(null); break; }
                if (format == null) return;
                for (Method method : handle.getClass().getMethods())
                    if (method.getParameterCount()==1 && method.getParameterTypes()[0].isInstance(format) && method.getReturnType()==void.class) { method.invoke(handle,format); return; }
            } catch (ReflectiveOperationException | RuntimeException ignored) { }
        }
        void close(Player player) {
            bar.removeAll();
            if (lastHeader!=null && Objects.equals(lastHeader,player.getPlayerListHeader()) && Objects.equals(lastFooter,player.getPlayerListFooter())) player.setPlayerListHeaderFooter(previousHeader==null?"":previousHeader,previousFooter==null?"":previousFooter);
            if (board != null && player.getScoreboard()==board) player.setScoreboard(previous);
        }
    }
    @Override public void onEnable() {
        saveDefaultConfig();
        bindHooks();
        Bukkit.getScheduler().runTask(this,this::bindHooks);
        readPack();
        Bukkit.getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(getCommand("tilkiui")).setExecutor(this);
        Bukkit.getScheduler().runTaskTimer(this,this::refresh,20,20);
        for (Player player : Bukkit.getOnlinePlayers()) requestPack(player);
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    private void bindHooks() {
        authApi=null; auth=null; unrestricted=null; placeholder=null; economy=null; balance=null; chat=null; rankPrefix=null; primaryGroup=null;
        var manager=Bukkit.getPluginManager();
        authRequired=manager.isPluginEnabled("AuthMe");
        try {
            if (authRequired) {
                Class<?> type=Class.forName("fr.xephi.authme.api.v3.AuthMeApi",true,manager.getPlugin("AuthMe").getClass().getClassLoader());
                authApi=type.getMethod("getInstance").invoke(null);
                auth=type.getMethod("isAuthenticated",Player.class);
                unrestricted=type.getMethod("isUnrestricted",Player.class);
            }
        } catch (ReflectiveOperationException ex) { getLogger().warning("AuthMe hook unavailable; stats remain hidden."); }
        try {
            if (manager.isPluginEnabled("PlaceholderAPI")) placeholder=Class.forName("me.clip.placeholderapi.PlaceholderAPI",true,manager.getPlugin("PlaceholderAPI").getClass().getClassLoader()).getMethod("setPlaceholders",OfflinePlayer.class,String.class);
        } catch (ReflectiveOperationException ex) { getLogger().warning("PlaceholderAPI hook unavailable."); }
        try {
            if (manager.isPluginEnabled("Vault")) {
                Class type=Class.forName("net.milkbowl.vault.economy.Economy",true,manager.getPlugin("Vault").getClass().getClassLoader());
                economy=Bukkit.getServicesManager().load(type);
                balance=type.getMethod("getBalance",OfflinePlayer.class);
            }
        } catch (ReflectiveOperationException ex) { getLogger().warning("Vault economy hook unavailable."); }
        try {
            if (manager.isPluginEnabled("Vault")) {
                Class type=Class.forName("net.milkbowl.vault.chat.Chat",true,manager.getPlugin("Vault").getClass().getClassLoader());
                chat=Bukkit.getServicesManager().load(type);
                rankPrefix=type.getMethod("getPlayerPrefix",Player.class);
                primaryGroup=type.getMethod("getPrimaryGroup",Player.class);
            }
        } catch (ReflectiveOperationException ex) { getLogger().warning("Vault rank hook unavailable."); }
    }
    private void readPack() {
        if (!getConfig().getBoolean("layout.compact-v2",false)) {
            List<String> lines=new ArrayList<>(getConfig().getStringList("sidebar.lines"));
            lines.removeIf(line->line.contains("{kills}") || line.contains("{deaths}"));
            String[] keys={"online","ping","player","rank","money","level","playtime"};
            String[] icons={"online","ping","player","rank","money","island","time"};
            for(int i=0;i<lines.size();i++) {
                String line=lines.get(i).replace("&e{player}","&f{player}").replace("{level}","{island-level}").replace("&fSeviye","&fAda Seviyesi");
                for(int k=0;k<keys.length;k++) if(line.contains("{"+(keys[k].equals("level")?"island-level":keys[k])+"}")) line=line.replaceFirst("^&6[●◆✦] ","{icon-"+icons[k]+"} ");
                lines.set(i,line);
            }
            getConfig().set("sidebar.lines",lines); getConfig().set("layout.compact-v2",true); saveConfig();
        }
        String previousUrl=getConfig().getString("pack.url","");
        if (previousUrl.equals("https://github.com/ardaacala/TilkiInterface/releases/download/v1.0.0/TilkiInterface-ResourcePack.zip") || previousUrl.equals("https://github.com/ardaacala/TilkiInterface/releases/download/v1.1.0/TilkiInterface-ResourcePack.zip")) {
            getConfig().set("pack.url","https://github.com/ardaacala/TilkiInterface/releases/download/v1.2.0/TilkiInterface-ResourcePack.zip");
            getConfig().set("pack.sha1","0d44a229df817295ac380a4e191d6b5e9f1b838a"); saveConfig();
        }
        url=getConfig().getString("pack.url","").trim(); hash=null;
        if (url.isEmpty()) return;
        try {
            if (!url.startsWith("https://")) throw new IllegalArgumentException("HTTPS required");
            hash=HexFormat.of().parseHex(getConfig().getString("pack.sha1",""));
            if (hash.length!=20) throw new IllegalArgumentException("SHA1 must be 40 hex characters");
        } catch (IllegalArgumentException ex) { hash=null; getLogger().warning("UI pack disabled: "+ex.getMessage()); }
    }
    private boolean loggedIn(Player player) {
        if (!authRequired) return true;
        if (authApi==null || auth==null) return false;
        try { return (boolean)auth.invoke(authApi,player) || (boolean)unrestricted.invoke(authApi,player); }
        catch (ReflectiveOperationException ex) { return false; }
    }
    private void requestPack(Player player) {
        if (SharedPack.active()) { if (SharedPack.loaded(player)) ready.add(player.getUniqueId()); return; }
        if (hash==null) return;
        UUID id=player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(this,()->{
            Player current=Bukkit.getPlayer(id);
            if (!SharedPack.active() && current!=null && hash!=null) current.addResourcePack(PACK,url,hash,getConfig().getString("pack.prompt"),getConfig().getBoolean("pack.required",false));
        },Math.max(1,getConfig().getLong("pack.delay-ticks",100)));
    }
    @EventHandler public void join(PlayerJoinEvent event) { requestPack(event.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent event) {
        remove(event.getPlayer()); ready.remove(event.getPlayer().getUniqueId()); hidden.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler public void pack(PlayerResourcePackStatusEvent event) {
        if (!SharedPack.matches(PACK,event.getID())) return;
        if (event.getStatus()==PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) ready.add(event.getPlayer().getUniqueId());
        else if (event.getStatus()!=PlayerResourcePackStatusEvent.Status.ACCEPTED && event.getStatus()!=PlayerResourcePackStatusEvent.Status.DOWNLOADED) ready.remove(event.getPlayer().getUniqueId());
    }
    private String expand(Player player,String value,String fallback) {
        if (value==null || value.isBlank()) return fallback;
        if (placeholder!=null) try { value=(String)placeholder.invoke(null,player,value); } catch (ReflectiveOperationException ignored) { }
        return UiText.unresolved(value)?fallback:value;
    }
    private Map<String,String> values(Player player) {
        String money="—";
        if (economy!=null && balance!=null) try { money=UiText.compact(((Number)balance.invoke(economy,player)).doubleValue()); } catch (ReflectiveOperationException ignored) { }
        Map<String,String> data=new HashMap<>();
        data.put("player",player.getName()); data.put("online",""+Bukkit.getOnlinePlayers().size()); data.put("max",""+Bukkit.getMaxPlayers());
        data.put("ping",""+player.getPing()); data.put("money",money);
        String rank=getConfig().getString("sidebar.rank-fallback","Oyuncu");
        if (chat!=null && rankPrefix!=null) try {
            String prefix=(String)rankPrefix.invoke(chat,player);
            String group=(String)primaryGroup.invoke(chat,player);
            if (prefix!=null && !prefix.isBlank()) rank=prefix;
            else if (group!=null && !group.isBlank()) rank=group;
        } catch (ReflectiveOperationException ignored) { }
        data.put("rank",expand(player,getConfig().getString("sidebar.rank-placeholder"),rank));
        if (getConfig().getBoolean("sidebar.use-tilkitags",true)) {
            var tags=Bukkit.getPluginManager().getPlugin("TilkiTags");
            if (tags!=null && tags.isEnabled()) {
                String badge=UiText.rankBadge(tags,player);
                if (badge!=null) data.put("rank",badge);
            }
        }
        data.put("level",""+player.getLevel()); data.put("kills",""+player.getStatistic(Statistic.PLAYER_KILLS)); data.put("deaths",""+player.getStatistic(Statistic.DEATHS));
        data.put("playtime",UiText.duration(player.getStatistic(Statistic.PLAY_ONE_MINUTE)));
        data.put("island",expand(player,getConfig().getString("sidebar.island-placeholder"),"—"));
        data.put("date",DATE.format(Instant.now())); data.put("footer",getConfig().getString("sidebar.footer","TilkiCraft"));
        data.put("island-level",expand(player,getConfig().getString("island-level.placeholder","%superior_island_level_format%"),"0"));
        String[] icons={"online","ping","player","rank","money","island","time"};
        for(int i=0;i<icons.length;i++) data.put("icon-"+icons[i],ready.contains(player.getUniqueId())?"§f"+(char)(0xea20+i):"&6◆");
        return data;
    }
    private String render(Player player,String text,Map<String,String> values) {
        for (var value:values.entrySet()) text=text.replace("{"+value.getKey()+"}",value.getValue());
        return ChatColor.translateAlternateColorCodes('&',expand(player,text,text));
    }
    private void refresh() {
        for (Player player:Bukkit.getOnlinePlayers()) {
            UUID id=player.getUniqueId();
            if (!loggedIn(player) || hidden.contains(id)) { remove(player); continue; }
            Panel panel=panels.get(id);
            if (panel!=null && panel.board!=null && player.getScoreboard()!=panel.board) { remove(player); continue; }
            if (panel==null) {
                boolean sidebar=getConfig().getBoolean("sidebar.enabled",true);
                if (getConfig().getBoolean("sidebar.respect-existing",true) && player.getScoreboard().getObjective(DisplaySlot.SIDEBAR)!=null) sidebar=false;
                panel=new Panel(player,sidebar,Math.min(13,getConfig().getStringList("sidebar.lines").size())); panels.put(id,panel);
            }
            Map<String,String> values=values(player);
            boolean framed=ready.contains(id) && getConfig().getBoolean("sidebar.frame",true);
            if (getConfig().getBoolean("tab.enabled",true)) {
                values.put("tab-logo",ready.contains(id)?"\n\n\n\uEA02\n":"&6&lTILKICRAFT");
                List<? extends Player> visible=Bukkit.getOnlinePlayers().stream().filter(player::canSee).toList();
                int players=Math.max(1,Math.min(80,visible.size())), columns=(players+19)/20, rows=(players+columns-1)/columns;
                int longest=visible.stream().mapToInt(p->UiText.tabNameWidth(p.getPlayerListName())).max().orElse(120);
                int width=Math.max(192,Math.min(768,((columns*(longest+24)+63)/64)*64)), variant=(width-192)/64;
                values.put("tab-frame",ready.contains(id)?"§f"+(char)(0xec00+variant*20+rows-1):"&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                values.put("tab-bottom",ready.contains(id)?"§f"+(char)(0xed00+variant):"&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                String header=String.join("\n",getConfig().getStringList("tab.header"));
                String footer=String.join("\n",getConfig().getStringList("tab.footer"));
                if (header.isEmpty()) header="{tab-logo}\n{tab-frame}\n&6&lTILKICRAFT &8┃ &f{online}/{max}\n";
                if (footer.isEmpty()) footer="\n&7Oyuncu: &f{player} &8┃ &7Ping: &f{ping} ms\n&6{rank} &8┃ &fTilkiCraft\n{tab-bottom}";
                panel.lastHeader=render(player,header,values); panel.lastFooter=render(player,footer,values);
                player.setPlayerListHeaderFooter(panel.lastHeader,panel.lastFooter);
            }
            if (panel.objective!=null) {
                List<String> lines=getConfig().getStringList("sidebar.lines");
                int widest=lines.stream().map(line->render(player,line,values)).mapToInt(UiText::tabNameWidth).max().orElse(100)+12;
                int style=widest<=128?0:widest<=144?1:widest<=160?2:3;
                char[] frames={'\uEA03','\uEA10','\uEA11','\uEA12'}, rewind={'\uEA04','\uEA13','\uEA15','\uEA17'}, center={'\uEA05','\uEA14','\uEA16','\uEA18'};
                String heading="§f"+frames[style]+rewind[style]+center[style]+"\uEA01"+center[style];
                panel.objective.setDisplayName(ready.contains(id)?(framed?heading:"\uEA01"):render(player,getConfig().getString("sidebar.title","TilkiCraft"),values));
                for (int i=0;i<panel.rows.size();i++) panel.rows.get(i).setPrefix((framed?"\uEA07":"")+(i<lines.size()?render(player,lines.get(i),values):""));
            }
            panel.bar.setVisible(getConfig().getBoolean("top-bar.enabled",true));
            panel.bar.setTitle(render(player,getConfig().getString("top-bar.text","TilkiCraft"),values));
        }
    }
    private void remove(Player player) { Panel panel=panels.remove(player.getUniqueId()); if (panel!=null) panel.close(player); }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if (args.length==1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("tilkiui.admin")) { sender.sendMessage("Yetkin yok."); return true; }
            for (Player player:Bukkit.getOnlinePlayers()) remove(player);
            reloadConfig(); bindHooks(); readPack(); ready.clear();
            for (Player player:Bukkit.getOnlinePlayers()) requestPack(player);
            sender.sendMessage("TilkiStatsHUD ayarları yenilendi."); return true;
        }
        if (sender instanceof Player player) {
            if (!hidden.add(player.getUniqueId())) hidden.remove(player.getUniqueId());
            remove(player); sender.sendMessage(hidden.contains(player.getUniqueId())?"Panel kapatıldı.":"Panel açıldı.");
        }
        return true;
    }
    @Override public void onDisable() { for (Player player:Bukkit.getOnlinePlayers()) remove(player); ready.clear(); hidden.clear(); }
}
