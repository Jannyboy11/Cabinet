package com.janboerman.cabinet.api;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

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
    public ChatSupport hasChatSupport();

    public CompletionStage<Boolean> hasPermission(UUID player, String permission);
    public CompletionStage<Boolean> hasPermission(String userName, String permission);
    public CompletionStage<Boolean> hasPermission(UUID player, CContext context, String permission);
    public CompletionStage<Boolean> hasPermission(String userName, CContext context, String permission);

    public default CompletionStage<Boolean> addPermission(UUID player, String... permission) {
        return addPermission(player, CContext.global(), toCPermission(permission));
    }
    public default CompletionStage<Boolean> addPermission(String userName, String... permission) {
        return addPermission(userName, CContext.global(), toCPermission(permission));
    }
    public CompletionStage<Boolean> addPermission(UUID player, CContext context, CPermission... permission);
    public CompletionStage<Boolean> addPermission(String userName, CContext context, CPermission... permission);

    public default CompletionStage<Boolean> removePermission(UUID player, String... permission) {
        return removePermission(player, CContext.global(), toCPermission(permission));
    }
    public default CompletionStage<Boolean> removePermission(String userName, String... permission) {
        return removePermission(userName, CContext.global(), toCPermission(permission));
    }
    public CompletionStage<Boolean> removePermission(UUID player, CContext context, CPermission... permission);
    public CompletionStage<Boolean> removePermission(String userName, CContext context, CPermission... permission);

    public CompletionStage<Optional<String>> getPrefix(UUID player);
    public CompletionStage<Optional<String>> getPrefix(String userName);
    public CompletionStage<Optional<String>> getPrefixGlobal(UUID player);
    public CompletionStage<Optional<String>> getPrefixGlobal(String userName);
    public CompletionStage<Optional<String>> getPrefixOnServer(UUID player, String server);
    public CompletionStage<Optional<String>> getPrefixOnServer(String userName, String server);
    public CompletionStage<Optional<String>> getPrefixOnWorld(UUID player, String server, String world);
    public CompletionStage<Optional<String>> getPrefixOnWorld(String userName, String server, String world);

    public CompletionStage<Optional<String>> getSuffix(UUID player);
    public CompletionStage<Optional<String>> getSuffix(String userName);
    public CompletionStage<Optional<String>> getSuffixGlobal(UUID player);
    public CompletionStage<Optional<String>> getSuffixGlobal(String userName);
    public CompletionStage<Optional<String>> getSuffixOnServer(UUID player, String server);
    public CompletionStage<Optional<String>> getSuffixOnServer(String userName, String server);
    public CompletionStage<Optional<String>> getSuffixOnWorld(UUID player, String server, String world);
    public CompletionStage<Optional<String>> getSuffixOnWorld(String userName, String server, String world);

    public CompletionStage<Optional<String>> getDisplayName(UUID player);
    public CompletionStage<Optional<String>> getDisplayName(String userName);
    public CompletionStage<Optional<String>> getDisplayNameGlobal(UUID player);
    public CompletionStage<Optional<String>> getDisplayNameGlobal(String userName);
    public CompletionStage<Optional<String>> getDisplayNameOnServer(UUID player, String server);
    public CompletionStage<Optional<String>> getDisplayNameOnServer(String userName, String server);
    public CompletionStage<Optional<String>> getDisplayNameOnWorld(UUID player, String server, String world);
    public CompletionStage<Optional<String>> getDisplayNameOnWorld(String userName, String server, String world);

    public CompletionStage<Boolean> setPrefix(UUID player, CContext where, String prefix, int priority);
    public CompletionStage<Boolean> setPrefix(String userName, CContext where, String prefix, int priority);
    public CompletionStage<Boolean> setSuffix(UUID player, CContext where, String prefix, int priority);
    public CompletionStage<Boolean> setSuffix(String userName, CContext where, String prefix, int priority);
    public CompletionStage<Boolean> setDisplayName(UUID player, CContext where, String displayName, int priority);
    public CompletionStage<Boolean> setDisplayName(String userName, CContext where, String displayName, int priority);

    public CompletionStage<Boolean> removePrefix(UUID player, CContext where);
    public CompletionStage<Boolean> removePrefix(String userName, CContext where);
    public default CompletionStage<Boolean> removePrefix(UUID player, String prefix) {
        return removePrefix(player, CContext.global(), prefix);
    }
    public default CompletionStage<Boolean> removePrefix(String userName, String prefix) {
        return removePrefix(userName, CContext.global(), prefix);
    }
    public CompletionStage<Boolean> removePrefix(UUID player, CContext where, String prefix);
    public CompletionStage<Boolean> removePrefix(String userName, CContext where, String prefix);

    public CompletionStage<Boolean> removeSuffix(UUID player, CContext where);
    public CompletionStage<Boolean> removeSuffix(String userName, CContext where);
    public default CompletionStage<Boolean> removeSuffix(UUID player, String suffix) {
        return removePrefix(player, CContext.global(), suffix);
    }
    public default CompletionStage<Boolean> removeSuffix(String userName, String suffix) {
        return removePrefix(userName, CContext.global(), suffix);
    }
    public CompletionStage<Boolean> removeSuffix(UUID player, CContext where, String suffix);
    public CompletionStage<Boolean> removeSuffix(String userName, CContext where, String suffix);

    public CompletionStage<Boolean> removeDisplayName(UUID player, CContext where);
    public CompletionStage<Boolean> removeDisplayName(String userName, CContext where);
    public default CompletionStage<Boolean> removeDisplayName(UUID player, String displayName) {
        return removePrefix(player, CContext.global(), displayName);
    }
    public default CompletionStage<Boolean> removeDisplayName(String userName, String displayName) {
        return removePrefix(userName, CContext.global(), displayName);
    }
    public CompletionStage<Boolean> removeDisplayName(UUID player, CContext where, String displayName);
    public CompletionStage<Boolean> removeDisplayName(String userName, CContext where, String displayName);
}
