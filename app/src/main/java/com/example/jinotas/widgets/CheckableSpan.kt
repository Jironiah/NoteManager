package com.example.jinotas.widgets

import android.text.style.ClickableSpan
import android.view.View

class CheckableSpan : ClickableSpan() {
    var isChecked: Boolean = false

    fun updateCheckedState(checked: Boolean) {
        isChecked = checked
    }

    override fun onClick(view: View) {
        isChecked = !isChecked
        if (view is SimplenoteEditText) {
            try {
                view.toggleCheckbox(this)
                view.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}