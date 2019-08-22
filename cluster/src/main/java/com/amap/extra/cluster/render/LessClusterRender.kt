package com.amap.extra.cluster.render

import android.support.annotation.DrawableRes
import com.amap.api.maps.AMap
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.extra.cluster.Cluster
import com.amap.extra.cluster.ClusterRender

class LessClusterRender(@DrawableRes private val markerIcon: Int) : ClusterRender {
    private val icon = BitmapDescriptorFactory.fromResource(markerIcon)
    override fun createMarker(map: AMap, cluster: Cluster): Marker {
        val latlng = cluster.centerLatLng
        val markerOptions = MarkerOptions()
                .anchor(0.5f, 0.5f)
                .icon(icon)
                .position(latlng)
        return map.addMarker(markerOptions)
    }

    override fun onDestroy() {
        icon.recycle()
    }
}