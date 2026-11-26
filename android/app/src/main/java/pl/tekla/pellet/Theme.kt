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

val Bg = Color(0xFF09090B); val Panel = Color(0xFF141418); val Panel2 = Color(0xFF1A1A1F); val Line = Color(0xFF26262C)
val Txt = Color(0xFFF3F3F5); val Mut = Color(0xFF8A8A94); val Acc = Color(0xFFFF7A1A); val Acc2 = Color(0xFFFFB066); val Okc = Color(0xFF4FE38A); val Warn = Color(0xFFFFC233); val Bad = Color(0xFFFF4D4D)

val LocalFrame = compositionLocalOf { 0 }

@Composable
fun TeklaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Acc, onPrimary = Color.Black, background = Bg, surface = Panel, onSurface = Txt, onBackground = Txt, surfaceVariant = Panel2, outline = Line), content = content)
}

/** Grafika panelu z assets/ic/N.png (wbudowana w aplikacje). */
@Composable
fun Ic(n: Int, size: Dp, modifier: Modifier = Modifier) {
    val bmp = Ctl.icon(n) ?: return
    Image(bmp, null, modifier.size(size), contentScale = ContentScale.Fit, filterQuality = FilterQuality.Medium)
}
