// com.example.ttcarburant.model.enums.Semestre.java
package com.example.ttcarburant.model.enums;

public enum Semestre {
    PREMIER(1),
    DEUXIEME(2);

    private final int value;

    Semestre(int value) { this.value = value; }

    public int getValue() { return value; }

    public static Semestre fromValue(int value) {
        for (Semestre s : values()) if (s.value == value) return s;
        throw new IllegalArgumentException("Semestre invalide : " + value);
    }
}