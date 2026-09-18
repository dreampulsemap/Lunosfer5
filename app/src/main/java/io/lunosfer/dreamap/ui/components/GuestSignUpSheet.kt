package io.lunosfer.dreamap.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.Void900
import io.lunosfer.dreamap.util.GuestPrompt

/**
 * Misafir oturumda bir yazma islemi denendiginde acilan kayit daveti.
 *
 * MainScreen'de BIR KEZ ciziliyor; her tetikleyici GuestPrompt.show() cagirir
 * (bkz. util/GuestMode.kt). Tum metinler string kaynagindan gelir, boylece
 * uygulamanin 11 dilinde de cevrilidir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestSignUpSheet(onSignUp: () -> Unit) {
    val visible by GuestPrompt.visibleFlow.collectAsState()
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { GuestPrompt.dismiss() },
        sheetState = sheetState,
        containerColor = Void900
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.guest_sheet_title),
                color = AstralGold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.guest_sheet_desc),
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = {
                    GuestPrompt.dismiss()
                    onSignUp()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AstralGold,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = stringResource(R.string.guest_sheet_cta),
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = { GuestPrompt.dismiss() }) {
                Text(
                    text = stringResource(R.string.guest_sheet_dismiss),
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
