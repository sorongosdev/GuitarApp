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
import be.tarsos.dsp.writer.WriterProcessor
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

class AudioProcessorHandler(private val context: Context) {
    private var start: Long = 0
    private val dictionary = mutableMapOf<Double, String>()
    private var dispatcher: AudioDispatcher? = null
    private var randomAccessFile: RandomAccessFile? = null
    private var audioThread: Thread? = null
    private val _pitchTextViewValue = MutableLiveData<String>()
    val pitchTextViewValue: LiveData<String> get() = _pitchTextViewValue

    /**녹음 시작시 실행*/
    fun SetupAudioProcessing() {
        // 코루틴을 사용하여 메인 스레드에서 비동기 작업을 수행
        CoroutineScope(Dispatchers.Main).launch {
            //시작 시간 측정
            start = System.currentTimeMillis()
            Log.d("cutWav","$start")

            //현재 사용하고 있는 dispatcher 객체를 제거하고, 마이크로부터 입력을 받는 dispatcher 객체를 생성
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

            //------------------------------------------------------------------------------------
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
            //------------------------------------------------------------------------------------

            //dispatcher로 Thread 실행
            audioThread = Thread(dispatcher, "Audio Thread")
            audioThread?.start()
        }
    }

    /**녹음 중지시 실행되는 리스너*/
    fun stopAudioProcessing() {
        releaseDispatcher()
        randomAccessFile?.close()
        randomAccessFile = null
        transferWavToPy()
    }

    /**녹음 중지시 wav 파일을 파이썬으로 넘겨주는 함수*/
    fun transferWavToPy() {
        // 내부 저장소에 있는 파일의 경로를 지정합니다.
        val filePath = "${context.filesDir.absolutePath}/recorded_audio.wav"

        // 정의한 함수를 사용하여 파일에서 바이트 데이터를 읽습니다.
        val waveBytes = readFileBytes(filePath)

        // 파이썬 init
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context));
        }

        // 파이썬 모듈 불러옴
        val py = Python.getInstance()
        val pyObj = py.getModule("example")

        // 읽은 바이트 데이터를 Python 코드에 전달
        val result = waveBytes?.let { bytes ->
            pyObj.callAttr("read_wav_file", bytes)
        }

        // Chaquopy에서 파이썬 함수 호출 결과를 받음
        val (sampleRate, signalList) = result?.asList() ?: listOf(0, listOf<Float>())

        // 결과 로그 출력
        Log.d("WAV_INFO", "Sample Rate: $sampleRate")
        // 신호 배열의 일부를 로그로 출력하려면, signalList를 적절히 슬라이싱 하거나 요약하여 출력
        Log.d("WAV_INFO", "Signal Array First Elements: ${signalList}")
    }



    // wav 의 바이트를 읽음
    fun readFileBytes(filePath: String): ByteArray? {
        return try {
            val file = File(filePath)
            file.readBytes()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    //dispatcher 객체 해제
    private fun releaseDispatcher() {
        dispatcher?.stop()
        dispatcher = null
    }
}
