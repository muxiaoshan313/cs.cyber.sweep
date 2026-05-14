package cs.cyber.sweep


import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import cs.cyber.sweep.databinding.ActivityOneBinding
import cs.cyber.sweep.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.jvm.java


class StartActivity : AppCompatActivity() {
    private val binding by lazy { ActivityOneBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.first)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        this.supportActionBar?.hide()
        onBackPressedDispatcher.addCallback {
        }
        startCountdown()
    }

    private fun startCountdown() {
        lifecycleScope.launch {
            delay(2100L)
            startActivity(Intent(this@StartActivity, MainActivity::class.java))
            finish()
        }

    }


}