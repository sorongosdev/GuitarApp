package com.example.tarsos_example.consts

object WavConsts {
    /**청크 개수*/
    const val CHUNK_CNT: Int = 24
    const val FEEDBACK_CHUNK_CNT: Int = 72

    const val BAR_PERIOD = 5000L

    /**카운트다운, 카운트업을 포함한 전체 타이머 길이*/
    const val TOT_DELAY: Long = 7200L

    /**전체 타이머 인터벌*/
    const val TOT_INTERVAL: Long = 100L

    const val BEEP_INTERVAL: Long = 600L

    /**카운트다운할 때 비프음이 울리는 위치에 해당하는 시점 리스트*/
    val countDownList = listOf<Long>(0,600L,1200L,1800L)

    /**카운트업할 때 비프음이 울리는 위치에 해당하는 시점 리스트*/
    val countUpList = listOf<Long>(2400L,3000L,3600L,4200L,4800L,5400L,6000L,6600L)

    /**프로세스바가 시작하는 시점*/
    const val START_BAR_MOM = 2200L

    const val START_CNT_UP_MOM = 2400L
}
