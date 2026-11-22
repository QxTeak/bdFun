package com.aritxonly.bdfun.infra.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.aritxonly.bdfun.core.domain.CellularNetworkSource
import com.aritxonly.bdfun.core.domain.NetworkBandwidthSource

/** 只读取系统提示值，不执行测速或修改网络；无网络/读取失败保持旧值回退。 */
class AndroidNetworkBandwidthSource(context: Context?) : NetworkBandwidthSource, CellularNetworkSource {
    private val context = context?.applicationContext
    override fun downstreamKbps(): Int = runCatching {
        val cm = context?.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        caps?.linkDownstreamBandwidthKbps ?: 0
    }.getOrDefault(0)

    override fun isCellular(): Boolean = runCatching {
        val cm = context?.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }.getOrDefault(false)
}
