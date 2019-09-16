package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import java.security.AccessControlContext

object Utils {
    fun getUniqueID(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }


     fun hideKeyboard(context: Context) {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = (context as Activity).currentFocus
        if (view == null) {
            view = View(context)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun dpFromPx(context: Context,dp: Int): Float {
        return dp * context.resources.displayMetrics.density
    }

}