package com.czaplicki.eproba.ui.manage_exams

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.db.Exam
import com.czaplicki.eproba.db.ExamDao
import kotlinx.coroutines.flow.Flow

class ManagedExamsViewModel(
    private val examDao: ExamDao,
    private val savedStateHandle: SavedStateHandle
) :
    ViewModel() {

    val exams: Flow<List<Exam>> = examDao.getAll()


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

                return ManagedExamsViewModel(
                    (application as EprobaApplication).database.examDao(),
                    savedStateHandle
                ) as T
            }
        }
    }
}