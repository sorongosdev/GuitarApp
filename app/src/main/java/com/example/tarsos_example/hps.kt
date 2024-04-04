package com.example.tarsos_example

import kotlin.math.*
import org.jtransforms.fft.DoubleFFT_1D
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.max

// Path
val path = "./"
val filename = "G_AcusticPlug26_1.wav"
val noteThreshold = 5_000.0 // 120 // 50_000.0 // 3_000.0

// Parameters
val sampleRate = 44100 // Sampling Frequency
val fftLen = 22050 // 2048 // Length of the FFT window
val overlap = 0.5 // Hop overlap percentage between windows
val hopLength = (fftLen * (1 - overlap)).toInt() // Number of samples between successive frames

// For the calculations of the music scale.
val twelveRootOf2 = 2.0.pow(1.0 / 12)

fun readWavFile(path: String, filename: String): Pair<Int, FloatArray> {
    val file = File(path, filename)
    val inputStream = file.inputStream()

    // 프레임 및 샘플레이트를 읽기 위한 준비 (여기서는 간단화를 위해 생략)
    val sampleRate = 44100 // 예시로 사용
    val numFrames = file.length().toInt() // 실제 사용시 정확한 프레임 수를 계산해야 함

    // WAV 파일의 프레임을 읽어옴
    val wavBytes = inputStream.readBytes()
    val wavShorts = ShortArray(wavBytes.size / 2)
    ByteBuffer.wrap(wavBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(wavShorts)

    // int16을 float64로 변환
    val signalArray = FloatArray(wavShorts.size)
    for (i in wavShorts.indices) {
        signalArray[i] = wavShorts[i] / 32768.0f
    }

    println("file_name: $filename")
    println("sample_rate: $sampleRate")
    println("input_buffer.size: ${signalArray.size}")
    println("seconds: ${String.format("%.4f", signalArray.size / sampleRate.toFloat())} s")
    println("type [-1, 1]: ${signalArray::class.java.simpleName}")
    println("min: ${String.format("%.4f", signalArray.minOrNull() ?: 0f)} max: ${String.format("%.4f", signalArray.maxOrNull() ?: 0f)}")

    return Pair(sampleRate, signalArray)
}



// Kotlin에서는 toStrF4 함수의 기능을 "${"%.4f".format(value)}"로 대체할 수 있음

// 주어진 버퍼를 최대 길이로 나누어 겹치지 않는 청크(작은 조각)로 분할, 분할된 배열 뷰 반환
fun divideBufferIntoNonOverlappingChunks(buffer: FloatArray, maxLen: Int): List<DoubleArray> {
    // 버퍼의 최대 길이 확인
    val bufferLen = buffer.size
    // 최대길이로 나누어질 수 있는 청크의 수 계산
    val chunks = bufferLen / maxLen
    println("buffers_num: $chunks")
    // 분할 지점을 나타내는 인덱스 리스트 생성
    val divisionPtsList = mutableListOf<Int>()
    for (i in 1 until chunks) {
        divisionPtsList.add(i * maxLen)
    }
    // 버퍼를 분할 지점으로 나누어 배열 뷰를 생성
    val splittedArrayView = mutableListOf<DoubleArray>()
    var startIdx = 0
    for (divisionPt in divisionPtsList) {
        // FloatArray를 DoubleArray로 변환하여 추가
        splittedArrayView.add(buffer.copyOfRange(startIdx, divisionPt).map { it.toDouble() }.toDoubleArray())
        startIdx = divisionPt
    }
    // 마지막 청크 추가 (FloatArray를 DoubleArray로 변환)
    if (startIdx < bufferLen) {
        splittedArrayView.add(buffer.copyOfRange(startIdx, bufferLen).map { it.toDouble() }.toDoubleArray())
    }
    // 분할된 뷰 반환
    return splittedArrayView
}


fun getFFT(data: DoubleArray, sampleRate: Double): Triple<DoubleArray, DoubleArray, Int> {
    // 데이터 길이 확인
    val lenData = data.size
    // 해밍 윈도우 함수 적용
    val windowedData =
        data.indices.map { data[it] * (0.54 - 0.46 * Math.cos(2 * Math.PI * it / (lenData - 1))) }
            .toDoubleArray()
    // fft 계산을 위한 배열 준비 (실수부와 허수부를 모두 포함해야 함)
    val fftData = DoubleArray(lenData * 2)
    System.arraycopy(windowedData, 0, fftData, 0, lenData)
    // fft 실행
    val fft = DoubleFFT_1D(lenData.toLong())
    fft.realForwardFull(fftData)
    // fft 결과 절대값 계산
    val absFFT =
        DoubleArray(lenData) { Math.sqrt(fftData[2 * it] * fftData[2 * it] + fftData[2 * it + 1] * fftData[2 * it + 1]) }
    // fft 주파수 계산
    val freq = DoubleArray(lenData) { it * sampleRate / lenData }
    return Triple(freq, absFFT, absFFT.size)
}

fun removeDcOffset(fftRes: DoubleArray): DoubleArray {
    // FFT 결과에서 DC 오프셋 제거 (첫 번째, 두 번째, 세 번째 bin의 값을 0으로 설정)
    // fftRes는 주파수 성분의 크기를 나타내는 복소수 형태로 표현될 것으로 예상되지만,
    // 복소수 표현을 위해 별도의 데이터 구조를 사용하지 않는 한, 이 예제에서는 DoubleArray로 간주합니다.
    // 실제 복소수 배열을 다루는 경우, 복소수를 표현하기 위한 라이브러리 사용 또는 별도의 데이터 구조 정의가 필요합니다.
    if (fftRes.size >= 3) {
        fftRes[0] = 0.0
        fftRes[1] = 0.0
        fftRes[2] = 0.0
    }
    return fftRes
}

fun freqForNote(baseNote: String, noteIndex: Int): Double {
    val A4 = 440.0
    val TWELVE_ROOT_OF_2 = 2.0.pow(1.0 / 12.0)

    val baseNotesFreq = mapOf(
        "A2" to A4 / 4,   // 110.0 Hz
        "A3" to A4 / 2,   // 220.0 Hz
        "A4" to A4,       // 440.0 Hz
        "A5" to A4 * 2,   // 880.0 Hz
        "A6" to A4 * 4    // 1760.0 Hz
    )

    val scaleNotes = mapOf(
        "C" to -9.0,
        "C#" to -8.0,
        "D" to -7.0,
        "D#" to -6.0,
        "E" to -5.0,
        "F" to -4.0,
        "F#" to -3.0,
        "G" to -2.0,
        "G#" to -1.0,
        "A" to 1.0,
        "A#" to 2.0,
        "B" to 3.0,
        "Cn" to 4.0
    )

    val scaleNotesIndex = (-9..4).toList() // Has one more note.
    val noteIndexValue = scaleNotesIndex[noteIndex]
    val freq0 = baseNotesFreq[baseNote] ?: error("Unknown base note: $baseNote")
    val freq = freq0 * TWELVE_ROOT_OF_2.pow(noteIndexValue)
    return freq
}

fun getAllNotesFreq(): List<Pair<String, Double>> {
    val orderedNoteFreq = mutableListOf<Pair<String, Double>>()
    val orderedNotes = listOf(
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    )
    // 2옥타브~6옥타브까지 각 음계의 주파수 계산
    for (octaveIndex in 2..6) {
        val baseNote = "A$octaveIndex"
        for (noteIndex in 0..11) {
            val noteFreq = freqForNote(baseNote, noteIndex)
            val noteName = "${orderedNotes[noteIndex]}_$octaveIndex" // 코드_숫자로 음정 표시
            orderedNoteFreq.add(Pair(noteName, noteFreq))
        }
    }
    return orderedNoteFreq
}

fun findNearestNote(orderedNoteFreq: List<Pair<String, Double>>, freq: Double): String {
    var finalNoteName = "note_not_found"
    // 노트까지의 최소 거리를 저장하는 변수
    var lastDist = 1_000_000.0
    // orderedNoteFreq 리스트를 순회하면서 각 음표의 이름과 주파수를 가져옴
    for ((noteName, noteFreq) in orderedNoteFreq) {
        // 현재 음표와 목표 주파수 간의 거리(currDist)를 계산
        val currDist = kotlin.math.abs(noteFreq - freq)
        // 현재 음표가 더 가까운 경우에는 lastDist를 currDist로 업데이트하고 finalNoteName을 현재 음표의 이름으로 업데이트
        if (currDist < lastDist) {
            lastDist = currDist
            finalNoteName = noteName
            // 더 이상 가까운 음표를 찾을 수 없을 때 순회 종료
        } else if (currDist > lastDist) {
            break
        }
    }
    return finalNoteName
}

// 주파수의 최댓값을 찾는 함수
fun pitchSpectralHps(
    X: DoubleArray,
    freqBuckets: DoubleArray,
    fS: Double,
    bufferRms: Double
): List<Pair<Double, Double>> {
    val iOrder = 4
    val fMin = 65.41 // C2
    val iLen = (X.size - 1) / iOrder
    val afHps = DoubleArray(iLen) { X[it] } // X가 이제 1차원 배열이므로 직접 접근
    val kMin = round(fMin / fS * 2 * (X.size - 1)).toInt()

    // HPS 계산
    for (j in 1 until iOrder) {
        val Xd = DoubleArray(iLen) { X[it * j] } // 1차원 배열에서 간격을 j로 해서 새 배열 생성
        for (i in 0 until iLen) {
            afHps[i] *= Xd[i]
        }
    }

    // 주파수 임계값 계산
    val noteThreshold = noteThresholdScaledByRMS(bufferRms) // 해당 함수 구현 필요

    // afHps에서 임계값보다 큰 값 찾기
    val allFreq = afHps.mapIndexed { index, value ->
        if (index >= kMin && value > noteThreshold) index else null
    }.filterNotNull() // null이 아닌 값만 필터링

    // 최대 값과 인덱스 찾기
    val maxIndex = allFreq.maxByOrNull { afHps[it] } ?: -1
    val maxValue = if (maxIndex != -1) afHps[maxIndex] else 0.0

    println("buffer_rms: $bufferRms")
    println("max_value: $maxValue  max_index: $maxIndex")
    println("note_threshold: $noteThreshold")

    // 결과 반환
    return allFreq.map { freqIndex ->
        Pair(freqIndex.toDouble(), afHps[freqIndex])
    }
}


fun noteThresholdScaledByRMS(bufferRms: Double): Double {
    val noteThreshold = 1000.0 * (4 / 0.090) * bufferRms
    return noteThreshold
}

fun normalize(arr: DoubleArray): DoubleArray {
    // 배열의 최대값 찾기
    val maxVal = arr.maxOrNull() ?: 0.0
    // 배열 정규화
    return arr.map { it / (maxVal / 2) - 1 }.toDoubleArray()
}

fun toStrF(value: Double): String {
    // 소수점 없이 부동소수점 숫자를 문자열로 변환
    return "%.0f".format(value)
}

fun toStrF4(value: Double): String {
    // 소수점 아래 4자리까지의 부동소수점 숫자를 문자열로 변환
    return "%.4f".format(value)
}

fun main() {
    println("\nPolyphonic note detector\n")

    // 음정 반환
    val orderedNoteFreq = getAllNotesFreq()

    // WAV 파일을 읽고 신호를 버퍼로 반환. null일 경우 함수를 빠져나온다.
    val (sampleRateFile, inputBuffer) = readWavFile(path, filename) ?: return
    // 주어진 버퍼를 최대 길이로 나누어 겹치지 않는 청크(작은 조각)로 분할
    val bufferChunks = divideBufferIntoNonOverlappingChunks(inputBuffer, fftLen)

    var count = 0

    // 청크 처리
    for (chunk in bufferChunks.take(60)) {
        println("\n...Chunk: $count")

        // fft 결과 반환
        val (fftFreq, fftRes, fftResLen) = getFFT(chunk, chunk.size.toDouble())
        // dc 오프셋 제거
        val fftResNoDC = removeDcOffset(fftRes)

        // RMS 계산
        val bufferRms = sqrt(chunk.map { it * it }.average())

        val allFreqs = pitchSpectralHps(fftResNoDC, fftFreq, sampleRateFile.toDouble(), bufferRms)

        for (freq in allFreqs) {
            val noteName = findNearestNote(orderedNoteFreq, freq.first)
            println("=> freq: ${toStrF(freq.first)} Hz  value: ${toStrF(freq.second)} note_name: $noteName")
        }

        // 여기서부터의 시각화 및 배열 출력 관련 코드는 Kotlin에서 직접적인 대응이 없으므로 생략

        count++
    }
}

