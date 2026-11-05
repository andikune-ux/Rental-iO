package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dev.andikune.rentalio.ui.MainActivity as KioskMainActivity

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    startActivity(Intent(this, KioskMainActivity::class.java))
    finish()
  }
}
