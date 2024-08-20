package com.czaplicki.eproba.ui.compose_worksheet

import androidx.recyclerview.widget.RecyclerView


interface StartDragListener {
    fun requestDrag(viewHolder: RecyclerView.ViewHolder?)
}