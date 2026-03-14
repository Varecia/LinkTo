package com.tos.linkto.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tos.linkto.data.model.User
import com.tos.linkto.data.repo.AuthRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthVM(
    private val authRepo: AuthRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _currentUser = MutableStateFlow(authRepo.getCurrentUser())
    val currentUser: StateFlow<User?> = _currentUser

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = authRepo.login(email, password)
            handleAuthResult(result)
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = authRepo.register(username, email, password)
            handleAuthResult(result)
        }
    }

    private fun handleAuthResult(result: Result<User>) {
        result.onSuccess { user ->
            _currentUser.value = user
            _uiState.value = AuthUiState(isSuccess = true, user = user)
        }.onFailure { exception ->
            _uiState.value = AuthUiState(error = exception.message ?: "认证失败")
        }
    }

    fun logout() {
        authRepo.logout()
        _currentUser.value = null
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val user: User? = null
)