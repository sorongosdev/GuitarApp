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
import com.example.tarsos_example.consts.WavConsts
import com.example.tarsos_example.model.MyViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

class AudioProcessorHandler(private val context: Context) {
    private var dispatcher: AudioDispatcher? = null
    private var randomAccessFile: RandomAccessFile? = null
    private var audioThread: Thread? = null
    private val sampleRate = 44100 // 샘플 레이트
    private val audioBufferSize = 7056 // 오디오 버퍼 크기
    private val bufferOverlap = 0 // 버퍼 겹침

    private var beepJob: Job? = null // 비프음 재생 관리하는 Job 객체

    /**녹음 시작시 실행*/
    fun SetupAudioProcessing(viewModel: MyViewModel) {
        // 코루틴을 사용하여 메인 스레드에서 비동기 작업을 수행
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.updateRecordingState(isRecording = false)
            viewModel.updateBeepingState(isBeeping = true) // 비프음 재생 시작 전에 true로 설정

            // 4박자에 대한 카운트 다운
            val totalDelay = WavConsts.TOT_DELAY // 총 지연 시간
            val totalInterval = WavConsts.TOT_INTERVAL // 로그를 기록할 간격 시간
            val beepInterval = WavConsts.BEEP_INTERVAL
            var totalElapsedTime = 0L // 경과 시간
            val startCountUpMoment = WavConsts.START_CNT_UP_MOM
            val startBarMoment = WavConsts.START_BAR_MOM

            while (totalElapsedTime < totalDelay) {
                //카운트 다운
                if (totalElapsedTime <= 1800L && totalElapsedTime % 600L == 0L) {
                    val newSecond = (4.0 - totalElapsedTime / beepInterval).toInt()
                    viewModel.updateCountDownSecond(newSecond)

                    // 메트로놈 소리
                    Log.d("startBar", "countDown ${totalElapsedTime}")
                    playBeep()
                }

                //녹음 시작 시점
                if (totalElapsedTime == startCountUpMoment) {
                    viewModel.updateCountDownSecond(0) // 녹음이 시작되면 카운트다운을 0으로 바꿈

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
                }

                //프로세스바가 시작되어야하는 시점, 2200L
                if (totalElapsedTime >= startBarMoment) {
                    viewModel.updateBarSecond((totalElapsedTime - startBarMoment) / 1000.0)
                }

                //카운트 업
                if (totalElapsedTime >= startCountUpMoment) {
                    viewModel.updateRecordSecond((totalElapsedTime - startCountUpMoment) / 1000.0) // 초 업데이트는 200L 간격으로

                    if (totalElapsedTime % beepInterval == 0L) { // 비프음은 600L 간격으로
                        Log.d("startBar", "countUp ${totalElapsedTime}")
                        playBeep()
                    }
                }

                delay(totalInterval) // 지정된 간격만큼 대기
                totalElapsedTime += totalInterval // 경과 시간 업데이트
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

        Log.d("intErr","feedbackNoteListPyObject ${feedbackNoteListPyObject?.size}")

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
//            Log.d("File Name", file.name)
        }
    }
}
