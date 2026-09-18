package com.clearance.retailer;

import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;
import android.content.SharedPreferences;
import com.clearance.retailer.data.*;
import com.clearance.retailer.model.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class StoreDataTests {
    private static final String ZIP="{\"post code\":\"00501\",\"places\":[{\"place name\":\"Holtsville\",\"state abbreviation\":\"NY\",\"latitude\":\"40.81\",\"longitude\":\"-73.04\"}]}";
    private static final String SHOPS="{\"elements\":[{\"type\":\"way\",\"id\":7,\"center\":{\"lat\":40.82,\"lon\":-73.05},\"tags\":{\"name\":\"Walmart\",\"shop\":\"supermarket\",\"addr:housenumber\":\"100\",\"addr:street\":\"Main St\",\"addr:city\":\"Holtsville\"}},{\"type\":\"node\",\"id\":8,\"lat\":40.83,\"lon\":-73.04,\"tags\":{\"name\":\"Flowers on Main\",\"shop\":\"florist\"}}]}";

    @Test public void testZipAndMappedStoreParsing() throws Exception {
        ZipLocation zip=StoreData.zip(ZIP,"00501");assertEquals("00501",zip.zip);assertEquals("NY",zip.state);
        List<NearbyStore> stores=StoreData.stores(SHOPS);assertEquals(1,stores.size());assertEquals("osm-way-7",stores.get(0).id);
        assertEquals("100 Main St, Holtsville",stores.get(0).address);
        try{StoreData.zip(ZIP,"84043");fail("Accepted mismatched ZIP");}catch(IOException expected){}
    }
    @Test public void testIncompleteResponseDoesNotBecomeEmptyInventory() throws Exception {
        for(String bad:Arrays.asList("{}","<html>busy</html>","{\"elements\":[{}]}","{\"elements\":[null]}","{\"elements\":[{\"tags\":\"invalid\"}]}","{\"remark\":\"runtime error: timed out\",\"elements\":[]}")){
            try{StoreData.stores(bad);fail("Accepted incomplete response");}catch(IOException expected){}
        }
        assertTrue(StoreData.stores("{\"elements\":[]}").isEmpty());
    }
    @Test public void testCacheKeyIncludesZipAndRadiusAndRestores() throws Exception {
        SharedPreferences prefs=InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("test-nearby",0);prefs.edit().clear().commit();
        AtomicInteger calls=new AtomicInteger();
        StoreLookup lookup=new StoreLookup(prefs,url->{calls.incrementAndGet();return url.contains("zippopotam")?ZIP:SHOPS;});
        StoreLookup.Snapshot first=lookup.find("00501",50,100_000L);
        assertEquals(2,calls.get());assertEquals(1,first.stores.size());
        assertEquals("00501",first.location.zip);
        lookup.find("00501",50,101_000L);assertEquals(2,calls.get());
        assertNull(lookup.cached("84043",50));assertNull(lookup.cached("00501",100));
        StoreLookup restored=new StoreLookup(prefs,url->{throw new IOException("offline");});
        assertEquals("00501",restored.last().location.zip);
        try{restored.find("00501",50,100_000L+StoreLookup.CACHE_MS+1);fail("Expected offline error");}catch(IOException expected){}
        assertEquals(1,restored.cached("00501",50).stores.size());
        assertEquals(100_000L,restored.cached("00501",50).fetchedAt);
    }
    @Test public void testRateLimitDoesNotOverwriteSavedData() throws Exception {
        SharedPreferences prefs=InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("test-rate",0);prefs.edit().clear().commit();
        StoreLookup lookup=new StoreLookup(prefs,url->url.contains("zippopotam")?ZIP:SHOPS);
        lookup.find("00501",50,100_000L);
        try{lookup.find("00501",100,101_000L);fail("Expected cooldown");}catch(IOException expected){}
        assertEquals(50,lookup.last().radiusMiles);
    }
    @Test public void testSavedStoresSurviveRepositoryRecreation() {
        SharedPreferences prefs=InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("test-saved-stores",0);prefs.edit().clear().commit();
        SavedStores saved=new SavedStores(prefs);NearbyStore s=new NearbyStore("osm-node-1","walmart","Walmart","Main St",40,-111);
        assertTrue(saved.toggle(s));assertTrue(new SavedStores(prefs).contains(s.id));
        assertEquals("Main St",new SavedStores(prefs).all().get(0).address);
        assertFalse(saved.toggle(s));assertTrue(saved.all().isEmpty());
    }
}
