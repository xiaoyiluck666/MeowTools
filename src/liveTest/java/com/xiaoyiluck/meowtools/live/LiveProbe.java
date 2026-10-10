package com.xiaoyiluck.meowtools.live;

import com.google.gson.Gson;
import com.xiaoyiluck.meowtools.MeowTools;
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.destroystokyo.paper.event.entity.ExperienceOrbMergeEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.*;

/** Test-only fixture/state observer. Clients perform commands, GUI clicks and F packets. */
public final class LiveProbe extends JavaPlugin implements Listener {
    private final Gson gson = new Gson();
    private MeowTools tools;
    private final Map<UUID, PermissionAttachment> grants = new HashMap<>();
    private boolean cancelPickup, cancelMerge, protectLeaf;
    private int pickups, merges, blockedMerges, leafEvents, clicks, deathMessages, suppressedDeaths, swaps;
    private final List<Double> tickMs = new ArrayList<>();
    private boolean measuring, stress;
    private long startNanos, startCpu;
    private Map<String, Object> perf = Map.of();
    private int stressTicks;
    private int explosions;
    private final List<Map<String, Object>> damageLog = new ArrayList<>();
    private final List<Map<String, Object>> deathLog = new ArrayList<>();

    @Override public void onEnable() {
        tools = (MeowTools) Bukkit.getPluginManager().getPlugin("MeowTools");
        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("mtprobe")).setExecutor(this);
        Bukkit.getScheduler().runTaskTimer(this, this::workload, 1, 1);
    }

    private Player player(String name) { return Objects.requireNonNull(Bukkit.getPlayerExact(name), name + " is offline"); }
    private World world() { return Bukkit.getWorlds().getFirst(); }
    private Location home(double x, double z) { return new Location(world(), x, 4, z); }

    @Override public boolean onCommand(CommandSender sender, Command c, String label, String[] a) {
        if (sender instanceof Player) return true;
        try {
            String mode = a.length == 0 ? "state" : a[0];
            switch (mode) {
                case "prepare" -> prepare();
                case "fixture" -> inventoryFixture();
                case "xp" -> xp(a[1]);
                case "leaves" -> leaves();
                case "break" -> player("Target").breakBlock(world().getBlockAt(2, 5, 0));
                case "explode" -> explode(a[1]);
                case "set" -> {
                    String key = a[1]; String value = a[2];
                    Object parsed = value.equals("null") ? null : value.equals("true") || value.equals("false")
                            ? Boolean.valueOf(value) : value;
                    tools.getConfig().set(key, parsed); tools.saveConfig();
                }
                case "slot" -> player(a[1]).getInventory().setItem(Integer.parseInt(a[2]), new ItemStack(Material.valueOf(a[3]), Integer.parseInt(a[4])));
                case "permission" -> grant(player(a[1]), a[2], Boolean.parseBoolean(a[3]));
                case "kill" -> player("Target").setHealth(0);
                case "respawn" -> player("Target").spigot().respawn();
                case "enable" -> { Bukkit.getPluginManager().enablePlugin(tools); tools = (MeowTools) Bukkit.getPluginManager().getPlugin("MeowTools"); }
                case "disable" -> Bukkit.getPluginManager().disablePlugin(tools);
                case "stress" -> { stress = Boolean.parseBoolean(a[1]); if (!stress) cleanupEntities(); }
                case "perf" -> {
                    if (a[1].equals("start")) {
                        tickMs.clear(); measuring = true; startNanos = System.nanoTime(); startCpu = cpu();
                    } else {
                        measuring = false;
                        List<Double> sorted = tickMs.stream().sorted().toList();
                        double seconds = (System.nanoTime() - startNanos) / 1e9;
                        perf = new LinkedHashMap<>();
                        perf.put("seconds", seconds); perf.put("ticks", sorted.size());
                        perf.put("observedTPS", sorted.size() / seconds);
                        perf.put("meanMSPT", sorted.stream().mapToDouble(x -> x).average().orElse(0));
                        perf.put("p50MSPT", percentile(sorted, .50)); perf.put("p95MSPT", percentile(sorted, .95));
                        perf.put("p99MSPT", percentile(sorted, .99)); perf.put("maxMSPT", percentile(sorted, 1));
                        perf.put("ticksOver50ms", sorted.stream().filter(x -> x > 50).count());
                        perf.put("processCPUSeconds", (cpu() - startCpu) / 1e9);
                        perf.put("usedHeapMiB", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576.0);
                        perf.put("players", Bukkit.getOnlinePlayers().size());
                    }
                }
                case "state" -> { }
                default -> throw new IllegalArgumentException("Unknown mode: " + mode);
            }
            var bytes = new java.io.ByteArrayOutputStream();
            try (var gzip = new java.util.zip.GZIPOutputStream(bytes)) {
                gzip.write(gson.toJson(state()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            sender.sendMessage("LIVEGZIP:" + Base64.getEncoder().encodeToString(bytes.toByteArray()));
        } catch (Exception e) {
            sender.sendMessage("LIVEERROR:" + e);
            getLogger().log(java.util.logging.Level.SEVERE, "Live probe failure", e);
        }
        return true;
    }

    private void grant(Player p, String permission, boolean enabled) {
        grants.computeIfAbsent(p.getUniqueId(), k -> p.addAttachment(this)).setPermission(permission, enabled);
    }

    private void prepare() {
        var config = tools.getConfig();
        config.set("language", "en_us");
        config.set("server_settings.exp_multiplier.custom", null);
        config.set("shortcuts.f.commands", List.of("say LIVE_F"));
        config.set("shortcuts.shift_f.commands", List.of("say LIVE_SHIFT_F"));
        config.set("shortcuts.look_up_f.enabled", false);
        config.set("shortcuts.look_up_shift_f.enabled", false);
        config.set("server_settings.death_message_control.same_message_only", false);
        tools.saveConfig(); tools.reloadPlugin();
        world().setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world().setGameRule(GameRule.SHOW_DEATH_MESSAGES, true);
        world().setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        world().setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world().setGameRule(GameRule.KEEP_INVENTORY, true);
        world().setDifficulty(Difficulty.NORMAL);
        world().setTime(6000);
        // Flat worlds start near y=-60; provide a real floor at the fixture's y=4.
        for (int x = -8; x <= 72; x++) for (int z = -8; z <= 32; z++) world().getBlockAt(x, 3, z).setType(Material.STONE, false);
        world().setSpawnLocation(0, 4, 0);
        int i = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.closeInventory(); p.setItemOnCursor(null); p.getInventory().clear();
            p.setOp(p.getName().startsWith("Admin") || p.getName().startsWith("Load"));
            p.setGameMode(GameMode.SURVIVAL); p.teleport(home(i++ * 3, 0));
            p.setHealth(20); p.setFoodLevel(20); p.setTotalExperience(0); p.setLevel(0); p.setExp(0);
        }
        cancelPickup = cancelMerge = protectLeaf = false;
        pickups = merges = blockedMerges = leafEvents = clicks = deathMessages = suppressedDeaths = swaps = 0;
        explosions = 0; damageLog.clear(); deathLog.clear();
        cleanupEntities();
    }

    private void inventoryFixture() {
        for (Player p : Bukkit.getOnlinePlayers()) { p.closeInventory(); p.setItemOnCursor(null); p.getInventory().clear(); }
        Player t = player("Target");
        t.getInventory().setItem(9, new ItemStack(Material.DIAMOND, 12));
        t.getInventory().setItem(10, new ItemStack(Material.DIAMOND, 7));
        t.getInventory().setItem(0, new ItemStack(Material.GOLD_INGOT, 5));
        t.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        t.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
        for (String name : List.of("AdminA", "AdminB")) {
            player(name).getInventory().setItem(0, new ItemStack(Material.IRON_INGOT, 3));
            player(name).getInventory().setItem(9, new ItemStack(Material.IRON_HELMET));
        }
    }

    private void xp(String mode) {
        cleanupEntities();
        Player p = player("Target");
        p.closeInventory(); p.getInventory().clear(); p.setTotalExperience(0); p.setLevel(0); p.setExp(0);
        p.teleport(home(0, 20));
        grant(p, "meowtools.exp.2.0", mode.equals("double"));
        grant(p, "meowtools.exp.0.0", mode.equals("zero"));
        cancelPickup = mode.equals("cancel"); cancelMerge = mode.equals("cancelMerge");
        pickups = merges = blockedMerges = 0;
        if (mode.equals("mending")) {
            ItemStack pick = new ItemStack(Material.DIAMOND_PICKAXE);
            pick.addEnchantment(Enchantment.MENDING, 1);
            Damageable meta = (Damageable) pick.getItemMeta(); meta.setDamage(100); pick.setItemMeta(meta);
            p.getInventory().setItemInMainHand(pick);
            orb(p.getLocation(), 20, 1);
        } else {
            orb(p.getLocation().add(.15, 0, 0), 7, 3);
            orb(p.getLocation().add(-.15, 0, 0), 5, 2);
        }
    }

    private void orb(Location at, int amount, int count) {
        world().spawn(at, ExperienceOrb.class, e -> {
            e.setExperience(amount); e.setCount(count); e.setGravity(false); e.setVelocity(new org.bukkit.util.Vector());
        });
    }

    private void cleanupEntities() {
        for (Entity e : world().getEntities()) if (e instanceof ExperienceOrb || e instanceof Item || e instanceof TNTPrimed) e.remove();
    }

    private void leaves() {
        protectLeaf = true; leafEvents = 0;
        world().getBlockAt(2, 5, 0).setType(Material.OAK_LOG);
        for (int x = 1; x <= 3; x++) {
            Block b = world().getBlockAt(x, 6, 0);
            Leaves data = (Leaves) Material.OAK_LEAVES.createBlockData();
            data.setPersistent(x == 2); data.setDistance(7); b.setBlockData(data, false);
        }
        player("Target").teleport(home(2, 2));
    }

    private void explode(String kind) {
        var cfg = tools.getConfig();
        for (String source : List.of("tnt", "creeper", "other")) {
            cfg.set("server_settings.explosion_settings." + source + ".break_blocks", false);
            cfg.set("server_settings.explosion_settings." + source + ".damage_entities", false);
        }
        tools.saveConfig(); tools.reloadPlugin();
        Player p = player("Target"); p.teleport(home(20, 20)); p.setHealth(20);
        world().getBlockAt(21, 4, 20).setType(Material.STONE);
        if (kind.equals("tnt")) world().spawn(p.getLocation().add(0, .3, 0), TNTPrimed.class, t -> t.setFuseTicks(5));
        else if (kind.equals("creeper")) {
            Creeper creeper = world().spawn(p.getLocation().add(0, .3, 0), Creeper.class);
            creeper.setAI(false);
            Bukkit.getScheduler().runTaskLater(this, creeper::explode, 5);
        }
        else world().createExplosion(p.getLocation(), 3, false, true);
    }

    private Map<String, Object> item(ItemStack s) {
        if (s == null || s.getType().isAir()) return Map.of("type", "AIR", "count", 0);
        Map<String, Object> v = new LinkedHashMap<>(); v.put("type", s.getType().name()); v.put("count", s.getAmount());
        if (s.getItemMeta() instanceof Damageable d) v.put("damage", d.getDamage());
        return v;
    }

    private Map<String, Object> state() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("server", Bukkit.getVersion()); result.put("enabled", tools.isEnabled());
        result.put("plugins", Arrays.stream(Bukkit.getPluginManager().getPlugins()).map(p -> p.getName()).toList());
        Map<String, Object> players = new LinkedHashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("slots", Arrays.stream(p.getInventory().getContents()).map(this::item).toList());
            info.put("cursor", item(p.getItemOnCursor()));
            Inventory top = p.getOpenInventory().getTopInventory();
            info.put("topSize", top.getSize()); info.put("top", Arrays.stream(top.getContents()).map(this::item).toList());
            info.put("xp", p.getTotalExperience()); info.put("health", p.getHealth());
            info.put("nightVision", p.hasPotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION));
            info.put("vanished", tools.isEnabled() && tools.getVanishManager().isVanished(p));
            Player target = Bukkit.getPlayerExact("AdminA"); info.put("seesAdminA", target == null || p.canSee(target));
            players.put(p.getName(), info);
        }
        result.put("players", players);
        result.put("orbTotal", world().getEntitiesByClass(ExperienceOrb.class).stream().mapToLong(e -> (long)e.getCount() * e.getExperience()).sum());
        result.put("orbs", world().getEntitiesByClass(ExperienceOrb.class).size());
        result.put("pickups", pickups); result.put("merges", merges); result.put("blockedMerges", blockedMerges);
        result.put("clicks", clicks); result.put("swaps", swaps);
        result.put("deathMessages", deathMessages); result.put("suppressedDeaths", suppressedDeaths);
        result.put("explosions", explosions); result.put("damageLog", damageLog); result.put("deathLog", deathLog);
        result.put("leaves", List.of(world().getBlockAt(1,6,0).getType().name(), world().getBlockAt(2,6,0).getType().name(), world().getBlockAt(3,6,0).getType().name()));
        result.put("leafEvents", leafEvents); result.put("explosionBlock", world().getBlockAt(21,4,20).getType().name());
        result.put("pluginTasks", Bukkit.getScheduler().getPendingTasks().stream().filter(t -> t.getOwner() == tools).count());
        result.put("perf", perf);
        return result;
    }

    private long cpu() { return ProcessHandle.current().info().totalCpuDuration().orElse(Duration.ZERO).toNanos(); }
    private double percentile(List<Double> values, double q) { return values.isEmpty() ? 0 : values.get(Math.min(values.size()-1, (int)Math.ceil(q * values.size())-1)); }

    private void workload() {
        if (!stress) return;
        stressTicks++;
        if (stressTicks % 100 == 0) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().startsWith("Load")) for (int i = 0; i < 30; i++) orb(p.getLocation().add((i % 5) * .2, 0, (i / 5) * .2), 3 + i % 2, 1);
            }
            Player actor = Bukkit.getPlayerExact("Target");
            if (actor != null) {
                for (int x = 30; x < 40; x++) for (int z = 0; z < 10; z++) {
                    world().getBlockAt(x, 5, z).setType(Material.OAK_LOG, false);
                    Leaves leaf = (Leaves) Material.OAK_LEAVES.createBlockData(); leaf.setDistance(7);
                    world().getBlockAt(x, 6, z).setBlockData(leaf, false);
                    actor.breakBlock(world().getBlockAt(x, 5, z));
                }
            }
        }
        Player target = Bukkit.getPlayerExact("Target");
        if (target != null) target.getInventory().setItem(10, new ItemStack(Material.DIAMOND, 1 + stressTicks % 64));
    }

    @EventHandler(priority = EventPriority.LOWEST) public void pickup(PlayerPickupExperienceEvent e) { pickups++; if (cancelPickup) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void merge(ExperienceOrbMergeEvent e) { merges++; if (cancelMerge) { e.setCancelled(true); blockedMerges++; } }
    @EventHandler(priority = EventPriority.LOWEST) public void leaf(LeavesDecayEvent e) { leafEvents++; if (protectLeaf && e.getBlock().getX() == 3 && e.getBlock().getY() == 6 && e.getBlock().getZ() == 0) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.MONITOR) public void click(InventoryClickEvent e) { clicks++; }
    @EventHandler(priority = EventPriority.MONITOR) public void swap(PlayerSwapHandItemsEvent e) { swaps++; }
    @EventHandler(priority = EventPriority.MONITOR) public void death(PlayerDeathEvent e) {
        if (e.deathMessage() == null) suppressedDeaths++; else deathMessages++;
        deathLog.add(Map.of("player", e.getEntity().getName(), "tick", Bukkit.getCurrentTick(), "messagePresent", e.deathMessage() != null));
    }
    @EventHandler(priority = EventPriority.MONITOR) public void damage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || !p.getName().equals("Target")) return;
        damageLog.add(Map.of("cause", e.getCause().name(), "damageType", e.getDamageSource().getDamageType().getKey().toString(), "cancelled", e.isCancelled(), "damage", e.getFinalDamage(), "tick", Bukkit.getCurrentTick(), "health", p.getHealth(), "damager", e instanceof EntityDamageByEntityEvent by ? by.getDamager().getType().name() : "none"));
    }
    @EventHandler(priority = EventPriority.MONITOR) public void explosion(EntityExplodeEvent e) { explosions++; }
    @EventHandler(priority = EventPriority.MONITOR) public void explosion(BlockExplodeEvent e) { explosions++; }
    @EventHandler public void tick(ServerTickEndEvent e) { if (measuring) tickMs.add(e.getTickDuration()); }
}
