package com.amap.extra.cluster.render

import android.content.Context
import android.graphics.Color
import android.support.annotation.DrawableRes
import android.util.LruCache
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import com.amap.api.maps.AMap
import com.amap.api.maps.model.BitmapDescriptor
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.extra.cluster.Cluster
import com.amap.extra.cluster.ClusterRender

class CountClusterRender(private val context: Context, @DrawableRes private val markerIcon: Int, @DrawableRes private val clusterBg: Int) : ClusterRender {
    private val mLruCache: LruCache<Int, BitmapDescriptor>

    init {
        //默认最多会缓存80张图片作为聚合显示元素图片,根据自己显示需求和app使用内存情况,可以修改数量
        mLruCache = object : LruCache<Int, BitmapDescriptor>(80) {
            override fun entryRemoved(evicted: Boolean, key: Int, oldValue: BitmapDescriptor, newValue: BitmapDescriptor?) {
                oldValue.recycle()
            }
        }
    }

    override fun createMarker(map: AMap, cluster: Cluster): Marker {
        val latlng = cluster.centerLatLng
        val markerOptions = MarkerOptions()
                .anchor(0.5f, 0.5f)
                .icon(getBitmapDes(cluster.clusterCount))
                .position(latlng)
        return map.addMarker(markerOptions)
    }

    override fun updateCluster(map: AMap, cluster: Cluster) {
        cluster.marker!!.setIcon(getBitmapDes(cluster.clusterCount))
    }

    /**
     * 获取每个聚合点的绘制样式
     */
    private fun getBitmapDes(num: Int): BitmapDescriptor {
        var bitmapDescriptor = mLruCache.get(num)
        if (bitmapDescriptor == null) {
            val textView = TextView(context)
            if (num > 1) {
                textView.text = num.toString()
                textView.setBackgroundResource(clusterBg)
            } else {
                textView.setBackgroundResource(markerIcon)
            }
            textView.gravity = Gravity.CENTER
            textView.setTextColor(Color.BLACK)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)

            bitmapDescriptor = BitmapDescriptorFactory.fromView(textView)
            mLruCache.put(num, bitmapDescriptor)
        }
        return bitmapDescriptor
    }

    override fun onDestroy() {
        mLruCache.evictAll()
    }
}