package com.madi.music

import android.net.Uri
import android.os.Bundle
import android.content.ComponentName
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.common.Player
import com.google.common.util.concurrent.ListenableFuture
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

private data class Track(val title: String, val artist: String, val file: String)
private val tracks = listOf(
    Track("Jak El Marsoul", "الشاب عقيل", "001.mp3"),
    Track("Histoire Kdima", "الشاب عقيل", "002.mp3"),
    Track("El 3achk mamnou3", "الشاب عقيل", "003.mp3"),
    Track("Balaki · انسايني وانساي حكايتي", "الشاب مومو", "004.mp3"),
    Track("Roho Gouloulha · واقيلا عشق جديد", "الشاب مومو", "005.mp3"),
    Track("Dikrayat · ذكريات", "الشاب مومو", "006.mp3"),
    Track("Atiteha L'amour · عطيتها", "الشاب مومو", "007.mp3"),
    Track("كوالالمبور", "جندي الراب", "008.mp3"),
    Track("لاريا", "جندي الراب", "009.mp3"),
    Track("دمعة", "جندي الراب", "010.mp3"),
    Track("يدوم عليهم (فرح كبير)", "رافع العكعوكي", "011.mp3"),
    Track("نحنا واحد مش اثنين", "رافع العكعوكي", "012.mp3"),
    Track("مانواطي شاني", "رافع العكعوكي", "013.mp3"),
    Track("بابا نويل · BABA NOEL", "كولمبي", "014.mp3"),
    Track("عيونه", "كولمبي", "015.mp3"),
    Track("نظرة", "كولمبي", "016.mp3")
)

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
    val bg = Color(0xFF080710)
    val purple = Color(0xFF9B4DFF)
    var selected by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("الكل") }
    val favorites = remember { mutableStateListOf<Int>() }
    val artists = listOf("الكل", "الشاب عقيل", "الشاب مومو", "جندي الراب", "رافع العكعوكي", "كولمبي")
    val current = tracks[selected]
    fun play(index: Int) {
        selected = index
        val uri = Uri.parse("asset:///music/${tracks[index].file}")
        player?.setMediaItem(MediaItem.fromUri(uri)); player?.prepare(); player?.play()
        playing = true
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = purple, background = bg, surface = Color(0xFF171321))) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1B1030), bg, bg))).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(Brush.linearGradient(listOf(purple, Color(0xFF39205E))), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Text("M", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("MADI MUSIC", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    Text("موسيقاك معك… حتى بدون إنترنت", color = Color(0xFFB8A9CE), fontSize = 11.sp)
                }
                Text("OFFLINE", color = Color(0xFFCB9CFF), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color(0x332B1746), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 7.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("مكتبتك الموسيقية", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("16 أغنية • 5 فنانين", color = Color(0xFFB9ADC9), fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("ابحث عن أغنية أو فنان…", color = Color(0xFF9A8DAA)) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = purple, unfocusedBorderColor = Color(0xFF443653), cursorColor = purple))
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(artists.size) { i ->
                    val name = artists[i]
                    FilterChip(selected = artist == name, onClick = { artist = name }, label = { Text(name) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF542B80), selectedLabelColor = Color.White, labelColor = Color(0xFFD5C9E4)))
                }
            }
            Spacer(Modifier.height(8.dp))
            val visible = tracks.indices.filter { i -> (artist == "الكل" || tracks[i].artist == artist) && (tracks[i].title.contains(query, true) || tracks[i].artist.contains(query, true)) }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                itemsIndexed(visible) { _, index ->
                    val t = tracks[index]
                    Row(Modifier.fillMaxWidth().background(if (index == selected) Color(0xFF29183E) else Color(0xFF15111E), RoundedCornerShape(15.dp)).clickable { play(index) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).background(Color(0xFF35204D), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("♫", color = Color(0xFFD7B4FF), fontSize = 22.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.artist, color = Color(0xFFB3A4C4), fontSize = 12.sp)
                        }
                        IconButton(onClick = { if (favorites.contains(index)) favorites.remove(index) else favorites.add(index) }) { Icon(if (favorites.contains(index)) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "المفضلة", tint = if (favorites.contains(index)) Color(0xFFFF6EAB) else Color(0xFF9D8EAF)) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().background(Color(0xFF21152F), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(current.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(current.artist, color = Color(0xFFBBA9D0), fontSize = 11.sp)
                }
                IconButton(onClick = { play(if (selected == 0) tracks.lastIndex else selected - 1) }) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White) }
                IconButton(onClick = { if (playing) { player?.pause(); playing = false } else { if (player?.currentMediaItem == null) play(selected) else { player?.play(); playing = true } } }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color(0xFFD9B8FF), modifier = Modifier.size(34.dp)) }
                IconButton(onClick = { play((selected + 1) % tracks.size) }) { Icon(Icons.Default.SkipNext, null, tint = Color.White) }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
