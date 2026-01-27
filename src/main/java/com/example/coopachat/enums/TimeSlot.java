package com.example.coopachat.enums;

public enum TimeSlot {

    MORNING("Matin (8h - 12h)"),
    AFTERNOON("Après-midi (14h - 18h)"),
    ALL_DAY("Toute la journée");

    private final String displayName;

    TimeSlot(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
