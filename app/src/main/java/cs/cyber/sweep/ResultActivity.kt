package cs.cyber.sweep

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cs.cyber.sweep.databinding.ActivityResultBinding
import cs.cyber.sweep.main.MainActivity
import java.text.DecimalFormat

class ResultActivity : AppCompatActivity() {

    private val binding by lazy { ActivityResultBinding.inflate(layoutInflater) }
    private val decimalFormat = DecimalFormat("#.#")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.result)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        displayCleanResults()
    }

    private fun initViews() {
        binding.imgBack.setOnClickListener {
            navigateToMain()
        }

        binding.atvCleanPicture.setOnClickListener {
            startPictureClean()
        }
        binding.mbCleanPicture.setOnClickListener {
            startPictureClean()
        }

        binding.atvCleanFile.setOnClickListener {
            startFileClean()
        }
        binding.mbCleanFile.setOnClickListener {
            startFileClean()
        }

        binding.atvCleanJunk.setOnClickListener {
            startScanClean()
        }
        binding.mbCleanJunk.setOnClickListener {
            startScanClean()
        }

        setupSmallCleanButtons()
    }

    private fun setupSmallCleanButtons() {
        val rootLayout = binding.root
        findAndSetupCleanButtons(rootLayout)
    }

    private fun findAndSetupCleanButtons(viewGroup: android.view.ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            if (child is com.google.android.material.button.MaterialButton) {
                if (child.text == "Clean" && child.backgroundTintList?.defaultColor == -16711939) { // #20C0FD
                    child.setOnClickListener {
                        when (child.parent) {
                            binding.atvCleanPicture.parent -> startPictureClean()
                            binding.atvCleanFile.parent -> startFileClean()
                            else -> navigateToMain()
                        }
                    }
                }
            } else if (child is android.view.ViewGroup) {
                findAndSetupCleanButtons(child)
            }
        }
    }

    private fun displayCleanResults() {
        val cleanedSize = intent.getStringExtra("cleaned_size")

        binding.tvSaveData.text = "Saved $cleanedSize space for you"

        if (cleanedSize?.isBlank() == true) {
            binding.tvSaveData.text = "No junk files found to clean"
        }
    }

    private fun formatStorage(bytes: Long): Pair<String, String> {
        return when {
            bytes >= 1024 * 1024 * 1024 -> {
                val gb = bytes.toDouble() / (1024 * 1024 * 1024)
                Pair(decimalFormat.format(gb), "GB")
            }

            bytes >= 1024 * 1024 -> {
                val mb = bytes.toDouble() / (1024 * 1024)
                Pair(decimalFormat.format(mb), "MB")
            }

            bytes >= 1024 -> {
                val kb = bytes.toDouble() / 1024
                Pair(decimalFormat.format(kb), "KB")
            }

            else -> {
                Pair(bytes.toString(), "B")
            }
        }
    }

    private fun startPictureClean() {
        val intent = Intent(this, PicCleanActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startFileClean() {
        val intent = Intent(this, FileScanActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startScanClean() {
        val intent = Intent(this, CleanActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToMain() {
        // 返回到主页面
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

}