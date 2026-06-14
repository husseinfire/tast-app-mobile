package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "daily_activities")
data class DailyActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayOfWeek: String, // "Monday", "Tuesday", etc.
    val name: String,      // e.g., "Work", "Sleep", "Social Media", "Gaming"
    val startTime: String, // "09:00"
    val endTime: String,   // "17:00"
    val category: String   // "Sleep", "Work/Study", "Leisure", "Chores", "Other"
)

@Entity(tableName = "time_allocations")
data class Allocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayOfWeek: String,
    val activityName: String,
    val startTime: String,
    val endTime: String,
    val type: String, // "STUDY" or "READING"
    val isCompleted: Boolean = false
)

@Entity(tableName = "time_insights")
data class TimeInsight(
    @PrimaryKey val id: Int = 1, // Store a single modern analysis
    val analysisText: String,
    val reclaimedWeeklyHours: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TimeDao {
    @Query("SELECT * FROM daily_activities ORDER BY id ASC")
    fun getAllActivities(): Flow<List<DailyActivity>>

    @Query("SELECT * FROM daily_activities WHERE dayOfWeek = :day")
    fun getActivitiesForDay(day: String): Flow<List<DailyActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: DailyActivity)

    @Delete
    suspend fun deleteActivity(activity: DailyActivity)

    @Query("DELETE FROM daily_activities")
    suspend fun clearAllActivities()

    @Query("SELECT * FROM time_allocations ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllAllocations(): Flow<List<Allocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocation(allocation: Allocation)

    @Query("UPDATE time_allocations SET isCompleted = :completed WHERE id = :id")
    suspend fun updateAllocationCompletion(id: Int, completed: Boolean)

    @Query("DELETE FROM time_allocations")
    suspend fun clearAllAllocations()

    @Query("SELECT * FROM time_insights WHERE id = 1 LIMIT 1")
    fun getLatestInsight(): Flow<TimeInsight?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: TimeInsight)

    @Query("DELETE FROM time_insights")
    suspend fun clearAllInsights()
}

@Database(entities = [DailyActivity::class, Allocation::class, TimeInsight::class], version = 1, exportSchema = false)
abstract class TimeDatabase : RoomDatabase() {
    abstract fun timeDao(): TimeDao
}
