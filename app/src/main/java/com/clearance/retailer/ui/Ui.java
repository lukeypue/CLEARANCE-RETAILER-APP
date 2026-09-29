package com.clearance.retailer.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Ui {
    public static final int INK=Color.rgb(23,43,53), MUTED=Color.rgb(91,110,120), GREEN=Color.rgb(13,113,87),
        PALE=Color.rgb(230,244,237), BG=Color.rgb(245,247,249), LINE=Color.rgb(218,227,230), WHITE=Color.WHITE,
        AMBER=Color.rgb(130,77,9), SAND=Color.rgb(255,242,219);
    private Ui() { }
    public static int dp(Context c, float v) { return Math.round(v*c.getResources().getDisplayMetrics().density); }
    public static LinearLayout column(Context c) {
        LinearLayout l=new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l;
    }
    public static LinearLayout row(Context c) {
        LinearLayout l=new LinearLayout(c); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }
    public static TextView text(Context c, String value, float size, int color, boolean bold) {
        TextView v=new TextView(c); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        v.setIncludeFontPadding(false); v.setLineSpacing(dp(c,3),1); return v;
    }
    public static GradientDrawable bg(Context c, int color, int radius, boolean border) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c,radius));
        if(border)d.setStroke(dp(c,1),LINE); return d;
    }
    public static void pad(View v,int h,int vertical) { v.setPadding(dp(v.getContext(),h),dp(v.getContext(),vertical),dp(v.getContext(),h),dp(v.getContext(),vertical)); }
    public static void gap(LinearLayout parent,int height) { View v=new View(parent.getContext());parent.addView(v,new LinearLayout.LayoutParams(1,dp(parent.getContext(),height))); }
    public static TextView button(Context c,String label,boolean selected,Runnable action) {
        TextView v=text(c,label,14,selected?WHITE:INK,true); pad(v,16,12); v.setGravity(Gravity.CENTER);
        v.setMinHeight(dp(c,48)); v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x220D7157),bg(c,selected?GREEN:WHITE,14,!selected),null));
        v.setClickable(true);v.setFocusable(true);v.setOnClickListener(view->action.run());return v;
    }
    public static TextView pill(Context c,String label,int color,int background) {
        TextView v=text(c,label,12,color,true);pad(v,10,6);v.setBackground(bg(c,background,8,false));return v;
    }
    public static String money(int cents) { return NumberFormat.getCurrencyInstance(Locale.US).format(cents/100.0); }
    public static String date(LocalDate date) {return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy",Locale.US));}
    public static String glyph(String category) {
        switch(category){case "Home":return "⌂";case "Electronics":return "♫";case "Tools":return "⚒";
        case "Outdoor":return "♧";case "Toys":return "★";case "Clothing":return "♧";
        case "Pets":return "♥";default:return "✚";}
    }
    public static LinearLayout card(Context c) {LinearLayout l=column(c);pad(l,18,18);l.setBackground(bg(c,WHITE,20,true));return l;}
    public static void addCard(LinearLayout parent,View card) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(parent.getContext(),12);parent.addView(card,p);
    }
}
