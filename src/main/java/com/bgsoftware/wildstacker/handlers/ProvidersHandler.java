package com.bgsoftware.wildstacker.handlers;

import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.hooks.ClaimsProvider;
import com.bgsoftware.wildstacker.api.hooks.SpawnersProvider;
import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import com.bgsoftware.wildstacker.api.upgrades.SpawnerUpgrade;
import com.bgsoftware.wildstacker.events.WildStackerSpawnersLoadEvent;
import com.bgsoftware.wildstacker.hooks.ClaimsProvider_FactionsUUID;
import com.bgsoftware.wildstacker.hooks.ClaimsProvider_MassiveFactions;
import com.bgsoftware.wildstacker.hooks.ClaimsProvider_WorldGuard;
import com.bgsoftware.wildstacker.hooks.CoreProtectHook;
import com.bgsoftware.wildstacker.hooks.CrazyEnchantmentsHook;
import com.bgsoftware.wildstacker.hooks.DataSerializer_NBTInjector;
import com.bgsoftware.wildstacker.hooks.EconomyHook;
import com.bgsoftware.wildstacker.hooks.FastAsyncWEHook;
import com.bgsoftware.wildstacker.hooks.JobsHook;
import com.bgsoftware.wildstacker.hooks.PluginHook_Novucs;
import com.bgsoftware.wildstacker.hooks.PluginHooks;
import com.bgsoftware.wildstacker.hooks.ProtocolLibHook;
import com.bgsoftware.wildstacker.hooks.SpawnersProvider_Default;
import com.bgsoftware.wildstacker.hooks.SpawnersProvider_MineableSpawners;
import com.bgsoftware.wildstacker.hooks.SpawnersProvider_SilkSpawners;
import com.bgsoftware.wildstacker.hooks.SuperiorSkyblockHook;
import com.bgsoftware.wildstacker.listeners.PaperListener;
import com.bgsoftware.wildstacker.listeners.ProvidersListener;
import com.bgsoftware.wildstacker.listeners.plugins.EchoPetListener;
import com.bgsoftware.wildstacker.listeners.plugins.MyPetListener;
import com.bgsoftware.wildstacker.listeners.plugins.MythicMobsListener;
import com.bgsoftware.wildstacker.listeners.plugins.SilkSpawnersListener;
import com.bgsoftware.wildstacker.utils.ServerVersion;
import com.bgsoftware.wildstacker.utils.threads.Executor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class ProvidersHandler {

    private SpawnersProvider spawnersProvider;
    private List<ClaimsProvider> claimsProviders;

    public ProvidersHandler(WildStackerPlugin plugin) {
        Executor.sync(() -> {
            WildStackerPlugin.log("Loading providers started...");
            long startTime = System.currentTimeMillis();

            loadSpawnersProvider();
            loadClaimsProvider();
            loadPluginHooks(plugin, null, true);

            Bukkit.getPluginManager().registerEvents(new ProvidersListener(plugin), plugin);

            fixConflicts(plugin);

            if (hasPaperEntityRemoveSupport()) {
                Bukkit.getPluginManager().registerEvents(new PaperListener(), plugin);
            }

            WildStackerPlugin.log(
                "Loading providers done (Took " + (System.currentTimeMillis() - startTime) + "ms)");
        }, 0L);

        Executor.sync(() -> {
            if (Bukkit.getPluginManager().isPluginEnabled("ASkyBlock") &&
                Bukkit.getPluginManager().getPlugin(
                    "ASkyBlock").getDescription().getAuthors().stream().noneMatch(
                    a -> a.contains("Ome_R"))) {
                WildStackerPlugin.log(
                    "&c#################################################################");
                WildStackerPlugin.log(
                    "&c##                                                             ##");
                WildStackerPlugin.log(
                    "&c## Seems like you're using ASkyBlock, but not the custom fork. ##");
                WildStackerPlugin.log(
                    "&c##            <The custom fork supports WildStacker>           ##");
                WildStackerPlugin.log(
                    "&c##           https://github.com/OmerBenGera/askyblock          ##");
                WildStackerPlugin.log(
                    "&c##                                                             ##");
                WildStackerPlugin.log(
                    "&c#################################################################");
            }
        }, 10L);

    }

    private void loadSpawnersProvider() {
        if (this.spawnersProvider != null) {
            return;
        }

        WildStackerSpawnersLoadEvent event = new WildStackerSpawnersLoadEvent();
        Bukkit.getPluginManager().callEvent(event);

        if (event.getSpawnersProvider() != null) {
            WildStackerPlugin.log(
                "Loading custom spawners provider: " + event.getSpawnersProvider());
            this.spawnersProvider = event.getSpawnersProvider();
            return;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("SilkSpawners") &&
            Bukkit.getPluginManager().getPlugin(
                "SilkSpawners").getDescription().getAuthors().contains("xGhOsTkiLLeRx")) {
            spawnersProvider = new SpawnersProvider_SilkSpawners();
        } else if (Bukkit.getPluginManager().isPluginEnabled("MineableSpawners")) {
            spawnersProvider = new SpawnersProvider_MineableSpawners();
        } else {
            spawnersProvider = new SpawnersProvider_Default();
        }
    }

    private void loadClaimsProvider() {
        claimsProviders = new ArrayList<>();
        if (Bukkit.getPluginManager().isPluginEnabled("Factions")) {
            if (Bukkit.getPluginManager().getPlugin(
                "Factions").getDescription().getAuthors().contains("drtshock")) {
                claimsProviders.add(new ClaimsProvider_FactionsUUID());
            } else {
                claimsProviders.add(new ClaimsProvider_MassiveFactions());
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            claimsProviders.add(new ClaimsProvider_WorldGuard());
        }
    }

    public void addClaimsProvider(ClaimsProvider provider) {
        this.claimsProviders.add(provider);
        WildStackerPlugin.log("Claims provider added: " + provider);
    }

    public void loadPluginHooks(WildStackerPlugin plugin, Plugin toCheck, boolean enable) {
        PluginManager pluginManager = plugin.getServer().getPluginManager();

        // Load listeners
        if (enable && isPlugin(toCheck, "Citizens") && pluginManager.isPluginEnabled("Citizens")) {
            PluginHooks.isCitizensEnabled = true;
        }
        if (enable && isPlugin(toCheck, "SilkSpawners") && pluginManager.isPluginEnabled(
            "SilkSpawners")) {
            pluginManager.registerEvents(new SilkSpawnersListener(plugin), plugin);
        }
        if (enable && isPlugin(toCheck, "MythicMobs") && pluginManager.isPluginEnabled(
            "MythicMobs")) {
            pluginManager.registerEvents(new MythicMobsListener(), plugin);
        }
        if (enable && isPlugin(toCheck, "LevelledMobs") && pluginManager.isPluginEnabled(
            "LevelledMobs")) {
            PluginHooks.isLevelledMobsEnabled = ServerVersion.isAtLeast(ServerVersion.v1_14);
        }
        if (enable && isPlugin(toCheck, "MyPet") && pluginManager.isPluginEnabled("MyPet")) {
            pluginManager.registerEvents(new MyPetListener(), plugin);
        }
        if (enable && isPlugin(toCheck, "EchoPet") && pluginManager.isPluginEnabled("EchoPet")) {
            pluginManager.registerEvents(new EchoPetListener(), plugin);
        }
        if (enable && isPlugin(toCheck, "CrazyEnchantments") && pluginManager.isPluginEnabled(
            "CrazyEnchantments")) {
            CrazyEnchantmentsHook.register();
        }

        //Load plugin hooks
        if (isPlugin(toCheck, "CoreProtect") && pluginManager.isPluginEnabled("CoreProtect")) {
            CoreProtectHook.setEnabled(enable);
        }
        if (isPlugin(toCheck, "WorldGuard") && pluginManager.isPluginEnabled("WorldGuard")) {
            PluginHooks.isWorldGuardEnabled = enable;
        }
        if (isPlugin(toCheck, "WildTools") && pluginManager.isPluginEnabled("WildTools")) {
            PluginHooks.isWildToolsEnabled = enable;
        }
        if (isPlugin(toCheck, "ProtocolLib") && pluginManager.isPluginEnabled("ProtocolLib")) {
            ProtocolLibHook.setEnabled(enable);
        }
        if (isPlugin(toCheck, "Vault") && pluginManager.isPluginEnabled("Vault")) {
            EconomyHook.setEnabled(enable);
        }
        if (isPlugin(toCheck, "MergedSpawner") && pluginManager.isPluginEnabled("MergedSpawner")) {
            PluginHooks.isMergedSpawnersEnabled = enable;
        }
        if (isPlugin(toCheck, "FastAsyncWorldEdit") && pluginManager.isPluginEnabled(
            "FastAsyncWorldEdit")) {
            PluginHooks.isFastAsyncWorldEditEnabled = enable;
        }
        if (enable && isPlugin(toCheck, "FactionsTop") && doesClassExist(
            "net.novucs.ftop.FactionsTopPlugin")) {
            PluginHook_Novucs.setEnabled(plugin);
        }
        if (isPlugin(toCheck, "Jobs") && pluginManager.isPluginEnabled("Jobs")) {
            JobsHook.setEnabled(enable);
        }
        if (enable && isPlugin(toCheck, "SuperiorSkyblock2") && pluginManager.isPluginEnabled(
            "SuperiorSkyblock2")) {
            SuperiorSkyblockHook.register(plugin);
        }
        if (enable && isPlugin(toCheck, "NBTInjector") && doesClassExist(
            "de.tr7zw.nbtinjector.NBTInjector")) {
            DataSerializer_NBTInjector.register(plugin);
        }
    }

    private boolean isPlugin(Plugin plugin, String pluginName) {
        return plugin == null || plugin.getName().equals(pluginName);
    }

    private void fixConflicts(WildStackerPlugin plugin) {
        List<String> messages = new ArrayList<>();
        if (PluginHooks.isEpicSpawnersEnabled) {
            messages.add("Detected EpicSpawners - Disabling spawners stacking...");
        }
        if (PluginHooks.isMergedSpawnersEnabled) {
            messages.add("Detected MergedSpawner - Disabling spawners stacking...");
        }
        if (PluginHooks.isFastAsyncWorldEditEnabled && plugin.getSettings().itemsStackingEnabled) {
            //WildStacker disabled the tick limiter for items.
            try {
                FastAsyncWEHook.disableTicksLimiter();
                messages.add("Detected FastAsyncWorldEdit - Disabling ticks limiter for items...");
            } catch (Throwable ignored) {
            }
        }

        if (!messages.isEmpty()) {
            WildStackerPlugin.log("");
            for (String msg : messages) {
                WildStackerPlugin.log(msg);
            }
            WildStackerPlugin.log("");
        }

    }

    /*
     * Spawners Provider
     */

    public ItemStack getSpawnerItem(EntityType entityType, int amount, SpawnerUpgrade spawnerUpgrade) {
        return spawnersProvider.getSpawnerItem(entityType, amount, spawnerUpgrade);
    }

    public EntityType getSpawnerType(ItemStack itemStack) {
        return spawnersProvider.getSpawnerType(itemStack);
    }

    public void handleSpawnerExplode(StackedSpawner stackedSpawner, Entity entity, Player ignite, int brokenAmount) {
        spawnersProvider.handleSpawnerExplode(stackedSpawner, entity, ignite, brokenAmount);
    }

    public void handleSpawnerBreak(StackedSpawner stackedSpawner, Player player, int brokenAmount, boolean breakMenu) {
        spawnersProvider.handleSpawnerBreak(stackedSpawner, player, brokenAmount, breakMenu);
    }

    public void handleSpawnerPlace(CreatureSpawner creatureSpawner, ItemStack itemStack) {
        spawnersProvider.handleSpawnerPlace(creatureSpawner, itemStack);
    }

    /*
     * Claims Provider
     */

    public boolean hasClaimAccess(Player player, Location location) {
        for (ClaimsProvider claimsProvider : claimsProviders) {
            if (!claimsProvider.hasClaimAccess(player, location)) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasPaperEntityRemoveSupport() {
        try {
            Class.forName("com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent");
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    private static boolean doesClassExist(String clazz) {
        try {
            Class.forName(clazz);
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

}
