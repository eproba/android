package com.czaplicki.eproba.ui.user_worksheets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.db.Worksheet
import com.czaplicki.eproba.db.WorksheetDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import java.util.UUID

class WorksheetsViewModel(private val worksheetDao: WorksheetDao, val savedStateHandle: SavedStateHandle) :
    ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val worksheets: Flow<List<Worksheet>> =
        savedStateHandle.getStateFlow("user_id", UUID.fromString("00000000-0000-0000-0000-000000000000"))
            .flatMapLatest { userId -> worksheetDao.getWorksheetsByUserId(userId) }


    var userId: UUID
        get() = savedStateHandle["user_id"] ?: UUID.fromString("00000000-0000-0000-0000-000000000000")
        set(value) {
            savedStateHandle["user_id"] = value
        }

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

                return WorksheetsViewModel(
                    (application as EprobaApplication).database.worksheetDao(),
                    savedStateHandle
                ) as T
            }
        }
    }
}