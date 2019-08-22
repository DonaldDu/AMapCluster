package com.amap.apis.cluster

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.Marker
import com.amap.apis.cluster.demo.RegionItem
import com.amap.extra.cluster.Cluster
import com.amap.extra.cluster.ClusterClickListener
import com.amap.extra.cluster.ClusterItem
import com.amap.extra.cluster.ClusterOverlay
import com.amap.extra.cluster.render.LessClusterRender

class MainActivity : Activity(), AMap.OnMapLoadedListener, ClusterClickListener {
    private var mMapView: MapView? = null
    private var mAMap: AMap? = null
    private var clusterOverlay: ClusterOverlay? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mMapView = findViewById<View>(R.id.map) as MapView
        mMapView!!.onCreate(savedInstanceState)
        init()
    }

    private fun init() {
        if (mAMap == null) {
            // 初始化地图
            mAMap = mMapView!!.map
            mAMap!!.setOnMapLoadedListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        mMapView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mMapView!!.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        clusterOverlay!!.onDestroy()
        mMapView!!.onDestroy()
    }


    override fun onMapLoaded() {
//        val render = CountClusterRender(this, R.drawable.defaultcluster, R.drawable.cluster_bg)
        val render = LessClusterRender(R.drawable.defaultcluster)
        val size = resources.getDimensionPixelSize(R.dimen.cluster_radius)
        clusterOverlay = ClusterOverlay(mAMap!!, size)
        Thread {
            val items = mutableListOf<ClusterItem>()
            for (i in 0..999) {
                val lat = 39.474923 + Math.random()
                val lon = 116.027116 + Math.random()

                val latLng = LatLng(lat, lon, false)
                items.add(RegionItem(latLng, "test$i"))
            }
            clusterOverlay!!.clusterItems = items
            clusterOverlay!!.setClusterRenderer(render)
            clusterOverlay!!.setOnClusterClickListener(this@MainActivity)
            clusterOverlay!!.assignClusters()
        }.start()
    }


    override fun onClick(marker: Marker, cluster: Cluster) {
        val builder = LatLngBounds.Builder()
        for (clusterItem in cluster.clusterItems) {
            builder.include(clusterItem.position)
        }
        val latLngBounds = builder.build()
        mAMap!!.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0))
    }
}
