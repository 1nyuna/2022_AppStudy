package com.example.zena_demoapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.zena_demoapp.databinding.ActivityMainBinding

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
    }
}