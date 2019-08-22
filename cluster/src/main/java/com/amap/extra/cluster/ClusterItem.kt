package com.amap.extra.cluster

import com.amap.api.maps.model.LatLng

interface ClusterItem {
    /**
     * 返回聚合元素的地理位置
     */
    val position: LatLng

    var cluster: Cluster
}
