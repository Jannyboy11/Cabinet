package com.janboerman.cabinet.api;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

public final class BungeeGroup {

    private final String name;
    private final Instant endingTimeStamp;
    private final Set<String> servers;
    private final Set<String> worlds;
    private final OptionalInt weight;

    public BungeeGroup(String name) {
        this(name, null, null);
    }

    public BungeeGroup(String name, Set<String> servers, Set<String> worlds) {
        this(name, servers, worlds, null, OptionalInt.empty());
    }

    public BungeeGroup(String name, Set<String> servers, Set<String> worlds, Instant endingTimeStamp, OptionalInt weight) {
        this.name = name;
        this.endingTimeStamp = endingTimeStamp;
        this.servers = servers;
        this.worlds = worlds;
        this.weight = weight;
    }

    public BungeeGroup(String name, Set<String> servers, Set<String> worlds, Instant endingTimeStamp, Integer weight) {
        this(name, servers, worlds, endingTimeStamp, weight == null ? OptionalInt.empty() : OptionalInt.of(weight));
    }

    public String getName() {
        return name;
    }

    public boolean hasDuration() {
        return getEndingTimeStamp() != null;
    }

    public Instant getEndingTimeStamp() {
        return endingTimeStamp;
    }

    public boolean isServerSpecific() {
        return !getServers().isEmpty();
    }

    public Set<String> getServers() {
        return servers == null ? Collections.emptySet() : servers;
    }

    public boolean isWorldSpecific() {
        return !getWorlds().isEmpty();
    }

    public Set<String> getWorlds() {
        return worlds == null ? Collections.emptySet() : worlds;
    }

    public OptionalInt getWeight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof BungeeGroup)) return false;

        BungeeGroup that = (BungeeGroup) o;
        return Objects.equals(this.getName(), that.getName())
                && (!(this.hasDuration() && that.hasDuration()) || Objects.equals(this.getEndingTimeStamp(), that.getEndingTimeStamp()))
                && (!(this.isServerSpecific() && that.isServerSpecific()) || Objects.equals(this.getServers(), that.getServers()))
                && (!(this.isWorldSpecific() && that.isWorldSpecific()) || Objects.equals(this.getWorlds(), that.getWorlds()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getName(),
                hasDuration() ? getEndingTimeStamp() : null,
                isServerSpecific() ? getServers() : null,
                isWorldSpecific() ? getWorlds() : null);
    }

    @Override
    public String toString() {
        return name;
    }
}
