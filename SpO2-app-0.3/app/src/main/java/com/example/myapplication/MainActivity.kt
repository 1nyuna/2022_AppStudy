package com.example.myapplication


//import com.android.example.cameraxapp.databinding.ActivityMainBinding


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.databinding.ActivityMainBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_result.*
import kotlinx.android.synthetic.main.activity_camera.*
import java.util.*
import java.util.concurrent.ExecutorService


class MainActivity : AppCompatActivity() {

    private val chart: LineChart? = null

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    val REQUEST_GALLERY = 12
    val PERMISSION_STORAGE = 9 // 앨범 권한 처리

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding =
            ActivityMainBinding.inflate(layoutInflater)   //viewBinding 클래스의 inflate 메서드로 액티비티에서 사용할 인스턴스 생성
        setContentView(viewBinding.root)    // getRoot 메서드로 레이아웃 내부의 최상위 뷰의 인스턴스를 활용하여 생성된 뷰를 액티비티에 표시
        val VISIVILITY_GALLERY =
            arrayOf(viewBinding.viewVideo, viewBinding.btnStart, viewBinding.btnOpen)

        // 권한 요청
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        // viewBinding으로 뷰 id 접근이 가능해짐, 뷰 id는 tv_message -> tvMessage로 자동 변환
        viewBinding.btnStart.setOnClickListener {
            startActivity(Intent(this@MainActivity, CameraActivity::class.java))
//            Toast.makeText(this, "카메라 켜는 중!", Toast.LENGTH_SHORT).show()
        }

        viewBinding.btnOpen.setOnClickListener {
            if (allPermissionsGranted()) {
                val intent =
                    Intent(Intent.ACTION_PICK) //ACTION_PICK -> 선택할 수 있는 창이 나옴. 어떤 데이터를 선택할건지 정하게됨
                intent.type = "video/*" //이미지는 image/*
//        intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        intent.action = Intent.ACTION_GET_CONTENT //갤러리가 아니라 파일로 보여쥼

                startActivityForResult(intent, REQUEST_GALLERY)
                changeVisivility(VISIVILITY_GALLERY)

            } else {
                ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
//                ActivityCompat.requestPermissions(this, (arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_STORAGE)
            }

            viewBinding.viewVideo.setOnCompletionListener {
                changeVisivility(VISIVILITY_GALLERY)
                startActivity(Intent(this, ResultActivity::class.java))
            }


        }
        viewBinding.textViewTest.setText(stringFromJNI())
        viewBinding.textViewTest.setText(addition(1, 2).toString())

    }

    external fun stringFromJNI(): String
    external fun addition(a: Int, b: Int): Int


    private fun changeVisivility(v: Array<View>) {
        for (i in 0 until v.size) {
            if (v[i].visibility == View.VISIBLE)
                v[i].visibility = View.INVISIBLE
            else if (v[i].visibility == View.INVISIBLE)
                v[i].visibility = View.VISIBLE
        }
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {  //프래그먼트가 파괴될때
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }

    //카메라 요청 승인,거절을 누르면 아래 함수 호출
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) { //requestCode 확인
            if (allPermissionsGranted()) {  //권한이 있다면 카메라실행
//                startCamera()
            } else { //권한이 없다면 사용자에게 권한없음을 알림
                Toast.makeText(this, "권한을 승인해주셔야 합니다!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /** 사용자가 권한을 승인하거나 거부한 다음에 호출되는 메서드
     * @param requestCode 요청한 주체를 확인하는 코드
     * @param permissions 요청한 권한 목록
     * @param grantResults 권한 목록에 대한 승인/미승인 값, 권한 목록의 개수와 같은 수의 결괏값이 전달된다.
     * */


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_GALLERY -> {
                    data?.data?.let { uri ->
                        val imageUri: Uri? = data?.data
                        viewBinding.viewVideo.setVideoPath(imageUri.toString()) // 선택한 비디오를 videoView에 setting
                        viewBinding.viewVideo.start()
//                        viewBinding.backImg.setImageURI(imageUri)
                    }
                } //data?의 data에 uri형태로 이미지 주소가 담겨이따!
            }
        }
    }
}