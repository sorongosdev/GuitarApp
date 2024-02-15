package com.example.tarsos_example

import android.util.Log

object ProcessPitch {
    // pitch -> key
    fun processPitch(pitchInHz: Float): String {
        Log.d("pitch","$pitchInHz")
        var noteText = "$pitchInHz"
        when {
            pitchInHz >= 16.35 && pitchInHz < 17.32 -> noteText = "C0"
            pitchInHz >= 17.32 && pitchInHz < 18.35 -> noteText = "C#0"
            pitchInHz >= 18.35 && pitchInHz < 19.45 -> noteText = "D0"
            pitchInHz >= 19.45 && pitchInHz < 20.60 -> noteText = "D#0"
            pitchInHz >= 20.60 && pitchInHz <= 21.83 -> noteText = "E0"
        }
        return noteText
    }
}
