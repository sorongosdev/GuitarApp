package com.example.tarsos_example.consts

object ChordTypes {
    val chords_int_string_map: Map<Int, String> = mapOf(
        1 to "A", 12 to "Am", 3 to "A7",
        4 to "B", 5 to "Bm", 6 to "B7",
        7 to "C", 8 to "C7",
        9 to "D", 10 to "Dm", 11 to "D7",
        12 to "E", 13 to "Em", 14 to "E7",
        15 to "F", 16 to "Fm", 17 to "F7",
        18 to "G", 19 to "G7"
    )
    val chords_string_int_map: Map<String, Int> = mapOf(
        "A" to 1, "Am" to 2, "A7" to 3,
        "B" to 4, "Bm" to 5, "B7" to 6,
        "C" to 7, "C7" to 8,
        "D" to 9, "Dm" to 10, "D7" to 11,
        "E" to 12, "Em" to 13, "E7" to 14,
        "F" to 15, "Fm" to 16, "F7" to 17,
        "G" to 18, "G7" to 19
    )

}
