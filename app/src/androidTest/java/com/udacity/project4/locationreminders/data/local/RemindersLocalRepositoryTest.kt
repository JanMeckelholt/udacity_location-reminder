package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var localRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    val testReminder1 = ReminderDTO("title1", "desc1", "loc1", 38.0, -111.33, "uuid1")
    val testReminder2 = ReminderDTO("title2", "desc2", "loc1", 18.0, -120.33, "uuid2")


    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        localRepository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun saveReminder_retrievesReminder() = runTest {
        localRepository.saveReminder(testReminder1)
        val result = localRepository.getReminder(testReminder1.id)
        runCurrent()
        Assert.assertTrue(result is Result.Success)
        val resultData = (result as Result.Success).data
        Assert.assertNotNull(resultData)
        Assert.assertEquals(testReminder1, resultData)
    }

    @Test
    fun saveReminders_getReminders() = runTest {
        localRepository.saveReminder(testReminder1)
        localRepository.saveReminder(testReminder2)
        val result = localRepository.getReminders()
        runCurrent()
        Assert.assertTrue(result is Result.Success)
        val resultData = (result as Result.Success).data
        Assert.assertNotNull(resultData)
        Assert.assertEquals(2, resultData.size)
        Assert.assertEquals(testReminder1, resultData[0])
        Assert.assertEquals(testReminder2, resultData[1])
    }

    @Test
    fun SaveAndDeleteAllReminders_retrieveEmptyList() = runTest {
        localRepository.saveReminder(testReminder1)
        localRepository.saveReminder(testReminder2)
        val resultBefore = localRepository.getReminders()
        runCurrent()
        Assert.assertTrue(resultBefore is Result.Success)
        val resultDataBefore = (resultBefore as Result.Success).data
        Assert.assertNotNull(resultDataBefore)
        Assert.assertEquals(2, resultDataBefore.size)
        localRepository.deleteAllReminders()
        val resultAfter = localRepository.getReminders()
        runCurrent()
        Assert.assertTrue(resultAfter is Result.Success)
        val resultDataAfter = (resultAfter as Result.Success).data
        Assert.assertNotNull(resultDataAfter)
        Assert.assertEquals(0, resultDataAfter.size)
    }
}