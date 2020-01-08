package com.janboerman.cabinet.api;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

//TODO do I want regex permissions support? both BungeePerms and LuckPerms support them.

public interface Permissions {

    static CPermission[] toCPermission(String... permission) {
        return Arrays.stream(permission).map(CPermission::new).toArray(CPermission[]::new);
    }

    public String getName();
    public boolean tryInitialise();
    public void onDisable();

    public boolean hasServerSupport();
    public boolean hasWorldSupport();
    public boolean hasChatSupport();

    public CompletableFuture<Boolean> hasPermission(UUID player, String permission);
    public CompletableFuture<Boolean> hasPermission(String userName, String permission);
    public CompletableFuture<Boolean> hasPermission(UUID player, CContext context, String permission);
    public CompletableFuture<Boolean> hasPermission(String userName, CContext context, String permission);

    public default CompletableFuture<Boolean> addPermission(UUID player, String... permission) {
        return addPermission(player, CContext.global(), toCPermission(permission));
    }
    public default CompletableFuture<Boolean> addPermission(String userName, String... permission) {
        return addPermission(userName, CContext.global(), toCPermission(permission));
    }
    public CompletableFuture<Boolean> addPermission(UUID player, CContext context, CPermission... permission);
    public CompletableFuture<Boolean> addPermission(String userName, CContext context, CPermission... permission);

    public default CompletableFuture<Boolean> removePermission(UUID player, String... permission) {
        return removePermission(player, CContext.global(), toCPermission(permission));
    }
    public default CompletableFuture<Boolean> removePermission(String userName, String... permission) {
        return removePermission(userName, CContext.global(), toCPermission(permission));
    }
    public CompletableFuture<Boolean> removePermission(UUID player, CContext context, CPermission... permission);
    public CompletableFuture<Boolean> removePermission(String userName, CContext context, CPermission... permission);

    public CompletableFuture<Optional<String>> getPrefix(UUID player);
    public CompletableFuture<Optional<String>> getPrefix(String userName);
    public CompletableFuture<Optional<String>> getPrefixGlobal(UUID player);
    public CompletableFuture<Optional<String>> getPrefixGlobal(String userName);
    public CompletableFuture<Optional<String>> getPrefixOnServer(UUID player, String server);
    public CompletableFuture<Optional<String>> getPrefixOnServer(String userName, String server);
    public CompletableFuture<Optional<String>> getPrefixOnWorld(UUID player, String server, String world);
    public CompletableFuture<Optional<String>> getPrefixOnWorld(String userName, String server, String world);

    public CompletableFuture<Optional<String>> getSuffix(UUID player);
    public CompletableFuture<Optional<String>> getSuffix(String userName);
    public CompletableFuture<Optional<String>> getSuffixGlobal(UUID player);
    public CompletableFuture<Optional<String>> getSuffixGlobal(String userName);
    public CompletableFuture<Optional<String>> getSuffixOnServer(UUID player, String server);
    public CompletableFuture<Optional<String>> getSuffixOnServer(String userName, String server);
    public CompletableFuture<Optional<String>> getSuffixOnWorld(UUID player, String server, String world);
    public CompletableFuture<Optional<String>> getSuffixOnWorld(String userName, String server, String world);

    public CompletableFuture<Optional<String>> getDisplayName(UUID player);
    public CompletableFuture<Optional<String>> getDisplayName(String userName);
    public CompletableFuture<Optional<String>> getDisplayNameGlobal(UUID player);
    public CompletableFuture<Optional<String>> getDisplayNameGlobal(String userName);
    public CompletableFuture<Optional<String>> getDisplayNameOnServer(UUID player, String server);
    public CompletableFuture<Optional<String>> getDisplayNameOnServer(String userName, String server);
    public CompletableFuture<Optional<String>> getDisplayNameOnWorld(UUID player, String server, String world);
    public CompletableFuture<Optional<String>> getDisplayNameOnWorld(String userName, String server, String world);

    public CompletableFuture<Boolean> setPrefix(UUID player, CContext where, String prefix, int priority);
    public CompletableFuture<Boolean> setPrefix(String userName, CContext where, String prefix, int priority);
    public CompletableFuture<Boolean> setSuffix(UUID player, CContext where, String prefix, int priority);
    public CompletableFuture<Boolean> setSuffix(String userName, CContext where, String prefix, int priority);
    public CompletableFuture<Boolean> setDisplayName(UUID player, CContext where, String displayName, int priority);
    public CompletableFuture<Boolean> setDisplayName(String userName, CContext where, String displayName, int priority);

    public CompletableFuture<Boolean> removePrefix(UUID player, CContext where);
    public CompletableFuture<Boolean> removePrefix(String userName, CContext where);
    public default CompletableFuture<Boolean> removePrefix(UUID player, String prefix) {
        return removePrefix(player, CContext.global(), prefix);
    }
    public default CompletableFuture<Boolean> removePrefix(String userName, String prefix) {
        return removePrefix(userName, CContext.global(), prefix);
    }
    public CompletableFuture<Boolean> removePrefix(UUID player, CContext where, String prefix);
    public CompletableFuture<Boolean> removePrefix(String userName, CContext where, String prefix);

    public CompletableFuture<Boolean> removeSuffix(UUID player, CContext where);
    public CompletableFuture<Boolean> removeSuffix(String userName, CContext where);
    public default CompletableFuture<Boolean> removeSuffix(UUID player, String suffix) {
        return removePrefix(player, CContext.global(), suffix);
    }
    public default CompletableFuture<Boolean> removeSuffix(String userName, String suffix) {
        return removePrefix(userName, CContext.global(), suffix);
    }
    public CompletableFuture<Boolean> removeSuffix(UUID player, CContext where, String suffix);
    public CompletableFuture<Boolean> removeSuffix(String userName, CContext where, String suffix);

    public CompletableFuture<Boolean> removeDisplayName(UUID player, CContext where);
    public CompletableFuture<Boolean> removeDisplayName(String userName, CContext where);
    public default CompletableFuture<Boolean> removeDisplayName(UUID player, String displayName) {
        return removePrefix(player, CContext.global(), displayName);
    }
    public default CompletableFuture<Boolean> removeDisplayName(String userName, String displayName) {
        return removePrefix(userName, CContext.global(), displayName);
    }
    public CompletableFuture<Boolean> removeDisplayName(UUID player, CContext where, String displayName);
    public CompletableFuture<Boolean> removeDisplayName(String userName, CContext where, String displayName);
}
