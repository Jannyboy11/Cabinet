package com.janboerman.cabinet.plugins.luckperms;

import com.janboerman.cabinet.api.BungeePermission;
import com.janboerman.cabinet.api.Permissions;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.node.types.RegexPermissionNode;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.AbstractMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

public class LuckPermsPermissions implements Permissions {

    private final ProxyServer proxyServer;
    private LuckPerms luckPerms;
    private UserManager userManager;

    public LuckPermsPermissions(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public String getName() {
        return "LuckPermsPermissions";
    }

    @Override
    public boolean tryInitialise() {
        Plugin plugin = proxyServer.getPluginManager().getPlugin("LuckPerms");
        if (plugin != null) {
            luckPerms = LuckPermsProvider.get();
            userManager = luckPerms.getUserManager();
        }
        return plugin != null;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean hasServerSupport() {
        return true;
    }

    @Override
    public boolean hasWorldSupport() {
        return true;
    }

    CompletionStage<User> loadUser(String username) {
        User user = userManager.getUser(username);
        return user != null
                ? CompletableFuture.completedFuture(user)
                : userManager.lookupUniqueId(username).thenCompose(this::loadUser);
    }

    CompletionStage<User> loadUser(UUID player) {
        User user = userManager.getUser(player);
        return user != null
                ? CompletableFuture.completedFuture(user)
                : userManager.loadUser(player);
    }

    static boolean hasPermission(User user, String permission) {
        PermissionNode node = PermissionNode.builder(permission).build();
        return user.data().contains(node, (n1, n2) -> {
            Node against = n1 == node ? n2 : n1;
            if (against instanceof PermissionNode) {
                PermissionNode permissionNode = (PermissionNode) against;
                return permissionNode.getValue() && permissionNode.getPermission().equals(permission);
            } else if (against instanceof RegexPermissionNode) {
                RegexPermissionNode regexPermissionNode = (RegexPermissionNode) against;
                return regexPermissionNode.getValue() && regexPermissionNode.getPattern().map(pattern -> pattern.matcher(permission).matches()).orElse(false);
            } else {
                return false;
            }
        }).asBoolean();
    }

    static boolean hasPermission(PermissionHolder permissionHolder, BungeePermission permission) {
        PermissionNode node = toPermissionNode(permission);
        return permissionHolder.data().contains(node, (n1, n2) -> {
            Node against = n1 == node ? n2 : n1;
            if (against instanceof PermissionNode || against instanceof RegexPermissionNode) {
                boolean equal = permission.isPositive() == against.getValue();
                if (permission.isServerSpecific()) equal &= against.getContexts().getValues(DefaultContextKeys.SERVER_KEY).containsAll(permission.getServers());
                if (permission.isWorldSpecific()) equal &= against.getContexts().getValues(DefaultContextKeys.WORLD_KEY).containsAll(permission.getWorlds());
                if (against instanceof PermissionNode) {
                    PermissionNode permissionNode = (PermissionNode) against;
                    equal &= permissionNode.getPermission().equals(permission.getValue());
                } else {
                    RegexPermissionNode regexPermissionNode = (RegexPermissionNode) against;
                    equal &= regexPermissionNode.getPattern().map(pattern -> pattern.matcher(permission.getValue()).matches()).orElse(false);
                }
                return equal;
            } else {
                return false;
            }
        }).asBoolean();
    }

    @Override
    public CompletionStage<Boolean> hasPermission(UUID player, String permission) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(player);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.hasPermission(permission));

        return loadUser(player).thenApply(user -> hasPermission(user, permission));
    }

    @Override
    public CompletionStage<Boolean> hasPermission(String username, String permission) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(username);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.hasPermission(permission));

        return loadUser(username).thenApply(user -> hasPermission(user, permission));
    }

    @Override
    public CompletionStage<Boolean> hasPermission(UUID player, BungeePermission permission) {
        return loadUser(player).thenApply(user -> hasPermission(user, permission));
    }

    @Override
    public CompletionStage<Boolean> hasPermission(String username, BungeePermission permission) {
        return loadUser(username).thenApply(user -> hasPermission(user, permission));
    }

    static PermissionNode toPermissionNode(BungeePermission permission) {
        PermissionNode.Builder builder = PermissionNode.builder();
        builder = builder.permission(permission.getValue());
        builder = builder.value(permission.isPositive());
        if (permission.hasDuration()) {
            builder = builder.expiry(permission.getEndingTimeStamp());
        }
        if (permission.isServerSpecific()) {
            for (String server : permission.getServers()) {
                builder = builder.withContext(DefaultContextKeys.SERVER_KEY, server);
            }
        }
        if (permission.isWorldSpecific()) {
            for (String world : permission.getWorlds()) {
                builder = builder.withContext(DefaultContextKeys.WORLD_KEY, world);
            }
        }
        return builder.build();
    }

    static boolean addPermission(PermissionHolder permissionHolder, BungeePermission permission) {
        return permissionHolder.data().add(toPermissionNode(permission)).wasSuccessful();
    }

    @Override
    public CompletionStage<Boolean> addPermission(UUID player, String permission) {
        return addPermission(player, new BungeePermission(permission));
    }

    @Override
    public CompletionStage<Boolean> addPermission(String username, String permission) {
        return addPermission(username, new BungeePermission(permission));
    }

    @Override
    public CompletionStage<Boolean> addPermission(UUID player, BungeePermission permission) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(player);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(addPermission(userManager.getUser(player), permission));

        return loadUser(player)
                .thenApply(user -> {
                    boolean success = addPermission(user, permission);
                    return new AbstractMap.SimpleImmutableEntry<>(user, success);
                })
                .thenCompose(entry -> userManager.saveUser(entry.getKey()).thenApply(unit -> entry.getValue()));
    }

    @Override
    public CompletionStage<Boolean> addPermission(String username, BungeePermission permission) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(username);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(addPermission(userManager.getUser(username), permission));

        return userManager.lookupUniqueId(username).thenCompose(uuid -> addPermission(uuid, permission));
    }

    @Override
    public CompletionStage<Boolean> removePermission(UUID player, String permission) {
        return loadUser(player)
                .thenApply(user -> {
                    boolean success = removePermission(user, permission);
                    return new AbstractMap.SimpleImmutableEntry<>(user, success);
                })
                .thenCompose(entry -> userManager.saveUser(entry.getKey()).thenApply(unit -> entry.getValue()));
    }

    @Override
    public CompletionStage<Boolean> removePermission(String username, String permission) {
        return userManager.lookupUniqueId(username).thenCompose(uuid -> removePermission(uuid, permission));
    }

    @Override
    public CompletionStage<Boolean> removePermission(UUID player, BungeePermission permission) {
        return loadUser(player).thenCompose(user -> {
            boolean result = removePermission(user, permission);
            return userManager.saveUser(user).thenApply(unit -> result);
        });
    }

    @Override
    public CompletionStage<Boolean> removePermission(String username, BungeePermission permission) {
        return loadUser(username).thenCompose(user -> {
            boolean result = removePermission(user, permission);
            return userManager.saveUser(user).thenApply(unit -> result);
        });
    }

    static boolean removePermission(PermissionHolder permissionHolder, BungeePermission permission) {
        AtomicBoolean value = new AtomicBoolean(false);
        permissionHolder.data().clear(node -> {
            if (node instanceof PermissionNode) {
                PermissionNode permissionNode = (PermissionNode) node;
                boolean check = Objects.equals(permissionNode.getPermission(), permission.getValue())
                        && (permission.isPositive() == node.getValue());
                if (permission.isServerSpecific()) check &= permissionNode.getContexts().getValues(DefaultContextKeys.SERVER_KEY).containsAll(permission.getServers());
                if (permission.isWorldSpecific()) check &= permissionNode.getContexts().getValues(DefaultContextKeys.WORLD_KEY).containsAll(permission.getWorlds());
                if (check) value.compareAndSet(false, true);
                return check;
            } else {
                return false;
            }
        });
        return value.get();
    }

    private static boolean removePermission(User user, String permission) {
        AtomicBoolean value = new AtomicBoolean(false);
        user.data().clear(node -> {
            if (node instanceof PermissionNode) {
                PermissionNode permissionNode = (PermissionNode) node;
                boolean check = Objects.equals(permissionNode.getPermission(), permission);
                if (check) value.compareAndSet(false, true);
                return check;
            } else {
                return false;
            }
        });
        return value.get();
    }
}
