import com.clearance.retailer.model.*;
import com.clearance.retailer.domain.*;
import java.util.*;

public final class NearbyTests {
    private static int passed;
    public static void main(String[] args) {
        check("ZIP validation preserves leading zeros", () -> {
            eq(ZipLocation.normalize(" 00501 "), "00501");
            for (String bad : Arrays.asList("", "8404", "840430", "84043-1234", "abcde", "１２３４５"))
                rejects(() -> ZipLocation.normalize(bad));
        });
        check("geography rejects malformed coordinates", () -> {
            rejects(() -> new ZipLocation("84043", "Lehi", "UT", Double.NaN, 0));
            rejects(() -> new ZipLocation("84043", "Lehi", "UT", 91, 0));
            near(NearbyQuery.miles(0, 0, 0, 1), 69.093, .01);
            near(NearbyQuery.miles(40, -111, 40, -111), 0, .00001);
        });
        check("classifies all six chains without Flowers false positive", () -> {
            String[][] names={{"Walmart Supercenter","walmart"},{"Wal-Mart","walmart"},{"Target","target"},
                {"The Home Depot","home-depot"},{"Lowe’s Home Improvement","lowes"},
                {"Tractor Supply Co.","tractor-supply"},{"Walgreens","walgreens"},{"Walgreens Pharmacy","walgreens"},
                {"Lowe's Home Improvement Warehouse","lowes"}};
            for(String[] entry:names)eq(NearbyQuery.retailer(Map.of("name",entry[0])),entry[1]);
            for(String bad:List.of("Flowers on Main","Target Practice","Walmart Pharmacy","Walmart Fuel Station","Lowes Foods"))
                eq(NearbyQuery.retailer(Map.of("name",bad)), "");
            eq(NearbyQuery.retailer(Map.of("name","Walmart", "shop","vacant")), "");
            eq(NearbyQuery.retailer(Map.of("name","Target", "disused","yes")), "");
            eq(NearbyQuery.retailer(Map.of("name","CVS Pharmacy", "brand","Target", "shop","chemist")), "");
            eq(NearbyQuery.retailer(Map.of("name","Walmart Garden Center", "brand","Walmart")), "");
            eq(NearbyQuery.retailer(Map.of("name","Walmart Auto Care Center", "brand","Walmart")), "");
        });
        check("proposed Target near 84414 is not an operating store", () -> {
            // OSM way 1493198240 has a normal shop tag alongside proposed:building=yes.
            eq(NearbyQuery.retailer(Map.of("name","Target", "brand","Target",
                "shop","supermarket", "proposed:building","yes")), "");
            eq(NearbyQuery.retailer(Map.of("name","Target", "shop","supermarket",
                "proposed:building","no")), "target");
        });
        check("nearest count applies to each retailer independently", () -> {
            List<NearbyStore> stores=new ArrayList<>();
            for(String brand:List.of("walmart","target"))for(int i=6;i>=1;i--)stores.add(shop(brand+i,brand,40+i*.01,-111));
            eq(NearbyQuery.nearest(stores,40,-111,3,50).size(),6);
            eq(NearbyQuery.nearest(stores,40,-111,5,50).size(),10);
            eq(NearbyQuery.nearest(stores,40,-111,3,50).get(0).id,"walmart1");
            eq(stores.get(0).id,"walmart6");
        });
        check("radius keeps fewer stores and includes its edge", () -> {
            List<NearbyStore> stores=List.of(shop("close","walmart",40.01,-111),shop("far","walmart",42,-111));
            eq(NearbyQuery.nearest(stores,40,-111,5,50).size(),1);
            rejects(() -> NearbyQuery.nearest(stores,40,-111,0,50));
        });
        check("duplicate node and building count once, other chains remain", () -> {
            List<NearbyStore> stores=List.of(shop("node-1","walmart",40.01,-111),shop("way-2","walmart",40.0101,-111),shop("target","target",40.01,-111));
            eq(NearbyQuery.nearest(stores,40,-111,5,50).size(),2);
        });
        check("equidistant ordering is stable", () -> {
            List<NearbyStore> stores=List.of(shop("b","target",40.01,-111.1),shop("a","target",40.01,-110.9));
            eq(NearbyQuery.nearest(stores,40,-111,3,50).get(0).id,"a");
        });
        check("category expansion covers omitted departments without duplicates", () -> {
            List<String> all=Categories.all();
            if(all.size()<35)throw new AssertionError("Too few categories");
            eq(new HashSet<>(all).size(),all.size());
            if(!all.containsAll(List.of("Baby","Grocery","Automotive","Appliances","Furniture","Farm & Ranch","Office & School","Seasonal","Shoes")))throw new AssertionError("Missing department");
        });
        System.out.println("PASS: "+passed+" nearby-store tests");
    }
    private static NearbyStore shop(String id,String retailer,double lat,double lon){return new NearbyStore(id,retailer,"Store "+id,"",lat,lon);}
    private static void eq(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError("Expected "+b+" got "+a);}
    private static void near(double a,double b,double tolerance){if(Math.abs(a-b)>tolerance)throw new AssertionError("Distance "+a+" != "+b);}
    private static void check(String name,Runnable test){test.run();passed++;System.out.println("  PASS "+name);}
    private static void rejects(Runnable action){try{action.run();}catch(IllegalArgumentException e){return;}throw new AssertionError("Expected rejection");}
}
