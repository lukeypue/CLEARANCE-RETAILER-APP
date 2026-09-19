package com.clearance.retailer.domain;

import com.clearance.retailer.model.*;
import java.util.*;

public final class NearbyQuery {
    private NearbyQuery(){}
    public static String retailer(Map<String,String> tags) {
        String name=clean(tags.get("name")),brand=clean(tags.get("brand")),shop=clean(tags.get("shop"));
        if(shop.equals("vacant")||shop.equals("no")||name.matches(".*\\b(fuel|gas|distribution|fulfillment|garden center|auto care|vision center|photo center)\\b.*"))return "";
        if(name.contains("pharmacy")&&!name.equals("walgreens pharmacy"))return "";
        if(name.contains("warehouse")&&!name.matches("(lowe s|lowes) home improvement warehouse"))return "";
        if("yes".equals(clean(tags.get("proposed:building"))))return "";
        for(String key:Arrays.asList("disused","abandoned","demolished","construction"))
            if("yes".equals(tags.get(key))||tags.containsKey(key+":shop"))return "";
        if(name.startsWith("lowes foods")||name.startsWith("lowe s foods"))return "";
        // A pharmacy inside a chain is not another physical store. Walgreens is a pharmacy retailer.
        if((shop.equals("chemist")||shop.equals("pharmacy")||"pharmacy".equals(tags.get("amenity")))
                &&!name.equals("walgreens")&&!name.equals("walgreens pharmacy")&&!brand.equals("walgreens"))return "";
        String result=classify(name);
        return result.isEmpty()?classify(brand):result;
    }
    private static String clean(String s){return s==null?"":s.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+"," ").trim().replaceAll(" +"," ");}
    private static String classify(String name){
        if(name.matches("(walmart|wal mart)( supercenter| super center| neighborhood market| discount store)?"))return "walmart";
        if(name.equals("target")||name.equals("super target")||name.equals("supertarget"))return "target";
        if(name.equals("home depot")||name.equals("the home depot"))return "home-depot";
        if(name.matches("(lowe s|lowes)( home improvement| home improvement warehouse)?"))return "lowes";
        if(name.matches("tractor supply( co| company)?"))return "tractor-supply";
        if(name.equals("walgreens")||name.equals("walgreens pharmacy"))return "walgreens";
        return "";
    }
    public static double miles(double lat1,double lon1,double lat2,double lon2){
        double dlat=Math.toRadians(lat2-lat1),dlon=Math.toRadians(lon2-lon1);
        double a=Math.sin(dlat/2)*Math.sin(dlat/2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dlon/2)*Math.sin(dlon/2);
        return 3958.7613*2*Math.asin(Math.sqrt(Math.min(1,Math.max(0,a))));
    }
    public static List<NearbyStore> nearest(List<NearbyStore> input,double lat,double lon,int count,int radiusMiles){
        if((count!=3&&count!=5)||radiusMiles<=0)throw new IllegalArgumentException("Invalid search options");
        ZipLocation.validateCoordinates(lat,lon);
        List<NearbyStore> sorted=new ArrayList<>(input);
        sorted.sort(Comparator.comparingDouble((NearbyStore s)->miles(lat,lon,s.latitude,s.longitude)).thenComparing(s->s.id));
        List<NearbyStore> selected=new ArrayList<>();
        for(Retailer retailer:Retailer.values()){
            List<NearbyStore> chain=new ArrayList<>();
            for(NearbyStore s:sorted){
                if(!retailer.id.equals(s.retailerId)||miles(lat,lon,s.latitude,s.longitude)>radiusMiles)continue;
                boolean duplicate=false;
                for(NearbyStore old:chain)if(old.id.equals(s.id)||miles(old.latitude,old.longitude,s.latitude,s.longitude)<.12){duplicate=true;break;}
                if(!duplicate)chain.add(s);
                if(chain.size()==count)break;
            }
            selected.addAll(chain);
        }
        return Collections.unmodifiableList(selected);
    }
}
