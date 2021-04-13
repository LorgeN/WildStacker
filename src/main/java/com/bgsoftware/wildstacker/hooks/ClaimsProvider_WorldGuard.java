package com.bgsoftware.wildstacker.hooks;

import com.bgsoftware.wildstacker.api.hooks.ClaimsProvider;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class ClaimsProvider_WorldGuard implements ClaimsProvider {

    @Override
    public boolean hasClaimAccess(Player player, Location bukkitLocation) {
        return WorldGuardHook.hasClaimAccess(player, bukkitLocation);
    }
}
