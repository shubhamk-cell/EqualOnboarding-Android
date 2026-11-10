package ai.equal.orb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Jump to a screen while developing (only adb sends these):
        // `adb shell am start -n ai.equal.onboarding.orbbubble/ai.equal.orb.MainActivity --es startStep access`
        val start = intent.getStringExtra("startStep")
        setContent { OrbApp(start) }
        // The green launch background is only for the moment before the app draws; after that it
        // would be painted, unseen, under every frame.
        window.decorView.post { window.setBackgroundDrawable(null) }
    }
}
