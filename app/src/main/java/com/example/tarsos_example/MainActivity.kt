package com.example.tarsos_example

import DrawFeedBackNotes
import DrawNotes
import DrawProcessBar
import DrawSheet
import ShowChords
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
import androidx.compose.ui.unit.dp

import be.tarsos.dsp.io.TarsosDSPAudioFormat
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.nio.ByteOrder
import android.Manifest
import android.content.Context
import androidx.activity.viewModels
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.tarsos_example.consts.NoteTypes
import com.example.tarsos_example.model.MyViewModel
import java.io.File
import java.io.IOException
import kotlin.random.Random

var tarsosDSPAudioFormat: TarsosDSPAudioFormat? = null

class MainActivity : ComponentActivity() {
    private lateinit var audioProcessorHandler: AudioProcessorHandler

    // ViewModel 인스턴스를 가져옴
    private val viewModel: MyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 권한 요청
        requestRecordAudioPermission()

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(applicationContext));
        }

        setContent {
            Tarsos_exampleTheme {
                val feedbackNoteListState = viewModel.feedbackNoteList.collectAsState()
                val recordSecondState = viewModel.recordSecond.collectAsState()
                val countDownSecondState = viewModel.countDownSecond.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainActivityUI(
                        feedbackNoteList = feedbackNoteListState,
                        recordSecond = recordSecondState,
                        countDownSecond = countDownSecondState,
                        name = "Android"
                    )
                    SetupTarsosDSP()
                }
            }
        }

        // AudioProcessorHandler 인스턴스 생성
        audioProcessorHandler = AudioProcessorHandler(applicationContext)
    }

    @Composable
    fun MainActivityUI(
        feedbackNoteList: State<List<Int>>,
        recordSecond: State<Double>,
        countDownSecond: State<Int>,
        name: String,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(text = recordSecond.value.toString()) // 녹음 시작후 지난 초 표시
                FloatingActionButton(
                    onClick = { viewModel.init() }
                ) {
                    Text(
                        text = "INIT",
                        style = TextStyle(fontSize = 50.sp)
                    )
                }
                FloatingActionButton(
                    onClick = { audioProcessorHandler.SetupAudioProcessing(viewModel) }
                ) {
                    Text(
                        text = "연주 시작",
                        style = TextStyle(fontSize = 50.sp)
                    )
                }
            }
            Text(
                text = "Count Down : ${countDownSecond.value}",
                style = TextStyle(fontSize = 20.sp)
            )

            Spacer(modifier = Modifier.fillMaxHeight(0.1f)) // 전체의 10% 공백

            ShowChords(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.fillMaxHeight(0.05f)) // 전체의 30% 공백

            // Box를 사용하여 DrawSheet와 DrawNotes를 겹치게 함, 표출 악보
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            ) {
                DrawSheet(modifier = Modifier.matchParentSize()) // 악보 그림
                DrawNotes(
                    viewModel = viewModel,
                    location = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) // 음표 그림
                DrawNotes(
                    viewModel = viewModel,
                    location = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) // 음표 그림
                DrawFeedBackNotes(
                    feedbackNoteList = feedbackNoteList.value, location = 1, modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                DrawProcessBar(
                    seconds = recordSecond.value,
                    modifier = Modifier.matchParentSize()
                )
                //TODO: 음표 3개 중 2개 랜덤 표출

                //TODO: 그자리에 정답이면 초록색, 틀리면 빨간색으로
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

