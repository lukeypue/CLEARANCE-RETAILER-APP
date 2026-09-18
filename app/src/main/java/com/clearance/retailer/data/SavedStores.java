package com.clearance.retailer.data;

import android.content.SharedPreferences;
import com.clearance.retailer.model.NearbyStore;
import org.json.*;
import java.util.*;

public final class SavedStores {
    private final SharedPreferences prefs;
    public SavedStores(SharedPreferences prefs){this.prefs=prefs;}
    public List<NearbyStore> all(){
        List<NearbyStore> stores=new ArrayList<>();
        try{JSONArray list=new JSONArray(prefs.getString("stores","[]"));for(int i=0;i<list.length();i++){
            try{stores.add(StoreData.decode(list.getJSONObject(i)));}catch(JSONException|IllegalArgumentException ignored){}
        }}catch(JSONException ignored){}
        return stores;
    }
    public boolean contains(String id){for(NearbyStore s:all())if(s.id.equals(id))return true;return false;}
    public boolean toggle(NearbyStore store){
        List<NearbyStore> stores=all();boolean removed=stores.removeIf(s->s.id.equals(store.id));if(!removed)stores.add(store);
        JSONArray entries=new JSONArray();try{for(NearbyStore s:stores)entries.put(StoreData.encode(s));}catch(JSONException e){throw new IllegalStateException(e);}
        prefs.edit().putString("stores",entries.toString()).apply();return !removed;
    }
}
