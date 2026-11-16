package com.aritxonly.bdfun.core.domain

import org.junit.Assert.*
import org.junit.Test

class PlaybackNetworkRecoveryPolicyTest {
    @Test fun transientNetworkFailuresRetryTwiceWithBackoff() {
        for (code in listOf("ERROR_CODE_IO_NETWORK_CONNECTION_FAILED", "ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT")) {
            assertEquals(1000L, playbackNetworkRetryDelay(code, 0))
            assertEquals(3000L, playbackNetworkRetryDelay(code, 1))
            assertNull(playbackNetworkRetryDelay(code, 2))
        }
    }
    @Test fun httpAndCodecFailuresKeepOriginalRecoveryPath() {
        for (code in listOf("ERROR_CODE_IO_BAD_HTTP_STATUS", "ERROR_CODE_DECODING_FAILED", "unknown")) {
            assertNull(playbackNetworkRetryDelay(code, 0))
        }
    }
}
