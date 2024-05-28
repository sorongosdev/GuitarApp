package com.example.tarsos_example.model

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.tarsos_example.consts.AnswerTypes
import com.example.tarsos_example.consts.ChordTypes
import com.example.tarsos_example.consts.NoteTypes
import com.example.tarsos_example.consts.WavConsts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

class MyViewModel : ViewModel() {

    /**파이썬으로부터 받은 사용자 연주 피드백 리스트*/
    private var _feedbackNoteList = MutableStateFlow(List(WavConsts.FEEDBACK_CHUNK_CNT + 1) { 0 })
    val feedbackNoteList: StateFlow<List<Int>> = _feedbackNoteList

    /**피드백리스트와 정답리스트를 비교하여 새로 그리기 위해 필요한 리스트*/
    private var _paintNoteList = MutableStateFlow(List(WavConsts.FEEDBACK_CHUNK_CNT + 1) { 0 })
    val paintNoteList: StateFlow<List<Int>> = _paintNoteList

    /**==============================================================악보, 코드*/
    /**보여주는 악보, 사용자가 친 것과 비교해서 정답인지 알려주기 위해 필요*/
    private var _shownNote1 = MutableStateFlow(listOf<Int>())
    val shownNote1: StateFlow<List<Int>> = _shownNote1

    private var _shownNote2 = MutableStateFlow(listOf<Int>())
    val shownNote2: StateFlow<List<Int>> = _shownNote2

    /**마디1에 보여주는 코드, 사용자가 친 것과 비교해서 정답인지 알려주기 위해 필요*/
    private var _shownChord1 = MutableStateFlow<String>("")
    val shownChord1: StateFlow<String> = _shownChord1

    /**마디2에 보여주는 코드, 사용자가 친 것과 비교해서 정답인지 알려주기 위해 필요*/
    private var _shownChord2 = MutableStateFlow<String>("")
    val shownChord2: StateFlow<String> = _shownChord2

    /**정답이 되는 노트*/
    var _answerNote = MutableStateFlow(listOf<Int>())
    val answerNote: StateFlow<List<Int>> = _answerNote
    /**============================================================================*/

    /**========================================================================초 관련*/
    /**녹음이 시작하고 지난 시간, 소수점 첫째자리까지 표시*/
    private var _recordSecond = MutableStateFlow<Double>(0.0)
    val recordSecond: StateFlow<Double> = _recordSecond

    /**녹음이 시작하고 지난 시간, 소수점 첫째자리까지 표시*/
    private var _countDownSecond = MutableStateFlow<Int>(4)
    val countDownSecond: StateFlow<Int> = _countDownSecond

    /**프로세스바 유지 시간*/
    private var _barSecond = MutableStateFlow<Double>(0.0)
    val barSecond: StateFlow<Double> = _barSecond
    /**=============================================================================*/

    /**녹음중 유무*/
    private var _isRecording = MutableStateFlow<Boolean>(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    /**현재 데시벨*/
//    private var _currentDb = MutableStateFlow<Double>(0.0)
//    val currentDb : StateFlow<Double> = _currentDb

    /****************************   함수들 **************************************/
    /**노트 타입별로 길이 4인 리스트를 받아 12개인 리스트로 반환해주는 함수*/
    private fun match_answer(note: List<Int>): List<Int> {
        return when (note) {
            NoteTypes.note_1111 -> NoteTypes.answer_note_1111
            NoteTypes.note_1011 -> NoteTypes.answer_note_1011
            NoteTypes.note_1010 -> NoteTypes.answer_note_1010
            else -> List(12) { 0 } // 해당하는 노트값이 없을 때 0으로 이루어진 리스트 반환
        }
    }

    /**사용자에게 보여줄 음표 리스트를 업데이트 해주는 함수*/
    fun updateNotes(note1: List<Int>, note2: List<Int>) {
        _shownNote1.value = note1
        _shownNote2.value = note2

        val draft_answer_note1 = match_answer(note1).toMutableList() // 4에 해당하는 인덱스를 12로 늘려줌
        val draft_answer_note2 = match_answer(note2)
        val halfFeedbackChunkCnt = WavConsts.HALF_FEEDBACK_CHUNK_CNT //36

        //정답 리스트를 위한 마디1 끝 값 1로 바꾸기
        draft_answer_note1[halfFeedbackChunkCnt - 1] = 1
        draft_answer_note1[halfFeedbackChunkCnt - 2] = 1
        draft_answer_note1[halfFeedbackChunkCnt - 3] = 1

        // note1과 note2에 해당하는 정답 리스트를 합쳐서 업데이트
        _answerNote.value = draft_answer_note1 + draft_answer_note2
        Log.d("answerNote", "${_answerNote.value}")
    }

    /**사용자에게 보여줄 코드를 업데이트 해주는 함수*/
    fun updateChords(chord1: String, chord2: String) {
        _shownChord1.value = chord1
        _shownChord2.value = chord2
    }

    /**연산 후 피드백 노트 리스트를 업데이트 해주는 함수*/
    fun updateFeedbackNoteList(feedbackNoteList: List<Int>) {
        _feedbackNoteList.value = feedbackNoteList

        Log.d("answerNote", "_feedbackNoteList.value ${_feedbackNoteList.value}")

        /**========================================================================박자 및 코드 판단*/
        var updatedPaintNoteList = _paintNoteList.value.toMutableList()
        updatedPaintNoteList = decideAnswer(feedbackNoteList, updatedPaintNoteList)
        /**===============================================================================*/
        _paintNoteList.value = updatedPaintNoteList

        Log.d("answerNote", "_paintNoteList.value ${_paintNoteList.value}")
    }

    /**박자 판정*/
    private fun decideAnswer(
        feedbackNoteList: List<Int>,
        updatedPaintNoteList: MutableList<Int>
    ): MutableList<Int> {
        /**=====================================================상수들*/
        val BEAT_W_CHORD_X = AnswerTypes.BEAT_W_CHORD_X
        val BEAT_C_CHORD_X = AnswerTypes.BEAT_C_CHORD_X
        val BEAT_W_CHORD_C = AnswerTypes.BEAT_W_CHORD_C
        val BEAT_W_CHORD_W = AnswerTypes.BEAT_W_CHORD_W
        val BEAT_C_CHORD_C = AnswerTypes.BEAT_C_CHORD_C
        val BEAT_C_CHORD_W = AnswerTypes.BEAT_C_CHORD_W
        val DELETED = AnswerTypes.DELETED

        val HALF_FEEDBACK_CHUNK_CNT = WavConsts.HALF_FEEDBACK_CHUNK_CNT
        val FEEDBACK_CHUNK_CNT = WavConsts.FEEDBACK_CHUNK_CNT
        /**==============================================================*/
        for (i in 0..<FEEDBACK_CHUNK_CNT) {
            val answerChordString =
                if (i < HALF_FEEDBACK_CHUNK_CNT) _shownChord1.value else _shownChord2.value // 마디마다 다른 정답 코드 설정
            val answerChordInt = ChordTypes.chords_string_int_map[answerChordString]
            if (feedbackNoteList[i + 1] != 0 && _answerNote.value[i] == 0) { // 박자 X, 코드 미정, 답이 없는데 친 경우
                updatedPaintNoteList[i] = BEAT_W_CHORD_X
            }
            else if(feedbackNoteList[i+1] == 0 && _answerNote.value[i] == 1 ) // 박자 X, 코드 미정, 답이 있는데 안 친 경우
            else if (feedbackNoteList[i + 1] != 0 && _answerNote.value[i] == 1) { // 박자 0, 코드 미정
                updatedPaintNoteList[i] = BEAT_C_CHORD_X
            }

            // 코드 판정
            if ((updatedPaintNoteList[i] == BEAT_W_CHORD_X) && (feedbackNoteList[i + 1] == answerChordInt)) { // 박자 X 코드 0
                updatedPaintNoteList[i] = BEAT_W_CHORD_C
            } else if ((updatedPaintNoteList[i] == BEAT_W_CHORD_X) && (feedbackNoteList[i + 1] != answerChordInt)) { // 박자 X 코드 X
                updatedPaintNoteList[i] = BEAT_W_CHORD_W
            } else if ((updatedPaintNoteList[i] == BEAT_C_CHORD_X) && (feedbackNoteList[i + 1] == answerChordInt)) { // 박자 0 코드 0
                updatedPaintNoteList[i] = BEAT_C_CHORD_C
            } else if ((updatedPaintNoteList[i] == BEAT_C_CHORD_X) && (feedbackNoteList[i + 1] != answerChordInt)) { // 박자 0 코드 X
                updatedPaintNoteList[i] = BEAT_C_CHORD_W
            }
        }

        Log.d("two", "updatedPaintNoteList ${updatedPaintNoteList}")

        // 3묶음 음표 합치기
        for (i in 1..<FEEDBACK_CHUNK_CNT - 3) { // 1~68
            // 연속된 박자 오답이 있으면 그 3묶음은 오답이라고 판단하고 첫번째 위치에만 오답을 남김
            // 3개 다 오답
//            Log.d("twoNotes", "for decision start updatedPaintNoteList ${updatedPaintNoteList}")
            if (isBeatCorrect(updatedPaintNoteList[i - 1]) == 'W'
                && isBeatCorrect(updatedPaintNoteList[i]) == 'W'
                && isBeatCorrect(updatedPaintNoteList[i + 1]) == 'W'
            ) {
                Log.d("twoNotes", "3개 다 오답 ${i - 1} ${i} ${i + 1}")

                updatedPaintNoteList[i] = DELETED
                updatedPaintNoteList[i + 1] = DELETED
            }
            /**========================================================================*/
            // 3묶음 중 하나라도 정답이면 3묶음 모두 지우고 정답 인 곳에 음표 그리기
            // 3묶음 중 정답과 가장 가까운 인덱스를 찾음
            /**======================================================================================================*/
            if (isBeatCorrect(updatedPaintNoteList[i - 1]) == 'C'
                && isBeatCorrect(updatedPaintNoteList[i]) == 'C'
                && isBeatCorrect(updatedPaintNoteList[i + 1]) == 'C'
            ) { // 3개 다 정답
                //=============================================================================================좌에 치우쳐져 있을 때
                Log.d("twoNotes", "3개 다 정답 ${i - 1} ${i} ${i + 1}")

                if (i - 1 == 0) { // 0부터 3개 연속 정답일 때
                    Log.d("twoNotes", "0부터 3개 연속 정답 ${i - 1} ${i} ${i + 1}")

                    updatedPaintNoteList[i] = DELETED
                    updatedPaintNoteList[i + 1] = DELETED
                } else if (_answerNote.value[i - 2] == 0 && _answerNote.value[i + 2] == 1) { // 제일 좌로 치우침
                    Log.d("twoNotes", "제일 좌로 치우침 $i")

                    updatedPaintNoteList[i + 2] = updatedPaintNoteList[i + 1]

                    updatedPaintNoteList[i - 1] = DELETED
                    updatedPaintNoteList[i] = DELETED
                    updatedPaintNoteList[i + 1] = DELETED
                } else if (i - 3 >= 0 && _answerNote.value[i - 3] == 0 && _answerNote.value[i + 3] == 1) { // 덜 좌로 치우침
                    Log.d("twoNotes", "덜 좌로 치우침 $i")

                    updatedPaintNoteList[i - 1] = DELETED
                    updatedPaintNoteList[i] = DELETED
                }
                //=======================================================================================================딱 중심
                else if (i - 4 >= 0 && _answerNote.value[i - 4] == 0 && _answerNote.value[i + 4] == 0) {
                    Log.d("twoNotes", "딱 중심 $i")

                    updatedPaintNoteList[i - 1] = DELETED
                    updatedPaintNoteList[i + 1] = DELETED
                }
                //=========================================================================================================우로 치우쳐짐
                else if (_answerNote.value[i + 2] == 0 && _answerNote.value[i - 2] == 1) { // 제일 우로 치우침
                    Log.d("twoNotes", "제일 우로 치우침 $i")
                    updatedPaintNoteList[i - 2] = updatedPaintNoteList[i - 1]
                    updatedPaintNoteList[i - 1] = DELETED
                    updatedPaintNoteList[i] = DELETED
                    updatedPaintNoteList[i + 1] = DELETED
                } else if (_answerNote.value[i + 3] == 0 && _answerNote.value[i - 3] == 1) { // 우로 조금만 치우침
                    Log.d("twoNotes", "우로 조금만 치우침 $i")

                    updatedPaintNoteList[i + 1] = DELETED
                    updatedPaintNoteList[i] = DELETED
                }
            }
//            //===============================================================================================앞에 한 개만 정답
//            else if (isBeatCorrect(updatedPaintNoteList[i - 1]) && isBeatCorrect(updatedPaintNoteList[i]) && isBeatCorrect(updatedPaintNoteList[i + 1])) {
//                val shiftedAnswerIndex = (i - 1) - 3
//                if (isBeatCorrect(updatedPaintNoteList[shiftedAnswerIndex])) { // =======================원점이 박자 정답이라면
//                    updatedPaintNoteList[i - 1] = change2WrongBeat(updatedPaintNoteList[i - 1])
//
//                    updatedPaintNoteList[i] = DELETED
//                    updatedPaintNoteList[i + 1] = DELETED
//                } else if (!isBeatCorrect(updatedPaintNoteList[shiftedAnswerIndex])) { // ================원점이 박자 정답이 아니라면
//                    updatedPaintNoteList[shiftedAnswerIndex] = updatedPaintNoteList[i - 1]
//
//                    updatedPaintNoteList[i - 1] = DELETED
//                    updatedPaintNoteList[i] = DELETED
//                    updatedPaintNoteList[i + 1] = DELETED
//                }
//            }
//            // =============================================================================================== 앞에 두 개 정답
//            else if (isBeatCorrect(updatedPaintNoteList[i - 1]) && isBeatCorrect(
//                    updatedPaintNoteList[i]
//                ) && !isBeatCorrect(updatedPaintNoteList[i + 1])
//            ) {
//                val shiftedAnswerIndex = (i - 1) - 2
//                if (isBeatCorrect(updatedPaintNoteList[shiftedAnswerIndex])) { // 만약 원점이 박자 정답이라면
//                    updatedPaintNoteList[i - 1] =
//                        change2WrongBeat(updatedPaintNoteList[i - 1]) // 앞에 한 개를 오답으로 만들고 나머지를 0으로 만듦
//                    updatedPaintNoteList[i] = DELETED
//                    updatedPaintNoteList[i + 1] = DELETED
//                } else { // 원점이 2가 아니라면
//                    updatedPaintNoteList[shiftedAnswerIndex] = updatedPaintNoteList[i - 1]
//
//                    updatedPaintNoteList[i - 1] = DELETED
//                    updatedPaintNoteList[i] = DELETED
//                    updatedPaintNoteList[i + 1] = DELETED
//                }
//            } else if (i + 4 < FEEDBACK_CHUNK_CNT && !isBeatCorrect(updatedPaintNoteList[i - 1]) && !isBeatCorrect(
//                    updatedPaintNoteList[i]
//                ) && isBeatCorrect(updatedPaintNoteList[i + 1])
//            ) { // 뒤에 한 개 정답
//
//                val shiftedAnswerIndex = (i + 1) + 3
//                updatedPaintNoteList[shiftedAnswerIndex] = updatedPaintNoteList[i + 1]
//
//                updatedPaintNoteList[i - 1] = DELETED
//                updatedPaintNoteList[i] = DELETED
//                updatedPaintNoteList[i + 1] = DELETED
//            } else if (!isBeatCorrect(updatedPaintNoteList[i - 1]) && isBeatCorrect(
//                    updatedPaintNoteList[i]
//                ) && isBeatCorrect(updatedPaintNoteList[i + 1])
//            ) { // 뒤에 두 개만 정답
//                val shiftedAnswerIndex = (i + 1) + 2
//                updatedPaintNoteList[shiftedAnswerIndex] = updatedPaintNoteList[i + 1]
//
//                updatedPaintNoteList[i - 1] = DELETED
//                updatedPaintNoteList[i] = DELETED
//                updatedPaintNoteList[i + 1] = DELETED
//            }
//            /**========================================================================*/
//
        }
        return updatedPaintNoteList
    }

    /**틀린 박자로 리턴*/
    private fun change2WrongBeat(updatedPaintNoteListElement: Int): Int {
        if (updatedPaintNoteListElement == AnswerTypes.BEAT_C_CHORD_C)
            return AnswerTypes.BEAT_W_CHORD_C
        else if (updatedPaintNoteListElement == AnswerTypes.BEAT_C_CHORD_W) {
            return AnswerTypes.BEAT_W_CHORD_W
        } else return updatedPaintNoteListElement
    }

    /**코드 정답 유무에 상관없이 박자가 맞으면 true를 반환해주는 함수*/
    fun isBeatCorrect(updatedPaintNoteListElement: Int): Char {
        if ((updatedPaintNoteListElement == AnswerTypes.BEAT_C_CHORD_W) || (updatedPaintNoteListElement == AnswerTypes.BEAT_C_CHORD_C)) {
            return 'C'
        } else if ((updatedPaintNoteListElement == AnswerTypes.BEAT_W_CHORD_W) || (updatedPaintNoteListElement == AnswerTypes.BEAT_W_CHORD_C)) {
            return 'W'
        } else if (updatedPaintNoteListElement == 0) {
            return 'N'
        } else return 'E'
    }

    /**녹음 진행 정도(초)를 업데이트 해주는 함수*/
    fun updateRecordSecond(newSecond: Double) {
        _recordSecond.value = newSecond
    }

    fun updateBarSecond(newSecond: Double) {
        _barSecond.value = newSecond
    }

    /**카운트 다운(박자)를 업데이트 해주는 함수*/
    fun updateCountDownSecond(newSecond: Int) {
        _countDownSecond.value = newSecond
    }

    /**녹음중 유무 변수를 세팅*/
    fun updateRecordingState(isRecording: Boolean) {
        _isRecording.value = isRecording
    }

    /**init 버튼을 눌렀을 때, 초를 다시 세팅*/
    fun init() {
//        AnswerDecisionModel. = List(WavConsts.FEEDBACK_CHUNK_CNT + 1) { 0 }
        _countDownSecond.value = 4
        _recordSecond.value = 0.0
        _barSecond.value = 0.0
        _paintNoteList.value = List(WavConsts.FEEDBACK_CHUNK_CNT + 1) { 0 }

        /******************************************NEW INIT*/
        // 코드와 악보 새로 불러오기
        _shownChord1.value = getRandomChord()
        _shownChord2.value = getRandomChord()
//        _shownNote1.value = getRandomNote()
//        _shownNote2.value = getRandomNote()
        updateNotes(getRandomNote(),getRandomNote())
    }

    /** 랜덤으로 코드를 가져오는 함수*/
    fun getRandomChord(): String {
        val chordMap = ChordTypes.chords_int_string_map // 코드 맵
        val randomInt = Random.nextInt(1, chordMap.size) // 1~19 랜덤 정수

        val randomChord = chordMap[randomInt] // 보여줄 코드를 인덱스를 통해 맵에서 찾아줌
        return randomChord!!
    }

    /** 랜덤으로 노트를 가져오는 함수*/
    fun getRandomNote(): List<Int> {
        val randomInt = Random.nextInt(1, 3)

        return when (randomInt) {
            1 -> NoteTypes.note_1111
            2 -> NoteTypes.note_1011
            3 -> NoteTypes.note_1010
            else -> {
                listOf<Int>(0, 0, 0, 0)
            }
        }
    }

    /**데시벨 찍어보기 위한 함수*/
//    fun updateCurrentDb(db: Double){
//        Log.d("answerNote", "_paintNoteList.value ${_paintNoteList.value}")
//    }
}
