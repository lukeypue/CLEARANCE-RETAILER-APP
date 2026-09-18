package com.clearance.retailer;

import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;
import com.clearance.retailer.data.*;
import com.clearance.retailer.domain.NearbyQuery;
import com.clearance.retailer.model.*;
import java.util.*;

/** One genuine HTTPS integration query per CI run. Never packages fixtures as real stores. */
public final class LiveLookupTests {
    @Test public void testPublicZipAndStoreEndpointsOnAndroid() throws Exception {
        StoreLookup lookup=new StoreLookup(InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("nearby-v2",0),StoreLookup::https);
        StoreLookup.Snapshot found=lookup.find("84043",50,System.currentTimeMillis());
        assertEquals("84043",found.location.zip);assertEquals("UT",found.location.state);
        List<NearbyStore> stores=NearbyQuery.nearest(found.stores,found.location.latitude,found.location.longitude,5,50);
        int walmart=0,target=0;
        for(NearbyStore s:stores){if(s.retailerId.equals("walmart"))walmart++;if(s.retailerId.equals("target"))target++;}
        assertEquals("Expected five actual Walmart map listings near the test ZIP",5,walmart);
        assertTrue("Expected at least three Target map listings",target>=3);
        assertTrue("No fictional stores in live results",stores.stream().allMatch(s->s.id.startsWith("osm-")));
    }
}
