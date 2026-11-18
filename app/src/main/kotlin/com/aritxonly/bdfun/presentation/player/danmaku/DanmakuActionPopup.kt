package com.aritxonly.bdfun.ui

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import com.aritxonly.bdfun.core.domain.DanmakuReportReason

/** 浮层与举报对话框共享一次选择，切换到举报时不提前释放固定弹幕。 */
internal class DanmakuActionPopup(private val anchor: View, private val release: () -> Unit) {
    private var popup: PopupWindow? = null
    private var dialog: AlertDialog? = null
    private var reporting = false
    private var closed = false
    private var likeAction: TextView? = null

    fun showVote(liked: Boolean, count: Long) {
        likeAction?.apply {
            val action = if (liked) "取消点赞" else "点赞"
            text = if (count > 0) "$action $count" else action
            contentDescription = "$action，$count 个赞"
            isSelected = liked
            setTextColor(if (liked) 0xFFFF759B.toInt() else Color.WHITE)
        }
    }

    fun show(text: String, x: Int, y: Int, liked: Boolean, count: Long, like: () -> Unit,
        report: (DanmakuReportReason, String) -> Unit) {
        val context = anchor.context
        val padding = (12 * context.resources.displayMetrics.density).toInt()
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            background = GradientDrawable().apply { setColor(0xEE242424.toInt()); cornerRadius = padding.toFloat() }
            addView(LinearLayout(context).apply {
                likeAction = toolbarAction("点赞") { like() }.also { addView(it) }
                showVote(liked, count)
                addView(toolbarAction("复制") {
                    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("弹幕", text))
                    close()
                })
                addView(toolbarAction("举报") {
                    reporting = true
                    popup?.dismiss()
                    showReport(report)
                })
            })
        }
        content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val width = content.measuredWidth.coerceAtMost(context.resources.displayMetrics.widthPixels - padding * 2)
        popup = PopupWindow(content, width, LinearLayout.LayoutParams.WRAP_CONTENT, true).apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            setOnDismissListener { if (!reporting) close() }
            showAtLocation(anchor, Gravity.TOP or Gravity.LEFT,
                (x - width / 2).coerceIn(0, (context.resources.displayMetrics.widthPixels - width).coerceAtLeast(0)), y)
        }
    }

    private fun toolbarAction(label: String, action: () -> Unit) = TextView(anchor.context).apply {
        val density = resources.displayMetrics.density
        text = label
        gravity = Gravity.CENTER
        minWidth = (48 * density).toInt()
        minHeight = (36 * density).toInt()
        setTextColor(Color.WHITE)
        setOnClickListener { action() }
    }

    private fun showReport(report: (DanmakuReportReason, String) -> Unit) {
        val reasons = DanmakuReportReason.entries
        var selected = 0
        val input = EditText(anchor.context).apply { hint = "补充说明（选择其它时必填）" }
        dialog = AlertDialog.Builder(anchor.context).setTitle("举报弹幕")
            .setSingleChoiceItems(reasons.map { it.title }.toTypedArray(), 0) { _, which -> selected = which }
            .setView(input).setNegativeButton("取消", null).setPositiveButton("提交举报", null)
            .create().apply {
                setOnDismissListener { close() }
                show()
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val reason = reasons[selected]
                    val content = input.text.toString().trim()
                    if (reason == DanmakuReportReason.Other && content.isBlank()) input.error = "请填写举报说明"
                    else { report(reason, content); close() }
                }
            }
    }

    fun close() {
        if (closed) return
        closed = true
        popup?.dismiss()
        dialog?.dismiss()
        release()
    }
}
