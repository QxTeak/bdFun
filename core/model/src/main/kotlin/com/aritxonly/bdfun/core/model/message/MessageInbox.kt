package com.aritxonly.bdfun.core.model.message

/** cursor 与 identity 是网络适配器的不透明快照，展示层不解析。 */
data class MessageInboxPage(
    val sessions: List<MessageSession>, val cursor: String?,
    val menu: List<MessageMenuItem> = emptyList(), val outside: List<MessageMenuItem> = emptyList(),
)
data class MessageMenuItem(val type: Int, val title: String, val url: String = "", val icon: String = "")
data class MessageLikePerson(val mid: Long, val name: String, val avatar: String, val time: Long)
data class MessageLikeDetail(val title: String, val people: List<MessageLikePerson>)
data class ImSettingChoice(val value: Int, val text: String, val selected: Boolean)
data class ImSetting(
    val key: Int, val title: String, val subtitle: String = "", val enabled: Boolean? = null,
    val choices: List<ImSettingChoice> = emptyList(), val pageType: Int? = null,
    val url: String = "", val summary: String = "", val token: String = "",
)
data class ImSettingsPage(val title: String, val settings: List<ImSetting>)
data class MessageKeywordList(val keywords: List<String>, val listLimit: Int, val charLimit: Int, val limitText: String)
data class MessageContactsPage(val people: List<MessageLikePerson>, val more: Boolean)
data class ChatPreferences(val name: String, val avatar: String, val pinned: Boolean?, val muted: Boolean,
    val push: Boolean?, val blocked: Boolean, val pinError: String? = null)
