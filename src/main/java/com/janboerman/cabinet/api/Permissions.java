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

    public CompletionStage<Boolean> hasPermission(UUID player, String permission);
    public CompletionStage<Boolean> hasPermission(String username, String permission);
    public CompletionStage<Boolean> hasPermission(UUID player, BungeePermission permission);
    public CompletionStage<Boolean> hasPermission(String username, BungeePermission permission);

    public CompletionStage<Boolean> addPermission(UUID player, String permission);
    public CompletionStage<Boolean> addPermission(String username, String permission);
    public CompletionStage<Boolean> addPermission(UUID player, BungeePermission permission);
    public CompletionStage<Boolean> addPermission(String username, BungeePermission permission);

    public CompletionStage<Boolean> removePermission(UUID player, String permission);
    public CompletionStage<Boolean> removePermission(String username, String permission);
    public CompletionStage<Boolean> removePermission(UUID player, BungeePermission permission);
    public CompletionStage<Boolean> removePermission(String username, BungeePermission permission);

    //TODO group- prefix and suffix in separate Chat api?
}
