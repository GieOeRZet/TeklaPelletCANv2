package pl.tekla.pellet

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
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
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Stan aplikacji (jedna instancja na proces): model, aktywne zrodlo danych (demo / ESP), obrazy z assets. */
object Ctl {
    lateinit var ctx: Context
    lateinit var model: AppModel
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    var startScreen: String? = null
    var startTick = 0   // do zrzutow w CI: --ei tick 24 = palnik w fazie pracy
    var source by mutableStateOf<DataSource?>(null)
    val icons = HashMap<Int, ImageBitmap?>()
    val devices = mutableStateListOf<Paired>()
  var appUnlocked by mutableStateOf(false)
    var activeId by mutableStateOf<String?>(null)
    fun init(c: Context) {
        if (::ctx.isInitialized) return
        ctx = c.applicationContext; model = AppModel(ctx)
        devices.addAll(try { DeviceStore.load(ctx) } catch (e: Exception) { emptyList() }); Discovery.start(ctx)
        val act = DeviceStore.active(ctx)?.let { id -> devices.firstOrNull { it.id == id } }
        if (act != null) connectTo(act) else source = DemoSource(model, scope)   // bez sparowanego modulu: demo z przykladowymi wartosciami
    }
    private fun connectTo(d: Paired) { source?.close(); activeId = d.id; DeviceStore.setActive(ctx, d.id); source = RemoteSource(d.host, scope, d.token, d.id).also { it.start() } }
    fun useDemo() { source?.close(); activeId = null; DeviceStore.setActive(ctx, null); source = DemoSource(model, scope) }
    /** Dodaje (lub aktualizuje po id) sparowany modul i przelacza na niego. */
    fun addPaired(p: Paired) { devices.removeAll { it.id == p.id }; devices.add(p); DeviceStore.save(ctx, devices); connectTo(p) }
    fun activate(id: String) { devices.firstOrNull { it.id == id }?.let { connectTo(it) } }
    fun forget(id: String) { devices.removeAll { it.id == id }; DeviceStore.save(ctx, devices); if (activeId == id) { if (devices.isNotEmpty()) activate(devices.first().id) else useDemo() } }
    fun deviceMoved(id: String, host: String) { devices.firstOrNull { it.id == id }?.let { it.host = host; DeviceStore.save(ctx, devices) } }
    var firmwareVersion by mutableStateOf<String?>(null)
    fun activeName(): String = devices.firstOrNull { it.id == activeId }?.name ?: "TeklaPellet"
    fun notify(msg: String) { scope.launch { android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show() } }
    val clientName: String get() = ("Android " + android.os.Build.MODEL).take(23).replace("\"", "").replace("\\", "")
    fun icon(n: Int): ImageBitmap? = icons.getOrPut(n) {
        try {
            ctx.assets.open("ic/$n.png").use { stream ->
                val src = BitmapFactory.decodeStream(stream) ?: return@getOrPut null
                val bmp = src.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                // Oryginalne bitmapy panela mają jednolite tło (41,40,41).
                // Usuwamy wyłącznie to tło przy renderowaniu, zachowując oryginalną grafikę.
                val br = 41; val bg = 40; val bb = 41
                for (y in 0 until bmp.height) for (x in 0 until bmp.width) {
                    val p = bmp.getPixel(x, y)
                    val d = maxOf(abs(Color.red(p) - br), abs(Color.green(p) - bg), abs(Color.blue(p) - bb))
                    if (d <= 7) bmp.setPixel(x, y, Color.TRANSPARENT)
                    else if (d < 28) {
                        val a = ((d - 7) * 255 / 21).coerceIn(0, 255)
                        bmp.setPixel(x, y, Color.argb(a, Color.red(p), Color.green(p), Color.blue(p)))
                    }
                }
                bmp.asImageBitmap()
            }
        } catch (e: Exception) { null }
    }
}

class MainActivity : androidx.fragment.app.FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ctl.startTick = intent?.getIntExtra("tick", 0) ?: 0
        Ctl.init(this)
        Ctl.appUnlocked = !AppLock.isSet()
        Ctl.startScreen = intent?.getStringExtra("screen")   // do zrzutow w CI: am start --es screen kociol
        setContent { TeklaApp() }
    }

    override fun onResume() { super.onResume(); Ctl.appUnlocked = !AppLock.isSet() }
    override fun onPause() { BoilerAccess.lock(); Ctl.appUnlocked = false; super.onPause() }
}
