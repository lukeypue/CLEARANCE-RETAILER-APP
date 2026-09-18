package com.clearance.retailer.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Paint;
import android.widget.*;
import com.clearance.retailer.model.*;
import com.clearance.retailer.data.SavedDeals;
import static com.clearance.retailer.ui.Ui.*;

public final class DealDetails {
    private DealDetails() { }
    public static void show(Context c,Deal d,Store store,SavedDeals saved,Runnable onSaved) {
        LinearLayout body=column(c);pad(body,22,12);
        body.addView(pill(c,"SAMPLE ITEM · NOT LIVE INVENTORY",AMBER,SAND));gap(body,16);
        body.addView(text(c,d.title,25,INK,true));gap(body,6);
        body.addView(text(c,store.displayName(),14,MUTED,false));gap(body,18);
        body.addView(text(c,money(d.priceCents),36,GREEN,true));
        if(d.originalCents!=null){
            TextView old=text(c,"Was "+money(d.originalCents)+"   ·   "+d.discountPercent()+"% off",16,MUTED,false);
            body.addView(old);
        }
        gap(body,12);body.addView(pill(c,d.stock.label,d.stock==Deal.Stock.UNKNOWN?AMBER:GREEN,d.stock==Deal.Stock.UNKNOWN?SAND:PALE));
        gap(body,20);body.addView(text(c,"About this find",17,INK,true));gap(body,10);
        line(body,"Category",d.category);
        line(body,"First spotted",date(d.firstSeen));
        line(body,"Clearance started",d.clearanceSince==null?"Not provided":date(d.clearanceSince));
        line(body,"Sample checked date",date(d.checkedOn));
        gap(body,10);body.addView(text(c,"First spotted is when an offer enters our catalog. It is not necessarily the day the store put it on clearance.",13,MUTED,false));
        gap(body,22);body.addView(text(c,"Sample price history",17,INK,true));gap(body,8);
        if(d.history.isEmpty()) body.addView(text(c,"No price history available.",14,MUTED,false));
        for(Deal.PricePoint p:d.history)line(body,date(p.date),money(p.cents));
        gap(body,16);body.addView(text(c,"Fictional prices and stock for testing. Real prices, availability, and clearance dates have not been checked with this retailer.",13,AMBER,false));
        ScrollView scroll=new ScrollView(c);scroll.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(c).setView(scroll).setNegativeButton("Close",null)
            .setPositiveButton(saved.ids().contains(d.id)?"Remove saved item":"Save item",(which,id)->{
                boolean added=saved.toggle(d.id);
                Toast.makeText(c,added?"Saved to your finds":"Removed from saved",Toast.LENGTH_SHORT).show();onSaved.run();
            }).create();
        dialog.show();
    }
    private static void line(LinearLayout body,String label,String value){
        Context c=body.getContext();LinearLayout row=row(c);pad(row,0,7);
        row.addView(text(c,label,14,MUTED,false),new LinearLayout.LayoutParams(0,-2,1));
        TextView right=text(c,value,14,INK,true);right.setGravity(android.view.Gravity.END);
        row.addView(right,new LinearLayout.LayoutParams(0,-2,1));body.addView(row);
    }
}
