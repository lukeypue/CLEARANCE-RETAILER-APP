package com.clearance.retailer.data;

import android.content.SharedPreferences;
import com.clearance.retailer.model.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Synchronous, injectable lookup. Call from a background worker. Cache only complete responses. */
public final class StoreLookup {
    public static final long CACHE_MS=24*60*60*1000L;
    private static final long COOLDOWN_MS=30_000;
    private final SharedPreferences prefs;
    private final Transport transport;
    public interface Transport {String get(String url)throws IOException;}
    public static final class Snapshot {
        public final ZipLocation location;
        public final List<NearbyStore> stores;
        public final int radiusMiles;
        public final long fetchedAt;
        Snapshot(ZipLocation location,List<NearbyStore> stores,int radius,long time){
            this.location=location;this.stores=Collections.unmodifiableList(new ArrayList<>(stores));this.radiusMiles=radius;this.fetchedAt=time;
        }
        public boolean stale(long now){return now-fetchedAt>=CACHE_MS||now<fetchedAt;}
    }
    public StoreLookup(SharedPreferences prefs,Transport transport){this.prefs=prefs;this.transport=transport;}
    public Snapshot last(){String key=prefs.getString("last","");return read(prefs.getString(key,""));}
    public Snapshot cached(String zip,int radius){Snapshot s=read(prefs.getString(key(zip,radius),""));return s!=null&&s.location.zip.equals(zip)&&s.radiusMiles==radius?s:null;}
    private static String key(String zip,int radius){return "cache:"+zip+":"+radius;}
    public synchronized Snapshot find(String input,int radius,long now)throws IOException {
        String zip=ZipLocation.normalize(input);if(radius!=50&&radius!=100)throw new IllegalArgumentException("Invalid radius");
        Snapshot cached=cached(zip,radius);
        if(cached!=null&&!cached.stale(now)){prefs.edit().putString("last",key(zip,radius)).apply();return cached;}
        long last=prefs.getLong("attempt",0);
        if(last>0&&now-last<COOLDOWN_MS)throw new IOException("Please wait 30 seconds between store searches.");
        long window=prefs.getLong("window",0);int requests=prefs.getInt("requests",0);
        if(now-window>=CACHE_MS||now<window){window=now;requests=0;}
        if(requests>=20)throw new IOException("Today's test lookup limit has been reached. Saved ZIP searches still work; try a new ZIP tomorrow.");
        prefs.edit().putLong("attempt",now).putLong("window",window).putInt("requests",requests+1).apply();
        ZipLocation location=StoreData.zip(transport.get("https://api.zippopotam.us/us/"+zip),zip);
        String pattern="^(Wal.?mart|Target$|Super.?Target$|The Home Depot|Home Depot|Lowe|Tractor Supply|Walgreens)";
        String around=String.format(Locale.US,"(around:%d,%.7f,%.7f)",(int)Math.ceil(radius*1609.344),location.latitude,location.longitude);
        String query="[out:json][timeout:25];(nwr"+around+"[\"shop\"][\"name\"~\""+pattern+"\",i];nwr"+around+"[\"shop\"][\"brand\"~\""+pattern+"\",i];);out center tags;";
        String response=transport.get("https://overpass-api.de/api/interpreter?data="+URLEncoder.encode(query,"UTF-8"));
        Snapshot result=new Snapshot(location,StoreData.stores(response),radius,now);
        save(result);return result;
    }
    private void save(Snapshot s)throws IOException {
        try{
            JSONArray stores=new JSONArray();for(NearbyStore store:s.stores)stores.put(StoreData.encode(store));
            JSONObject j=new JSONObject().put("zip",s.location.zip).put("city",s.location.city).put("state",s.location.state)
                .put("lat",s.location.latitude).put("lon",s.location.longitude).put("radius",s.radiusMiles).put("at",s.fetchedAt).put("stores",stores);
            String newKey=key(s.location.zip,s.radiusMiles);SharedPreferences.Editor edit=prefs.edit();
            List<String> keys=new ArrayList<>();for(String k:prefs.getAll().keySet())if(k.startsWith("cache:")&&!k.equals(newKey))keys.add(k);
            keys.sort(Comparator.comparingLong(k->{Snapshot v=read(prefs.getString(k,""));return v==null?0:v.fetchedAt;}));
            while(keys.size()>=8)edit.remove(keys.remove(0));
            edit.putString(newKey,j.toString()).putString("last",newKey).apply();
        }catch(JSONException e){throw new IOException("Could not save store results.",e);}
    }
    private static Snapshot read(String value){
        try{
            JSONObject j=new JSONObject(value);ZipLocation zip=new ZipLocation(j.getString("zip"),j.getString("city"),j.getString("state"),j.getDouble("lat"),j.getDouble("lon"));
            List<NearbyStore> stores=new ArrayList<>();JSONArray entries=j.getJSONArray("stores");for(int i=0;i<entries.length();i++)stores.add(StoreData.decode(entries.getJSONObject(i)));
            int radius=j.getInt("radius");if(radius!=50&&radius!=100)return null;
            return new Snapshot(zip,stores,radius,j.getLong("at"));
        }catch(JSONException|IllegalArgumentException e){return null;}
    }
    public static String https(String url)throws IOException {
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(10_000);connection.setReadTimeout(40_000);connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent","ClearanceRetailer/0.2 (https://github.com/lukeypue/CLEARANCE-RETAILER-APP)");
        connection.setRequestProperty("Accept","application/json");
        try{
            int status=connection.getResponseCode();
            if(status==404)throw new IOException("That ZIP was not found. Check the five digits and try again.");
            if(status==429||status==406)throw new IOException("The store service is busy. Wait at least 30 seconds and try again.");
            if(status!=200)throw new IOException("The location service is unavailable ("+status+"). Please try later.");
            try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int n;
                while((n=in.read(buffer))!=-1){if(out.size()+n>2_000_000)throw new IOException("The store response was too large. Try a smaller radius.");out.write(buffer,0,n);}
                return new String(out.toByteArray(),StandardCharsets.UTF_8);
            }
        }finally{connection.disconnect();}
    }
}
