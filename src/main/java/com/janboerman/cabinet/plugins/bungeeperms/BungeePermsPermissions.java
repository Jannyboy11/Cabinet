package com.janboerman.cabinet.plugins.bungeeperms;

import com.janboerman.cabinet.api.BungeePermission;
import com.janboerman.cabinet.api.Permissions;
import com.janboerman.cabinet.util.Executors;
import net.alpenblock.bungeeperms.BungeePerms;
import net.alpenblock.bungeeperms.BungeePermsAPI;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class BungeePermsPermissions implements Permissions {

    private final ProxyServer proxyServer;
    private Plugin plugin;
    private BungeePerms bungeePerms;
    private Executor executor;

    public BungeePermsPermissions(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public String getName() {
        return "BungeePermsPermissions";
    }

    @Override
    public boolean tryInitialise() {
        plugin = proxyServer.getPluginManager().getPlugin("BungeePerms");
        if (plugin != null) {
            bungeePerms = BungeePerms.getInstance();
            executor = Executors.asyncExecutor(plugin);
        }
        return plugin != null;
    }

    @Override
    public boolean hasServerSupport() {
        return true;
    }

    @Override
    public boolean hasWorldSupport() {
        return true;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public CompletionStage<Boolean> hasPermission(UUID player, String permission) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(player);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.hasPermission(permission));

        return CompletableFuture.supplyAsync(() -> BungeePermsAPI.userHasPermission(player.toString(), permission, null, null), executor);
    }

    @Override
    public CompletionStage<Boolean> hasPermission(String username, String permission) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(username);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.hasPermission(permission));

        return CompletableFuture.supplyAsync(() -> BungeePermsAPI.userHasPermission(username, permission, null, null), executor);
    }

    @Override
    public CompletionStage<Boolean> hasPermission(UUID player, BungeePermission permission) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            if (permission.isServerSpecific()) {
                for (String server : permission.getServers()) {
                    if (permission.isWorldSpecific()) {
                        for (String world : permission.getWorlds()) {
                            result &= BungeePermsAPI.userHasPermission(player.toString(), permission.getValue(), server, world);
                        }
                    } else {
                        result &= BungeePermsAPI.userHasPermission(player.toString(), permission.getValue(), server, null);
                    }
                }
            } else {
                //bungeeperms does not support server-unspecific, world-specific permissions
                result &= BungeePermsAPI.userHasPermission(player.toString(), permission.getValue(), null, null);
            }
            return result;
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> hasPermission(String username, BungeePermission permission) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            if (permission.isServerSpecific()) {
                for (String server : permission.getServers()) {
                    if (permission.isWorldSpecific()) {
                        for (String world : permission.getWorlds()) {
                            result &= BungeePermsAPI.userHasPermission(username, permission.getValue(), server, world);
                        }
                    } else {
                        result &= BungeePermsAPI.userHasPermission(username, permission.getValue(), server, null);
                    }
                }
            } else {
                //bungeeperms does not support server-unspecific, world-specific permissions
                result &= BungeePermsAPI.userHasPermission(username, permission.getValue(), null, null);
            }
            return result;
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> addPermission(UUID player, String permission) {
        return CompletableFuture.supplyAsync(() -> BungeePermsAPI.userAdd(player.toString(), permission, null, null), executor);
    }

    @Override
    public CompletionStage<Boolean> addPermission(String username, String permission) {
        return CompletableFuture.supplyAsync(() -> BungeePermsAPI.userAdd(username, permission, null, null), executor);
    }

    @Override
    public CompletionStage<Boolean> addPermission(UUID player, BungeePermission permission) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            if (permission.hasDuration()) {
                long now = System.currentTimeMillis();
                int timeDifferenceSeconds = (int) ((permission.getEndingTimeStamp().toEpochMilli() - now) / 1000);

                if (permission.isServerSpecific()) {
                    for (String server : permission.getServers()) {
                        if (permission.isWorldSpecific()) {
                            for (String world : permission.getWorlds()) {
                                result &= BungeePermsAPI.userTimedAdd(player.toString(), permission.getValue(), server, world, new Date(now), timeDifferenceSeconds);
                            }
                        } else {
                            result &= BungeePermsAPI.userTimedAdd(player.toString(), permission.getValue(), server, null, new Date(now), timeDifferenceSeconds);
                        }
                    }
                } else {
                    result &= BungeePermsAPI.userTimedAdd(player.toString(), permission.getValue(), null, null, new Date(now), timeDifferenceSeconds);
                }
            } else {
                if (permission.isServerSpecific()) {
                    for (String server : permission.getServers()) {
                        if (permission.isWorldSpecific()) {
                            for (String world : permission.getWorlds()) {
                                result &= BungeePermsAPI.userAdd(player.toString(), permission.getValue(), server, world);
                            }
                        } else {
                            result &= BungeePermsAPI.userAdd(player.toString(), permission.getValue(), server, null);
                        }
                    }
                } else {
                    result &= BungeePermsAPI.userAdd(player.toString(), permission.getValue(), null, null);
                }
            }
            return result;
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> addPermission(String username, BungeePermission permission) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            if (permission.hasDuration()) {
                long now = System.currentTimeMillis();
                int timeDifferenceSeconds = (int) ((permission.getEndingTimeStamp().toEpochMilli() - now) / 1000);

                if (permission.isServerSpecific()) {
                    for (String server : permission.getServers()) {
                        if (permission.isWorldSpecific()) {
                            for (String world : permission.getWorlds()) {
                                result &= BungeePermsAPI.userTimedAdd(username, permission.getValue(), server, world, new Date(now), timeDifferenceSeconds);
                            }
                        } else {
                            result &= BungeePermsAPI.userTimedAdd(username, permission.getValue(), server, null, new Date(now), timeDifferenceSeconds);
                        }
                    }
                } else {
                    result &= BungeePermsAPI.userTimedAdd(username, permission.getValue(), null, null, new Date(now), timeDifferenceSeconds);
                }
            } else {
                if (permission.isServerSpecific()) {
                    for (String server : permission.getServers()) {
                        if (permission.isWorldSpecific()) {
                            for (String world : permission.getWorlds()) {
                                result &= BungeePermsAPI.userAdd(username, permission.getValue(), server, world);
                            }
                        } else {
                            result &= BungeePermsAPI.userAdd(username, permission.getValue(), server, null);
                        }
                    }
                } else {
                    result &= BungeePermsAPI.userAdd(username, permission.getValue(), null, null);
                }
            }
            return result;
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> removePermission(UUID player, String permission) {
        return CompletableFuture.supplyAsync(() -> BungeePermsAPI.userRemove(player.toString(), permission, null, null), executor);
    }

    @Override
    public CompletionStage<Boolean> removePermission(String username, String permission) {
        return CompletableFuture.supplyAsync(() -> BungeePermsAPI.userRemove(username, permission, null, null), executor);
    }

    @Override
    public CompletionStage<Boolean> removePermission(UUID player, BungeePermission permission) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            if (permission.hasDuration()) {
                if (permission.isServerSpecific()) {
                    for (String server : permission.getServers()) {
                        if (permission.isWorldSpecific()) {
                            for (String world : permission.getWorlds()) {
                                result &= BungeePermsAPI.userTimedRemove(player.toString(), permission.getValue(), server, world);
                            }
                        } else {
                            result &= BungeePermsAPI.userTimedRemove(player.toString(), permission.getValue(), server, null);
                        }
                    }
                } else {
                    result &= BungeePermsAPI.userTimedRemove(player.toString(), permission.getValue(), null, null);
                }
            } else {
                if (permission.isServerSpecific()) {
                    for (String server : permission.getServers()) {
                        if (permission.isWorldSpecific()) {
                            for (String world : permission.getWorlds()) {
                                result &= BungeePermsAPI.userRemove(player.toString(), permission.getValue(), server, world);
                            }
                        } else {
                            result &= BungeePermsAPI.userRemove(player.toString(), permission.getValue(), server, null);
                        }
                    }
                } else {
                    result &= BungeePermsAPI.userRemove(player.toString(), permission.getValue(), null, null);
                }
            }
            return result;
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> removePermission(String username, BungeePermission permission) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            if (permission.hasDuration()) {
                if (permission.isServerSpecific()) {
                    for (String server : permission.getServers()) {
                        if (permission.isWorldSpecific()) {
                            for (String world : permission.getWorlds()) {
                                result &= BungeePermsAPI.userTimedRemove(username, permission.getValue(), server, world);
                            }
                        } else {
                            result &= BungeePermsAPI.userTimedRemove(username, permission.getValue(), server, null);
                        }
                    }
                } else {
                    result &= BungeePermsAPI.userTimedRemove(username, permission.getValue(), null, null);
                }
            } else {
                if (permission.isServerSpecific()) {
                    for (String server : permission.getServers()) {
                        if (permission.isWorldSpecific()) {
                            for (String world : permission.getWorlds()) {
                                result &= BungeePermsAPI.userRemove(username, permission.getValue(), server, world);
                            }
                        } else {
                            result &= BungeePermsAPI.userRemove(username, permission.getValue(), server, null);
                        }
                    }
                } else {
                    result &= BungeePermsAPI.userRemove(username, permission.getValue(), null, null);
                }
            }
            return result;
        }, executor);
    }

}
