package com.example.data

import kotlinx.coroutines.flow.Flow

class TimeRepository(private val timeDao: TimeDao) {

    val allActivities: Flow<List<DailyActivity>> = timeDao.getAllActivities()
    val allAllocations: Flow<List<Allocation>> = timeDao.getAllAllocations()
    val latestInsight: Flow<TimeInsight?> = timeDao.getLatestInsight()

    fun getActivitiesForDay(day: String): Flow<List<DailyActivity>> {
        return timeDao.getActivitiesForDay(day)
    }

    suspend fun insertActivity(activity: DailyActivity) {
        timeDao.insertActivity(activity)
    }

    suspend fun deleteActivity(activity: DailyActivity) {
        timeDao.deleteActivity(activity)
    }

    suspend fun insertAllocation(allocation: Allocation) {
        timeDao.insertAllocation(allocation)
    }

    suspend fun updateAllocationCompletion(id: Int, completed: Boolean) {
        timeDao.updateAllocationCompletion(id, completed)
    }

    suspend fun clearAllData() {
        timeDao.clearAllActivities()
        timeDao.clearAllAllocations()
        timeDao.clearAllInsights()
    }

    suspend fun clearAllocations() {
        timeDao.clearAllAllocations()
    }

    suspend fun insertInsight(insight: TimeInsight) {
        timeDao.insertInsight(insight)
    }
}
