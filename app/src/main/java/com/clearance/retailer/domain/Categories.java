package com.clearance.retailer.domain;

import java.util.*;

public final class Categories {
    private Categories(){}
    private static final List<String> ALL=Collections.unmodifiableList(Arrays.asList(
        "Appliances","Automotive","Baby","Bath","Bedding","Books","Clothing","Computers",
        "Crafts & Sewing","Electrical","Electronics","Farm & Ranch","Flooring","Furniture",
        "Gaming","Grocery","Hardware","Health & Beauty","Home","Household Essentials",
        "Jewelry & Accessories","Kitchen & Dining","Lawn & Garden","Lighting","Office & School",
        "Outdoor","Paint","Patio","Pets","Plumbing","Seasonal","Shoes","Sports & Fitness",
        "Storage & Organization","Tools","Toys"));
    public static List<String> all(){return ALL;}
}
