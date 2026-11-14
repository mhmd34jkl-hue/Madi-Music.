package com.madi.music

import android.media.audiofx.Equalizer

object MadiAudioEffects {
    @Volatile private var equalizer: Equalizer? = null

    @Synchronized
    fun attach(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        runCatching {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
        }.onFailure {
            equalizer = null
        }
    }

    fun bandCount(): Int = runCatching {
        equalizer?.numberOfBands?.toInt() ?: 5
    }.getOrDefault(5)

    fun levelRange(): Pair<Float, Float> = runCatching {
        val range = equalizer?.bandLevelRange
        if (range != null && range.size >= 2) {
            range[0] / 100f to range[1] / 100f
        } else {
            -15f to 15f
        }
    }.getOrDefault(-15f to 15f)

    fun centerFrequencyHz(band: Int): Int = runCatching {
        (equalizer?.getCenterFreq(band.toShort()) ?: 0) / 1000
    }.getOrDefault(0)

    fun setBandLevel(band: Int, levelDb: Float) {
        runCatching {
            equalizer?.setBandLevel(band.toShort(), (levelDb * 100f).toInt().toShort())
        }
    }

    @Synchronized
    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
    }
}
