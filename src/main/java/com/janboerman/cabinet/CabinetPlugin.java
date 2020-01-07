package com.janboerman.cabinet;

import com.janboerman.cabinet.api.Groups;
import com.janboerman.cabinet.api.Permissions;
import com.janboerman.cabinet.plugins.bungeeperms.BungeePermsGroups;
import com.janboerman.cabinet.plugins.bungeeperms.BungeePermsPermissions;
import com.janboerman.cabinet.plugins.luckperms.LuckPermsGroups;
import com.janboerman.cabinet.plugins.luckperms.LuckPermsPermissions;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.event.EventHandler;

import java.util.*;

public class CabinetPlugin extends Plugin implements Listener {

    private final Map<String, Permissions> permissionProviders = new LinkedHashMap<>();
    private final Map<String, Groups> groupsProviders = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        ProxyServer proxyServer = getProxy();
        PluginManager pluginManager = proxyServer.getPluginManager();

        if (pluginManager.getPlugin("LuckPerms") != null) {
            LuckPermsPermissions luckPermsPermissions = new LuckPermsPermissions(proxyServer);
            LuckPermsGroups luckPermsGroups = new LuckPermsGroups(proxyServer, luckPermsPermissions);
            registerPermissionProvider(luckPermsPermissions);
            registerGroupProvider(luckPermsGroups);
        }

        if (pluginManager.getPlugin("BungeePerms") != null) {
            BungeePermsPermissions bungeePermsPermissions = new BungeePermsPermissions(proxyServer);
            BungeePermsGroups bungeePermsGroups = new BungeePermsGroups(proxyServer);
            registerPermissionProvider(bungeePermsPermissions);
            registerGroupProvider(bungeePermsGroups);
        }
    }

    @Override
    public void onDisable() {
        groupsProviders.values().forEach(Groups::onDisable);
        permissionProviders.values().forEach(Permissions::onDisable);
        permissionProviders.clear();
        groupsProviders.clear();
    }

    public boolean registerPermissionProvider(Permissions provider) {
        boolean result = provider.tryInitialise() && permissionProviders.putIfAbsent(provider.getName(), Objects.requireNonNull(provider, "Permissions provider cannot be null!")) == null;
        if (result) {
            getLogger().info("Registered permissions provider: " + provider.getName());
        }
        return result;
    }

    public boolean registerGroupProvider(Groups provider) {
        boolean result = provider.tryInitialise() && groupsProviders.putIfAbsent(provider.getName(), Objects.requireNonNull(provider, "Groups provider cannot be null!")) == null;
        if (result) {
            getLogger().info("Registered Groups provider: " + provider.getName());
        }
        return result;
    }

    public Optional<? extends Permissions> getPermissionsProvider() {
        if (permissionProviders.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(permissionProviders.values().iterator().next());
        }
    }

    public Collection<? extends Permissions> getPermissionsProviders() {
        return Collections.unmodifiableCollection(permissionProviders.values());
    }

    public Optional<? extends Groups> getGroupsProvider() {
        if (groupsProviders.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(groupsProviders.values().iterator().next());
        }
    }

    public Collection<? extends Groups> getGroupsProviders() {
        return Collections.unmodifiableCollection(groupsProviders.values());
    }
}
