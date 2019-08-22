package com.amap.extra.cluster;

import com.amap.api.maps.model.Marker;

import java.util.List;

public interface ClusterClickListener {
    /**
     * 点击聚合点的回调处理函数
     *
     * @param marker 点击的聚合点
     */
    void onClick(Marker marker, Cluster cluster);
}
