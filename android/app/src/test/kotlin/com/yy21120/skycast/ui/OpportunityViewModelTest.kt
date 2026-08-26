package com.yy21120.skycast.ui

import com.yy21120.skycast.data.OpportunityDataSource
import com.yy21120.skycast.data.OpportunityRepository
import com.yy21120.skycast.data.OpportunityResult
import com.yy21120.skycast.data.testOpportunityResponse
import java.net.ConnectException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OpportunityViewModelTest {
    @Test
    fun `shows actionable message when service cannot be reached`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = object : OpportunityRepository {
                override suspend fun getWuhanOpportunities(days: Int): OpportunityResult {
                    throw ConnectException("Connection refused")
                }
            }

            val viewModel = OpportunityViewModel(repository)
            advanceUntilIdle()

            assertEquals(
                OpportunityUiState.Error(
                    "无法连接 SkyCast 服务，请检查网络或确认本地服务已启动。",
                ),
                viewModel.uiState.value,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `keeps cache metadata in success state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val expected = OpportunityResult(
                response = testOpportunityResponse(),
                source = OpportunityDataSource.CACHE,
                cachedAtEpochMillis = 1_000L,
            )
            val repository = object : OpportunityRepository {
                override suspend fun getWuhanOpportunities(days: Int): OpportunityResult = expected
            }

            val viewModel = OpportunityViewModel(repository)
            advanceUntilIdle()

            assertEquals(OpportunityUiState.Success(expected), viewModel.uiState.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
