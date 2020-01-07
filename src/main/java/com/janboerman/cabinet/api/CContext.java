package com.janboerman.cabinet.api;

import java.util.*;

public class CContext {

    private static final CContext GLOBAL = new CContext(Collections.emptySet(), Collections.emptySet());

    private Set<String> servers;
    private Set<String> worlds;

    private CContext(Set<String> servers, Set<String> worlds) {
        if (!servers.isEmpty() && servers.stream().anyMatch(server -> server == null || server.isEmpty()))
            throw new IllegalArgumentException("servers cannot contain null or an empty string");
        if (!worlds.isEmpty() && worlds.stream().anyMatch(world -> world == null || world.isEmpty()))
            throw new IllegalArgumentException("worlds cannot contain null or an empty string");
        //defensive copying has already been done
        this.servers = servers;
        this.worlds = worlds;
    }

    public static CContext onServers(String... servers) {
        if (servers.length == 0) {
            return global();
        } else if (servers.length == 1) {
            return new CContext(Collections.singleton(servers[0]), Collections.emptySet());
        } else {
            //can't use Set#of in Java 8 :(
            return new CContext(Collections.unmodifiableSet(new HashSet<>(Arrays.asList(servers))), Collections.emptySet());
        }
    }

    public static CContext onWorlds(String server, String... worlds) {
        Set<String> servers = Collections.singleton(server);
        if (worlds.length == 0) {
            return new CContext(servers, Collections.emptySet());
        } else if (worlds.length == 1) {
            return new CContext(servers, Collections.singleton(worlds[0]));
        } else {
            //can't use Set#of in Java 8 :(
            return new CContext(servers, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(worlds))));
        }
    }

    public static CContext on(Collection<String> servers, Collection<String> worlds) {
        Objects.requireNonNull(servers, "servers cannot be null");
        Objects.requireNonNull(worlds, "worlds cannot be null");

        if (servers.isEmpty() && worlds.isEmpty()) {
            return global();
        } else {
            return new CContext(Collections.unmodifiableSet(new HashSet<>(servers)), Collections.unmodifiableSet(new HashSet<>(worlds)));
        }
    }

    public static CContext global() {
        return GLOBAL;
    }

    public boolean isServerSensitive() {
        return !servers.isEmpty();
    }

    public boolean isWorldSensitive() {
        return !worlds.isEmpty();
    }

    public boolean isGlobal() {
        return this == GLOBAL || (!isServerSensitive() && !isWorldSensitive());
    }

    public Set<String> getServers() {
        return servers;
    }

    public Set<String> getWorlds() {
        return worlds;
    }
}
