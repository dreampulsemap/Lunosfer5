package io.lunosfer.dreamap.ui.viewmodel

import io.lunosfer.dreamap.data.model.Dream

object DreamReelsQueueHolder {
    var dreams: List<Dream> = emptyList()
        private set
    var startIndex: Int = 0
        private set

    fun set(dreams: List<Dream>, startIndex: Int) {
        this.dreams = dreams
        this.startIndex = if (dreams.isEmpty()) 0 else startIndex.coerceIn(0, dreams.size - 1)
    }
}
