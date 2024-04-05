package com.udacity.project4

import android.Manifest
import android.app.Application
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.Root
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.koin.test.inject


@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource

    private lateinit var appContext: Application

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    companion object {
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    @get:Rule
    var activityRule = ActivityScenarioRule(RemindersActivity::class.java)
    private var decorView: View? = null

    @JvmField
    @Rule
    val permissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(*permissions, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant(*permissions)
        }


    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)

    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun addReminder_displayInUiIncludingSuccessToast() = runTest {
        val viewModel: SaveReminderViewModel by inject()
        val locStr = "Added LOCATION"
        val titleStr = "Added TITLE"
        val descStr = "Added DESCRIPTION"
        withContext(Dispatchers.Main) {
            viewModel.setLocationIsEnabled(true)
            viewModel.setBackgroundLocationAccessGranted(true)
            viewModel.reminderTitle.value = titleStr
            viewModel.reminderSelectedLocationStr.value = locStr
            viewModel.latitude.value = 11.0
            viewModel.longitude.value = -11.0
        }
        val activityScenario = activityRule.scenario
        dataBindingIdlingResource.monitorActivity(activityScenario)
        activityScenario.onActivity {
            val navHostFragment =
                it.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.welcomeFragment_to_reminderListFragment)
            decorView = it.getWindow().getDecorView()
        }
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())
        onView(withId(R.id.reminderTitle))
            .perform(ViewActions.replaceText(titleStr))
        onView(withId(R.id.reminderDescription))
            .perform(ViewActions.replaceText(descStr))
        onView(withId(R.id.selectedLocation))
            .perform(setTextInTextView(locStr))
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())
        onView(withText(titleStr))
            .check(ViewAssertions.matches(isDisplayed()))
        onView(withText(descStr))
            .check(ViewAssertions.matches(isDisplayed()))
        onView(withText(locStr))
            .check(ViewAssertions.matches(isDisplayed()))
        onView(withText(R.string.reminder_saved))
            .inRoot(ToastMatcher().apply {
                matches(isDisplayed())
            })
        activityScenario.close()
    }


    @Test
    fun saveReminderWithoutTitle_showSnackbar() = runTest {
        val viewModel: SaveReminderViewModel by inject()
        withContext(Dispatchers.Main) {
            viewModel.setLocationIsEnabled(true)
            viewModel.setBackgroundLocationAccessGranted(true)
        }
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        activityScenario.onActivity {
            val navHostFragment =
                it.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.welcomeFragment_to_reminderListFragment)
        }
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())
        onView(withId(R.id.snackbar_text)).check(
            matches(
                isDisplayed()
            )
        )
        onView(withId(R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))
        activityScenario.close()
    }

    fun setTextInTextView(value: String): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return CoreMatchers.allOf(ViewMatchers.isAssignableFrom(TextView::class.java))
            }

            override fun perform(uiController: UiController, view: View) {
                (view as TextView).text = value
            }

            override fun getDescription(): String {
                return "replace text"
            }
        }
    }
}

// adapted from https://stackoverflow.com/questions/28390574/checking-toast-message-in-android-espresso
class ToastMatcher : TypeSafeMatcher<Root?>() {
    override fun matchesSafely(item: Root?): Boolean {
        val type: Int? = item?.windowLayoutParams?.get()?.type
        if (type == WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW) {
            val windowToken: IBinder = item.decorView.windowToken
            val appToken: IBinder = item.decorView.applicationWindowToken
            if (windowToken === appToken) { // means this window isn't contained by any other windows.
                return true
            }
        }
        return false
    }

    override fun describeTo(description: Description?) {
        description?.appendText("is toast")
    }

}

