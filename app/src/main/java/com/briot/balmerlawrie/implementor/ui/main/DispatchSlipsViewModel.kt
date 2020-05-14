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

class DispatchSlipsViewModel : ViewModel() {
    val TAG = "DispatchLoadingListsVM"

    val networkError: LiveData<Boolean> = MutableLiveData()
    val dispatchLoadingList: LiveData<Array<DispatchSlip?>> = MutableLiveData()
    val invalidDispatchList: Array<DispatchSlip?> = arrayOf(null)
    var userId: Int = 0
    private var appDatabase = AppDatabase.getDatabase(MainApplication.applicationContext())


    fun loadDispatchLoadingLists(userId: Int) {
        (networkError as MutableLiveData<Boolean>).value = false
        (this.dispatchLoadingList as MutableLiveData<Array<DispatchSlip?>>).value = null // emptyArray()

        RemoteRepository.singleInstance.getAssignedLoaderDispatchSlips(userId, this::handleDispatchLoadingListsResponse, this::handleLoadingListsError)
    }

    fun isDispatchSlipInProgress(dispatchSlip: Int): Boolean {
        var dbDao = appDatabase.dispatchSlipLoadingItemDuo()
        val count = dbDao.getAllDispatchSlipItemsCount(dispatchSlip)
        if (count > 0) {
            return true
        }
        return false
    }

    private fun handleDispatchLoadingListsResponse(dispatchLoadingList: Array<DispatchSlip?>) {
        Log.d(TAG, "successful selected loader's dispatch list details" + dispatchLoadingList.toString())
        (this.dispatchLoadingList as MutableLiveData<Array<DispatchSlip?>>).value = dispatchLoadingList
    }

    private fun handleLoadingListsError(error: Throwable) {
        Log.d(TAG, error.localizedMessage)

        if (UiHelper.isNetworkError(error)) {
            (networkError as MutableLiveData<Boolean>).value = true
        } else {
            (this.dispatchLoadingList as MutableLiveData<Array<DispatchSlip?>>).value = invalidDispatchList
        }
    }
}

