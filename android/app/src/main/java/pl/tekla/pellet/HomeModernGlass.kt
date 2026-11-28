package pl.tekla.pellet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlin.math.roundToInt

private object HomeGlass {
    val p get() = TeklaThemes.ModernGlass
    val shape = RoundedCornerShape(17.dp)
}

private data class HomeModule(
    val id: String,
    val title: String,
    val route: String,
    val kind: Kind,
    val activeColor: Color
) {
    enum class Kind { TEMP, STATUS, FUEL }
}

private fun homeModules(ui: Ui): List<HomeModule> {
    val out = mutableListOf<HomeModule>()
    val co = (ui.n("0x2105") ?: 0.0).roundToInt().coerceIn(0, 4)
    for (i in 1..co) {
        val id = "co$i"
        if (ui.m.screens.containsKey(id)) {
            out += HomeModule(id, "CO $i", id, HomeModule.Kind.TEMP, HomeGlass.p.cold)
        }
    }
    if ((ui.n("0x2106") ?: 0.0) != 0.0 && ui.m.screens.containsKey("cwu")) {
        out += HomeModule("cwu", "CWU", "cwu", HomeModule.Kind.TEMP, HomeGlass.p.cold)
    }
    out += HomeModule("fuel", "PALIWO", "fuel", HomeModule.Kind.FUEL, HomeGlass.p.accent)
    if ((ui.n("0x2107") ?: 0.0) != 0.0 && ui.m.screens.containsKey("bufor")) {
        out += HomeModule("bufor", "BUFOR", "bufor", HomeModule.Kind.TEMP, HomeGlass.p.cold)
    }
    if ((ui.n("0x2108") ?: 0.0) != 0.0 && ui.m.screens.containsKey("solary")) {
        out += HomeModule("solary", "SOLARY", "solary", HomeModule.Kind.TEMP, HomeGlass.p.accent)
    }
    if ((ui.n("0x350C") ?: 0.0) != 0.0 && ui.m.screens.containsKey("kaskada")) {
        out += HomeModule("kaskada", "KASKADA", "kaskada", HomeModule.Kind.STATUS, HomeGlass.p.accent)
    }
    if ((ui.n("0x3525") ?: 0.0) != 0.0 && ui.m.screens.containsKey("magazyn")) {
        out += HomeModule("magazyn", "MAGAZYN", "magazyn", HomeModule.Kind.STATUS, HomeGlass.p.accent)
    }
    return out
}

private fun homeActive(ui: Ui, id: String): Boolean {
    return when {
        id.startsWith("co") -> {
            val s = ui.m.screens[id]
            s?.mode?.let { (ui.n(it) ?: 0.0).roundToInt() != 0 } ?: false
        }
        id == "cwu" -> (ui.n("0x2014") ?: 0.0).roundToInt() != 0
        id == "bufor" -> (ui.n("0x2277") ?: 0.0) != 0.0
        id == "solary" -> (ui.n("0x311C") ?: 0.0) != 0.0
        id == "kaskada" -> true
        id == "magazyn" -> (ui.n("0x3522") ?: 0.0) != 0.0 || (ui.n("0x3523") ?: 0.0) != 0.0
        else -> false
    }
}

@Composable
fun HomeModernGlass(ui: Ui, nav: (String) -> Unit) {
    val modules = homeModules(ui)
    val pages = modules.chunked(6).ifEmpty { listOf(emptyList()) }
    val pager = rememberPagerState(pageCount = { pages.size })

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        HomeBoilerTemplate(ui) { nav("kociol") }

        Spacer(Modifier.height(8.dp))

        HomeOutputTemplate(ui)

        Spacer(Modifier.height(9.dp))

        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 0.dp,
            beyondViewportPageCount = 0
        ) { page ->
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pages[page].chunked(2).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().height(88.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { module ->
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                HomeModuleTemplate(ui, module) { nav(module.route) }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        if (pages.size > 1) {
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth().height(10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (i == pager.currentPage) 8.dp else 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (i == pager.currentPage) HomeGlass.p.accent else HomeGlass.p.muted.copy(alpha = .45f))
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeGlassCard(
    modifier: Modifier,
    active: Color? = null,
    click: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = HomeGlass.shape
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(HomeGlass.p.glassTop, HomeGlass.p.glassBottom)
                )
            )
            .border(1.dp, HomeGlass.p.line, shape)
            .then(if (click != null) Modifier.clickable(onClick = click) else Modifier)
    ) {
        // Subtelny "szklany" highlight, bez kolorowania nieaktywnego kafla.
        Box(
            Modifier
                .fillMaxWidth(.32f)
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(HomeGlass.p.glassHighlight)
        )
        if (active != null) {
            Box(
                Modifier
                    .fillMaxWidth(.38f)
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, active.copy(alpha = .82f), Color.Transparent)
                        )
                    )
            )
        }
        Column(
            Modifier.fillMaxSize().padding(horizontal = 13.dp, vertical = 10.dp),
            content = content
        )
    }
}

@Composable
private fun HomeBoilerTemplate(ui: Ui, click: () -> Unit) {
    val on = (ui.n("0x2005") ?: 0.0) != 0.0
    val state = ui.n("0x2007")?.roundToInt() ?: 0
    val stateText = ui.m.statusTables["burner"]?.getOrNull(state) ?: if (on) "Praca" else "Wyłączony"
    val active = if (on) HomeGlass.p.accent else null

    HomeGlassCard(
        Modifier.fillMaxWidth().height(176.dp),
        active,
        click
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(145.dp).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                BoilerImage(ui)
            }

            Column(
                Modifier.weight(1f).padding(start = 4.dp, end = 5.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Kocioł", color = HomeGlass.p.text, fontSize = 21.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(8.dp).clip(RoundedCornerShape(50))
                            .background(if (on) HomeGlass.p.ok else HomeGlass.p.muted.copy(alpha = .6f))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stateText,
                        color = if (on) HomeGlass.p.ok else HomeGlass.p.muted,
                        fontSize = 15.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        f1(ui.n("0x2001")),
                        color = HomeGlass.p.text,
                        fontSize = 39.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Light
                    )
                    Text(
                        "°C",
                        color = HomeGlass.p.muted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 3.dp, bottom = 5.dp)
                    )
                }
                Text(
                    "→ " + f1(ui.n("0x2004"), 0) + " °C",
                    color = HomeGlass.p.accentSoft,
                    fontSize = 13.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                Spacer(Modifier.height(7.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeMetric("Płomień", f1(ui.n("0x4005"), 0) + " %", Modifier.weight(1f))
                    HomeMetric("Spalanie", f1(ui.n("0x3710"), 1) + " kg/h", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = HomeGlass.p.muted, fontSize = 9.sp)
        Text(value, color = HomeGlass.p.text, fontSize = 12.sp)
    }
}

@Composable
private fun HomeOutputTemplate(ui: Ui) {
    val screen = ui.m.screens["palnik"] ?: return
    val cells = screen.bar.filter { ui.v.containsKey(it.p) }
    if (cells.isEmpty()) return

    HomeGlassCard(
        Modifier.fillMaxWidth().height(54.dp)
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            cells.take(6).forEach { c ->
                val value = (ui.n(c.p) ?: 0.0).roundToInt()
                val on = value != 0
                val led = if (on) HomeGlass.p.accentSoft else HomeGlass.p.muted.copy(alpha = .20f)
                Box(
                    Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (on) HomeGlass.p.accent.copy(alpha = .13f)
                                else HomeGlass.p.bg.copy(alpha = .28f)
                            )
                            .border(
                                1.dp,
                                if (on) led.copy(alpha = .72f) else HomeGlass.p.line.copy(alpha = .75f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (c.fr.isNotEmpty()) {
                            Ic(c.fr.first(), 22.dp, Modifier.alpha(if (on) 1f else .25f))
                        } else {
                            Box(
                                Modifier.size(if (on) 8.dp else 6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(led)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeModuleTemplate(ui: Ui, module: HomeModule, click: () -> Unit) {
    when (module.kind) {
        HomeModule.Kind.FUEL -> HomeFuelTemplate(ui, click)
        else -> {
            val screen = ui.m.screens[module.id] ?: return
            val active = homeActive(ui, module.id)
            HomeGlassCard(
                Modifier.fillMaxSize(),
                if (active) module.activeColor else null,
                click
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        module.title,
                        color = HomeGlass.p.text,
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    val icon = screen.mode?.let { pid ->
                        ui.m.programModes.firstOrNull {
                            it.v == (ui.n(pid)?.roundToInt() ?: -1)
                        }?.icon
                    }
                    if (icon != null) Ic(icon, 17.dp)
                }
                Spacer(Modifier.height(2.dp))

                if (screen.big != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            f1(ui.n(screen.big)),
                            color = HomeGlass.p.text,
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Light
                        )
                        if (screen.bigUnit.isNotEmpty()) {
                            Text(
                                screen.bigUnit,
                                color = HomeGlass.p.muted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 3.dp, bottom = 3.dp)
                            )
                        }
                    }
                } else if (screen.state != null) {
                    val v = ui.n(screen.stateP ?: screen.big)?.roundToInt() ?: 0
                    Text(
                        ui.m.statusTables[screen.state]?.getOrNull(v) ?: "—",
                        color = HomeGlass.p.text,
                        fontSize = 18.sp
                    )
                }

                if (screen.pill != null) {
                    Text(
                        "→ " + f1(ui.n(screen.pill), 0) + " °C",
                        color = if (active) module.activeColor else HomeGlass.p.muted,
                        fontSize = 11.sp
                    )
                } else {
                    val status = screen.mode?.let { pid ->
                        if ((ui.n(pid) ?: 0.0) != 0.0) "Aktywny" else "Gotowość"
                    }
                    if (status != null) {
                        Text(
                            status,
                            color = if (active) module.activeColor else HomeGlass.p.muted,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeFuelTemplate(ui: Ui, click: () -> Unit) {
    val fuel = ui.n("0x3601") ?: 0.0
    val capacity = ui.n("0x3600") ?: 0.0
    val reserve = ui.n("0x3603") ?: 0.0
    val reserveMode = reserve > 0 && fuel <= reserve

    HomeGlassCard(
        Modifier.fillMaxSize(),
        if (reserveMode) HomeGlass.p.accent else null,
        click
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PALIWO", color = HomeGlass.p.text, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(
                if (reserveMode) "REZERWA" else "",
                color = if (reserveMode) HomeGlass.p.accentSoft else Color.Transparent,
                fontSize = 8.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeFuelContainer(fuel, capacity, reserveMode)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    f1(fuel, 0),
                    color = HomeGlass.p.text,
                    fontSize = 25.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Light
                )
                Text(" kg", color = HomeGlass.p.muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
private fun HomeFuelContainer(fuel: Double, capacity: Double, reserve: Boolean) {
    val ratio = if (capacity > 0) (fuel / capacity).coerceIn(0.0, 1.0) else 0.0
    Box(Modifier.width(38.dp).height(52.dp), contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val left = 2.dp.toPx()
            val top = 2.dp.toPx()
            val w = size.width - 4.dp.toPx()
            val h = size.height - 4.dp.toPx()
            drawRoundRect(
                HomeGlass.p.bg.copy(alpha = .35f),
                androidx.compose.ui.geometry.Offset(left, top),
                androidx.compose.ui.geometry.Size(w, h),
                androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()),
                style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
            )
            if (ratio > 0) {
                val fillH = (h * ratio.toFloat()).coerceAtLeast(2.dp.toPx())
                drawRoundRect(
                    if (reserve) HomeGlass.p.accent.copy(alpha = .20f) else HomeGlass.p.accent.copy(alpha = .16f),
                    androidx.compose.ui.geometry.Offset(left + 2.dp.toPx(), top + h - fillH),
                    androidx.compose.ui.geometry.Size(w - 4.dp.toPx(), fillH),
                    androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                )
                val dots = when {
                    ratio >= .87 -> 24
                    ratio >= .74 -> 19
                    ratio >= .57 -> 14
                    ratio >= .38 -> 9
                    ratio >= .25 -> 5
                    ratio >= .08 -> 2
                    else -> 0
                }
                for (i in 0 until dots) {
                    val col = i % 4
                    val row = i / 4
                    val x = left + 8.dp.toPx() + col * 7.dp.toPx()
                    val y = top + h - 7.dp.toPx() - row * 6.dp.toPx()
                    drawCircle(HomeGlass.p.accentSoft, 1.8.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
                }
            }
        }
    }
}
