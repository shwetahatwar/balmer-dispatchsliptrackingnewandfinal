package com.briot.balmerlawrie.implementor.ui.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import android.widget.LinearLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import com.briot.balmerlawrie.implementor.MainApplication

import com.briot.balmerlawrie.implementor.R
import com.briot.balmerlawrie.implementor.UiHelper
import com.briot.balmerlawrie.implementor.repository.local.PrefConstants
import com.briot.balmerlawrie.implementor.repository.remote.DispatchSlipItem
import io.github.pierry.progress.Progress
import kotlinx.android.synthetic.main.dispatch_picking_list_fragment.*
import kotlinx.android.synthetic.main.dispatch_slip_loading_fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.briot.balmerlawrie.implementor.ui.main.SimpleDispatchSlipPickingItemAdapter.ViewHolder as SimpleDispatchSlipPickingItemAdapterViewHolder

class DispatchPickingListDetailsFragment : Fragment() {

    companion object {
        fun newInstance() = DispatchPickingListDetailsFragment()
    }

    private lateinit var viewModel: DispatchPickingListDetailsViewModel
    private var progress: Progress? = null
    private var oldDispatchSlipItems: Array<DispatchSlipItem?>? = null
    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.dispatch_picking_list_fragment, container, false)

        this.recyclerView = rootView.findViewById(R.id.picking_dispatchSlipItems)
        recyclerView.layoutManager = LinearLayoutManager(this.activity)

        return rootView

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DispatchPickingListDetailsViewModel::class.java)

        (this.activity as AppCompatActivity).setTitle("Picking Dispatch Slip")

        if (this.arguments != null) {
            viewModel.dispatchSlipId = this.arguments!!.getInt("loadingDispatchSlip_id")
            viewModel.dispatchSlipVehicleNumber = this.arguments!!.getString("loadingDispatchSlip_vehicle_number")
            viewModel.dispatchSlipNumber = this.arguments!!.getString("loadingDispatchSlip_slipnumber")
            viewModel.dispatchSlipStatus = this.arguments!!.getString("loadingDispatchSlip_slipstatus")
            viewModel.dispatchSlipTruckId = this.arguments!!.getInt("loadingDispatchSlip_truckid")
            viewModel.customer = this.arguments!!.getString("loadingDispatchSlip_customer")


            picking_dispatchListId.text = viewModel.dispatchSlipNumber
            picking_dispatchListStatusId.text = viewModel.dispatchSlipStatus
            picking_truckNumber.text = viewModel.dispatchSlipVehicleNumber
            picking_dispatchListCustomer.text = viewModel.customer
        }

        recyclerView.adapter = SimpleDispatchSlipPickingItemAdapter(recyclerView, viewModel.dispatchPickingItems, viewModel)
        viewModel.dispatchPickingItems.observe(viewLifecycleOwner, Observer<Array<DispatchSlipItem?>>{
            if (it != null) {
                UiHelper.hideProgress(this.progress)
                this.progress = null
                if (viewModel.dispatchPickingItems.value.orEmpty().isNotEmpty() && viewModel.dispatchPickingItems.value?.first() == null) {
                    UiHelper.showSomethingWentWrongSnackbarMessage(this.activity as AppCompatActivity)
                    picking_scanned_count.text = "0/0"
                } else if (it != oldDispatchSlipItems) {
                    picking_dispatchSlipItems.adapter?.notifyDataSetChanged()
                    picking_scanned_count.text = viewModel.totalScannedItems.toString() + "/" + it.size.toString()
                }
            }

            picking_materialBarcode.text?.clear()
            picking_materialBarcode.requestFocus()

            oldDispatchSlipItems = viewModel.dispatchPickingItems.value
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
                    setMessage("Dispatch slip for picking operation submitted successfully.")
                    setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", { dialog, _ ->
                        dialog.dismiss()
//                        Navigation.findNavController(thisObject.recyclerView).popBackStack(R.id.homeFragment, false)
                        Navigation.findNavController(thisObject.recyclerView).popBackStack()
                    })
                    show()
                }
            }
        })



        picking_materialBarcode.setOnEditorActionListener { _, i, keyEvent ->
            var handled = false
            if ((picking_materialBarcode.text != null && picking_materialBarcode.text!!.isNotEmpty()) && i == EditorInfo.IME_ACTION_DONE
                    || (keyEvent != null && (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER || keyEvent.keyCode == KeyEvent.KEYCODE_TAB)
                            && keyEvent.action == KeyEvent.ACTION_DOWN)) {
                val keyboard = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                keyboard.hideSoftInputFromWindow(activity?.currentFocus?.getWindowToken(), 0)
                var value = picking_materialBarcode.text!!.toString().trim()
                var arguments = value.split("#")
                var productCode = ""
                var batchCode = ""
                var serialNumber = ""
                if (arguments.size < 3 || arguments[0].length == 0 || arguments[1].length == 0 || arguments[2].length == 0) {
                    UiHelper.showErrorToast(this.activity as AppCompatActivity, "Invalid barcode, please try again!")
                } else {
                    productCode = arguments[0].toString()
                    batchCode = arguments[1].toString()
                    serialNumber = arguments[2].toString()
                    if (viewModel.isMaterialBelongToSameGroup(productCode, batchCode)) {
                        if (viewModel.materialQuantityLoadingCompleted(productCode, batchCode)) {
                            UiHelper.showErrorToast(this.activity as AppCompatActivity, "For given batch and material, quantity is already picked for dispatch!")
                        } else {
                            if (viewModel.isSameSerialNumber(productCode, batchCode, serialNumber)) {
                                UiHelper.showErrorToast(this.activity as AppCompatActivity, "This barcode is already added, please add other item")
                            } else {
                                this.progress = UiHelper.showProgressIndicator(this.activity as AppCompatActivity, "Please wait")
                                // prodeed to add the material in database
                                GlobalScope.launch {
                                    viewModel.addMaterial(productCode, batchCode, serialNumber)
                                }
                            }
                        }
                    } else {
                        UiHelper.showErrorToast(this.activity as AppCompatActivity, "Scanned material batch and material is not matching with dispatch slip!")
                        // @dinesh gajjar: get admin permission flow
                    }
                }
                picking_materialBarcode.text?.clear()
                picking_materialBarcode.requestFocus()

                handled = true
            }
            handled
        }
        picking_items_submit_button.setOnClickListener({
            if (viewModel.dispatchPickingItems  != null && viewModel.dispatchPickingItems.value != null && viewModel.dispatchPickingItems.value!!.size > 0) {
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
                                    viewModel.handleSubmitPickingList()
                                }
                            })
                            show()
                        }
                    }
                } else {
                    UiHelper.showErrorToast(this.activity as AppCompatActivity, "Please submit the list when in Network!")
                }

                picking_materialBarcode.text?.clear()
                picking_materialBarcode.requestFocus()
            }


        })
        picking_scanButton.setOnClickListener({

        })


        this.progress = UiHelper.showProgressIndicator(activity!!, "Picking dispatch slip Items")
        viewModel.loadDispatchSlipPickingItems()

        picking_materialBarcode.requestFocus()
    }
}


//
//                    if (keyEvent == null) {
//                Log.d("materialDetailsScan: ", "event is null")
//            } else if ((picking_materialBarcode.text != null && picking_materialBarcode.text!!.isNotEmpty()) && i == EditorInfo.IME_ACTION_DONE || ((keyEvent.keyCode == KeyEvent.KEYCODE_ENTER || keyEvent.keyCode == KeyEvent.KEYCODE_TAB) && keyEvent.action == KeyEvent.ACTION_DOWN)) {
//                this.progress = UiHelper.showProgressIndicator(this.activity as AppCompatActivity, "Please wait")
//                UiHelper.hideProgress(this.progress)
//                this.progress = null
//                handled = true
//            }
//            handled
//        }

/*picking_items_submit_button.setOnClickListener({
    if (viewModel.dispatchPickingItems  != null && viewModel.dispatchPickingItems.value != null && viewModel.dispatchPickingItems.value!!.size > 0) {
        val keyboard = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        keyboard.hideSoftInputFromWindow(activity?.currentFocus?.getWindowToken(), 0)
        this.progress = UiHelper.showProgressIndicator(this.activity as AppCompatActivity, "Please wait")

        // viewModel.submitDispatchItems();

    } else {
        UiHelper.showToast(this.activity as AppCompatActivity, "No items for Picking")
    }

})*/





//        this.progress = UiHelper.showProgressIndicator(activity!!, "Picking dispatch slip Items")
//        viewModel.loadDispatchSlipPickingItems()
//    }
//}

open class SimpleDispatchSlipPickingItemAdapter(private val recyclerView: androidx.recyclerview.widget.RecyclerView,
                                                private val dispatchSlipItems: LiveData<Array<DispatchSlipItem?>>,
                                                private val viewModel: DispatchPickingListDetailsViewModel)
    : androidx.recyclerview.widget.RecyclerView.Adapter<SimpleDispatchSlipPickingItemAdapter.ViewHolder>() {

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
            var dbItems = viewModel.getItemsOfSameBatchProductCode(dispatchSlipItem.batchNumber!!,
                    dispatchSlipItem.materialCode!!)
            if (dbItems != null) {
                for (dbItem in dbItems!!.iterator()) {
                    var item = dbItem.batchCode + "#" + dbItem.productCode + "#" + dbItem.serialNumber
                    list.add(item)
                }
            }

//    override fun onBindViewHolder(holder: SimpleDispatchSlipLoadingItemAdapter.ViewHolder, position: Int) {
//        holder.bind()
//
//        val dispatchSlipItem = dispatchSlipItems.value!![position]!!
//        holder.itemView.setOnClickListener {
//
//            if (viewModel.dispatchSlipStatus.toString().toLowerCase().contains("complete")) {
//                return@setOnClickListener
//            }
//
//            val list = mutableListOf<String>()
//            var dbItems = viewModel.getItemsOfSameBatchProductCode(dispatchSlipItem.batchNumber!!, dispatchSlipItem.materialCode!!)
//            if (dbItems != null) {
//                for (dbItem in dbItems!!.iterator()) {
//                    var item = dbItem.batchCode + "#" + dbItem.productCode + "#" + dbItem.serialNumber
//                    list.add(item)
//                }
//            }

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
            dispatchSlipItemMaterialDetails = itemView.findViewById(R.id.dispatch_slip_item_material_product_details)
            dispatchSlipItemPackQuantity = itemView.findViewById(R.id.dispatch_slip_item_material_pack_quantity)
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
        }
    }
}
