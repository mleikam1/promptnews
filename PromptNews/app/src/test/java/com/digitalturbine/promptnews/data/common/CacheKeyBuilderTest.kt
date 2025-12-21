package com.digitalturbine.promptnews.data.common

import org.junit.Assert.assertEquals
import org.junit.Test

class CacheKeyBuilderTest {

    @Test
    fun build_returnsStableKey() {
        val filters = mapOf(
            "Sort" to "Top",
            "Category" to "Tech"
        )

        val result = CacheKeyBuilder.build(
            prompt = "  Hello   World ",
            locale = "EN-US",
            geo = "US",
            filters = filters
        )

        assertEquals(
            "version=v1|prompt=hello world|locale=en-us|geo=us|filters=category=tech&sort=top",
            result
        )
    }
}
