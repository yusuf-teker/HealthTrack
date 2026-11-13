package com.yusufteker.healthtrack.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheckAppVersionUseCaseTest {

    private val useCase = CheckAppVersionUseCase()

    @Test
    fun forceUpdateRequiredWhenBelowMinVersion() {
        val config = AppVersionConfigDto(
            minSupportedVersion = "1.2.0",
            latestVersion = "1.3.0"
        )
        val result = useCase.evaluate("1.0.5", config)
        assertTrue(result is UpdateStatus.ForceUpdateRequired)
    }

    @Test
    fun optionalUpdateWhenBetweenMinAndLatest() {
        val config = AppVersionConfigDto(
            minSupportedVersion = "1.0.0",
            latestVersion = "1.3.0"
        )
        val result = useCase.evaluate("1.2.0", config)
        assertTrue(result is UpdateStatus.OptionalUpdateAvailable)
    }

    @Test
    fun upToDateWhenAtLatest() {
        val config = AppVersionConfigDto(
            minSupportedVersion = "1.0.0",
            latestVersion = "1.3.0"
        )
        val result = useCase.evaluate("1.3.0", config)
        assertEquals(UpdateStatus.UpToDate, result)
    }
}

