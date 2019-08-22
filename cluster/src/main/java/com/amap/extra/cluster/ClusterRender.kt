package com.amap.extra.cluster

import com.amap.api.maps.AMap
import com.amap.api.maps.model.Marker

interface ClusterRender {
    /**
     * 根据聚合点的元素数目返回渲染背景样式
     */
    fun createMarker(map: AMap, cluster: Cluster): Marker

    fun updateCluster(map: AMap, cluster: Cluster) {}

    fun onDestroy()
}
