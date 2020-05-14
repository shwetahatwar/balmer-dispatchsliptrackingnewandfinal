package com.briot.balmerlawrie.implementor

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.briot.balmerlawrie.implementor.repository.local.PrefConstants
import com.briot.balmerlawrie.implementor.repository.local.PrefRepository
import kotlinx.android.synthetic.main.user_profile_fragment.*


class UserProfileFragment : Fragment() {

    companion object {
        fun newInstance() = UserProfileFragment()
    }

    private lateinit var viewModel: UserProfileViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.user_profile_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProvider(this).get(UserProfileViewModel::class.java)

        (this.activity as AppCompatActivity).setTitle("User Profile")


        val userName = PrefRepository.singleInstance.getValueOrDefault(PrefConstants().USER_NAME, "")
        val roleName = PrefRepository.singleInstance.getValueOrDefault(PrefConstants().ROLE_NAME, "")
        val employeeId = PrefRepository.singleInstance.getValueOrDefault(PrefConstants().EMPLOYEE_ID, "")

        userNameValue.text = userName
        userRoleValue.text = roleName
        userEmailValue.text = employeeId
        this.activity?.invalidateOptionsMenu()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.clear()
    }

}
