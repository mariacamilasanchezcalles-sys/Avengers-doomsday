package com.cami.maratondoomsday.data

import android.content.Context

/** Lo que se guarda solo en este celular: el código del maratón y quién lo usa. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("maraton", Context.MODE_PRIVATE)

    var code: String?
        get() = sp.getString("code", null)
        set(value) = sp.edit().putString("code", value).apply()

    var author: String?
        get() = when (val a = sp.getString("author", null)) {
            "Cami" -> "María"
            "Papá" -> "Mario"
            else -> a
        }
        set(value) = sp.edit().putString("author", value).apply()
}
