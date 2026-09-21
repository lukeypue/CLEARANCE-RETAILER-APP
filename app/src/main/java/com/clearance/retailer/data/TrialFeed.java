package com.clearance.retailer.data;

import com.clearance.retailer.model.Retailer;
import com.clearance.retailer.model.ZipLocation;
import org.json.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/** Dated retailer observations. Never interpreted as guaranteed shelf inventory. */
public final class TrialFeed {
 public static final long OLD_MS=60*60*1000L;
 public final String zip; public final long generatedAt;
 public final List<Source> sources; public final List<Store> stores; public final List<Offer> offers;
 public static final class Source {public final String retailer,status,detail;Source(JSONObject j)throws JSONException{retailer=str(j,"retailer",40);Retailer.byId(retailer);status=str(j,"status",100);detail=str(j,"detail",600);}}
 public static final class Store {public final String id,retailer,name,address;public final double distance;Store(JSONObject j)throws JSONException{id=str(j,"id",40);retailer=str(j,"retailer",40);Retailer.byId(retailer);name=str(j,"name",150);address=str(j,"address",300);distance=j.getDouble("distance");if(!Double.isFinite(distance)||distance<0||distance>100)throw new IllegalArgumentException("Invalid distance");}}
 public static final class Offer {
  public final String id,retailer,storeId,productId,title,category,scope,url; public final int price;public final Integer original;public final boolean pickup;public final long observedAt;
  Offer(JSONObject j,long generated,Map<String,Store> stores)throws Exception{
   id=str(j,"id",150);retailer=str(j,"retailer",40);Retailer.byId(retailer);storeId=j.getString("store_id");productId=str(j,"product_id",60);title=str(j,"title",600);category=str(j,"category",80);scope=str(j,"scope",30);url=str(j,"url",2000);
   price=(int)integer(j,"price_cents",1,100000000);original=j.isNull("original_cents")?null:(int)integer(j,"original_cents",price,100000000);
   if(!(j.get("pickup") instanceof Boolean))throw new IllegalArgumentException("Invalid pickup");pickup=j.getBoolean("pickup");observedAt=integer(j,"observed_at",1577836800000L,generated);
   if(scope.equals("store_pickup")){Store s=stores.get(retailer+":"+storeId);if(!retailer.equals("walmart")||s==null||!pickup)throw new IllegalArgumentException("Unmatched store");}
   else if(!scope.equals("online")||!retailer.equals("home-depot")||!storeId.isEmpty()||pickup)throw new IllegalArgumentException("Invalid scope");
   URI u=new URI(url);String host=retailer.equals("walmart")?"www.walmart.com":"www.homedepot.com";
   if(!"https".equals(u.getScheme())||!host.equals(u.getHost())||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443)||!u.getPath().startsWith(retailer.equals("walmart")?"/ip/":"/p/"))throw new IllegalArgumentException("Unsafe retailer URL");
  }
  public boolean old(long now){return now<observedAt||now-observedAt>=OLD_MS;}
 }
 private TrialFeed(String zip,long generated,List<Source> sources,List<Store> stores,List<Offer> offers){this.zip=zip;generatedAt=generated;this.sources=Collections.unmodifiableList(sources);this.stores=Collections.unmodifiableList(stores);this.offers=Collections.unmodifiableList(offers);}
 public boolean covers(String requested){return zip.equals(requested);}
 public Store store(Offer o){for(Store s:stores)if(s.id.equals(o.storeId)&&s.retailer.equals(o.retailer))return s;return null;}
 public static TrialFeed parse(String body)throws IOException{
  try{
   if(body.length()>2000000)throw new IllegalArgumentException("Feed too large");JSONObject root=new JSONObject(body);
   if(integer(root,"schema",1,1)!=1)throw new IllegalArgumentException("Schema");String zip=ZipLocation.normalize(root.getString("zip"));
   long generated=integer(root,"generated_at",1577836800000L,System.currentTimeMillis()+300000L);
   JSONArray ss=root.getJSONArray("sources"),st=root.getJSONArray("stores"),os=root.getJSONArray("offers");
   if(ss.length()!=6||st.length()>15||os.length()>400)throw new IllegalArgumentException("Feed limits");
   List<Source> sources=new ArrayList<>();Set<String> brands=new HashSet<>();for(int i=0;i<ss.length();i++){Source s=new Source(ss.getJSONObject(i));if(!brands.add(s.retailer))throw new IllegalArgumentException("Duplicate source");sources.add(s);}
   Map<String,Store> map=new LinkedHashMap<>();for(int i=0;i<st.length();i++){Store s=new Store(st.getJSONObject(i));if(map.put(s.retailer+":"+s.id,s)!=null)throw new IllegalArgumentException("Duplicate store");}
   List<Offer> offers=new ArrayList<>();Set<String> ids=new HashSet<>();for(int i=0;i<os.length();i++){Offer o=new Offer(os.getJSONObject(i),generated,map);if(!ids.add(o.id))throw new IllegalArgumentException("Duplicate offer");offers.add(o);}
   return new TrialFeed(zip,generated,sources,new ArrayList<>(map.values()),offers);
  }catch(Exception e){throw new IOException("The published feed was invalid. Your previous results were kept.",e);}
 }
 private static String str(JSONObject j,String key,int max)throws JSONException{Object value=j.get(key);if(!(value instanceof String))throw new IllegalArgumentException("Invalid text");String s=((String)value).trim();if(s.isEmpty()||s.length()>max)throw new IllegalArgumentException("Invalid text");return s;}
 private static long integer(JSONObject j,String key,long min,long max)throws JSONException{Object n=j.get(key);if(!(n instanceof Number))throw new IllegalArgumentException("Invalid number");double d=((Number)n).doubleValue();long v=((Number)n).longValue();if(!Double.isFinite(d)||d!=v||v<min||v>max)throw new IllegalArgumentException("Invalid number");return v;}
}
