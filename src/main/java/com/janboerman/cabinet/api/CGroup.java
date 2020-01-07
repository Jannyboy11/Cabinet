package com.janboerman.cabinet.api;

import java.util.Objects;
import java.util.OptionalInt;

//TODO implements Constable
public final class CGroup {

    private final String name;
    private final OptionalInt weight;
    private final String displayName;
    private final String prefix;
    private final String suffix;

    public CGroup(String name) {
        this(name, OptionalInt.empty());
    }

    public CGroup(String name, int weight) {
        this(name, OptionalInt.of(weight));
    }

    public CGroup(String name, Integer weight) {
        this(name, weight == null ? OptionalInt.empty() : OptionalInt.of(weight));
    }

    public CGroup(String name, OptionalInt weight) {
        this(name, weight, null, null, null);
    }

    public CGroup(String name, OptionalInt weight, String displayName, String prefix, String suffix) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.weight = Objects.requireNonNull(weight, "weight cannot be null");
        this.displayName = displayName;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public String getName() {
        return name;
    }

    public OptionalInt getWeight() {
        return weight;
    }

    public boolean hasDisplayName() {
        String displayName = getDisplayName();
        return displayName != null && !displayName.isEmpty();
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean hasPrefix() {
        String prefix = getPrefix();
        return prefix != null && !prefix.isEmpty();
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean hasSuffix() {
        String suffix = getSuffix();
        return suffix != null && !suffix.isEmpty();
    }

    public String getSuffix() {
        return suffix;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CGroup)) return false;

        CGroup that = (CGroup) o;
        return Objects.equals(this.getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getName());
    }

    @Override
    public String toString() {
        return name;
    }
}
