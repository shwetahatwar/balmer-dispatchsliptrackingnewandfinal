package com.briot.balmerlawrie.implementor.ui.main


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.briot.balmerlawrie.implementor.R
import com.briot.balmerlawrie.implementor.UiHelper
import com.briot.balmerlawrie.implementor.repository.local.PrefConstants
import com.briot.balmerlawrie.implementor.repository.local.PrefRepository
import com.briot.balmerlawrie.implementor.repository.remote.DispatchSlip
import io.github.pierry.progress.Progress
import kotlinx.android.synthetic.main.dispatch_picking_lists_fragment.*
import java.text.SimpleDateFormat
import java.util.*

class DispatchPickingListsFragment : Fragment() {

    companion object {
        fun newInstance() = DispatchPickingListsFragment()
    }

    private lateinit var viewModel: DispatchPickingListsViewModel
    private var progress: Progress? = null
    private var oldDispatchSlipList: Array<DispatchSlip?>? = null
    lateinit var recyclerView: RecyclerView
    private var userId = PrefRepository.singleInstance.getValueOrDefault(PrefConstants().USER_ID, "0").toInt()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.dispatch_picking_lists_fragment, container, false)

        this.recyclerView = rootView.findViewById(R.id.picking_dispatchSlipsView)
        recyclerView.layoutManager = LinearLayoutManager(this.activity)
        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DispatchPickingListsViewModel::class.java)

        (this.activity as AppCompatActivity).setTitle("Picking Dispatch Slips")

        recyclerView.adapter = SimpleAdapter(recyclerView, viewModel.dispatchPickerList, viewModel)

        viewModel.dispatchPickerList.observe(viewLifecycleOwner, Observer<Array<DispatchSlip?>> {
            if (it != null) {
                UiHelper.hideProgress(this.progress)
                this.progress = null

                if (viewModel.dispatchPickerList.value.orEmpty().isNotEmpty() && viewModel.dispatchPickerList.value?.first() == null) {
                    UiHelper.showSomethingWentWrongSnackbarMessage(this.activity as AppCompatActivity)
                } else if (it != oldDispatchSlipList) {
                    picking_dispatchSlipsView.adapter?.notifyDataSetChanged()
                }
            }

            oldDispatchSlipList = viewModel.dispatchPickerList.value
        })

        viewModel.networkError.observe(viewLifecycleOwner, Observer<Boolean> {
            if (it == true) {
                UiHelper.hideProgress(this.progress)
                this.progress = null

                UiHelper.showNoInternetSnackbarMessage(this.activity as AppCompatActivity)
            }
        })

        this.progress = UiHelper.showProgressIndicator(activity!!, "Loading picking list")
        viewModel.loadDispatchPickingLists(userId)
    }

}
open class SimpleAdapter(private val recyclerView: androidx.recyclerview.widget.RecyclerView, private val dispatchSlips:
LiveData<Array<DispatchSlip?>>, viewModel: DispatchPickingListsViewModel) : androidx.recyclerview.widget.RecyclerView.Adapter<SimpleAdapter.ViewHolder>() {

    private var viewModel: DispatchPickingListsViewModel = viewModel
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.dispatch_list_picking_row_item, parent, false)

        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()

        val dispatchSlip = dispatchSlips.value!![position]!!
        holder.itemView.setOnClickListener{
            val bundle = Bundle()
            if (dispatchSlip.id != null) {
                bundle.putInt("loadingDispatchSlip_id", dispatchSlip.id!!.toInt())
            }

            if (dispatchSlip.dispatchSlipNumber != null) {
                bundle.putString("loadingDispatchSlip_slipnumber", dispatchSlip.dispatchSlipNumber!!)
            }

            if (dispatchSlip.dispatchSlipStatus != null) {
                bundle.putString("loadingDispatchSlip_slipstatus", dispatchSlip.dispatchSlipStatus!!)
            }

            if (dispatchSlip.ttat != null && dispatchSlip.ttat!!.truckNumber !=  null) {
                bundle.putString("loadingDispatchSlip_vehicle_number", dispatchSlip.ttat!!.truckNumber!!)
            }

            if (dispatchSlip.truckId != null) {
                bundle.putInt("loadingDispatchSlip_truckid", dispatchSlip.truckId!!.toInt())
            }
            //my changes
            if (dispatchSlip.depot != null && dispatchSlip.depot!!.name != null) {
                bundle.putString("loadingDispatchSlip_customer", dispatchSlip.depot!!.name)
            }
            //end


            Navigation.findNavController(it).navigate(R.id.action_dispatchPickingListsFragment_to_dispatchPickingListFragment, bundle)
        }
    }

    override fun getItemCount(): Int {
        return dispatchSlips.value?.size ?: 0
    }

    open inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        protected val linearLayout: LinearLayout
        protected val dispatchSlipId: TextView
        protected val dispatchSlipTruckNumber: TextView
        protected val dispatchSlipDriverName: TextView
        protected val dispatchSlipDepotName: TextView
        protected val dispatchSlipDepotLocation: TextView
        protected val dispatchSlipDepotCreatedOn: TextView

        init {
            dispatchSlipId = itemView.findViewById(R.id.dispatch_list_row_title)
            dispatchSlipTruckNumber = itemView.findViewById(R.id.dispatch_list_row_vehicle_number)
            dispatchSlipDriverName = itemView.findViewById(R.id.dispatch_list_row_driver_name)
            dispatchSlipDepotName = itemView.findViewById(R.id.dispatch_list_row_depot_name)
            dispatchSlipDepotLocation = itemView.findViewById(R.id.dispatch_list_row_depot_location)
            dispatchSlipDepotCreatedOn = itemView.findViewById(R.id.dispatch_list_row_creation_date)
            linearLayout = itemView.findViewById(R.id.dispatch_slips_row_layout)
        }

        fun bind() {
            val dispatchSlip = dispatchSlips.value!![adapterPosition]!!

            dispatchSlipId.text = dispatchSlip.dispatchSlipNumber

            if (dispatchSlip.ttat != null)  {
                dispatchSlipTruckNumber.text  = dispatchSlip.ttat!!.truckNumber
                dispatchSlipDriverName.text = dispatchSlip.ttat!!.driver
            }
            if (dispatchSlip.depot != null) {
                dispatchSlipDepotName.text = dispatchSlip.depot!!.name
                dispatchSlipDepotLocation.text = dispatchSlip.depot!!.location
            }
            if (dispatchSlip.createdAt !=  null) {
                val value = dispatchSlip.createdAt!!
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                val output = SimpleDateFormat("dd/MM/yyyy hh:mm a")
                parser.setTimeZone(TimeZone.getTimeZone("IST"))
                val result =  parser.parse(value)
                dispatchSlipDepotCreatedOn.text = output.format(result)
            }
            if (dispatchSlip.dispatchSlipStatus != null) {
                if (dispatchSlip.dispatchSlipStatus!!.toLowerCase().contains("progress")) {
                    linearLayout.setBackgroundColor(PrefConstants().lightOrangeColor)
                } else if (dispatchSlip.dispatchSlipStatus!!.toLowerCase().contains("complete") ||
                        dispatchSlip.dispatchSlipStatus!!.toLowerCase().contains("picked" ) ) {
                    linearLayout.setBackgroundColor(PrefConstants().lightGreenColor)
                } else if (dispatchSlip.id != null && viewModel.isDispatchSlipInProgress(dispatchSlip.id!!.toInt())) {
                    linearLayout.setBackgroundColor(PrefConstants().lightOrangeColor)
                } else if (dispatchSlip.dispatchSlipStatus!!.toLowerCase().contains("active")) {
                    linearLayout.setBackgroundColor(PrefConstants().lightGrayColor)
                } else {
                    linearLayout.setBackgroundColor(PrefConstants().lightGrayColor)
                }
            } else if (dispatchSlip.id != null && viewModel.isDispatchSlipInProgress(dispatchSlip.id!!.toInt())) {
                linearLayout.setBackgroundColor(PrefConstants().lightOrangeColor)
            }
        }
    }
}