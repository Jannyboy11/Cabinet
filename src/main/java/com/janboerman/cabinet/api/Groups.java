package com.janboerman.cabinet.api;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

//TODO ladder/track support? both LuckPerms and BungeePerms support it
//TODO inheritances support? both LuckPerms and BungeePerms support it

public interface Groups {

    public String getName();
    public boolean tryInitialise();
    public void onDisable();

    public boolean hasServerSupport();
    public boolean hasWorldSupport();
    public boolean hasChatSupport();

    public default CompletableFuture<Boolean> isMember(UUID player, String groupName) {
        return isMember(player, CContext.global(), groupName);
    }
    public default CompletableFuture<Boolean> isMember(String username, String groupName) {
        return isMember(username, CContext.global(), groupName);
    }
    public CompletableFuture<Boolean> isMember(UUID player, CContext context, String groupName);
    public CompletableFuture<Boolean> isMember(String username, CContext context, String groupName);

    public CompletableFuture<Optional<String>> getPrimaryGroup(UUID player);
    public CompletableFuture<Optional<String>> getPrimaryGroup(String username);

    public CompletableFuture<Boolean> addMember(UUID player, String... groupNames);
    public CompletableFuture<Boolean> addMember(String username, String... groupNames);

    public default CompletableFuture<Collection<String>> getGroups(UUID player, boolean includeParentGroups) {
        return getGroups(player, CContext.global(), includeParentGroups);
    }
    public default CompletableFuture<Collection<String>> getGroups(String username, boolean includeParentGroups) {
        return getGroups(username, CContext.global(), includeParentGroups);
    }
    public CompletableFuture<Collection<String>> getGroups(UUID player, CContext context, boolean includeParentGroups);
    public CompletableFuture<Collection<String>> getGroups(String username, CContext context, boolean includeParentGroups);

    public CompletableFuture<Optional<CGroup>> getGroup(String groupName);

    public CompletableFuture<CGroup> createOrUpdateGroup(CGroup group);
    public CompletableFuture<Boolean> removeGroup(String groupName);

    public default CompletableFuture<Boolean> groupAddPermission(String groupName, CPermission... permissions) {
        return groupAddPermission(groupName, CContext.global(), permissions);
    }
    public default CompletableFuture<Boolean> groupRemovePermission(String groupName, CPermission... permissions) {
        return groupRemovePermission(groupName, CContext.global(), permissions);
    }
    public default CompletableFuture<Boolean> groupHasPermission(String groupName, String... permissions) {
        return groupHasPermission(groupName, CContext.global(), permissions);
    }
    public CompletableFuture<Boolean> groupAddPermission(String groupName, CContext context, CPermission... permissions);
    public CompletableFuture<Boolean> groupRemovePermission(String groupName, CContext context, CPermission... permissions);
    public CompletableFuture<Boolean> groupHasPermission(String groupName, CContext context, String... permissions);

}
