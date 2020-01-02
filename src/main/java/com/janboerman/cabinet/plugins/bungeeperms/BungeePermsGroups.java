package com.janboerman.cabinet.plugins.bungeeperms;

import com.janboerman.cabinet.api.BungeeGroup;
import com.janboerman.cabinet.api.BungeePermission;
import com.janboerman.cabinet.api.Groups;
import com.janboerman.cabinet.util.Executors;
import net.alpenblock.bungeeperms.*;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BungeePermsGroups implements Groups {

    private final ProxyServer proxyServer;
    private BungeePerms bungeePerms;
    private Executor executor;
    private PermissionsManager permissionsManager;

    public BungeePermsGroups(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public String getName() {
        return "BungeePermsGroups";
    }

    @Override
    public boolean tryInitialise() {
        Plugin plugin = proxyServer.getPluginManager().getPlugin("BungeePerms");
        if (plugin != null) {
            bungeePerms = BungeePerms.getInstance();
            permissionsManager = bungeePerms.getPermissionsManager();
            executor = Executors.asyncExecutor(plugin);
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

    @Override
    public CompletionStage<Boolean> isMember(UUID player, String groupName) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(player);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups().contains(groupName));

        return CompletableFuture.supplyAsync(() -> BungeePermsAPI.userInGroup(player.toString(), groupName), executor);
    }

    @Override
    public CompletionStage<Boolean> isMember(String username, String groupName) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(username);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups().contains(groupName));

        return CompletableFuture.supplyAsync(() -> BungeePermsAPI.userInGroup(username, groupName), executor);
    }

    @Override
    public CompletionStage<Optional<String>> getPrimaryGroup(UUID player) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(player);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups().stream().findFirst());

        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userMainGroup(player.toString())), executor);
        //return CompletableFuture.supplyAsync(() -> permissionsManager.getUser(player, true), executor).thenApply(BungeePermsGroups::getPrimaryGroup);
    }

    @Override
    public CompletionStage<Optional<String>> getPrimaryGroup(String username) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(username);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups().stream().findFirst());

        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userMainGroup(username)), executor);
        //return CompletableFuture.supplyAsync(() -> permissionsManager.getUser(username, true), executor).thenApply(BungeePermsGroups::getPrimaryGroup);
    }

//    private static Optional<String> getPrimaryGroup(User user) {
//        List<Group> groups = user.getGroups();
//        if (groups.isEmpty()) {
//            return Optional.empty();
//        } else if (groups.size() == 1) {
//            return Optional.of(groups.get(0).getName());
//        } else {
//            groups.sort(Group.WEIGHT_COMPARATOR);
//            Group primaryGroup = groups.get(0);
//            Group secondaryGroup = groups.get(1);
//            if (Group.WEIGHT_COMPARATOR.compare(primaryGroup, secondaryGroup) == 0) {
//                //both groups are equally ranked, meaning there is no primary group
//                return Optional.empty();
//            } else {
//                return Optional.of(primaryGroup.getName());
//            }
//        }
//    }

    @Override
    public CompletionStage<Boolean> addMember(UUID player, String... groupNames) {
        CompletionStage<Boolean> result = CompletableFuture.completedFuture(true);
        for (String groupName : groupNames) {
            result = result.thenCompose(acc -> CompletableFuture.supplyAsync(() -> BungeePermsAPI.userAddGroup(player.toString(), groupName), executor).thenApply(b -> b && acc));
        }
        return result;
    }

    @Override
    public CompletionStage<Boolean> addMember(String username, String... groupNames) {
        CompletionStage<Boolean> result = CompletableFuture.completedFuture(true);
        for (String groupName : groupNames) {
            result = result.thenCompose(acc -> CompletableFuture.supplyAsync(() -> BungeePermsAPI.userAddGroup(username, groupName), executor).thenApply(b -> b && acc));
        }
        return result;
    }

    @Override
    public CompletionStage<Collection<String>> getGroups(UUID player) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(player);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups());

        return CompletableFuture.supplyAsync(() -> BungeePermsAPI.userAllGroups(player.toString()), executor);
    }

    @Override
    public CompletionStage<Collection<String>> getGroups(String username) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(username);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups());

        return CompletableFuture.supplyAsync(() -> BungeePermsAPI.userAllGroups(username), executor);
    }

    @Override
    public CompletionStage<Optional<BungeeGroup>> getGroup(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            Group group = permissionsManager.getGroup(groupName);
            if (group == null) {
                return Optional.empty();
            } else {
                return Optional.of(toBungeeGroup(group));
            }
        }, executor);
    }

    private static BungeeGroup toBungeeGroup(Group group) {
        String name = group.getName();
        Instant endingTime = null; //bungeeperms does not support timed groups! but it does support timed inheritance. (as does luckperms)
        Set<String> servers = group.getServers().keySet();
        Set<String> worlds = group.getServers().values().stream().flatMap(server -> server.getWorlds().keySet().stream()).collect(Collectors.toSet());
        OptionalInt weight = OptionalInt.of(group.getWeight());
        return new BungeeGroup(name, servers, worlds, endingTime, weight);
    }

    @Override
    public CompletionStage<BungeeGroup> createOrUpdateGroup(BungeeGroup group) {
        return CompletableFuture.supplyAsync(() -> {
            Consumer<Group> consumer = bpGroup -> {
                bpGroup.setWeight(group.getWeight().orElse(0));
                bpGroup.setServers(group.getServers().stream().collect(Collectors.toMap(Function.identity(), bpGroup::getServer)));
                bpGroup.getServers().values().forEach(server -> server.setWorlds(group.getWorlds().stream().collect(Collectors.toMap(Function.identity(), server::getWorld))));
                //can't set expire timestamp... :(
            };

            Optional<Group> optionalGroup = permissionsManager.getGroups().stream().filter(g -> g.getName().equals(group.getName())).findFirst();
            if (optionalGroup.isPresent()) {
                Group bpGroup = optionalGroup.get();
                consumer.accept(bpGroup);
            } else {
                Group bpGroup = new Group(
                        group.getName(),    //name
                        new ArrayList<>(),  //inheritances
                        new ArrayList<>(),  //timed inheritances
                        new ArrayList<>(),  //permissions
                        new ArrayList<>(),  //timed permissions
                        new HashMap<>(),    //servers
                        0,              //rank
                        0,              //weight
                        null,           //ladder
                        false,          //isDefault
                        group.getName(),        //display name
                        "",             //prefix
                        "");            //suffix
                consumer.accept(bpGroup);
                permissionsManager.addGroup(bpGroup);
            }

            return group;
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> removeGroup(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            Group group = permissionsManager.getGroup(groupName);
            if (group == null) {
                return false;
            } else {
                permissionsManager.deleteGroup(group);
                return true;
            }
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> addGroupPermission(String groupName, BungeePermission... permissions) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            for (BungeePermission permission : permissions) {
                if (permission.hasDuration()) {
                    long now = System.currentTimeMillis();
                    int durationSeconds = (int) ((permission.getEndingTimeStamp().toEpochMilli() - now) / 1000);

                    if (permission.isServerSpecific()) {
                        for (String server : permission.getServers()) {
                            if (permission.isWorldSpecific()) {
                                for (String world : permission.getWorlds()) {
                                    result &= BungeePermsAPI.groupTimedAdd(groupName, permission.getValue(), server, world, new Date(now), durationSeconds);
                                }
                            } else {
                                result &= BungeePermsAPI.groupTimedAdd(groupName, permission.getValue(), server, null, new Date(now), durationSeconds);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.groupTimedAdd(groupName, permission.getValue(), null, null, new Date(now), durationSeconds);
                    }
                } else {
                    if (permission.isServerSpecific()) {
                        for (String server : permission.getServers()) {
                            if (permission.isWorldSpecific()) {
                                for (String world : permission.getWorlds()) {
                                    result &= BungeePermsAPI.groupAdd(groupName, permission.getValue(), server, world);
                                }
                            } else {
                                result &= BungeePermsAPI.groupAdd(groupName, permission.getValue(), server, null);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.groupAdd(groupName, permission.getValue(), null, null);
                    }
                }
            }
            return result;
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> removeGroupPermission(String groupName, BungeePermission... permissions) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            for (BungeePermission permission : permissions) {
                if (permission.hasDuration()) {
                    if (permission.isServerSpecific()) {
                        for (String server : permission.getServers()) {
                            if (permission.isWorldSpecific()) {
                                for (String world : permission.getWorlds()) {
                                    result &= BungeePermsAPI.groupTimedRemove(groupName, permission.getValue(), server, world);
                                }
                            } else {
                                result &= BungeePermsAPI.groupTimedRemove(groupName, permission.getValue(), server, null);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.groupTimedRemove(groupName, permission.getValue(), null, null);
                    }
                } else {
                    if (permission.isServerSpecific()) {
                        for (String server : permission.getServers()) {
                            if (permission.isWorldSpecific()) {
                                for (String world : permission.getWorlds()) {
                                    result &= BungeePermsAPI.groupRemove(groupName, permission.getValue(), server, world);
                                }
                            } else {
                                result &= BungeePermsAPI.groupRemove(groupName, permission.getValue(), server, null);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.groupRemove(groupName, permission.getValue(), null, null);
                    }
                }
            }
            return result;
        }, executor);
    }

    @Override
    public CompletionStage<Boolean> hasGroupPermission(String groupName, BungeePermission... permissions) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = false;
            for (BungeePermission permission : permissions) {
                //ignore duration
                if (permission.isServerSpecific()) {
                    for (String server : permission.getServers()) {
                        if (permission.isWorldSpecific()) {
                            for (String world : permission.getWorlds()) {
                                result &= BungeePermsAPI.groupHas(groupName, permission.getValue(), server, world);
                            }
                        } else {
                            result &= BungeePermsAPI.groupHas(groupName, permission.getValue(), server, null);
                        }
                    }
                } else {
                    result &= BungeePermsAPI.groupHas(groupName, permission.getValue(), null, null);
                }
            }
            return result;
        }, executor);
    }

}
