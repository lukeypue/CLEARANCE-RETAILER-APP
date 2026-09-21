package com.clearance.retailer.model;

public final class Store {
    public final String id, retailerId, name;
    public final boolean isDemo;
    public Store(String id, String retailerId, String name, boolean isDemo) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Store identity is required");
        Retailer.byId(retailerId);
        this.id = id; this.retailerId = retailerId; this.name = name; this.isDemo = isDemo;
    }
    public String displayName() { return Retailer.byId(retailerId).label + " · " + name; }
}
