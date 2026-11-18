package com.aritxonly.bdfun.core.domain

object TabClickPolicy {
    const val DEFAULT_REFRESH = false
    fun shouldRefresh(alwaysRefresh: Boolean, atTop: Boolean): Boolean = alwaysRefresh || atTop
}
