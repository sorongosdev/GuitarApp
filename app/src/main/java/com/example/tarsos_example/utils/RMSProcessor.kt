package com.example.tarsos_example

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor

class RMSProcessor(private val onRmsChanged: (Double) -> Unit) : AudioProcessor {

    override fun process(audioEvent: AudioEvent): Boolean {
        val buffer = audioEvent.floatBuffer
        val rms = calculateRMS(buffer)
        val db = 20 * Math.log10(rms)
        onRmsChanged(db)
        return true
    }

    override fun processingFinished() {}

    private fun calculateRMS(buffer: FloatArray): Double {
        var sum = 0.0
        for (sample in buffer) {
            sum += sample * sample
        }
        return Math.sqrt(sum / buffer.size)
    }
}
