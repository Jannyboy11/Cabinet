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

    public CompletionStage<Boolean> isMember(UUID player, String groupName);
    public CompletionStage<Boolean> isMember(String username, String groupName);

    public CompletionStage<Optional<String>> getPrimaryGroup(UUID player);
    public CompletionStage<Optional<String>> getPrimaryGroup(String username);

    public CompletionStage<Boolean> addMember(UUID player, String... groupNames);
    public CompletionStage<Boolean> addMember(String username, String... groupNames);

    public CompletionStage<Collection<String>> getGroups(UUID player);
    public CompletionStage<Collection<String>> getGroups(String username);

    public CompletionStage<Optional<CGroup>> getGroup(String groupName);

    public CompletionStage<CGroup> createOrUpdateGroup(CGroup group);
    public CompletionStage<Boolean> removeGroup(String groupName);

    //TODO overloads with normal permission strings
    public CompletionStage<Boolean> addGroupPermission(String groupName, CPermission... permissions);
    public CompletionStage<Boolean> removeGroupPermission(String groupName, CPermission... permissions);
    public CompletionStage<Boolean> hasGroupPermission(String groupName, CPermission... permissions);

}
