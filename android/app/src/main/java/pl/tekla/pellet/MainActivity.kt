package pl.tekla.pellet

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Stan aplikacji (jedna instancja na proces): model, aktywne zrodlo danych (demo / ESP), obrazy z assets. */
object Ctl {
    lateinit var ctx: Context
    lateinit var model: AppModel
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    var source by mutableStateOf<DataSource?>(null)
    val icons = HashMap<Int, ImageBitmap?>()
    fun init(c: Context) {
        if (::ctx.isInitialized) return
        ctx = c.applicationContext; model = AppModel(ctx)
        source = DemoSource(model, scope)   // start zawsze w trybie demo; polaczenie z ESP (wymaga hasla) w Ustawieniach
    }
    fun useDemo() { source?.close(); source = DemoSource(model, scope) }
    fun use(s: DataSource) { source?.close(); source = s }
    fun icon(n: Int): ImageBitmap? = icons.getOrPut(n) { try { ctx.assets.open("ic/$n.png").use { BitmapFactory.decodeStream(it)?.asImageBitmap() } } catch (e: Exception) { null } }
    var host: String
        get() = ctx.getSharedPreferences("tp", Context.MODE_PRIVATE).getString("host", "teklapellet.local") ?: ""
        set(v) { ctx.getSharedPreferences("tp", Context.MODE_PRIVATE).edit().putString("host", v).apply() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ctl.init(this)
        setContent { TeklaApp() }
    }
}
