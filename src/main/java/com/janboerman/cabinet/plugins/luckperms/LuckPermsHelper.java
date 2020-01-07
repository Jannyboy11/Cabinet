package com.janboerman.cabinet.plugins.luckperms;

import com.janboerman.cabinet.api.CContext;
import com.janboerman.cabinet.api.CPermission;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.DisplayNameNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.query.QueryOptions;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collector;

public class LuckPermsHelper {

    private LuckPermsHelper() {}

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

    static boolean addPermission(PermissionHolder permissionHolder, CContext context, CPermission permission) {
        return permissionHolder.data().add(toPermissionNode(permission, context)).wasSuccessful();
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

    static Collector<String, ImmutableContextSet.Builder, ImmutableContextSet> toImmutableContextSet(String contextKey) {
        return Collector.of(
                ImmutableContextSet::builder,
                (builder, value) -> builder.add(contextKey, value),
                (builder1, builder2) -> builder1.addAll(builder2.build()),
                ImmutableContextSet.Builder::build);
    }

    static Optional<String> getPrefix(PermissionHolder permissionHolder, QueryOptions queryOptions) {
        return permissionHolder.resolveDistinctInheritedNodes(queryOptions).stream()
                .filter(NodeType.PREFIX::matches).findFirst().map(NodeType.PREFIX::cast).map(PrefixNode::getMetaValue);
    }

    static Optional<String> getSuffix(PermissionHolder permissionHolder, QueryOptions queryOptions) {
        return permissionHolder.resolveDistinctInheritedNodes(queryOptions).stream()
                .filter(NodeType.SUFFIX::matches).findFirst().map(NodeType.SUFFIX::cast).map(SuffixNode::getMetaValue);
    }

    static Optional<String> getDisplayName(PermissionHolder permissionHolder, QueryOptions queryOptions) {
        return permissionHolder.resolveDistinctInheritedNodes(queryOptions).stream()
                .filter(NodeType.DISPLAY_NAME::matches).findFirst().map(NodeType.DISPLAY_NAME::cast).map(DisplayNameNode::getDisplayName);
    }
}
