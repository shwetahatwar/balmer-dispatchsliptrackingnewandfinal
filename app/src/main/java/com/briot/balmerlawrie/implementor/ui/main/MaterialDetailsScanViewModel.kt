package com.briot.balmerlawrie.implementor.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel;
import android.util.Log
import com.briot.balmerlawrie.implementor.repository.remote.DispatchSlip
import com.briot.balmerlawrie.implementor.repository.remote.MaterialInward
import com.briot.balmerlawrie.implementor.repository.remote.RemoteRepository
import java.net.SocketException
import java.net.SocketTimeoutException

class MaterialDetailsScanViewModel : ViewModel() {
    val TAG = "MaterialScanViewModel"

    val materialInwards: LiveData<MaterialInward> = MutableLiveData<MaterialInward>()
    val dispatchSlip: LiveData<DispatchSlip> = MutableLiveData<DispatchSlip>()

    val networkError: LiveData<Boolean> = MutableLiveData<Boolean>()
    val invalidMaterialInward: MaterialInward = MaterialInward()

    fun loadMaterialItems(barcodeSerial: String) {
        (networkError as MutableLiveData<Boolean>).value = false
        RemoteRepository.singleInstance.getMaterialDetails(barcodeSerial, this::handleMaterialResponse, this::handleMaterialError)
    }

    private fun handleMaterialResponse(materialInwards: Array<MaterialInward>) {
        Log.d(TAG, "successful material" + materialInwards.toString())
        if (materialInwards.size > 0) {
            (this.materialInwards as MutableLiveData<MaterialInward>).value = materialInwards.first()
        }else {
            (this.materialInwards as MutableLiveData<MaterialInward>).value = null

        }
    }

    private fun handleMaterialError(error: Throwable) {
        Log.d(TAG, error.localizedMessage)

        if (error is SocketException || error is SocketTimeoutException) {
            (networkError as MutableLiveData<Boolean>).value = true
        } else {
            (this.materialInwards as MutableLiveData<MaterialInward>).value = null
        }
    }

    fun getMaterialDispatchSlip(dispatchSlipId:  String) {
        (networkError as MutableLiveData<Boolean>).value = false
        RemoteRepository.singleInstance.getDispatchSlip(dispatchSlipId, this::handleDispatchSlipResponse, this::handleDispatchSlipError)
    }

    private fun handleDispatchSlipResponse(dispatchSlips: Array<DispatchSlip>) {
        Log.d(TAG, "successful material" + materialInwards.toString())
        if (dispatchSlips.size > 0) {
            (this.dispatchSlip as MutableLiveData<DispatchSlip>).value = dispatchSlips.first()
        }else {
            (this.dispatchSlip as MutableLiveData<DispatchSlip>).value = null

        }
    }

    private fun handleDispatchSlipError(error: Throwable) {
        Log.d(TAG, error.localizedMessage)

        if (error is SocketException || error is SocketTimeoutException) {
            (networkError as MutableLiveData<Boolean>).value = true
        } else {
            (this.dispatchSlip as MutableLiveData<DispatchSlip>).value = null
        }
    }
}
