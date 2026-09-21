package com.clearance.retailer.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.clearance.retailer.model.ZipLocation;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** One operation survives Activity recreation. Observers are removed when an Activity is destroyed. */
public final class NearbyRepository {
    private static NearbyRepository instance;
    public static synchronized NearbyRepository get(Context context){
        if(instance==null)instance=new NearbyRepository(new StoreLookup(context.getApplicationContext().getSharedPreferences("nearby-v2",0),StoreLookup::https));
        return instance;
    }
    private final StoreLookup lookup;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Set<Runnable> listeners=new HashSet<>();
    public StoreLookup.Snapshot result;
    public boolean loading;
    public String requestedZip="",error="";
    public int requestedRadius=50;
    private NearbyRepository(StoreLookup lookup){
        this.lookup=lookup;result=lookup.last();if(result!=null){requestedZip=result.location.zip;requestedRadius=result.radiusMiles;}
    }
    public void observe(Runnable listener){listeners.add(listener);}
    public void remove(Runnable listener){listeners.remove(listener);}
    private void changed(){for(Runnable listener:new ArrayList<>(listeners))listener.run();}
    public void search(String value,int radius){
        if(loading)return;
        try{requestedZip=ZipLocation.normalize(value);}catch(IllegalArgumentException e){error=e.getMessage();changed();return;}
        requestedRadius=radius;result=lookup.cached(requestedZip,radius);error="";loading=true;changed();
        final String zip=requestedZip;
        worker.execute(()->{
            StoreLookup.Snapshot found=null;String failure="";
            try{found=lookup.find(zip,radius,System.currentTimeMillis());}
            catch(UnknownHostException|ConnectException|SocketTimeoutException e){failure="Could not reach the store service. Check your connection and try again.";}
            catch(IOException e){failure=e.getMessage()==null?"Could not reach the store service. Check your connection and try again.":e.getMessage();}
            catch(RuntimeException e){failure="Could not read the store results. Please try later.";}
            final StoreLookup.Snapshot complete=found;final String message=failure;
            main.post(()->{if(complete!=null)result=complete;error=message;loading=false;changed();});
        });
    }
}
