package com.amap.extra.cluster

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import com.amap.api.maps.AMap
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.animation.AlphaAnimation
import com.amap.api.maps.model.animation.Animation
import java.util.*


/**
 * Created by yiyi.qi on 16/10/10.
 * 整体设计采用了两个线程,一个线程用于计算组织聚合数据,一个线程负责处理Marker相关操作
 */
class ClusterOverlay(private val amap: AMap, private val clusterSize: Int) : AMap.OnCameraChangeListener, AMap.OnMarkerClickListener {
    private val clusters: MutableList<Cluster> = mutableListOf()
    private var mClusterClickListener: ClusterClickListener? = null
    private lateinit var clusterRender: ClusterRender
    private val markers = mutableListOf<Marker>()
    private var clusterDistance: Float = 0f
    private val markerHandlerThread = HandlerThread("addMarker")
    private val signClusterThread = HandlerThread("calculateCluster")
    private lateinit var markerhandler: Handler
    private lateinit var signClusterHandler: Handler
    private var isCanceled = false
    lateinit var clusterItems: MutableList<ClusterItem>
    private val addAnimation = AlphaAnimation(0f, 1f)

    init {
        updateClusterDistance(amap, clusterSize)
        amap.setOnCameraChangeListener(this)
        initThreadHandler()
        assignClusters()
    }

    private var zoom: Float = -1f
    /**
     * @return update or not
     * */
    private fun updateClusterDistance(amap: AMap, clusterSize: Int): Boolean {
        val newZoom = amap.cameraPosition.zoom
        return if (newZoom != zoom) {
            zoom = newZoom
            clusterDistance = amap.scalePerPixel * clusterSize
            true
        } else false
    }

    /**
     * 设置聚合点的点击事件
     */
    fun setOnClusterClickListener(clusterClickListener: ClusterClickListener) {
        mClusterClickListener = clusterClickListener
        amap.setOnMarkerClickListener(this)
    }

    fun setClusterRenderer(render: ClusterRender) {
        clusterRender = render
    }

    fun onDestroy() {
        isCanceled = true
        signClusterHandler.removeCallbacksAndMessages(null)
        markerhandler.removeCallbacksAndMessages(null)
        signClusterThread.quit()
        markerHandlerThread.quit()
        markers.forEach { it.remove() }
        markers.clear()
        clusterRender.onDestroy()
    }

    //初始化Handler
    private fun initThreadHandler() {
        markerHandlerThread.start()
        signClusterThread.start()
        markerhandler = MarkerHandler(markerHandlerThread.looper)
        signClusterHandler = SignClusterHandler(signClusterThread.looper)
    }

    override fun onCameraChange(arg0: CameraPosition) {}

    override fun onCameraChangeFinish(arg0: CameraPosition) {
        val update = updateClusterDistance(amap, clusterSize)
        if (update) assignClusters()
        else update()
    }

    //点击事件
    override fun onMarkerClick(marker: Marker): Boolean {
        if (mClusterClickListener == null) return true
        val cluster = marker.data.cluster
        mClusterClickListener!!.onClick(marker, cluster)
        return true
    }

    /**
     * 动态显示可见聚合元素至地图上
     * @param clusters 当前缩放比例下的所有聚合点
     */
    private fun addClusterToMap(clusters: List<Cluster>) {
        val visibleBounds = amap.projection.visibleRegion.latLngBounds
        val visibleClusters = clusters.filter { visibleBounds.contains(it.centerLatLng) }
        val handled: MutableList<Cluster> = mutableListOf()
        if (markers.isNotEmpty()) {
            val alphaAnimation = AlphaAnimation(1f, 0f)
            markers.iteratorOnEach {
                val m = it.next()
                val cluster = visibleClusters.find { c -> c.centerLatLng == m.position }
                if (cluster == null) {
                    it.remove()
                    m.setAnimation(alphaAnimation)
                    m.setAnimationListener(MyAnimationListener(m))
                    m.startAnimation()
                } else {
                    clusterRender.updateCluster(amap, cluster)
                    handled.add(cluster)
                }
            }
        }
        visibleClusters.toMutableList().apply {
            removeAll(handled)
            forEach {
                addSingleClusterToMap(it)
            }
        }
    }

    /**
     * 将单个聚合元素添加至地图显示
     */
    private fun addSingleClusterToMap(cluster: Cluster) {
        val marker = clusterRender.createMarker(amap, cluster)
        marker.setAnimation(addAnimation)
        if (mClusterClickListener != null) {
            val data = cluster.center
            data.cluster = cluster
            marker.data = data
        }
        marker.startAnimation()
        cluster.marker = marker
        markers.add(marker)
    }

    private var Marker.data: ClusterItem
        get() = this.`object` as ClusterItem
        set(value) {
            this.`object` = value
        }

    /**
     * 将当前缩放比例下的所有聚合点计算出来，滑动时动态显示可见点，这样可以保证滑动过程中点位置不变。
     * */
    private fun calculateClusters() {
        isCanceled = false
        clusters.clear()
        for (clusterItem in clusterItems) {
            if (isCanceled) return
            val latlng = clusterItem.position
            var cluster = getCluster(clusters, latlng)
            if (cluster != null) {
                cluster.addClusterItem(clusterItem)
            } else {
                cluster = Cluster(latlng)
                cluster.addClusterItem(clusterItem)
                clusters.add(cluster)
            }
        }
        if (!isCanceled) update()
    }

    private fun update() {
        val message = Message.obtain()
        message.what = ADD_CLUSTER_LIST
        message.obj = ArrayList(clusters)  //复制一份数据，规避同步
        markerhandler.sendMessage(message)
    }

    /**
     * 对点进行聚合
     */
    fun assignClusters() {
        if (!::clusterItems.isInitialized) return
        isCanceled = true
        signClusterHandler.removeMessages(CALCULATE_CLUSTER)
        signClusterHandler.sendEmptyMessage(CALCULATE_CLUSTER)
    }

    /**
     * 根据一个点获取是否可以依附的聚合点，没有则返回null
     */
    private fun getCluster(clusters: List<Cluster>, p: LatLng): Cluster? {
        if (amap.cameraPosition.zoom >= 19) return null
        return clusters.find { AMapUtils.calculateLineDistance(it.centerLatLng, p) < clusterDistance }
    }

    /**
     * 更新已加入地图聚合点的样式
     */
    private fun updateCluster(cluster: Cluster) {
        clusterRender.updateCluster(amap, cluster)
    }


    /**
     * marker渐变动画，动画结束后将Marker删除
     */
    private class MyAnimationListener(private val marker: Marker) : Animation.AnimationListener {

        override fun onAnimationStart() {}

        override fun onAnimationEnd() {
            marker.remove()
        }
    }

    /**
     * 处理market添加，更新等操作
     */
    private inner class MarkerHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(message: Message) {
            when (message.what) {
                ADD_CLUSTER_LIST -> {
                    @Suppress("UNCHECKED_CAST")
                    val clusters = message.obj as List<Cluster>
                    addClusterToMap(clusters)
                }
                ADD_SINGLE_CLUSTER -> {
                    val cluster = message.obj as Cluster
                    addSingleClusterToMap(cluster)
                }
                UPDATE_SINGLE_CLUSTER -> {
                    val updateCluster = message.obj as Cluster
                    updateCluster(updateCluster)
                }
            }
        }
    }

    private val ADD_CLUSTER_LIST = 0
    private val ADD_SINGLE_CLUSTER = 1
    private val UPDATE_SINGLE_CLUSTER = 2

    /**
     * 处理聚合点算法线程
     */
    private inner class SignClusterHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(message: Message) {
            when (message.what) {
                CALCULATE_CLUSTER -> calculateClusters()
            }
        }
    }

    private val CALCULATE_CLUSTER = 0
}

fun <E> MutableCollection<E>.iteratorOnEach(action: (MutableIterator<E>) -> Unit) {
    val it = iterator()
    while (it.hasNext()) {
        action(it)
    }
}