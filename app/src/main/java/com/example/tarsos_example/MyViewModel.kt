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

    /**마디1에 보여주는 코드, 사용자가 친 것과 비교해서 정답인지 알려주기 위해 필요*/
    private var _shownChord1 = MutableStateFlow<String>("")
    val shownChord1 : StateFlow<String> = _shownChord1

    /**마디2에 보여주는 코드, 사용자가 친 것과 비교해서 정답인지 알려주기 위해 필요*/
    private var _shownChord2 = MutableStateFlow<String>("")
    val shownChord2 : StateFlow<String> = _shownChord2

    fun updateChords(chord1: String, chord2: String) {
        _shownChord1.value = chord1
        _shownChord2.value = chord2
    }

    /**녹음이 시작하고 지난 시간, 소수점 첫째자리까지 표시*/
    private var _recordSecond = MutableStateFlow<Double>(0.0)
    val recordSecond : StateFlow<Double> = _recordSecond

    /**녹음중 유무*/
    private var _isRecording = MutableStateFlow<Boolean>(true)
    val isRecording : StateFlow<Boolean> = _isRecording

    fun updateFeedbackNoteList(newList: List<Int>) {
        _feedbackNoteList.value = newList
        Log.d("processbar","updateFeedbackNoteList // feedbackNoteList.value ${feedbackNoteList.value}")
    }

    fun updateRecordSecond(newSecond: Double){
        _recordSecond.value = newSecond
    }
}
