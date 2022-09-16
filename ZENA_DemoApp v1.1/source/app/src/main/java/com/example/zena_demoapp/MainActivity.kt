package com.example.zena_demoapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.zena_demoapp.databinding.ActivityMainBinding
import org.jetbrains.annotations.NotNull
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.ImageButton1.setOnClickListener({
            startActivity(Intent(this, PlayerActivity::class.java).putExtra("video_flag", 1))
        })

        viewBinding.ImageButton2.setOnClickListener({
            startActivity(Intent(this, PlayerActivity::class.java).putExtra("video_flag", 2))
        })

        val bigPictureBitmap = BitmapFactory.decodeResource(resources, R.drawable.image)
        val image = TensorImage.fromBitmap(bigPictureBitmap)

        val interpreter = Interpreter(loadModelFile())

        val input = interpreter.getInputTensor(0)
        val output = interpreter.getOutputTensor(0)
        interpreter.run(input, output)



    }

    private fun loadModelFile():ByteBuffer{
        val assetFileDescriptor = this.assets.openFd("model.tflite")
        val fileInputStream  = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val length = assetFileDescriptor.length
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, length)
    }


}