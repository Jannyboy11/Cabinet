package com.janboerman.cabinet.plugins.bungeeperms;

import com.janboerman.cabinet.api.CContext;
import com.janboerman.cabinet.api.CGroup;
import com.janboerman.cabinet.api.CPermission;
import com.janboerman.cabinet.plugins.PluginGroups;
import com.janboerman.cabinet.util.Executors;
import net.alpenblock.bungeeperms.*;
import net.md_5.bungee.api.ProxyServer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BungeePermsGroups extends PluginGroups {

    private BungeePerms bungeePerms;
    private Executor executor;
    private PermissionsManager permissionsManager;

    public BungeePermsGroups(ProxyServer proxyServer) {
        super("BungeePerms", proxyServer);
    }

    @Override
    public void initialise() {
        bungeePerms = BungeePerms.getInstance();
        permissionsManager = bungeePerms.getPermissionsManager();
        executor = Executors.asyncExecutor(getPlugin());
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
    public boolean hasChatSupport() {
        return true;
    }

    @Override
    public CompletableFuture<Boolean> isMember(UUID player, String groupName) {
        return CompletableFuture.supplyAsync(() -> BungeePermsAPI.userInGroup(player.toString(), groupName), executor);
    }

    @Override
    public CompletableFuture<Boolean> isMember(String username, String groupName) {
        return CompletableFuture.supplyAsync(() -> BungeePermsAPI.userInGroup(username, groupName), executor);
    }

    @Override
    public CompletableFuture<Boolean> isMember(UUID player, CContext context, String groupName) {
        if (context.isGlobal()) {
            return isMember(player, groupName);
        } else {
            //BungeePerms api does not support context sensitive group memberships!
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public CompletableFuture<Boolean> isMember(String username, CContext context, String groupName) {
        if (context.isGlobal()) {
            return isMember(username, groupName);
        } else {
            //BungeePerms api does not support context sensitive group memberships!
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public CompletableFuture<Optional<String>> getPrimaryGroup(UUID player) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(BungeePermsAPI.userMainGroup(player.toString())), executor);
        //return CompletableFuture.supplyAsync(() -> permissionsManager.getUser(player, true), executor).thenApply(BungeePermsGroups::getPrimaryGroup);
    }

    @Override
    public CompletableFuture<Optional<String>> getPrimaryGroup(String username) {
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
    public CompletableFuture<Boolean> addMember(UUID player, String... groupNames) {
        CompletableFuture<Boolean> result = CompletableFuture.completedFuture(true);
        for (String groupName : groupNames) {
            result = result.thenCompose(acc -> CompletableFuture.supplyAsync(() -> BungeePermsAPI.userAddGroup(player.toString(), groupName), executor).thenApply(b -> b && acc));
        }
        return result;
    }

    @Override
    public CompletableFuture<Boolean> addMember(String username, String... groupNames) {
        CompletableFuture<Boolean> result = CompletableFuture.completedFuture(true);
        for (String groupName : groupNames) {
            result = result.thenCompose(acc -> CompletableFuture.supplyAsync(() -> BungeePermsAPI.userAddGroup(username, groupName), executor).thenApply(b -> b && acc));
        }
        return result;
    }

    private static void includeParentGroups(Set<Group> groups) {
        Set<Group> parentGroups = groups.stream().flatMap(group -> group.getInheritances().stream()).collect(Collectors.toSet());
        while (groups.addAll(parentGroups)) {
            parentGroups = parentGroups.stream().flatMap(group -> group.getInheritances().stream()).collect(Collectors.toSet());
        }
    }

    @Override
    public CompletableFuture<Collection<String>> getGroups(UUID player, boolean includeParentGroups) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> directGroups = BungeePermsAPI.userAllGroups(player.toString());
            if (includeParentGroups) {
                Set<Group> groups = directGroups.stream().map(permissionsManager::getGroup).collect(Collectors.toSet());
                includeParentGroups(groups);
                return groups.stream().map(Group::getName).collect(Collectors.toList());
            } else {
                return directGroups;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Collection<String>> getGroups(String username, boolean includeParentGroups) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> directGroups = BungeePermsAPI.userAllGroups(username);
            if (includeParentGroups) {
                Set<Group> groups = directGroups.stream().map(permissionsManager::getGroup).collect(Collectors.toSet());
                includeParentGroups(groups);
                return groups.stream().map(Group::getName).collect(Collectors.toList());
            } else {
                return directGroups;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Collection<String>> getGroups(UUID player, CContext context, boolean includeParentGroups) {
        if (context.isGlobal()) {
            return getGroups(player, includeParentGroups);
        } else {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }
    }

    @Override
    public CompletableFuture<Collection<String>> getGroups(String username, CContext context, boolean includeParentGroups) {
        if (context.isGlobal()) {
            return getGroups(username, includeParentGroups);
        } else {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }
    }

    @Override
    public CompletableFuture<Optional<CGroup>> getGroup(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            Group group = permissionsManager.getGroup(groupName);
            if (group == null) {
                return Optional.empty();
            } else {
                return Optional.of(toCGroup(group));
            }
        }, executor);
    }

    private static CGroup toCGroup(Group group) {
        String name = group.getName();
        OptionalInt weight = OptionalInt.of(group.getWeight());
        String displayName = group.getDisplay();
        String prefix = group.getPrefix();
        String suffix = group.getSuffix();
        return new CGroup(name, weight, displayName, prefix, suffix);
    }

    @Override
    public CompletableFuture<CGroup> createOrUpdateGroup(CGroup group) {
        return CompletableFuture.supplyAsync(() -> {
            Consumer<Group> consumer = bpGroup -> {
                bpGroup.setWeight(group.getWeight().orElse(0));
                bpGroup.setPrefix(group.getPrefix());
                bpGroup.setSuffix(group.getSuffix());
                bpGroup.setDisplay(group.getDisplayName());
            };

            Optional<Group> optionalGroup = permissionsManager.getGroups().stream().filter(g -> g.getName().equals(group.getName())).findFirst();
            if (optionalGroup.isPresent()) {
                Group bpGroup = optionalGroup.get();
                consumer.accept(bpGroup);
            } else {
                //defaults copied from CommandHandler#handleGroupCommandsCreate
                Group bpGroup = new Group(
                        group.getName(),    //name
                        new ArrayList<>(),  //inheritances
                        new ArrayList<>(),  //timed inheritances
                        new ArrayList<>(),  //permissions
                        new ArrayList<>(),  //timed permissions
                        new HashMap<>(),    //servers
                        1000,           //rank
                        1000,         //weight
                        "default",    //ladder
                        false,      //isDefault
                        null,        //display name
                        null,         //prefix
                        null);        //suffix
                consumer.accept(bpGroup);
                permissionsManager.addGroup(bpGroup);
            }

            return group;
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> removeGroup(String groupName) {
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
    public CompletableFuture<Boolean> groupAddPermission(String groupName, CContext context, CPermission... permissions) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            for (CPermission permission : permissions) {
                if (permission.hasDuration()) {
                    long now = System.currentTimeMillis();
                    int durationSeconds = (int) ((permission.getEndingTimeStamp().toEpochMilli() - now) / 1000);

                    if (context.isServerSensitive()) {
                        for (String server : context.getServers()) {
                            if (context.isWorldSensitive()) {
                                for (String world : context.getWorlds()) {
                                    result &= BungeePermsAPI.groupTimedAdd(groupName, permission.getPermission(), server, world, new Date(now), durationSeconds);
                                }
                            } else {
                                result &= BungeePermsAPI.groupTimedAdd(groupName, permission.getPermission(), server, null, new Date(now), durationSeconds);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.groupTimedAdd(groupName, permission.getPermission(), null, null, new Date(now), durationSeconds);
                    }
                } else {
                    if (context.isServerSensitive()) {
                        for (String server : context.getServers()) {
                            if (context.isWorldSensitive()) {
                                for (String world : context.getWorlds()) {
                                    result &= BungeePermsAPI.groupAdd(groupName, permission.getPermission(), server, world);
                                }
                            } else {
                                result &= BungeePermsAPI.groupAdd(groupName, permission.getPermission(), server, null);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.groupAdd(groupName, permission.getPermission(), null, null);
                    }
                }
            }
            return result;
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> groupRemovePermission(String groupName, CContext context, CPermission... permissions) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = true;
            for (CPermission permission : permissions) {
                if (permission.hasDuration()) {
                    if (context.isServerSensitive()) {
                        for (String server : context.getServers()) {
                            if (context.isWorldSensitive()) {
                                for (String world : context.getWorlds()) {
                                    result &= BungeePermsAPI.groupTimedRemove(groupName, permission.getPermission(), server, world);
                                }
                            } else {
                                result &= BungeePermsAPI.groupTimedRemove(groupName, permission.getPermission(), server, null);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.groupTimedRemove(groupName, permission.getPermission(), null, null);
                    }
                } else {
                    if (context.isServerSensitive()) {
                        for (String server : context.getServers()) {
                            if (context.isWorldSensitive()) {
                                for (String world : context.getWorlds()) {
                                    result &= BungeePermsAPI.groupRemove(groupName, permission.getPermission(), server, world);
                                }
                            } else {
                                result &= BungeePermsAPI.groupRemove(groupName, permission.getPermission(), server, null);
                            }
                        }
                    } else {
                        result &= BungeePermsAPI.groupRemove(groupName, permission.getPermission(), null, null);
                    }
                }
            }
            return result;
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> groupHasPermission(String groupName, CContext context, String... permissions) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = false;
            for (String permission : permissions) {
                //ignore duration
                if (context.isServerSensitive()) {
                    for (String server : context.getServers()) {
                        if (context.isWorldSensitive()) {
                            for (String world : context.getWorlds()) {
                                result &= BungeePermsAPI.groupHas(groupName, permission, server, world);
                            }
                        } else {
                            result &= BungeePermsAPI.groupHas(groupName, permission, server, null);
                        }
                    }
                } else {
                    result &= BungeePermsAPI.groupHas(groupName, permission, null, null);
                }
            }
            return result;
        }, executor);
    }

}
