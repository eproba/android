package com.czaplicki.eproba.ui.manage_worksheets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.db.Worksheet
import com.czaplicki.eproba.db.WorksheetDao
import kotlinx.coroutines.flow.Flow

class ManagedWorksheetsViewModel(
    private val worksheetDao: WorksheetDao,
    private val savedStateHandle: SavedStateHandle
) :
    ViewModel() {

    val worksheets: Flow<List<Worksheet>> = worksheetDao.getAll()


    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])
                // Create a SavedStateHandle for this ViewModel from extras
                val savedStateHandle = extras.createSavedStateHandle()

                return ManagedWorksheetsViewModel(
                    (application as EprobaApplication).database.worksheetDao(),
                    savedStateHandle
                ) as T
            }
        }
    }
}