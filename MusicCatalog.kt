package com.madi.music

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Track(val id: String, val title: String, val artist: String)

object MusicCatalog {
    val tracks = listOf(
        Track("001", "Jak El Marsoul", "الشاب عقيل"),
        Track("002", "Histoire Kdima", "الشاب عقيل"),
        Track("003", "El 3achk mamnou3", "الشاب عقيل"),
        Track("004", "Balaki · انسايني وانساي حكايتي", "الشاب مومو"),
        Track("005", "Roho Gouloulha · واقيلا عشق جديد", "الشاب مومو"),
        Track("006", "Dikrayat · ذكريات", "الشاب مومو"),
        Track("007", "عطيتها لامور", "الشاب مومو"),
        Track("008", "كوالالمبور", "جندي الراب"),
        Track("009", "لاريا", "جندي الراب"),
        Track("010", "دمعة", "جندي الراب"),
        Track("011", "يدوم عليهم (فرح كبير)", "رافع العكعوكي"),
        Track("012", "نحنا واحد مش اثنين", "رافع العكعوكي"),
        Track("013", "مانواطي شاني", "رافع العكعوكي"),
        Track("014", "بابا نويل", "كولمبي"),
        Track("015", "عيونه", "كولمبي"),
        Track("016", "نظرة", "كولمبي")
    )
    fun mediaItems(): List<MediaItem> = tracks.map { track ->
        MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(Uri.parse("asset:///music/${track.id}.mp3"))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist).build())
            .build()
    }
}
