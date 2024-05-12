import android.util.Log
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor

class MyAudioProcessor : AudioProcessor {
    override fun process(audioEvent: AudioEvent): Boolean {
        Log.d("cutWav","${System.currentTimeMillis()}")
        return true
    }

    override fun processingFinished() {
        // 모든 오디오 처리가 완료된 후의 로직 구현
        // 예: 리소스 정리, 로깅 등
    }
}
