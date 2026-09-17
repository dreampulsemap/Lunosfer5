package io.lunosfer.dreamap.ui.viewmodel

import io.lunosfer.dreamap.data.model.Goal

/**
 * Vizyon kartlarına (Ana Sayfa / Keşfet / Vizyon / Profil — HEPSİ) tıklandığında,
 * tam ekran Reels görüntüleyicisine hangi liste üzerinde ve hangi index'ten
 * başlayarak kaydırma yapılacağını taşımak için basit bir bellek-içi tutucu.
 *
 * Navigation-Compose argümanları üzerinden bütün bir Goal listesini taşımak
 * (Parcelable/JSON serileştirme) gereksiz karmaşıklık yaratacağından, aynı
 * process içinde senkron navigasyon garantisine dayanan bu basit tutucu tercih
 * edildi — uygulamanın geri kalanındaki basitlik seviyesiyle tutarlı.
 */
object ReelsQueueHolder {
    var goals: List<Goal> = emptyList()
        private set
    var startIndex: Int = 0
        private set

    fun set(goals: List<Goal>, startIndex: Int) {
        this.goals = goals
        this.startIndex = if (goals.isEmpty()) 0 else startIndex.coerceIn(0, goals.size - 1)
    }
}
