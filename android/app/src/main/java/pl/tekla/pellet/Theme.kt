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

data class TeklaPalette(
    val bg: Color,
    val panel: Color,
    val panel2: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accentSoft: Color,
    val ok: Color,
    val warn: Color,
    val bad: Color,
    val cold: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassHighlight: Color
)

object TeklaThemes {
    /** Kanoniczny motyw aplikacji — Modern Glass. */
    val ModernGlass = TeklaPalette(
        bg = Color(0xFF090D12),
        panel = Color(0xD91A222B),
        panel2 = Color(0xCC202B35),
        line = Color(0xFF34424D),
        text = Color(0xFFF3F5F7),
        muted = Color(0xFF9AA5AE),
        accent = Color(0xFFFF981F),
        accentSoft = Color(0xFFFFB45C),
        ok = Color(0xFF69E36F),
        warn = Color(0xFFFFC233),
        bad = Color(0xFFFF4D4D),
        cold = Color(0xFF59B8FF),
        glassTop = Color(0xD92A3640),
        glassBottom = Color(0xCC141A20),
        glassHighlight = Color(0x4DFFFFFF)
    )
}

/** Jedno miejsce wyboru motywu. Kolejne motywy dodajemy do TeklaThemes bez przepisywania UI. */
private val ActiveTeklaTheme = TeklaThemes.ModernGlass

val Bg get() = ActiveTeklaTheme.bg
val Panel get() = ActiveTeklaTheme.panel
val Panel2 get() = ActiveTeklaTheme.panel2
val Line get() = ActiveTeklaTheme.line
val Txt get() = ActiveTeklaTheme.text
val Mut get() = ActiveTeklaTheme.muted
val Acc get() = ActiveTeklaTheme.accent
val Acc2 get() = ActiveTeklaTheme.accentSoft
val Okc get() = ActiveTeklaTheme.ok
val Warn get() = ActiveTeklaTheme.warn
val Bad get() = ActiveTeklaTheme.bad

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
