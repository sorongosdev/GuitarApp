package com.example.tarsos_example

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.writer.WriterProcessor
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.tarsos_example.model.MyViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.time.Instant
import java.util.Date
import java.util.Timer

class AudioProcessorHandler(private val context: Context) {
    private var dispatcher: AudioDispatcher? = null
    private var randomAccessFile: RandomAccessFile? = null
    private var audioThread: Thread? = null
    private val sampleRate = 44100 // 샘플 레이트
    private val audioBufferSize = 7056 // 오디오 버퍼 크기
    private val bufferOverlap = 0 // 버퍼 겹침

    private var beepJob: Job? = null // 비프음 재생 관리하는 Job 객체

//    private val timer = Timer()

    /**녹음 시작시 실행*/
    fun SetupAudioProcessing(viewModel: MyViewModel) {
        Log.d("countdown", "SetupAudioProcessing")
        // 코루틴을 사용하여 메인 스레드에서 비동기 작업을 수행
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.updateRecordingState(isRecording = false)
            viewModel.updateBeepingState(isBeeping = true) // 비프음 재생 시작 전에 true로 설정

            // 4박자에 대한 카운트 다운
            val totalCountDownDelay = 2400L // 총 지연 시간
            val countDownInterval = 2400L / 4L // 로그를 기록할 간격 시간
            var countDownElapsedTime = 0L // 경과 시간

            while (countDownElapsedTime < totalCountDownDelay) {
                delay(countDownInterval) // 지정된 간격만큼 대기
                countDownElapsedTime += countDownInterval // 경과 시간 업데이트
                val newSecond = (4.0 - countDownElapsedTime / countDownInterval).toInt()
                viewModel.updateCountDownSecond(newSecond)

                // 메트로놈 소리
                playBeep()
                if(newSecond == 0){
                    Log.d("syncBeep","countdown start ${(4.0 - countDownElapsedTime / countDownInterval)}")
                }
                Log.d("syncBeep","countdownBeep ${newSecond} ${Instant.now().toEpochMilli()}")
            }

            // 카운트 다운 종료 후 625ms 간격으로 지속적으로 소리 내기
            beepJob = CoroutineScope(Dispatchers.IO).launch {
                val beepInterval = countDownInterval // 비프음 간격

                while (viewModel.isBeeping.value) {
                    delay(beepInterval)
                    playBeep()
                    Log.d("syncBeep","secondBeep ${viewModel.recordSecond.value} ${Instant.now().toEpochMilli()}")
                }

//                while (isActive) { // 코루틴이 활성 상태인 동안 반복
//                    delay(beepInterval)
//                    playBeep()
//                    Log.d("beep", "beep sound ${viewModel.recordSecond.value}")
//                }
            }

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

            viewModel.updateRecordingState(isRecording = true)

            // 5초 동안 진행 상황을 로그로 기록
            val totalDelay = 4800L // 총 지연 시간
            val interval = 100L // 로그를 기록할 간격 시간
            var elapsedTime = 0L // 경과 시간
            while (elapsedTime < totalDelay) {
                viewModel.updateCountDownSecond(0) // 녹음이 시작되면 카운트다운을 0으로 바꿈

                delay(interval) // 지정된 간격만큼 대기
                elapsedTime += interval // 경과 시간 업데이트
                viewModel.updateRecordSecond(elapsedTime / 1000.0)
                Log.d("beep", "녹음 진행 중: ${elapsedTime / 1000.0}초")
            }

            stopAudioProcessing(viewModel = viewModel)
        }
    }

    /**녹음 중지시 실행되는 리스너*/
    suspend fun stopAudioProcessing(viewModel: MyViewModel) {
        viewModel.updateBeepingState(false) // 비프음이 중지되었다는 것을 알림
        beepJob?.join() // 비프음 재생이 완전히 중지될 때까지 대기
        releaseDispatcher()
        viewModel.updateRecordingState(isRecording = false)
        randomAccessFile?.close()
        randomAccessFile = null
        getResultList(viewModel = viewModel) // 연산 결과 리턴
    }

    /**비프음 재생 로직*/
    fun playBeep() {
        val toneType = ToneGenerator.TONE_DTMF_0
        ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME).apply {
            startTone(toneType, 100)
            release() // 자원 해제
        }
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
