package pl.tekla.pellet

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val G2Bg = Color(0xFF09090B)
private val G2Panel = Color(0xFF141418)
private val G2Panel2 = Color(0xFF1A1A1F)
private val G2Line = Color(0xFF26262C)
private val G2Text = Color(0xFFF3F3F5)
private val G2Mut = Color(0xFF8A8A94)
private val G2Orange = Color(0xFFFF7A1A)
private val G2Orange2 = Color(0xFFFFB066)
private val G2Green = Color(0xFF4FE38A)
private val G2Blue = Color(0xFF5AB4FF)
private val G2Red = Color(0xFFFF4D4D)

@Composable
private fun AssetIcon(id:Int, modifier:Modifier=Modifier.size(24.dp)){
    val image=remember(id){
        runCatching{
            Ctl.ctx.assets.open("ic/$id.png").use{BitmapFactory.decodeStream(it)?.asImageBitmap()}
        }.getOrNull()
    }
    if(image!=null) Image(image,contentDescription=null,modifier=modifier)
}

private data class TestDef(
    val id:String,
    val label:String,
    val pid:String,
    val closeOpen:Boolean=false,
    val pctPid:String?=null
)

/**
 * Canonical TEST_WYJSC model.
 * Source: analiza/.../ui_model/outputs_test.json.
 * UI deliberately has no row numbering; the order is the panel order.
 */
private val TESTS = listOf(
    TestDef("cwu_pump","Pompa CWU","0x2296"),
    TestDef("cwu_circulation","Pompa cyrkulacyjna CWU","0x374A"),
    TestDef("boiler_pump","Pompa kotłowa","0x2286"),
    TestDef("solar_pump","Pompa solarna","0x311F"),
    TestDef("feeder","Podajnik","0x2259"),
    TestDef("grate","Ruszt","0x2279",closeOpen=true),
    TestDef("exchanger","Wymiennik","0x2285"),
    TestDef("igniter","Zapalarka","0x2284"),
    TestDef("burner_feeder","Podajnik palnika","0x2295"),
    TestDef("fuel_transport","Magazyn paliwa transport(1)","0x351E"),
    TestDef("fuel_feeder","Magazyn paliwa podajnik(2)","0x351F"),
    TestDef("blower","Dm. (dmuchawa)","0x2283",pctPid="0x2288"),
    TestDef("exhaust_fan","WentWyc (wentylator wyciagowy)","0x2293",pctPid="0x2298"),
    TestDef("co1_pump","Pompa CO1","0x229D"),
    TestDef("co2_pump","Pompa CO2","0x229E"),
    TestDef("co3_pump","Pompa CO3","0x229F"),
    TestDef("co4_pump","Pompa CO4","0x22A0"),
    TestDef("co1_mixer","Mieszacz CO1","0x214A",closeOpen=true),
    TestDef("co2_mixer","Mieszacz CO2","0x214B",closeOpen=true),
    TestDef("co3_mixer","Mieszacz CO3","0x214C",closeOpen=true),
    TestDef("co4_mixer","Mieszacz CO4","0x214D",closeOpen=true),
    TestDef("return_mixer","Mieszacz powrotu","0x216D",closeOpen=true)
)

private fun configModules(ui:Ui):List<String>{
    val out=mutableListOf<String>()
    val co=(ui.n("0x2105")?:0.0).toInt().coerceIn(0,4)
    for(i in 1..co) out.add("co"+i)
    if((ui.n("0x2106")?:0.0)!=0.0) out.add("cwu")
    out.add("fuel")
    if((ui.n("0x2107")?:0.0)!=0.0) out.add("bufor")
    if((ui.n("0x2108")?:0.0)!=0.0) out.add("solary")
    if((ui.n("0x350C")?:0.0)!=0.0) out.add("kaskada")
    if((ui.n("0x3525")?:0.0)!=0.0) out.add("magazyn")
    return out
}

private fun moduleLed(ui:Ui,id:String):Color?{
    val on=when{
        id.startsWith("co")->(ui.n(ui.m.screens.getValue(id).mode)?.toInt()?:0)!=0
        id=="cwu"->(ui.n("0x2278")?:0.0)!=0.0
        id=="bufor"->(ui.n("0x2277")?:0.0)!=0.0
        id=="solary"->(ui.n("0x311C")?:0.0)!=0.0
        id=="magazyn"->(ui.n("0x3522")?:0.0)!=0.0 || (ui.n("0x3523")?:0.0)!=0.0
        else->false
    }
    if(!on) return null
    return if(id=="cwu") G2Blue else if(id=="solary") G2Orange else G2Green
}

@Composable
fun TeklaAppV2(){
    TeklaTheme{
        if(AppLock.isSet()&&!Ctl.appUnlocked){ LockScreen{Ctl.appUnlocked=true}; return@TeklaTheme }
        val src=Ctl.source?:return@TeklaTheme
        val vals by src.values.collectAsState()
        val level by src.level.collectAsState()
        val connected by src.connected.collectAsState()
        val ui=Ui(src,vals,level,true,{},{},{Ctl.notify(it)})
        var screen by remember{mutableStateOf(Ctl.startScreen?:"home")}
        var settingsOpen by remember{mutableStateOf(false)}
        fun nav(s:String){screen=s;settingsOpen=false}
        BackHandler(screen!="home"){nav("home")}
        Scaffold(
            containerColor=G2Bg,
            topBar={HeaderV2(ui,screen,connected,::nav)},
            bottomBar={BottomV2(ui,screen,::nav)}
        ){pad->
            Box(Modifier.fillMaxSize().padding(pad)){
                when{
                    screen=="home"->HomeV2(ui,::nav)
                    screen=="outtest"->OutputTestV2(ui)
                    screen=="menu"->MenuV2(ui,::nav)
                    screen=="settings"->MenuV2(ui,::nav)
                    screen=="alarms"->ListV2(ui,"Historia alarmów","0x11")
                    else->ModuleV2(ui,screen,settingsOpen){settingsOpen=!settingsOpen}
                }
            }
        }
    }
}

@Composable private fun HeaderV2(ui:Ui,screen:String,connected:Boolean,nav:(String)->Unit){
    val title=when(screen){
        "home"->"Pulpit"
        "outtest"->"Test wyjść"
        "menu"->"Menu"
        "settings"->"Ustawienia"
        "alarms"->"Alarmy"
        else->ui.m.screens[if(screen=="co")"co1" else screen]?.title?:"TeklaPellet"
    }
    Row(
        Modifier.fillMaxWidth().background(G2Bg).statusBarsPadding()
            .padding(horizontal=14.dp,vertical=6.dp),
        verticalAlignment=Alignment.CenterVertically
    ){
        if(screen!="home"){
            Text("‹",color=G2Text,fontSize=32.sp,modifier=Modifier.clickable{nav("home")})
            Spacer(Modifier.width(6.dp))
        }
        Column(Modifier.weight(1f)){
            Text(title,color=G2Text,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
            if(screen=="home") Text("TeklaPellet",color=G2Mut,fontSize=10.5.sp)
        }
        if(screen=="home"){
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp,G2Line,RoundedCornerShape(20.dp))
                    .background(G2Panel).padding(horizontal=9.dp,vertical=4.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(if(connected)G2Green else G2Red))
                Spacer(Modifier.width(5.dp))
                Text(if(connected)"Na żywo · CAN OK" else "Brak połączenia",color=G2Mut,fontSize=10.5.sp,fontWeight=FontWeight.SemiBold)
            }
        }
    }
}

@Composable private fun BottomV2(ui:Ui,screen:String,nav:(String)->Unit){
    val items=listOf(
        Triple("home","Pulpit","⌂"),
        Triple("co1","Ogrzewanie","▥"),
        Triple("schedule","Harmonogram","□"),
        Triple("alarms","Alarmy","♧"),
        Triple("menu","Menu","☰")
    )
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().height(58.dp)
            .background(G2Bg).border(1.dp,G2Line),
        verticalAlignment=Alignment.CenterVertically
    ){
        for((id,label,glyph) in items){
            val selected=screen==id || (id=="co1" && screen.startsWith("co"))
            Column(
                Modifier.weight(1f).fillMaxHeight().clickable{
                    nav(if(id=="co1" && (ui.n("0x2105")?:0.0)==0.0) "home" else id)
                },
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.Center
            ){
                if(selected) Box(Modifier.fillMaxWidth(.38f).height(3.dp).clip(RoundedCornerShape(0,0,3,3)).background(G2Orange))
                Spacer(Modifier.height(2.dp))
                Text(glyph,color=if(selected)G2Orange else G2Mut,fontSize=20.sp)
                Text(label,color=if(selected)G2Orange else G2Mut,fontSize=9.5.sp,fontWeight=FontWeight.SemiBold)
            }
        }
    }
}

@Composable private fun HomeV2(ui:Ui,nav:(String)->Unit){
    val mods=configModules(ui)
    val co1=mods.firstOrNull{it=="co1"}
    val cwu=mods.firstOrNull{it=="cwu"}
    val others=mods.filter{it!="co1"&&it!="cwu"&&it!="fuel"}
    val cards=buildList{
        if(co1!=null)add("co1")
        if(cwu!=null)add("cwu")
        add("fuel"); add("palnik"); add("alarms"); add("can")
        addAll(others)
    }
    val pages=cards.chunked(6).ifEmpty{listOf(emptyList())}
    val scroll=rememberScrollState()
    val scope=rememberCoroutineScope()
    var page by remember{mutableIntStateOf(0)}
    BoxWithConstraints(Modifier.fillMaxSize()){
        val pageWidth=maxWidth
        val density=androidx.compose.ui.platform.LocalDensity.current
        LaunchedEffect(scroll.value,pageWidth,density){
            val px=with(density){pageWidth.toPx()}
            if(px>1f){ val p=(scroll.value/px).toInt().coerceIn(0,pages.lastIndex); if(page!=p)page=p }
        }
        Column(Modifier.fillMaxSize().padding(horizontal=12.dp,vertical=4.dp)){
            HeroV2(ui){nav("kociol")}
            Spacer(Modifier.height(8.dp))
            BurnerStripV2(ui)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().weight(1f).horizontalScroll(scroll)){
                for(p in pages){
                    Column(Modifier.width(pageWidth),verticalArrangement=Arrangement.spacedBy(8.dp)){
                        for(row in p.chunked(2)){
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                for(id in row) Box(Modifier.weight(1f)){HomeTileV2(ui,id,nav)}
                                if(row.size==1)Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            if(pages.size>1)Row(Modifier.fillMaxWidth().padding(3.dp),horizontalArrangement=Arrangement.Center){
                for(i in pages.indices)Box(Modifier.padding(3.dp).size(if(i==page)8.dp else 6.dp)
                    .clip(RoundedCornerShape(50)).background(if(i==page)G2Orange else G2Mut).clickable{
                        scope.launch{val px=with(density){pageWidth.toPx()};scroll.animateScrollTo((i*px).toInt())}
                    })
            }
        }
    }
}

@Composable private fun HeroV2(ui:Ui,click:()->Unit){
    val run=(ui.n("0x2005")?:0.0)!=0.0
    GlassV2(Modifier.fillMaxWidth().height(176.dp),if(run)G2Green else null,click){
        Row(Modifier.fillMaxSize(),verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.width(176.dp).fillMaxHeight(),contentAlignment=Alignment.Center){
                GaugeV2(ui,run)
            }
            Column(Modifier.weight(1f).padding(start=2.dp)){
                Text("Kocioł",color=G2Text,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                Text(if(run)"● Praca" else "Wyłączony",color=if(run)G2Orange2 else G2Mut,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                Text("zadana "+f1(ui.n("0x2004"),0)+" °C · tryb Auto",color=G2Mut,fontSize=10.sp)
                Spacer(Modifier.height(8.dp))
                V2Row("Powrót",f1(ui.n("0x2002"))+" °C")
                V2Row("Temp. palnika",f1(ui.n("0x2260"))+" °C")
                V2Row("Temp. zewnętrzna",f1(ui.n("0x2011"))+" °C")
                V2Row("Moc palnika",f1(ui.n("0x2250"),0)+" %")
            }
        }
    }
}
@Composable private fun GaugeV2(ui:Ui,run:Boolean){
    val temp=(ui.n("0x2001")?:0.0).toFloat()
    Canvas(Modifier.size(170.dp)){
        val stroke=10.dp.toPx()
        val r=size.minDimension/2-stroke/2
        drawArc(G2Line,135f,270f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(stroke,cap=StrokeCap.Round))
        drawArc(Brush.linearGradient(listOf(G2Orange,G2Orange2)),135f,(temp.coerceIn(0f,100f)/100f)*270f,false,
            style=androidx.compose.ui.graphics.drawscope.Stroke(stroke,cap=StrokeCap.Round))
    }
    Box(Modifier.size(170.dp),contentAlignment=Alignment.Center){
        Column(horizontalAlignment=Alignment.CenterHorizontally){
            Text(f1(ui.n("0x2001")),color=G2Text,fontSize=38.sp,fontWeight=FontWeight.Light)
            Text("°C",color=G2Orange,fontSize=14.sp)
            Text(if(run)"Praca" else "Wyłączony",color=if(run)G2Orange2 else G2Mut,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
        }
    }
}

@Composable private fun BurnerStripV2(ui:Ui){
    val scr=ui.m.screens["palnik"]?:return
    Row(
        Modifier.fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(G2Panel)
            .border(1.dp,G2Line,RoundedCornerShape(14.dp))
            .padding(horizontal=8.dp,vertical=6.dp),
        horizontalArrangement=Arrangement.SpaceEvenly,
        verticalAlignment=Alignment.CenterVertically
    ){
        for(c in scr.bar){
            if(!ui.v.containsKey(c.p))continue
            val active=(ui.n(c.p)?.toInt()?:0)!=0
            val led=if(active)G2Orange else Color(0xFF3B3B42)
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                Box(
                    Modifier.size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(led.copy(alpha=if(active).20f else .10f))
                        .border(1.dp,led.copy(alpha=if(active).75f else .25f),RoundedCornerShape(8.dp)),
                    contentAlignment=Alignment.Center
                ){
                    if(c.fr.isNotEmpty())Ic(c.fr.first(),20.dp)
                    else Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(led))
                }
            }
        }
    }
}

@Composable private fun HomeTileV2(ui:Ui,id:String,nav:(String)->Unit){
    when(id){
        "fuel"->{FuelTile(ui,Modifier.fillMaxWidth().height(96.dp));return}
        "palnik"->GlassV2(Modifier.fillMaxWidth().height(96.dp),null,{nav("palnik")}){
            Text("Palnik",color=G2Mut,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
            Text(if((ui.n("0x2250")?:0.0)>0)"Praca" else "Wyłączony",color=G2Text,fontSize=22.sp,fontWeight=FontWeight.Light)
            V2Row("jasność płomienia",f1(ui.n("0x2250"),0)+" %")
            V2Row("zapalarka",f1(ui.n("0x2284"),0))
        }
        "alarms"->GlassV2(Modifier.fillMaxWidth().height(76.dp),null,{nav("alarms")}){
            Text("Ostatni alarm",color=G2Mut,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
            Text("kod 140 · Brak ognia/opału",color=G2Text,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
            Text("w historii 20 zdarzeń · nie aktywny",color=G2Mut,fontSize=10.sp)
        }
        "can"->GlassV2(Modifier.fillMaxWidth().height(76.dp),G2Green,null){
            Text("Moduły CAN",color=G2Mut,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
            Text("1 online",color=G2Text,fontSize=20.sp,fontWeight=FontWeight.Light)
            Text("CAN IO BIG · Node 86",color=G2Mut,fontSize=10.sp)
        }
        else->{
            val s=ui.m.screens[id]?:return
            GlassV2(Modifier.fillMaxWidth().height(96.dp),moduleLed(ui,id),{nav(id)}){
                Text(tileTitle(id,s),color=G2Mut,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
                if(s.big!=null)BigV2(f1(ui.n(s.big)),s.bigUnit) else Text(if(moduleLed(ui,id)!=null)"Praca" else "Wyłączony",color=G2Text,fontSize=22.sp,fontWeight=FontWeight.Light)
                if(s.pill!=null)Text("zadana wynikowa  "+f1(ui.n(s.pill),0)+" °C",color=G2Mut,fontSize=10.sp)
                if(s.mode!=null)Text("tryb  Komfort",color=G2Mut,fontSize=10.sp)
            }
        }
    }
}

@Composable private fun ModuleV2(ui:Ui,id0:String,settings:Boolean,toggle:()->Unit){
    val id=if(id0=="co")"co1" else id0
    val s=ui.m.screens[id]?:return
    val scope=rememberCoroutineScope()
    var editRow by remember { mutableStateOf<SetRow?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        GlassV2(Modifier.fillMaxWidth().height(178.dp),moduleLed(ui,id)){
            if(s.image)BoilerImage(ui) else if(s.big!=null){
                Text(s.title,color=G2Text,fontSize=17.sp,fontWeight=FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp));BigV2(f1(ui.n(s.big)),s.bigUnit)
                if(s.pill!=null)Text("→ "+f1(ui.n(s.pill),0)+" °C",color=G2Orange,fontSize=15.sp)
                if(s.state!=null)Text(ui.m.statusTables[s.state]?.getOrNull(ui.n(s.stateP?:s.big)?.toInt()?:0)?:"--",color=G2Green,fontSize=13.sp)
            }
        }
        if(s.mode!=null){
            val cur=ui.n(s.mode)?.toInt()?:0
            GlassV2(Modifier.fillMaxWidth()){
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    for(m in ui.m.programModes){
                        val selected=m.v==cur
                        Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(10.dp)).background(if(selected)G2Orange.copy(.88f) else G2Panel2).border(1.dp,if(selected)G2Orange else G2Line,RoundedCornerShape(10.dp)).clickable{scope.launch{ui.src.write(s.mode,m.v.toDouble())}},contentAlignment=Alignment.Center){
                            Text(m.label,color=G2Text,fontSize=11.sp,maxLines=1)
                        }
                    }
                }
            }
        }
        if(s.kv.isNotEmpty())GlassV2(Modifier.fillMaxWidth()){
            for(k in s.kv){
                if(k.cond?.let{ui.n(it)?.toInt()!=0}==false)continue
                V2Row(k.label,f1(ui.n(k.pid),k.dec)+if(k.unit.isNotEmpty())" "+k.unit else "",active=false)
            }
        }
        if(s.bar.isNotEmpty())GlassV2(Modifier.fillMaxWidth()){
            for(c in s.bar)V2Row(c.l?:"Wyjście",if((ui.n(c.p)?.toInt()?:0)!=0) "Praca" else "Wyłączone",(ui.n(c.p)?.toInt()?:0)!=0)
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(G2Panel2).border(1.dp,G2Line,RoundedCornerShape(14.dp)).clickable{toggle()}.padding(15.dp),verticalAlignment=Alignment.CenterVertically){
            Text("⚙  Nastawy",color=G2Text,fontSize=15.sp,modifier=Modifier.weight(1f));Text(if(settings)"▲" else "▼",color=G2Orange)
        }
        if(settings)SettingsForScreenV2(ui,s){ editRow=it }
        editRow?.let { row -> EditSettingDialogV2(ui,row){ editRow=null } }
    }
}

@Composable private fun SettingsForScreenV2(ui:Ui,s:ScreenDef,onEdit:(SetRow)->Unit){
    val cls=s.list.substringBefore("|")
    val rows=ui.m.lists[cls].orEmpty().filter{it.levelMin<=ui.level}
    GlassV2(Modifier.fillMaxWidth()){
        for(r in rows){
            val value=ui.n(r.pid)
            val text=if(r.options!=null)r.options.firstOrNull{it.v==(value?:r.def).toInt()}?.label?:f1(value?:r.def,0) else f1(value?:r.def,if(r.step<1)1 else 0)
            V2Row(r.label,text,click=if(r.pid!=null){ { onEdit(r) } } else null)
        }
    }
}

@Composable private fun EditSettingDialogV2(ui:Ui,row:SetRow,close:()->Unit){
    val scope=rememberCoroutineScope()
    val current=ui.n(row.pid)?:row.def
    var text by remember(current){mutableStateOf(current.toString())}
    AlertDialog(
        onDismissRequest=close,
        containerColor=Color(0xFF16242D),
        titleContentColor=G2Text,
        textContentColor=G2Text,
        title={Text(row.label)},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
                if(row.options!=null){
                    for(opt in row.options){
                        val selected=opt.v==current.toInt()
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(if(selected)G2Orange.copy(alpha=.20f) else G2Panel2)
                                .clickable{scope.launch{ui.src.write(row.pid?:return@launch,opt.v.toDouble());close()}}
                                .padding(12.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            Text(opt.label,color=G2Text,modifier=Modifier.weight(1f))
                            if(selected)Text("✓",color=G2Orange,fontWeight=FontWeight.Bold)
                        }
                    }
                }else{
                    OutlinedTextField(
                        value=text,
                        onValueChange={text=it},
                        singleLine=true,
                        label={Text("Wartość")},
                        supportingText={Text("Zakres: "+row.min+" … "+row.max)}
                    )
                }
            }
        },
        confirmButton={
            if(row.options==null)TextButton(onClick={
                val x=text.replace(',','.').toDoubleOrNull()
                if(x!=null && x>=row.min && x<=row.max){
                    scope.launch{ui.src.write(row.pid?:return@launch,x);close()}
                }else Ctl.notify("Wartość poza zakresem")
            }){Text("Zapisz",color=G2Orange)}
        },
        dismissButton={TextButton(onClick=close){Text("Anuluj",color=G2Mut)}}
    )
}

@Composable private fun OutputTestV2(ui:Ui){
    val scroll=rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(10.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp)
    ){
        if(ui.level<2){
            GlassV2(Modifier.fillMaxWidth()){
                Text("Test wyjść",color=G2Text,fontSize=16.sp,fontWeight=FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Sterowanie wyjściami od poziomu 2 (PIN 0999).",color=G2Mut,fontSize=13.sp)
            }
        } else {
            for(t in TESTS) TestRowV2(ui,t)
        }
    }
}

@Composable private fun TestRowV2(ui:Ui,t:TestDef){
    val scope=rememberCoroutineScope()
    val cur=ui.n(t.pid)?.toInt()?:0
    val labels=if(t.closeOpen)listOf("A","CLOSE","OPEN") else listOf("A","OFF","ON")
    GlassV2(Modifier.fillMaxWidth()){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text(t.label,color=G2Text,fontSize=13.sp,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
            for(i in 0..2)Box(Modifier.padding(start=4.dp).width(55.dp).height(34.dp).clip(RoundedCornerShape(8.dp)).background(if(cur==i)G2Orange.copy(.9f) else G2Panel2).border(1.dp,if(cur==i)G2Orange else G2Line,RoundedCornerShape(8.dp)).clickable{scope.launch{ui.src.write(t.pid,i.toDouble())}},contentAlignment=Alignment.Center){Text(labels[i],color=G2Text,fontSize=10.sp)}
        }
        if(t.pctPid!=null){
            val pct=(ui.n(t.pctPid)?:0.0).toFloat().coerceIn(0f,100f)
            Row(Modifier.fillMaxWidth().padding(top=3.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("Moc testowa",color=G2Mut,fontSize=11.sp);Text(pct.toInt().toString()+" %",color=G2Orange,fontSize=11.sp)}
            Slider(value=pct,onValueChange={scope.launch{ui.src.write(t.pctPid,it.toDouble())}},valueRange=0f..100f,colors=SliderDefaults.colors(thumbColor=G2Text,activeTrackColor=G2Orange,inactiveTrackColor=G2Line))
        }
    }
}

@Composable private fun MenuV2(ui:Ui,nav:(String)->Unit){
    val rows=ui.m.menu.filter{ui.m.menuVisible(it,ui.v,ui.level)}
    LazyColumn(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
        items(rows){r->
            val route=ui.m.routeOf(r)
            GlassRowV2(r.title,r.levelMin>ui.level){if(route!=null)nav(route)}
        }
    }
}

@Composable private fun ListV2(ui:Ui,title:String,cls:String){
    val rows=ui.m.lists[cls].orEmpty()
    LazyColumn(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
        items(rows){r->GlassRowV2(r.label,r.levelMin>ui.level){}}
    }
}

@Composable private fun V2Row(label:String,value:String,active:Boolean=false,click:(()->Unit)?=null){
    Row(Modifier.fillMaxWidth().then(if(click!=null)Modifier.clickable(onClick=click) else Modifier).padding(vertical=9.dp,horizontal=3.dp),verticalAlignment=Alignment.CenterVertically){
        Text(label,color=G2Mut,fontSize=12.5.sp,modifier=Modifier.weight(1f))
        Text(value,color=if(active)G2Green else G2Text,fontSize=12.5.sp,fontWeight=FontWeight.SemiBold)
    }
    HorizontalDivider(color=G2Line)
}

@Composable private fun GlassRowV2(label:String,locked:Boolean,click:()->Unit){
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(G2Panel).border(1.dp,G2Line,RoundedCornerShape(12.dp)).clickable(onClick=click).padding(13.dp),verticalAlignment=Alignment.CenterVertically){
        Text(label+(if(locked)"  🔒" else ""),color=if(locked)G2Mut else G2Text,fontSize=14.sp,modifier=Modifier.weight(1f));Text("›",color=G2Orange,fontSize=24.sp)
    }
}

@Composable private fun GlassV2(modifier:Modifier,led:Color?=null,click:(()->Unit)?=null,content:@Composable ColumnScope.()->Unit){
    val shape=RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A1A1F),
                        Color(0xFF141418)
                    )
                )
            )
            .border(1.dp,G2Line,shape)
            .then(if(click!=null)Modifier.clickable(onClick=click) else Modifier)
            .padding(12.dp)
    ){
        if(led!=null){
            Box(
                Modifier.fillMaxWidth(.42f)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent,led.copy(alpha=.85f),Color.Transparent)
                        )
                    )
            )
            Spacer(Modifier.height(4.dp))
        }
        content()
    }
}

@Composable private fun BigV2(v:String,u:String){
    Row(verticalAlignment=Alignment.Bottom){Text(v,color=G2Text,fontSize=34.sp,fontWeight=FontWeight.Light);if(u.isNotEmpty())Text(u,color=G2Mut,fontSize=14.sp,modifier=Modifier.padding(start=3.dp,bottom=4.dp))}
}
