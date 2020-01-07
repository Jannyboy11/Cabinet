package com.janboerman.cabinet.plugins;

import com.janboerman.cabinet.api.CContext;
import com.janboerman.cabinet.api.Permissions;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class PluginPermissions implements Permissions {

    private Plugin plugin;
    private final String name;
    protected final ProxyServer proxyServer;

    protected PluginPermissions(String name, ProxyServer proxyServer) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.proxyServer = Objects.requireNonNull(proxyServer, "proxy cannot be null");
    }

    protected Plugin getPlugin() {
        return plugin;
    }

    public abstract void initialise();

    @Override
    public void onDisable() {
    }

    @Override
    public boolean tryInitialise() {
        plugin = proxyServer.getPluginManager().getPlugin(name);
        if (plugin != null) {
            initialise();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getName() {
        return name + "Permissions";
    }

    public CompletionStage<Boolean> hasPermission(UUID player, String permission) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(player);
        if (player != null) return CompletableFuture.completedFuture(proxiedPlayer.hasPermission(permission));

        return hasPermission(player, CContext.global(), permission);
    }

    public CompletionStage<Boolean> hasPermission(String userName, String permission) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(userName);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.hasPermission(permission));

        return hasPermission(userName, CContext.global(), permission);
    }
}
