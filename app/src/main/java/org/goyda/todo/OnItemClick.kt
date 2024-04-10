package org.goyda.todo

import android.view.View

interface OnItemClick {

    fun onItemClick(v: View, position: Int)
}