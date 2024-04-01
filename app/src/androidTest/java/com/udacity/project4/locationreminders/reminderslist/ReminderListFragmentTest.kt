package com.udacity.project4.locationreminders.reminderslist

import FakeAndroidTestRepository
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.GrantPermissionRule
import androidx.test.rule.GrantPermissionRule.grant
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
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

import org.koin.test.inject
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : KoinTest {

//    TODO: test the navigation of the fragments.
//    TODO: test the displayed data on the UI.
//    TODO: add testing for the error messages.
companion object{
    val permissions = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )
}

    @JvmField
    @Rule
    val permissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            grant(*permissions, android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            grant(*permissions)
        }

    val repository : ReminderDataSource by inject()

    val testReminder1 = ReminderDTO("title1", "desc1", "loc1", 38.0, -111.33, "uuid1")
    val testReminder2 = ReminderDTO("title2", "desc2", "loc1", 18.0, -120.33, "uuid2")

    @Before
    fun initRepository() {
        stopKoin()
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(
                module {
                    viewModel {
                        RemindersListViewModel(
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
    fun reminderList_displayedInUi() = runTest {
        repository.saveReminder(testReminder1)
        repository.saveReminder(testReminder2)
        launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
        onView(withId(R.id.addReminderFAB)).check(ViewAssertions.matches(isDisplayed()))
        onView(withId(R.id.reminderssRecyclerView)).check(ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText(testReminder1.title))))
        onView(withId(R.id.reminderssRecyclerView)).check(ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText(testReminder1.description))))
        onView(withId(R.id.reminderssRecyclerView)).check(ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText(testReminder2.title))))
        onView(withId(R.id.reminderssRecyclerView)).check(ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText(testReminder2.description))))
    }

    @Test
    fun clickAdd_navigateToSaveReminderFragmentOne() = runTest {
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        onView(withId(R.id.addReminderFAB))
            .perform(click())
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

}