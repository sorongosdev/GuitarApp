/* **************
* CanvasDrawCanvasDraw.kt
*
* Canvas 를 이용해서 악보를 그려주는 부분
* *******************/
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.sqrt

/**악보 전체 틀을 그려주는 함수*/
@Composable
fun DrawSheet(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // 예제: 악보 선 그리기
        val startY = 0f
        val endY = size.height
        for (i in 1..9) {
            if (i == 1 || i == 5 || i == 9) {
                val xOffset = i * (size.width / 10)
                drawLine(
                    color = Color.Black,
                    start = Offset(x = xOffset, y = startY),
                    end = Offset(x = xOffset, y = endY),
                    strokeWidth = Stroke.DefaultMiter
                )
            }
        }
        // 세로줄의 위에서 4/5 지점에 가로줄 그리기
        val threeQuartersY = startY + (endY - startY) * 4 / 5
        drawLine(
            color = Color.Black,
            start = Offset(x = 1 * (size.width / 10), y = threeQuartersY), // 가로줄 시작점 조정
            end = Offset(x = 9 * (size.width / 10), y = threeQuartersY), // 가로줄 끝점 조정
            strokeWidth = Stroke.DefaultMiter
        )
    }
}

/**noteType, 그리는 위치(1또는 2)를 받아 음표를 그려주는 함수*/
@Composable
fun DrawNotes(noteType: List<Int>, location: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val startX_measure = if (location == 1) {
            1 * (size.width / 10) // 첫번째 마디 시작점
        } else {
            5 * (size.width / 10) // 두번째 마디 시작점
        }
        val measure_width = 4 * (size.width / 10) // 한 마디 넓이
        val startY_tail = size.height * 1 / 5 // 꼬리가 시작되는 Y지점
        val endY_tail = size.height * 4 / 5 // 꼬리가 끝나는 Y지점
        val centerY = size.height * 4 / 5 // 선의 중심점
        val lineLength = sqrt(2f) * 25f // 45도 각도에서의 선 길이, 대각선 길이 계산

        // for 문 간소화
        listOf(1, 4, 7, 10).forEach { i ->
            val noteIndex = (i - 1) / 3 // { 0,1 / 1,4 / 2,7 / 3,10 } =======> { 3n+1 = i }
            if (noteType[noteIndex] == 1) {
                val xOffset = startX_measure + i * (measure_width / 13) // 음표가 그려지는 곳

                drawLine(
                    color = Color.Black,
                    start = Offset(x = xOffset + lineLength / 2, y = startY_tail),
                    end = Offset(x = xOffset + lineLength / 2, y = endY_tail - lineLength / 2),
                    strokeWidth = Stroke.DefaultMiter
                )

                // 45도 기울인 선을 그리기 위한 시작점과 끝점 계산
                val start = Offset(x = xOffset - lineLength / 2, y = centerY + lineLength / 2)
                val end = Offset(x = xOffset + lineLength / 2, y = centerY - lineLength / 2)
                drawLine(
                    color = Color.Black,
                    start = start,
                    end = end,
                    strokeWidth = Stroke.DefaultMiter
                )
            }
        }
    }
}

@Composable
fun DrawFeedBackNotes(feedbackNoteList: List<Int>?, location: Int, modifier: Modifier) {
    Log.d("undraw", "DrawFeedBackNotes, List length: ${feedbackNoteList?.size}")
    if (!feedbackNoteList.isNullOrEmpty()) {
        Canvas(modifier = modifier) {
            val startX_measure = 1 * (size.width / 10) // 첫번째 마디 시작점
            val measure_width = 8 * (size.width / 10) // 두 마디 넓이
            val startY_tail = size.height * 1 / 5 // 꼬리가 시작되는 Y지점
            val endY_tail = size.height * 4 / 5 // 꼬리가 끝나는 Y지점
            val centerY = size.height * 4 / 5 // 선의 중심점
            val lineLength = sqrt(2f) * 25f // 45도 각도에서의 선 길이, 대각선 길이 계산

            for (i in 1..24) {
                if (feedbackNoteList[i] != 0) { // 받은 리스트의 값이 0이 아니면 친 곳
                    val xOffset = startX_measure + i * (measure_width / 26) // 음표가 그려지는 곳

                    drawLine(
                        color = Color.Red,
                        start = Offset(x = xOffset + lineLength / 2, y = startY_tail),
                        end = Offset(x = xOffset + lineLength / 2, y = endY_tail - lineLength / 2),
                        strokeWidth = 10f
                    )

                    // 45도 기울인 선을 그리기 위한 시작점과 끝점 계산
                    val start = Offset(x = xOffset - lineLength / 2, y = centerY + lineLength / 2)
                    val end = Offset(x = xOffset + lineLength / 2, y = centerY - lineLength / 2)
                    drawLine(
                        color = Color.Red,
                        start = start,
                        end = end,
                        strokeWidth = 100f
                    )
                }
            }
        }
    }
}

@Composable
fun ShowChords(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val startX_measure1 = 1 * (size.width / 10) // 첫번째 마디 시작점
        val startX_measure2 = 5 * (size.width / 10) // 두번째 마디 시작점

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK // 텍스트 색상 설정
            textSize = 40f // 텍스트 크기 설정
        }

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                "텍스트",
                startX_measure1, // x 좌표
                0f, // y 좌표
                paint // Paint 객체
            )
            canvas.nativeCanvas.drawText(
                "텍스트",
                startX_measure2, // x 좌표
                0f, // y 좌표
                paint // Paint 객체
            )
        }
    }
}