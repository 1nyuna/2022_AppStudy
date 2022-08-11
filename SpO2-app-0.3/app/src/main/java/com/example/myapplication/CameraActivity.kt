package com.example.myapplication

import android.Manifest
import android.content.ContentValues
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.myapplication.databinding.ActivityCameraBinding
import com.example.myapplication.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_result.*
import kotlinx.android.synthetic.main.activity_camera.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    val viewBinding by lazy { ActivityCameraBinding.inflate(layoutInflater) }
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        viewBinding.btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        viewBinding.btnVideo.setOnClickListener {
            captureVideo()
        }


    }

    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return
        viewBinding.btnVideo.isEnabled = false  //작업이 완료될 때까지 버튼 비활성화/ 이후 Video Record Listener내에서 다시 버튼 활성화

        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            return
        }   //진행 중인 recording이 있으면 중지하고 기록 해제

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4") //이미지 저장할 mediastore uri
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }   //새 recording 세션 만들고 비디오 capture

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()    //생성된 비디오 콘텐츠 값을 MediaStoreOutputOptions(미디어 저장소 출력 옵션)로 설정합니다.MediaStore Output Options 인스턴스를 빌드
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(this@CameraActivity,
                        Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED)
                {
                    withAudioEnabled()  //비디오 캡처 레코더에 출력 옵션을 구성하고 오디오 녹음을 활성화
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->    //새 녹화를 시작하고 람다 비디오 레코드 이벤트 수신기를 등록
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        viewBinding.btnVideo.apply {
                            text = getString(R.string.stop_capture)
                            isEnabled = true    //녹화가 시작되면 버튼 텍스트를 stop_capture로 전환
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }
                        viewBinding.btnVideo.apply {
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                    }   //녹화가 완료되면 토스트로 알림, 버튼을 다시 "start_capture"으로 바꿈
                }
            }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)  //인스턴스 생성
        cameraProviderFuture.addListener({  //인스턴스에 리스너 추가
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()  //cameraProviderFuture.get() -> 메인스레드에서 실행되는 것을 반환
            val preview = Preview.Builder() //preciew객체 초기화
                .build()    //build호출
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
//            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA //default는 후면카메라
            try {   //cameraProvider에 바인딩된 것이 없는지 확인하고 cameraSelector와 preview를 cameraProvider에 바인딩
                cameraProvider.unbindAll()  //rebind하기 전에 사용했던 것들 바이딩 해제
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture) //카메라에 바이딩
            } catch(exc: Exception) {   //앱이 더 이상 포커스에 있지 않은 경우, 오류가 발생할 경우 기록
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }


    override fun onDestroy() {  //프래그먼트가 파괴될때
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun allPermissionsGranted() = CameraActivity.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    //카메라 요청 승인,거절을 누르면 아래 함수 호출
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == CameraActivity.REQUEST_CODE_PERMISSIONS) { //requestCode 확인
            if (allPermissionsGranted()) {  //권한이 있다면 카메라실행
//                startCamera()
            } else { //권한이 없다면 사용자에게 권한없음을 알림
                Toast.makeText(this, "권한을 승인해주셔야 합니다!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

