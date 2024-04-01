package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    val testReminder1 = ReminderDTO("title1", "desc1", "loc1", 38.0, -111.33, "uuid1")
    val testReminder2 = ReminderDTO("title2", "desc2", "loc1", 18.0, -120.33, "uuid2")

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertReminderAndGetById() = runTest {

        database.reminderDao().saveReminder(testReminder1)
        runCurrent()
        val loaded = database.reminderDao().getReminderById(testReminder1.id)

        Assert.assertNotNull(loaded as ReminderDTO)
        Assert.assertEquals(testReminder1.id, loaded.id)
        Assert.assertEquals(testReminder1.title, loaded.title)
        Assert.assertEquals(testReminder1.description, loaded.description)
        Assert.assertEquals(testReminder1.latitude, loaded.latitude)
        Assert.assertEquals(testReminder1.longitude, loaded.longitude)
    }


    @Test
    fun insertAndGetReminders() = runTest {
        database.reminderDao().saveReminder(testReminder1)
        database.reminderDao().saveReminder(testReminder2)
        runCurrent()
        val loaded = database.reminderDao().getReminders()
        Assert.assertNotNull(loaded)
        Assert.assertEquals(2, loaded.size)
        Assert.assertEquals(testReminder1, loaded[0])
        Assert.assertEquals(testReminder2, loaded[1])
    }

    @Test
    fun insertAndDeleteAllReminders() = runTest {
        database.reminderDao().saveReminder(testReminder1)
        database.reminderDao().saveReminder(testReminder2)
        runCurrent()
        var loaded = database.reminderDao().getReminders()
        Assert.assertNotNull(loaded)
        Assert.assertEquals(2, loaded.size)
        database.reminderDao().deleteAllReminders()
        loaded = database.reminderDao().getReminders()
        runCurrent()
        Assert.assertNotNull(loaded)
        Assert.assertEquals(0, loaded.size)
    }
}