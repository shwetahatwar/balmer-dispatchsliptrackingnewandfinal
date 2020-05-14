package com.briot.balmerlawrie.implementor.ui.main

import android.content.ContentValues.TAG
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.LiveData
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.briot.balmerlawrie.implementor.R
import com.briot.balmerlawrie.implementor.UiHelper
import com.briot.balmerlawrie.implementor.repository.local.PrefConstants
import com.briot.balmerlawrie.implementor.repository.remote.Project
import com.briot.balmerlawrie.implementor.repository.remote.auditProjectItem
import com.briot.balmerlawrie.implementor.repository.remote.auditProjectList
import kotlinx.android.synthetic.main.audit_project_list_fragment.*
import kotlinx.android.synthetic.main.audit_project_row.*
import kotlinx.android.synthetic.main.dispatch_slip_loading_fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.Text

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.pierry.progress.Progress

class AuditProjectList : Fragment() {

    companion object {
        fun newInstance() = AuditProjectList()
    }

    private lateinit var viewModel: AuditProjectListViewModel
    private lateinit var viewModelAudit: AuditProjectsViewModel
    lateinit var recyclerView: RecyclerView
    private var progress: Progress? = null
    lateinit var auditsubmit: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.audit_project_list_fragment, container, false)
        this.recyclerView = rootView.findViewById(R.id.auditprojects_projectlist)
        recyclerView.layoutManager = LinearLayoutManager(this.activity)
        auditsubmit = rootView.findViewById(R.id.audit_items_submit_button)
        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(AuditProjectListViewModel::class.java)
//        viewModelAudit = ViewModelProviders.of(this).get(AuditProjectsViewModel::class.java)

        viewModel.loadAuditProjects("In Progress")
        audit_materialBarcode.requestFocus()

        if (this.arguments != null) {
            viewModel.projectId = this.arguments!!.getInt("projectId")
        }
        // Log.d(TAG, "viewModelAudit -->"+viewModel.projectId)
        viewModel.updatedListAsPerDatabase(viewModel.projectId)


        audit_scanButton.setOnClickListener {
            viewModel.serialNumber = audit_materialBarcode.getText().toString()
            var thisObject = this
            var value = audit_materialBarcode.text!!.toString().trim()
            var arguments = value.split("#")
            var found: Boolean = true
            if (arguments.size < 3 || arguments[0].length == 0 || arguments[1].length == 0 || arguments[2].length == 0) {
                UiHelper.showErrorToast(this.activity as AppCompatActivity, "Please Enter barcode")
            } else{
                var materialBarcode = arguments[0].toString()
                var productCode = arguments[1].toString()
                var serialNumber = value
                for (auditItem in viewModel.auditProjectListItems!!.value!!){
                    if (auditItem!!.serialNumber == serialNumber && auditItem!!.batchCode == productCode && auditItem.productCode == materialBarcode){
                        UiHelper.showErrorToast(this.activity as AppCompatActivity, "Already scanned")
                        found = false
                        break
                    }
                }
                if (found) {
                    GlobalScope.launch {
                        viewModel.addMaterial(materialBarcode, productCode, serialNumber, viewModel.projectId)
                    }
                }
                recyclerView.adapter = SimpleAuditItemAdapter(recyclerView, viewModel.auditProjectListItems, viewModel)
            }
            audit_materialBarcode.text?.clear()

        };

        viewModel.itemSubmissionSuccessful.observe(viewLifecycleOwner, Observer<Boolean> {
            if (it == true) {
                UiHelper.hideProgress(this.progress)
                this.progress = null
                var thisObject = this
                UiHelper.showSuccessToast(this.activity as AppCompatActivity,
                        "Updated successfully")
            }
        })

        audit_items_submit_button.setOnClickListener {
            var auditProjectItems = emptyArray<auditProjectItem>()

            for (items in viewModel.auditProjectListItems!!.value!!) {
                //Making list to display and postcall
                var auditProjectItem = auditProjectItem()
                auditProjectItem.projectId = items!!.projectId
                auditProjectItem.serialNumber = items!!.serialNumber
                auditProjectItems = auditProjectItems + auditProjectItem
            }
            if (auditProjectItems.size == 0){
                UiHelper.showErrorToast(this.activity as AppCompatActivity,
                        "Items not avialable")
            } else {
                viewModel.updateAuditProjects(auditProjectItems)
            }
        }
            recyclerView.adapter = SimpleAuditItemAdapter(recyclerView, viewModel.auditProjectListItems, viewModel)
        }


    open class SimpleAuditItemAdapter(private val recyclerView: androidx.recyclerview.widget.RecyclerView,
                                      private val auditProjectListItems: LiveData<Array<auditProjectList?>>,
                   private val viewModel: AuditProjectListViewModel):
            androidx.recyclerview.widget.RecyclerView.Adapter<SimpleAuditItemAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleAuditItemAdapter.ViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.audit_project_row, parent, false)
            return ViewHolder(itemView)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind()
            val auditProjectItem = auditProjectListItems.value!![position]
            holder.itemView.setOnClickListener{
                    Log.d(TAG, "on bind")
            }
        }
        override fun getItemCount(): Int {
//            Log.d(TAG, "inside get count "+ auditProjectListItems.value?.size)
            return auditProjectListItems.value?.size ?: 0
        }

        open inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            protected val material_barcode: TextView
            protected val batch_number: TextView
            protected val barcode_serial: TextView
            init {
                material_barcode = itemView.findViewById(R.id.material_barcode)
                batch_number = itemView.findViewById(R.id.batch_number)
                barcode_serial = itemView.findViewById(R.id.barcode_serial)
            }
            fun bind() {
                val auditItems = auditProjectListItems.value!![adapterPosition]!!
                material_barcode.text = auditItems.productCode
                batch_number.text = auditItems.batchCode
                barcode_serial.text = auditItems.serialNumber
            }
        }
    }
}



