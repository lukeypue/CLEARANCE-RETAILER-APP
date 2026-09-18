package com.clearance.retailer.model;

public final class NearbyStore {
    public final String id, retailerId, name, address;
    public final double latitude, longitude;
    public NearbyStore(String id,String retailerId,String name,String address,double lat,double lon) {
        if(id==null||id.trim().isEmpty()||name==null||name.trim().isEmpty())throw new IllegalArgumentException("Missing store identity");
        Retailer.byId(retailerId);ZipLocation.validateCoordinates(lat,lon);
        this.id=id;this.retailerId=retailerId;this.name=name;this.address=address==null?"":address;
        this.latitude=lat;this.longitude=lon;
    }
}
