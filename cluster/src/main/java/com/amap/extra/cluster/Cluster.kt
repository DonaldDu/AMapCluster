package com.amap.extra.cluster

import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker

class Cluster(val centerLatLng: LatLng) {
    val clusterItems: MutableList<ClusterItem> = mutableListOf()
    var marker: Marker? = null

    val clusterCount: Int
        get() = clusterItems.size

    val center: ClusterItem
        get() = clusterItems.first()

    internal fun addClusterItem(clusterItem: ClusterItem) {
        clusterItems.add(clusterItem)
    }
}