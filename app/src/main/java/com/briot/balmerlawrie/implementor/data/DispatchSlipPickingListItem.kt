package com.briot.balmerlawrie.implementor.data

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Date
import java.util.*

@Entity(tableName = "dispatchslip_picking_list_item")
public data class DispatchSlipPickingListItem(

    @PrimaryKey(autoGenerate = true) var id: Int,
    @ColumnInfo(name = "serialNumber") val serialNumber: String?,
    @ColumnInfo(name = "batchCode") val batchCode: String?,
    @ColumnInfo(name = "productCode") val productCode: String?,
    @ColumnInfo(name = "genericName") val genericName: String?,
    @ColumnInfo(name = "materialDescription") val materialDescription: String?,
    @ColumnInfo(name = "timestamp") val timeStamp: Long,
    @ColumnInfo(name = "dispatchSlipId") val dispatchSlipId: Int,
    @ColumnInfo(name = "dipatchSlipNumber") val dipatchSlipNumber: String?,
    @ColumnInfo(name = "vehicleNumber") val vehicleNumber: String?,
    @ColumnInfo(name = "submitted") val submitted: Int,
    @ColumnInfo(name = "user") val user: String?,
    @ColumnInfo(name = "submittedOn") val submittedOn: Long
)
