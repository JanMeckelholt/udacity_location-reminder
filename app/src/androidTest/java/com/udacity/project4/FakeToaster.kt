package com.udacity.project4

import android.content.Context
import com.udacity.project4.base.Toaster

//adapted from https://con-fotiadis.medium.com/android-shorts-testing-toasts-with-espresso-50584d5c8937
object FakeToaster {
    val toasts = mutableListOf<String> ()
    fun providesToaster(context: Context): Toaster {
        return object : Toaster {
            override fun showToast(text: String) {
                toasts.add(text)
            }
        }
    }
}