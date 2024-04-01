package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(private var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        reminders?.let {
            return Result.Success(ArrayList(it))
        }
        return Result.Error("Tasks not found", null)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        if (reminders == null) {
            reminders = mutableListOf(reminder)
        } else {
            reminders?.add(reminder)
        }
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val found = reminders?.filter { it.id == id }
        return if (found.isNullOrEmpty()){
            Result.Error("reminder with id $id not found", null)
        } else Result.Success(found[0])
    }

    override suspend fun deleteAllReminders() {
        reminders = null
    }
}