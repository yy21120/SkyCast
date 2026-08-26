package com.yy21120.skycast.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yy21120.skycast.data.HttpOpportunityRepository
import com.yy21120.skycast.data.OpportunityRepository
import com.yy21120.skycast.data.OpportunitiesResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OpportunityUiState {
    data object Loading : OpportunityUiState
    data class Success(val response: OpportunitiesResponse) : OpportunityUiState
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
                OpportunityUiState.Error(
                    message = exception.message ?: "暂时无法获取晚霞机会，请稍后重试。",
                )
            }
        }
    }

    companion object {
        fun factory(baseUrl: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(OpportunityViewModel::class.java))
                    return OpportunityViewModel(HttpOpportunityRepository(baseUrl)) as T
                }
            }
    }
}
