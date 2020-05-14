package com.briot.balmerlawrie.implementor.ui.main

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.briot.balmerlawrie.implementor.repository.remote.RemoteRepository
import com.briot.balmerlawrie.implementor.repository.remote.SignInResponse
import retrofit2.HttpException
import java.net.SocketException
import java.net.SocketTimeoutException

class LoginDialogViewModel : ViewModel() {
    val signInResponse: LiveData<SignInResponse> = MutableLiveData<SignInResponse>()
    var errorMessage: String = ""

    val networkError: LiveData<Boolean> = MutableLiveData<Boolean>()


    fun loginUser(username: String, password: String) {
        (networkError as MutableLiveData<Boolean>).value = false
        RemoteRepository.singleInstance.loginUser(username, password, this::handleLoginResponse, this::handleLoginError)
    }

    private fun handleLoginResponse(signInResponse: SignInResponse) {

        // Check for Admin role
        Log.d(TAG, "---- Login success-->"+ signInResponse)
        // Need to check after login user is admin then call addItemToList function
        (this.signInResponse as MutableLiveData<SignInResponse>).value = signInResponse
    }

    private fun handleLoginError(error: Throwable) {
        Log.d(TAG, error.localizedMessage)

        if (error is HttpException) {
            if (error.code() >= 401) {
                var msg = error.response()?.errorBody()?.string()
                if (msg != null && msg.isNotEmpty()) {
                    errorMessage = msg
                } else {
                    errorMessage = error.message()
                }
            }
            (networkError as MutableLiveData<Boolean>).value = true
        } else if (error is SocketException || error is SocketTimeoutException) {
            (networkError as MutableLiveData<Boolean>).value = true
        } else {
//            (this.user as MutableLiveData<PopulatedUser>).value = invalidUser
        }
    }


}
