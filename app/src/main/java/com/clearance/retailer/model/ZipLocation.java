package com.clearance.retailer.model;

public final class ZipLocation {
    public final String zip, city, state;
    public final double latitude, longitude;
    public ZipLocation(String zip, String city, String state, double latitude, double longitude) {
        this.zip=normalize(zip); validateCoordinates(latitude,longitude);
        this.city=city;this.state=state;this.latitude=latitude;this.longitude=longitude;
    }
    public static String normalize(String value) {
        String zip=value==null?"":value.trim();
        if(!zip.matches("[0-9]{5}"))throw new IllegalArgumentException("Enter a five-digit US ZIP code.");
        return zip;
    }
    public static void validateCoordinates(double lat,double lon) {
        if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)
            throw new IllegalArgumentException("Invalid coordinates");
    }
    public String label(){return city+", "+state+" "+zip;}
}
