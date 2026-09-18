package io.lunosfer.dreamap.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.util.GuestMode
import io.lunosfer.dreamap.service.LunosferMessagingService
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.theme.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Github
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.util.Locale

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val username: String,
    val updated_at: String,
    val created_at: String? = null
)

/** ensureUserProfile icin: sadece kimlik alanlarini okuyan hafif satir. */
@Serializable
private data class ProfileIdentityRow(
    val id: String,
    val username: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
    val sessionStatus by supabaseClient.auth.sessionStatus.collectAsState(initial = io.github.jan.supabase.auth.status.SessionStatus.Initializing)
    LaunchedEffect(sessionStatus) {
        // Misafir oturumu da "Authenticated" sayiliyor. Bu kontrol misafiri
        // dislamasaydi, kayit davetinden bu ekrana gelen misafir ayni karede
        // geri Ana Sayfa'ya atiliyordu ve hesabini hic olusturamiyordu
        // (emÃ¼latorde goruldu). Misafirin buraya gelmesinin TEK sebebi
        // zaten anonim oturumdan cikip gercek hesap acmak.
        if (sessionStatus is io.github.jan.supabase.auth.status.SessionStatus.Authenticated &&
            !GuestMode.isGuest()
        ) {
            onLoginSuccess()
        }
    }

    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void950)
            // Kucuk ekranlarda (ve klavye acikken) kart ekrana sigmiyordu:
            // "Kayit ol" modunda ekstra alan eklendiginde alttaki butonlara
            // ulasmanin hicbir yolu yoktu. Artik icerik kaydirilabiliyor.
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Void900),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isLogin) stringResource(R.string.auth_title) else stringResource(R.string.auth_register_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = SerifFontFamily),
                    color = AstralGold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.auth_email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedLabelColor = AstralGold,
                        unfocusedLabelColor = Color.LightGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.auth_password)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Filled.Visibility
                        else
                            Icons.Filled.VisibilityOff

                        val description = if (passwordVisible)
                            stringResource(R.string.auth_hide_password)
                        else
                            stringResource(R.string.auth_show_password)

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = description,
                                tint = AstralGold
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedLabelColor = AstralGold,
                        unfocusedLabelColor = Color.LightGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                if (!isLogin) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.auth_username)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AstralGold,
                            unfocusedBorderColor = Void800,
                            focusedLabelColor = AstralGold,
                            unfocusedLabelColor = Color.LightGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        val emailInput = email.trim()
                        val passwordInput = password
                        // Onceden bos alanda butona basinca HICBIR sey olmuyordu
                        // (sessiz return) â€” kullanici butonun bozuk oldugunu
                        // saniyordu.
                        if (emailInput.isBlank() || passwordInput.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.auth_error_fields_required), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                            Toast.makeText(context, context.getString(R.string.auth_error_invalid_email), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!isLogin && username.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.auth_username_required), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!isLogin && passwordInput.length < 6) {
                            Toast.makeText(context, context.getString(R.string.auth_error_password_too_short), Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true
                        coroutineScope.launch {
                            try {
                                if (isLogin) {
                                    supabaseClient.auth.signInWith(Email) {
                                        this.email = emailInput
                                        this.password = passwordInput
                                    }
                                } else {
                                    supabaseClient.auth.signUpWith(Email) {
                                        this.email = emailInput
                                        this.password = passwordInput
                                        // E-posta onayi aciksa kayit aninda oturum
                                        // olusmuyor; secilen kullanici adi kaybolmasin
                                        // diye metadata'da tasiniyor (ilk giriste
                                        // ensureUserProfile uyguluyor).
                                        this.data = buildJsonObject { put("username", username.trim()) }
                                    }
                                }

                                val user = supabaseClient.auth.currentUserOrNull()
                                if (user != null) {
                                    ensureUserProfile(
                                        userId = user.id,
                                        email = user.email ?: emailInput,
                                        desiredUsername = if (!isLogin) username.trim() else pendingUsernameFromMetadata(user)
                                    )
                                    applySavedLanguage(user.id)
                                }

                                if (!isLogin) {
                                    Toast.makeText(context, context.getString(R.string.auth_success), Toast.LENGTH_LONG).show()
                                    isLogin = true
                                } else {
                                    LunosferMessagingService.registerCurrentFcmToken()
                                    onLoginSuccess()
                                }
                            } catch (e: Exception) {
                                // Ham Supabase hatasi (URL + header dokumu iceren
                                // teknik metin) yerine anlasilir mesaj.
                                Toast.makeText(context, friendlyAuthError(context, e, isLogin), Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AstralGold, contentColor = Void950),
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) stringResource(R.string.auth_loading) else if (isLogin) stringResource(R.string.auth_login) else stringResource(R.string.auth_register))
                }

                // Sifresini unutan kullanicinin uygulamaya girmesinin hicbir yolu
                // yoktu â€” ne burada ne web'de sifirlama akisi vardi. Supabase
                // sifirlama e-postasi gonderiyor; e-postadaki link web'deki
                // /auth/reset sayfasina dusup yeni sifre belirlemeyi sagliyor.
                if (isLogin) {
                    TextButton(
                        onClick = {
                            if (email.isBlank()) {
                                Toast.makeText(context, context.getString(R.string.auth_reset_needs_email), Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    supabaseClient.auth.resetPasswordForEmail(
                                        email = email.trim(),
                                        redirectUrl = "https://www.lunosfer.com/auth/reset"
                                    )
                                } catch (e: Exception) {
                                    // Hatayi da yutuyoruz: farkli mesajlar hangi
                                    // e-postanin kayitli oldugunu sizdirir.
                                } finally {
                                    isLoading = false
                                    Toast.makeText(context, context.getString(R.string.auth_reset_sent), Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = stringResource(R.string.auth_forgot_password),
                            color = AstralGold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                                    text = stringResource(R.string.auth_or_separator),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                OutlinedButton(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                supabaseClient.auth.signInAnonymously()
                                LunosferMessagingService.registerCurrentFcmToken()
                                onLoginSuccess()
                            } catch (e: Exception) {
                                Toast.makeText(context, friendlyAuthError(context, e, isLogin = true), Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AetherCyan)
                ) {
                    Text(stringResource(R.string.auth_continue_as_guest))
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val url = supabaseClient.auth.getOAuthUrl(provider = Google, redirectUrl = "io.lunosfer.dreamap://auth-callback")
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                            } catch (e: Exception) {
                                Toast.makeText(context, friendlyAuthError(context, e, isLogin = true), Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.action_login_google))
                }
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val url = supabaseClient.auth.getOAuthUrl(provider = Github, redirectUrl = "io.lunosfer.dreamap://auth-callback")
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                            } catch (e: Exception) {
                                Toast.makeText(context, friendlyAuthError(context, e, isLogin = true), Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.action_login_github))
                }

                Spacer(Modifier.height(16.dp))

                TextButton(onClick = { isLogin = !isLogin }) {
                    Text(
                        text = if (isLogin) stringResource(R.string.auth_no_account) else stringResource(R.string.auth_has_account),
                        color = AetherCyan
                    )
                }
            }
        }
    }
}

/** Veritabanindaki sanitize_username() trigger'inin urettigi varsayilan adin istemci tarafi karsiligi. */
private fun sanitizedDefaultUsername(email: String?): String {
    val base = email
        ?.substringBefore("@")
        ?.lowercase(Locale.US)
        ?.replace(Regex("""\s+"""), "_")
        ?.replace(Regex("[^a-z0-9_]"), "")
        ?.take(24)
    return if (base.isNullOrBlank()) "dreamer" else base
}

/** Kayit sirasinda metadata'ya yazilan, henuz profile uygulanmamis kullanici adi. */
private fun pendingUsernameFromMetadata(user: io.github.jan.supabase.auth.user.UserInfo): String? =
    (user.userMetadata?.get("username") as? JsonPrimitive)
        ?.contentOrNull
        ?.trim()
        ?.takeIf { it.isNotBlank() }

/**
 * Profil satirinin var oldugundan emin olur.
 *
 * ONEMLI: Var olan bir profilin username'ini ARTIK EZMIYOR. Onceden her
 * giriste `upsert(username = e-postanin @ oncesi)` calisiyordu; kullanicinin
 * Profil > Duzenle'den verdigi kullanici adi her girisinde geri aliniyordu
 * (user_profiles.sanitize_username trigger'i da bunu kucultup benzersizlestiriyordu).
 * Yeni kullanici adi yalnizca (a) profil satiri hic yoksa, veya (b) satirdaki ad
 * hala e-postadan turetilmis varsayilan ad ise ve kayitta bir ad secilmisse yazilir.
 */
private suspend fun ensureUserProfile(userId: String, email: String?, desiredUsername: String?) {
    val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(java.util.Date())

    val existing = runCatching {
        supabaseClient.postgrest["user_profiles"]
            .select(columns = Columns.list("id", "username")) { filter { eq("id", userId) } }
            .decodeList<ProfileIdentityRow>()
            .firstOrNull()
    }.getOrNull()

    if (existing == null) {
        val payload = mutableMapOf(
            "id" to userId,
            "username" to (desiredUsername?.takeIf { it.isNotBlank() } ?: sanitizedDefaultUsername(email)),
            "updated_at" to now
        )
        if (!email.isNullOrBlank()) payload["email"] = email
        runCatching { supabaseClient.postgrest["user_profiles"].upsert(payload) { onConflict = "id" } }
        return
    }

    val stillDefaultName = existing.username.isNullOrBlank() || existing.username == sanitizedDefaultUsername(email)
    if (!desiredUsername.isNullOrBlank() && desiredUsername != existing.username && stillDefaultName) {
        // Kullanici adi baskasinda olabilir (UNIQUE) â€” basarisiz olursa sessizce
        // varsayilanda kaliyoruz, kullanici Profil > Duzenle'den degistirebilir.
        runCatching {
            supabaseClient.postgrest["user_profiles"]
                .update(mapOf("username" to desiredUsername, "updated_at" to now)) { filter { eq("id", userId) } }
        }
    }

    if (!email.isNullOrBlank() ) {
        runCatching {
            supabaseClient.postgrest["user_profiles"]
                .update(mapOf("email" to email, "updated_at" to now)) { filter { eq("id", userId) } }
        }
    }
}

/** Profildeki dil tercihini uygulama diline uygular (giris/kayit sonrasi). */
private suspend fun applySavedLanguage(userId: String) {
    runCatching {
        val result = supabaseClient.postgrest["user_profiles"]
            .select(columns = Columns.list("language")) { filter { eq("id", userId) } }
            .decodeList<Map<String, String>>()
        val lang = result.firstOrNull()?.get("language")
        if (!lang.isNullOrBlank()) {
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(lang)
            )
        }
    }
}

/**
 * Supabase'in ham hata metni ("Invalid login credentials" + URL + header dokumu)
 * kullaniciya dogrudan gosteriliyordu. Bilinen durumlari uygulama diline cevirir.
 */
private fun friendlyAuthError(context: Context, e: Throwable, isLogin: Boolean): String {
    val raw = (e.message ?: "").lowercase(Locale.US)
    return when {
        raw.contains("invalid login credentials") || raw.contains("invalid_credentials") ->
            context.getString(R.string.auth_error_invalid_credentials)
        raw.contains("email not confirmed") || raw.contains("email_not_confirmed") ->
            context.getString(R.string.auth_error_email_not_confirmed)
        raw.contains("already registered") || raw.contains("user_already_exists") ->
            context.getString(R.string.auth_error_email_in_use)
        raw.contains("password should be at least") || raw.contains("weak password") || raw.contains("weak_password") ->
            context.getString(R.string.auth_error_password_too_short)
        raw.contains("anonymous sign-ins are disabled") || raw.contains("anonymous_provider_disabled") ->
            context.getString(R.string.auth_error_guest_disabled)
        raw.contains("rate limit") || raw.contains("too many requests") || raw.contains("over_request_rate_limit") ->
            context.getString(R.string.auth_error_rate_limited)
        raw.contains("unable to resolve host") || raw.contains("timeout") || raw.contains("failed to connect") || raw.contains("network") ->
            context.getString(R.string.auth_error_network)
        else -> context.getString(if (isLogin) R.string.auth_error_login_failed else R.string.auth_error_register_failed)
    }
}
