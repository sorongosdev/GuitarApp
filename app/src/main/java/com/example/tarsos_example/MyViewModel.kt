package com.example.tarsos_example

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class MyViewModel : ViewModel() {

    private var _feedbackNoteList = MutableStateFlow(List(25) { 0 })

    // Public readonly StateFlow
    val feedbackNoteList: StateFlow<List<Int>> = _feedbackNoteList

    fun updateFeedbackNoteList(newList: List<Int>) {
        _feedbackNoteList.value = newList
        Log.d("undraw","updateFeedbackNoteList // feedbackNoteList.value ${feedbackNoteList.value}")
    }
}
