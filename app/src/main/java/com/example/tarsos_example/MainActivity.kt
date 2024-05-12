package com.example.tarsos_example

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import be.tarsos.dsp.io.TarsosDSPAudioFormat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.nio.ByteOrder
import android.Manifest
import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import java.io.IOException
import java.util.Date

var tarsosDSPAudioFormat: TarsosDSPAudioFormat? = null

class MainActivity : ComponentActivity() {
    private lateinit var audioProcessorHandler: AudioProcessorHandler
    private val pitchTextViewValue = mutableStateOf("0")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 권한 요청
        requestRecordAudioPermission()

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(applicationContext));
        }
        val py = Python.getInstance()
        val pyObj = py.getModule("example")

        setContent {
            Tarsos_exampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainActivityUI("Android")
                    SetupTarsosDSP()
                }
            }
        }

        // AudioProcessorHandler 인스턴스 생성
        audioProcessorHandler = AudioProcessorHandler(applicationContext)
        audioProcessorHandler.pitchTextViewValue.observe(this) { value ->
            pitchTextViewValue.value = value  // UI 업데이트 로직
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


    private fun requestRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        }
    }

    @Composable
    fun MainActivityUI(name: String, modifier: Modifier = Modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { audioProcessorHandler.SetupAudioProcessing() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "연주 시작")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { audioProcessorHandler.stopAudioProcessing() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "중지")
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Box를 사용하여 DrawSheet와 DrawNotes를 겹치게 함
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                DrawSheet(modifier = Modifier.matchParentSize()) // 악보 그림
                DrawNotes(
                    noteType = NoteSorts.note_1010, // 예시 음표 타입 리스트
                    location = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) // 음표 그림
                DrawNotes(
                    noteType = NoteSorts.note_1011, // 예시 음표 타입 리스트
                    location = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) // 음표 그림
            }
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

