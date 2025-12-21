package com.digitalturbine.promptnews.data.common

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlCanonicalizerTest {

    @Test
    fun canonicalize_removesTrackingParamsAndFragment() {
        val url = "HTTPS://Example.COM/path?utm_source=ads&keep=1&gclid=abc#section"

        val result = UrlCanonicalizer.canonicalize(url)

        assertEquals("https://example.com/path?keep=1", result)
    }

    @Test
    fun canonicalize_dropsEmptyQueryAfterFiltering() {
        val url = "https://example.com/path?utm_campaign=test#frag"

        val result = UrlCanonicalizer.canonicalize(url)

        assertEquals("https://example.com/path", result)
    }
}
