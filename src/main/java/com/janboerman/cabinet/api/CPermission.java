package com.janboerman.cabinet.api;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public final class CPermission {

    private final String value;
    private final boolean positive;
    private final Instant endingTimeStamp;
    private final Set<String> servers;
    private final Set<String> worlds;

    public CPermission(String value) {
        this(value, true, null);
    }

    public CPermission(String value, boolean positive) {
        this(value, positive, null);
    }

    public CPermission(String value, Instant endingTimestamp) {
        this(value, true, endingTimestamp);
    }

    public CPermission(String value, boolean positive, Instant endingTimestamp) {
        this(value, positive, endingTimestamp, null, null);
    }

    public CPermission(String value, Set<String> servers, Set<String> worlds) {
        this(value, true, null, servers, worlds);
    }

    public CPermission(String value, boolean positive, Set<String> servers, Set<String> worlds) {
        this(value, positive, null, servers, worlds);
    }

    public CPermission(String value, boolean positive, Instant endingTimestamp, Set<String> servers, Set<String> worlds) {
        this.value = value;
        this.positive = positive;
        this.endingTimeStamp = endingTimestamp;
        this.servers = servers;
        this.worlds = worlds;
    }

    public String getValue() {
        return value;
    }

    public boolean isPositive() {
        return positive;
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

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CPermission)) return false;

        CPermission that = (CPermission) o;
        return Objects.equals(this.getValue(), that.getValue())
                && (this.isPositive() == that.isPositive())
                && (!(this.hasDuration() && that.hasDuration()) || Objects.equals(this.getEndingTimeStamp(), that.getEndingTimeStamp()))
                && (!(this.isServerSpecific() && that.isServerSpecific()) || Objects.equals(this.getServers(), that.getServers()))
                && (!(this.isWorldSpecific() && that.isWorldSpecific()) || Objects.equals(this.getWorlds(), that.getWorlds()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getValue(),
                isPositive(),
                hasDuration() ? getEndingTimeStamp() : 0L,
                isServerSpecific() ? getServers() : null,
                isWorldSpecific() ? getWorlds() : null);
    }

    @Override
    public String toString() {
        return value;
    }
}
