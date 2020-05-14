package com.briot.balmerlawrie.implementor.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DBProjectItemDao {

    @Query("SELECT * from audit_project_list_item ORDER BY serialNumber ASC")
    fun getAllItems(): LiveData<List<DispatchSlipPickingListItem>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: DBProjectItem)

    @Query("SELECT * FROM audit_project_list_item WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getProjectItems(projectId: Int): List<DBProjectItem>

    @Query("SELECT * FROM audit_project_list_item WHERE projectId = :projectId AND batchCode LIKE :batchNumber ORDER BY timestamp ASC")
    fun getItemsForBatch(projectId: Int, batchNumber: String): LiveData<List<DispatchSlipPickingListItem>>

    @Query("UPDATE audit_project_list_item SET submittedOn = 1, timestamp = :timestamp LIKE :projectId")
    suspend fun updateSubmittedStatus(projectId: Int,  timestamp: Long)

    @Query("DELETE from audit_project_list_item WHERE projectId = :projectId")
    suspend fun deleteSelectedProject(projectId: Int)

    @Query("DELETE from audit_project_list_item")
    suspend fun deleteAll()
}