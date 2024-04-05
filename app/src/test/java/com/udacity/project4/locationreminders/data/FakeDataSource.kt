package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(private var reminders: MutableList<ReminderDTO> = mutableListOf()) :
    ReminderDataSource {
    private var shouldReturnError = false
    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Test Exception")
        }
        return Result.Success(ArrayList(reminders))
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
            reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Test Exception")
        }
        val found = reminders.filter { it.id == id }
        return if (found.isEmpty()) {
            Result.Error("reminder with id $id not found", null)
        } else Result.Success(found[0])
    }

    override suspend fun deleteAllReminders() {
        reminders = mutableListOf()
    }
}