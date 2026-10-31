package com.kmhmubin.kothagolp.foryou

import com.kmhmubin.kothagolp.ai.OpenRouterService
import org.junit.Assert.assertEquals
import org.junit.Test

class AiErrorCopyTest {
    @Test fun dailyLimit() = assertEquals(
        "You've used today's free AI picks. They'll be back tomorrow.",
        OpenRouterService.errorMessage(429, "Rate limit exceeded: free-models-per-day")
    )

    @Test fun busy() = assertEquals(
        "The free AI is busy right now. Try again in a minute.",
        OpenRouterService.errorMessage(429, "Provider returned error")
    )

    @Test fun badKey() = assertEquals(
        "That AI key didn't work. Check it in AI picks.",
        OpenRouterService.errorMessage(401, "No auth credentials found")
    )
}
