import com.clearance.retailer.model.*;
import com.clearance.retailer.domain.DealQuery;
import com.clearance.retailer.data.DemoCatalog;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Executable assertions over production Java, without Android or testing dependencies. */
public final class DomainTests {
    private static int passed;
    private static final LocalDate DAY = LocalDate.parse("2026-09-18");

    public static void main(String[] args) {
        check("sold-out defaults hidden, unknown stays visible", () -> {
            DealQuery.Filter f = new DealQuery.Filter();
            equal(ids(DealQuery.apply(fixtures(), f, Set.of())), "north-recent,north-old,south-drill,north-unknown");
        });
        check("sold-out opt-in sorts after unknown", () -> {
            DealQuery.Filter f = new DealQuery.Filter(); f.showSoldOut = true;
            equal(ids(DealQuery.apply(fixtures(), f, Set.of())), "north-recent,north-old,south-drill,north-unknown,north-sold");
        });
        check("physical store scope does not leak a same-name item", () -> {
            DealQuery.Filter f = new DealQuery.Filter(); f.storeId = "walmart-north";
            equal(ids(DealQuery.apply(fixtures(), f, Set.of())), "north-recent,north-old,north-unknown");
        });
        check("retailer category and case-insensitive trimmed search compose", () -> {
            DealQuery.Filter f = new DealQuery.Filter();
            f.retailerId = "walmart"; f.category = "Tools"; f.search = "  DRILL  ";
            equal(ids(DealQuery.apply(fixtures(), f, Set.of())), "north-recent,south-drill");
            f.retailerId = "target";
            equal(DealQuery.apply(fixtures(), f, Set.of()).size(), 0);
        });
        check("conflicting store and retailer cannot return other stores", () -> {
            DealQuery.Filter f = new DealQuery.Filter(); f.storeId = "walmart-north"; f.retailerId = "target";
            equal(DealQuery.apply(fixtures(), f, Set.of()).size(), 0);
        });
        check("saved is scoped to offer ID, not product title", () -> {
            DealQuery.Filter f = new DealQuery.Filter(); f.savedOnly = true;
            equal(ids(DealQuery.apply(fixtures(), f, Set.of("south-drill"))), "south-drill");
        });
        check("discount ordering uses percentage with stock tiers", () -> {
            DealQuery.Filter f = new DealQuery.Filter(); f.sort = DealQuery.Sort.DISCOUNT;
            equal(ids(DealQuery.apply(fixtures(), f, Set.of())), "north-old,north-recent,south-drill,north-unknown");
        });
        check("price ordering never promotes unknown above available", () -> {
            DealQuery.Filter f = new DealQuery.Filter(); f.sort = DealQuery.Sort.PRICE;
            equal(ids(DealQuery.apply(fixtures(), f, Set.of())), "north-old,south-drill,north-recent,north-unknown");
        });
        check("newest uses known clearance start rather than observation time", () -> {
            Deal old = offer("old", "walmart-north", "Desk", "Home", 1000, 2000, Deal.Stock.IN_STOCK, DAY, DAY.minusDays(10));
            Deal recent = offer("new", "walmart-north", "Lamp", "Home", 1000, 2000, Deal.Stock.IN_STOCK, DAY.minusDays(1), null);
            equal(ids(DealQuery.apply(List.of(old, recent), new DealQuery.Filter(), Set.of())), "new,old");
            equal(old.firstSeen, DAY);
            equal(old.clearanceSince, DAY.minusDays(10));
            equal(recent.clearanceSince, null);
        });
        check("query does not mutate catalog and ties are deterministic", () -> {
            List<Deal> input = new ArrayList<>(fixtures()); String before = ids(input);
            DealQuery.Filter f = new DealQuery.Filter(); f.sort = DealQuery.Sort.PRICE;
            DealQuery.apply(input, f, Set.of()); equal(ids(input), before);
            Deal b = offer("b", "walmart-north", "Same", "Home", 100, 200, Deal.Stock.IN_STOCK, DAY, null);
            Deal a = offer("a", "walmart-north", "Same", "Home", 100, 200, Deal.Stock.IN_STOCK, DAY, null);
            equal(ids(DealQuery.apply(List.of(b, a), new DealQuery.Filter(), Set.of())), "a,b");
        });
        check("discount handles unknown original without fake savings", () -> {
            Deal a = offer("a", "walmart-north", "A", "Home", 750, 1000, Deal.Stock.IN_STOCK, DAY, null);
            equal(a.discountPercent(), 25);
            Deal b = offer("b", "walmart-north", "B", "Home", 750, null, Deal.Stock.IN_STOCK, DAY, null);
            equal(b.discountPercent(), 0);
        });
        check("invalid money rejected", () -> {
            rejects(() -> offer("a", "walmart-north", "A", "Home", -1, 100, Deal.Stock.IN_STOCK, DAY, null));
            rejects(() -> offer("a", "walmart-north", "A", "Home", 100, 0, Deal.Stock.IN_STOCK, DAY, null));
            rejects(() -> offer("a", "walmart-north", "A", "Home", 200, 100, Deal.Stock.IN_STOCK, DAY, null));
        });
        check("history cannot disagree with displayed current price", () -> {
            rejects(() -> new Deal("a", "walmart-north", "walmart", "A", "Home", 100, 200,
                Deal.Stock.IN_STOCK, DAY, null, DAY, true, List.of(new Deal.PricePoint(DAY, 150))));
        });
        check("sample catalog covers all six retailers and distinct store prices", () -> {
            Set<String> retailerIds = DemoCatalog.deals().stream().map(d -> d.retailerId).collect(Collectors.toSet());
            equal(retailerIds, Set.of("walmart", "target", "home-depot", "lowes", "tractor-supply", "walgreens"));
            Set<String> stores = DemoCatalog.stores().stream().map(s -> s.id).collect(Collectors.toSet());
            Set<String> offerIds = new HashSet<>();
            for (Deal d : DemoCatalog.deals()) {
                require(d.isDemo, "Unlabeled fictional inventory");
                require(stores.contains(d.storeId), "Unresolvable store");
                require(offerIds.add(d.id), "Duplicate offer identity");
                require(!d.firstSeen.isAfter(d.checkedOn), "First seen after checked date");
            }
            require(stores.containsAll(Set.of("walmart-north", "walmart-south")), "Two physical store samples missing");
        });
        System.out.println("PASS: " + passed + " domain tests");
    }
    private static List<Deal> fixtures() {
        return List.of(
            offer("north-old", "walmart-north", "Lamp", "Home", 1000, 4000, Deal.Stock.IN_STOCK, DAY.minusDays(2), null),
            offer("north-unknown", "walmart-north", "Cable", "Electronics", 100, 1000, Deal.Stock.UNKNOWN, DAY, null),
            offer("south-drill", "walmart-south", "Cordless Drill", "Tools", 2000, 3000, Deal.Stock.IN_STOCK, DAY.minusDays(3), null),
            offer("north-sold", "walmart-north", "Kettle", "Home", 200, 2000, Deal.Stock.SOLD_OUT, DAY, null),
            offer("north-recent", "walmart-north", "Cordless Drill", "Tools", 3000, 6000, Deal.Stock.LIMITED, DAY.minusDays(1), null)
        );
    }
    private static Deal offer(String id, String store, String title, String category, int cents, Integer original,
                              Deal.Stock stock, LocalDate first, LocalDate clearance) {
        return new Deal(id, store, "walmart", title, category, cents, original, stock, first, clearance, DAY, true,
                List.of(new Deal.PricePoint(DAY, cents)));
    }
    private static String ids(List<Deal> items) { return items.stream().map(d -> d.id).collect(Collectors.joining(",")); }
    private static void check(String name, Runnable test) { test.run(); passed++; System.out.println("  PASS " + name); }
    private static void equal(Object actual, Object expected) { require(Objects.equals(actual, expected), "Expected " + expected + "; got " + actual); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void rejects(Runnable work) {
        try { work.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected invalid data to be rejected");
    }
}
