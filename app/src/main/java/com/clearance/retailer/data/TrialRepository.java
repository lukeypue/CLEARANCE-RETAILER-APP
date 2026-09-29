package com.clearance.retailer.data;
import android.content.Context;
import android.os.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
public final class TrialRepository {
 private static TrialRepository instance;
 public static synchronized TrialRepository get(Context c){if(instance==null)instance=new TrialRepository(c.getApplicationContext());return instance;}
 private static final String URL="https://raw.githubusercontent.com/lukeypue/CLEARANCE-RETAILER-APP/codex/android-foundation/feeds/trial.json";
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());private final Set<Runnable> listeners=new HashSet<>();private TrialCache cache;private long attempt;
 public TrialFeed feed;public boolean loading;public String message="Bundled trial observations. Check for a newer published feed when online.";
 private TrialRepository(Context c){try(InputStream in=c.getAssets().open("trial.json");ByteArrayOutputStream out=new ByteArrayOutputStream()){
  byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
  cache=new TrialCache(c.getSharedPreferences("trial-feed",0),new String(out.toByteArray(),StandardCharsets.UTF_8),()->StoreLookup.https(URL));feed=cache.current();
 }catch(IOException e){message="Trial feed unavailable. Store search still works.";}}
 public void observe(Runnable r){listeners.add(r);}public void remove(Runnable r){listeners.remove(r);}private void changed(){for(Runnable r:new ArrayList<>(listeners))r.run();}
 public void refresh(){
  if(loading||cache==null)return;long now=SystemClock.elapsedRealtime();if(attempt>0&&now-attempt<30000){message="Please wait 30 seconds before checking again.";changed();return;}attempt=now;loading=true;message="Checking the published feed…";changed();
  worker.execute(()->{TrialFeed found=null;String result;
   try{found=cache.refresh();result="Published feed checked. Each listing keeps its actual observation time.";}
   catch(IOException|RuntimeException e){result="Could not update the feed. Showing previous dated results; check your connection and try again.";}
   final TrialFeed f=found;final String m=result;main.post(()->{if(f!=null)feed=f;message=m;loading=false;changed();});
  });
 }
}
