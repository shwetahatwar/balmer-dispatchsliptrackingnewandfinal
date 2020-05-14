package com.briot.balmerlawrie.implementor.ui.main

import android.app.Activity
import android.app.Dialog
import android.content.ContentValues.TAG
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.login_dialog_fragment.*
import kotlinx.android.synthetic.main.login_dialog_fragment.view.*
import com.briot.balmerlawrie.implementor.R
import com.briot.balmerlawrie.implementor.UiHelper
import com.briot.balmerlawrie.implementor.repository.remote.SignInResponse
import io.github.pierry.progress.Progress

interface LoginDialogListener {
    fun onSuccessfulAdminLogin(productCode: String, batchCode:String, serialNumber:String)
}

class LoginDialog: DialogFragment() {

    companion object {
        fun newInstance() = LoginDialog()
    }

    private lateinit var viewModel: LoginDialogViewModel
   // private lateinit var viewModelDispatchLoading: DispatchSlipLoadingViewModel

    private var progress: Progress? = null
    var alertDialog: AlertDialog? = null
    var adminAuthenticated: Boolean = false
    var loginDialogListener: LoginDialogListener? = null
    var productCode : String = ""
    var batchCode : String = ""
    var serialNumber : String = ""

    
    // internal var callback: onActivityCreated
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.login_dialog_fragment, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(LoginDialogViewModel::class.java)

        Log.d(TAG, "LoginDialog.kt --->")
        viewModel.signInResponse.observe(viewLifecycleOwner, Observer<SignInResponse> {
            UiHelper.hideProgress(this.progress)
            // Log.d(TAG, "while checking roleId --->"+  viewModel.signInResponse!!.value)
            this.progress = null
            // Log.d(TAG, "while checking roleId 1 --->"+ it.roleId)
            if (it != null && it.roleId!!.toInt() == 1) {
                if(it.roleId!!.toInt() == 1){
                    loginDialogListener?.onSuccessfulAdminLogin(productCode, batchCode, serialNumber)
                    // Log.d(TAG, "while checking roleId 2 --->"+ it.roleId)
                    dismiss()
                }else{
                    UiHelper.showErrorToast(this.activity as AppCompatActivity, "this user do not have permission for FIFO violation.");
                }
                Log.d(TAG, "in observer success --->")
                // DispatchSlipLoadingFragment
                // this.addItemToList(viewModel.productCode, viewModel.batchCode, viewModel.serialNumber)
                Log.d(TAG, "after call to add --> ")

            } else {
                UiHelper.showErrorToast(this.activity as AppCompatActivity, "An error has occurred, please try again.");
            }
        })


//        val mDialogView = LayoutInflater.from(this.context).inflate(R.layout.login_dialog_fragment, null)
//        //AlertDialogBuilder
//        val mBuilder = AlertDialog.Builder(this.requireContext())
//                .setView(mDialogView)
//                .setTitle("Admin Login")
//        //show dialog
//        val  mAlertDialog = mBuilder.show()
//        (mDialogView as? LoginDialog)?.alertDialog = mAlertDialog
//
        //login button click of custom layout
        dialogLoginBtn.setOnClickListener {
            val name = dialogNameEt.text.toString()
            val password = dialogPasswEt.text.toString()
            viewModel.loginUser(name, password)
            Log.d(TAG, "name -->"+ name)
            Log.d(TAG, "password -->"+ password)
        }
        //cancel button click of custom layout
        dialogCancelBtn.setOnClickListener {
            //dismiss dialog
            dismiss()
        }
    }
}
