package com.briot.balmerlawrie.implementor.ui.main

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.briot.balmerlawrie.implementor.MainApplication
import com.briot.balmerlawrie.implementor.data.AppDatabase
import com.briot.balmerlawrie.implementor.UiHelper
import com.briot.balmerlawrie.implementor.data.DBAuditItem
import com.briot.balmerlawrie.implementor.data.DispatchSlipLoadingListItem
import com.briot.balmerlawrie.implementor.repository.local.PrefConstants
import com.briot.balmerlawrie.implementor.repository.local.PrefRepository
import com.briot.balmerlawrie.implementor.repository.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.gson.GsonBuilder

class AuditProjectListViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    var batchCode: String = ""
    var serialNumber: String = ""
    var material_barcode: String = ""
    var errorMessage: String = ""
    var projectId: Int = 0
    val networkError: LiveData<Boolean> = MutableLiveData()
    val projects: LiveData<Array<Project?>> = MutableLiveData()
    val itemSubmissionSuccessful: LiveData<Boolean> = MutableLiveData()

    //    val auditProjectListItems: Array<auditProjectList?> = arrayOf(null)
    val auditProjectListItems: LiveData<Array<auditProjectList?>> = MutableLiveData()

    val invalidProjects: Array<Project?> = arrayOf(null)
    private var appDatabase = AppDatabase.getDatabase(MainApplication.applicationContext())

    fun updateAuditProjects(auditRequestBody: Array<auditProjectItem>) {
        RemoteRepository.singleInstance.postProjectItems(auditRequestBody,
                this::handleAuditProjectsResponse, this::handleAuditProjectsError)
    }

    private fun handleAuditProjectsResponse(auditProject: auditProjectItem?) {
        Log.d(TAG, "successful project list abcbddd-->" + auditProject)
        // delete from dbo
        for (items in auditProjectListItems!!.value!!) {
            Log.d(TAG,"inside audit resp->"+items!!.serialNumber)
            deleteSelectedAuditFromDB(items!!.serialNumber!!)
        }
        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                (itemSubmissionSuccessful as MutableLiveData<Boolean>).value = true
            }
        }


    }

    fun loadAuditProjects(status: String) {
        (networkError as MutableLiveData<Boolean>).value = false
        (this.projects as MutableLiveData<Array<Project?>>).value = emptyArray()

        RemoteRepository.singleInstance.getProjects(status, this::handleProjectsResponse, this::handleProjectsError)
    }

    private fun handleProjectsResponse(projects: Array<Project?>) {
//        Log.d(TAG, "successful project list" + projects.toString())
        (this.projects as MutableLiveData<Array<Project?>>).value = projects
    }

    private fun handleAuditProjectsError(error: Throwable) {
        Log.d(TAG, "error msg auditProjectError-->" + error.localizedMessage)
        if (UiHelper.isNetworkError(error)) {
            (networkError as MutableLiveData<Boolean>).value = true
        } else {
            (this.projects as MutableLiveData<Array<Project?>>).value = invalidProjects
        }
    }

    private fun handleProjectsError(error: Throwable) {
        Log.d(TAG, "error msg xxxxx--->" + error.localizedMessage)
        if (UiHelper.isNetworkError(error)) {
            (networkError as MutableLiveData<Boolean>).value = true
        } else {
            (this.projects as MutableLiveData<Array<Project?>>).value = invalidProjects
        }
    }

    suspend fun addMaterial(productNumber: String, batchNumber: String, serialNumber: String, projectId: Int) : Boolean {
//        val result = responseDispatchLoadingItems.filter {
//            it?.materialCode.equals(materialCode)
//        }
//        if (result.size > 0) {
//            updateItemInDatabase(result.first()!!, serialNumber)
//        }
        Log.d(TAG,"addMaterial -->"+batchNumber)
        val auditProjectObj = auditProjectList()
        auditProjectObj.batchCode = batchNumber
        auditProjectObj.productCode = productNumber
        auditProjectObj.serialNumber = serialNumber
        auditProjectObj.projectId = projectId

        updateItemInDatabase(auditProjectObj)
        return true
    }
    private suspend fun updateItemInDatabase(item: auditProjectList) {
        // Log.d(TAG, "item in update "+ item.batchNumber)
        var dbItem = DBAuditItem(
                id = 0,
                batchCode = item.batchCode,
                productCode = item.productCode,
                serialNumber = item.serialNumber,
                projectId = item.projectId)
        var dbDao = appDatabase.auditListItemDuo()
//        Log.d(TAG,"dbitem --> "+dbItem)
        try {
            dbDao.insert(item = dbItem)
        }
        catch (e: Exception){
            Log.d(TAG, "Getting exception while inserting data to db "+ e)
        }
        try {
            GlobalScope.launch {
                withContext(Dispatchers.Main) {
                    updatedListAsPerDatabase(item.projectId)
                }
            }
        }  catch (e: Exception){
            Log.d(TAG, "--inside globallaunch exception "+ e)
        }
    }
    fun updatedListAsPerDatabase(projectId: Int) {
        var dbDao = appDatabase.auditListItemDuo()
        var dbItems = dbDao.getAllItems(projectId) //diff batch nunber items (dbItems)
//        Log.d(TAG, "dbItems size-->"+dbItems.size)
        var updatedItems: Array<auditProjectList?> = emptyArray()

        for (item in dbItems){
            updatedItems += item
        }
//        Log.d(TAG, "updatedItems size-->"+updatedItems.size)
        (this.auditProjectListItems as MutableLiveData<Array<auditProjectList?>>).value = updatedItems
    }

    fun deleteSelectedAuditFromDB(serialNumber: String) {

        var dbDao = appDatabase.auditListItemDuo()

        GlobalScope.launch {

            dbDao.deleteSelectedAuditFromDB(serialNumber)

            withContext(Dispatchers.Main) {
                //updatedListAsPerDatabase()
            }
        }
    }
}

