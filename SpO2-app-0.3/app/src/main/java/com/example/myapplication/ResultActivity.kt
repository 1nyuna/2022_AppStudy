package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.graphics.Color
import com.example.myapplication.databinding.ActivityCameraBinding
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.databinding.ActivityResultBinding
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_result.*
import kotlinx.android.synthetic.main.activity_camera.*
import java.util.ArrayList


class ResultActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding =
            ActivityResultBinding.inflate(layoutInflater)   //viewBinding 클래스의 inflate 메서드로 액티비티에서 사용할 인스턴스 생성
        setContentView(viewBinding.root)    // getRoot 메서드로 레이아웃 내부의 최상위 뷰의 인스턴스를 활용하여 생성된 뷰를 액티비티에 표시

        viewBinding.btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        Toast.makeText(this, "gd", Toast.LENGTH_SHORT).show()

        val entries = ArrayList<BarEntry>()
//        val entries: List<Map.Entry<*, *>> = ArrayList()
        entries.add(BarEntry(1.2f, 20.0f))
        entries.add(BarEntry(2.2f, 70.0f))
        entries.add(BarEntry(3.2f, 30.0f))
        entries.add(BarEntry(4.2f, 90.0f))
        entries.add(BarEntry(5.2f, 70.0f))
        entries.add(BarEntry(6.2f, 30.0f))
        entries.add(BarEntry(7.2f, 90.0f))

        barChart.run {
            description.isEnabled = false // 차트 옆에 별도로 표기되는 description을 안보이게 설정 (false)
            setMaxVisibleValueCount(7) // 최대 보이는 그래프 개수를 7개로 지정
            setPinchZoom(false) // 핀치줌(두손가락으로 줌인 줌 아웃하는것) 설정
            setDrawGridBackground(false)//격자구조 넣을건지
            axisLeft.run { //왼쪽 축. 즉 Y방향 축을 뜻한다.
                axisMaximum = 101f //100 위치에 선을 그리기 위해 101f로 맥시멈값 설정
                axisMinimum = 0f // 최소값 0
                granularity = 50f // 50 단위마다 선을 그리려고 설정.
                setDrawLabels(true) // 값 적는거 허용 (0, 50, 100)
                setDrawGridLines(true) //격자 라인 활용
                setDrawAxisLine(false) // 축 그리기 설정
                textSize = 13f //라벨 텍스트 크기
            }
            xAxis.run {
                position = XAxis.XAxisPosition.BOTTOM //X축을 아래에다가 둔다.
                granularity = 1f // 1 단위만큼 간격 두기
                setDrawAxisLine(true) // 축 그림
                setDrawGridLines(false) // 격자
                textSize = 12f // 텍스트 크기
//                valueFormatter = SubActivity.MyXAxisFormatter() // X축 라벨값(밑에 표시되는 글자) 바꿔주기 위해 설정
            }
            axisRight.isEnabled = false // 오른쪽 Y축을 안보이게 해줌.
            setTouchEnabled(false) // 그래프 터치해도 아무 변화없게 막음
            animateY(1000) // 밑에서부터 올라오는 애니매이션 적용
            legend.isEnabled = false //차트 범례 설정
        }

        var set = BarDataSet(entries, "DataSet") // 데이터셋 초기화

        val dataSet: ArrayList<IBarDataSet> = ArrayList()
        dataSet.add(set)
        val data = BarData(dataSet)
        data.barWidth = 0.3f //막대 너비 설정
        barChart.run {
            this.data = data //차트의 데이터를 data로 설정해줌.
            setFitBars(true)
            invalidate()
        }


        val values: ArrayList<Entry> = ArrayList()
        values.add(Entry(2.2f, 70.0f))
        values.add(Entry(3.2f, 30.0f))
        values.add(Entry(4.2f, 90.0f))
        values.add(Entry(5.2f, 70.0f))
        values.add(Entry(6.2f, 30.0f))
        values.add(Entry(7.2f, 90.0f))

        val set1: LineDataSet
        set1 = LineDataSet(values, "DataSet 1")

        val dataSets: ArrayList<ILineDataSet> = ArrayList()
        dataSets.add(set1) // add the data sets

        // create a data object with the data sets
        val data2 = LineData(dataSets)

        // black lines and points
        set1.color = Color.BLACK
        set1.setCircleColor(Color.BLACK)

        // set data
        lineChart.run {
            this.data = data2 //차트의 데이터를 data로 설정해줌.
            invalidate()
        }

    }

    inner class MyXAxisFormatter : ValueFormatter() {
        private val days = arrayOf("1차", "2차", "3차", "4차", "5차", "6차", "7차")
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return days.getOrNull(value.toInt() - 1) ?: value.toString()
        }
    }
}