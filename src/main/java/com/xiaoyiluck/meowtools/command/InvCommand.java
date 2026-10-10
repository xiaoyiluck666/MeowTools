package com.xiaoyiluck.meowtools.command;

import com.xiaoyiluck.meowtools.MeowTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventPriority;
import java.util.Objects;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InvCommand implements CommandExecutor {

    private final MeowTools plugin;
    private final NamespacedKey placeholderKey;
    private final Map<Inventory, UUID> views = new HashMap<>();
    private final Map<Material, ItemStack> decorations = new HashMap<>();
    private final InvViewListener listener = new InvViewListener();
    private BukkitTask refreshTask;
    private int refreshCount;
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    public InvCommand(MeowTools plugin) {
        this.plugin = plugin;
        this.placeholderKey = new NamespacedKey(plugin, "inv_placeholder");
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    private Component legacyAmpersand(String text) {
        return LEGACY_AMPERSAND.deserialize(text == null ? "" : text);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("meowtools.inv")) {
            plugin.handleNoPermission(sender, command, label, args, "meowtools.inv");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player_only"));
            return true;
        }

        Player viewer = (Player) sender;

        Player target;
        if (args.length == 0) {
            target = viewer;
        } else {
            target = Bukkit.getPlayer(args[0]);
        }
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_found", "{player}", args[0]));
            return true;
        }

        Inventory view = createInventoryView(target);
        viewer.openInventory(view);
        startSyncTask(target, view);
        sender.sendMessage(plugin.getLanguageManager().getMessage("inv_opened", "{player}", target.getName()));
        return true;
    }

    private Inventory createInventoryView(Player target) {
        TargetInventoryHolder holder = new TargetInventoryHolder(target.getUniqueId());
        Inventory inv = Bukkit.createInventory(
                holder,
                54,
                plugin.getLanguageManager().getMessage("inv_title", "{player}", target.getName())
        );
        holder.inventory = inv;
        PlayerInventory targetInv = target.getInventory();

        ItemStack helmet = targetInv.getHelmet();
        ItemStack chest = targetInv.getChestplate();
        ItemStack legs = targetInv.getLeggings();
        ItemStack boots = targetInv.getBoots();
        ItemStack offHand = targetInv.getItemInOffHand();

        inv.setItem(0, armorOrPlaceholder(helmet, Material.CHAINMAIL_HELMET));
        inv.setItem(1, armorOrPlaceholder(chest, Material.CHAINMAIL_CHESTPLATE));
        inv.setItem(2, armorOrPlaceholder(legs, Material.CHAINMAIL_LEGGINGS));
        inv.setItem(3, armorOrPlaceholder(boots, Material.CHAINMAIL_BOOTS));
        inv.setItem(4, offhandOrPlaceholder(offHand));

        populateHeaderSlots(target, inv);
        inv.setItem(8, createPlayerHead(target));

        for (int slot = 9; slot <= 35; slot++) {
            ItemStack item = targetInv.getItem(slot);
            inv.setItem(slot, item == null ? null : item.clone());
        }

        for (int slot = 36; slot <= 44; slot++) {
            inv.setItem(slot, createGlassSeparator());
        }

        for (int hotbar = 0; hotbar <= 8; hotbar++) {
            ItemStack item = targetInv.getItem(hotbar);
            inv.setItem(45 + hotbar, item == null ? null : item.clone());
        }

        return inv;
    }

    private ItemStack armorOrPlaceholder(ItemStack item, Material placeholderType) {
        if (item != null && item.getType() != Material.AIR) {
            return item.clone();
        }
        ItemStack cached = decorations.get(placeholderType);
        if (cached != null) return cached;
        ItemStack placeholder = new ItemStack(placeholderType);
        ItemMeta meta = placeholder.getItemMeta();
        meta.customName(plugin.getLanguageManager().getMessage("inv_empty_armor"));
        meta.lore(List.of(plugin.getLanguageManager().getMessage("inv_edit_hint")));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(placeholderKey, PersistentDataType.BYTE, (byte) 1);
        placeholder.setItemMeta(meta);
        decorations.put(placeholderType, placeholder);
        return placeholder;
    }

    private ItemStack offhandOrPlaceholder(ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            return item.clone();
        }
        ItemStack cached = decorations.get(Material.SHIELD);
        if (cached != null) return cached;
        ItemStack placeholder = new ItemStack(Material.SHIELD);
        ItemMeta meta = placeholder.getItemMeta();
        meta.customName(plugin.getLanguageManager().getMessage("inv_empty_offhand"));
        meta.lore(List.of(plugin.getLanguageManager().getMessage("inv_edit_hint")));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(placeholderKey, PersistentDataType.BYTE, (byte) 1);
        placeholder.setItemMeta(meta);
        decorations.put(Material.SHIELD, placeholder);
        return placeholder;
    }

    private ItemStack createGlassSeparator() {
        ItemStack cached = decorations.get(Material.GRAY_STAINED_GLASS_PANE);
        if (cached != null) return cached;
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.customName(legacyAmpersand("&8分隔栏"));
        meta.lore(List.of());
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(placeholderKey, PersistentDataType.BYTE, (byte) 1);
        glass.setItemMeta(meta);
        decorations.put(Material.GRAY_STAINED_GLASS_PANE, glass);
        return glass;
    }

    private void populateHeaderSlots(Player target, Inventory inv) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("inv_view.header_slots");
        for (int slot = 5; slot <= 7; slot++) {
            ConfigurationSection configured = section == null ? null : section.getConfigurationSection(String.valueOf(slot));
            if (configured == null) {
                setIfChanged(inv, slot, createGlassSeparator());
                continue;
            }
            Material material = Material.matchMaterial(configured.getString("material", "GRAY_STAINED_GLASS_PANE"));
            if (material == null || material == Material.AIR || !material.isItem()) material = Material.GRAY_STAINED_GLASS_PANE;
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            String name = configured.getString("name", "");
            if (!name.isEmpty()) meta.customName(legacyAmpersand(plugin.getLanguageManager().applyPlaceholders(target, name)));
            List<Component> lore = new ArrayList<>();
            for (String line : configured.getStringList("lore")) {
                lore.add(legacyAmpersand(plugin.getLanguageManager().applyPlaceholders(target, line)));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            setIfChanged(inv, slot, item);
        }
    }

    private ItemStack createPlayerHead(Player target) { return createPlayerHead(target, null); }

    private ItemStack createPlayerHead(Player target, ItemStack previous) {
        ItemStack head = previous == null ? new ItemStack(Material.PLAYER_HEAD) : previous.clone();
        ItemMeta baseMeta = head.getItemMeta();
        if (!(baseMeta instanceof SkullMeta)) {
            return head;
        }
        SkullMeta meta = (SkullMeta) baseMeta;
        if (previous == null) applyAvailableProfile(target, meta);

        ConfigurationSection headSection = plugin.getConfig().getConfigurationSection("inv_view.player_head");
        String nameTemplate;
        if (headSection != null) {
            String configuredName = headSection.getString("name", "");
            if (configuredName != null && !configuredName.isEmpty()) {
                nameTemplate = configuredName;
            } else {
                nameTemplate = plugin.getLanguageManager().getRawMessage("inv_head_name");
            }
        } else {
            nameTemplate = plugin.getLanguageManager().getRawMessage("inv_head_name");
        }
        if (nameTemplate == null || nameTemplate.isEmpty() || nameTemplate.equals("inv_head_name")) {
            nameTemplate = "&e{player}";
        }
        String name = nameTemplate.replace("{player}", target.getName());
        name = plugin.getLanguageManager().applyPlaceholders(target, name);
        meta.customName(legacyAmpersand(name));

        double health = target.getHealth();
        AttributeInstance maxHealthAttribute = target.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : health;
        int food = target.getFoodLevel();
        int level = target.getLevel();
        float expProgress = target.getExp() * 100.0f;
        List<Component> lore = new ArrayList<>();

        List<String> configLore = headSection != null ? headSection.getStringList("lore") : null;
        if (configLore != null && !configLore.isEmpty()) {
            for (String raw : configLore) {
                if (raw == null) {
                    continue;
                }
                String processed = raw
                        .replace("{health}", String.format("%.1f", health))
                        .replace("{max_health}", String.format("%.1f", maxHealth))
                        .replace("{exp_level}", String.valueOf(level))
                        .replace("{exp_percent}", String.format("%.0f", expProgress))
                        .replace("{food}", String.valueOf(food));
                processed = plugin.getLanguageManager().applyPlaceholders(target, processed);
                lore.add(legacyAmpersand(processed));
            }
        } else {
            String line1 = plugin.getLanguageManager().getRawMessage("inv_head_lore_line1");
            if (line1 == null || line1.isEmpty() || line1.equals("inv_head_lore_line1")) {
                line1 = "&7生命值: &a{health}/{max_health}";
            }
            line1 = line1.replace("{health}", String.format("%.1f", health))
                    .replace("{max_health}", String.format("%.1f", maxHealth));
            line1 = plugin.getLanguageManager().applyPlaceholders(target, line1);
            lore.add(legacyAmpersand(line1));

            String line2 = plugin.getLanguageManager().getRawMessage("inv_head_lore_line2");
            if (line2 == null || line2.isEmpty() || line2.equals("inv_head_lore_line2")) {
                line2 = "&7经验: &a{exp_level} 级 &7({exp_percent}%)";
            }
            line2 = line2.replace("{exp_level}", String.valueOf(level))
                    .replace("{exp_percent}", String.format("%.0f", expProgress));
            line2 = plugin.getLanguageManager().applyPlaceholders(target, line2);
            lore.add(legacyAmpersand(line2));

            String line3 = plugin.getLanguageManager().getRawMessage("inv_head_lore_line3");
            if (line3 == null || line3.isEmpty() || line3.equals("inv_head_lore_line3")) {
                line3 = "&7饱食度: &a{food}/20";
            }
            line3 = line3.replace("{food}", String.valueOf(food));
            line3 = plugin.getLanguageManager().applyPlaceholders(target, line3);
            lore.add(legacyAmpersand(line3));
        }

        String line4 = plugin.getLanguageManager().getRawMessage("inv_head_lore_line4");
        if (line4 == null || line4.isEmpty() || line4.equals("inv_head_lore_line4")) {
            line4 = "&7当前药水效果:";
        }
        line4 = plugin.getLanguageManager().applyPlaceholders(target, line4);
        lore.add(legacyAmpersand(line4));

        Collection<PotionEffect> effects = target.getActivePotionEffects();
        if (effects.isEmpty()) {
            String noEffects = plugin.getLanguageManager().getRawMessage("inv_head_no_effects");
            if (noEffects == null || noEffects.isEmpty() || noEffects.equals("inv_head_no_effects")) {
                noEffects = "&8无";
            }
            noEffects = plugin.getLanguageManager().applyPlaceholders(target, noEffects);
            lore.add(legacyAmpersand(noEffects));
        } else {
            for (PotionEffect effect : effects) {
                String effectName = effect.getType().getKey().getKey();
                int amplifier = effect.getAmplifier() + 1;
                int seconds = effect.getDuration() / 20;
                String line = "&7- &b" + effectName + " " + amplifier + " &7(" + seconds + "s)";
                lore.add(legacyAmpersand(line));
            }
        }

        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
    }

    static void applyAvailableProfile(Player target, SkullMeta meta) {
        var profile = target.getPlayerProfile();
        // Incomplete skull owners make Paper retry Mojang lookups whenever lore
        // is refreshed. Use already available textures; otherwise keep the default head.
        if (profile != null && profile.hasTextures()) meta.setPlayerProfile(profile);
    }

    private void setIfChanged(Inventory view, int slot, ItemStack item) {
        if (!Objects.equals(view.getItem(slot), item)) view.setItem(slot, item);
    }

    private void syncViewFromTarget(Player target, Inventory inv, boolean header) {
        PlayerInventory inventory = target.getInventory();
        for (int slot = 0; slot < inv.getSize(); slot++) {
            int targetSlot = InventoryEditor.targetSlot(slot);
            if (targetSlot < 0) continue;
            ItemStack item = InventoryEditor.copy(inventory.getItem(targetSlot));
            if (slot <= 3) {
                Material material = switch (slot) {
                    case 0 -> Material.CHAINMAIL_HELMET; case 1 -> Material.CHAINMAIL_CHESTPLATE;
                    case 2 -> Material.CHAINMAIL_LEGGINGS; default -> Material.CHAINMAIL_BOOTS;
                };
                item = armorOrPlaceholder(item, material);
            } else if (slot == 4) item = offhandOrPlaceholder(item);
            setIfChanged(inv, slot, item);
        }
        if (header) {
            populateHeaderSlots(target, inv);
            setIfChanged(inv, 8, createPlayerHead(target, inv.getItem(8)));
        }
    }

    private void startSyncTask(Player target, Inventory view) {
        views.put(view, target.getUniqueId());
        if (refreshTask != null) return;
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            boolean header = ++refreshCount % 2 == 0;
            for (var entry : new ArrayList<>(views.entrySet())) {
                Player live = Bukkit.getPlayer(entry.getValue());
                if (live == null || !live.isOnline()) closeView(entry.getKey());
                else syncViewFromTarget(live, entry.getKey(), header);
            }
            stopIfEmpty();
        }, 10L, 10L);
    }

    private void closeView(Inventory inventory) {
        views.remove(inventory);
        for (var viewer : new ArrayList<>(inventory.getViewers())) viewer.closeInventory();
    }

    private void stopIfEmpty() {
        if (views.isEmpty() && refreshTask != null) { refreshTask.cancel(); refreshTask = null; }
    }

    public void reload() {
        decorations.clear();
        for (var entry : views.entrySet()) {
            Player target = Bukkit.getPlayer(entry.getValue());
            if (target != null) syncViewFromTarget(target, entry.getKey(), true);
        }
    }

    public void disable() {
        for (Inventory view : new ArrayList<>(views.keySet())) closeView(view);
        stopIfEmpty();
        HandlerList.unregisterAll(listener);
        decorations.clear();
    }

    static class TargetInventoryHolder implements InventoryHolder {
        private final UUID targetUuid;
        private Inventory inventory;
        TargetInventoryHolder(UUID targetUuid) { this.targetUuid = targetUuid; }
        @Override public @NotNull Inventory getInventory() { return inventory; }
    }

    class InvViewListener implements Listener {
        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            views.remove(event.getView().getTopInventory());
            stopIfEmpty();
            // The GUI is a projection. Never write a stale snapshot on close.
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onInventoryClick(InventoryClickEvent event) {
            Inventory top = event.getView().getTopInventory();
            if (!(top.getHolder() instanceof TargetInventoryHolder holder)) return;
            int slot = event.getRawSlot();
            if (slot >= top.getSize() || slot < 0) {
                // These actions can pull from/push into the upper GUI implicitly.
                if (event.isShiftClick() || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player viewer)) return;
            Player target = Bukkit.getPlayer(holder.targetUuid);
            if (target == null || !target.isOnline() || !viewer.hasPermission("meowtools.inv")) return;
            InventoryEditor.edit(viewer, target, event.getView(), slot, event.getClick(), event.getHotbarButton());
            // Other observers refresh from the authoritative target, not from this GUI.
            for (var entry : views.entrySet()) {
                if (entry.getValue().equals(target.getUniqueId())) syncViewFromTarget(target, entry.getKey(), false);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onInventoryDrag(InventoryDragEvent event) {
            Inventory top = event.getView().getTopInventory();
            if (!(top.getHolder() instanceof TargetInventoryHolder)) return;
            if (event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize())) event.setCancelled(true);
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            for (var entry : new ArrayList<>(views.entrySet())) {
                if (entry.getValue().equals(event.getPlayer().getUniqueId())) closeView(entry.getKey());
            }
            stopIfEmpty();
        }
    }
}
