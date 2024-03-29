package com.czaplicki.eproba.ui.user_exams

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.db.Exam
import com.czaplicki.eproba.db.ExamDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

class ExamsViewModel(private val examDao: ExamDao, val savedStateHandle: SavedStateHandle) :
    ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val exams: Flow<List<Exam>> =
        savedStateHandle.getStateFlow("user_id", -1L)
            .flatMapLatest { userId -> examDao.getExamsByUserId(userId) }


    var userId: Long
        get() = savedStateHandle["user_id"] ?: -1L
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

                return ExamsViewModel(
                    (application as EprobaApplication).database.examDao(),
                    savedStateHandle
                ) as T
            }
        }
    }
}