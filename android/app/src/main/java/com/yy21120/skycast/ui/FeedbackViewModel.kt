package com.yy21120.skycast.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yy21120.skycast.data.FeedbackNetworkException
import com.yy21120.skycast.data.FeedbackRepository
import com.yy21120.skycast.data.HttpFeedbackRepository
import com.yy21120.skycast.data.SunsetFeedbackRequest
import com.yy21120.skycast.data.SunsetOutcome
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

sealed interface FeedbackSubmissionStatus {
    data object Idle : FeedbackSubmissionStatus
    data object Submitting : FeedbackSubmissionStatus
    data class Success(val duplicate: Boolean) : FeedbackSubmissionStatus
    data class Error(val message: String) : FeedbackSubmissionStatus
}

data class FeedbackUiState(
    val sceneId: String? = null,
    val clientFeedbackId: String = "",
    val formVisible: Boolean = false,
    val outcome: SunsetOutcome? = null,
    val shootingQuality: Int = 0,
    val notes: String = "",
    val submissionStatus: FeedbackSubmissionStatus = FeedbackSubmissionStatus.Idle,
    internal val pendingRequest: SunsetFeedbackRequest? = null,
) {
    val canSubmit: Boolean
        get() = outcome != null && shootingQuality in 1..5 &&
            submissionStatus !is FeedbackSubmissionStatus.Submitting
}

sealed interface FeedbackAction {
    data class SceneSelected(val sceneId: String) : FeedbackAction
    data object ShowForm : FeedbackAction
    data object HideForm : FeedbackAction
    data class OutcomeSelected(val outcome: SunsetOutcome) : FeedbackAction
    data class QualitySelected(val quality: Int) : FeedbackAction
    data class NotesChanged(val notes: String) : FeedbackAction
    data object Submit : FeedbackAction
}

class FeedbackViewModel(
    private val repository: FeedbackRepository,
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
    private val submittedAtProvider: () -> String = { OffsetDateTime.now().toString() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun onAction(action: FeedbackAction) {
        when (action) {
            is FeedbackAction.SceneSelected -> selectScene(action.sceneId)
            FeedbackAction.ShowForm -> showForm()
            FeedbackAction.HideForm -> hideForm()
            is FeedbackAction.OutcomeSelected -> updateDraft(outcome = action.outcome)
            is FeedbackAction.QualitySelected -> updateDraft(quality = action.quality)
            is FeedbackAction.NotesChanged -> updateDraft(notes = action.notes.take(MAX_NOTES_LENGTH))
            FeedbackAction.Submit -> submit()
        }
    }

    private fun selectScene(sceneId: String) {
        if (_uiState.value.sceneId == sceneId) return
        _uiState.value = freshState(sceneId)
    }

    private fun showForm() {
        if (_uiState.value.sceneId == null) return
        _uiState.value = _uiState.value.copy(formVisible = true)
    }

    private fun hideForm() {
        if (_uiState.value.submissionStatus is FeedbackSubmissionStatus.Submitting) return
        _uiState.value = _uiState.value.copy(formVisible = false)
    }

    private fun updateDraft(
        outcome: SunsetOutcome? = _uiState.value.outcome,
        quality: Int = _uiState.value.shootingQuality,
        notes: String = _uiState.value.notes,
    ) {
        val current = _uiState.value
        if (current.submissionStatus is FeedbackSubmissionStatus.Submitting) return
        if (outcome == current.outcome && quality == current.shootingQuality && notes == current.notes) {
            return
        }
        _uiState.value = current.copy(
            clientFeedbackId = idProvider(),
            outcome = outcome,
            shootingQuality = quality.coerceIn(0, 5),
            notes = notes,
            submissionStatus = FeedbackSubmissionStatus.Idle,
            pendingRequest = null,
        )
    }

    private fun submit() {
        val current = _uiState.value
        if (!current.canSubmit || current.sceneId == null || current.outcome == null) return
        val request = current.pendingRequest ?: SunsetFeedbackRequest(
            clientFeedbackId = current.clientFeedbackId,
            sceneId = current.sceneId,
            outcome = current.outcome,
            shootingQuality = current.shootingQuality,
            notes = current.notes.trim().ifEmpty { null },
            submittedAt = submittedAtProvider(),
        )
        _uiState.value = current.copy(
            submissionStatus = FeedbackSubmissionStatus.Submitting,
            pendingRequest = request,
        )

        viewModelScope.launch {
            try {
                val response = repository.submitSunsetFeedback(request)
                _uiState.value = _uiState.value.copy(
                    submissionStatus = FeedbackSubmissionStatus.Success(response.duplicate),
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    submissionStatus = FeedbackSubmissionStatus.Error(
                        feedbackErrorMessage(exception),
                    ),
                )
            }
        }
    }

    private fun freshState(sceneId: String) = FeedbackUiState(
        sceneId = sceneId,
        clientFeedbackId = idProvider(),
    )

    companion object {
        private const val MAX_NOTES_LENGTH = 200

        fun factory(baseUrl: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(FeedbackViewModel::class.java))
                    return FeedbackViewModel(HttpFeedbackRepository(baseUrl)) as T
                }
            }
    }
}

internal fun feedbackErrorMessage(exception: Exception): String =
    when (exception) {
        is UnknownHostException,
        is ConnectException,
        -> "无法连接反馈服务，请检查网络后重试。"

        is SocketTimeoutException -> "提交超时，内容已保留，请重新提交。"
        is FeedbackNetworkException -> exception.message ?: "反馈服务暂时不可用。"
        is SerializationException -> "反馈服务返回的数据暂时无法解析。"
        else -> "反馈提交失败，内容已保留，请重新提交。"
    }
