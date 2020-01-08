package com.janboerman.cabinet;

import com.janboerman.cabinet.api.Groups;
import com.janboerman.cabinet.api.Permissions;
import com.janboerman.cabinet.plugins.bungeeperms.BungeePermsGroups;
import com.janboerman.cabinet.plugins.bungeeperms.BungeePermsPermissions;
import com.janboerman.cabinet.plugins.luckperms.LuckPermsGroups;
import com.janboerman.cabinet.plugins.luckperms.LuckPermsPermissions;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.*;

//TODO support RedisBungee? What about players who are on a different proxy?

public class CabinetPlugin extends Plugin {

    private final Map<String, Permissions> permissionProviders = new LinkedHashMap<>();
    private final Map<String, Groups> groupsProviders = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        boolean success = tryHookLuckPerms();
        if (!success) success = tryHookBungeePerms();
        //TODO more bungeecord permission plugin hooks

        //ideally I would call these hook methods again if one of the plugins enables,
        //but bungeecord does not have a PluginEnableEvent. It should not ever be a problem though because
        //Cabinet soft-depends on LuckPerms and BungeePerms.
    }

    public boolean tryHookLuckPerms() {
        ProxyServer proxyServer = getProxy();
        boolean doesHook = proxyServer.getPluginManager().getPlugin("LuckPerms") != null;
        if (doesHook) {
            LuckPermsPermissions luckPermsPermissions = new LuckPermsPermissions(proxyServer);
            LuckPermsGroups luckPermsGroups = new LuckPermsGroups(proxyServer, luckPermsPermissions);
            registerPermissionProvider(luckPermsPermissions);
            registerGroupProvider(luckPermsGroups);
        }
        return doesHook;
    }

    public boolean tryHookBungeePerms() {
        ProxyServer proxyServer = getProxy();
        boolean doesHook = proxyServer.getPluginManager().getPlugin("BungeePerms") != null;
        if (doesHook) {
            BungeePermsPermissions bungeePermsPermissions = new BungeePermsPermissions(proxyServer);
            BungeePermsGroups bungeePermsGroups = new BungeePermsGroups(proxyServer);
            registerPermissionProvider(bungeePermsPermissions);
            registerGroupProvider(bungeePermsGroups);
        }
        return doesHook;
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
            getLogger().info("Registered Permissions provider: " + provider.getName());
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
