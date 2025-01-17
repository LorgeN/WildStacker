package com.bgsoftware.wildstacker;

import com.bgsoftware.wildstacker.api.WildStacker;
import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.command.CommandsHandler;
import com.bgsoftware.wildstacker.handlers.DataHandler;
import com.bgsoftware.wildstacker.handlers.LootHandler;
import com.bgsoftware.wildstacker.handlers.ProvidersHandler;
import com.bgsoftware.wildstacker.handlers.SettingsHandler;
import com.bgsoftware.wildstacker.handlers.SystemHandler;
import com.bgsoftware.wildstacker.handlers.UpgradesHandler;
import com.bgsoftware.wildstacker.listeners.BarrelsListener;
import com.bgsoftware.wildstacker.listeners.BucketsListener;
import com.bgsoftware.wildstacker.listeners.ChunksListener;
import com.bgsoftware.wildstacker.listeners.EntitiesListener;
import com.bgsoftware.wildstacker.listeners.ItemsListener;
import com.bgsoftware.wildstacker.listeners.MenusListener;
import com.bgsoftware.wildstacker.listeners.NoClaimConflictListener;
import com.bgsoftware.wildstacker.listeners.ShulkerOversizedPatch;
import com.bgsoftware.wildstacker.listeners.SpawnersListener;
import com.bgsoftware.wildstacker.listeners.StewListener;
import com.bgsoftware.wildstacker.listeners.ToolsListener;
import com.bgsoftware.wildstacker.listeners.WorldsListener;
import com.bgsoftware.wildstacker.listeners.events.EventsListener;
import com.bgsoftware.wildstacker.menu.EditorMenu;
import com.bgsoftware.wildstacker.metrics.Metrics;
import com.bgsoftware.wildstacker.nms.NMSAdapter;
import com.bgsoftware.wildstacker.nms.NMSHolograms;
import com.bgsoftware.wildstacker.nms.NMSSpawners;
import com.bgsoftware.wildstacker.utils.ServerVersion;
import com.bgsoftware.wildstacker.utils.entity.EntityStorage;
import com.bgsoftware.wildstacker.utils.items.GlowEnchantment;
import com.bgsoftware.wildstacker.utils.threads.Executor;
import com.bgsoftware.wildstacker.utils.threads.StackService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.lang.reflect.Field;

public final class WildStackerPlugin extends JavaPlugin implements WildStacker {

    private static WildStackerPlugin plugin;

    private SettingsHandler settingsHandler;
    private SystemHandler systemManager;
    private UpgradesHandler upgradesHandler;
    private DataHandler dataHandler;
    private ProvidersHandler providersHandler;
    private LootHandler lootHandler;

    private NMSAdapter nmsAdapter;
    private NMSHolograms nmsHolograms;
    private NMSSpawners nmsSpawners;

    private boolean shouldEnable = true;

    @Override
    public void onLoad() {
        plugin = this;
        new Metrics(this);

        this.loadNMSAdapter();
        this.loadAPI();

        if (!shouldEnable) {
            log("&cThere was an error while loading the plugin.");
        }
    }

    @Override
    public void onEnable() {
        if (!shouldEnable) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        log("******** ENABLE START ********");

        GlowEnchantment.registerEnchantment();

        dataHandler = new DataHandler(this);
        systemManager = new SystemHandler(this);
        upgradesHandler = new UpgradesHandler();
        settingsHandler = new SettingsHandler(this);
        providersHandler = new ProvidersHandler(this);
        lootHandler = new LootHandler(this);

        EditorMenu.init(this);

        Locale.reload();

        PluginManager pluginManager = getServer().getPluginManager();

        if (ServerVersion.isAtLeast(ServerVersion.v1_8)) {
            pluginManager.registerEvents(new BarrelsListener(this), this);
        }

        pluginManager.registerEvents(new BucketsListener(this), this);
        pluginManager.registerEvents(new ChunksListener(this), this);
        pluginManager.registerEvents(new EntitiesListener(this), this);
        pluginManager.registerEvents(new ItemsListener(this), this);
        pluginManager.registerEvents(new MenusListener(), this);
        pluginManager.registerEvents(new NoClaimConflictListener(this), this);
        pluginManager.registerEvents(new ShulkerOversizedPatch(), this);
        pluginManager.registerEvents(new SpawnersListener(this), this);
        pluginManager.registerEvents(new StewListener(this), this);
        pluginManager.registerEvents(new ToolsListener(this), this);
        pluginManager.registerEvents(new WorldsListener(this), this);

        EventsListener.register(this);

        CommandsHandler commandsHandler = new CommandsHandler(this);
        getCommand("stacker").setExecutor(commandsHandler);
        getCommand("stacker").setTabCompleter(commandsHandler);

        log("******** ENABLE DONE ********");
    }

    @Override
    public void onDisable() {
        log("Cancelling tasks...");

        try {
            Bukkit.getScheduler().cancelAllTasks();
        } catch (Throwable ex) {
            try {
                BukkitScheduler.class.getMethod("cancelTasks", Plugin.class).invoke(
                    Bukkit.getScheduler(), this);
            } catch (Exception ignored) {
            }
        }

        log("Shutting down stacking service...");

        StackService.stop();

        if (shouldEnable) {
            log("Performing entity&items save");

            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    systemManager.handleChunkUnload(chunk);
                }
            }

            //We need to save the entire database
            systemManager.performCacheSave();

            Executor.stopData();

            log("Clearing database...");
            //We need to close the connection
            dataHandler.clearDatabase();
        }

        log("Stopping executor...");

        Executor.stop();

        EntityStorage.clearCache();
    }

    private void loadAPI() {
        try {
            Field instance = WildStackerAPI.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, this);
        } catch (Exception ex) {
            log("Failed to set-up API - disabling plugin...");
            ex.printStackTrace();
            shouldEnable = false;
        }
    }

    private void loadNMSAdapter() {
        log("Loading NMS adapters...");

        String bukkitVersion = ServerVersion.getBukkitVersion();

        try {
            if (this.nmsAdapter == null) {
                log("Loading " + bukkitVersion + " NMS adapter...");
                this.nmsAdapter = (NMSAdapter) Class.forName(
                    "com.bgsoftware.wildstacker.nms.NMSAdapter_" + bukkitVersion).newInstance();

            }

            if (this.nmsHolograms == null) {
                log("Loading " + bukkitVersion + " NMS holograms...");
                this.nmsHolograms = (NMSHolograms) Class.forName(
                    "com.bgsoftware.wildstacker.nms.NMSHolograms_" + bukkitVersion).newInstance();
            }

            if (this.nmsSpawners == null) {
                log("Loading " + bukkitVersion + " NMS spawners...");
                this.nmsSpawners = (NMSSpawners) Class.forName(
                    "com.bgsoftware.wildstacker.nms.NMSSpawners_" + bukkitVersion).newInstance();
            }
        } catch (Exception ex) {
            log("WildStacker doesn't support " + bukkitVersion + " - shutting down...");
            shouldEnable = false;
        }
    }

    public void setNmsAdapter(NMSAdapter nmsAdapter) {
        this.nmsAdapter = nmsAdapter;
    }

    public void setNmsHolograms(NMSHolograms nmsHolograms) {
        this.nmsHolograms = nmsHolograms;
    }

    public void setNmsSpawners(NMSSpawners nmsSpawners) {
        this.nmsSpawners = nmsSpawners;
    }

    public NMSAdapter getNMSAdapter() {
        return nmsAdapter;
    }

    public NMSHolograms getNMSHolograms() {
        return nmsHolograms;
    }

    public NMSSpawners getNMSSpawners() {
        return nmsSpawners;
    }

    public LootHandler getLootHandler() {
        return lootHandler;
    }

    public void setLootHandler(LootHandler lootHandler) {
        this.lootHandler = lootHandler;
    }

    public ProvidersHandler getProviders() {
        return providersHandler;
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    @Override
    public SystemHandler getSystemManager() {
        return systemManager;
    }

    @Override
    public UpgradesHandler getUpgradesManager() {
        return upgradesHandler;
    }

    public SettingsHandler getSettings() {
        return settingsHandler;
    }

    public void setSettings(SettingsHandler settingsHandler) {
        this.settingsHandler = settingsHandler;
    }

    public static void log(String message) {
        message = ChatColor.translateAlternateColorCodes('&', message);
        if (message.contains(ChatColor.COLOR_CHAR + "")) {
            Bukkit.getConsoleSender().sendMessage("[WildStacker] " + message);
        } else {
            plugin.getLogger().info(message);
        }
    }

    public static WildStackerPlugin getPlugin() {
        return plugin;
    }

}
