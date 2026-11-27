package pl.tekla.pellet

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp

val Bg = Color(0xFF111A22)
val Panel = Color(0xB8222D37)
val Panel2 = Color(0xA92C3843)
val Glass = Color(0x7A2A3742)
val Glass2 = Color(0x8F34424E)
val GlassBar = Color(0xCC18242D)
val Line = Color(0x5C9BAAB5)
val Txt = Color(0xFFF4F6F7)
val Mut = Color(0xFFB0BAC1)
val Acc = Color(0xFFFF861A)
val Acc2 = Color(0xFFFFB45E)
val Okc = Color(0xFF78E83A)
val Warn = Color(0xFFFFC233)
val Bad = Color(0xFFFF4D4D)

val LocalFrame = compositionLocalOf { 0 }

@Composable
fun TeklaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Acc,
            onPrimary = Color.Black,
            background = Bg,
            surface = Panel,
            onSurface = Txt,
            onBackground = Txt,
            surfaceVariant = Panel2,
            outline = Line
        ),
        content = content
    )
}

/** Grafika panelu z assets/ic/N.png (wbudowana w aplikacje). */
@Composable
fun Ic(n: Int, size: Dp, modifier: Modifier = Modifier) {
    val bmp = Ctl.icon(n) ?: return
    Image(bmp, null, modifier.size(size), contentScale = ContentScale.Fit, filterQuality = FilterQuality.Medium)
}
