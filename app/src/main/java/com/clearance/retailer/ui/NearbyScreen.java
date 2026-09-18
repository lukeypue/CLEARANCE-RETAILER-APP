package com.clearance.retailer.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.clearance.retailer.data.*;
import com.clearance.retailer.domain.*;
import com.clearance.retailer.model.*;
import java.text.DateFormat;
import java.util.*;
import static com.clearance.retailer.ui.Ui.*;

/** Real mapped locations. No sample deal can enter this screen. */
public final class NearbyScreen {
    private final Activity activity;
    private final SharedPreferences prefs;
    private final NearbyRepository repository;
    private final SavedStores saved;
    private final Runnable redraw;
    private String retailer,category,search,zip;
    private int count,radius;
    private boolean showingSaved;

    public NearbyScreen(Activity activity,Runnable redraw){
        this.activity=activity;this.redraw=redraw;repository=NearbyRepository.get(activity);
        prefs=activity.getSharedPreferences("nearby-browsing",0);
        saved=new SavedStores(activity.getSharedPreferences("saved-stores",0));
        retailer=prefs.getString("retailer","");category=prefs.getString("category","");search=prefs.getString("search","");
        zip=prefs.getString("zip",repository.requestedZip);count=prefs.getInt("count",5);radius=prefs.getInt("radius",repository.requestedRadius);
        if(count!=3&&count!=5)count=5;if(radius!=50&&radius!=100)radius=50;
    }
    public void observe(){repository.observe(redraw);}
    public void close(){repository.remove(redraw);}
    private void persist(){prefs.edit().putString("zip",zip).putInt("count",count).putInt("radius",radius)
        .putString("retailer",retailer).putString("category",category).putString("search",search).apply();}

    public void render(LinearLayout content,String tab){
        boolean favorites=tab.equals("Saved");
        showingSaved=favorites;
        content.addView(text(activity,favorites?"Your saved stores.":tab.equals("Stores")?"Stores near you.":"Start close to home.",30,INK,true));gap(content,8);
        content.addView(text(activity,favorites?"Keep your usual stops together.":"Find the nearest listed locations for six retailers.",15,MUTED,false));gap(content,18);
        if(!favorites)locationForm(content);
        inventoryNotice(content);
        if(!tab.equals("Stores"))searchForm(content);
        retailers(content);gap(content,16);
        if(favorites){
            List<NearbyStore> stores=saved.all();int shown=0;
            for(NearbyStore store:stores)if(retailer.isEmpty()||retailer.equals(store.retailerId)){storeCard(content,store,null);shown++;}
            if(shown==0)empty(content,"No saved stores in this selection","Find stores by ZIP and tap Save store. Choose All retailers to see every saved location.");
        }else renderResults(content);
        gap(content,10);attribution(content);
        content.setFocusableInTouchMode(true);content.requestFocus();
    }
    private void locationForm(LinearLayout content){
        LinearLayout card=card(activity);card.addView(text(activity,"YOUR ZIP CODE",11,GREEN,true));gap(card,8);
        EditText input=field("Five-digit US ZIP",zip);input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});input.setEnabled(!repository.loading);
        card.addView(input);
        TextView validation=text(activity,"",13,AMBER,false);validation.setVisibility(View.GONE);
        validation.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);card.addView(validation);
        watch(input,value->{zip=value;persist();validation.setVisibility(View.GONE);});gap(card,10);
        LinearLayout options=row(activity);
        TextView number=button(activity,"Closest "+count,false,()->new AlertDialog.Builder(activity).setTitle("Stores per retailer")
            .setSingleChoiceItems(new String[]{"Closest 3","Closest 5"},count==3?0:1,(dialog,which)->{count=which==0?3:5;persist();dialog.dismiss();redraw.run();}).setNegativeButton("Cancel",null).show());
        options.addView(number,new LinearLayout.LayoutParams(0,-2,1));
        TextView distance=button(activity,radius+" miles",false,()->new AlertDialog.Builder(activity).setTitle("Search radius from ZIP center")
            .setSingleChoiceItems(new String[]{"50 miles","100 miles"},radius==50?0:1,(dialog,which)->{radius=which==0?50:100;persist();dialog.dismiss();redraw.run();}).setNegativeButton("Cancel",null).show());
        distance.setEnabled(!repository.loading);options.addView(distance,new LinearLayout.LayoutParams(0,-2,1));card.addView(options);gap(card,10);
        TextView find=button(activity,repository.loading?"Finding stores…":"Find stores",true,()->{
            hideKeyboard();try{zip=ZipLocation.normalize(input.getText().toString());persist();repository.search(zip,radius);}
            catch(IllegalArgumentException e){validation.setText(e.getMessage());validation.setVisibility(View.VISIBLE);input.requestFocus();}
        });find.setEnabled(!repository.loading);card.addView(find);gap(card,8);
        card.addView(text(activity,"Up to "+count+" locations per retailer, ordered by distance from the ZIP center.",12,MUTED,false));
        if(repository.loading){gap(card,10);ProgressBar progress=new ProgressBar(activity);card.addView(progress);}
        if(!repository.error.isEmpty()){
            gap(card,10);card.addView(text(activity,"Search for "+repository.requestedZip+": "+repository.error,14,AMBER,false));
        }
        addCard(content,card);
    }
    private void inventoryNotice(LinearLayout content){
        LinearLayout notice=column(activity);pad(notice,14,12);notice.setBackground(bg(activity,SAND,12,false));
        notice.addView(text(activity,"LIVE INVENTORY NOT CONNECTED",11,AMBER,true));gap(notice,5);
        notice.addView(text(activity,"Store locations are real map listings. In-store clearance prices and stock are not available in this app yet.",13,AMBER,false));addCard(content,notice);
    }
    private void searchForm(LinearLayout content){
        content.addView(text(activity,"Search on retailer websites",17,INK,true));gap(content,8);
        EditText input=field("Product to search on retailer site",search);watch(input,value->{search=value;persist();});content.addView(input);gap(content,8);
        content.addView(button(activity,category.isEmpty()?"All 36 categories  ›":category+"  ›",false,this::chooseCategory));gap(content,8);
        content.addView(text(activity,"Your product and category are used when you tap Search retailer website. Select the location again on that site to check its prices.",12,MUTED,false));gap(content,16);
    }
    private void chooseCategory(){
        hideKeyboard();LinearLayout body=column(activity);pad(body,18,10);EditText find=field("Find a category","");body.addView(find);
        List<String> all=new ArrayList<>();all.add("All categories");all.addAll(Categories.all());
        ArrayAdapter<String> adapter=new ArrayAdapter<>(activity,android.R.layout.simple_list_item_1,new ArrayList<>(all));
        ListView list=new ListView(activity);list.setAdapter(adapter);body.addView(list,new LinearLayout.LayoutParams(-1,dp(activity,300)));
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Browse 36 categories").setView(body).setNegativeButton("Cancel",null).create();
        watch(find,value->{adapter.clear();for(String c:all)if(c.toLowerCase(Locale.US).contains(value.toLowerCase(Locale.US)))adapter.add(c);adapter.notifyDataSetChanged();});
        list.setOnItemClickListener((parent,view,position,id)->{String selected=adapter.getItem(position);category="All categories".equals(selected)?"":selected;persist();dialog.dismiss();redraw.run();});
        dialog.show();
    }
    private void retailers(LinearLayout content){
        HorizontalScrollView scroller=new HorizontalScrollView(activity);scroller.setHorizontalScrollBarEnabled(false);LinearLayout row=row(activity);
        retailerPill(row,"All retailers","");for(Retailer r:Retailer.values())retailerPill(row,r.label,r.id);scroller.addView(row);content.addView(scroller);
    }
    private void retailerPill(LinearLayout row,String label,String id){
        TextView button=button(activity,label,id.equals(retailer),()->{hideKeyboard();retailer=id;persist();redraw.run();});
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.rightMargin=dp(activity,8);row.addView(button,p);
    }
    private void renderResults(LinearLayout content){
        StoreLookup.Snapshot result=repository.result;
        if(result==null){
            if(!repository.loading)empty(content,"Choose a ZIP to get started","Walmart, Target, Home Depot, Lowe’s, Tractor Supply and Walgreens. We show mapped locations, even when inventory is unavailable.");
            return;
        }
        content.addView(text(activity,result.location.label(),22,INK,true));gap(content,6);
        String time=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(result.fetchedAt));
        boolean stale=result.stale(System.currentTimeMillis());
        content.addView(text(activity,(stale?"Older saved locations":"Saved locations")+" · retrieved "+time,12,stale?AMBER:MUTED,false));gap(content,6);
        content.addView(text(activity,"Within "+result.radiusMiles+" miles · straight-line distance, not driving distance. Map coverage may be incomplete. Searches are cached for 24 hours.",12,MUTED,false));gap(content,16);
        List<NearbyStore> nearby=NearbyQuery.nearest(result.stores,result.location.latitude,result.location.longitude,count,result.radiusMiles);
        for(Retailer r:Retailer.values()){
            if(!retailer.isEmpty()&&!retailer.equals(r.id))continue;
            List<NearbyStore> chain=new ArrayList<>();for(NearbyStore store:nearby)if(store.retailerId.equals(r.id))chain.add(store);
            content.addView(text(activity,r.label+" · "+chain.size()+" nearby",19,INK,true));gap(content,8);
            if(chain.isEmpty())empty(content,"No mapped locations found","No "+r.label+" locations were returned within "+result.radiusMiles+" miles. This does not prove there are no stores. Try 100 miles or the retailer's locator.");
            else for(NearbyStore store:chain)storeCard(content,store,result.location);
            if(chain.size()<count){content.addView(button(activity,"Open "+r.label+" store locator",false,()->open(locator(r.id))),new LinearLayout.LayoutParams(-1,-2));gap(content,12);}
            gap(content,8);
        }
    }
    private void storeCard(LinearLayout content,NearbyStore store,ZipLocation location){
        LinearLayout card=card(activity);card.addView(text(activity,store.name,20,INK,true));gap(card,7);
        card.addView(text(activity,store.address.isEmpty()?"Street address not supplied by map source":store.address,14,MUTED,false));gap(card,8);
        if(location!=null){card.addView(pill(activity,String.format(Locale.US,"%.1f miles from ZIP center",NearbyQuery.miles(location.latitude,location.longitude,store.latitude,store.longitude)),GREEN,PALE));gap(card,12);}
        card.addView(text(activity,"Clearance & stock: unavailable",13,AMBER,true));gap(card,12);
        TextView save=button(activity,saved.contains(store.id)?"Saved store ✓":"Save store",false,()->{});
        save.setContentDescription("Save store "+store.id);save.setOnClickListener(v->{boolean added=saved.toggle(store);save.setText(added?"Saved store ✓":"Save store");Toast.makeText(activity,added?"Store saved":"Store removed from saved",Toast.LENGTH_SHORT).show();if(showingSaved)redraw.run();});
        card.addView(save);gap(card,8);
        card.addView(button(activity,"Directions",false,()->open("https://www.google.com/maps/dir/?api=1&destination="+store.latitude+","+store.longitude)));gap(card,8);
        card.addView(button(activity,"Search retailer website",true,()->{
            hideKeyboard();String terms=(search+" "+category+" clearance").trim();
            new AlertDialog.Builder(activity).setTitle("Check "+Retailer.byId(store.retailerId).label)
                .setMessage("Search for: "+terms+"\n\nChoose this store on the retailer's website:\n"+store.name+"\n"+(store.address.isEmpty()?"Use the map location to identify the store.":store.address)+"\n\nThe website may show online prices until you select the store. This app has not checked its inventory.")
                .setPositiveButton("Open website",(dialog,which)->open(searchUrl(store.retailerId,terms))).setNegativeButton("Cancel",null).show();
        }));addCard(content,card);
    }
    public void sources(LinearLayout content,Runnable toggleDemo,boolean demo){
        content.addView(text(activity,"Know your data.",32,INK,true));gap(content,14);
        LinearLayout info=card(activity);info.addView(text(activity,"Store discovery is connected",20,INK,true));gap(info,8);
        info.addView(text(activity,"ZIP centers: Zippopotam.us. Store locations: OpenStreetMap through Overpass. Community map records can be missing or out of date. Distances are straight-line estimates from a ZIP center. Store lookups are saved for 24 hours; older results are labeled.",14,MUTED,false));addCard(content,info);
        for(Retailer r:Retailer.values()){
            LinearLayout card=card(activity);card.addView(text(activity,r.label,19,INK,true));gap(card,8);
            card.addView(text(activity,"Inventory: not connected",14,AMBER,true));gap(card,6);
            card.addView(text(activity,"No verified in-store stock, clearance prices, complete product list, or clearance-start dates. A retailer-approved inventory feed is still required.",13,MUTED,false));addCard(content,card);
        }
        LinearLayout privacy=card(activity);privacy.addView(text(activity,"Your information",20,INK,true));gap(privacy,8);
        privacy.addView(text(activity,"The ZIP you search is sent to Zippopotam.us. Its approximate center is sent to Overpass. No GPS permission, account or analytics. Your searches and saved stores stay on this phone. Directions and retailer buttons open other apps or websites, whose privacy policies apply.\n\nThis is an independent test app, not affiliated with the retailers.",14,MUTED,false));addCard(content,privacy);
        LinearLayout sample=card(activity);sample.addView(text(activity,"Optional sample catalog",19,INK,true));gap(sample,8);
        sample.addView(text(activity,"Fictional prices and products for trying the original browsing design. Sample saved items are kept separately from real saved stores.",14,MUTED,false));gap(sample,12);
        sample.addView(button(activity,demo?"Return to real stores":"Try sample catalog",false,toggleDemo));addCard(content,sample);attribution(content);
    }
    private void attribution(LinearLayout content){
        content.addView(text(activity,"Store locations © OpenStreetMap contributors · ODbL. ZIP data: Zippopotam.us.",11,MUTED,false));gap(content,6);
        content.addView(button(activity,"Map data and attribution",false,()->open("https://www.openstreetmap.org/copyright")));
    }
    private void empty(LinearLayout parent,String title,String body){LinearLayout card=card(activity);card.addView(text(activity,title,19,INK,true));gap(card,8);card.addView(text(activity,body,14,MUTED,false));addCard(parent,card);}
    private EditText field(String hint,String value){
        EditText field=new EditText(activity);field.setSingleLine(true);field.setTextSize(15);field.setTextColor(INK);field.setHintTextColor(MUTED);
        field.setHint(hint);field.setContentDescription(hint);field.setText(value);field.setBackground(bg(activity,WHITE,12,true));pad(field,14,12);field.setMinHeight(dp(activity,52));return field;
    }
    private interface Change{void accept(String value);}
    private static void watch(EditText input,Change change){input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){change.accept(s.toString());}public void afterTextChanged(Editable s){}});}
    private void hideKeyboard(){View focus=activity.getCurrentFocus();if(focus!=null){((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0);focus.clearFocus();}}
    private void open(String url){try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(activity,"Install a browser or Maps app to open this link.",Toast.LENGTH_LONG).show();}}
    private static String searchUrl(String retailer,String terms){
        String query=Uri.encode(terms);
        switch(retailer){case "walmart":return "https://www.walmart.com/search?q="+query;
        case "target":return "https://www.target.com/s?searchTerm="+query;
        case "home-depot":return "https://www.homedepot.com/s/"+query;
        case "lowes":return "https://www.lowes.com/search?searchTerm="+query;
        case "tractor-supply":return "https://www.tractorsupply.com/tsc/search/"+query;
        default:return "https://www.walgreens.com/search/results.jsp?Ntt="+query;}
    }
    private static String locator(String id){switch(id){case "walmart":return "https://www.walmart.com/store-finder";
        case "target":return "https://www.target.com/store-locator/find-stores";
        case "home-depot":return "https://www.homedepot.com/l/store-locator";
        case "lowes":return "https://www.lowes.com/store/";
        case "tractor-supply":return "https://www.tractorsupply.com/tsc/store-locator";
        default:return "https://www.walgreens.com/storelocator/find.jsp";}}
}
