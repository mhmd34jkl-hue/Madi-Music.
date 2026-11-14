package com.madi.music

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Track(val id: String, val title: String, val artist: String, val coverResourceName: String)

object MusicCatalog {
    val tracks = listOf(
        Track("001", "Jak El Marsoul", "الشاب عقيل", "cover_jak_el_marsoul"),
        Track("002", "Histoire Kdima Jdiiid 2013", "الشاب عقيل", "cover_histoire_kdima"),
        Track("003", "El 3achk mamnou3", "الشاب عقيل", "cover_el_3achk_mamnou3"),
        Track("004", "Balaki · انسايني وانساي حكايتي", "الشاب مومو", "cover_balaki"),
        Track("005", "Roho Gouloulha · واقيلا عشق جديد", "الشاب مومو", "cover_roho_gouloulha"),
        Track("006", "Dikrayat · ذكريات", "الشاب مومو", "cover_dikrayat"),
        Track("007", "Atiteha L'amour · عطيتها (2025)", "الشاب مومو", "cover_atiteha_lamour"),
        Track("008", "كوالالمبور", "جندي الراب", "cover_kuala_lumpur"),
        Track("009", "لاريا", "جندي الراب", "cover_larya"),
        Track("010", "دمعة", "جندي الراب", "cover_dam3a"),
        Track("011", "يدوم عليهم (فرح كبير)", "رافع العكعوكي & علي العبيدي", "cover_yadoum_alayhom"),
        Track("012", "نحنا واحد مش اثنين (مصدر لارياح)", "رافع العكعوكي & مامو العزومي", "cover_nahna_wahed"),
        Track("013", "مانواطي شاني", "رافع العكعوكي", "cover_manawati_shani"),
        Track("014", "بابا نويل · BABA NOEL", "كولمبي", "cover_baba_noel"),
        Track("015", "عيونه", "كولمبي", "cover_oyouni"),
        Track("016", "نظرة", "كولمبي", "cover_nadhra")
    )

    fun mediaItems(): List<MediaItem> = tracks.map { track ->
        val artwork = Uri.parse("android.resource://com.madi.music/drawable/" + track.coverResourceName)
        MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(Uri.parse("asset:///music/" + track.id + ".mp3"))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setDisplayTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle("Madi Music")
                    .setArtworkUri(artwork)
                    .build()
            )
            .build()
    }
}
