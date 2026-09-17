package io.lunosfer.dreamap.ui.screens.videoeditor

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import io.lunosfer.dreamap.data.model.AspectRatioOption
import io.lunosfer.dreamap.data.model.ClipType
import io.lunosfer.dreamap.data.model.MediaClip

@OptIn(UnstableApi::class)
@Composable
fun PreviewPlayer(
    player: ExoPlayer,
    aspectRatio: AspectRatioOption,
    currentClip: MediaClip?,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(aspectRatio.widthPx.toFloat() / aspectRatio.heightPx.toFloat()),
            contentAlignment = Alignment.Center,
        ) {
            if (currentClip != null && currentClip.type == ClipType.IMAGE) {
                AsyncImage(
                    model = currentClip.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        PlayerView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            this.player = player
                        }
                    },
                    update = { view -> view.player = player },
                )
            }
        }
    }
}
