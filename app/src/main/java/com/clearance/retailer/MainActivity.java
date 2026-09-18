package com.clearance.retailer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.clearance.retailer.data.*;
import com.clearance.retailer.domain.DealQuery;
import com.clearance.retailer.domain.Categories;
import com.clearance.retailer.model.*;
import com.clearance.retailer.ui.DealDetails;
import com.clearance.retailer.ui.NearbyScreen;
import java.util.*;
import static com.clearance.retailer.ui.Ui.*;

public final class MainActivity extends Activity {
    private final DealQuery.Filter filter=new DealQuery.Filter();
    private final List<Deal> catalog=DemoCatalog.deals();
    private SavedDeals saved;
    private SharedPreferences prefs;
    private LinearLayout content,results;
    private TextView resultCount;
    private ScrollView scroll;
    private String tab="Discover";
    private NearbyScreen nearby;
    private boolean demoMode;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        saved=new SavedDeals(this);prefs=getSharedPreferences("browsing",MODE_PRIVATE);restoreFilters();
        demoMode=prefs.getBoolean("demoMode",false);nearby=new NearbyScreen(this,this::render);nearby.observe();
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        render();
    }
    @Override protected void onPause(){persist();super.onPause();}
    @Override protected void onDestroy(){nearby.close();super.onDestroy();}
    private void persist(){
        prefs.edit().putString("retailer",filter.retailerId).putString("store",filter.storeId)
            .putString("category",filter.category).putString("search",filter.search)
            .putString("sort",filter.sort.name()).putBoolean("sold",filter.showSoldOut).putString("tab",tab).putBoolean("demoMode",demoMode).apply();
    }
    private void restoreFilters(){
        filter.retailerId=prefs.getString("retailer","");filter.storeId=prefs.getString("store","");
        filter.category=prefs.getString("category","");filter.search=prefs.getString("search","");
        filter.showSoldOut=prefs.getBoolean("sold",false);tab=prefs.getString("tab","Discover");
        if(!Arrays.asList("Discover","Stores","Saved","Sources").contains(tab))tab="Discover";
        try{filter.sort=DealQuery.Sort.valueOf(prefs.getString("sort","NEWEST"));}catch(IllegalArgumentException ignored){filter.sort=DealQuery.Sort.NEWEST;}
        if(!filter.storeId.isEmpty()){try{DemoCatalog.store(filter.storeId);}catch(IllegalArgumentException ignored){filter.storeId="";}}
        if(!filter.retailerId.isEmpty()){try{Retailer.byId(filter.retailerId);}catch(IllegalArgumentException ignored){filter.retailerId="";}}
        if(!filter.category.isEmpty()&&!Categories.all().contains(filter.category))filter.category="";
    }
    private void render(){
        results=null;resultCount=null;
        LinearLayout root=column(this);root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;
        });
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        content=column(this);pad(content,20,20);scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=row(this);pad(nav,10,8);nav.setBackgroundColor(WHITE);
        for(String label:Arrays.asList("Discover","Stores","Saved","Sources")){
            TextView action=button(this,label,tab.equals(label),()->navigate(label));
            action.setTextSize(12);pad(action,3,12);
            action.setContentDescription(label+" tab");
            nav.addView(action,new LinearLayout.LayoutParams(0,-2,1));
        }
        root.addView(nav);setContentView(root);root.requestApplyInsets();
        LinearLayout brand=row(this);
        TextView mark=text(this,"C",22,WHITE,true);mark.setGravity(Gravity.CENTER);mark.setBackground(bg(this,GREEN,12,false));
        brand.addView(mark,new LinearLayout.LayoutParams(dp(this,42),dp(this,42)));
        LinearLayout name=column(this);pad(name,12,0);name.addView(text(this,"CLEARANCE",17,INK,true));name.addView(text(this,"Find more. Spend less.",11,MUTED,false));
        brand.addView(name,new LinearLayout.LayoutParams(0,-2,1));brand.addView(pill(this,"v0.2 TEST",GREEN,PALE));content.addView(brand);gap(content,22);
        if(tab.equals("Sources"))nearby.sources(content,this::toggleDemo,demoMode);
        else if(!demoMode)nearby.render(content,tab);
        else if(tab.equals("Stores"))renderStores();else renderBrowse();
    }
    private void navigate(String selected){
        hideKeyboard();
        if(demoMode&&selected.equals("Saved")&&!tab.equals("Saved"))clearFilters();
        tab=selected;persist();render();
    }
    private void toggleDemo(){hideKeyboard();demoMode=!demoMode;tab="Discover";persist();render();}
    private void sampleNotice(){
        LinearLayout notice=column(this);pad(notice,14,12);notice.setBackground(bg(this,SAND,12,false));
        notice.addView(text(this,"SAMPLE INVENTORY",11,AMBER,true));gap(notice,3);
        notice.addView(text(this,"Try the app with fictional finds. Live store data is not connected yet.",13,AMBER,false));content.addView(notice);gap(content,18);
        content.addView(button(this,"Return to real stores",false,this::toggleDemo));gap(content,12);
    }
    private void renderBrowse(){
        boolean savedTab=tab.equals("Saved");filter.savedOnly=savedTab;
        content.addView(text(this,savedTab?"Your saved finds.":"Good finds.\nBetter prices.",32,INK,true));gap(content,8);
        content.addView(text(this,savedTab?"Keep the things worth coming back for.":"Explore clearance, one store at a time.",15,MUTED,false));gap(content,20);sampleNotice();
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(15);search.setTextColor(INK);search.setHintTextColor(MUTED);
        search.setHint("Search products or categories");search.setContentDescription("Search products or categories");search.setBackground(bg(this,WHITE,14,true));pad(search,16,14);
        search.setMinHeight(dp(this,52));search.setText(filter.search);search.setSelectAllOnFocus(false);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        search.setOnEditorActionListener((v,action,event)->{hideKeyboard();return false;});
        content.addView(search);gap(content,14);
        horizontalRetailers();gap(content,12);
        String storeLabel=filter.storeId.isEmpty()?"Choose a specific store  ›":DemoCatalog.store(filter.storeId).displayName()+"  ›";
        TextView storeButton=button(this,storeLabel,false,this::chooseStore);storeButton.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);storeButton.setContentDescription("Choose a specific store");content.addView(storeButton);gap(content,16);
        horizontalCategories();gap(content,16);
        LinearLayout order=row(this);order.addView(text(this,"Sort by",13,MUTED,true));
        Spinner sorting=new Spinner(this);sorting.setContentDescription("Sort finds");
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,new String[]{"Newest finds","Biggest discount","Lowest price"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);sorting.setAdapter(adapter);sorting.setSelection(filter.sort.ordinal());
        order.addView(sorting,new LinearLayout.LayoutParams(0,dp(this,48),1));
        TextView reset=button(this,"Reset",false,()->{clearFilters();persist();render();});reset.setContentDescription("Reset filters");order.addView(reset);content.addView(order);
        Switch sold=new Switch(this);sold.setText("Include sold-out items");sold.setTextSize(14);sold.setTextColor(MUTED);sold.setMinHeight(dp(this,48));sold.setChecked(filter.showSoldOut);content.addView(sold);
        content.addView(text(this,"In stock and limited stock first; unknown stock follows.",11,MUTED,false));gap(content,20);
        resultCount=text(this,"",15,INK,true);content.addView(resultCount);gap(content,12);results=column(this);content.addView(results);renderResults();
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){filter.search=s.toString();renderResults();}public void afterTextChanged(Editable s){}});
        sold.setOnCheckedChangeListener((button,on)->{filter.showSoldOut=on;persist();renderResults();});
        sorting.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){filter.sort=DealQuery.Sort.values()[pos];persist();renderResults();}public void onNothingSelected(AdapterView<?> p){}});
        content.setFocusableInTouchMode(true);content.requestFocus();
    }
    private void horizontalRetailers(){
        HorizontalScrollView horizontal=new HorizontalScrollView(this);horizontal.setHorizontalScrollBarEnabled(false);
        LinearLayout row=row(this);addPill(row,"All retailers",filter.retailerId.isEmpty(),()->setRetailer(""));
        for(Retailer r:Retailer.values())addPill(row,r.label,filter.retailerId.equals(r.id),()->setRetailer(r.id));
        horizontal.addView(row);content.addView(horizontal);
    }
    private void horizontalCategories(){
        HorizontalScrollView horizontal=new HorizontalScrollView(this);horizontal.setHorizontalScrollBarEnabled(false);
        LinearLayout row=row(this);addPill(row,"All categories",filter.category.isEmpty(),()->setCategory(""));
        for(String category:Categories.all())addPill(row,category,filter.category.equals(category),()->setCategory(category));
        horizontal.addView(row);content.addView(horizontal);
    }
    private void addPill(LinearLayout row,String label,boolean active,Runnable click){
        TextView pill=button(this,label,active,click);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.rightMargin=dp(this,8);row.addView(pill,p);
    }
    private void setRetailer(String id){hideKeyboard();filter.retailerId=id;filter.storeId="";persist();render();}
    private void setCategory(String value){hideKeyboard();filter.category=value;persist();render();}
    private void clearFilters(){filter.retailerId="";filter.storeId="";filter.category="";filter.search="";filter.showSoldOut=false;filter.sort=DealQuery.Sort.NEWEST;}
    private void chooseStore(){
        hideKeyboard();List<Store> available=new ArrayList<>();
        for(Store s:DemoCatalog.stores())if(filter.retailerId.isEmpty()||filter.retailerId.equals(s.retailerId))available.add(s);
        String[] labels=new String[available.size()+1];labels[0]="All stores in this selection";
        for(int i=0;i<available.size();i++)labels[i+1]=available.get(i).displayName();
        new AlertDialog.Builder(this).setTitle("Choose a sample store").setItems(labels,(dialog,index)->{
            filter.storeId=index==0?"":available.get(index-1).id;persist();render();
        }).setNegativeButton("Cancel",null).show();
    }
    private void renderResults(){
        if(results==null)return;filter.savedOnly=tab.equals("Saved");Set<String> ids=saved.ids();
        List<Deal> found=DealQuery.apply(catalog,filter,ids);results.removeAllViews();
        resultCount.setText(found.size()+" sample "+(found.size()==1?"find":"finds")+(filter.savedOnly?" saved":""));
        if(found.isEmpty()){
            LinearLayout empty=card(this);empty.addView(text(this,filter.savedOnly&&ids.isEmpty()?"Save your first find":"No matches with these filters",22,INK,true));gap(empty,8);
            empty.addView(text(this,filter.savedOnly&&ids.isEmpty()?"Open any item and tap Save item. Your saved finds stay on this phone.":"Try another category, clear your search, or include sold-out items.",15,MUTED,false));addCard(results,empty);return;
        }
        for(Deal d:found)renderCard(d,ids.contains(d.id));
    }
    private void renderCard(Deal d,boolean isSaved){
        LinearLayout card=card(this);LinearLayout top=row(this);
        TextView icon=text(this,glyph(d.category),29,GREEN,true);icon.setGravity(Gravity.CENTER);icon.setBackground(bg(this,PALE,14,false));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);top.addView(icon,new LinearLayout.LayoutParams(dp(this,56),dp(this,56)));
        LinearLayout heading=column(this);pad(heading,12,0);heading.addView(text(this,Retailer.byId(d.retailerId).label.toUpperCase(Locale.US),10,MUTED,true));gap(heading,4);
        heading.addView(text(this,d.title,17,INK,true));top.addView(heading,new LinearLayout.LayoutParams(0,-2,1));card.addView(top);gap(card,14);
        LinearLayout price=row(this);price.addView(text(this,money(d.priceCents),29,GREEN,true));
        if(d.originalCents!=null){TextView was=text(this,money(d.originalCents),14,MUTED,false);was.setPaintFlags(was.getPaintFlags()|Paint.STRIKE_THRU_TEXT_FLAG);pad(was,10,0);price.addView(was);}
        Space stretch=new Space(this);price.addView(stretch,new LinearLayout.LayoutParams(0,1,1));
        if(d.discountPercent()>0)price.addView(pill(this,d.discountPercent()+"% OFF",GREEN,PALE));card.addView(price);gap(card,12);
        card.addView(text(this,DemoCatalog.store(d.storeId).name,12,MUTED,false));gap(card,6);
        card.addView(text(this,d.dateLabel()+" "+date(d.discoveryDate())+" · sample",12,MUTED,false));gap(card,12);
        LinearLayout footer=row(this);footer.addView(pill(this,d.stock.label,d.stock==Deal.Stock.UNKNOWN?AMBER:GREEN,d.stock==Deal.Stock.UNKNOWN?SAND:PALE));
        footer.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1));
        TextView open=button(this,isSaved?"Saved · View":"View find",false,()->DealDetails.show(this,d,DemoCatalog.store(d.storeId),saved,this::renderResults));
        open.setTextSize(12);open.setContentDescription("View "+d.title+" at "+DemoCatalog.store(d.storeId).displayName());footer.addView(open);card.addView(footer);addCard(results,card);
    }
    private void renderStores(){
        content.addView(text(this,"Pick your store.",32,INK,true));gap(content,8);content.addView(text(this,"Every location has its own finds.",15,MUTED,false));gap(content,20);sampleNotice();
        for(Store store:DemoCatalog.stores()){
            LinearLayout card=card(this);card.addView(text(this,Retailer.byId(store.retailerId).label,21,INK,true));gap(card,5);card.addView(text(this,store.name,14,MUTED,false));gap(card,12);
            DealQuery.Filter f=new DealQuery.Filter();f.storeId=store.id;int n=DealQuery.apply(catalog,f,Collections.emptySet()).size();
            TextView button=button(this,"Browse "+n+" sample finds",false,()->{
                clearFilters();filter.storeId=store.id;filter.retailerId=store.retailerId;tab="Discover";persist();render();
            });button.setContentDescription("Browse "+store.displayName());card.addView(button);addCard(content,card);
        }
        content.addView(text(this,"These sample locations are fictional. Return to real stores to search nearby locations by ZIP.",14,MUTED,false));
    }
    private void hideKeyboard(){View focus=getCurrentFocus();if(focus!=null){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0);focus.clearFocus();}}
}
