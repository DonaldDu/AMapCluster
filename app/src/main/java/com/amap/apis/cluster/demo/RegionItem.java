package com.amap.apis.cluster.demo;

import android.support.annotation.NonNull;

import com.amap.api.maps.model.LatLng;
import com.amap.extra.cluster.Cluster;
import com.amap.extra.cluster.ClusterItem;

public class RegionItem implements ClusterItem {
    private LatLng mLatLng;
    private String mTitle;

    public RegionItem(LatLng latLng, String title) {
        mLatLng = latLng;
        mTitle = title;
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return mLatLng;
    }

    public String getTitle() {
        return mTitle;
    }

    private transient Cluster cluster;

    @NonNull
    @Override
    public Cluster getCluster() {
        return cluster;
    }

    @Override
    public void setCluster(@NonNull Cluster cluster) {
        this.cluster = cluster;
    }
}
