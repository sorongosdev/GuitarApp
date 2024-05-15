package com.example.tarsos_example

import DrawFeedBackNotes
import DrawNotes
import DrawSheet
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.tarsos_example.ui.theme.Tarsos_exampleTheme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.ui.unit.dp

import be.tarsos.dsp.io.TarsosDSPAudioFormat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.nio.ByteOrder
import android.Manifest
import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Note
import android.util.Log
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.tarsos_example.consts.NoteTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

var tarsosDSPAudioFormat: TarsosDSPAudioFormat? = null

class MainActivity : ComponentActivity() {
    private lateinit var audioProcessorHandler: AudioProcessorHandler

    // noteListState를 정의하고 초기값을 빈 리스트로 설정합니다.
    val feedbackNoteListState = MutableStateFlow(List(25) { 0 })

    // ViewModel 인스턴스를 가져오기
//    private val viewModel by viewModels<MyViewModel>()

    // ViewModel 인스턴스를 가져옴
    private val viewModel: MyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val viewModel by viewModels<MyViewModel>()

        // 권한 요청
        requestRecordAudioPermission()

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(applicationContext));
        }
        val py = Python.getInstance()
        val pyObj = py.getModule("example")

        // ViewModel의 feedbackNoteList StateFlow 관찰
        lifecycleScope.launch {
            Log.d("undraw", "lifecycleScope.launch")
            // repeatOnLifecycle을 사용하여 STARTED 상태일 때만 collect가 실행되도록 합니다.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.feedbackNoteList.collect { noteList ->
                    feedbackNoteListState.value = noteList
                    Log.d("undraw", "collect, noteList.size ${noteList.size}")
                }
            }
        }

        setContent {
            Tarsos_exampleTheme {

                val feedbackNoteList by feedbackNoteListState.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainActivityUI(feedbackNoteList = feedbackNoteList, name = "Android")
                    SetupTarsosDSP()
                }
            }
        }

        // AudioProcessorHandler 인스턴스 생성
        audioProcessorHandler = AudioProcessorHandler(applicationContext)
    }

    @Composable
    fun MainActivityUI(feedbackNoteList: List<Int>, name: String, modifier: Modifier = Modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { audioProcessorHandler.SetupAudioProcessing(viewModel) }, // viewModel 내부에서 _feedbackNoteList 상태 업데이트
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "연주 시작")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Box를 사용하여 DrawSheet와 DrawNotes를 겹치게 함, 표출 악보
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                DrawSheet(modifier = Modifier.matchParentSize()) // 악보 그림
                DrawNotes(
                    noteType = NoteTypes.note_1010, // 예시 음표 타입 리스트
                    location = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) // 음표 그림
                DrawNotes(
                    noteType = NoteTypes.note_1011, // 예시 음표 타입 리스트
                    location = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) // 음표 그림
                DrawFeedBackNotes(
                    feedbackNoteList = feedbackNoteList, location = 1, modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

        }
    }

    private fun requestRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        }
    }

    fun readFileBytes(filePath: String): ByteArray? {
        return try {
            val file = File(filePath)
            file.readBytes()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun readAssetFile(context: Context, fileName: String): ByteArray? {
        return try {
            context.assets.open(fileName).use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}


@Composable
fun SetupTarsosDSP() {
    LaunchedEffect(Unit) {
        tarsosDSPAudioFormat = TarsosDSPAudioFormat(
            TarsosDSPAudioFormat.Encoding.PCM_SIGNED,
            22050f,
            2 * 8,
            1,
            2 * 1,
            22050f,
            ByteOrder.BIG_ENDIAN == ByteOrder.nativeOrder()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Tarsos_exampleTheme {
//        MainActivityUI("Android")
    }
}

