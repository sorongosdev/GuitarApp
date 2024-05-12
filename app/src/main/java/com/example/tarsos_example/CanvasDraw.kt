// ScoreCanvas.kt 파일 내부

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

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
        val startX = if (location == 1) {
            1 * (size.width / 10) // 첫번째 마디 시작점
        } else {
            5 * (size.width / 10) // 두번째 마디 시작점
        }
        val measure_width = 5 * (size.width / 10) - 1 * (size.width / 10)
        val startY = size.height * 4 / 5 - 15
        val endY = size.height * 4 / 5 + 15

        // for 문 간소화
        listOf(1, 4, 7, 10).forEach { i -> // 0,1 / 1,4 / 2,7 / 3,10 // 3n+1 = i // 여기서 n은 noteType에서의 인덱스
            val noteIndex = (i-1)/3
            if (noteType[noteIndex] == 1) {
                val xOffset = startX + i * (measure_width / 13)
                drawLine(
                    color = Color.Black,
                    start = Offset(x = xOffset, y = startY),
                    end = Offset(x = xOffset, y = endY),
                    strokeWidth = Stroke.DefaultMiter
                )
            }
        }
    }
}
