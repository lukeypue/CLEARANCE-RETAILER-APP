package com.clearance.retailer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.clearance.retailer.data.TrialFeed;
import com.clearance.retailer.data.TrialCache;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class TrialFeedTests {
 private String seed() throws Exception {try(java.io.InputStream in=InstrumentationRegistry.getInstrumentation().getTargetContext().getAssets().open("trial.json")){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return new String(out.toByteArray(),StandardCharsets.UTF_8);}}
 private void rejects(JSONObject j) throws Exception {try{TrialFeed.parse(j.toString());fail("Invalid feed accepted");}catch(java.io.IOException expected){}}
 @Test public void failedRefreshPreservesValidCache() throws Exception {
  android.content.SharedPreferences prefs=InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("cache-test",0);prefs.edit().clear().commit();
  String original=seed();TrialCache cache=new TrialCache(prefs,original,()->"<html>Challenge Validation</html>");
  long stamp=cache.current().generatedAt;int count=cache.current().offers.size();
  try{cache.refresh();fail("Invalid response accepted");}catch(java.io.IOException expected){}
  assertEquals(stamp,cache.current().generatedAt);assertEquals(count,cache.current().offers.size());assertFalse(prefs.contains("body"));
 }
 @Test public void bundledFeedAndCoverage() throws Exception {TrialFeed f=TrialFeed.parse(seed());assertEquals("84414",f.zip);assertEquals(6,f.sources.size());assertTrue(f.offers.size()>0);assertFalse(f.covers("84043"));assertTrue(f.covers("84414"));}
 @Test public void rejectsUnsafeLinkAndUnknownStore() throws Exception {JSONObject j=new JSONObject(seed());j.getJSONArray("offers").getJSONObject(0).put("url","https://evil.example/ip/123");rejects(j);j=new JSONObject(seed());j.getJSONArray("offers").getJSONObject(0).put("store_id","999999");rejects(j);}
 @Test public void rejectsSchemaAndFutureObservation() throws Exception {JSONObject j=new JSONObject(seed());j.put("schema",2);rejects(j);j=new JSONObject(seed());j.getJSONArray("offers").getJSONObject(0).put("observed_at",System.currentTimeMillis()+86400000L);rejects(j);}
 @Test public void rejectsOnlinePickupAndBadPrice() throws Exception {JSONObject j=new JSONObject(seed());j.getJSONArray("offers").getJSONObject(0).put("scope","online");rejects(j);j=new JSONObject(seed());j.getJSONArray("offers").getJSONObject(0).put("price_cents",0);rejects(j);}
}
