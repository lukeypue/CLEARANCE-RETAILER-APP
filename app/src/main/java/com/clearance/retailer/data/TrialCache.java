package com.clearance.retailer.data;
import android.content.SharedPreferences;
import java.io.IOException;
/** Validated disk cache; an unsuccessful or older response never replaces valid data. */
public final class TrialCache {
 public interface Transport {String get() throws IOException;}
 private final SharedPreferences prefs;private final Transport transport;
 private TrialFeed feed;
 public TrialCache(SharedPreferences prefs,String seed,Transport transport)throws IOException{
  this.prefs=prefs;this.transport=transport;feed=TrialFeed.parse(seed);
  try{TrialFeed saved=TrialFeed.parse(prefs.getString("body",""));if(saved.generatedAt>=feed.generatedAt)feed=saved;}catch(IOException ignored){}
 }
 public TrialFeed current(){return feed;}
 public synchronized TrialFeed refresh()throws IOException{
  String body=transport.get();TrialFeed next=TrialFeed.parse(body);
  if(next.generatedAt<feed.generatedAt)throw new IOException("The server returned an older feed. Your newer results were kept.");
  prefs.edit().putString("body",body).apply();feed=next;return next;
 }
}
