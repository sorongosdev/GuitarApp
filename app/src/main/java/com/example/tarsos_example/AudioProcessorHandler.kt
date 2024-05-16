package com.example.tarsos_example

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.writer.WriterProcessor
import com.chaquo.python.PyObject
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
    private var dispatcher: AudioDispatcher? = null
    private var randomAccessFile: RandomAccessFile? = null
    private var audioThread: Thread? = null
    private val sampleRate = 44100 // 샘플 레이트
    private val audioBufferSize = 7056 // 오디오 버퍼 크기
    private val bufferOverlap = 0 // 버퍼 겹침

    private val timer = Timer()

    /**녹음 시작시 실행*/
    fun SetupAudioProcessing(viewModel: MyViewModel) {
        Log.d("processbar", "SetupAudioProcessing")
        // 코루틴을 사용하여 메인 스레드에서 비동기 작업을 수행
        CoroutineScope(Dispatchers.Main).launch {
            // 시작 시간 측정
//            val start = System.currentTimeMillis()

            // 현재 사용하고 있는 dispatcher 객체를 제거하고, 마이크로부터 입력을 받는 dispatcher 객체를 생성
            releaseDispatcher()
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(
                sampleRate,
                audioBufferSize,
                bufferOverlap
            )

            val filename = "recorded_audio.wav"
            val file = File(context.filesDir, filename)

            // 입력받은 음성 파일을 저장하기 위해 RandomAccessFile을 생성
            randomAccessFile = RandomAccessFile(file, "rw")
            val recordProcessor = WriterProcessor(tarsosDSPAudioFormat, randomAccessFile)
            dispatcher?.addAudioProcessor(recordProcessor)

            // dispatcher로 Thread 실행
            audioThread = Thread(dispatcher, "Audio Thread")
            audioThread?.start()

            // 5초 동안 진행 상황을 로그로 기록
            val totalDelay = 5000L // 총 지연 시간
            val interval = 100L // 로그를 기록할 간격 시간
            var elapsedTime = 0L // 경과 시간

            while (elapsedTime < totalDelay) {
                delay(interval) // 지정된 간격만큼 대기
                elapsedTime += interval // 경과 시간 업데이트
                viewModel.updateRecordSecond(elapsedTime/1000.0)
                Log.d("processbar", "녹음 진행 중: ${elapsedTime / 1000.0}초")
            }

            stopAudioProcessing(viewModel = viewModel)
        }
    }



    /**녹음 중지시 실행되는 리스너*/
    fun stopAudioProcessing(viewModel: MyViewModel) {
        releaseDispatcher()
        timer.cancel()
        randomAccessFile?.close()
        randomAccessFile = null
        getResultList(viewModel = viewModel) // 연산 결과 리턴
    }

    /**녹음 중지시 wav 파일을 파이썬으로 넘겨주고 출력을 받아오는 함수*/
    fun getResultList(viewModel: MyViewModel) {
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
        val feedbackNoteListPyObject = waveBytes?.let { bytes ->
            pyObj.callAttr("main", bytes).asList()
        }

        // feedbackNoteListPyObject를 Kotlin의 List<String>으로 변환
        val feedbackNoteListKotlin: List<Int> =
            feedbackNoteListPyObject?.map { it.toInt() } ?: listOf()

        viewModel.updateFeedbackNoteList(feedbackNoteListKotlin)
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

    private fun getFileList() {
        val filesList = File(context.filesDir.absolutePath).listFiles()
        filesList?.forEach { file ->
            Log.d("File Name", file.name)
        }
    }
}
