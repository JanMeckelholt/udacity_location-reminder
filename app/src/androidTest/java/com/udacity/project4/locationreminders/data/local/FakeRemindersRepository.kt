import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.asDomainModell
import com.udacity.project4.locationreminders.reminderslist.asDTO

class FakeAndroidTestRepository : ReminderDataSource {

    var reminderDataMap: LinkedHashMap<String, ReminderDataItem> = LinkedHashMap()
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return Result.Success(reminderDataMap.values.map { it.asDTO() }.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminderDataMap[reminder.id] = reminder.asDomainModell()
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        reminderDataMap[id]?.let {
            return Result.Success(ReminderDTO(it.title,it.description,it.location,it.latitude,it.longitude, it.id))
        }
        return Result.Error("Could not find reminder with id $id", null)
    }

    override suspend fun deleteAllReminders() {
        reminderDataMap.clear()
    }


}