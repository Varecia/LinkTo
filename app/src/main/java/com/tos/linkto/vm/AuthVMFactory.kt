package com.tos.linkto.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tos.linkto.MainActivity

class AuthVMFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthVM::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthVM(
                MainActivity.instance.authRepo
            ) as T
        }
        throw IllegalArgumentException("Unknown VM class")
    }
}