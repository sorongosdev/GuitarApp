package com.example.tarsos_example

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.writer.WriterProcessor
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.Timer
import java.util.TimerTask

class AudioProcessorHandler(private val context: Context) {
    private var start: Long = 0
    private val dictionary = mutableMapOf<Double, String>()
    private var dispatcher: AudioDispatcher? = null
    private var randomAccessFile: RandomAccessFile? = null
    private var audioThread: Thread? = null
    private val _pitchTextViewValue = MutableLiveData<String>()
    val pitchTextViewValue: LiveData<String> get() = _pitchTextViewValue
    private val sampleRate = 44100 // 샘플 레이트
    private val audioBufferSize = 7056 // 오디오 버퍼 크기
    private val bufferOverlap = 0 // 버퍼 겹침

    private val timer = Timer()

    /**녹음 시작시 실행*/
    fun SetupAudioProcessing() {
        // 코루틴을 사용하여 메인 스레드에서 비동기 작업을 수행
        CoroutineScope(Dispatchers.Main).launch {
            // 시작 시간 측정
            start = System.currentTimeMillis()
            Log.d("cutWav", "$start")

            // 현재 사용하고 있는 dispatcher 객체를 제거하고, 마이크로부터 입력을 받는 dispatcher 객체를 생성
            releaseDispatcher()
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, audioBufferSize, bufferOverlap)

            val filename = "recorded_audio.wav"
            val file = File(context.filesDir, filename)

            // 입력받은 음성 파일을 저장하기 위해 RandomAccessFile을 생성
            // 지정한 출력으로 음성 데이터를 기록하는 WriterProcessor(AudioProcessor) 객체를 생성
            // dispatcher에 추가
            randomAccessFile = RandomAccessFile(file, "rw")
            val recordProcessor = WriterProcessor(tarsosDSPAudioFormat, randomAccessFile)
            dispatcher?.addAudioProcessor(recordProcessor)

            // dispatcher로 Thread 실행
            audioThread = Thread(dispatcher, "Audio Thread")
            audioThread?.start()

            // 4.8초 후에 녹음 중지 (Timer와 TimerTask 대신 delay 사용)
            delay(4800)
            stopAudioProcessing()
        }
    }


    /**녹음 중지시 실행되는 리스너*/
    fun stopAudioProcessing() {
        releaseDispatcher()
        timer.cancel()
        randomAccessFile?.close()
        randomAccessFile = null
        transferWavToPy()
        getFileList()

    }

    private fun getFileList() {
        val filesList = File(context.filesDir.absolutePath).listFiles()
        filesList?.forEach { file ->
            Log.d("File Name", file.name)
        }
    }

    /**녹음 중지시 wav 파일을 파이썬으로 넘겨주는 함수*/
    fun transferWavToPy() {
        // 내부 저장소에 있는 파일의 경로를 지정
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
            pyObj.callAttr("main", bytes)
        }
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
