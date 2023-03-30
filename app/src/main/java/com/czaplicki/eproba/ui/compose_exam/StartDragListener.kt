package com.czaplicki.eproba.ui.compose_exam

import androidx.recyclerview.widget.RecyclerView


interface StartDragListener {
    fun requestDrag(viewHolder: RecyclerView.ViewHolder?)
}