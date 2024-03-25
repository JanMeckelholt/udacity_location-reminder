package com.udacity.project4

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

const val globalTag = "jmeckel"
open class BaseActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(object : Timber.DebugTree(){
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                super.log(priority, "${globalTag}_$tag", message, t)
            }
            override fun createStackElementTag(element: StackTraceElement): String {
                return String.format(
                    "%s:%s",
                    element.methodName,
                    super.createStackElementTag(element)
                )
            }
        })

    }
}