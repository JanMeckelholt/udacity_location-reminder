package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainDispatcherRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.mockito.Mockito

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainDispatcherRule = MainDispatcherRule()

    // Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    private val validReminder = ReminderDataItem("validTitle", "validDesc", "validLoc", 12.0, -33.4, "validUuid")
    private val invalidReminder = ReminderDataItem("invalidTitle", null, "invalidLoc", 12.0, -33.4, "invalidUuid")



    @Before
    fun setupSaveReminderViewModel() {
        val application = Mockito.mock(Application::class.java)
        val testReminder1 = ReminderDTO("title1", "desc1", "loc1", 38.0, -111.33, "uuid1")
        val testReminder2 = ReminderDTO("title2", "desc2", "loc1", 18.0, -120.33, "uuid2")
        val fakeDataSource = FakeDataSource(mutableListOf(testReminder1, testReminder2))
        saveReminderViewModel = SaveReminderViewModel(application, fakeDataSource)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun loadTasks_loading() = runTest() {
        saveReminderViewModel.validateAndSaveReminder(validReminder, false)
        Assert.assertTrue(saveReminderViewModel.showLoading.getOrAwaitValue())
        runCurrent()
        Assert.assertFalse(saveReminderViewModel.showLoading.getOrAwaitValue())
    }

    @Test
    fun saveReminder_callErrorToDisplay() = runTest() {
        saveReminderViewModel.validateAndSaveReminder(invalidReminder, false)
        runCurrent()
        Assert.assertNotNull(saveReminderViewModel.showErrorMessage)
    }
}


