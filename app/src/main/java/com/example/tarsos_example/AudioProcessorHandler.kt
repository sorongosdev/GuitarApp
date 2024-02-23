package com.example.tarsos_example
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.util.fft.FFT
import be.tarsos.dsp.util.fft.WindowFunction
import be.tarsos.dsp.writer.WriterProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile

class AudioProcessorHandler(private val context: Context) {
    private var start: Long = 0
    private val dictionary = mutableMapOf<Double, String>()
    private var dispatcher: AudioDispatcher? = null
    private var randomAccessFile: RandomAccessFile? = null
    private var audioThread: Thread? = null
    private val _pitchTextViewValue = MutableLiveData<String>()
    val pitchTextViewValue: LiveData<String> get() = _pitchTextViewValue

    fun SetupAudioProcessing() {
        CoroutineScope(Dispatchers.Main).launch {
            //시작 시간 측정
            start = System.currentTimeMillis()

            //현재 사용하고 있는 dispatcher 객체를 제거하고, 마이크로부터 입력을 받는 dispatcher 객체를 생성한다.
            releaseDispatcher()
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)

            val filename = "recorded_audio.wav"
            val file = File(context.filesDir, filename)

            //입력받은 음성 파일을 저장하기 위해 RandomAccessFile을 생성
            //지정한 출력으로 음성 데이터를 기록하는 WriterProcessor(AudioProcessor) 객체를 생성
            //dispatcher에 추가
            randomAccessFile = RandomAccessFile(file, "rw")
            val recordProcessor = WriterProcessor(tarsosDSPAudioFormat, randomAccessFile)
            dispatcher?.addAudioProcessor(recordProcessor)

            //pitch detection handler를 만들어 입력된 pitch를 가져온 후, note로 변환해준다.
            val pitchDetectionHandler = object : PitchDetectionHandler {
                override fun handlePitch(res: PitchDetectionResult, e: AudioEvent) {
                    val pitchInHz = res.pitch  // 입력된 pitch 가져오기
                    val octav = ProcessPitch.processPitch(pitchInHz)  // pitch -> note

                    val end = System.currentTimeMillis()  // note가 입력된 시간 가져오기(일반시각)
                    val time = (end - start) / 1000.0  // 녹음이 시작된 이후의 시간으로 변경
                    dictionary[time] = octav  // hashmap에 <time, note> 입력

                    _pitchTextViewValue.postValue(octav)

                    // FFT 변환을 통해 음성 데이터를 스펙트럼으로 변환
                    val fftTransform = FFT(e.floatBuffer.size / 2)
                    val amplitudes = FloatArray(e.floatBuffer.size / 2)
                    fftTransform.forwardTransform(e.floatBuffer)
                    fftTransform.modulus(e.floatBuffer, amplitudes)

                    // HPS 알고리즘을 적용하여 다중 음향 정보를 얻음
                    val hpsResult = hps(amplitudes)

                    // 로그 출력
                    Log.d("HPS pitch", "The result of HPS: $hpsResult Hz")

                }
            }

            //AMDF - A pitch extractor that extracts the Average Magnitude Difference (AMDF) from an audio buffer.DYNAMIC_WAVELET - An implementation of a dynamic wavelet pitch detection algorithm (See DynamicWavelet), described in a paper by Eric Larson and Ross Maddox “Real-Time Time-Domain Pitch Tracking Using Wavelets
            //FFT_PITCH - Returns the frequency of the FFT-bin with most energy.
            //FFT_YIN - A YIN implementation with a faster FastYin for the implementation.
            //MPM - McLeodPitchMethod.YIN - YIN algorithm.

            //pitch detection은 pitchProcessor 클래스를 통해 수행
            //이 때 실시간 pitch detection 결과를 전달받기 위한 thread handler 객체를 지정해줘야 한다.
            val pitchProcessor = PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                22050f,
                1024,
                pitchDetectionHandler // thread handler
            )
            dispatcher?.addAudioProcessor(pitchProcessor)

            //dispatcher로 Thread 실행
            audioThread = Thread(dispatcher, "Audio Thread")
            audioThread?.start()
        }
    }

    fun stopAudioProcessing() {
        releaseDispatcher()
        randomAccessFile?.close()
        randomAccessFile = null
    }

    //dispatcher 객체 해제
    private fun releaseDispatcher() {
        dispatcher?.stop()
        dispatcher = null
    }

    fun hps(inputSignal: FloatArray): Float {
        val downsampled = Array(5) { FloatArray(inputSignal.size / (it+1)) }
        for (i in downsampled.indices) {
            for (j in downsampled[i].indices) {
                downsampled[i][j] = inputSignal[j * i]
            }
        }

        val product = FloatArray(downsampled[0].size) { 1.0f }
        for (i in downsampled.indices) {
            for (j in downsampled[i].indices) {
                product[j] *= downsampled[i][j]
            }
        }

        val peakIndex = product.indices.maxByOrNull { product[it] } ?: -1
        val peakFreq = 22050f * peakIndex / product.size

        return peakFreq
    }

}
