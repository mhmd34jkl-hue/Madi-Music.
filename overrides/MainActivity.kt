package com.madi.music

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private val Night = Color(0xFF07070D)
private val Panel = Color(0xFF11111B)
private val PanelLight = Color(0xFF1B1727)
private val Violet = Color(0xFFB33CFF)
private val Muted = Color(0xFFA9A4B8)
private val SupportNumber = "218947088407"

class MainActivity : ComponentActivity() {
    private var controller by mutableStateOf<MediaController?>(null)
    private var signedInName by mutableStateOf(FirebaseAuth.getInstance().currentUser?.displayName ?: FirebaseAuth.getInstance().currentUser?.email)
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7201)
        }
        setContent {
            MadiMusic(controller, signedInName, ::startGoogleLogin, ::startFacebookLogin, ::signOut)
        }
        val token = SessionToken(this, ComponentName(this, MusicPlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture.addListener({ runCatching { controller = controllerFuture.get() } }, mainExecutor)
    }

    private fun startGoogleLogin() {
        runCatching {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()
            startActivityForResult(GoogleSignIn.getClient(this, options).signInIntent, 7101)
        }.onFailure {
            Toast.makeText(this, "تعذر بدء تسجيل Google. تحقق من إعداد Firebase.", Toast.LENGTH_LONG).show()
        }
    }

    private fun startFacebookLogin() {
        val provider = OAuthProvider.newBuilder("facebook.com")
        provider.addCustomParameter("display", "popup")
        auth.startActivityForSignInWithProvider(this, provider.build())
            .addOnSuccessListener { result ->
                signedInName = result.user?.displayName ?: result.user?.email ?: "حساب متصل"
                Toast.makeText(this, "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "تعذر تسجيل Facebook: ${error.localizedMessage ?: "حاول مرة أخرى"}", Toast.LENGTH_LONG).show()
            }
    }

    @Deprecated("Deprecated by Android, retained for Google Sign-In compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 7101 && resultCode == Activity.RESULT_OK) {
            runCatching {
                GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            }.onSuccess { account ->
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { result ->
                        signedInName = result.user?.displayName ?: result.user?.email ?: "حساب متصل"
                        Toast.makeText(this, "تم تسجيل الدخول عبر Google", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "تعذر إكمال تسجيل Google", Toast.LENGTH_LONG).show()
                    }
            }.onFailure {
                Toast.makeText(this, "لم يكتمل تسجيل Google", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun signOut() {
        auth.signOut()
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()
        GoogleSignIn.getClient(this, options).signOut()
        signedInName = null
        Toast.makeText(this, "تم تسجيل الخروج", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        if (::controllerFuture.isInitialized) MediaController.releaseFuture(controllerFuture)
        controller = null
        super.onDestroy()
    }
}

@Composable
private fun MadiMusic(
    player: Player?,
    signedInName: String?,
    onGoogleLogin: () -> Unit,
    onFacebookLogin: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("madi_music_preferences", Context.MODE_PRIVATE) }
    // Madi Music typography is fixed: users cannot switch the app font.
    val appFont = FontFamily(Font(R.font.iphone))
    var showIntro by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2800)
        showIntro = false
    }
    if (showIntro) {
        MadiIntroScreen()
    } else {
    var page by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var artistFilter by remember { mutableStateOf("الكل") }
    val favorites = remember { mutableStateListOf<String>() }
    val tracks = MusicCatalog.tracks
    val current = tracks[selectedIndex]
    val artists = listOf("الكل") + tracks.map { it.artist }.distinct()

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val newIndex = tracks.indexOfFirst { it.id == mediaItem?.mediaId }
                if (newIndex >= 0) selectedIndex = newIndex
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }

    fun play(index: Int) {
        val safeIndex = index.coerceIn(0, tracks.lastIndex)
        selectedIndex = safeIndex
        player?.setMediaItems(MusicCatalog.mediaItems(), safeIndex, 0L)
        player?.prepare()
        player?.play()
        playing = player != null
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Violet,
        background = Night,
        surface = Panel,
        onBackground = Color.White,
        onSurface = Color.White
    )) {
        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = appFont)) {
        Column(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF211333), Night, Night))
            ).statusBarsPadding()
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
                    5 -> NowPlayingPage(current, playing, { page = 0 }, { if (playing) { player?.pause(); playing = false } else { if (player?.currentMediaItem == null) play(selectedIndex) else { player?.play(); playing = true } } }, ::play, selectedIndex, tracks.lastIndex)
                    else -> SettingsPage(
                        signedInName, onGoogleLogin, onFacebookLogin, onSignOut
                    )
                }
            }

            if (page != 4) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp).background(PanelLight, RoundedCornerShape(18.dp)).clickable { page = 5 }.padding(12.dp),
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
    }
}


@Composable
private fun MadiIntroScreen() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0xFF382052), Color(0xFF120C1D), Color(0xFF050509)),
                radius = 950f
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(142.dp).background(
                    Brush.linearGradient(listOf(Color(0xFFB33CFF), Color(0xFF4A1D70), Color(0xFF17101F))),
                    RoundedCornerShape(38.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("M", color = Color.White, fontSize = 100.sp, fontWeight = FontWeight.Black)
                Text("♛", Modifier.align(Alignment.TopCenter).offset(y = (-17).dp), color = Color(0xFFE7D9F5), fontSize = 35.sp)
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "MADI MUSIC",
                color = Color.White,
                fontSize = 31.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(15.dp))
            Box(Modifier.width(76.dp).height(2.dp).background(
                Brush.horizontalGradient(listOf(Color.Transparent, Violet, Color.Transparent))
            ))
            Spacer(Modifier.height(18.dp))
            Text(
                "صُنع بكل شغف على يد شاب ليبي من ضواحي بنغازي",
                color = Color(0xFFE1C9F7),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(9.dp))
            Text("M + K = 78 ❤️", color = Color(0xFFE7C5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text("MADE IN LIBYA  •  🇱🇾", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        }
        Text(
            "OFFLINE MUSIC EXPERIENCE",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
            color = Color(0xFF80758E),
            fontSize = 9.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun HomePage(
    query: String, onQuery: (String) -> Unit, artistFilter: String, onArtist: (String) -> Unit,
    artists: List<String>, indexedTitles: List<Pair<Int, String>>, artistNames: List<String>,
    selected: Int, favorites: List<String>, onFavorite: (String) -> Unit, onPlay: (Int) -> Unit, onSearch: () -> Unit
) {
    val tracks = MusicCatalog.tracks
    val filtered = indexedTitles.filter { pair ->
        val track = tracks[pair.first]
        (artistFilter == "الكل" || track.artist == artistFilter) &&
            (query.isBlank() || track.title.contains(query, true) || track.artist.contains(query, true))
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 14.dp)
    ) {
        item {
            Column {
                Text("مساحتك الموسيقية", color = Muted, fontSize = 12.sp)
                Text("صوتك. مزاجك. عالمك.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(12.dp))
                SearchField(query, onQuery, onSearch)
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().background(
                        Brush.linearGradient(
                            listOf(Color(0xFF7130A0), Color(0xFF34204F), Color(0xFF14111E))
                        ),
                        RoundedCornerShape(24.dp)
                    ).padding(18.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("MADI MUSIC", color = Color(0xFFF0E4FF), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            Spacer(Modifier.weight(1f))
                            Surface(
                                color = Color(0x3322D69A),
                                shape = RoundedCornerShape(30.dp)
                            ) {
                                Text("● OFFLINE", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color(0xFF9AF0C9), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(13.dp))
                        Text("موسيقاك… بأسلوبك", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                        Text("كل نغمة قريبة منك، حتى بدون إنترنت.", color = Color(0xFFE0D0EF), fontSize = 12.sp)
                        Spacer(Modifier.height(17.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("16 أغنية", "5 فنانين", "صوتك الخاص").forEach { label ->
                                Surface(color = Color(0x22FFFFFF), shape = RoundedCornerShape(12.dp)) {
                                    Text(label, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Color(0xFFF2E7FF), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("الفنانون", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(artists.size) { i ->
                        FilterChip(
                            selected = artistFilter == artists[i],
                            onClick = { onArtist(artists[i]) },
                            label = { Text(artists[i], fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF63318A),
                                selectedLabelColor = Color.White,
                                labelColor = Muted
                            )
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("كل الأغاني", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.weight(1f))
                    Text("${filtered.size} أغنية", color = Muted, fontSize = 11.sp)
                }
            }
        }
        if (filtered.isEmpty()) {
            item { Text("لا توجد نتائج مطابقة. جرّب اسماً آخر.", color = Muted, modifier = Modifier.padding(vertical = 22.dp)) }
        } else {
            itemsIndexed(filtered) { _, pair ->
                val index = pair.first
                TrackRow(
                    index, tracks[index], selected,
                    favorites.contains(tracks[index].id),
                    { onPlay(index) },
                    { onFavorite(tracks[index].id) }
                )
            }
        }
    }
}

@Composable
private fun NowPlayingPage(track: Track, playing: Boolean, onBack: () -> Unit, onToggle: () -> Unit, onPlayIndex: (Int) -> Unit, selectedIndex: Int, lastIndex: Int) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Home, "رجوع", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            Text("يُشغّل الآن", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Settings, null, tint = Muted)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Brush.linearGradient(listOf(Color(0xFF8C35C9), Color(0xFF2C1746), Color(0xFF10101A))), RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().padding(18.dp).background(Color(0x22FFFFFF), RoundedCornerShape(22.dp)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("M", color = Color.White, fontSize = 112.sp, fontWeight = FontWeight.Black)
                Text("M A D I   M U S I C", color = Color(0xFFE8D5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(26.dp))
        Text(track.title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.artist, color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("00:00", color = Muted, fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            Text("محفوظة دون إنترنت", color = Color(0xFFD9B4FF), fontSize = 10.sp)
        }
        Spacer(Modifier.height(7.dp))
        LinearProgressIndicator(progress = 0f, modifier = Modifier.fillMaxWidth().height(4.dp), color = Violet, trackColor = PanelLight)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onPlayIndex(if (selectedIndex <= 0) lastIndex else selectedIndex - 1) }) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
            Surface(color = Violet, shape = RoundedCornerShape(50), modifier = Modifier.size(68.dp).clickable(onClick = onToggle)) {
                Box(contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(38.dp)) }
            }
            IconButton(onClick = { onPlayIndex(if (selectedIndex >= lastIndex) 0 else selectedIndex + 1) }) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
        }
        Spacer(Modifier.height(20.dp))
        Text("استمتع بموسيقاك في أي مكان", color = Muted, fontSize = 12.sp)
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
                    TrackRow(index, tracks[index], selected, favorites.contains(tracks[index].id), { onPlay(index) }, { onFavorite(tracks[index].id) })
                }
            }
        }
    }
}

@Composable
private fun TrackRow(index: Int, track: Track, selected: Int, favorite: Boolean, onPlay: () -> Unit, onFavorite: () -> Unit) {
    val palettes = listOf(listOf(Color(0xFF5C268B), Color(0xFF21152F)), listOf(Color(0xFF174D67), Color(0xFF14202D)), listOf(Color(0xFF85462B), Color(0xFF2B1A25)), listOf(Color(0xFF67325F), Color(0xFF211629)))
    Row(
        Modifier.fillMaxWidth()
            .background(if (index == selected) Color(0xFF2B1D3B) else Panel, RoundedCornerShape(19.dp))
            .clickable(onClick = onPlay)
            .padding(horizontal = 11.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(57.dp).background(
                Brush.linearGradient(palettes[index % palettes.size]),
                RoundedCornerShape(15.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("M", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("♫", color = Color(0xFFEAD8FF), fontSize = 10.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(track.artist, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (index == selected) {
                Spacer(Modifier.height(5.dp))
                Text("يُشغّل الآن", color = Color(0xFFD9B6FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(onClick = onFavorite) {
            Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (favorite) Color(0xFFFF6FAE) else Muted, modifier = Modifier.size(20.dp))
        }
        Icon(Icons.Default.PlayArrow, null, tint = if (index == selected) Violet else Muted, modifier = Modifier.padding(end = 5.dp))
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
private fun SettingsPage(
    signedInName: String?,
    onGoogleLogin: () -> Unit,
    onFacebookLogin: () -> Unit,
    onSignOut: () -> Unit
) {
    var showEqualizer by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val version = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.1" }
        catch (_: Exception) { "1.1" }
    }
    LazyColumn(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF211333), Night, Night))).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("مركز التحكم", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("كل تفاصيل تجربتك في مكان واحد", color = Muted, fontSize = 12.sp)
                    }
                    Box(Modifier.size(48.dp).background(Brush.linearGradient(listOf(Color(0xFF8F3BC5), Color(0xFF302044))), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Text("⚙", color = Color.White, fontSize = 25.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF64308A), Color(0xFF2B1B40), Color(0xFF17121F))), RoundedCornerShape(23.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(52.dp).background(Color(0x33FFFFFF), RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                        Text("M", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text("MADI MUSIC", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                        Text("تجربتك الموسيقية، تحت سيطرتك", color = Color(0xFFE0CBEF), fontSize = 10.sp)
                    }
                    Surface(color = Color(0x3326C98A), shape = RoundedCornerShape(20.dp)) {
                        Text("OFFLINE", Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = Color(0xFF9BE8C7), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            SettingsSection("الحساب", "إدارة اتصالك بحساب Madi Music") {
                if (signedInName == null) {
                    Text("استمع دون حساب، أو سجّل الدخول لاستخدام حسابك.", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onGoogleLogin, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF241B31)), shape = RoundedCornerShape(14.dp)) {
                        Text("G   المتابعة باستخدام Google", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onFacebookLogin, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                        Text("f   المتابعة باستخدام Facebook", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(signedInName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("الحساب متصل", color = Color(0xFF8FE0B7), fontSize = 10.sp)
                    TextButton(onClick = onSignOut) { Text("تسجيل الخروج", color = Color(0xFFFFA7B5)) }
                }
            }
        }
        item {
            SettingsSection("الصوت والتشغيل", "تحكّم في طابع الصوت وتجربة الاستماع") {
                SettingsRow("♫", "معادل الصوت الاحترافي", "ترددات متعددة وإعدادات صوتية جاهزة", "AUDIO") { showEqualizer = true }
                SettingsRow("▣", "التشغيل في الخلفية", "تحكّم من الإشعارات وشاشة القفل", null) {
                    Toast.makeText(context, "يمكنك التحكم بالتشغيل من لوحة الإشعارات أثناء تشغيل الموسيقى", Toast.LENGTH_SHORT).show()
                }
            }
        }
        item {
            SettingsSection("المظهر والتخصيص", "هوية Madi Music ثابتة وأنيقة") {
                SettingsRow("Aa", "الخط المعتمد", "خط التطبيق موحّد ولا يمكن تغييره", "FIXED") {
                    Toast.makeText(context, "خط Madi Music موحّد ولا يمكن تغييره من داخل التطبيق", Toast.LENGTH_SHORT).show()
                }
                SettingsRow("文", "اللغة", "العربية حالياً • دعم الإنجليزية قيد التطوير", null) {
                    Toast.makeText(context, "الواجهة العربية مفعّلة حالياً", Toast.LENGTH_SHORT).show()
                }
            }
        }
        item {
            SettingsSection("مكتبتك", "إدارة موسيقاك المحلية") {
                SettingsRow("♡", "الأغاني المفضلة", "اعثر على الأغاني التي أحببتها", null) {
                    Toast.makeText(context, "افتح تبويب المفضلة من الشريط السفلي", Toast.LENGTH_SHORT).show()
                }
            }
        }
        item {
            SettingsSection("المساعدة ومعلومات التطبيق", "نحن قريبون عندما تحتاج إلينا") {
                SettingsRow("✉", "الدعم والمساعدة", "تواصل معنا عبر واتساب", "HELP") {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$SupportNumber"))) }
                }
                SettingsRow("ⓘ", "حول Madi Music", "الإصدار $version • صنع في ليبيا", null) {
                    Toast.makeText(context, "Madi Music • Made with passion in Libya", Toast.LENGTH_LONG).show()
                }
            }
        }
        item {
            Text("صُنع بكل شغف على يد شاب ليبي من ضواحي بنغازي  •  M + K = 78 ❤️", Modifier.fillMaxWidth().padding(top = 3.dp), color = Color(0xFF8F829E), fontSize = 9.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
    if (showEqualizer) EqualizerDialog(onDismiss = { showEqualizer = false })
}

@Composable
private fun EqualizerDialog(onDismiss: () -> Unit) {
    val count = MadiAudioEffects.bandCount().coerceIn(3, 10)
    val range = MadiAudioEffects.levelRange()
    val levels = remember(count) { mutableStateListOf<Float>().apply { repeat(count) { add(0f) } } }
    val labels = remember(count) {
        (0 until count).map { band ->
            val hz = MadiAudioEffects.centerFrequencyHz(band)
            if (hz >= 1000) "${hz / 1000.0} kHz" else if (hz > 0) "${hz} Hz" else "Band ${band + 1}"
        }
    }
    val presets = listOf(
        "متوازن" to List(count) { 0f },
        "جهير قوي" to List(count) { i -> if (i < count / 3) 5f else if (i > count * 2 / 3) -1f else 1f },
        "صوت واضح" to List(count) { i -> if (i < count / 3) -1f else if (i > count * 2 / 3) 4f else 2f },
        "هادئ" to List(count) { i -> if (i < count / 3) 2f else if (i > count * 2 / 3) -2f else 0f }
    )
    var selectedPreset by remember { mutableStateOf("متوازن") }
    fun applyPreset(name: String) {
        selectedPreset = name
        val values = presets.first { it.first == name }.second
        levels.indices.forEach { i ->
            val value = values[i].coerceIn(range.first, range.second)
            levels[i] = value
            MadiAudioEffects.setBandLevel(i, value)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF15101F),
        title = {},
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 590.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("MADI AUDIO", color = Color(0xFFD9B5FF), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("معادل الصوت", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                        Text("شكّل صوتك بالطريقة التي تحبها", color = Muted, fontSize = 11.sp)
                    }
                    Box(Modifier.size(48.dp).background(Brush.linearGradient(listOf(Color(0xFF8D38C6), Color(0xFF302044))), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Text("♫", color = Color.White, fontSize = 27.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("أنماط جاهزة", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset.first,
                            onClick = { applyPreset(preset.first) },
                            label = { Text(preset.first, fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF66318C), selectedLabelColor = Color.White, labelColor = Muted)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الترددات", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("dB", color = Color(0xFFD9B5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.fillMaxWidth().background(Color(0xFF21182D), RoundedCornerShape(18.dp)).padding(12.dp)) {
                    levels.indices.forEach { i ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(labels[i], color = Color(0xFFE9DDF4), fontSize = 9.sp, modifier = Modifier.width(57.dp))
                            Slider(
                                value = levels[i],
                                onValueChange = {
                                    levels[i] = it
                                    selectedPreset = "مخصص"
                                    MadiAudioEffects.setBandLevel(i, it)
                                },
                                valueRange = range.first..range.second,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFE7C9FF), activeTrackColor = Violet, inactiveTrackColor = Color(0xFF493653))
                            )
                            Text("${levels[i].toInt()}", color = Color(0xFFD9B5FF), fontSize = 9.sp, modifier = Modifier.width(25.dp))
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
                Text("التعديلات تطبّق على جلسة التشغيل الحالية حسب دعم الجهاز.", color = Muted, fontSize = 10.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("حفظ وإغلاق", color = Color(0xFFD9B5FF), fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = { applyPreset("متوازن") }) { Text("إعادة ضبط", color = Muted) } }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(21.dp)).padding(15.dp)
    ) {
        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = Muted, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SettingsRow(
    symbol: String,
    title: String,
    subtitle: String,
    tag: String?,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(39.dp).background(Color(0xFF30213F), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, color = Color(0xFFE1C2FF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (tag != null) {
            Surface(color = Color(0x332B1746), shape = RoundedCornerShape(20.dp)) {
                Text(tag, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Color(0xFFD3B3F4), fontSize = 9.sp)
            }
        } else {
            Text("›", color = Color(0xFF8D7A9F), fontSize = 22.sp, modifier = Modifier.padding(horizontal = 5.dp))
        }
    }
}
