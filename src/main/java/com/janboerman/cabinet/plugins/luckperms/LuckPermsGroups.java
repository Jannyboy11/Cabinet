package com.janboerman.cabinet.plugins.luckperms;

import com.janboerman.cabinet.api.CContext;
import com.janboerman.cabinet.api.CGroup;
import com.janboerman.cabinet.api.CPermission;
import com.janboerman.cabinet.plugins.PluginGroups;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.*;
import net.luckperms.api.query.QueryOptions;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.janboerman.cabinet.plugins.luckperms.LuckPermsHelper.*;

public class LuckPermsGroups extends PluginGroups {

    private LuckPerms luckPerms;
    private UserManager userManager;
    private GroupManager groupManager;
    private LuckPermsPermissions luckPermsPermissions;
    private ContextManager contextManager;

    public LuckPermsGroups(ProxyServer proxyServer, LuckPermsPermissions luckPermsPermissions) {
        super("LuckPerms", proxyServer);
        this.luckPermsPermissions = luckPermsPermissions;
    }

    @Override
    public void initialise() {
        this.luckPerms = LuckPermsProvider.get();
        this.groupManager = luckPerms.getGroupManager();
        this.userManager = luckPerms.getUserManager();
        this.contextManager = luckPerms.getContextManager();
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

    CompletableFuture<Optional<Group>> loadGroup(String groupName) {
        Group group = groupManager.getGroup(groupName);
        return group == null
                ? CompletableFuture.completedFuture(Optional.of(group))
                : groupManager.loadGroup(groupName);
    }

    @Override
    public CompletableFuture<Boolean> isMember(UUID player, String groupName) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(player);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.hasPermission("group." + groupName));

        return luckPermsPermissions.hasPermission(player, "group." + groupName);
    }

    @Override
    public CompletableFuture<Boolean> isMember(String username, String groupName) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(username);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups().contains(groupName));

        return luckPermsPermissions.hasPermission(username, "group." + groupName);
    }

    @Override
    public CompletableFuture<Boolean> isMember(UUID player, CContext context, String groupName) {
        return luckPermsPermissions.hasPermission(player, context, "group." + groupName);
    }

    @Override
    public CompletableFuture<Boolean> isMember(String username, CContext context, String groupName) {
        return luckPermsPermissions.hasPermission(username, context, "group." + groupName);
    }

    @Override
    public CompletableFuture<Optional<String>> getPrimaryGroup(UUID player) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(player);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups().stream().findFirst());

        return luckPermsPermissions.loadUser(player).thenApply(user -> Optional.of(user.getPrimaryGroup()));
    }

    @Override
    public CompletableFuture<Optional<String>> getPrimaryGroup(String username) {
        ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(username);
        if (proxiedPlayer != null) return CompletableFuture.completedFuture(proxiedPlayer.getGroups().stream().findFirst());

        return luckPermsPermissions.loadUser(username).thenApply(user -> Optional.of(user.getPrimaryGroup()));
    }

    @Override
    public CompletableFuture<Boolean> addMember(UUID player, String... groupNames) {
        return luckPermsPermissions.loadUser(player).thenCompose(user -> {
            boolean result = true;
            for (String groupName : groupNames) {
                Node membershipNode = InheritanceNode.builder(groupName).build();
                result &= user.data().add(membershipNode).wasSuccessful();
            }
            boolean finalresult = result;
            return userManager.saveUser(user).thenApply(unit -> finalresult);
        });
    }

    @Override
    public CompletableFuture<Boolean> addMember(String userName, String... groupNames) {
        return luckPermsPermissions.loadUser(userName).thenCompose(user -> {
            boolean result = true;
            for (String groupName : groupNames) {
                Node membershipNode = InheritanceNode.builder(groupName).build();
                result &= user.data().add(membershipNode).wasSuccessful();
            }
            boolean finalresult = result;
            return userManager.saveUser(user).thenApply(unit -> finalresult);
        });
    }

    @Override
    public CompletableFuture<Collection<String>> getGroups(UUID player, CContext context, boolean includeParentGroups) {
        return luckPermsPermissions.loadUser(player).thenApply(user -> getGroups(user, context, includeParentGroups));
    }

    @Override
    public CompletableFuture<Collection<String>> getGroups(String username, CContext context, boolean includeParentGroups) {
        return luckPermsPermissions.loadUser(username).thenApply(user -> getGroups(user, context, includeParentGroups));
    }

    Set<String> getGroups(User user, CContext context, boolean includeAncestors) {
        Stream<Node> nodes;
        ContextSet contextSet = context.isGlobal() ? contextManager.getStaticContext() : toContextSet(context);
        if (!includeAncestors) {
            nodes = user.getNodes().stream()
                    .filter(node -> contextSet.isSatisfiedBy(node.getContexts()));
        } else {
            nodes = user.resolveDistinctInheritedNodes(QueryOptions.contextual(contextSet)).stream();
        }
        return nodes.filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .map(InheritanceNode::getGroupName)
                .collect(Collectors.toSet());
    }

    @Override
    public CompletableFuture<Optional<CGroup>> getGroup(String groupName) {
        return loadGroup(groupName).thenApply(optionalGroup -> optionalGroup.map(LuckPermsGroups::toCGroup));
    }

    @Override
    public CompletableFuture<CGroup> createOrUpdateGroup(CGroup cGroup) {
        CompletableFuture<Group> luckPermsGroup = groupManager.createAndLoadGroup(cGroup.getName());

        return luckPermsGroup.thenApply(group -> {
            OptionalInt weight = cGroup.getWeight();
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
        OptionalInt weight = group.getWeight();
        String displayName = null;
        String prefix = null;
        String suffix = null;

        //TODO inheritance?!
        for (Node node : group.getDistinctNodes()) {
            if (displayName == null && NodeType.DISPLAY_NAME.matches(node)) {
                DisplayNameNode dnn = NodeType.DISPLAY_NAME.cast(node);
                displayName = dnn.getDisplayName();
            } else if (prefix == null && NodeType.PREFIX.matches(node)) {
                PrefixNode pn = NodeType.PREFIX.cast(node);
                prefix = pn.getMetaValue();
            } else if (suffix == null && NodeType.SUFFIX.matches(node)) {
                SuffixNode sn = NodeType.SUFFIX.cast(node);
                suffix = sn.getMetaValue();
            }
            if (prefix != null && suffix != null && displayName != null) break;
            //TODO instead of picking the highest priority values, concatenate all prefixes, suffixes and display names?
            //TODO need to check with a running LuckPerms install :)
        }

        return new CGroup(name, weight, displayName, prefix, suffix);
    }

    @Override
    public CompletableFuture<Boolean> removeGroup(String groupName) {
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
    public CompletableFuture<Boolean> groupHasPermission(String groupName, CContext context, String... permissions) {
        return loadGroup(groupName).thenApply(maybeGroup -> {
            if (maybeGroup.isPresent()) {
                Group group = maybeGroup.get();
                boolean result = true;
                for (String permission : permissions) {
                    result &= LuckPermsHelper.hasPermission(group, permission, context);
                }
                return result;
            } else {
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> groupRemovePermission(String groupName, CContext context, CPermission... permissions) {
        return loadGroup(groupName).thenCompose(optionalGroup -> {
            if (optionalGroup.isPresent()) {
                Group group = optionalGroup.get();
                boolean result = true;
                for (CPermission permission : permissions) {
                    result &= LuckPermsHelper.removePermission(group, context, permission);
                }
                boolean finalResult = result;
                return groupManager.saveGroup(group).thenApply(unit -> finalResult);
            } else {
                return CompletableFuture.completedFuture(false);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> groupAddPermission(String groupName, CContext context, CPermission... permissions) {
        return loadGroup(groupName).thenCompose(optionalGroup -> {
            if (optionalGroup.isPresent()) {
                Group group = optionalGroup.get();
                boolean result = true;
                for (CPermission permission : permissions) {
                    result &= LuckPermsHelper.addPermission(group, context, permission);
                }
                boolean finalResult = result;
                return groupManager.saveGroup(group).thenApply(unit -> finalResult);
            } else {
                return CompletableFuture.completedFuture(false);
            }
        });
    }

}
