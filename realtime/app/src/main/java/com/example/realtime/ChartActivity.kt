package com.example.realtime

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import com.example.realtime.databinding.ActivityChartBinding
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet


class ChartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChartBinding
    var isrunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChartBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)    //뷰바인딩


        binding.startButton.setOnClickListener {
            if (isrunning == false) {
                isrunning = true
                binding.startButton.text = "그래프 구현중"
                binding.startButton.isClickable = false
                val thread = ThreadClass()
                thread.start()
            }
        }

        binding.backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    inner class ThreadClass : Thread() {
        override fun run() {
            val input = Array<Double>(1000, { Math.random() })

            var entries: ArrayList<Entry> = ArrayList() // Entry 배열 생성
            entries.add(Entry(0F, 0F)) // Entry 배열 초기값 입력
            var dataset: LineDataSet = LineDataSet(entries, "input")    // 그래프 구현을 위한 LineDataSet 생성
            var data: LineData = LineData(dataset)  // 그래프 data 생성 -> 최종 입력 데이터

            dataset.mode = LineDataSet.Mode.LINEAR
            dataset.setDrawCircles(false)
            dataset.highLightColor = Color.rgb(190, 190, 190)

            binding.lineChart.data = data   // chart.xml에 배치된 lineChart에 데이터 연결

            runOnUiThread {
                binding.lineChart.animateXY(1, 1)   // 그래프 생성

            }

            for (i in 0 until input.size) {  // 그래프 표현식

                SystemClock.sleep(10)
                data.addEntry(Entry(i.toFloat(), input[i].toFloat()), 0)
                data.notifyDataChanged()
                binding.lineChart.notifyDataSetChanged()
                binding.lineChart.invalidate()
                binding.lineChart.setVisibleXRangeMaximum(100.toFloat())
                binding.lineChart.moveViewToX(data.entryCount.toFloat())
//                binding.lineChart.moveViewTo(data.entryCount, 50f, YAxis.AxisDependency.LEFT)
                binding.lineChart.invalidate()

            }
            binding.startButton.text = "난수 생성 시작"
            binding.startButton.isClickable = true

            isrunning = false

        }
    }
}