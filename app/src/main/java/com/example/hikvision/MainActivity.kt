package com.example.hikvision

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HikvisionScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HikvisionScreen() {
    var rtspUrl by remember { 
        mutableStateOf("rtsp://admin:Stockings88@192.168.1.55:554/Streaming/Channels/101") 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hikvision Viewer (LibVLC)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                HikvisionVlcPlayer(rtspUrl = rtspUrl)
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = rtspUrl,
                    onValueChange = { rtspUrl = it },
                    label = { Text("Adres RTSP") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun HikvisionVlcPlayer(
    rtspUrl: String,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val context = LocalContext.current

    val libVLC = remember(context) {
        val options = arrayListOf(
            "--rtsp-tcp",
            "--network-caching=300",
            "--drop-late-frames",
            "--skip-frames"
        )
        LibVLC(context, options)
    }

    val mediaPlayer = remember(libVLC) {
        MediaPlayer(libVLC)
    }

    DisposableEffect(rtspUrl) {
        if (rtspUrl.isNotBlank()) {
            val media = Media(libVLC, Uri.parse(rtspUrl)).apply {
                setHWDecoderEnabled(true, true)
                addOption(":clock-jitter=0")
                addOption(":clock-synchro=0")
            }
            mediaPlayer.media = media
            media.release()
            mediaPlayer.play()
        }

        onDispose {
            mediaPlayer.stop()
            mediaPlayer.detachViews()
            mediaPlayer.release()
            libVLC.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            VLCVideoLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Ustawienie useTextureView = true (czwarty parametr)
                // Wymusza renderowanie na warstwie Jetpack Compose bez czarnego ekranu
                mediaPlayer.attachViews(this, null, false, true)
            }
        },
        modifier = modifier
    )
}
