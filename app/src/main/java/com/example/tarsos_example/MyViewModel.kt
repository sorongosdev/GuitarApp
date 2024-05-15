package com.example.tarsos_example

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class MyViewModel : ViewModel() {

    /**파이썬으로부터 받은 사용자 연주 피드백 리스트*/
    private var _feedbackNoteList = MutableStateFlow(List(25) { 0 })
    val feedbackNoteList: StateFlow<List<Int>> = _feedbackNoteList

    /**보여주는 악보, 사용자가 친 것과 비교해서 정답인지 알려주기 위해 필요*/
    private var _shownNoteList = MutableStateFlow(listOf<Int>())
    val shownNoteList : StateFlow<List<Int>> = _shownNoteList

    /**보여주는 코드, 사용자가 친 것과 비교해서 정답인지 알려주기 위해 필요*/
    private var _shownChord1 = MutableStateFlow<Int>(0)
    val shownChord1 : StateFlow<Int> = _shownChord1

    private var _shownChord2 = MutableStateFlow<Int>(0)
    val shownChord2 : StateFlow<Int> = _shownChord1

    fun updateFeedbackNoteList(newList: List<Int>) {
        _feedbackNoteList.value = newList
        Log.d("undraw","updateFeedbackNoteList // feedbackNoteList.value ${feedbackNoteList.value}")
    }

    fun updateShownChord1(newInt: Int) {
        _shownChord1.value = newInt
    }

    fun updateShownChord2(newInt: Int) {
        _shownChord2.value = newInt
    }
}
