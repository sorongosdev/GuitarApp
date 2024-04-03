package com.example.tarsos_example

import android.util.Log

object ProcessPitch {
    // pitch -> key
    fun processPitch(pitchInHz: Float): String {
//        if (pitchInHz > 0)
//            Log.d("pitch", "$pitchInHz")
        var noteText = "$pitchInHz"
        when {
            pitchInHz >= 82 && pitchInHz < 83 -> noteText = "E"
            pitchInHz >= 110 && pitchInHz < 111 -> noteText = "A"
        }
        return noteText
    }
}
