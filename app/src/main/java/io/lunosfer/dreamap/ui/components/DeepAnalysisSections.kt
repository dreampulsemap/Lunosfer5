package io.lunosfer.dreamap.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.DeepAnalysisContent
import io.lunosfer.dreamap.data.model.DeepEmotion
import io.lunosfer.dreamap.data.model.DeepSymbol
import io.lunosfer.dreamap.ui.theme.AetherViolet
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.SerifFontFamily
import io.lunosfer.dreamap.ui.theme.Void800

/**
 * Derin analizin bolumlu gorunumu — web'deki DreamAnalysisView.jsx ile ayni
 * bolumler ve ayni sira: baslik/ozet, arketipler, golgeler, bireylesme,
 * semboller, duygular, yansima sorulari. Sunucu bu alanlarin bir kismini
 * bos birakabildigi icin her bolum kendi icerigi varsa ciziliyor.
 */
@Composable
fun DeepAnalysisSections(
    content: DeepAnalysisContent,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!content.title.isNullOrBlank() || !content.summary.isNullOrBlank()) {
            SectionCard {
                content.title?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = AstralGold,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                }
                content.summary?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }

        if (content.archetypes.isNotEmpty()) {
            SectionCard {
                SectionHeader(stringResource(R.string.deep_section_archetypes))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    content.archetypes.forEach { archetype ->
                        BulletLine(archetype, AstralGold)
                    }
                }
            }
        }

        if (!content.shadowFocus.isNullOrBlank() || !content.coreConflict.isNullOrBlank()) {
            SectionCard {
                SectionHeader(stringResource(R.string.deep_section_shadows))
                content.shadowFocus?.takeIf { it.isNotBlank() }?.let { Paragraph(it) }
                content.coreConflict?.takeIf { it.isNotBlank() }?.let { Paragraph(it) }
            }
        }

        if (!content.symbolicReading.isNullOrBlank() || !content.individuationPath.isNullOrBlank()) {
            SectionCard {
                SectionHeader(stringResource(R.string.deep_section_individuation))
                content.symbolicReading?.takeIf { it.isNotBlank() }?.let { Paragraph(it) }
                content.individuationPath?.takeIf { it.isNotBlank() }?.let { Paragraph(it) }
            }
        }

        if (content.symbols.isNotEmpty()) {
            SectionCard {
                SectionHeader(stringResource(R.string.deep_section_symbols))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    content.symbols.forEach { SymbolRow(it) }
                }
            }
        }

        if (content.emotions.isNotEmpty()) {
            SectionCard {
                SectionHeader(stringResource(R.string.deep_section_emotions))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    content.emotions.forEach { EmotionRow(it) }
                }
            }
        }

        if (content.reflectionQuestions.isNotEmpty()) {
            SectionCard {
                SectionHeader(stringResource(R.string.deep_section_reflections))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    content.reflectionQuestions.forEach { question ->
                        Text(
                            text = "“$question”",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(body: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Void800.copy(alpha = 0.8f)),
        border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            body()
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AetherViolet)
        )
        Text(
            text = title,
            color = AstralGold,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(text = text, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
}

@Composable
private fun BulletLine(text: String, dotColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(text = text, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun SymbolRow(symbol: DeepSymbol) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = symbol.symbol,
                color = AstralGold,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            symbol.intensity?.let { IntensityDots(it) }
        }
        symbol.meaning?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun EmotionRow(emotion: DeepEmotion) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = emotion.emotion,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        emotion.score?.let { IntensityDots(it) }
    }
}

/** 1-5 arasi yogunluk; sunucu bazen bu araligin disinda deger donebiliyor. */
@Composable
private fun IntensityDots(value: Int) {
    val filled = value.coerceIn(0, 5)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < filled) AetherViolet else Color.White.copy(alpha = 0.2f)
                    )
            )
        }
    }
}
