package com.example.tarsos_example.consts

/**악보 타입*/
object NoteTypes {
    val note_1111 = listOf<Int>(1,1,1,1)
    val note_1011 = listOf<Int>(1,0,1,1)
    val note_1010 = listOf<Int>(1,0,1,0)

//    val note_feedback = listOf<Int>(
//        1,1,1,1,1, // 0~4
//        1,1,1,1,1, // 5~9
//        1,1,1,1,1, // 10~14
//        1,1,1,1,1, // 15~19
//        1,1,1,1,1,1 // 20~25
//    )
    val note_feedback = List(WavConsts.CHUNK_CNT+3){1} //27

    val dummyList = listOf<Double>(
        0.6,1.2,1.8,2.4,
        3.0,3.6,4.2,4.8
    )
}