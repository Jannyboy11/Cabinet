package com.janboerman.cabinet.api;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

//TODO ladder/track support? both LuckPerms and BungeePerms support it
//TODO inheritances support? both LuckPerms and BungeePerms support it

public interface Groups {

    public String getName();
    public boolean tryInitialise();
    public void onDisable();

    public boolean hasServerSupport();
    public boolean hasWorldSupport();
    public boolean hasChatSupport();

    public default CompletionStage<Boolean> isMember(UUID player, String groupName) {
        return isMember(player, CContext.global(), groupName);
    }
    public default CompletionStage<Boolean> isMember(String username, String groupName) {
        return isMember(username, CContext.global(), groupName);
    }
    public CompletionStage<Boolean> isMember(UUID player, CContext context, String groupName);
    public CompletionStage<Boolean> isMember(String username, CContext context, String groupName);

    public CompletionStage<Optional<String>> getPrimaryGroup(UUID player);
    public CompletionStage<Optional<String>> getPrimaryGroup(String username);

    public CompletionStage<Boolean> addMember(UUID player, String... groupNames);
    public CompletionStage<Boolean> addMember(String username, String... groupNames);

    public default CompletionStage<Collection<String>> getGroups(UUID player, boolean includeParentGroups) {
        return getGroups(player, CContext.global(), includeParentGroups);
    }
    public default CompletionStage<Collection<String>> getGroups(String username, boolean includeParentGroups) {
        return getGroups(username, CContext.global(), includeParentGroups);
    }
    public CompletionStage<Collection<String>> getGroups(UUID player, CContext context, boolean includeParentGroups);
    public CompletionStage<Collection<String>> getGroups(String username, CContext context, boolean includeParentGroups);

    public CompletionStage<Optional<CGroup>> getGroup(String groupName);

    public CompletionStage<CGroup> createOrUpdateGroup(CGroup group);
    public CompletionStage<Boolean> removeGroup(String groupName);

    public default CompletionStage<Boolean> groupAddPermission(String groupName, CPermission... permissions) {
        return groupAddPermission(groupName, CContext.global(), permissions);
    }
    public default CompletionStage<Boolean> groupRemovePermission(String groupName, CPermission... permissions) {
        return groupRemovePermission(groupName, CContext.global(), permissions);
    }
    public default CompletionStage<Boolean> groupHasPermission(String groupName, String... permissions) {
        return groupHasPermission(groupName, CContext.global(), permissions);
    }
    public CompletionStage<Boolean> groupAddPermission(String groupName, CContext context, CPermission... permissions);
    public CompletionStage<Boolean> groupRemovePermission(String groupName, CContext context, CPermission... permissions);
    public CompletionStage<Boolean> groupHasPermission(String groupName, CContext context, String... permissions);

}
