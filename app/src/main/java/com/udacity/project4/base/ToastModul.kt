package com.udacity.project4.base

import android.content.Context
import android.widget.Toast

object ToastModule {
    fun providesToaster(context: Context): Toaster {
        return object : Toaster {
            override fun showToast(text: String) {
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            }
        }
    }
}

interface Toaster {
    fun showToast(text: String)
}