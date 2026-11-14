package com.aritxonly.bdfun.infra.bili

/** 扫码签发与 IM gRPC 必须使用同一客户端，TV 凭据不能搭配 HD 元数据。 */
internal object HdClientIdentity {
    const val mobiApp = "android_hd"
    const val build = 2001100
    const val userAgent = "Mozilla/5.0 BiliDroid/2.0.1 (bbcallen@gmail.com) os/android model/android_hd mobi_app/android_hd build/2001100 channel/master innerVer/2001100 osVer/15 network/2"

    fun qrParams(signer: AppRequestSigner, authCode: String? = null): Map<String, String> {
        val params = mutableMapOf("local_id" to "0")
        if (authCode == null) {
            params["platform"] = "android"
            params["mobi_app"] = mobiApp
        } else {
            params["auth_code"] = authCode
        }
        signer.sign(params, BILI_APP_KEY, BILI_APP_SEC, null)
        return params
    }
}
