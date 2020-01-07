package com.janboerman.cabinet.plugins.luckperms;

import com.janboerman.cabinet.api.CContext;
import com.janboerman.cabinet.api.CPermission;
import com.janboerman.cabinet.api.ChatSupport;
import com.janboerman.cabinet.plugins.PluginPermissions;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import net.md_5.bungee.api.ProxyServer;

import java.util.AbstractMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collector;

public class LuckPermsPermissions extends PluginPermissions {

    private LuckPerms luckPerms;
    private UserManager userManager;

    public LuckPermsPermissions(ProxyServer proxyServer) {
        super("LuckPerms", proxyServer);
    }

    @Override
    public void initialise() {
        luckPerms = LuckPermsProvider.get();
        userManager = luckPerms.getUserManager();
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

    CompletionStage<User> loadUser(String username) {
        User user = userManager.getUser(username);
        return user != null
                ? CompletableFuture.completedFuture(user)
                : userManager.lookupUniqueId(username).thenCompose(this::loadUser);
    }

    CompletionStage<User> loadUser(UUID player) {
        User user = userManager.getUser(player);
        return user != null
                ? CompletableFuture.completedFuture(user)
                : userManager.loadUser(player);
    }

    static Collector<String, ImmutableContextSet.Builder, ImmutableContextSet> toImmutableContextSet(String contextKey) {
        return Collector.of(
                ImmutableContextSet::builder,
                (builder, value) -> builder.add(contextKey, value),
                (builder1, builder2) -> builder1.addAll(builder2.build()),
                ImmutableContextSet.Builder::build);
    }

    static boolean hasPermission(PermissionHolder permissionHolder, String permission, CContext context) {
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
        if (context.isWorldSensitive()) {
            context.getWorlds().forEach(world -> builder.add(DefaultContextKeys.WORLD_KEY, world));
        }
        if (context.isServerSensitive()) {
            context.getServers().forEach(server -> builder.add(DefaultContextKeys.SERVER_KEY, server));
        }
        ImmutableContextSet contextSet = builder.build();
        QueryOptions queryOptions = contextSet.isEmpty() ? QueryOptions.nonContextual() : QueryOptions.contextual(contextSet);
        return permissionHolder.getCachedData().getPermissionData(queryOptions).checkPermission(permission).asBoolean();
    }

    @Override
    public CompletionStage<Boolean> hasPermission(UUID player, CContext context, String permission) {
        return loadUser(player).thenApply(user -> hasPermission(user, permission, context));
    }

    @Override
    public CompletionStage<Boolean> hasPermission(String username, CContext context, String permission) {
        return loadUser(username).thenApply(user -> hasPermission(user, permission, context));
    }

    static PermissionNode toPermissionNode(CPermission permission, CContext context) {
        PermissionNode.Builder builder = PermissionNode.builder();
        builder = builder.permission(permission.getPermission());
        builder = builder.value(permission.getValue());
        if (permission.hasDuration()) {
            builder = builder.expiry(permission.getEndingTimeStamp());
        }
        if (context != null && context.isServerSensitive()) {
            for (String server : context.getServers()) {
                builder = builder.withContext(DefaultContextKeys.SERVER_KEY, server);
            }
        }
        if (context != null && context.isWorldSensitive()) {
            for (String world : context.getWorlds()) {
                builder = builder.withContext(DefaultContextKeys.WORLD_KEY, world);
            }
        }
        return builder.build();
    }

    static boolean addPermission(PermissionHolder permissionHolder, CContext context, CPermission permission) {
        return permissionHolder.data().add(toPermissionNode(permission, context)).wasSuccessful();
    }

    @Override
    public CompletionStage<Boolean> addPermission(UUID player, CContext context, CPermission... permission) {
        return loadUser(player)
                .thenApply(user -> {
                    boolean success = true;
                    for (CPermission cPermission : permission) {
                        success &= addPermission(user, context, cPermission);
                    }
                    return new AbstractMap.SimpleImmutableEntry<>(user, success);
                })
                .thenCompose(entry -> userManager.saveUser(entry.getKey()).thenApply(unit -> entry.getValue()));
    }

    @Override
    public CompletionStage<Boolean> addPermission(String username, CContext context, CPermission... permission) {
        return userManager.lookupUniqueId(username).thenCompose(uuid -> addPermission(uuid, context, permission));
    }

    @Override
    public CompletionStage<Boolean> removePermission(UUID player, CContext context, CPermission... permission) {
        return loadUser(player).thenCompose(user -> {
            boolean result = true;
            for (CPermission cPermission : permission) {
                result &= removePermission(user, context, cPermission);
            }
            boolean finalResult = result;
            return userManager.saveUser(user).thenApply(unit -> finalResult);
        });
    }

    @Override
    public CompletionStage<Boolean> removePermission(String username, CContext context, CPermission... permission) {
        return loadUser(username).thenCompose(user -> {
            boolean result = true;
            for (CPermission cPermission : permission) {
                result &= removePermission(user, context, cPermission);
            }
            boolean finalResult = result;
            return userManager.saveUser(user).thenApply(unit -> finalResult);
        });
    }

    static boolean removePermission(PermissionHolder permissionHolder, CContext context, CPermission permission) {
        AtomicBoolean value = new AtomicBoolean(false);
        permissionHolder.data().clear(node -> {
            if (node instanceof PermissionNode) {
                //TODO why not just check node#getKey()?
                PermissionNode permissionNode = (PermissionNode) node;
                boolean check = Objects.equals(permissionNode.getPermission(), permission.getPermission())
                        && (permission.getValue() == node.getValue());
                if (context.isServerSensitive()) check &= permissionNode.getContexts().getValues(DefaultContextKeys.SERVER_KEY).containsAll(context.getServers());
                if (context.isWorldSensitive()) check &= permissionNode.getContexts().getValues(DefaultContextKeys.WORLD_KEY).containsAll(context.getWorlds());
                if (check) value.compareAndSet(false, true);
                return check;
            } else {
                return false;
            }
        });
        return value.get();
    }

    static Optional<String> getPrefix(PermissionHolder permissionHolder, QueryOptions queryOptions) {
        return permissionHolder.resolveDistinctInheritedNodes(queryOptions).stream()
                .filter(NodeType.PREFIX::matches).findFirst().map(NodeType.PREFIX::cast).map(PrefixNode::getMetaValue);
    }

    static Optional<String> getSuffix(PermissionHolder permissionHolder, QueryOptions queryOptions) {
        return permissionHolder.resolveDistinctInheritedNodes(queryOptions).stream()
                .filter(NodeType.SUFFIX::matches).findFirst().map(NodeType.SUFFIX::cast).map(SuffixNode::getMetaValue);
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixGlobal(UUID player) {
        return loadUser(player).thenApply(user -> getPrefix(user, QueryOptions.nonContextual()));
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixGlobal(String username) {
        return loadUser(username).thenApply(user -> getPrefix(user, QueryOptions.nonContextual()));
    }


    @Override
    public CompletionStage<Optional<String>> getPrefixOnServer(UUID player, String server) {
        return loadUser(player).thenApply(user -> getPrefix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.of(DefaultContextKeys.SERVER_KEY, server))
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixOnServer(String userName, String server) {
        return loadUser(userName).thenApply(user -> getPrefix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.of(DefaultContextKeys.SERVER_KEY, server))
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixOnWorld(UUID player, String server, String world) {
        return loadUser(player).thenApply(user -> getPrefix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.builder()
                        .add(DefaultContextKeys.SERVER_KEY, server)
                        .add(DefaultContextKeys.WORLD_KEY, world)
                        .build())
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixOnWorld(String userName, String server, String world) {
        return loadUser(userName).thenApply(user -> getPrefix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.builder()
                        .add(DefaultContextKeys.SERVER_KEY, server)
                        .add(DefaultContextKeys.WORLD_KEY, world)
                        .build())
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixGlobal(UUID player) {
        return loadUser(player).thenApply(user -> getSuffix(user, QueryOptions.nonContextual()));
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixGlobal(String userName) {
        return loadUser(userName).thenApply(user -> getSuffix(user, QueryOptions.nonContextual()));
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixOnServer(UUID player, String server) {
        return loadUser(player).thenApply(user -> getSuffix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.of(DefaultContextKeys.SERVER_KEY, server))
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixOnServer(String userName, String server) {
        return loadUser(userName).thenApply(user -> getSuffix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.of(DefaultContextKeys.SERVER_KEY, server))
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixOnWorld(UUID player, String server, String world) {
        return loadUser(player).thenApply(user -> getSuffix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.builder()
                        .add(DefaultContextKeys.SERVER_KEY, server)
                        .add(DefaultContextKeys.WORLD_KEY, world)
                        .build())
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixOnWorld(String userName, String server, String world) {
        return loadUser(userName).thenApply(user -> getSuffix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.builder()
                        .add(DefaultContextKeys.SERVER_KEY, server)
                        .add(DefaultContextKeys.WORLD_KEY, world)
                        .build())
                .build()));
    }

    static ContextSet toContextSet(CContext context) {
        MutableContextSet mutableContextSet = MutableContextSet.create();
        if (context.isServerSensitive()) {
            context.getServers().forEach(server -> mutableContextSet.add(DefaultContextKeys.SERVER_KEY, server));
        }
        if (context.isWorldSensitive()) {
            context.getWorlds().forEach(world -> mutableContextSet.add(DefaultContextKeys.WORLD_KEY, world));
        }
        return mutableContextSet;
    }

    CompletionStage<Boolean> setPrefix(User user, CContext where, String prefix, int priority) {
        PrefixNode.Builder builder = PrefixNode.builder(prefix, priority);
        ContextSet contextSet = toContextSet(where);
        if (!contextSet.isEmpty()) {
            builder = builder.context(contextSet);
        }
        boolean result = user.data().add(builder.build()).wasSuccessful();
        return userManager.saveUser(user).thenApply(unit -> result);
    }

    CompletionStage<Boolean> setSuffix(User user, CContext where, String suffix, int priority) {
        SuffixNode.Builder builder = SuffixNode.builder(suffix, priority);
        ContextSet contextSet = toContextSet(where);
        if (!contextSet.isEmpty()) {
            builder = builder.context(contextSet);
        }
        boolean result = user.data().add(builder.build()).wasSuccessful();
        return userManager.saveUser(user).thenApply(unit -> result);
    }

    @Override
    public CompletionStage<Boolean> setPrefix(UUID player, CContext where, String prefix, int priority) {
        return loadUser(player).thenCompose(user -> setPrefix(user, where, prefix, priority));
    }

    @Override
    public CompletionStage<Boolean> setPrefix(String userName, CContext where, String prefix, int priority) {
        return loadUser(userName).thenCompose(user -> setPrefix(user, where, prefix, priority));
    }

    @Override
    public CompletionStage<Boolean> setSuffix(UUID player, CContext where, String prefix, int priority) {
        return loadUser(player).thenCompose(user -> setSuffix(user, where, prefix, priority));
    }

    @Override
    public CompletionStage<Boolean> setSuffix(String userName, CContext where, String prefix, int priority) {
        return loadUser(userName).thenCompose(user -> setSuffix(user, where, prefix, priority));
    }

    @Override
    public CompletionStage<Boolean> removePrefix(UUID player, CContext where) {
        return loadUser(player).thenCompose(user -> {
            user.data().clear(toContextSet(where), NodeType.PREFIX::matches);
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

    @Override
    public CompletionStage<Boolean> removePrefix(String userName, CContext where) {
        return loadUser(userName).thenCompose(user -> {
            user.data().clear(toContextSet(where), NodeType.PREFIX::matches);
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

    @Override
    public CompletionStage<Boolean> removePrefix(UUID player, CContext where, String prefix) {
        return loadUser(player).thenCompose(user -> {
            Predicate<Node> nodePredicate = node -> NodeType.PREFIX.matches(node) && NodeType.PREFIX.cast(node).getMetaValue().equals(prefix);
            if (!where.isGlobal()) {
                user.data().clear(nodePredicate);
            } else {
                user.data().clear(toContextSet(where), nodePredicate);
            }
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

    @Override
    public CompletionStage<Boolean> removePrefix(String userName, CContext where, String prefix) {
        return loadUser(userName).thenCompose(user -> {
            Predicate<Node> nodePredicate = node -> NodeType.PREFIX.matches(node) && NodeType.PREFIX.cast(node).getMetaValue().equals(prefix);
            if (!where.isGlobal()) {
                user.data().clear(nodePredicate);
            } else {
                user.data().clear(toContextSet(where), nodePredicate);
            }
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

    @Override
    public CompletionStage<Boolean> removeSuffix(UUID player, CContext where) {
        return loadUser(player).thenCompose(user -> {
            user.data().clear(toContextSet(where), NodeType.SUFFIX::matches);
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

    @Override
    public CompletionStage<Boolean> removeSuffix(String userName, CContext where) {
        return loadUser(userName).thenCompose(user -> {
            user.data().clear(toContextSet(where), NodeType.SUFFIX::matches);
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

    @Override
    public CompletionStage<Boolean> removeSuffix(UUID player, CContext where, String suffix) {
        return loadUser(player).thenCompose(user -> {
            Predicate<Node> nodePredicate = node -> NodeType.SUFFIX.matches(node) && NodeType.SUFFIX.cast(node).getMetaValue().equals(suffix);
            if (!where.isGlobal()) {
                user.data().clear(nodePredicate);
            } else {
                user.data().clear(toContextSet(where), nodePredicate);
            }
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

    @Override
    public CompletionStage<Boolean> removeSuffix(String userName, CContext where, String suffix) {
        return loadUser(userName).thenCompose(user -> {
            Predicate<Node> nodePredicate = node -> NodeType.SUFFIX.matches(node) && NodeType.SUFFIX.cast(node).getMetaValue().equals(suffix);
            if (!where.isGlobal()) {
                user.data().clear(nodePredicate);
            } else {
                user.data().clear(toContextSet(where), nodePredicate);
            }
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

}
