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
    /**
     * Vérifie si ce créneau horaire couvre le créneau passé en paramètre.
     *
     * Logique de couverture :
     * - ALL_DAY couvre tous les créneaux (MORNING et AFTERNOON)
     * - MORNING ne couvre que MORNING
     * - AFTERNOON ne couvre que AFTERNOON
     *
     * @param timeSlot Le créneau à vérifier
     * @return true si ce créneau couvre le paramètre, false sinon
     */
    public Boolean cover(TimeSlot timeSlot) {
        // Si ce créneau est "Toute la journée", il couvre tous les créneaux
        if (this == ALL_DAY) {
            return true;
        }

        // Sinon, vérifie l'égalité stricte des créneaux
        return this == timeSlot;
    }
}
