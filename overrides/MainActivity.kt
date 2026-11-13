package com.madi.music

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

private val Night = Color(0xFF090711)
private val Panel = Color(0xFF171222)
private val PanelLight = Color(0xFF241832)
private val Violet = Color(0xFFAA68FF)
private val Muted = Color(0xFFB9ADC9)
private val SupportNumber = "218947088407"

class MainActivity : ComponentActivity() {
    private var controller by mutableStateOf<MediaController?>(null)
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MadiMusic(controller) }
        val token = SessionToken(this, ComponentName(this, MusicPlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture.addListener({ controller = controllerFuture.get() }, mainExecutor)
    }

    override fun onDestroy() {
        if (::controllerFuture.isInitialized) MediaController.releaseFuture(controllerFuture)
        controller = null
        super.onDestroy()
    }
}

@Composable
private fun MadiMusic(player: Player?) {
    var page by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var artistFilter by remember { mutableStateOf("الكل") }
    val favorites = remember { mutableStateListOf<String>() }
    val tracks = MusicCatalog.tracks
    val current = tracks[selectedIndex]
    val artists = listOf("الكل") + tracks.map { it.artist }.distinct()

    fun play(index: Int) {
        selectedIndex = index.coerceIn(0, tracks.lastIndex)
        player?.setMediaItem(MusicCatalog.mediaItems()[selectedIndex])
        player?.prepare()
        player?.play()
        playing = true
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Violet,
        background = Night,
        surface = Panel,
        onBackground = Color.White,
        onSurface = Color.White
    )) {
        Column(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF211333), Night, Night))
            )
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(46.dp).background(
                        Brush.linearGradient(listOf(Violet, Color(0xFF49236D))),
                        RoundedCornerShape(15.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("M", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("MADI MUSIC", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp)
                    Text("موسيقاك معك… حتى بدون إنترنت", color = Muted, fontSize = 11.sp)
                }
                Surface(color = Color(0x332B1746), shape = RoundedCornerShape(30.dp)) {
                    Text("● OFFLINE", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = Color(0xFFD7B5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (page) {
                    0 -> HomePage(
                        query, { query = it }, artistFilter, { artistFilter = it },
                        artists, tracks.mapIndexed { index, track -> index to track.title },
                        tracks.map { it.artist }, selectedIndex, favorites,
                        { id -> if (favorites.contains(id)) favorites.remove(id) else favorites.add(id) }, ::play,
                        onSearch = { page = 1 }
                    )
                    1 -> SearchPage(query, { query = it }, tracks, selectedIndex, favorites, { id -> if (favorites.contains(id)) favorites.remove(id) else favorites.add(id) }, ::play) 
                    2 -> TrackListPage("مكتبتي", "كل أغانيك في مكان واحد", tracks.indices.toList(), tracks, selectedIndex, favorites, { id -> if (favorites.contains(id)) favorites.remove(id) else favorites.add(id) }, ::play)
                    3 -> TrackListPage("الأغاني المفضلة", "الأغاني التي حفظتها بقلبك", tracks.indices.filter { favorites.contains(tracks[it].id) }, tracks, selectedIndex, favorites, { id -> if (favorites.contains(id)) favorites.remove(id) else favorites.add(id) }, ::play)
                    else -> SettingsPage()
                }
            }

            if (page != 4) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp).background(PanelLight, RoundedCornerShape(18.dp)).clickable { page = 2 }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(40.dp).background(Color(0xFF3A2452), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("♫", color = Color(0xFFE3C9FF), fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(current.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(current.artist, color = Muted, fontSize = 11.sp)
                    }
                    IconButton(onClick = { play(if (selectedIndex == 0) tracks.lastIndex else selectedIndex - 1) }) {
                        Icon(Icons.Default.SkipPrevious, null, tint = Color.White)
                    }
                    IconButton(onClick = {
                        if (playing) { player?.pause(); playing = false }
                        else { if (player?.currentMediaItem == null) play(selectedIndex) else { player?.play(); playing = true } }
                    }) {
                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Violet, modifier = Modifier.size(30.dp))
                    }
                    IconButton(onClick = { play((selectedIndex + 1) % tracks.size) }) {
                        Icon(Icons.Default.SkipNext, null, tint = Color.White)
                    }
                }
            }

            NavigationBar(containerColor = Color(0xFF100C19), contentColor = Muted) {
                val destinations = listOf(
                    Triple("الرئيسية", Icons.Default.Home, 0),
                    Triple("بحث", Icons.Default.Search, 1),
                    Triple("مكتبتي", Icons.Default.LibraryMusic, 2),
                    Triple("المفضلة", Icons.Default.Favorite, 3),
                    Triple("الإعدادات", Icons.Default.Settings, 4)
                )
                destinations.forEach { item ->
                    NavigationBarItem(
                        selected = page == item.third,
                        onClick = { page = item.third },
                        icon = { Icon(item.second, contentDescription = item.first) },
                        label = { Text(item.first, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Violet,
                            selectedTextColor = Color(0xFFDCC4FF),
                            indicatorColor = Color(0xFF38234F),
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun HomePage(
    query: String,
    onQuery: (String) -> Unit,
    artistFilter: String,
    onArtist: (String) -> Unit,
    artists: List<String>,
    indexedTitles: List<Pair<Int, String>>,
    artistNames: List<String>,
    selected: Int,
    favorites: List<String>,
    onFavorite: (String) -> Unit,
    onPlay: (Int) -> Unit,
    onSearch: () -> Unit
) {
    val tracks = MusicCatalog.tracks
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text("مساء الموسيقى ✨", color = Muted, fontSize = 13.sp)
        Text("اكتشف موسيقاك", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(14.dp))
        SearchField(query, onQuery, onSearch)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("الفنانون", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("5 فنانين", color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(artists.size) { i ->
                FilterChip(
                    selected = artistFilter == artists[i],
                    onClick = { onArtist(artists[i]) },
                    label = { Text(artists[i]) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF57317C),
                        selectedLabelColor = Color.White,
                        labelColor = Muted
                    )
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("اختيارات من مكتبتك", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("تشغيل مباشر دون اتصال بالإنترنت", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        val filtered = indexedTitles.filter { pair ->
            val track = tracks[pair.first]
            (artistFilter == "الكل" || track.artist == artistFilter) &&
                (query.isBlank() || track.title.contains(query, true) || track.artist.contains(query, true))
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
            itemsIndexed(filtered) { _, pair ->
                TrackRow(pair.first, tracks[pair.first], selected, favorites.contains(tracks[pair.first].id), { onPlay(pair.first) }, { onFavorite(tracks[pair.first].id) })
            }
        }
    }
}

@Composable
private fun SearchPage(
    query: String,
    onQuery: (String) -> Unit,
    tracks: List<Track>,
    selected: Int,
    favorites: List<String>,
    onFavorite: (String) -> Unit,
    onPlay: (Int) -> Unit
) {
    val results = tracks.indices.filter { i ->
        query.isNotBlank() && (tracks[i].title.contains(query, true) || tracks[i].artist.contains(query, true))
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text("البحث", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text("ابحث باسم الأغنية أو المغني", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        SearchField(query, onQuery, {})
        Spacer(Modifier.height(12.dp))
        if (query.isBlank()) {
            Text("جرّب البحث عن: الشاب مومو، جندي الراب، كوالالمبور…", color = Muted, fontSize = 13.sp)
        } else if (results.isEmpty()) {
            Text("لم نعثر على نتائج. جرّب اسماً آخر.", color = Muted, modifier = Modifier.padding(top = 20.dp))
        } else {
            Text("${results.size} نتيجة", color = Muted, fontSize = 12.sp)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(results) { _, index ->
                    TrackRow(index, tracks[index], selected, favorites.contains(tracks[index].id), { onPlay(index) }, { onFavorite(tracks[index].id) })
                }
            }
        }
    }
}

@Composable
private fun TrackListPage(
    title: String,
    subtitle: String,
    indices: List<Int>,
    tracks: List<Track>,
    selected: Int,
    favorites: List<String>,
    onFavorite: (String) -> Unit,
    onPlay: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        if (indices.isEmpty()) {
            Text("لا توجد أغاني هنا بعد. أضف الأغاني إلى المفضلة من المكتبة.", color = Muted, modifier = Modifier.padding(top = 20.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(indices) { _, index ->
                    TrackRow(index, tracks[index], selected, favorites.contains(tracks[index].id), { onPlay(index) }, {})
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    index: Int,
    track: Track,
    selected: Int,
    favorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(if (index == selected) Color(0xFF302044) else Panel, RoundedCornerShape(15.dp))
            .clickable(onClick = onPlay).padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).background(Color(0xFF3A2452), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text("♫", color = Color(0xFFE0C4FF), fontSize = 22.sp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Muted, fontSize = 11.sp)
        }
        IconButton(onClick = onFavorite) {\n            Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (favorite) Color(0xFFFF75AE) else Muted, modifier = Modifier.size(19.dp))\n        }
    }
}

@Composable
private fun SearchField(value: String, onValue: (String) -> Unit, onSearch: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("ابحث عن أغنية أو فنان…", color = Muted) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = Violet) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Violet,
            unfocusedBorderColor = Color(0xFF49365D),
            cursorColor = Violet
        )
    )
}

@Composable
private fun SettingsPage() {
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "غير معروف"
        } catch (_: Exception) { "غير معروف" }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text("الإعدادات", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text("تحكّم في تجربتك داخل Madi Music", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            item {
                SettingsCard("حسابك", "إدارة حسابك الشخصي", "👤") {
                    Text("لا تحتاج إلى حساب لتشغيل موسيقاك. ميزات الحساب والمزامنة يمكن إضافتها لاحقاً.", color = Muted, fontSize = 12.sp)
                }
            }
            item {
                SettingsCard("حول التطبيق", "معلومات عن Madi Music", "ⓘ") {
                    Text("Madi Music — مشغل موسيقى يضع مكتبتك في متناولك حتى دون اتصال بالإنترنت.", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("إصدار التطبيق: $version", color = Color(0xFFDCC4FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            item {
                SettingsCard("اتصل بالدعم", "نحن هنا لمساعدتك", "✉") {
                    Text("تواصل مع فريق الدعم عبر واتساب.", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$SupportNumber"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF63389A)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("فتح واتساب للدعم") }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, subtitle: String, symbol: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(17.dp)).padding(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(Color(0xFF38234F), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Text(symbol, color = Color(0xFFE2C8FF), fontSize = 18.sp)
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}
