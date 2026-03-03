
package com.ubb.gui;

public enum DuckCapability {
    ALL("Toate Rațele"),
    SWIMMING("Înotătoare"),
    FLYING("Zburătoare"),
    DUAL("Duală");

    private final String display;

    DuckCapability(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        return display;
    }
}