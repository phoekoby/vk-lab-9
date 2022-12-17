package org.example;

public enum ClanEvent {
    ACTIVATE(true),
    DISACTIVATE(false);

    private final Boolean value;

    ClanEvent(Boolean value) {
        this.value = value;
    }

    public Boolean getValue() {
        return value;
    }
}
