package com.clearance.retailer.domain;

import com.clearance.retailer.model.Deal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DealQuery {
    private DealQuery() { }
    public enum Sort { NEWEST, DISCOUNT, PRICE }
    public static final class Filter {
        public String retailerId = "", storeId = "", category = "", search = "";
        public boolean showSoldOut = false, savedOnly = false;
        public Sort sort = Sort.NEWEST;
    }
    public static List<Deal> apply(List<Deal> catalog, Filter filter, Set<String> saved) {
        List<Deal> result = new ArrayList<>();
        String needle = filter.search.trim().toLowerCase(Locale.ROOT);
        for (Deal d : catalog) {
            if (!filter.showSoldOut && d.stock == Deal.Stock.SOLD_OUT) continue;
            if (!filter.retailerId.isEmpty() && !filter.retailerId.equals(d.retailerId)) continue;
            if (!filter.storeId.isEmpty() && !filter.storeId.equals(d.storeId)) continue;
            if (!filter.category.isEmpty() && !filter.category.equals(d.category)) continue;
            if (filter.savedOnly && !saved.contains(d.id)) continue;
            if (!(d.title + " " + d.category).toLowerCase(Locale.ROOT).contains(needle)) continue;
            result.add(d);
        }
        Comparator<Deal> byChoice;
        switch (filter.sort) {
            case DISCOUNT: byChoice = Comparator.comparingInt(Deal::discountPercent).reversed(); break;
            case PRICE: byChoice = Comparator.comparingInt(d -> d.priceCents); break;
            default: byChoice = Comparator.comparing(Deal::discoveryDate).reversed(); break;
        }
        result.sort(Comparator.<Deal>comparingInt(d -> d.stock.tier).thenComparing(byChoice).thenComparing(d -> d.id));
        return result;
    }
}
