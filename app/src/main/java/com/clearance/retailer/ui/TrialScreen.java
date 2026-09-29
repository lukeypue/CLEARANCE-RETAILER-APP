package com.clearance.retailer.ui;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.clearance.retailer.data.*;
import com.clearance.retailer.model.*;
import java.text.DateFormat;
import java.util.*;
import static com.clearance.retailer.ui.Ui.*;
/** Browsable real observations, kept separate from the fictional demo catalog. */
public final class TrialScreen {
 private final Activity activity;private final Runnable redraw;private final TrialRepository repository;private final SharedPreferences prefs;
 private String retailer,store,category,zip;private boolean priceSort;private int limit=24;
 public TrialScreen(Activity a,Runnable redraw){activity=a;this.redraw=redraw;repository=TrialRepository.get(a);prefs=a.getSharedPreferences("trial-browsing",0);retailer=prefs.getString("retailer","");store=prefs.getString("store","");category=prefs.getString("category","");zip=prefs.getString("zip","84414");priceSort=prefs.getBoolean("priceSort",false);}
 public void observe(){repository.observe(redraw);}public void close(){repository.remove(redraw);}
 private void changed(){prefs.edit().putString("retailer",retailer).putString("store",store).putString("category",category).putString("zip",zip).putBoolean("priceSort",priceSort).apply();limit=24;hideKeyboard();redraw.run();}
 public void render(LinearLayout content){
  content.addView(text(activity,"Browse the trial finds.",30,INK,true));gap(content,8);
  content.addView(text(activity,"Real retailer listings, ready to browse. No product search needed.",15,MUTED,false));gap(content,14);
  LinearLayout location=card(activity);location.addView(text(activity,"TRIAL ZIP",11,GREEN,true));gap(location,6);
  EditText input=new EditText(activity);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setText(zip);input.setContentDescription("Trial ZIP");input.setTextColor(INK);location.addView(input);
  TextView validation=text(activity,"",13,AMBER,false);location.addView(validation);
  location.addView(button(activity,"Use this ZIP",false,()->{try{zip=ZipLocation.normalize(input.getText().toString());changed();}catch(IllegalArgumentException e){validation.setText(e.getMessage());}}));addCard(content,location);
  TrialFeed f=repository.feed;
  if(f==null){notice(content,repository.message);return;}
  notice(content,"Dated trial feed · "+f.zip+"\nShelf prices, clearance markdowns and quantities are unverified. Check the exact variant and store before traveling.");
  content.addView(text(activity,repository.message,12,MUTED,false));gap(content,8);
  TextView refresh=button(activity,repository.loading?"Checking feed…":"Check for feed updates",true,repository::refresh);refresh.setEnabled(!repository.loading);content.addView(refresh);gap(content,6);
  content.addView(text(activity,"Checks shared published results; does not perform a new retailer inventory scan. Collection is currently manual.",12,MUTED,false));gap(content,18);
  if(!f.covers(zip)){notice(content,"Trial listings currently cover 84414 only. Use the Stores tab to find mapped locations for "+zip+".");return;}
  HorizontalScrollView scroller=new HorizontalScrollView(activity);scroller.setHorizontalScrollBarEnabled(false);LinearLayout brands=row(activity);brand(brands,"All retailers","");for(Retailer r:Retailer.values())brand(brands,r.label,r.id);scroller.addView(brands);content.addView(scroller);gap(content,12);
  String storeLabel="All trial stores + online";for(TrialFeed.Store s:f.stores)if(s.id.equals(store))storeLabel=s.name;
  content.addView(button(activity,storeLabel+"  ›",false,()->chooseStore(f)));gap(content,8);
  content.addView(button(activity,category.isEmpty()?"All listing categories  ›":category+"  ›",false,()->chooseCategory(f)));gap(content,8);
  content.addView(button(activity,priceSort?"Sort: lowest listed price":"Sort: recently checked",false,()->{priceSort=!priceSort;changed();}));gap(content,16);
  if(!retailer.isEmpty())for(TrialFeed.Source s:f.sources)if(s.retailer.equals(retailer))notice(content,s.status+"\n"+s.detail);
  List<TrialFeed.Offer> found=new ArrayList<>();for(TrialFeed.Offer o:f.offers)if((retailer.isEmpty()||retailer.equals(o.retailer))&&(store.isEmpty()||store.equals(o.storeId))&&(category.isEmpty()||category.equals(o.category)))found.add(o);
  found.sort(priceSort?Comparator.comparingInt((TrialFeed.Offer o)->o.price).thenComparing(o->o.id):Comparator.comparingLong((TrialFeed.Offer o)->o.observedAt).reversed().thenComparing(o->o.id));
  content.addView(text(activity,found.size()+" trial listings",20,INK,true));gap(content,6);
  content.addView(text(activity,"Categories are estimated from listing titles. Recently checked is not the date clearance began.",12,MUTED,false));gap(content,12);
  if(found.isEmpty())notice(content,"No verified listing feed in this selection yet. This does not mean the store has no clearance items.");
  for(int i=0;i<Math.min(limit,found.size());i++)offer(content,f,found.get(i));
  if(found.size()>limit)content.addView(button(activity,"Show more listings",false,()->{limit+=24;redraw.run();}));
  gap(content,16);content.addView(text(activity,"Six-retailer trial status",20,INK,true));gap(content,10);statuses(content);
 }
 private void brand(LinearLayout parent,String label,String id){TextView b=button(activity,label,retailer.equals(id),()->{retailer=id;store="";category="";changed();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.rightMargin=dp(activity,8);parent.addView(b,p);}
 private void chooseStore(TrialFeed f){List<String> labels=new ArrayList<>(),ids=new ArrayList<>();labels.add("All trial stores + online");ids.add("");for(TrialFeed.Store s:f.stores){labels.add(s.name+String.format(Locale.US," · %.1f miles",s.distance));ids.add(s.id);}new AlertDialog.Builder(activity).setTitle("Store pickup listings near 84414").setItems(labels.toArray(new String[0]),(d,i)->{store=ids.get(i);retailer=store.isEmpty()?"":"walmart";changed();}).setNegativeButton("Cancel",null).show();}
 private void chooseCategory(TrialFeed f){TreeSet<String> values=new TreeSet<>();for(TrialFeed.Offer o:f.offers)if(retailer.isEmpty()||retailer.equals(o.retailer))values.add(o.category);List<String> labels=new ArrayList<>();labels.add("All listing categories");labels.addAll(values);new AlertDialog.Builder(activity).setTitle("Listing categories").setItems(labels.toArray(new String[0]),(d,i)->{category=i==0?"":labels.get(i);changed();}).setNegativeButton("Cancel",null).show();}
 private void offer(LinearLayout content,TrialFeed f,TrialFeed.Offer o){
  LinearLayout card=card(activity);boolean old=o.old(System.currentTimeMillis());
  card.addView(pill(activity,o.scope.equals("online")?"WEBSITE DEAL · NOT LOCAL STOCK":"STORE PICKUP LISTING",GREEN,PALE));gap(card,10);
  card.addView(text(activity,o.title,18,INK,true));gap(card,8);card.addView(text(activity,money(o.price)+" listed",24,GREEN,true));gap(card,7);
  TrialFeed.Store s=f.store(o);card.addView(text(activity,s==null?"Home Depot website · local price unverified":s.name+"\n"+s.address,13,MUTED,false));gap(card,7);
  String when=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(o.observedAt));card.addView(text(activity,(old?"Older observation · ":"Checked · ")+when,12,old?AMBER:MUTED,false));gap(card,7);
  card.addView(text(activity,o.scope.equals("online")?"Website promotion; no store availability confirmed.":"Pickup was offered when checked. Shelf price, markdown and quantity are unverified.",12,AMBER,false));gap(card,10);
  TextView open=button(activity,"Check at "+Retailer.byId(o.retailer).label,false,()->new AlertDialog.Builder(activity).setTitle("Check the current retailer listing")
   .setMessage((s==null?"This is a website deal, not verified local inventory.":"Select this location on Walmart:\n"+s.name+"\n"+s.address)+"\n\nConfirm the size/color and current price. Availability may have changed.")
   .setPositiveButton("Open retailer",(d,w)->open(o.url)).setNegativeButton("Cancel",null).show());open.setContentDescription("Check listing "+o.id);card.addView(open);addCard(content,card);
 }
 public void statuses(LinearLayout content){TrialFeed f=repository.feed;if(f==null){notice(content,"Trial feed unavailable.");return;}for(TrialFeed.Source s:f.sources){LinearLayout card=card(activity);card.addView(text(activity,Retailer.byId(s.retailer).label,18,INK,true));gap(card,5);card.addView(text(activity,s.status,14,GREEN,true));gap(card,5);card.addView(text(activity,s.detail,13,MUTED,false));addCard(content,card);}}
 private void notice(LinearLayout parent,String value){LinearLayout c=column(activity);pad(c,14,12);c.setBackground(bg(activity,SAND,12,false));c.addView(text(activity,value,13,AMBER,false));addCard(parent,c);}
 private void hideKeyboard(){View v=activity.getCurrentFocus();if(v!=null)((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);}
 private void open(String url){try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(activity,"Install a browser to open this listing.",Toast.LENGTH_SHORT).show();}}
}
