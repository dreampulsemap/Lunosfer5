package io.lunosfer.dreamap.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val username: String,
    val updated_at: String,
    val created_at: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
    val sessionStatus by supabaseClient.auth.sessionStatus.collectAsState(initial = io.github.jan.supabase.auth.status.SessionStatus.Initializing)
    LaunchedEffect(sessionStatus) {
        if (sessionStatus is io.github.jan.supabase.auth.status.SessionStatus.Authenticated) {
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
                        if (email.isBlank() || password.isBlank()) return@Button
                        if (!isLogin && username.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.auth_username_required), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                if (isLogin) {
                                    supabaseClient.auth.signInWith(Email) {
                                        this.email = email@email
                                        this.password = password@password
                                    }
                                } else {
                                    supabaseClient.auth.signUpWith(Email) {
                                        this.email = email@email
                                        this.password = password@password
                                    }
                                }
                                
                                val user = supabaseClient.auth.currentUserOrNull()
                                if (user != null) {
                                    val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                                    }.format(java.util.Date())
                                    val profileData = mutableMapOf(
                                        "id" to user.id,
                                        "email" to (user.email ?: email),
                                        "username" to username.ifEmpty { user.email?.substringBefore("@") ?: "user" },
                                        "updated_at" to now
                                    )
                                    if (!isLogin) {
                                        profileData["created_at"] = now
                                    }
                                    
                                    supabaseClient.postgrest["user_profiles"].upsert(profileData) {
                                        onConflict = "id"
                                    }
                                    
                                    if (isLogin) {
                                        try {
                                            val result = supabaseClient.postgrest["user_profiles"]
                                                .select(columns = Columns.list("language")) {
                                                    filter { eq("id", user.id) }
                                                }.decodeList<Map<String, String>>()
                                                
                                            val lang = result.firstOrNull()?.get("language")
                                            if (lang != null && lang.isNotEmpty()) {
                                                val localeList = androidx.core.os.LocaleListCompat.forLanguageTags(lang)
                                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
                                            }
                                        } catch (e: Exception) {
                                            // Ignore
                                        }
                                    }
                                }
                                
                                if (!isLogin) {
                                    Toast.makeText(context, context.getString(R.string.auth_success), Toast.LENGTH_LONG).show()
                                    isLogin = true
                                } else {
                                    LunosferMessagingService.registerCurrentFcmToken()
                                    onLoginSuccess()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
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

                Spacer(Modifier.height(16.dp))
                
                Text(
                                    text = stringResource(R.string.auth_or_separator),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val url = supabaseClient.auth.getOAuthUrl(provider = Google, redirectUrl = "io.lunosfer.dreamap://auth-callback")
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
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
