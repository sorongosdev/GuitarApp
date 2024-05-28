package com.example.tarsos_example.consts

import androidx.compose.ui.graphics.Color

object AnswerTypes {
    const val BEAT_W_CHORD_X = 1
    const val BEAT_C_CHORD_X = 2
    const val BEAT_W_CHORD_C = 3
    const val BEAT_W_CHORD_W = 4
    const val BEAT_C_CHORD_C = 5
    const val BEAT_C_CHORD_W = 6
    const val DELETED = 7

    val answerCodeMap: Map<Int, Color> = mapOf(
        BEAT_W_CHORD_X to (Color.Red),       // 박자 X 코드 미정
        BEAT_C_CHORD_X to (Color.Green),     // 박자 0 코드 미정
        BEAT_W_CHORD_C to (Color.Blue),      // 박자 X 코드 0
        BEAT_W_CHORD_W to (Color.Cyan),      // 박자 X 코드 X
        BEAT_C_CHORD_C to (Color.Magenta),   // 박자 0 코드 0
        BEAT_C_CHORD_W to (Color.Yellow),     // 박자 0 코드 X
        DELETED to (Color.Gray)
    )
}
