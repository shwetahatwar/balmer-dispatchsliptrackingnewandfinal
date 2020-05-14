package com.briot.balmerlawrie.implementor.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.briot.balmerlawrie.implementor.UiHelper
import com.briot.balmerlawrie.implementor.repository.remote.DispatchSlip
import com.briot.balmerlawrie.implementor.repository.remote.Project
import com.briot.balmerlawrie.implementor.repository.remote.RemoteRepository

class AuditProjectsViewModel : ViewModel() {
    val TAG = "AuditProjectsVM"

    val networkError: LiveData<Boolean> = MutableLiveData()
    val projects: LiveData<Array<Project?>> = MutableLiveData()
    val invalidProjects: Array<Project?> = arrayOf(null)
    var userId: Int = 0
    var projectID: Int = 0

    fun loadAuditProjects(status: String) {
        (networkError as MutableLiveData<Boolean>).value = false
        (this.projects as MutableLiveData<Array<Project?>>).value = emptyArray()

        RemoteRepository.singleInstance.getProjects(status, this::handleProjectsResponse, this::handleProjectsError)
    }

    private fun handleProjectsResponse(projects: Array<Project?>) {
        // Log.d(TAG, "successful project list" + projects.toString())
        (this.projects as MutableLiveData<Array<Project?>>).value = projects
    }

    private fun handleProjectsError(error: Throwable) {
        Log.d(TAG, "error msg--->"+error.localizedMessage)

        if (UiHelper.isNetworkError(error)) {
            (networkError as MutableLiveData<Boolean>).value = true
        } else {
            (this.projects as MutableLiveData<Array<Project?>>).value = invalidProjects
        }
    }
}
