package com.briot.balmerlawrie.implementor.ui.main

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import androidx.fragment.app.DialogFragment
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.briot.balmerlawrie.implementor.MainApplication
import com.briot.balmerlawrie.implementor.UiHelper
import com.briot.balmerlawrie.implementor.repository.local.PrefConstants
import com.briot.balmerlawrie.implementor.repository.remote.DispatchSlipItem
import io.github.pierry.progress.Progress
import kotlinx.android.synthetic.main.dispatch_slip_loading_fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.briot.balmerlawrie.implementor.R
import com.briot.balmerlawrie.implementor.repository.remote.SignInResponse
import kotlinx.android.synthetic.main.login_dialog_fragment.view.*

class DispatchSlipLoadingFragment : Fragment(), LoginDialogListener {


    override fun onSuccessfulAdminLogin(productCode: String, batchCode:String, serialNumber:String ) {
        // UiHelper.showErrorToast(this.activity as AppCompatActivity, "dismissed dialog!")
        // Log.d(TAG, "After success ---->")
        addItemToList(productCode, batchCode, serialNumber)
        // Log.d(TAG, "After add list ---->")

    }

    companion object {
        fun newInstance() = DispatchSlipLoadingFragment()
    }

    private lateinit var viewModel: DispatchSlipLoadingViewModel
    private lateinit var LoginDialog: LoginDialog

    private var progress: Progress? = null
    private var oldDispatchSlipItems: Array<DispatchSlipItem?>? = null
    lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.dispatch_slip_loading_fragment, container, false)

        this.recyclerView = rootView.findViewById(R.id.loading_dispatchSlipItems)
        recyclerView.layoutManager = LinearLayoutManager(this.activity)

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProvider(this).get(DispatchSlipLoadingViewModel::class.java)

        // viewModel.getUsers()

        (this.activity as AppCompatActivity).setTitle("Loading Dispatch Slip")

        if (this.arguments != null) {
            viewModel.dispatchSlipId = this.arguments!!.getInt("loadingDispatchSlip_id")
            viewModel.dispatchSlipVehicleNumber = this.arguments!!.getString("loadingDispatchSlip_vehicle_number")
            viewModel.dispatchSlipNumber = this.arguments!!.getString("loadingDispatchSlip_slipnumber")
            viewModel.dispatchSlipStatus = this.arguments!!.getString("loadingDispatchSlip_slipstatus")
            viewModel.dispatchSlipTruckId = this.arguments!!.getInt("loadingDispatchSlip_truckid")
            viewModel.customer = this.arguments!!.getString("loadingDispatchSlip_customer")

            loading_dispatchSlipId.text = viewModel.dispatchSlipNumber
            loading_dispatchListStatusId.text = viewModel.dispatchSlipStatus
            loading_truckNumber.text = viewModel.dispatchSlipVehicleNumber
            loading_dispatchListCustomer.text = viewModel.customer
        }

        recyclerView.adapter = SimpleDispatchSlipLoadingItemAdapter(recyclerView, viewModel.dispatchloadingItems, viewModel)
        viewModel.dispatchloadingItems.observe(viewLifecycleOwner, Observer<Array<DispatchSlipItem?>> {
            if (it != null) {
                UiHelper.hideProgress(this.progress)
                this.progress = null

                if (viewModel.dispatchloadingItems.value.orEmpty().isNotEmpty() && viewModel.dispatchloadingItems.value?.first() == null) {
                    UiHelper.showSomethingWentWrongSnackbarMessage(this.activity as AppCompatActivity)
                    loading_scanned_count.text = "0/0"
                } else if (it != oldDispatchSlipItems) {
                    loading_dispatchSlipItems.adapter?.notifyDataSetChanged()

                    loading_scanned_count.text = viewModel.totalScannedItems.toString() + "/" + it.size.toString()
                }
            }

            loading_materialBarcode.text?.clear()
            loading_materialBarcode.requestFocus()

            oldDispatchSlipItems = viewModel.dispatchloadingItems.value
        })

        viewModel.networkError.observe(viewLifecycleOwner, Observer<Boolean> {
            if (it == true) {
                UiHelper.hideProgress(this.progress)
                this.progress = null


                if (viewModel.errorMessage != null) {
                    UiHelper.showErrorToast(this.activity as AppCompatActivity, viewModel.errorMessage)
                } else {
                    UiHelper.showNoInternetSnackbarMessage(this.activity as AppCompatActivity)
                }

            }
        })

        viewModel.itemSubmissionSuccessful.observe(viewLifecycleOwner, Observer<Boolean> {
            if (it == true) {
                UiHelper.hideProgress(this.progress)
                this.progress = null

                var thisObject = this
                AlertDialog.Builder(this.activity as AppCompatActivity, R.style.MyDialogTheme).create().apply {
                setTitle("Success")
                    setMessage("Dispatch slip for loading operation submitted successfully.")
                    setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", {
                        dialog, _ -> dialog.dismiss()
//                        Navigation.findNavController(thisObject.recyclerView).popBackStack(R.id.homeFragment, false)
                        Navigation.findNavController(thisObject.recyclerView).popBackStack()
                    })
                    show()
                }
            }
        })

//        viewModel.signInResponse.observe(this, Observer<SignInResponse> {
//            UiHelper.hideProgress(this.progress)
//            this.progress = null
//
//            if (it != null) {
//                this.addItemToList(viewModel.productCode, viewModel.batchCode, viewModel.serialNumber)
//            } else {
//                UiHelper.showErrorToast(this.activity as AppCompatActivity, "An error has occurred, please try again.");
//            }
//        })

        loading_materialBarcode.setOnEditorActionListener { _, i, keyEvent ->
            var handled = false

            if ((loading_materialBarcode.text != null && loading_materialBarcode.text!!.isNotEmpty())
                    && i == EditorInfo.IME_ACTION_DONE || (keyEvent != null && (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                            keyEvent.keyCode == KeyEvent.KEYCODE_TAB) && keyEvent.action == KeyEvent.ACTION_DOWN)) {
                val keyboard = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                keyboard.hideSoftInputFromWindow(activity?.currentFocus?.getWindowToken(), 0)

                var value = loading_materialBarcode.text!!.toString().trim()
                var arguments  = value.split("#")
                var productCode = ""
                var batchCode = ""
                var serialNumber =  ""

                if (arguments.size < 3 || arguments[0].length == 0 || arguments[1].length == 0 || arguments[2].length == 0) {
                    UiHelper.showErrorToast(this.activity as AppCompatActivity, "Invalid barcode, please try again!")
                } else {
                    productCode = arguments[0].toString()
                    batchCode = arguments[1].toString()
                    serialNumber = value

                    if (viewModel.isMaterialBelongToSameGroup(productCode, batchCode)) {
                        if (viewModel.materialQuantityPickingCompleted(productCode, batchCode)) {
                            UiHelper.showErrorToast(this.activity as AppCompatActivity, "For given batch and material, quantity is already picked for dispatch!")
                        } else {
                            if (viewModel.isSameSerialNumber(productCode, batchCode, serialNumber)) {
                                UiHelper.showErrorToast(this.activity as AppCompatActivity, "This barcode is already added, please add other item")
                            } else {
                                this.progress = UiHelper.showProgressIndicator(this.activity as AppCompatActivity, "Please wait")
                                // prodeed to add the material in database
                                GlobalScope.launch {
                                    Log.d(TAG,"inside loading material barcode"+productCode)
                                    viewModel.addMaterial(productCode, batchCode, serialNumber)
                                }
                            }
                        }

                    } else {
                        UiHelper.showErrorToast(this.activity as AppCompatActivity, "Scanned material batch and material is not matching with dispatch slip!")
                        // @dinesh gajjar: get admin permission flow

                        var thisObject = this
                        AlertDialog.Builder(this.activity as AppCompatActivity, R.style.MyDialogTheme).create().apply {
                            setTitle("Confirm")
                            setMessage("Are you sure you want to load this material from different batch?")
                            setButton(AlertDialog.BUTTON_NEUTRAL, "No", { dialog, _ -> dialog.dismiss() })
                            setButton(AlertDialog.BUTTON_POSITIVE, "Yes", {
                                dialog, _ -> dialog.dismiss()
                                // open another dialog of credentials  to check  if user has valid admin role
                                // call thisObject.addItemToList(productCode, batchCode, serialNumber)
                                // thisObject.addItemToList(productCode, batchCode, serialNumber)
                                thisObject.openLoginDialog(productCode, batchCode, serialNumber)
                            })
                            show()
                        }
                    }
                }

                loading_materialBarcode.text?.clear()
                loading_materialBarcode.requestFocus()

                handled = true
            }
            handled
        }

        loading_items_submit_button.setOnClickListener({
            if (viewModel.dispatchloadingItems  != null && viewModel.dispatchloadingItems.value != null && viewModel.dispatchloadingItems.value!!.size > 0) {
                val keyboard = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                keyboard.hideSoftInputFromWindow(activity?.currentFocus?.getWindowToken(), 0)
                if (viewModel.dispatchSlipStatus.toString().toLowerCase().contains("complete")) {
                    UiHelper.showWarningToast(this.activity as AppCompatActivity, "Items can not be scanned for completed Dispatch Slip")
                } else if (MainApplication.hasNetwork(MainApplication.applicationContext())) {

                    if (viewModel.isDispatchListSubmitted()) {
                        UiHelper.showErrorToast(this.activity as AppCompatActivity, "Items listed in this dispatch list is already submitted")

                    } else if (!viewModel.isDispatchSlipHasEntries()) {
                        UiHelper.showErrorToast(this.activity as AppCompatActivity, "There is no item added for selected dispatch list")

                    } else {

                        var thisObject = this
                        AlertDialog.Builder(this.activity as AppCompatActivity, R.style.MyDialogTheme).create().apply {
                            setTitle("Confirm")
                            setMessage("Are you sure you want to submit this dispatch slip items")
                            setButton(AlertDialog.BUTTON_NEUTRAL, "No", { dialog, _ -> dialog.dismiss() })
                            setButton(AlertDialog.BUTTON_POSITIVE, "Yes", {
                                dialog, _ -> dialog.dismiss()
                                thisObject.progress = UiHelper.showProgressIndicator(thisObject.activity as AppCompatActivity, "Please wait")

                                GlobalScope.launch {
                                    viewModel.handleSubmitLoadingList()
                                }
                            })
                            show()
                        }
                    }
                } else {
                    UiHelper.showErrorToast(this.activity as AppCompatActivity, "Please submit the list when in Network!")
                }

                loading_materialBarcode.text?.clear()
                loading_materialBarcode.requestFocus()
            }

        })

        loading_scanButton.setOnClickListener({

        })


        this.progress = UiHelper.showProgressIndicator(activity!!, "Loading dispatch slip Items")
        viewModel.loadDispatchSlipLoadingItems()

        loading_materialBarcode.requestFocus()
    }

    fun addItemToList(productCode: String, batchCode:  String, serialNumber: String) {

        if (viewModel.isSameSerialNumber(productCode, batchCode, serialNumber)) {
            UiHelper.showErrorToast(this.activity as AppCompatActivity, "This barcode is already added, please add other item")
        } else {
            // this.progress = UiHelper.showProgressIndicator(this.activity as AppCompatActivity, "Please wait")
            // prodeed to add the material in database
            GlobalScope.launch {
                viewModel.addMaterial(productCode, batchCode, serialNumber)
            }
        }
    }

    fun openLoginDialog(productCode: String, batchCode:  String, serialNumber: String) {
//        viewModel.productCode = productCode
//        viewModel.batchCode = batchCode
//        viewModel.serialNumber = serialNumber
        val dialogFragment = LoginDialog()
        dialogFragment.loginDialogListener = this
        dialogFragment.productCode = productCode
        dialogFragment.batchCode = batchCode
        dialogFragment.serialNumber = serialNumber
        viewModel.serialNumber = serialNumber
        viewModel.batchCode = batchCode
        viewModel.productCode = productCode

        val ft = this.activity!!.supportFragmentManager.beginTransaction()
        val prev = this.activity!!.supportFragmentManager.findFragmentByTag("dialog")
        if (prev != null)
        {
            ft.remove(prev)
        }
        ft.addToBackStack(null)
        dialogFragment.show(ft, "dialog")
//        if(dialogFragment.adminAuthenticated == true){
//            Log.d(TAG, "in side if to call additem to list-->")
//            addItemToList(productCode, batchCode, serialNumber)
//        }


//        dialogFragment.onDismiss{
//            UiHelper.showErrorToast(this.activity as AppCompatActivity, "dismissed dialog!")
//        }
//        mAlertDialog.setOnDismissListener({
//            UiHelper.showErrorToast(this.activity as AppCompatActivity, "dismissed dialog!")
//        })
         return;

        val mDialogView = LayoutInflater.from(this.context).inflate(R.layout.login_dialog_fragment, null)
        //AlertDialogBuilder
        val mBuilder = AlertDialog.Builder(this.requireContext())
                .setView(mDialogView)
        //show dialog
        val  mAlertDialog = mBuilder.show()
        (mDialogView as? LoginDialog)?.alertDialog = mAlertDialog

//         Log.d(TAG, "adminAuthenticated 316 Line-->"+LoginDialog.adminAuthenticated)

        mAlertDialog.setOnDismissListener({
           //only checking admin Authentication
            UiHelper.showErrorToast(this.activity as AppCompatActivity, "dismissed dialog!")
            Log.d(TAG, "LoginDialog.adminAuthenticated -- "+LoginDialog.adminAuthenticated)

//            if(mDialogView.adminAuthenticated == true){
            if(LoginDialog.adminAuthenticated == true){
                Log.d(TAG, "in side if to call additem to list-->")
                addItemToList(productCode,batchCode,serialNumber)
            }
        })
    }
}

open class SimpleDispatchSlipLoadingItemAdapter(private val recyclerView: androidx.recyclerview.widget.RecyclerView,
    private val dispatchSlipItems: LiveData<Array<DispatchSlipItem?>>,
    private val viewModel: DispatchSlipLoadingViewModel) : androidx.recyclerview.widget.RecyclerView.Adapter<SimpleDispatchSlipLoadingItemAdapter.ViewHolder>() {

//    private var viewModel: DispatchSlipsViewModel = viewModel

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.dispatch_slip_item_row, parent, false)

        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()

        val dispatchSlipItem = dispatchSlipItems.value!![position]!!
        holder.itemView.setOnClickListener{

            if (viewModel.dispatchSlipStatus.toString().toLowerCase().contains("complete")) {
                return@setOnClickListener
            }

            val list = mutableListOf<String>()
            var dbItems = viewModel.getItemsOfSameBatchProductCode(dispatchSlipItem.batchNumber!!, dispatchSlipItem.materialCode!!)
            if (dbItems != null) {
                for (dbItem in dbItems!!.iterator()) {
                    var item = dbItem.batchCode + "#" + dbItem.productCode + "#" + dbItem.serialNumber
                    list.add(item)
                }
            }

            val listPopupWindow = ListPopupWindow(this.recyclerView.context)
            listPopupWindow.setAnchorView(it)
            listPopupWindow.setDropDownGravity(Gravity.CENTER_HORIZONTAL)
            listPopupWindow.height = ListPopupWindow.WRAP_CONTENT
            listPopupWindow.width = ListPopupWindow.MATCH_PARENT
            listPopupWindow.isModal = true
            listPopupWindow.setAdapter(ArrayAdapter(this.recyclerView.context,
                    android.R.layout.simple_list_item_1, list.toTypedArray())) // list_item is your textView with gravity.

            listPopupWindow.setOnItemClickListener { parent, view, position, id ->
                listPopupWindow.dismiss()
                var item = dbItems[position]
                var message = "Are you sure you want to remove this item  from scanned dispatch list?"
                AlertDialog.Builder(recyclerView.context, R.style.MyDialogTheme).create().apply {
                    setTitle("Confirm")
                    setMessage("Are you sure you want to remove this item \n\n${list[position]}\n\nfrom scanned dispatch list?")
                    setButton(AlertDialog.BUTTON_NEUTRAL, "NO", {
                        dialog, _ -> dialog.dismiss()
                    })
                    setButton(AlertDialog.BUTTON_POSITIVE, "YES", {
                        dialog, _ -> dialog.dismiss()
                        if (item.batchCode != null && item.productCode != null && item.serialNumber != null) {
                            viewModel.deleteItemFromDB(item.batchCode!!, item.productCode!!, item.serialNumber!!)
                        }
                    })
                    show()
                }
            }

            listPopupWindow.show()
        }
    }

    override fun getItemCount(): Int {
        return dispatchSlipItems.value?.size ?: 0
    }


    open inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        protected val dispatchSlipItemBatchNumber: TextView
        protected val dispatchSlipItemMaterialCode: TextView
        protected val dispatchSlipItemMaterialDetails:  TextView
        protected val dispatchSlipItemPackQuantity: TextView
        protected val linearLayout: LinearLayout

        init {
            dispatchSlipItemBatchNumber = itemView.findViewById(R.id.dispatch_slip_item_batch_number)
            dispatchSlipItemMaterialCode = itemView.findViewById(R.id.dispatch_slip_item_material_product_code)
            dispatchSlipItemPackQuantity = itemView.findViewById(R.id.dispatch_slip_item_material_pack_quantity)
            dispatchSlipItemMaterialDetails = itemView.findViewById(R.id.dispatch_slip_item_material_product_details)
            linearLayout = itemView.findViewById(R.id.dispatch_slip_layout)
        }

        fun bind() {
            val dispatchSlipItem = dispatchSlipItems.value!![adapterPosition]!!

            dispatchSlipItemBatchNumber.text = dispatchSlipItem.batchNumber
            dispatchSlipItemMaterialCode.text = dispatchSlipItem.materialCode
            dispatchSlipItemMaterialDetails.text = dispatchSlipItem.materialGenericName;
            dispatchSlipItemPackQuantity.text = dispatchSlipItem.scannedPacks.toString() + "/" + dispatchSlipItem.numberOfPacks.toString()
            if (dispatchSlipItem.scannedPacks.toInt() == 0) {
                linearLayout.setBackgroundColor(PrefConstants().lightGrayColor)
            } else if (dispatchSlipItem.scannedPacks.toInt() < dispatchSlipItem.numberOfPacks.toInt()) {
                linearLayout.setBackgroundColor(PrefConstants().lightOrangeColor)
            } else if (dispatchSlipItem.scannedPacks.toInt() >= dispatchSlipItem.numberOfPacks.toInt()) {
                linearLayout.setBackgroundColor(PrefConstants().lightGreenColor)
            }
            if (dispatchSlipItem.materialCode == viewModel.productCode && dispatchSlipItem.batchNumber == viewModel.batchCode){
               linearLayout.setBackgroundColor(PrefConstants().lightYellowColor)
            }
        }
    }
}
