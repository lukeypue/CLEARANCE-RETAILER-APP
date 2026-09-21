package com.clearance.retailer.model;

public enum Retailer {
    WALMART("walmart", "Walmart", "W"), TARGET("target", "Target", "T"),
    HOME_DEPOT("home-depot", "Home Depot", "H"), LOWES("lowes", "Lowe’s", "L"),
    TRACTOR_SUPPLY("tractor-supply", "Tractor Supply", "T"), WALGREENS("walgreens", "Walgreens", "W");
    public final String id, label, letter;
    Retailer(String id, String label, String letter) { this.id = id; this.label = label; this.letter = letter; }
    public static Retailer byId(String id) {
        for (Retailer r : values()) if (r.id.equals(id)) return r;
        throw new IllegalArgumentException("Unknown retailer");
    }
}
