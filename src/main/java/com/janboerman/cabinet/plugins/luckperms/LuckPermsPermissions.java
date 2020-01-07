package com.janboerman.cabinet.plugins.luckperms;

import com.janboerman.cabinet.api.CContext;
import com.janboerman.cabinet.api.CPermission;
import com.janboerman.cabinet.api.ChatSupport;
import com.janboerman.cabinet.plugins.PluginPermissions;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.DisplayNameNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import net.md_5.bungee.api.ProxyServer;

import java.util.AbstractMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

public class LuckPermsPermissions extends PluginPermissions {

    private LuckPerms luckPerms;
    private UserManager userManager;
    private ContextManager contextManager;

    public LuckPermsPermissions(ProxyServer proxyServer) {
        super("LuckPerms", proxyServer);
    }

    @Override
    public void initialise() {
        luckPerms = LuckPermsProvider.get();
        userManager = luckPerms.getUserManager();
        contextManager = luckPerms.getContextManager();
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

    @Override
    public CompletionStage<Boolean> hasPermission(UUID player, CContext context, String permission) {
        return loadUser(player).thenApply(user -> LuckPermsHelper.hasPermission(user, permission, context));
    }

    @Override
    public CompletionStage<Boolean> hasPermission(String username, CContext context, String permission) {
        return loadUser(username).thenApply(user -> LuckPermsHelper.hasPermission(user, permission, context));
    }

    @Override
    public CompletionStage<Boolean> addPermission(UUID player, CContext context, CPermission... permission) {
        return loadUser(player)
                .thenApply(user -> {
                    boolean success = true;
                    for (CPermission cPermission : permission) {
                        success &= LuckPermsHelper.addPermission(user, context, cPermission);
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
                result &= LuckPermsHelper.removePermission(user, context, cPermission);
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
                result &= LuckPermsHelper.removePermission(user, context, cPermission);
            }
            boolean finalResult = result;
            return userManager.saveUser(user).thenApply(unit -> finalResult);
        });
    }

    @Override
    public CompletionStage<Optional<String>> getPrefix(UUID player) {
        return loadUser(player).thenApply(user -> LuckPermsHelper.getPrefix(user, contextManager.getQueryOptions(user).orElse(contextManager.getStaticQueryOptions())));
    }

    @Override
    public CompletionStage<Optional<String>> getPrefix(String userName) {
        return loadUser(userName).thenApply(user -> LuckPermsHelper.getPrefix(user, contextManager.getQueryOptions(user).orElse(contextManager.getStaticQueryOptions())));
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixGlobal(UUID player) {
        return loadUser(player).thenApply(user -> LuckPermsHelper.getPrefix(user, QueryOptions.nonContextual()));
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixGlobal(String username) {
        return loadUser(username).thenApply(user -> LuckPermsHelper.getPrefix(user, QueryOptions.nonContextual()));
    }


    @Override
    public CompletionStage<Optional<String>> getPrefixOnServer(UUID player, String server) {
        return loadUser(player).thenApply(user -> LuckPermsHelper.getPrefix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.of(DefaultContextKeys.SERVER_KEY, server))
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixOnServer(String userName, String server) {
        return loadUser(userName).thenApply(user -> LuckPermsHelper.getPrefix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.of(DefaultContextKeys.SERVER_KEY, server))
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixOnWorld(UUID player, String server, String world) {
        return loadUser(player).thenApply(user -> LuckPermsHelper.getPrefix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.builder()
                        .add(DefaultContextKeys.SERVER_KEY, server)
                        .add(DefaultContextKeys.WORLD_KEY, world)
                        .build())
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getPrefixOnWorld(String userName, String server, String world) {
        return loadUser(userName).thenApply(user -> LuckPermsHelper.getPrefix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.builder()
                        .add(DefaultContextKeys.SERVER_KEY, server)
                        .add(DefaultContextKeys.WORLD_KEY, world)
                        .build())
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getSuffix(UUID player) {
        return loadUser(player).thenApply(user -> LuckPermsHelper.getSuffix(user, contextManager.getQueryOptions(user).orElse(contextManager.getStaticQueryOptions())));
    }

    @Override
    public CompletionStage<Optional<String>> getSuffix(String userName) {
        return loadUser(userName).thenApply(user -> LuckPermsHelper.getSuffix(user, contextManager.getQueryOptions(user).orElse(contextManager.getStaticQueryOptions())));
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixGlobal(UUID player) {
        return loadUser(player).thenApply(user -> LuckPermsHelper.getSuffix(user, QueryOptions.nonContextual()));
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixGlobal(String userName) {
        return loadUser(userName).thenApply(user -> LuckPermsHelper.getSuffix(user, QueryOptions.nonContextual()));
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixOnServer(UUID player, String server) {
        return loadUser(player).thenApply(user -> LuckPermsHelper.getSuffix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.of(DefaultContextKeys.SERVER_KEY, server))
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixOnServer(String userName, String server) {
        return loadUser(userName).thenApply(user -> LuckPermsHelper.getSuffix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.of(DefaultContextKeys.SERVER_KEY, server))
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixOnWorld(UUID player, String server, String world) {
        return loadUser(player).thenApply(user -> LuckPermsHelper.getSuffix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.builder()
                        .add(DefaultContextKeys.SERVER_KEY, server)
                        .add(DefaultContextKeys.WORLD_KEY, world)
                        .build())
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getSuffixOnWorld(String userName, String server, String world) {
        return loadUser(userName).thenApply(user -> LuckPermsHelper.getSuffix(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.builder()
                        .add(DefaultContextKeys.SERVER_KEY, server)
                        .add(DefaultContextKeys.WORLD_KEY, world)
                        .build())
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getDisplayName(UUID player) {
        return loadUser(player).thenApply(user -> LuckPermsHelper.getDisplayName(user, contextManager.getQueryOptions(user).orElse(contextManager.getStaticQueryOptions())));
    }

    @Override
    public CompletionStage<Optional<String>> getDisplayName(String userName) {
        return loadUser(userName).thenApply(user -> LuckPermsHelper.getDisplayName(user, contextManager.getQueryOptions(user).orElse(contextManager.getStaticQueryOptions())));
    }

    @Override
    public CompletionStage<Optional<String>> getDisplayNameGlobal(UUID player) {
        return loadUser(player).thenApply(user -> LuckPermsHelper.getDisplayName(user, QueryOptions.nonContextual()));
    }

    @Override
    public CompletionStage<Optional<String>> getDisplayNameGlobal(String userName) {
        return loadUser(userName).thenApply(user -> LuckPermsHelper.getDisplayName(user, QueryOptions.nonContextual()));
    }

    @Override
    public CompletionStage<Optional<String>> getDisplayNameOnServer(UUID player, String server) {
        return loadUser(player).thenApply(user -> LuckPermsHelper.getDisplayName(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.of(DefaultContextKeys.SERVER_KEY, server))
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getDisplayNameOnServer(String userName, String server) {
        return loadUser(userName).thenApply(user -> LuckPermsHelper.getDisplayName(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.of(DefaultContextKeys.SERVER_KEY, server))
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getDisplayNameOnWorld(UUID player, String server, String world) {
        return loadUser(player).thenApply(user -> LuckPermsHelper.getDisplayName(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.builder()
                        .add(DefaultContextKeys.SERVER_KEY, server)
                        .add(DefaultContextKeys.WORLD_KEY, world)
                        .build())
                .build()));
    }

    @Override
    public CompletionStage<Optional<String>> getDisplayNameOnWorld(String userName, String server, String world) {
        return loadUser(userName).thenApply(user -> LuckPermsHelper.getDisplayName(user, QueryOptions.builder(QueryMode.CONTEXTUAL)
                .context(ImmutableContextSet.builder()
                        .add(DefaultContextKeys.SERVER_KEY, server)
                        .add(DefaultContextKeys.WORLD_KEY, world)
                        .build())
                .build()));
    }

    private CompletionStage<Boolean> setPrefix(User user, CContext where, String prefix, int priority) {
        PrefixNode.Builder builder = PrefixNode.builder(prefix, priority);
        ContextSet contextSet = LuckPermsHelper.toContextSet(where);
        if (!contextSet.isEmpty()) {
            builder = builder.context(contextSet);
        }
        boolean result = user.data().add(builder.build()).wasSuccessful();
        return userManager.saveUser(user).thenApply(unit -> result);
    }

    private CompletionStage<Boolean> setSuffix(User user, CContext where, String suffix, int priority) {
        SuffixNode.Builder builder = SuffixNode.builder(suffix, priority);
        ContextSet contextSet = LuckPermsHelper.toContextSet(where);
        if (!contextSet.isEmpty()) {
            builder = builder.context(contextSet);
        }
        boolean result = user.data().add(builder.build()).wasSuccessful();
        return userManager.saveUser(user).thenApply(unit -> result);
    }

    private CompletionStage<Boolean> setDisplayName(User user, CContext where, String displayName, int priority) {
        //displayname priorities not supported by luckperms
        DisplayNameNode.Builder builder = DisplayNameNode.builder(displayName);
        ContextSet contextSet = LuckPermsHelper.toContextSet(where);
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
    public CompletionStage<Boolean> setSuffix(UUID player, CContext where, String suffix, int priority) {
        return loadUser(player).thenCompose(user -> setSuffix(user, where, suffix, priority));
    }

    @Override
    public CompletionStage<Boolean> setSuffix(String userName, CContext where, String suffix, int priority) {
        return loadUser(userName).thenCompose(user -> setSuffix(user, where, suffix, priority));
    }

    @Override
    public CompletionStage<Boolean> setDisplayName(UUID player, CContext where, String displayName, int priority) {
        return loadUser(player).thenCompose(user -> setDisplayName(user, where, displayName, priority));
    }

    @Override
    public CompletionStage<Boolean> setDisplayName(String userName, CContext where, String displayName, int priority) {
        return loadUser(userName).thenCompose(user -> setDisplayName(user, where, displayName, priority));
    }

    @Override
    public CompletionStage<Boolean> removePrefix(UUID player, CContext where) {
        return loadUser(player).thenCompose(user -> {
            user.data().clear(LuckPermsHelper.toContextSet(where), NodeType.PREFIX::matches);
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

    @Override
    public CompletionStage<Boolean> removePrefix(String userName, CContext where) {
        return loadUser(userName).thenCompose(user -> {
            user.data().clear(LuckPermsHelper.toContextSet(where), NodeType.PREFIX::matches);
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
                user.data().clear(LuckPermsHelper.toContextSet(where), nodePredicate);
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
                user.data().clear(LuckPermsHelper.toContextSet(where), nodePredicate);
            }
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

    @Override
    public CompletionStage<Boolean> removeSuffix(UUID player, CContext where) {
        return loadUser(player).thenCompose(user -> {
            user.data().clear(LuckPermsHelper.toContextSet(where), NodeType.SUFFIX::matches);
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

    @Override
    public CompletionStage<Boolean> removeSuffix(String userName, CContext where) {
        return loadUser(userName).thenCompose(user -> {
            user.data().clear(LuckPermsHelper.toContextSet(where), NodeType.SUFFIX::matches);
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
                user.data().clear(LuckPermsHelper.toContextSet(where), nodePredicate);
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
                user.data().clear(LuckPermsHelper.toContextSet(where), nodePredicate);
            }
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

    @Override
    public CompletionStage<Boolean> removeDisplayName(UUID player, CContext where) {
        return loadUser(player).thenCompose(user -> {
            user.data().clear(LuckPermsHelper.toContextSet(where), NodeType.DISPLAY_NAME::matches);
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

    @Override
    public CompletionStage<Boolean> removeDisplayName(String userName, CContext where) {
        return loadUser(userName).thenCompose(user -> {
            user.data().clear(LuckPermsHelper.toContextSet(where), NodeType.DISPLAY_NAME::matches);
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

    @Override
    public CompletionStage<Boolean> removeDisplayName(UUID player, CContext where, String displayName) {
        return loadUser(player).thenCompose(user -> {
            Predicate<Node> nodePredicate = node -> NodeType.DISPLAY_NAME.matches(node) && NodeType.DISPLAY_NAME.cast(node).getDisplayName().equals(displayName);
            if (!where.isGlobal()) {
                user.data().clear(nodePredicate);
            } else {
                user.data().clear(LuckPermsHelper.toContextSet(where), nodePredicate);
            }
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

    @Override
    public CompletionStage<Boolean> removeDisplayName(String userName, CContext where, String displayName) {
        return loadUser(userName).thenCompose(user -> {
            Predicate<Node> nodePredicate = node -> NodeType.DISPLAY_NAME.matches(node) && NodeType.DISPLAY_NAME.cast(node).getDisplayName().equals(displayName);
            if (!where.isGlobal()) {
                user.data().clear(nodePredicate);
            } else {
                user.data().clear(LuckPermsHelper.toContextSet(where), nodePredicate);
            }
            return userManager.saveUser(user).thenApply(unit -> true);
        });
    }

}
