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

// Tekla Modern Glass — wspólny język całej aplikacji.
val Bg = Color(0xFF080D14)
val BgTop = Color(0xFF111A24)
val Panel = Color(0xB8141D27)
val Panel2 = Color(0xCC1B2632)
val PanelGlass = Color(0xA61A2530)
val Line = Color(0x33E6EDF5)
val LineStrong = Color(0x55E6EDF5)
val GlassHi = Color(0x14FFFFFF)
val Txt = Color(0xFFF4F7FA)
val Mut = Color(0xFFA3AFBC)
val Acc = Color(0xFFFF8A1C)
val Acc2 = Color(0xFFFFB45C)
val Okc = Color(0xFF48D597)
val Warn = Color(0xFFFFC34D)
val Bad = Color(0xFFFF5F66)
val Cold = Color(0xFF66B8FF)

val LocalFrame = compositionLocalOf { 0 }

@Composable
fun TeklaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Acc,
            onPrimary = Color.Black,
            primaryContainer = Panel2,
            background = Bg,
            surface = Panel,
            onSurface = Txt,
            onBackground = Txt,
            surfaceVariant = Panel2,
            outline = LineStrong,
            secondary = Acc2,
            error = Bad
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
