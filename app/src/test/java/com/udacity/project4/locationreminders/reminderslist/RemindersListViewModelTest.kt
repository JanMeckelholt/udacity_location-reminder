package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainDispatcherRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.asDomainModell
import com.udacity.project4.locationreminders.getOrAwaitValue
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

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainDispatcherRule = MainDispatcherRule()

    // Subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var fakeDataSource: FakeDataSource

    private val testReminder1 = ReminderDTO("title1", "desc1", "loc1", 38.0, -111.33, "uuid1")
    private val testReminder2 = ReminderDTO("title2", "desc2", "loc1", 18.0, -120.33, "uuid2")

    @Before
    fun setupRemindersListViewModel() {
        val application = Mockito.mock(Application::class.java)
        fakeDataSource = FakeDataSource(mutableListOf(testReminder1, testReminder2))
        remindersListViewModel = RemindersListViewModel(application, fakeDataSource)
        remindersListViewModel.showSnackBar.value = null
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun loadReminders_loading() = runTest() {
        remindersListViewModel.loadReminders()
        Assert.assertTrue(remindersListViewModel.showLoading.getOrAwaitValue())
        runCurrent()
        Assert.assertFalse(remindersListViewModel.showLoading.getOrAwaitValue())
    }

    @Test
    fun loadReminders_returnsError() = runTest() {
        fakeDataSource.setReturnError(true)
        remindersListViewModel.loadReminders()
        runCurrent()
        Assert.assertNotNull(remindersListViewModel.showSnackBar.getOrAwaitValue())
        Assert.assertEquals("Test Exception",remindersListViewModel.showSnackBar.getOrAwaitValue())
    }

    @Test
    fun loadReminders_RemindersList() = runTest() {
        remindersListViewModel.loadReminders()
        runCurrent()
        Assert.assertNotNull(remindersListViewModel.remindersList.value)
        Assert.assertEquals(2, remindersListViewModel.remindersList.value!!.size)
        Assert.assertEquals(testReminder1.asDomainModell(), remindersListViewModel.remindersList.value!![0])
        Assert.assertEquals(testReminder2.asDomainModell(), remindersListViewModel.remindersList.value!![1])
    }

}