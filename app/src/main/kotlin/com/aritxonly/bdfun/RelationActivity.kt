package com.aritxonly.bdfun

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import com.aritxonly.bdfun.ui.*
import com.aritxonly.teak.common.chrome.*

class RelationActivity : BdFunActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val mid = intent.getLongExtra("mid", 0)
        if (mid <= 0) { finish(); return }
        val fans = intent.getBooleanExtra("fans", false)
        setContent { RelationScreenRoute(mid, fans, onBack = ::finish) }
    }

    companion object {
        fun open(context: Context, mid: Long, fans: Boolean) {
            if (mid > 0) context.startActivity(Intent(context, RelationActivity::class.java)
                .putExtra("mid", mid).putExtra("fans", fans))
        }
    }
}
