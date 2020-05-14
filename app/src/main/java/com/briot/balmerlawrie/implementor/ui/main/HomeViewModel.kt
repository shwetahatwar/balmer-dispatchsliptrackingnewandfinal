package com.briot.balmerlawrie.implementor.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel;
import com.briot.balmerlawrie.implementor.repository.remote.RemoteRepository
//import com.briot.balmerlawrie.implementor.repository.remote.RoleAccessRelation
import java.net.SocketException
import java.net.SocketTimeoutException

class HomeViewModel : ViewModel() {
    val TAG = "HomeViewModel"

//    val roleAccessRelations: LiveData<Array<RoleAccessRelation>> = MutableLiveData<Array<RoleAccessRelation>>()

    val networkError: LiveData<Boolean> = MutableLiveData<Boolean>()

//    fun loadRoleAccess() {
//        (networkError as MutableLiveData<Boolean>).value = false
//        RemoteRepository.singleInstance.getRoleAccessRelation(this::handleGetRoleAccessRelations, this::handleGetRoleAccessRelationsError)
//    }

//    private fun handleGetRoleAccessRelations(roleAccessRelations: Array<RoleAccessRelation>) {
//        Log.d(TAG, "successful Role Access Relations" + roleAccessRelations.toString())
//        if (roleAccessRelations.size > 0) {
//            (this.roleAccessRelations as MutableLiveData<Array<RoleAccessRelation>>).value = roleAccessRelations
//        }else {
//            (this.roleAccessRelations as MutableLiveData<Array<RoleAccessRelation>>).value = null
//        }
//    }

//    private fun handleGetRoleAccessRelationsError(error: Throwable) {
//        Log.d(TAG, error.localizedMessage)
//
//        if (error is SocketException || error is SocketTimeoutException) {
//            (networkError as MutableLiveData<Boolean>).value = true
//        } else {
//            (this.roleAccessRelations as MutableLiveData<Array<RoleAccessRelation>>).value = null
//        }
//    }
}
