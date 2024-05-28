package com.example.tarsos_example.consts

object ChordTypes {
    val chords_int_string_map: Map<Int, String> = mapOf(
        1 to "A",
        2 to "B", 3 to "Bm", 4 to "B7",
        5 to "C", 6 to "C7",
        7 to "Dm",
        8 to "E",
        9 to "F",
        10 to "G", 11 to "G7"
    )
    val chords_string_int_map: Map<String, Int> = mapOf(
        "A" to 1,
        "B" to 2, "Bm" to 3, "B7" to 4,
        "C" to 5, "C7" to 6,
        "Dm" to 7,
        "E" to 8,
        "F" to 9,
        "G" to 10, "G7" to 11
    )

}
