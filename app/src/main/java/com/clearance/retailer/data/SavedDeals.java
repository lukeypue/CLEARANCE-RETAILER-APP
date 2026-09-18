package com.clearance.retailer.data;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public final class SavedDeals {
    private final SharedPreferences prefs;
    public SavedDeals(Context context) { prefs = context.getSharedPreferences("saved_deals", Context.MODE_PRIVATE); }
    public Set<String> ids() { return new HashSet<>(prefs.getStringSet("ids", new HashSet<>())); }
    public boolean toggle(String id) {
        Set<String> values = ids();
        boolean saved = values.add(id);
        if (!saved) values.remove(id);
        prefs.edit().putStringSet("ids", values).apply();
        return saved;
    }
}
