package com.briot.balmerlawrie.implementor.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DispatchSlipPickingListItemDao {

    @Query("SELECT * from dispatchslip_picking_list_item ORDER BY serialNumber ASC")
    fun getAllItems(): List<DispatchSlipPickingListItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: DispatchSlipPickingListItem)

    @Query("SELECT * FROM dispatchslip_picking_list_item WHERE dispatchSlipId = :dispatchSlipId ORDER BY timestamp ASC")
    fun getAllDispatchSlipItems(dispatchSlipId: Int): List<DispatchSlipPickingListItem>

    @Query("SELECT COUNt (*) FROM dispatchslip_picking_list_item WHERE dispatchSlipId = :dispatchSlipId ORDER BY timestamp ASC")
    fun getAllDispatchSlipItemsCount(dispatchSlipId: Int): Int

    @Query("SELECT COUNT (*) FROM dispatchslip_picking_list_item WHERE dispatchSlipId = :dispatchSlipId AND productCode LIKE :materialCode AND batchCode LIKE :batchNumber ORDER BY timestamp ASC")
    fun getCountForBatchMaterialCode(dispatchSlipId: Int, materialCode: String, batchNumber: String): Int

    @Query("SELECT COUNT (*) FROM dispatchslip_picking_list_item WHERE dispatchSlipId = :dispatchSlipId AND productCode LIKE :materialCode AND batchCode LIKE :batchNumber AND serialNumber LIKE :serialNumber ORDER BY timestamp ASC")
    fun getCountForBatchMaterialCodeSerial(dispatchSlipId: Int, materialCode: String, batchNumber: String, serialNumber: String): Int

    @Query("SELECT * FROM dispatchslip_picking_list_item WHERE dispatchSlipId = :dispatchSlipId AND batchCode LIKE :batchNumber ORDER BY timestamp ASC")
    fun getItemsForBatch(dispatchSlipId: Int, batchNumber: String): List<DispatchSlipPickingListItem>

    @Query("SELECT * FROM dispatchslip_picking_list_item WHERE dispatchSlipId = :dispatchSlipId AND productCode LIKE :materialCode AND batchCode LIKE :batchNumber ORDER BY timestamp ASC")
    fun getItemsForBatchMaterialCode(dispatchSlipId: Int, materialCode: String, batchNumber: String): List<DispatchSlipPickingListItem>

    @Query("SELECT * FROM dispatchslip_picking_list_item WHERE dispatchSlipId = :dispatchSlipId AND submitted = 1 ORDER BY timestamp ASC")
    fun getSubmitedDispatchDetails(dispatchSlipId: Int): List<DispatchSlipPickingListItem>

    @Query("UPDATE dispatchslip_picking_list_item SET submitted = 1, submittedOn = :timestamp WHERE dispatchSlipId LIKE :dispatchSlipId")
    suspend fun updateSubmittedStatus(dispatchSlipId: String,  timestamp: Long)

    @Query("DELETE from dispatchslip_picking_list_item WHERE dispatchSlipId = :dispatchSlipId")
    suspend fun deleteForSelectedDispatchSlip(dispatchSlipId: String)

    @Query("DELETE from dispatchslip_picking_list_item WHERE dispatchSlipId = :dispatchSlipId AND serialNumber LIKE :serialNumber")
    suspend fun deleteForSelectedDispatchSlipSerialNumber(dispatchSlipId: Int, serialNumber: String)

    @Query("DELETE from dispatchslip_picking_list_item WHERE dispatchSlipId = :dispatchSlipId AND productCode LIKE :materialCode AND batchCode LIKE :batchNumber AND serialNumber LIKE :serialNumber")
    suspend fun deleteSelectedItem(dispatchSlipId: Int, materialCode: String, batchNumber: String, serialNumber: String)

    @Query("DELETE from dispatchslip_picking_list_item")
    suspend fun deleteAll()
}