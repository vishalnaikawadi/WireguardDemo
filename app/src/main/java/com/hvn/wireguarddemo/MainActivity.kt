package com.hvn.wireguarddemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.hvn.wireguarddemo.wireguard.activity.MainActivity


class MainActivity : AppCompatActivity() {

    private lateinit var btnAction: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnAction = findViewById(R.id.btnWireGuard)
        btnAction.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}