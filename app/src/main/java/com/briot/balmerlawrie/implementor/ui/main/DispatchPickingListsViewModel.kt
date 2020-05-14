package com.briot.balmerlawrie.implementor.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.briot.balmerlawrie.implementor.MainApplication
import com.briot.balmerlawrie.implementor.UiHelper
import com.briot.balmerlawrie.implementor.data.AppDatabase
import com.briot.balmerlawrie.implementor.repository.remote.DispatchSlip
import com.briot.balmerlawrie.implementor.repository.remote.RemoteRepository

class DispatchPickingListsViewModel : ViewModel() {
    val TAG = "DispatchPickingListsVM"

    val networkError: LiveData<Boolean> = MutableLiveData()
    val dispatchPickerList: LiveData<Array<DispatchSlip?>> = MutableLiveData()
    val invalidDispatchPickerList: Array<DispatchSlip?> = arrayOf(null)
    var userId: Int = 0
    private var appDatabase = AppDatabase.getDatabase(MainApplication.applicationContext())



    fun loadDispatchPickingLists(userId: Int) {
        (networkError as MutableLiveData<Boolean>).value = false
        (this.dispatchPickerList as MutableLiveData<Array<DispatchSlip?>>).value = null

        RemoteRepository.singleInstance.getAssignedPickerDispatchSlips(userId, this::handleDispatchPickingListsResponse, this::handleDispatchPickingListsError)
    }
    fun isDispatchSlipInProgress(dispatchSlip: Int): Boolean {
        var dbDao = appDatabase.dispatchSlipPickingItemDuo()
        val count = dbDao.getAllDispatchSlipItemsCount(dispatchSlip)
        if (count > 0) {
            return true
        }
        return false
    }

    private fun handleDispatchPickingListsResponse(dispatchPickerList: Array<DispatchSlip?>) {
        Log.d(TAG, "successful dispatch picker list details" + dispatchPickerList.toString())
        (this.dispatchPickerList as MutableLiveData<Array<DispatchSlip?>>).value = dispatchPickerList
    }

    private fun handleDispatchPickingListsError(error: Throwable) {
        Log.d(TAG, error.localizedMessage)

        if (UiHelper.isNetworkError(error)) {
            (networkError as MutableLiveData<Boolean>).value = true
        } else {
            (this.dispatchPickerList as MutableLiveData<Array<DispatchSlip?>>).value = invalidDispatchPickerList
        }
    }
}
