package com.janboerman.cabinet.api;

import java.util.Objects;
import java.util.UUID;

/** @deprecated not sure if this class will continue to exist. */
@Deprecated
public final class PlayerId {

    private final UUID uniqueId;
    private final String userName;

    private PlayerId(UUID uniqueId, String userName) {
        this.uniqueId = uniqueId;
        this.userName = userName;
    }

    public PlayerId(UUID uniqueId) {
        this(Objects.requireNonNull(uniqueId, "uniqueId cannot be null"), null);
    }

    public PlayerId(String userName) {
        this(null, Objects.requireNonNull(userName,"userName cannot be null"));
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerId)) return false;
        PlayerId playerId = (PlayerId) o;
        return Objects.equals(uniqueId, playerId.uniqueId) &&
                Objects.equals(userName, playerId.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId, userName);
    }
}
