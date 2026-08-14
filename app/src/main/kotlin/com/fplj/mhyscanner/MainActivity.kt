package com.fplj.mhyscanner

import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.fplj.mhyscanner.ui.AppRoot
import com.fplj.mhyscanner.ui.MHYTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            vm.startScreenScan(result.resultCode, data)
        } else {
            vm.stopScan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MHYTheme {
                AppRoot(
                    vm = vm,
                    onRequestScreenCapture = {
                        val manager = getSystemService(MediaProjectionManager::class.java)
                        projectionLauncher.launch(manager.createScreenCaptureIntent())
                    }
                )
            }
        }
    }
}