package com.clearance.retailer.data;

import com.clearance.retailer.model.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Fictional samples, never fetched from or attributed to live retailer inventory. */
public final class DemoCatalog {
    public static final LocalDate AS_OF = LocalDate.of(2026, 9, 18);
    private DemoCatalog() { }
    public static List<Store> stores() {
        return Arrays.asList(
            new Store("walmart-north", "walmart", "North · sample store", true),
            new Store("walmart-south", "walmart", "South · sample store", true),
            new Store("target-central", "target", "Central · sample store", true),
            new Store("home-depot-west", "home-depot", "West · sample store", true),
            new Store("lowes-east", "lowes", "East · sample store", true),
            new Store("tractor-supply-north", "tractor-supply", "North · sample store", true),
            new Store("walgreens-central", "walgreens", "Central · sample store", true)
        );
    }
    public static Store store(String id) {
        for (Store s : stores()) if (s.id.equals(id)) return s;
        throw new IllegalArgumentException("Unknown sample store");
    }
    public static List<String> categories() {
        return Arrays.asList("Home", "Electronics", "Tools", "Outdoor", "Toys", "Clothing", "Health & Beauty", "Pets");
    }
    public static List<Deal> deals() {
        List<Deal> deals = new ArrayList<>();
        add(deals,"wm-n-airfryer","walmart-north","Compact air fryer","Home",2400,5900,Deal.Stock.IN_STOCK,0,false);
        add(deals,"wm-n-headphones","walmart-north","Wireless over-ear headphones","Electronics",1800,4999,Deal.Stock.LIMITED,1,false);
        add(deals,"wm-n-drill","walmart-north","Cordless drill kit","Tools",3500,7900,Deal.Stock.IN_STOCK,3,true);
        add(deals,"wm-n-toy","walmart-north","Wooden building set","Toys",900,2499,Deal.Stock.SOLD_OUT,2,false);
        add(deals,"wm-n-lamp","walmart-north","Rechargeable desk lamp","Home",700,2400,Deal.Stock.UNKNOWN,1,false);
        add(deals,"wm-s-airfryer","walmart-south","Compact air fryer","Home",3200,5900,Deal.Stock.LIMITED,2,false);
        add(deals,"wm-s-hoodie","walmart-south","Everyday fleece hoodie","Clothing",800,2200,Deal.Stock.IN_STOCK,1,false);
        add(deals,"wm-s-chair","walmart-south","Folding camp chair","Outdoor",1400,2999,Deal.Stock.UNKNOWN,4,false);
        add(deals,"tg-sheets","target-central","Cotton queen sheet set","Home",2200,5500,Deal.Stock.IN_STOCK,0,false);
        add(deals,"tg-bricks","target-central","Space explorer building kit","Toys",1250,3499,Deal.Stock.LIMITED,1,false);
        add(deals,"tg-speaker","target-central","Portable Bluetooth speaker","Electronics",1600,3999,Deal.Stock.IN_STOCK,4,true);
        add(deals,"hd-saw","home-depot-west","Compact circular saw","Tools",4900,9900,Deal.Stock.IN_STOCK,0,false);
        add(deals,"hd-planter","home-depot-west","Tall ceramic planter","Outdoor",1700,4499,Deal.Stock.LIMITED,2,false);
        add(deals,"hd-light","home-depot-west","LED workshop light","Tools",1900,3999,Deal.Stock.SOLD_OUT,3,false);
        add(deals,"lw-trimmer","lowes-east","Cordless grass trimmer","Outdoor",3900,8900,Deal.Stock.IN_STOCK,1,false);
        add(deals,"lw-tap","lowes-east","Single-handle kitchen faucet","Home",2900,6900,Deal.Stock.LIMITED,5,true);
        add(deals,"lw-toolbox","lowes-east","Stackable tool organizer","Tools",1900,3999,Deal.Stock.UNKNOWN,0,false);
        add(deals,"ts-bed","tractor-supply-north","Washable pet bed","Pets",1500,3999,Deal.Stock.IN_STOCK,0,false);
        add(deals,"ts-boots","tractor-supply-north","Waterproof garden boots","Clothing",2000,4999,Deal.Stock.LIMITED,2,false);
        add(deals,"ts-feeder","tractor-supply-north","Hanging bird feeder","Outdoor",800,1999,Deal.Stock.SOLD_OUT,4,false);
        add(deals,"wg-brush","walgreens-central","Rechargeable toothbrush","Health & Beauty",1200,2999,Deal.Stock.IN_STOCK,1,false);
        add(deals,"wg-care","walgreens-central","Skin care gift set","Health & Beauty",750,2400,Deal.Stock.LIMITED,3,true);
        add(deals,"wg-cable","walgreens-central","Braided USB-C cable","Electronics",400,null,Deal.Stock.UNKNOWN,2,false);
        return Collections.unmodifiableList(deals);
    }
    private static void add(List<Deal> list, String id, String storeId, String title, String category,
                            int price, Integer original, Deal.Stock stock, int daysAgo, boolean hasKnownDate) {
        Store store = store(storeId);
        LocalDate first = AS_OF.minusDays(daysAgo);
        List<Deal.PricePoint> history = new ArrayList<>();
        if (daysAgo > 0 && original != null) history.add(new Deal.PricePoint(first, price + (original - price) / 2));
        history.add(new Deal.PricePoint(AS_OF, price));
        list.add(new Deal(id, storeId, store.retailerId, title, category, price, original, stock, first,
            hasKnownDate ? first : null, AS_OF, true, history));
    }
}
