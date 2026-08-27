package com.yy21120.skycast.ui

import com.yy21120.skycast.data.FeedbackRepository
import com.yy21120.skycast.data.SunsetFeedbackRecord
import com.yy21120.skycast.data.SunsetFeedbackRequest
import com.yy21120.skycast.data.SunsetFeedbackResponse
import com.yy21120.skycast.data.SunsetOutcome
import java.net.SocketTimeoutException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackViewModelTest {
    @Test
    fun `submits complete feedback and shows success`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingFeedbackRepository()
            val viewModel = feedbackViewModel(repository)

            fillValidFeedback(viewModel)
            viewModel.onAction(FeedbackAction.Submit)
            advanceUntilIdle()

            val request = repository.requests.single()
            assertEquals("wuhan-sunset-2026-08-27", request.sceneId)
            assertEquals(SunsetOutcome.VIVID, request.outcome)
            assertEquals(5, request.shootingQuality)
            assertEquals("东湖边明显染色", request.notes)
            assertEquals("2026-08-27T19:20:00+08:00", request.submittedAt)
            assertEquals(FeedbackSubmissionStatus.Success(false), viewModel.uiState.value.submissionStatus)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `failed submission keeps input and exact retry reuses request`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingFeedbackRepository(failFirst = true)
            val viewModel = feedbackViewModel(repository)
            fillValidFeedback(viewModel)

            viewModel.onAction(FeedbackAction.Submit)
            advanceUntilIdle()

            val failedState = viewModel.uiState.value
            assertEquals(SunsetOutcome.VIVID, failedState.outcome)
            assertEquals(5, failedState.shootingQuality)
            assertEquals("东湖边明显染色", failedState.notes)
            assertTrue(failedState.submissionStatus is FeedbackSubmissionStatus.Error)

            viewModel.onAction(FeedbackAction.Submit)
            advanceUntilIdle()

            assertEquals(2, repository.requests.size)
            assertEquals(repository.requests[0], repository.requests[1])
            assertEquals(FeedbackSubmissionStatus.Success(false), viewModel.uiState.value.submissionStatus)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `editing after failure creates a new idempotency key`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingFeedbackRepository(failFirst = true)
            val viewModel = feedbackViewModel(repository)
            fillValidFeedback(viewModel)

            viewModel.onAction(FeedbackAction.Submit)
            advanceUntilIdle()
            val failedRequest = repository.requests.single()

            viewModel.onAction(FeedbackAction.NotesChanged("修改后的现场记录"))
            viewModel.onAction(FeedbackAction.Submit)
            advanceUntilIdle()

            val editedRequest = repository.requests.last()
            assertNotEquals(failedRequest.clientFeedbackId, editedRequest.clientFeedbackId)
            assertEquals("修改后的现场记录", editedRequest.notes)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `limits notes and requires outcome and quality`() {
        val viewModel = feedbackViewModel(RecordingFeedbackRepository())
        viewModel.onAction(FeedbackAction.SceneSelected("wuhan-sunset-2026-08-27"))
        viewModel.onAction(FeedbackAction.ShowForm)

        assertTrue(!viewModel.uiState.value.canSubmit)
        viewModel.onAction(FeedbackAction.OutcomeSelected(SunsetOutcome.NOT_VISIBLE))
        assertTrue(!viewModel.uiState.value.canSubmit)
        viewModel.onAction(FeedbackAction.QualitySelected(1))
        viewModel.onAction(FeedbackAction.NotesChanged("x".repeat(250)))

        assertTrue(viewModel.uiState.value.canSubmit)
        assertEquals(200, viewModel.uiState.value.notes.length)
    }

    private fun feedbackViewModel(repository: FeedbackRepository) = FeedbackViewModel(
        repository = repository,
        idProvider = { UUID.randomUUID().toString() },
        submittedAtProvider = { "2026-08-27T19:20:00+08:00" },
    )

    private fun fillValidFeedback(viewModel: FeedbackViewModel) {
        viewModel.onAction(FeedbackAction.SceneSelected("wuhan-sunset-2026-08-27"))
        viewModel.onAction(FeedbackAction.ShowForm)
        viewModel.onAction(FeedbackAction.OutcomeSelected(SunsetOutcome.VIVID))
        viewModel.onAction(FeedbackAction.QualitySelected(5))
        viewModel.onAction(FeedbackAction.NotesChanged("东湖边明显染色"))
    }

    private class RecordingFeedbackRepository(
        private val failFirst: Boolean = false,
    ) : FeedbackRepository {
        val requests = mutableListOf<SunsetFeedbackRequest>()

        override suspend fun submitSunsetFeedback(
            request: SunsetFeedbackRequest,
        ): SunsetFeedbackResponse {
            requests += request
            if (failFirst && requests.size == 1) throw SocketTimeoutException("timeout")
            return SunsetFeedbackResponse(
                status = "accepted",
                duplicate = false,
                feedback = SunsetFeedbackRecord(
                    clientFeedbackId = request.clientFeedbackId,
                    sceneId = request.sceneId,
                    outcome = request.outcome,
                    shootingQuality = request.shootingQuality,
                    notes = request.notes,
                    submittedAt = request.submittedAt,
                    createdAt = "2026-08-27T11:20:01Z",
                ),
            )
        }
    }
}
