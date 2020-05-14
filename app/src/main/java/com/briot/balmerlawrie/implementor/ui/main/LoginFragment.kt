package com.briot.balmerlawrie.implementor.ui.main

import androidx.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.navigation.Navigation
import com.briot.balmerlawrie.implementor.MainActivity
import com.briot.balmerlawrie.implementor.R
import com.briot.balmerlawrie.implementor.repository.local.PrefConstants
import com.briot.balmerlawrie.implementor.repository.local.PrefRepository
import com.briot.balmerlawrie.implementor.repository.remote.User
import io.github.pierry.progress.Progress
import kotlinx.android.synthetic.main.login_fragment.*
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModelProvider
import com.briot.balmerlawrie.implementor.UiHelper
import com.briot.balmerlawrie.implementor.repository.remote.SignInResponse


class LoginFragment : androidx.fragment.app.Fragment() {

    companion object {
        fun newInstance() = LoginFragment()
    }

    private lateinit var viewModel: LoginViewModel
    private var progress: Progress? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.login_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        // TODO: Use the ViewModel

        username.requestFocus()

        viewModel.signInResponse.observe(this, Observer<SignInResponse> {
            UiHelper.hideProgress(this.progress)
            this.progress = null

            if (it != null) {
                this.activity?.invalidateOptionsMenu()
                PrefRepository.singleInstance.setKeyValue(PrefConstants().USER_TOKEN, it.token!!)
                PrefRepository.singleInstance.setKeyValue(PrefConstants().USER_ID, it.userId!!.toString())
                PrefRepository.singleInstance.setKeyValue(PrefConstants().USER_NAME, it.username!!)
                PrefRepository.singleInstance.setKeyValue(PrefConstants().ROLE_ID, it.roleId!!.toString())
                PrefRepository.singleInstance.setKeyValue(PrefConstants().ROLE_NAME, it.role!!)
                PrefRepository.singleInstance.setKeyValue(PrefConstants().EMPLOYEE_ID, it.employeeId!!)

                this.context?.let { it1 -> PrefRepository.singleInstance.serializePrefs(it1) }

                Navigation.findNavController(login).navigate(R.id.action_loginFragment_to_homeFragment)
            } else {
                UiHelper.showErrorToast(this.activity as AppCompatActivity, "An error has occurred, please try again.");
            }

        })

        viewModel.networkError.observe(this, Observer<Boolean> {

            if (it == true) {
                UiHelper.hideProgress(this.progress)
                this.progress = null

                var message: String = "Server is not reachable, please check if your network connection is working"
                if (viewModel.errorMessage.isNotEmpty()) {
                    message = viewModel.errorMessage
                }

                UiHelper.showSnackbarMessage(this.activity as AppCompatActivity, message, 3000);
            }
        })


        login.setOnClickListener {
            val keyboard = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            keyboard.hideSoftInputFromWindow(activity?.currentFocus?.getWindowToken(), 0)


            // @dineshgajjar - remove following coments later on
            this.progress = UiHelper.showProgressIndicator(this.activity as AppCompatActivity, "Please wait")
            viewModel.loginUser(username.text.toString(), password.text.toString())
            username.setText("");
            password.setText("");

        }
    }

}
