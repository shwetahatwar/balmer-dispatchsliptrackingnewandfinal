package com.briot.balmerlawrie.implementor.repository.local

import android.content.Context
import android.graphics.Color

class PrefRepository {
    companion object {
        val singleInstance = PrefRepository();
    }

    private val prefs = HashMap<String, String>()

    fun setKeyValue(key: String, value: String) {
        prefs[key] = value;
    }

    fun getValueOrDefault(key: String, defaultValue: String) : String {
        return prefs[key] ?: defaultValue;
    }

    fun serializePrefs(context: Context) {
        val sharedPref = context.getSharedPreferences("default", Context.MODE_PRIVATE) ?: return
        val editor = sharedPref.edit();

        prefs.keys.asIterable().forEach {
            editor.putString(it, prefs[it] ?: "")
        }

        editor.commit();
    }

    fun deserializePrefs(context: Context) {
        val sharedPref = context.getSharedPreferences("default", Context.MODE_PRIVATE) ?: return
        sharedPref.all.keys.forEach {
            prefs[it] = sharedPref.getString(it, "").orEmpty()
        }
    }
}

class PrefConstants {
    public val USER_TOKEN = "USERTOKEN"
    public val USER_NAME = "USER_NAME"
    public val USER_ID = "USER_ID"
    public val ROLE_NAME = "ROLE_NAME"
    public val ROLE_ID = "ROLE_ID"
    public val EMPLOYEE_ID = "EMPLOYEE_ID"
    public val EMPLOYEE_NAME = "EMPLOYEE_NAME"
    public val EMPLOYEE_EMAIL = "EMPLOYEE_EMAIL"
    public val EMPLOYEE_PHONE = "EMPLOYEE_PHONE"
    public val EMPLOYEE_STATUS = "EMPLOYEE_STATUS"

    public val MAX_STALE = 60 * 60 * 3 * 1 // 3 hours
    public val MAX_AGE = 60 * 60 * 2 // 2 hours

    val lightGreenColor = Color.parseColor("#FF9CF780")
    val lightOrangeColor = Color.parseColor("#73FF8800")
    val lightGrayColor = Color.parseColor("#FFF3F3F3")
    val lightYellowColor = Color.parseColor("#FFFF99")
    val messageBackgroundColor = Color.parseColor("#FFD50000")
}