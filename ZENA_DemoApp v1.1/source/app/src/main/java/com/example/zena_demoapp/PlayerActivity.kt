package com.example.zena_demoapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.zena_demoapp.databinding.ActivityPlayerBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet


class PlayerActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityPlayerBinding
    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    var isrunning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_player)
        viewBinding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // graph
        val thread = ThreadClass()
        thread.start()

        // choose video ...
        val video_flag: Int = intent.getIntExtra("video_flag", -1)
        RefVideoLoad(video_flag)

        // play or pause button
        var isPlay = true
        viewBinding.imageButton.setOnClickListener({
            // icon-ref.: https://www.flaticon.com/kr/packs/music-player-icons?word=player
            if (isPlay) {   //일시정지 눌렀을 때
                isPlay = false
                isrunning = false
                thread.interrupt()
                viewBinding.imageButton.setImageResource(R.drawable.play)
                viewBinding.videoViewRef.pause()
            }
            else {
                isPlay = true
                isrunning = true
                viewBinding.imageButton.setImageResource(R.drawable.pause)
                viewBinding.videoViewRef.start()
                val thread = ThreadClass()
                thread.start()
            }
        })
        viewBinding.videoViewRef.setOnCompletionListener({
            isPlay = false
            isrunning = false
            thread.interrupt()
//            thread!!.interrupt()
            viewBinding.imageButton.setImageResource(R.drawable.play)
        })


        // exit button
        viewBinding.button.setOnClickListener({ finish() })


        // ON/OFF button
        var ischecked = false
        viewBinding.toggle.setOnClickListener({
            // Ref.: https://aries574.tistory.com/213
            if (ischecked) {
                ischecked = false
                Toast.makeText(this.getApplicationContext(),"ON", Toast.LENGTH_SHORT).show() // On일 경우
            }
            else {
                ischecked = true
                Toast.makeText(this.getApplicationContext(),"OFF", Toast.LENGTH_SHORT).show() // Off일 경우
            }
        })


        // ########CAMERA#########
        // Request camera permissions
        if (allPermissionsGranted()) {
            viewBinding.videoViewUser.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

    }

    private fun RefVideoLoad(video_flag: Int): Int? {
        // Ref.: https://jwsoft91.tistory.com/144

        val video_path_root = "android.resource://" + packageName + '/'
        var video_path: String = ""
        when (video_flag) {
            1 -> video_path = video_path_root + R.raw.hueji
            2 -> video_path = video_path_root + R.raw.zzon_ddeok
            else -> {
                println("Error: Video open error")
                return -1
            }
        }

        val uri: Uri = Uri.parse(video_path)
        viewBinding.videoViewRef.setVideoURI(uri)

        // Ref.: https://ggugguchang.tistory.com/53
        viewBinding.videoViewRef.setOnPreparedListener(OnPreparedListener { //재생 준비가 완료 되었을 때
            viewBinding.seekBar.setMax(viewBinding.videoViewRef.getDuration())
        })
        handler.sendEmptyMessageDelayed(0, 0)

        viewBinding.videoViewRef.start()
        return null
    }

    var handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                0 -> {
                    this.sendEmptyMessageDelayed(0, 10)
                    val position = viewBinding.videoViewRef.getCurrentPosition()
                    viewBinding.seekBar.setProgress(position)
                    viewBinding.textView.setText(stringForTime(position))
                }
            }
        }
    }

    private fun stringForTime(timeMs: Int): String? {
        // Ref.: https://blog.naver.com/PostView.nhn?blogId=mym0404&logNo=221344421423&parentCategoryNo=&categoryNo=43&viewDate=&isShowPopularPosts=false&from=postView
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60

        return String.format("%02d:%02d", minutes, seconds)
    }


    // ###########CAMERA###########
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.videoViewUser.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    // #############GRAPH############
    inner class ThreadClass : Thread() {
        override fun run() {
            val input = Array<Double>(50, { Math.random() })

            var entries: ArrayList<Entry> = ArrayList() // Entry 배열 생성
            entries.add(Entry(0F, 0F)) // Entry 배열 초기값 입력
            var dataset: LineDataSet = LineDataSet(entries, "input")    // 그래프 구현을 위한 LineDataSet 생성
            var data: LineData = LineData(dataset)  // 그래프 data 생성 -> 최종 입력 데이터

            dataset.mode = LineDataSet.Mode.LINEAR
            dataset.setDrawCircles(false)
            dataset.highLightColor = Color.rgb(190, 190, 190)

            viewBinding.lineChart.data = data   // chart.xml에 배치된 lineChart에 데이터 연결

            runOnUiThread {
                viewBinding.lineChart.animateXY(1, 1)   // 그래프 생성

            }

    //            for (i in 0 until input.size) {  // 그래프 표현식
            var i = 0
            while(isrunning) {
                try {
                    SystemClock.sleep(100)
                    Thread.sleep(10)
                    data.addEntry(Entry(i.toFloat(), input[i].toFloat()), 0)
                    data.notifyDataChanged()
                    viewBinding.lineChart.notifyDataSetChanged()
                    viewBinding.lineChart.invalidate()
                    viewBinding.lineChart.setVisibleXRangeMaximum(100.toFloat())
                    viewBinding.lineChart.moveViewToX(data.entryCount.toFloat())
    //                binding.lineChart.moveViewTo(data.entryCount, 50f, YAxis.AxisDependency.LEFT)
                    viewBinding.lineChart.invalidate()
                    i+=1
                }
                catch(e : ArrayIndexOutOfBoundsException){
                    e.printStackTrace();
 //                    isrunning = false
                }
                catch (i : InterruptedException) {
                    i.printStackTrace();
                    return; // 인터럽트 받을 경우 return됨
                }
            }

        }
    }

}