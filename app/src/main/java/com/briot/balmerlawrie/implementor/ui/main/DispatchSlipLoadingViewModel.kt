package com.briot.balmerlawrie.implementor.ui.main

import android.app.Application
import android.content.ContentValues
import android.graphics.DiscretePathEffect
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.briot.balmerlawrie.implementor.MainApplication
import com.briot.balmerlawrie.implementor.UiHelper
import com.briot.balmerlawrie.implementor.data.AppDatabase
import com.briot.balmerlawrie.implementor.data.DispatchSlipLoadingListItem
import com.briot.balmerlawrie.implementor.repository.local.PrefConstants
import com.briot.balmerlawrie.implementor.repository.local.PrefRepository
import com.briot.balmerlawrie.implementor.repository.remote.*
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*

class DispatchSlipLoadingViewModel : ViewModel() {

    var dispatchSlipId: Int = 0
    var dispatchSlipNumber: String? = ""
    var dispatchSlipVehicleNumber: String? = ""
    var dispatchSlipStatus: String? = ""
    var userId: Int = 0
    var dispatchSlipTruckId: Int = 0
    var totalScannedItems: Int = 0
    var customer: String? = null
    val signInResponse: LiveData<SignInResponse> = MutableLiveData()
    val users: LiveData<userResponse?> = MutableLiveData()
//    var allUsers: LiveData<Array<SignInResponse>> = MutableLiveData()

    var userResponseData: (Array<userResponse?>) = arrayOf(null)

    // var allUsers: MutableLiveData<userResponse> = MutableLiveData()
    var productCode: String = ""
    var batchCode: String = ""
    var serialNumber: String = ""
    var username: String = ""
    var password: String = ""

    private var appDatabase = AppDatabase.getDatabase(MainApplication.applicationContext())

    val TAG = "DispatchLoadingListVM"

    val networkError: LiveData<Boolean> = MutableLiveData()
    val itemSubmissionSuccessful: LiveData<Boolean> = MutableLiveData()
    val dispatchloadingItems: LiveData<Array<DispatchSlipItem?>> = MutableLiveData()
    private var responseDispatchLoadingItems: Array<DispatchSlipItem?> = arrayOf(null)
    // private lateinit var DispatchSlipItemToUpdate: DispatchSlipItem
    val DispatchSlipItemToUpdate: LiveData<Array<DispatchSlipItem?>> = MutableLiveData()

    // MutableLiveData()
    val invalidDispatchloadingItems: Array<DispatchSlipItem?> = arrayOf(null)
    var errorMessage: String = ""

    fun loadDispatchSlipLoadingItems() {
        (networkError as MutableLiveData<Boolean>).value = false
        (this.dispatchloadingItems as MutableLiveData<Array<DispatchSlipItem?>>).value = null

        RemoteRepository.singleInstance.getDispatchSlipItems(dispatchSlipId, this::handleDispatchLoadingItemsResponse, this::handleDispatchLoadingItemsError)
    }

    private fun handleDispatchLoadingItemsResponse(dispatchSlipItems: Array<DispatchSlipItem?>) {
        Log.d(TAG, "successful dispatch loading items details" + dispatchSlipItems.toString())

        responseDispatchLoadingItems = dispatchSlipItems
        updatedListAsPerDatabase(responseDispatchLoadingItems)

    }

    private fun handleDispatchLoadingItemsError(error: Throwable) {
        Log.d(TAG, error.localizedMessage)

        if (UiHelper.isNetworkError(error)) {
            (networkError as MutableLiveData<Boolean>).value = true
            errorMessage = "Not able to connect to the server."
        } else if (error is HttpException) {
            if (error.code() >= 401) {
                var msg = error.response()?.errorBody()?.string()
                if (msg != null && msg.isNotEmpty()) {
                    errorMessage = msg
                } else {
                    errorMessage = error.message()
                }
            }
            (networkError as MutableLiveData<Boolean>).value = true
        }  else {
            (this.dispatchloadingItems as MutableLiveData<Array<DispatchSlipItem?>>).value = invalidDispatchloadingItems
            errorMessage = "Oops something went wrong."
        }
    }

    private fun updatedListAsPerDatabase(items: Array<DispatchSlipItem?>) {

        var dbDao = appDatabase.dispatchSlipLoadingItemDuo()
        var dbItems = dbDao.getAllDispatchSlipItems(dispatchSlipId) //diff batch nunber items (dbItems)
//        var differentItems: Array<DispatchSlipItem?> = arrayOf(null)
        //server items
         var updatedItems: Array<DispatchSlipItem?> = items.clone()
        totalScannedItems = 0
        for (item in updatedItems) {
            if (item != null) {
                var count = dbDao.getCountForBatchMaterialCode(
                        dispatchSlipId,
                        item.materialCode!!,
                        item.batchNumber!!
                )
                item.scannedPacks = count
                if (item.scannedPacks.toInt() == item.numberOfPacks.toInt()) {
                    totalScannedItems += 1
                }
            }
        }

        var checkingItems = updatedItems;
        for (dbitem in dbItems){
            var checkStatus = false;
            var newItem = null;
            for(item in checkingItems){
                var checkArguments  = dbitem.serialNumber!!.split("#")
                if(item!!.materialCode == dbitem.productCode && item.batchNumber == checkArguments[1]){
                    checkStatus = false
                    break;
                }
                else{
                    checkStatus = true
                }
            }
            if(checkStatus == true){
                var checkArguments  = dbitem.serialNumber!!.split("#")
//                if(dbitem.productCode == item!!.materialCode && checkArguments[1] != item.batchNumber && item.numberOfPacks != item.scannedPacks){
                    var arguments  = dbitem.serialNumber!!.split("#")
                    var item = DispatchSlipItem()
                    item.id = 0
                    item.scannedPacks = 1
//                item.batchNumber = dbitem.batchCode
                    item.batchNumber = arguments[1]
                    item.materialCode = dbitem.productCode
                    item.dispatchSlipId = dbitem.dispatchSlipId
                    item.materialGenericName = dbitem.materialGenericName
                    item.numberOfPacks = 1
                    updatedItems += item
                    totalScannedItems += 1
//                    break;
//                }
            }
        }
//    }

        updatedItems.sortWith(compareBy<DispatchSlipItem?> {
            it!!.scannedPacks.toInt() == it!!.numberOfPacks.toInt()
        }.thenBy {
            (it!!.scannedPacks.toInt() < it!!.numberOfPacks.toInt())
        }.thenBy {
            it!!.scannedPacks == 0
        })

        (this.dispatchloadingItems as MutableLiveData<Array<DispatchSlipItem?>>).value = updatedItems
    }

    fun isMaterialBelongToSameGroup(materialCode: String, batchNumber: String): Boolean {
        val result = responseDispatchLoadingItems.filter {
            (it?.materialCode.equals(materialCode) && it?.batchNumber.equals(batchNumber))
        }
        return (result.size > 0)
    }

    fun materialQuantityPickingCompleted(materialCode: String, batchNumber: String): Boolean {

        val result = responseDispatchLoadingItems.filter {
            (it?.materialCode.equals(materialCode) && it?.batchNumber.equals(batchNumber))
        }

        var dbDao = appDatabase.dispatchSlipLoadingItemDuo()

        for (item in result) {
            if (item != null) {
                var count = dbDao.getCountForBatchMaterialCode(
                        dispatchSlipId,
                        item.materialCode!!,
                        item.batchNumber!!
                )
                item.scannedPacks = count
                if (item.scannedPacks.toInt() == item.numberOfPacks.toInt()) {
                    return true
                } else {
                    return false
                }
            }
        }

        return false
    }

    fun isSameSerialNumber(materialCode: String, batchNumber: String, serialNumber: String): Boolean {

        var dbDao = appDatabase.dispatchSlipLoadingItemDuo()

        var count = dbDao.getCountForBatchMaterialCodeSerial(
                dispatchSlipId,
                materialCode,
                batchNumber,
                serialNumber
        )

        if (count > 0) {
            return true
        } else {
            return false
        }
    }

    suspend fun addMaterial(materialCode: String, batchNumber: String, serialNumber: String): Boolean {
        val result = responseDispatchLoadingItems.filter {
            it?.materialCode.equals(materialCode)
//            it?.batchNumber.equals(batchNumber)
        }
        if (result.size > 0) {
            updateItemInDatabase(result.first()!!, serialNumber)
        }
        return true
    }

    private suspend fun updateItemInDatabase(item: DispatchSlipItem, serialNumber: String) {

        val userName = PrefRepository.singleInstance.getValueOrDefault(PrefConstants().USER_NAME, "")

        // Log.d(TAG, "item in update "+ item.batchNumber)
        var argument = serialNumber.split("#")
        var dbItem = DispatchSlipLoadingListItem(
//                batchCode = item.batchNumber,
                batchCode = argument[1],
                productCode = item.materialCode,
                materialGenericName = item.materialGenericName,
                materialDescription = item.materialDescription,
                dispatchSlipId = item.dispatchSlipId!!.toInt(),
                dipatchSlipNumber = dispatchSlipNumber,
                timeStamp = Date().time,
                serialNumber = serialNumber,
                vehicleNumber = dispatchSlipVehicleNumber, id = 0, submitted = 0, submittedOn = 0, user = userName)

        var dbDao = appDatabase.dispatchSlipLoadingItemDuo()
        dbDao.insert(item = dbItem)

        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                updatedListAsPerDatabase(responseDispatchLoadingItems)
            }
        }
    }

    fun isDispatchSlipHasEntries(): Boolean {
        var dbDao = appDatabase.dispatchSlipLoadingItemDuo()
        var dbItems = dbDao.getAllDispatchSlipItems(dispatchSlipId)

        if (dbItems != null && dbItems.size > 0) {
            return true
        }

        return false
    }
    fun isDispatchListSubmitted(): Boolean {
        var dbDao = appDatabase.dispatchSlipLoadingItemDuo()
        var dbItems = dbDao.getSubmitedDispatchDetails(
                dispatchSlipId
        )

        if (dbItems != null && dbItems.size > 0) {
            return true
        }

        return false
    }



    suspend fun handleSubmitLoadingList() {
        var dispatchSlipRequestObject = DispatchSlipRequest()
        var dbDao = appDatabase.dispatchSlipLoadingItemDuo()
        var dbItems = dbDao.getAllDispatchSlipItems(
                dispatchSlipId
        )

        var items = mutableListOf<DispatchSlipItemRequest>()
        var startTime: Long = 0
        var endTime: Long = 0
        if (dbItems != null) {

            startTime = dbItems!!.first().timeStamp
            endTime = dbItems!!.last().timeStamp

            for (dbItem in dbItems!!.iterator()) {
                var item = DispatchSlipItemRequest()
                item.batchNumber = dbItem.batchCode
                item.materialCode = dbItem.productCode
                item.serialNumber = dbItem.serialNumber
                items.add(item)
                // Log.d(TAG,"dbItem.productCode-->"+dbItem.productCode)
            }
        }

        dispatchSlipRequestObject.dispatchId = dispatchSlipId
        dispatchSlipRequestObject.truckNumber = dispatchSlipVehicleNumber
        dispatchSlipRequestObject.truckId = dispatchSlipTruckId
        dispatchSlipRequestObject.loadStartTime = startTime
        dispatchSlipRequestObject.loadEndTime = endTime
        dispatchSlipRequestObject.materials = items.toTypedArray()


        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                (networkError as MutableLiveData<Boolean>).value = false
            }
        }

        RemoteRepository.singleInstance.postDispatchSlipLoadedMaterials(dispatchSlipId, dispatchSlipRequestObject,
                this::handleDispatchLoadingItemsSubmissionResponse, this::handleDispatchLoadingItemsSubmissionError)


    }

    private fun handleDispatchLoadingItemsSubmissionResponse(dispatchSlipResponse: DispatchSlipItemResponse?) {
        var dbDao = appDatabase.dispatchSlipLoadingItemDuo()
        GlobalScope.launch {
            val timestamp = Date().time
            dbDao.updateSubmittedStatus(dispatchSlipId.toString(), timestamp)

            withContext(Dispatchers.Main) {
                (itemSubmissionSuccessful as MutableLiveData<Boolean>).value = true
            }
        }

    }

    private fun handleDispatchLoadingItemsSubmissionError(error: Throwable) {
        Log.d(TAG, error.localizedMessage)

        if (UiHelper.isNetworkError(error)) {
            (networkError as MutableLiveData<Boolean>).value = true
            errorMessage = "Not able to connect to the server."
        } else if (error is HttpException) {
            if (error.code() >= 401) {
                var msg = error.response()?.errorBody()?.string()
                if (error.message() != null && error.message().length > 0) {
                    errorMessage = error.message()
                } else if (msg != null && msg.isNotEmpty()) {
                    errorMessage = msg
                } else {
                    errorMessage = "Unknown error has occurred, please try again later!"
                }
            }
            (networkError as MutableLiveData<Boolean>).value = true
        }  else {
            (networkError as MutableLiveData<Boolean>).value = true
            errorMessage = "Oops something went wrong."
        }
    }

    fun getItemsOfSameBatchProductCode(batchNumber: String, materialCode: String): List<DispatchSlipLoadingListItem> {

        var dbDao = appDatabase.dispatchSlipLoadingItemDuo()

        var dbItems = dbDao.getItemsForBatchMaterialCode(
                dispatchSlipId,
                materialCode,
                batchNumber
        )

        return dbItems
    }

    fun deleteItemFromDB(batchNumber: String, materialCode: String, serialNumber: String) {

        var dbDao = appDatabase.dispatchSlipLoadingItemDuo()

        GlobalScope.launch {

            dbDao.deleteSelectedItem(dispatchSlipId, materialCode, batchNumber, serialNumber)

            withContext(Dispatchers.Main) {
                updatedListAsPerDatabase(responseDispatchLoadingItems)
            }
        }
    }



    fun getUsers(){
        RemoteRepository.singleInstance.getUsers(this::handleUserResponse, this::handleLoginError)
    }

    private fun handleUserResponse(users: (Array<userResponse?>)) {

        userResponseData = users
        Log.d(ContentValues.TAG, "item  response----- " + userResponseData)
//        Log.d(ContentValues.TAG, "item  response1----- " + userResponseData)
//        Log.d(ContentValues.TAG, "item  handleUserResponse----- " + userResponseData[1]!!.username)
//        (this.users as MutableLiveData<userResponse?>).value = users
        (this.users as MutableLiveData<Array<userResponse?>>).value = users
    }



    fun loginUser(username: String, password: String) {
        (networkError as MutableLiveData<Boolean>).value = false
        RemoteRepository.singleInstance.loginUser(username, password, this::handleLoginResponse, this::handleLoginError)
    }

    private fun handleLoginResponse(signInResponse: SignInResponse) {
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
