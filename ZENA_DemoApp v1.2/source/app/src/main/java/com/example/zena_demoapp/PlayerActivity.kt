package com.example.zena_demoapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.zena_demoapp.databinding.ActivityPlayerBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min
import kotlin.random.Random


class PlayerActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityPlayerBinding
    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    var isrunning = true

    companion object {
        init {
            if (!OpenCVLoader.initDebug()) { Log.d(ContentValues.TAG, "OpenCV is not loaded!") }
            else { Log.d(ContentValues.TAG, "OpenCV is loaded successfully!") }
        }

        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_player)
        viewBinding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        // choose video ...
        val video_flag: Int = intent.getIntExtra("video_flag", -1)
        RefVideoLoad(video_flag)

        // ########GRAPH#######
        val thread = ThreadClass()
        thread.start()

        // play or pause button
        var isPlay = true
        viewBinding.imageButton.setOnClickListener({
            // icon-ref.: https://www.flaticon.com/kr/packs/music-player-icons?word=player
            if (isPlay) {
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
            viewBinding.imageButton.setImageResource(R.drawable.play)
            thread.interrupt()
        })

        // exit button
        viewBinding.button.setOnClickListener({ finish() })


        // ON/OFF button
        var ischecked = true
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

        run()
        val (xpositions, ypositions) = initModel()
        val returnvalue = create_2d_data(xpositions, ypositions)

    }



    private fun initModel() : Pair <Array<Float>, Array<Float>> {

        val interpreter = Interpreter(loadModelFile())

        val inputWidth = interpreter.getInputTensor(0).shape()[1]
        val inputHeight = interpreter.getInputTensor(0).shape()[2]
        var outputShape: IntArray = interpreter.getOutputTensor(0).shape()

        val tfInputSize by lazy {
            Size(inputHeight.toDouble(), inputWidth.toDouble()) // Order of axis is: {1, height, width, 3}
        }

        val outputProbabilityBuffer: TensorBuffer = TensorBuffer.createFixedSize(
            interpreter.getOutputTensor(0).shape(),
            interpreter.getOutputTensor(0).dataType())

//        val outputProbabilityBuffer: TensorBuffer = TensorBuffer.createFixedSize(
//            intArrayOf(1, 1, 17, 3),
//            DataType.FLOAT32)

        val tfImageProcessor by lazy {
            ImageProcessor.Builder()
//                .add(ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(
                    ResizeOp(tfInputSize.height.toInt(), tfInputSize.width.toInt(), ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .build()}

        val img_path = assetFilePath("test.png")
        val imageBitmap: Bitmap = BitmapFactory.decodeFile(img_path)

        val tImage = TensorImage()
        tImage.load(imageBitmap);

        // Creates processor for the TensorImage.
        val cropSize = min(imageBitmap.width, imageBitmap.height)

        val convertimage = tfImageProcessor.process(tImage)

        interpreter.run(convertimage.buffer, outputProbabilityBuffer.buffer.rewind())
        val output = outputProbabilityBuffer.floatArray
        val widthRatio = convertimage.width.toFloat() / inputWidth
        val heightRatio = convertimage.height.toFloat() / inputHeight
        val numKeyPoints = outputShape[2]

        var xpositions : Array<Float> = emptyArray()
        var ypositions : Array<Float> = emptyArray()

        for (idx in 0 until numKeyPoints) {
//            val x = output[idx * 3 + 1] * inputWidth * widthRatio
//            val y = output[idx * 3 + 0] * inputHeight * heightRatio
//            val score = output[idx * 3 + 2]

            xpositions = xpositions.plus(output[idx * 3 + 1])
            ypositions = ypositions.plus(output[idx * 3 + 0])
        }

        return Pair(xpositions, ypositions)
    }

    private fun create_2d_data (xpositions: Array<Float>, ypositions: Array<Float>) : Array<Array<Float>> {
        /*
        Assume `keypoints` comes from the MoveNet, which is scaled to [0, 1]; The image size is [1000, 1000].
        1. In this functions, we first do the keypoints convertion from COCO style to H36M/MPII style.
        2. Then we do the normailize screen coodinates using function `normalize_screen_coordinates`.

        inputs_2d index별 부위
        [0] : 엉덩이 중심, [1] : 오른쪽 엉덩이, [2] : 오른쪽 무릎. [3] : 오른쪽 발, [4] : 왼쪽 엉덩이, [5] : 왼쪽 무릎, [6] : 왼쪽 발
        [7] : 엉덩이 중심과 어깨 중심의 중앙, [8] : 어깨 중심, [9] : 눈 중심, [10] : 왼쪽 어깨, [11] : 왼쪽 엘보우, [12] : 왼쪽 손목
        [13] : 오른쪽 어깨, [14] : 오른쪽 엘보우, [15] : 오른쪽 손목
         */

        var inputx : Array<Float> = emptyArray()
        var inputy : Array<Float> = emptyArray()
        val dic_array = arrayOf(0, 12, 14, 16, 11, 13, 15, 0, 0, 0, 5, 7, 9, 6, 8, 10)

        for (i in dic_array) {
            inputx = inputx.plus(xpositions[i])
            inputy = inputy.plus(ypositions[i])
        }

        inputx[0] = (xpositions[11] + xpositions[12]) / 2
        inputy[0] = (ypositions[11] + ypositions[12]) / 2 // 엉덩이 중심

        inputx[8] = (xpositions[5] + xpositions[6]) / 2
        inputy[8] = (ypositions[5] + ypositions[6]) / 2 // 어깨 중심

        inputx[9] = (xpositions[1] + xpositions[2]) / 2
        inputy[9] = (ypositions[1] + ypositions[2]) / 2 // 눈 중심

        inputx[7] = (inputx[0] + inputx[8]) / 2
        inputy[7] = (inputy[0] + inputy[8]) / 2 // 엉덩이 중심과 어깨 중심의 중앙

        val inputs_2d = arrayOf(inputx, inputy)
        return inputs_2d

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

    private fun run() {
        val width = 16
        val height = 2

        // opencv ref.: https://velog.io/@newtree8/Android-Kotlin%EC%9C%BC%EB%A1%9C-OpenCV-%EC%82%AC%EC%9A%A9%ED%95%98%EA%B8%B0-1
        val img_path = assetFilePath("test.png")
        val ori_img = Imgcodecs.imread(img_path, Imgcodecs.IMREAD_COLOR)

        val img = Mat()
        val img_size = Size(width.toDouble(), height.toDouble())
        Imgproc.resize(ori_img, img, img_size)

        val net_path = assetFilePath("videopose.onnx")
        val net: Net = Dnn.readNetFromONNX(net_path)
        net.setInput(img)

        var result = net.forward()
        println("=========================================== " + result.toString() + " ===========================================")
    }

    private fun loadModelFile(): ByteBuffer {
        val assetFileDescriptor = this.assets.openFd("movenet.tflite")
        val fileInputStream  = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val length = assetFileDescriptor.length
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, length)
    }

    private fun assetFilePath(assetName: String): String? {
        val file = File(filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        try {
            assets.open(assetName).use { `is` ->
                FileOutputStream(file).use { os ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (`is`.read(buffer).also { read = it } != -1) {
                        os.write(buffer, 0, read)
                    }
                    os.flush()
                }
                return file.absolutePath
            }
        } catch (e: IOException) {
            Log.e(ContentValues.TAG, "Error process asset $assetName to file path")
        }
        return null
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

    // #############GRAPH############
    inner class ThreadClass : Thread() {
        override fun run() {
            val input = Array<Int>(1000, { Random.nextInt(100)})

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

            var i = 0
            while(isrunning) {
                try {
                    SystemClock.sleep(100)
                    data.addEntry(Entry(i.toFloat(), input[i].toFloat()), 0)
                    data.notifyDataChanged()
                    viewBinding.lineChart.notifyDataSetChanged()
                    viewBinding.lineChart.invalidate()
                    viewBinding.lineChart.setVisibleXRangeMaximum(100.toFloat())
                    viewBinding.lineChart.moveViewToX(data.entryCount.toFloat())
                    //                binding.lineChart.moveViewTo(data.entryCount, 50f, YAxis.AxisDependency.LEFT)
                    viewBinding.lineChart.invalidate()

                    Thread.sleep(10)
                    i += 1

                    //if(input[i]>70)
                    //viewBinding.score.setText(input[i].toString())

                    // SCORE
                    if (input[i] > 70) {
                        viewBinding.score.setText(input[i].toString())
                        viewBinding.score.setTextColor(Color.parseColor("#00ff00"))
                    } else if (40 < input[i] && input[i] < 70) {
                        viewBinding.score.setText(input[i].toString())
                        viewBinding.score.setTextColor(Color.parseColor("#00ffff"))
                    } else {
                        viewBinding.score.setText(input[i].toString())
                        viewBinding.score.setTextColor(Color.parseColor("#ff0000"))
                    }
                }
                catch(e : ArrayIndexOutOfBoundsException) {
                    e.printStackTrace();
                    //isrunning = false
                }
                catch (i : InterruptedException) {
                    i.printStackTrace();
                    return; // 인터럽트 받을 경우 return됨
                }

            }
        }
    }
}