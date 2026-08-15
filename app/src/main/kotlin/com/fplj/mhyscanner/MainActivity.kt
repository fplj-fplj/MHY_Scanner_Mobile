package com.fplj.mhyscanner

import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fplj.mhyscanner.log.AppLog
import com.fplj.mhyscanner.service.FloatingLogWindow
import com.fplj.mhyscanner.ui.AppRoot
import com.fplj.mhyscanner.ui.MHYTheme
import kotlinx.coroutines.launch

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
        AppLog.info("MainActivity", "onCreate")
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
        // 悬浮日志窗:随设置开关启停;RESUMED 时重新评估,用户从授权页返回后自动生效
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    vm.uiState.collect { state ->
                        if (state.config.floatingLogEnabled && FloatingLogWindow.canDraw(applicationContext)) {
                            FloatingLogWindow.start(applicationContext)
                        } else {
                            FloatingLogWindow.stop()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        AppLog.info("MainActivity", "onDestroy")
        FloatingLogWindow.stop()
        super.onDestroy()
    }
}
