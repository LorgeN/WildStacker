package com.bgsoftware.wildstacker.events;

import com.bgsoftware.wildstacker.api.hooks.SpawnersProvider;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WildStackerSpawnersLoadEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private SpawnersProvider spawnersProvider;

    public SpawnersProvider getSpawnersProvider() {
        return spawnersProvider;
    }

    public void setSpawnersProvider(SpawnersProvider spawnersProvider) {
        this.spawnersProvider = spawnersProvider;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
