package com.janboerman.cabinet.plugins.bungeeperms;

import com.janboerman.cabinet.api.CContext;
import com.janboerman.cabinet.api.CPermission;
import com.janboerman.cabinet.api.ChatSupport;
import com.janboerman.cabinet.plugins.PluginPermissions;
import com.janboerman.cabinet.util.Executors;
import net.alpenblock.bungeeperms.*;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class BungeePermsPermissions extends PluginPermissions {

    private BungeePerms bungeePerms;
    private PermissionsManager permissionsManager;
    private Executor executor;

    public BungeePermsPermissions(ProxyServer proxyServer) {
        super("BungeePerms", proxyServer);
    }

    @Override
    public void initialise() {
        bungeePerms = BungeePerms.getInstance();
        permissionsManager = bungeePerms.getPermissionsManager();
        executor = Executors.asyncExecutor(getPlugin());
    }

    private void saveUser(User user) {
        permissionsManager.getBackEnd().saveUser(user, true);
        bungeePerms.getNetworkNotifier().reloadUser(user, null);
        bungeePerms.getEventDispatcher().dispatchUserChangeEvent(user);
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
    public ChatSupport hasChatSupport() {
        return ChatSupport.READ_WRITE;
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
    public CompletionStage<Boolean> hasPermission(UUID player, CContext context, String permission) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            if (context.isServerSensitive()) {
                for (String server : context.getServers()) {
                    if (context.isWorldSensitive()) {
                        for (String world : context.getWorlds()) {
                            result &= BungeePermsAPI.userHasPermission(player.toString(), permission, server, world);
                        }
                    } else {
                        result &= BungeePermsAPI.userHasPermission(player.toString(), permission, server, null);
                    }
                }
            } else {
                //bungeeperms does not support server-unspecific, world-specific permissions
                result &= BungeePermsAPI.userHasPermission(player.toString(), permission, null, null);
            }
            return result;
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> hasPermission(String username, CContext context, String permission) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            if (context.isServerSensitive()) {
                for (String server : context.getServers()) {
                    if (context.isWorldSensitive()) {
                        for (String world : context.getWorlds()) {
                            result &= BungeePermsAPI.userHasPermission(username, permission, server, world);
                        }
                    } else {
                        result &= BungeePermsAPI.userHasPermission(username, permission, server, null);
                    }
                }
            } else {
                //bungeeperms does not support server-unspecific, world-specific permissions
                result &= BungeePermsAPI.userHasPermission(username, permission, null, null);
            }
            return result;
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> addPermission(UUID player, CContext context, CPermission... permissions) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            for (CPermission permission : permissions) {
                if (permission.hasDuration()) {
                    long now = System.currentTimeMillis();
                    int timeDifferenceSeconds = (int) ((permission.getEndingTimeStamp().toEpochMilli() - now) / 1000);

                    if (context.isServerSensitive()) {
                        for (String server : context.getServers()) {
                            if (context.isWorldSensitive()) {
                                for (String world : context.getWorlds()) {
                                    result &= BungeePermsAPI.userTimedAdd(player.toString(), permission.getPermission(), server, world, new Date(now), timeDifferenceSeconds);
                                }
                            } else {
                                result &= BungeePermsAPI.userTimedAdd(player.toString(), permission.getPermission(), server, null, new Date(now), timeDifferenceSeconds);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.userTimedAdd(player.toString(), permission.getPermission(), null, null, new Date(now), timeDifferenceSeconds);
                    }
                } else {
                    if (context.isServerSensitive()) {
                        for (String server : context.getServers()) {
                            if (context.isWorldSensitive()) {
                                for (String world : context.getWorlds()) {
                                    result &= BungeePermsAPI.userAdd(player.toString(), permission.getPermission(), server, world);
                                }
                            } else {
                                result &= BungeePermsAPI.userAdd(player.toString(), permission.getPermission(), server, null);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.userAdd(player.toString(), permission.getPermission(), null, null);
                    }
                }
            }
            return result;
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> addPermission(String username, CContext context, CPermission... permissions) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            for (CPermission permission : permissions) {
                if (permission.hasDuration()) {
                    long now = System.currentTimeMillis();
                    int timeDifferenceSeconds = (int) ((permission.getEndingTimeStamp().toEpochMilli() - now) / 1000);

                    if (context.isServerSensitive()) {
                        for (String server : context.getServers()) {
                            if (context.isWorldSensitive()) {
                                for (String world : context.getWorlds()) {
                                    result &= BungeePermsAPI.userTimedAdd(username, permission.getPermission(), server, world, new Date(now), timeDifferenceSeconds);
                                }
                            } else {
                                result &= BungeePermsAPI.userTimedAdd(username, permission.getPermission(), server, null, new Date(now), timeDifferenceSeconds);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.userTimedAdd(username, permission.getPermission(), null, null, new Date(now), timeDifferenceSeconds);
                    }
                } else {
                    if (context.isServerSensitive()) {
                        for (String server : context.getServers()) {
                            if (context.isWorldSensitive()) {
                                for (String world : context.getWorlds()) {
                                    result &= BungeePermsAPI.userAdd(username, permission.getPermission(), server, world);
                                }
                            } else {
                                result &= BungeePermsAPI.userAdd(username, permission.getPermission(), server, null);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.userAdd(username, permission.getPermission(), null, null);
                    }
                }
            }
            return result;
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> removePermission(UUID player, CContext context, CPermission... permissions) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            for (CPermission permission : permissions) {
                if (permission.hasDuration()) {
                    if (context.isServerSensitive()) {
                        for (String server : context.getServers()) {
                            if (context.isWorldSensitive()) {
                                for (String world : context.getWorlds()) {
                                    result &= BungeePermsAPI.userTimedRemove(player.toString(), permission.getPermission(), server, world);
                                }
                            } else {
                                result &= BungeePermsAPI.userTimedRemove(player.toString(), permission.getPermission(), server, null);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.userTimedRemove(player.toString(), permission.getPermission(), null, null);
                    }
                } else {
                    if (context.isServerSensitive()) {
                        for (String server : context.getServers()) {
                            if (context.isWorldSensitive()) {
                                for (String world : context.getWorlds()) {
                                    result &= BungeePermsAPI.userRemove(player.toString(), permission.getPermission(), server, world);
                                }
                            } else {
                                result &= BungeePermsAPI.userRemove(player.toString(), permission.getPermission(), server, null);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.userRemove(player.toString(), permission.getPermission(), null, null);
                    }
                }
            }
            return result;
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> removePermission(String username, CContext context, CPermission... permissions) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            for (CPermission permission : permissions) {
                if (permission.hasDuration()) {
                    if (context.isServerSensitive()) {
                        for (String server : context.getServers()) {
                            if (context.isWorldSensitive()) {
                                for (String world : context.getWorlds()) {
                                    result &= BungeePermsAPI.userTimedRemove(username, permission.getPermission(), server, world);
                                }
                            } else {
                                result &= BungeePermsAPI.userTimedRemove(username, permission.getPermission(), server, null);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.userTimedRemove(username, permission.getPermission(), null, null);
                    }
                } else {
                    if (context.isServerSensitive()) {
                        for (String server : context.getServers()) {
                            if (context.isWorldSensitive()) {
                                for (String world : context.getWorlds()) {
                                    result &= BungeePermsAPI.userRemove(username, permission.getPermission(), server, world);
                                }
                            } else {
                                result &= BungeePermsAPI.userRemove(username, permission.getPermission(), server, null);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.userRemove(username, permission.getPermission(), null, null);
                    }
                }
            }
            return result;
        }, executor);
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixGlobal(UUID player) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userPrefix(player.toString(), null, null)), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixGlobal(String userName) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userPrefix(userName, null, null)), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixOnServer(UUID player, String server) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userPrefix(player.toString(), server, null)), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixOnServer(String userName, String server) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userPrefix(userName, server, null)), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixOnWorld(UUID player, String server, String world) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userPrefix(player.toString(), server, world)), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixOnWorld(String userName, String server, String world) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userPrefix(userName, server, world)), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixGlobal(UUID player) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userSuffix(player.toString(), null, null)), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixGlobal(String userName) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userSuffix(userName, null, null)), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixOnServer(UUID player, String server) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userSuffix(player.toString(), server, null)), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixOnServer(String userName, String server) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userSuffix(userName, server, null)), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixOnWorld(UUID player, String server, String world) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userSuffix(player.toString(), server, world)), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixOnWorld(String userName, String server, String world) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userSuffix(userName, server, world)), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getDisplayNameGlobal(UUID player) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(permissionsManager.getUser(player).getDisplay()), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getDisplayNameGlobal(String userName) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(permissionsManager.getUser(userName).getDisplay()), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getDisplayNameOnServer(UUID player, String server) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(permissionsManager.getUser(player).getServer(server).getDisplay()), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getDisplayNameOnServer(String userName, String server) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(permissionsManager.getUser(userName).getServer(server).getDisplay()), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getDisplayNameOnWorld(UUID player, String server, String world) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(permissionsManager.getUser(player).getServer(server).getWorld(world).getDisplay()), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getDisplayNameOnWorld(String userName, String server, String world) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(permissionsManager.getUser(userName).getServer(server).getWorld(world).getDisplay()), executor);
    }

    private boolean setPrefix(User user, CContext context, String prefix) {
        if (context.isGlobal()) {
            permissionsManager.setUserPrefix(user, prefix, null, null);
            return true;
        } else if (context.isServerSensitive()) {
            for (String server : context.getServers()) {
                if (context.isWorldSensitive()) {
                    for (String world : context.getWorlds()) {
                        permissionsManager.setUserPrefix(user, prefix, server, world);
                    }
                } else {
                    permissionsManager.setUserPrefix(user, prefix, server, null);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public CompletionStage<Boolean> setPrefix(UUID player, CContext where, String prefix, int priority) {
        return CompletableFuture.supplyAsync(() -> {
            User user = permissionsManager.getUser(player);
            if (user != null) {
                return setPrefix(user, where, prefix);
            } else {
                return false;
            }
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> setPrefix(String userName, CContext where, String prefix, int priority) {
        return CompletableFuture.supplyAsync(() -> {
            User user = permissionsManager.getUser(userName);
            if (user != null) {
                return setPrefix(user, where, prefix);
            } else {
                return false;
            }
        }, executor);
    }

    private boolean setSuffix(User user, CContext context, String suffix) {
        if (context.isGlobal()) {
            permissionsManager.setUserSuffix(user, suffix, null, null);
            return true;
        } else if (context.isServerSensitive()) {
            for (String server : context.getServers()) {
                if (context.isWorldSensitive()) {
                    for (String world : context.getWorlds()) {
                        permissionsManager.setUserSuffix(user, suffix, server, world);
                    }
                } else {
                    permissionsManager.setUserSuffix(user, suffix, server, null);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public CompletionStage<Boolean> setSuffix(UUID player, CContext where, String suffix, int priority) {
        return CompletableFuture.supplyAsync(() -> {
            User user = permissionsManager.getUser(player);
            if (user != null) {
                return setSuffix(user, where, suffix);
            } else {
                return false;
            }
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> setSuffix(String userName, CContext where, String suffix, int priority) {
        return CompletableFuture.supplyAsync(() -> {
            User user = permissionsManager.getUser(userName);
            if (user != null) {
                return setSuffix(user, where, suffix);
            } else {
                return false;
            }
        }, executor);
    }

    private boolean setDisplayName(User user, CContext context, String displayName) {
        if (context.isGlobal()) {
            permissionsManager.setUserDisplay(user, displayName, null, null);
            return true;
        } else if (context.isServerSensitive()) {
            for (String server : context.getServers()) {
                if (context.isWorldSensitive()) {
                    for (String world : context.getWorlds()) {
                        permissionsManager.setUserDisplay(user, displayName, server, world);
                    }
                } else {
                    permissionsManager.setUserDisplay(user, displayName, server, null);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public CompletionStage<Boolean> setDisplayName(UUID player, CContext where, String displayName, int priority) {
        return CompletableFuture.supplyAsync(() -> {
            User user = permissionsManager.getUser(player);
            if (user != null) {
                return setDisplayName(user, where, displayName);
            } else {
                return false;
            }
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> setDisplayName(String userName, CContext where, String displayName, int priority) {
        return CompletableFuture.supplyAsync(() -> {
            User user = permissionsManager.getUser(userName);
            if (user != null) {
                return setDisplayName(user, where, displayName);
            } else {
                return false;
            }
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> removePrefix(UUID player, CContext where) {
        return setPrefix(player, where, null, 0);
    }

    @Override
    public CompletionStage<Boolean> removePrefix(String userName, CContext where) {
        return setPrefix(userName, where, null, 0);
    }

    private boolean removePrefixes(User user, CContext where, String prefix) {
        boolean equal = false;
        if (where.isGlobal()) {
            equal = Objects.equals(user.getPrefix(), prefix);
            if (equal) user.setPrefix(null);
        } else if (where.isServerSensitive()) {
            for (Map.Entry<String, Server> serverEntry : user.getServers().entrySet()) {
                Server server = serverEntry.getValue();
                if (where.isWorldSensitive()) {
                    for (Map.Entry<String, World> worldEntry : server.getWorlds().entrySet()) {
                        World world = worldEntry.getValue();
                        equal = Objects.equals(world.getPrefix(), prefix);
                        if (equal) world.setPrefix(null);
                    }
                } else {
                    equal = Objects.equals(server.getPrefix(), prefix);
                    if (equal) server.setPrefix(null);
                }
            }
        }

        return equal;
    }

    private boolean removeSuffixes(User user, CContext where, String suffix) {
        boolean equal = false;
        if (where.isGlobal()) {
            equal = Objects.equals(user.getSuffix(), suffix);
            if (equal) user.setSuffix(null);
        } else if (where.isServerSensitive()) {
            for (Map.Entry<String, Server> serverEntry : user.getServers().entrySet()) {
                Server server = serverEntry.getValue();
                if (where.isWorldSensitive()) {
                    for (Map.Entry<String, World> worldEntry : server.getWorlds().entrySet()) {
                        World world = worldEntry.getValue();
                        equal = Objects.equals(world.getSuffix(), suffix);
                        if (equal) world.setSuffix(null);
                    }
                } else {
                    equal = Objects.equals(server.getSuffix(), suffix);
                    if (equal) server.setSuffix(null);
                }
            }
        }

        return equal;
    }

    private boolean removeDisplayNames(User user, CContext where, String displayName) {
        boolean equal = false;
        if (where.isGlobal()) {
            equal = Objects.equals(user.getDisplay(), displayName);
            if (equal) user.setDisplay(null);
        } else if (where.isServerSensitive()) {
            for (Map.Entry<String, Server> serverEntry : user.getServers().entrySet()) {
                Server server = serverEntry.getValue();
                if (where.isWorldSensitive()) {
                    for (Map.Entry<String, World> worldEntry : server.getWorlds().entrySet()) {
                        World world = worldEntry.getValue();
                        equal = Objects.equals(world.getDisplay(), displayName);
                        if (equal) world.setDisplay(null);
                    }
                } else {
                    equal = Objects.equals(server.getDisplay(), displayName);
                    if (equal) server.setDisplay(null);
                }
            }
        }

        return equal;
    }

    @Override
    public CompletionStage<Boolean> removePrefix(UUID player, CContext where, String prefix) {
        return CompletableFuture.supplyAsync(() -> {
            User user = permissionsManager.getUser(player);
            if (user != null) {
                boolean result = removePrefixes(user, where, prefix);
                saveUser(user);
                return result;
            } else {
                return false;
            }
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> removePrefix(String userName, CContext where, String prefix) {
        return CompletableFuture.supplyAsync(() -> {
            User user = permissionsManager.getUser(userName);
            if (user != null) {
                boolean result = removePrefixes(user, where, prefix);
                saveUser(user);
                return result;
            } else {
                return false;
            }
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> removeSuffix(UUID player, CContext where) {
        return setSuffix(player, where, null, 0);
    }

    @Override
    public CompletionStage<Boolean> removeSuffix(String userName, CContext where) {
        return setSuffix(userName, where, null, 0);
    }

    @Override
    public CompletionStage<Boolean> removeSuffix(UUID player, CContext where, String suffix) {
        return CompletableFuture.supplyAsync(() -> {
            User user = permissionsManager.getUser(player);
            if (user != null) {
                boolean result = removeSuffixes(user, where, suffix);
                saveUser(user);
                return result;
            } else {
                return false;
            }
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> removeSuffix(String userName, CContext where, String suffix) {
        return CompletableFuture.supplyAsync(() -> {
            User user = permissionsManager.getUser(userName);
            if (user != null) {
                boolean result = removeSuffixes(user, where, suffix);
                saveUser(user);
                return result;
            } else {
                return false;
            }
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> removeDisplayName(UUID player, CContext where) {
        return setDisplayName(player, where, null, 0);
    }

    @Override
    public CompletionStage<Boolean> removeDisplayName(String userName, CContext where) {
        return setDisplayName(userName, where, null, 0);
    }

    @Override
    public CompletionStage<Boolean> removeDisplayName(UUID player, CContext where, String displayName) {
        return CompletableFuture.supplyAsync(() -> {
            User user = permissionsManager.getUser(player);
            if (user != null) {
                boolean result = removeDisplayNames(user, where, displayName);
                saveUser(user);
                return result;
            } else {
                return false;
            }
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> removeDisplayName(String userName, CContext where, String displayName) {
        return CompletableFuture.supplyAsync(() -> {
            User user = permissionsManager.getUser(userName);
            if (user != null) {
                boolean result = removeDisplayNames(user, where, displayName);
                saveUser(user);
                return result;
            } else {
                return false;
            }
        }, executor);
    }

}
