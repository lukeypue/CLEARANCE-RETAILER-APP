package com.clearance.retailer.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** One offer at one physical store. Money is USD cents, dates are catalog dates. */
public final class Deal {
    public enum Stock {
        IN_STOCK("In stock", 0), LIMITED("Limited stock", 0), UNKNOWN("Stock unknown", 1), SOLD_OUT("Sold out", 2);
        public final String label; public final int tier;
        Stock(String label, int tier) { this.label = label; this.tier = tier; }
    }
    public static final class PricePoint {
        public final LocalDate date; public final int cents;
        public PricePoint(LocalDate date, int cents) {
            if (date == null || cents <= 0) throw new IllegalArgumentException("Invalid price observation");
            this.date = date; this.cents = cents;
        }
    }
    public final String id, storeId, retailerId, title, category;
    public final int priceCents;
    public final Integer originalCents;
    public final Stock stock;
    public final LocalDate firstSeen, clearanceSince, checkedOn;
    public final boolean isDemo;
    public final List<PricePoint> history;

    public Deal(String id, String storeId, String retailerId, String title, String category,
                int priceCents, Integer originalCents, Stock stock, LocalDate firstSeen,
                LocalDate clearanceSince, LocalDate checkedOn, boolean isDemo, List<PricePoint> history) {
        this.id = required(id); this.storeId = required(storeId); this.retailerId = required(retailerId);
        Retailer.byId(retailerId);
        this.title = required(title); this.category = required(category);
        if (priceCents <= 0 || (originalCents != null && originalCents < priceCents))
            throw new IllegalArgumentException("Invalid current or original price");
        if (firstSeen == null || checkedOn == null || firstSeen.isAfter(checkedOn)
                || (clearanceSince != null && clearanceSince.isAfter(checkedOn)))
            throw new IllegalArgumentException("Invalid offer dates");
        this.priceCents = priceCents; this.originalCents = originalCents;
        this.stock = Objects.requireNonNull(stock); this.firstSeen = firstSeen;
        this.clearanceSince = clearanceSince; this.checkedOn = checkedOn; this.isDemo = isDemo;
        ArrayList<PricePoint> points = new ArrayList<>(history);
        points.sort((a, b) -> a.date.compareTo(b.date));
        LocalDate previous = null;
        for (PricePoint p : points) {
            if (p.date.isBefore(firstSeen) || p.date.isAfter(checkedOn) || p.date.equals(previous))
                throw new IllegalArgumentException("Invalid history dates");
            previous = p.date;
        }
        if (!points.isEmpty() && points.get(points.size() - 1).cents != priceCents)
            throw new IllegalArgumentException("Latest history must match current price");
        this.history = Collections.unmodifiableList(points);
    }
    private static String required(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Missing offer identity");
        return value;
    }
    public int discountPercent() {
        if (originalCents == null) return 0;
        return (int) ((originalCents - (long) priceCents) * 100 / originalCents);
    }
    public LocalDate discoveryDate() { return clearanceSince == null ? firstSeen : clearanceSince; }
    public String dateLabel() { return clearanceSince == null ? "First spotted" : "Clearance since"; }
}
