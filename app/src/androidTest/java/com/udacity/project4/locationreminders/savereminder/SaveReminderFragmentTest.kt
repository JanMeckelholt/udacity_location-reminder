package com.udacity.project4.locationreminders.savereminder

import FakeAndroidTestRepository
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.mockito.Mockito
import org.mockito.Mockito.verify



@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class SaveReminderFragmentTest : KoinTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    @Before
    fun init() {
        stopKoin()
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(
                module {
                    viewModel {
                        SaveReminderViewModel(
                            get(),
                            get() as ReminderDataSource
                        )
                    }
                    single { FakeAndroidTestRepository() as ReminderDataSource }
                })
        }

    }

    @After
    fun cleanupDb() = runTest {
        stopKoin()
    }

    @Test
    fun reminder_displayedInUi() = runTest {
        launchFragmentInContainer<SaveReminderFragment>(null, R.style.AppTheme)
        Espresso.onView(ViewMatchers.withId(R.id.saveReminder)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.reminderTitle)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.reminderDescription)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.selectLocation)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun clickSelectLocation_navigateToSelectReminderFragment() = runTest {
        val scenario = launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        Espresso.onView(ViewMatchers.withId(R.id.selectLocation))
            .perform(ViewActions.click())
        verify(navController).navigate(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
    }
}