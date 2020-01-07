package com.janboerman.cabinet.api;

import java.time.Instant;
import java.util.Objects;

//TODO implements Constable
public final class CPermission {

    private final String permission;
    private final boolean value;
    private final Instant endingTimeStamp;

    public CPermission(String permission) {
        this(permission, true, null);
    }

    public CPermission(String permission, boolean value) {
        this(permission, value, null);
    }

    public CPermission(String permission, Instant endingTimestamp) {
        this(permission, true, endingTimestamp);
    }

    public CPermission(String permission, boolean value, Instant endingTimestamp) {
        this.permission = Objects.requireNonNull(permission, "value cannot be null");
        this.value = value;
        this.endingTimeStamp = endingTimestamp;
    }

    public String getPermission() {
        return permission;
    }

    public boolean getValue() {
        return value;
    }

    public boolean hasDuration() {
        return getEndingTimeStamp() != null;
    }

    public Instant getEndingTimeStamp() {
        return endingTimeStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CPermission)) return false;

        CPermission that = (CPermission) o;
        return Objects.equals(this.getPermission(), that.getPermission())
                && (this.getValue() == that.getValue())
                && (!(this.hasDuration() && that.hasDuration()) || Objects.equals(this.getEndingTimeStamp(), that.getEndingTimeStamp()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getPermission(),
                getValue(),
                hasDuration() ? getEndingTimeStamp() : 0L);
    }

    @Override
    public String toString() {
        return permission;
    }

}

