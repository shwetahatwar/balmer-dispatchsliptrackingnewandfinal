package com.briot.balmerlawrie.implementor.data

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Date
import java.util.*

@Entity(tableName = "audit_project_list_item")
public data class DBProjectItem(

    @PrimaryKey(autoGenerate = true) var id: Int,
    @ColumnInfo(name = "serialNumber") val serialNumber: String?,
    @ColumnInfo(name = "batchCode") val batchCode: String?,
    @ColumnInfo(name = "productCode") val productCode: String?,
    @ColumnInfo(name = "itemStatus") val itemStatus: String?,
    @ColumnInfo(name = "dispatchSlipId") val dispatchSlipId: Int,
    @ColumnInfo(name = "projectId") val projectId: String?,
    @ColumnInfo(name = "timestamp") val timeStamp: Long,
    @ColumnInfo(name = "submittedOn") val submittedOn: Long,
    @ColumnInfo(name = "user") val user: String?,
    @ColumnInfo(name = "submitted") val submitted: Int
)
