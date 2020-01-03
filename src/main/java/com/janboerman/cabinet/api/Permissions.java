package com.janboerman.cabinet.api;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

//TODO do I want regex permissions support? both BungeePerms and LuckPerms support them.

public interface Permissions {

    public String getName();
    public boolean tryInitialise();
    public void onDisable();

    public boolean hasServerSupport();
    public boolean hasWorldSupport();
    public boolean hasChatSupport();

    //TODO varArgs all the things!
    public CompletionStage<Boolean> hasPermission(UUID player, String permission);
    public CompletionStage<Boolean> hasPermission(String username, String permission);
    public CompletionStage<Boolean> hasPermission(UUID player, CPermission permission);
    public CompletionStage<Boolean> hasPermission(String username, CPermission permission);

    public CompletionStage<Boolean> addPermission(UUID player, String permission);
    public CompletionStage<Boolean> addPermission(String username, String permission);
    public CompletionStage<Boolean> addPermission(UUID player, CPermission permission);
    public CompletionStage<Boolean> addPermission(String username, CPermission permission);

    public CompletionStage<Boolean> removePermission(UUID player, String permission);
    public CompletionStage<Boolean> removePermission(String username, String permission);
    public CompletionStage<Boolean> removePermission(UUID player, CPermission permission);
    public CompletionStage<Boolean> removePermission(String username, CPermission permission);

    public CompletionStage<String> getPrefix(UUID player);
    public CompletionStage<String> getPrefix(String username);
    public CompletionStage<String> getSuffix(UUID player);
    public CompletionStage<String> getSuffix(String username);
    public CompletionStage<String> getDisplayName(UUID player);
    public CompletionStage<String> getDisplayName(String username);
}
