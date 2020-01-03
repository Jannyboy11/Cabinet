package com.janboerman.cabinet.plugins.luckperms;

import com.janboerman.cabinet.api.CGroup;
import com.janboerman.cabinet.api.Groups;
import com.janboerman.cabinet.api.CPermission;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class LuckPermsGroups implements Groups {

    private final ProxyServer proxyServer;
    private final LuckPermsPermissions luckPermsPermissions;
    private LuckPerms luckPerms;
    private UserManager userManager;
    private GroupManager groupManager;

    public LuckPermsGroups(ProxyServer proxyServer, LuckPermsPermissions luckPermsPermissions) {
        this.proxyServer = proxyServer;
        this.luckPermsPermissions = luckPermsPermissions;
    }

    @Override
    public String getName() {
        return "LuckPermsGroups";
    }

    @Override
    public boolean tryInitialise() {
        Plugin plugin = proxyServer.getPluginManager().getPlugin("LuckPerms");
        if (plugin != null) {
            luckPerms = LuckPermsProvider.get();
            userManager = luckPerms.getUserManager();
            groupManager = luckPerms.getGroupManager();
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
    public boolean hasChatSupport() {
        return true;
    }

    @Override
    public void onDisable() {
    }

    CompletionStage<Optional<Group>> loadGroup(String groupName) {
        Group group = groupManager.getGroup(groupName);
        return group == null
                ? CompletableFuture.completedFuture(Optional.of(group))
                : groupManager.loadGroup(groupName);
    }

    @Override
    public CompletionStage<Boolean> isMember(UUID player, String groupName) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(player);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups().contains(groupName));

        return luckPermsPermissions.hasPermission(player, "group." + groupName);
    }

    @Override
    public CompletionStage<Boolean> isMember(String username, String groupName) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(username);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups().contains(groupName));

        return luckPermsPermissions.hasPermission(username, "group." + groupName);
    }

    @Override
    public CompletionStage<Optional<String>> getPrimaryGroup(UUID player) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(player);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups().stream().findFirst());

        return luckPermsPermissions.loadUser(player).thenApply(user -> Optional.of(user.getPrimaryGroup()));
    }

    @Override
    public CompletionStage<Optional<String>> getPrimaryGroup(String username) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(username);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups().stream().findFirst());

        return luckPermsPermissions.loadUser(username).thenApply(user -> Optional.of(user.getPrimaryGroup()));
    }

    @Override
    public CompletionStage<Boolean> addMember(UUID player, String... groupNames) {
        CompletionStage<Boolean> result = CompletableFuture.completedFuture(true);
        for (String groupName : groupNames) {
            result = result.thenCompose(acc -> luckPermsPermissions.addPermission(player, "group." + groupName).thenApply(b -> b && acc));
        }
        return result;
    }

    @Override
    public CompletionStage<Boolean> addMember(String username, String... groupNames) {
        CompletionStage<Boolean> result = CompletableFuture.completedFuture(true);
        for (String groupName : groupNames) {
            result = result.thenCompose(acc -> luckPermsPermissions.addPermission(username, "group." + groupName).thenApply(b -> b && acc));
        }
        return result;
    }

    @Override
    public CompletionStage<Collection<String>> getGroups(UUID player) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(player);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups());

        return luckPermsPermissions.loadUser(player).thenApply(LuckPermsGroups::getGroups);
    }

    @Override
    public CompletionStage<Collection<String>> getGroups(String username) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(username);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups());

        return luckPermsPermissions.loadUser(username).thenApply(LuckPermsGroups::getGroups);
    }

    static Set<String> getGroups(User user) {
        return user.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .map(InheritanceNode::getGroupName)
                .collect(Collectors.toSet());
    }

    @Override
    public CompletionStage<Optional<CGroup>> getGroup(String groupName) {
        return loadGroup(groupName).thenApply(optionalGroup -> optionalGroup.map(LuckPermsGroups::toCGroup));
    }

    @Override
    public CompletionStage<CGroup> createOrUpdateGroup(CGroup cGroup) {
        CompletionStage<Group> luckPermsGroup = groupManager.createAndLoadGroup(cGroup.getName());

        return luckPermsGroup.thenApply(group -> {
            String name = group.getName();

            Node groupNode = group.getNodes().stream().filter(n -> n.getKey().equals("group." + name)).findFirst().get();

            Set<String> servers = cGroup.getServers();
            Set<String> worlds = cGroup.getWorlds();
            Instant expire = cGroup.getEndingTimeStamp();
            OptionalInt weight = cGroup.getWeight();

            NodeBuilder builder = groupNode.toBuilder();
            builder = builder.expiry(expire);
            //TODO remove existing servers and worlds
            for (String server : servers) builder = builder.withContext(DefaultContextKeys.SERVER_KEY, server);
            for (String world : worlds) builder = builder.withContext(DefaultContextKeys.WORLD_KEY, world);
            groupNode = builder.build();
            group.data().add(groupNode, TemporaryNodeMergeStrategy.REPLACE_EXISTING_IF_DURATION_LONGER);
            if (weight.isPresent()) {
                WeightNode weightNode = WeightNode.builder(weight.getAsInt()).build();
                group.data().clear(NodeType.WEIGHT::matches);
                group.data().add(weightNode);
            }
            if (cGroup.hasDisplayName()) {
                group.data().clear(NodeType.DISPLAY_NAME::matches);
                group.data().add(DisplayNameNode.builder(cGroup.getDisplayName()).build());
            }
            if (cGroup.hasPrefix()) {
                group.data().clear(NodeType.PREFIX::matches);
                group.data().add(PrefixNode.builder().prefix(cGroup.getPrefix()).build());
            }
            if (cGroup.hasSuffix()) {
                group.data().clear(NodeType.SUFFIX::matches);
                group.data().add(SuffixNode.builder().suffix(cGroup.getSuffix()).build());
            }
            groupManager.saveGroup(group);

            return toCGroup(group);
        });
    }

    private static CGroup toCGroup(Group group) {
        String name = group.getName();
        Node groupNode = group.getNodes().stream().filter(n -> n.getKey().equals("group."+ name)).findFirst().get();
        Set<String> servers = groupNode.getContexts().getValues(DefaultContextKeys.SERVER_KEY);
        if (servers.isEmpty()) servers = null;
        Set<String> worlds = groupNode.getContexts().getValues(DefaultContextKeys.WORLD_KEY);
        if (worlds.isEmpty()) worlds = null;
        Instant expiry = groupNode.getExpiry();
        OptionalInt weight = group.getWeight();
        String displayName = group.getNodes().stream()
                .filter(NodeType.DISPLAY_NAME::matches)
                .map(NodeType.DISPLAY_NAME::cast)
                //cannot sort because DisplayNameNode is not a ChatMetaNode and therefore does not have a priority.
                .map(DisplayNameNode::getDisplayName)
                .findFirst()
                .orElse(null);
        String prefix = group.getNodes().stream()
                .filter(NodeType.PREFIX::matches)
                .map(NodeType.PREFIX::cast)
                .sorted(Comparator.comparing(ChatMetaNode::getPriority))
                .map(PrefixNode::getMetaValue)
                .collect(Collectors.joining(ChatColor.RESET.toString()));
        String suffix = group.getNodes().stream()
                .filter(NodeType.SUFFIX::matches)
                .map(NodeType.SUFFIX::cast)
                .sorted(Comparator.comparing(ChatMetaNode::getPriority))
                .map(SuffixNode::getMetaValue)
                .collect(Collectors.joining(ChatColor.RESET.toString()));

        return new CGroup(name, servers, worlds, expiry, weight, displayName, prefix, suffix);
    }

    @Override
    public CompletionStage<Boolean> removeGroup(String groupName) {
        return loadGroup(groupName).thenCompose(optionalGroup -> {
            if (optionalGroup.isPresent()) {
                Group group = optionalGroup.get();
                return groupManager.deleteGroup(group).thenApply(v -> true);
            } else {
                return CompletableFuture.completedFuture(false);
            }
        });
    }

    @Override
    public CompletionStage<Boolean> addGroupPermission(String groupName, CPermission... permissions) {
        return loadGroup(groupName).thenCompose(optionalGroup -> {
           if (optionalGroup.isPresent()) {
               Group group = optionalGroup.get();
               boolean result = true;
               for (CPermission cPermission : permissions) {
                   result &= LuckPermsPermissions.addPermission(group, cPermission);
               }
               boolean finalResult = result;
               return groupManager.saveGroup(group).thenApply(v -> finalResult);
           } else {
               return CompletableFuture.completedFuture(false);
           }
        });
    }

    @Override
    public CompletionStage<Boolean> removeGroupPermission(String groupName, CPermission... permissions) {
        return loadGroup(groupName).thenCompose(optionalGroup -> {
            if (optionalGroup.isPresent()) {
                Group group = optionalGroup.get();
                boolean result = true;
                for (CPermission cPermission : permissions) {
                    result &= LuckPermsPermissions.removePermission(group, cPermission);
                }
                boolean finalResult = result;
                return groupManager.saveGroup(group).thenApply(v -> finalResult);
            } else {
                return CompletableFuture.completedFuture(false);
            }
        });
    }

    @Override
    public CompletionStage<Boolean> hasGroupPermission(String groupName, CPermission... permissions) {
        return loadGroup(groupName).thenApply(optionalGroup -> {
            if (optionalGroup.isPresent()) {
                Group group = optionalGroup.get();
                boolean result = true;
                for (CPermission cPermission : permissions) {
                    result &= LuckPermsPermissions.hasPermission(group, cPermission);
                }
                return result;
            } else {
                return false;
            }
        });
    }

}
