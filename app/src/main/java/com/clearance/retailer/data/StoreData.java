package com.clearance.retailer.data;

import com.clearance.retailer.domain.NearbyQuery;
import com.clearance.retailer.model.*;
import org.json.*;
import java.io.IOException;
import java.util.*;

/** Parses location data only. It never creates product or stock records. */
public final class StoreData {
    private StoreData(){}
    public static ZipLocation zip(String body,String requested) throws IOException {
        try{
            JSONObject root=new JSONObject(body);
            if(!requested.equals(root.getString("post code")))throw new JSONException("Mismatched ZIP");
            JSONObject place=root.getJSONArray("places").getJSONObject(0);
            return new ZipLocation(requested,place.getString("place name"),place.getString("state abbreviation"),
                Double.parseDouble(place.getString("latitude")),Double.parseDouble(place.getString("longitude")));
        }catch(JSONException|IllegalArgumentException e){throw new IOException("The ZIP service returned an unreadable location. Please try later.",e);}
    }
    public static List<NearbyStore> stores(String body) throws IOException {
        try{
            JSONObject root=new JSONObject(body);
            if(!root.optString("remark").isEmpty())throw new JSONException("Incomplete map response");
            JSONArray elements=root.getJSONArray("elements");List<NearbyStore> stores=new ArrayList<>();int malformed=0;
            for(int i=0;i<elements.length();i++){
                JSONObject item=elements.getJSONObject(i);
                JSONObject tags=item.getJSONObject("tags");
                Map<String,String> values=new HashMap<>();Iterator<String> keys=tags.keys();
                while(keys.hasNext()){String key=keys.next();values.put(key,tags.optString(key));}
                String retailer=NearbyQuery.retailer(values);if(retailer.isEmpty())continue;
                try{
                    JSONObject point=item.has("center")?item.getJSONObject("center"):item;
                    String type=item.getString("type");if(!Arrays.asList("node","way","relation").contains(type))throw new JSONException("Invalid map identity");
                    String address=address(tags);String branch=tags.optString("branch").trim();
                    String name=Retailer.byId(retailer).label+(branch.isEmpty()?"":" · "+branch);
                    stores.add(new NearbyStore("osm-"+type+"-"+item.getLong("id"),retailer,name,address,point.getDouble("lat"),point.getDouble("lon")));
                }catch(JSONException|IllegalArgumentException e){malformed++;}
            }
            if(malformed>0)throw new JSONException("Some mapped stores could not be read");
            return Collections.unmodifiableList(stores);
        }catch(JSONException e){throw new IOException("The store service returned incomplete data. Please try later.",e);}
    }
    private static String address(JSONObject tags){
        String street=(tags.optString("addr:housenumber")+" "+tags.optString("addr:street")).trim();
        List<String> parts=new ArrayList<>();if(!street.isEmpty())parts.add(street);
        for(String key:Arrays.asList("addr:unit","addr:city")){String s=tags.optString(key).trim();if(!s.isEmpty())parts.add(s);}
        String stateZip=(tags.optString("addr:state")+" "+tags.optString("addr:postcode")).trim();if(!stateZip.isEmpty())parts.add(stateZip);
        return String.join(", ",parts);
    }
    static JSONObject encode(NearbyStore s) throws JSONException {
        return new JSONObject().put("id",s.id).put("retailer",s.retailerId).put("name",s.name).put("address",s.address).put("lat",s.latitude).put("lon",s.longitude);
    }
    static NearbyStore decode(JSONObject j) throws JSONException {
        return new NearbyStore(j.getString("id"),j.getString("retailer"),j.getString("name"),j.getString("address"),j.getDouble("lat"),j.getDouble("lon"));
    }
}
