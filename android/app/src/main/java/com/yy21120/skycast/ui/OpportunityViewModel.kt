package com.yy21120.skycast.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yy21120.skycast.data.CachedOpportunityRepository
import com.yy21120.skycast.data.HttpOpportunityDataSource
import com.yy21120.skycast.data.OpportunityNetworkException
import com.yy21120.skycast.data.OpportunityRepository
import com.yy21120.skycast.data.OpportunityResult
import com.yy21120.skycast.data.local.RoomOpportunityCacheStore
import com.yy21120.skycast.data.local.SkyCastDatabase
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

sealed interface OpportunityUiState {
    data object Loading : OpportunityUiState
    data class Success(val result: OpportunityResult) : OpportunityUiState
    data class Error(val message: String) : OpportunityUiState
}

class OpportunityViewModel(
    private val repository: OpportunityRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<OpportunityUiState>(OpportunityUiState.Loading)
    val uiState: StateFlow<OpportunityUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = OpportunityUiState.Loading
            _uiState.value = try {
                OpportunityUiState.Success(repository.getWuhanOpportunities())
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                OpportunityUiState.Error(message = userFacingErrorMessage(exception))
            }
        }
    }

    companion object {
        fun factory(
            applicationContext: Context,
            baseUrl: String,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(OpportunityViewModel::class.java))
                    val database = SkyCastDatabase.getInstance(applicationContext)
                    val repository = CachedOpportunityRepository(
                        remote = HttpOpportunityDataSource(baseUrl),
                        cache = RoomOpportunityCacheStore(database.opportunityCacheDao()),
                    )
                    return OpportunityViewModel(repository) as T
                }
            }
    }
}

internal fun userFacingErrorMessage(exception: Exception): String =
    when (exception) {
        is UnknownHostException,
        is ConnectException,
        -> "无法连接 SkyCast 服务，请检查网络或确认本地服务已启动。"

        is SocketTimeoutException -> "服务响应超时，请稍后重试。"
        is OpportunityNetworkException -> exception.message ?: "服务暂时不可用，请稍后重试。"
        is SerializationException -> "服务返回的数据暂时无法解析，请稍后重试。"
        else -> "暂时无法获取晚霞机会，请稍后重试。"
    }
