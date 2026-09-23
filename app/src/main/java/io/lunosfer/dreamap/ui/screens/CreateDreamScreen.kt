package io.lunosfer.dreamap.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.AnalyzeDreamRequest
import io.lunosfer.dreamap.data.model.DreamInsertPayload
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.data.repository.ProfileRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.util.GlobalContentPicker
import io.lunosfer.dreamap.util.VisibilityPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.core.content.ContextCompat
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import io.lunosfer.dreamap.util.AppLanguage
import io.lunosfer.dreamap.data.model.PixabayHit
import io.lunosfer.dreamap.data.model.PixabayImageRequest
import androidx.appcompat.app.AppCompatDelegate

// Bug #16 dÃ¼zeltmesi: sesle yazma her zaman Ä°ngilizce Ã§alÄ±ÅŸÄ±yordu Ã§Ã¼nkÃ¼
// Locale.getDefault() burada CÄ°HAZIN sistem dilini dÃ¶ndÃ¼rÃ¼yordu, kullanÄ±cÄ±nÄ±n
// Profil > Dil ayarÄ±ndan (AppCompatDelegate.setApplicationLocales) SEÃ‡TÄ°ÄÄ°
// uygulama iÃ§i dili deÄŸil. Ã–nce uygulamanÄ±n kendi seÃ§tiÄŸi dili kontrol
// ediyoruz; ayrÄ±ca RecognizerIntent bÃ¶lge kodu olmayan ("tr" gibi) etiketleri
// bazÄ± cihazlarda tanÄ±madÄ±ÄŸÄ± iÃ§in yaygÄ±n diller iÃ§in bÃ¶lge kodu ekliyoruz.
private val SPEECH_LOCALE_REGION_FALLBACK = mapOf(
    "tr" to "tr-TR", "en" to "en-US", "es" to "es-ES", "fr" to "fr-FR",
    "de" to "de-DE", "pt" to "pt-PT", "ru" to "ru-RU", "ja" to "ja-JP",
    "ar" to "ar-SA", "hi" to "hi-IN", "zh" to "zh-CN",
    "fi" to "fi-FI", "ro" to "ro-RO", "uk" to "uk-UA"
)

private fun resolveSpeechRecognitionLocaleTag(): String {
    val appLocales = AppCompatDelegate.getApplicationLocales()
    val locale = if (!appLocales.isEmpty) appLocales[0] else Locale.getDefault()
    val tag = locale?.toLanguageTag() ?: return "en-US"
    if (tag.contains("-")) return tag
    return SPEECH_LOCALE_REGION_FALLBACK[tag] ?: tag
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CreateDreamScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val microphonePermissionRequiredMsg = stringResource(R.string.error_microphone_permission_required)
    val speechRecognitionUnavailableMsg = stringResource(R.string.error_speech_recognition_unavailable)
    val errorSelectingImageMsg = stringResource(R.string.error_selecting_image)
    val imageUploadFailedTemplate = stringResource(R.string.error_image_upload_failed)
    val commonErrorUnknownMsg = stringResource(R.string.common_error_unknown)
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp > 600

    var content by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("public") }
    // KullanÄ±cÄ±nÄ±n profil gizliliÄŸi â€” paylaÅŸÄ±m gizliliÄŸi seÃ§enekleri buna gÃ¶re
    // kÄ±sÄ±tlanÄ±r (bkz. util/VisibilityPolicy.kt). YÃ¼klenene kadar en kÄ±sÄ±tlayÄ±cÄ±
    // varsayÄ±mla (private) baÅŸlÄ±yoruz.
    var profileVisibility by remember { mutableStateOf<String?>("private") }
    val profileRepository = remember { ProfileRepository() }
    var inFeed by remember { mutableStateOf(true) }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(listOf<String>()) }
    var selectedEmotions by remember { mutableStateOf(setOf<String>()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isListening by remember { mutableStateOf(false) }
    var showPixabayDialog by remember { mutableStateOf(false) }
    var aiImageUrl by remember { mutableStateOf<String?>(null) }
    var imageSource by remember { mutableStateOf<String?>(null) }
    var imageWidth by remember { mutableStateOf<Int?>(null) }
    var imageHeight by remember { mutableStateOf<Int?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    val dreamRepository = remember { io.lunosfer.dreamap.data.repository.DreamRepository() }

    fun uploadPickedDreamImage(uri: android.net.Uri) {
        coroutineScope.launch {
            try {
                isUploadingImage = true
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    val fileName = "dream_user_${System.currentTimeMillis()}.jpg"
                    val uploadResult = dreamRepository.uploadDreamImage(bytes, fileName)
                    uploadResult.onSuccess { uploadedUrl ->
                        aiImageUrl = uploadedUrl
                        imageSource = "upload"
                        imageWidth = null
                        imageHeight = null
                    }.onFailure { err ->
                        Toast.makeText(context, imageUploadFailedTemplate.format(err.message ?: ""), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, io.lunosfer.dreamap.util.safeMessage(e) ?: commonErrorUnknownMsg, Toast.LENGTH_SHORT).show()
            } finally {
                isUploadingImage = false
            }
        }
    }

    // DiaryComposerScreen/ThreadScreen'de kanÄ±tlanan aynÄ± sorun: composable'a
    // baÄŸlÄ± rememberLauncherForActivityResult(PickVisualMedia()) galeriden
    // dÃ¶nÃ¼ÅŸte hiÃ§ tetiklenmeyebiliyor. Activity'ye baÄŸlÄ± GlobalContentPicker
    // singleton'Ä± kullanÄ±p sonucu her (yeniden) compose giriÅŸinde kontrol
    // ediyoruz â€” bkz. ThreadScreen.kt / util/GlobalContentPicker.kt.
    LaunchedEffect(Unit) {
        GlobalContentPicker.consumePendingUri()?.let { uri -> uploadPickedDreamImage(uri) }
    }

    LaunchedEffect(Unit) {
        val uid = supabaseClient.auth.currentUserOrNull()?.id ?: return@LaunchedEffect
        profileRepository.getUserProfile(uid).onSuccess { profile ->
            profileVisibility = profile.profileVisibility
        }
    }
    val allowedVisibilityOptions = remember(profileVisibility) { VisibilityPolicy.allowedOptions(profileVisibility) }
    // KullanÄ±cÄ± gizlilik seÃ§imini elle deÄŸiÅŸtirene kadar, profil yÃ¼klendikÃ§e
    // en aÃ§Ä±k (varsayÄ±lan) seÃ§eneÄŸe otomatik senkronlanÄ±r â€” aksi halde profil
    // henÃ¼z yÃ¼klenmeden atanan geÃ§ici "private" varsayÄ±mÄ± kalÄ±cÄ± olarak takÄ±lÄ± kalÄ±r.
    var visibilityUserSet by remember { mutableStateOf(false) }
    LaunchedEffect(allowedVisibilityOptions) {
        if (!visibilityUserSet) {
            visibility = VisibilityPolicy.defaultFor(profileVisibility)
        } else if (visibility !in allowedVisibilityOptions) {
            visibility = allowedVisibilityOptions.first()
        }
    }
    
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechErrorNoMatchMsg = stringResource(R.string.error_speech_no_match)
    val speechErrorTimeoutMsg = stringResource(R.string.error_speech_timeout)
    val speechErrorNetworkMsg = stringResource(R.string.error_speech_network)
    val speechErrorRecognizerBusyMsg = stringResource(R.string.error_speech_recognizer_busy)
    val speechErrorGenericMsg = stringResource(R.string.error_speech_recognition_failed)

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                // Ã–NCEDEN: hata burada sessizce yutuluyordu â€” mikrofon
                // "dinliyor" gÃ¶sterip hiÃ§bir sonuÃ§ gelmeden kapanÄ±yordu ve
                // kullanÄ±cÄ± iÃ§in "sesle yazma Ã§alÄ±ÅŸmÄ±yor" gibi gÃ¶rÃ¼nÃ¼yordu.
                // ArtÄ±k en azÄ±ndan NEDEN gÃ¶rÃ¼nÃ¼r oluyor (Ã¶r. eÅŸleÅŸme yok,
                // zaman aÅŸÄ±mÄ±, aÄŸ hatasÄ±) â€” sessiz sonlanma yerine.
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> speechErrorNoMatchMsg
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> speechErrorTimeoutMsg
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> speechErrorNetworkMsg
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> microphonePermissionRequiredMsg
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> speechErrorRecognizerBusyMsg
                    else -> speechErrorGenericMsg
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    val newContent = if (content.isEmpty()) recognizedText else "$content $recognizedText"
                    content = newContent.take(12000)
                }
                isListening = false
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
    
    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose {
            speechRecognizer.destroy()
        }
    }
    
    // BazÄ± cihazlarda (Google uygulamasÄ± devre dÄ±ÅŸÄ±, GMS'siz cihazlar, bazÄ±
    // Ã¶zel ROM'lar) SpeechRecognizer.isRecognitionAvailable() false dÃ¶nÃ¼yor
    // Ã§Ã¼nkÃ¼ arka planda baÄŸlanabilecek bir RecognitionService yok â€” ama yine
    // de ACTION_RECOGNIZE_SPEECH'i AKTÄ°VÄ°TE olarak aÃ§abilen bir uygulama
    // (Ã¼retici sesli arama vb.) kurulu olabiliyor. Ã–nceden bu durumda
    // kullanÄ±cÄ±ya doÄŸrudan "kullanÄ±lamÄ±yor" gÃ¶sterilip hiÃ§bir ÅŸey
    // denenmiyordu. Bu launcher, servis yolu baÅŸarÄ±sÄ±z olduÄŸunda aynÄ± intent'i
    // aktivite olarak baÅŸlatan bir yedek yol saÄŸlÄ±yor.
    val speechActivityFallbackLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (!matches.isNullOrEmpty()) {
            val recognizedText = matches[0]
            val newContent = if (content.isEmpty()) recognizedText else "$content $recognizedText"
            content = newContent.take(12000)
        }
    }

    fun buildSpeechIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, resolveSpeechRecognitionLocaleTag())
    }

    fun startSpeechRecognition() {
        val intent = buildSpeechIntent()
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer.startListening(intent)
            isListening = true
        } else if (intent.resolveActivity(context.packageManager) != null) {
            isListening = true
            speechActivityFallbackLauncher.launch(intent)
        } else {
            Toast.makeText(context, speechRecognitionUnavailableMsg, Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechRecognition()
        } else {
            Toast.makeText(context, microphonePermissionRequiredMsg, Toast.LENGTH_SHORT).show()
        }
    }

    var pendingLocationFill by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            pendingLocationFill = true
        }
    }
    
    fun toggleListening() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }


    val charLimit = 12000
    val maxTags = 10

    val unknownLocationStr = stringResource(R.string.dream_unknownLocation)

    val emotions = listOf(
        stringResource(R.string.dream_emotion_joy),
        stringResource(R.string.dream_emotion_peace),
        stringResource(R.string.dream_emotion_love),
        stringResource(R.string.dream_emotion_hope),
        stringResource(R.string.dream_emotion_awe),
        stringResource(R.string.dream_emotion_surprise),
        stringResource(R.string.dream_emotion_curiosity),
        stringResource(R.string.dream_emotion_confusion),
        stringResource(R.string.dream_emotion_fear),
        stringResource(R.string.dream_emotion_anxiety),
        stringResource(R.string.dream_emotion_sadness),
        stringResource(R.string.dream_emotion_loneliness),
        stringResource(R.string.dream_emotion_anger),
        stringResource(R.string.dream_emotion_shame),
        stringResource(R.string.dream_emotion_disgust),
        stringResource(R.string.dream_emotion_relief)
    )

    // ONCEDEN: ekran acilir acilmaz KONUM IZNI isteniyordu ve izin verilmezse
    // kullaniciya sorulmadan ipinfo.io'ya istek atilip IP'den sehir tahmin
    // ediliyordu. Kullanici "ruya yaz" dedigi anda gerekcesiz bir konum
    // diyalogu goruyordu (canli goruldu) ve IP'si ucuncu bir servise gidiyordu.
    // Artik konum YALNIZCA kullanici alandaki konum butonuna basinca aliniyor.
    fun fillLocationFromDevice() {
        coroutineScope.launch(Dispatchers.IO) {
            val sysLoc = getSystemLocationName(context)
            if (sysLoc.isNotBlank()) {
                withContext(Dispatchers.Main) { location = sysLoc }
                return@launch
            }
            try {
                val url = URL("https://ipinfo.io/json")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val city = json.optString("city")
                val countryCode = json.optString("country")
                val country = if (countryCode.isNotBlank()) Locale("", countryCode).displayCountry else ""
                val loc = listOf(city, country).filter { it.isNotBlank() }.joinToString(", ")
                withContext(Dispatchers.Main) {
                    if (loc.isNotBlank()) location = loc
                }
            } catch (e: Exception) {
                android.util.Log.e("CreateDreamScreen", "Failed to fetch location", e)
            }
        }
    }

    fun requestLocation() {
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasCoarse) {
            fillLocationFromDevice()
        } else {
            // Yalnizca COARSE isteniyor: manifestte artik FINE yok, bildirilmemis
            // bir izni istemek sistemde sessizce reddedilir.
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    
    if (showPixabayDialog) {
        Dialog(
            onDismissRequest = { showPixabayDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Void950) {
                var query by remember { mutableStateOf("") }
                var results by remember { mutableStateOf<List<PixabayHit>>(emptyList()) }
                var isSearching by remember { mutableStateOf(false) }

                LaunchedEffect(query) {
                    delay(500)
                    isSearching = true
                    try {
                        val response = NetworkModule.api.searchPixabay(query)
                        results = response.hits
                    } catch (e: Exception) {
                        // Handle
                    }
                    isSearching = false
                }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showPixabayDialog = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back_cd), tint = Color.White)
                        }
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.create_dream_pixabay_search)) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AstralGold,
                                unfocusedBorderColor = Void800,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isSearching) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AstralGold)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(results) { hit ->
                                AsyncImage(
                                    model = hit.webformatURL,
                                    contentDescription = hit.tags.joinToString(", "),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            coroutineScope.launch {
                                                try {
                                                    val rawUrl = hit.webformatURL
                                                    val permanentUrl = dreamRepository.persistImageToStorage(rawUrl, "dream_pixabay").getOrDefault(rawUrl)
                                                    aiImageUrl = permanentUrl
                                                    imageSource = "pixabay"
                                                    imageWidth = hit.width
                                                    imageHeight = hit.height
                                                    showPixabayDialog = false
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, errorSelectingImageMsg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back_cd), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        },
        containerColor = Void950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Void800)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.create_dream_journal_watermark),
                    color = AstralGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.dream_addTitle),
                color = Color.White,
                fontSize = 32.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            
            val pulseAlpha by animateFloatAsState(
                targetValue = if (isListening) 0.5f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ), label = ""
            )

            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= charLimit) content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                placeholder = { Text(stringResource(R.string.dream_placeholder), color = Color.Gray) },
                trailingIcon = {
                    IconButton(onClick = { toggleListening() }) {
                        Icon(
                            imageVector = if (isListening) Icons.Filled.Mic else Icons.Outlined.MicNone,
                            contentDescription = stringResource(R.string.cd_dictate),
                            tint = if (isListening) Color.Red.copy(alpha = pulseAlpha) else Color.White
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AstralGold,
                    unfocusedBorderColor = Void800,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )


            
            if (aiImageUrl != null) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))) {
                    AsyncImage(
                        model = aiImageUrl,
                        contentDescription = stringResource(R.string.cd_cover_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    if (isUploadingImage) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Void950.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AstralGold, modifier = Modifier.size(32.dp))
                        }
                    }
                    IconButton(
                        onClick = { aiImageUrl = null; imageSource = null; imageWidth = null; imageHeight = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_remove_cover), tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showPixabayDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Void800),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = AstralGold)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.create_dream_pixabay_btn), color = AstralGold)
                    }
                    Button(
                        onClick = { GlobalContentPicker.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Void800),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = AstralGold)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.editor_gallery_button), color = AstralGold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
val charCount = content.length
            val isNearLimit = charCount > charLimit * 0.9
            Text(
                text = "$charCount / $charLimit",
                color = if (isNearLimit) ShadowWorkRose else Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isWideScreen) 8 else 4),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(emotions) { emotion ->
                    val isSelected = selectedEmotions.contains(emotion)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AstralGold.copy(alpha = 0.1f) else Void800)
                            .clickable {
                                selectedEmotions = if (isSelected) {
                                    selectedEmotions - emotion
                                } else {
                                    selectedEmotions + emotion
                                }
                            }
                            .then(
                                if (isSelected) Modifier.shadow(8.dp, spotColor = AstralGold, ambientColor = AstralGold)
                                else Modifier
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emotion,
                            color = if (isSelected) AstralGold else Color.White,
                            fontSize = 12.sp,
                            maxLines = 1,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = tagInput,
                onValueChange = { 
                    val newValue = it
                    if (newValue.contains(",")) {
                        val newTag = newValue.replace(",", "").trim()
                        if (newTag.isNotEmpty() && tags.size < maxTags && !tags.contains(newTag)) {
                            tags = tags + newTag
                        }
                        tagInput = ""
                    } else {
                        tagInput = newValue
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.create_dream_tags_placeholder), color = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val newTag = tagInput.trim()
                        if (newTag.isNotEmpty() && tags.size < maxTags && !tags.contains(newTag)) {
                            tags = tags + newTag
                        }
                        tagInput = ""
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AstralGold,
                    unfocusedBorderColor = Void800,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                // Duz Row idi: 3-4 etiketten sonrasi ekrandan tasip goruntulenmiyor
                // ve silinemiyordu (10 etikete kadar izin var).
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        AssistChip(
                            onClick = { tags = tags - tag },
                            label = { Text(tag) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove_tag), modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Void800, labelColor = Color.White, trailingIconContentColor = Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LaunchedEffect(pendingLocationFill) {
                if (pendingLocationFill) {
                    pendingLocationFill = false
                    fillLocationFromDevice()
                }
            }

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.dream_location)) },
                trailingIcon = {
                    IconButton(onClick = { requestLocation() }) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = stringResource(R.string.cd_use_my_location),
                            tint = AstralGold
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AstralGold,
                    unfocusedBorderColor = Void800,
                    focusedLabelColor = AstralGold,
                    unfocusedLabelColor = Color.LightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            var visibilityExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = visibilityExpanded,
                onExpandedChange = { visibilityExpanded = !visibilityExpanded }
            ) {
                OutlinedTextField(
                    value = when (visibility) {
                        "public" -> stringResource(R.string.dream_public)
                        "friends" -> stringResource(R.string.dream_friends)
                        "private" -> stringResource(R.string.dream_private)
                        else -> stringResource(R.string.dream_public)
                    },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = visibilityExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                ExposedDropdownMenu(
                    expanded = visibilityExpanded,
                    onDismissRequest = { visibilityExpanded = false },
                    modifier = Modifier.background(Void800)
                ) {
                    if ("public" in allowedVisibilityOptions) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dream_public), color = Color.White) },
                            onClick = { visibility = "public"; visibilityUserSet = true; visibilityExpanded = false }
                        )
                    }
                    if ("friends" in allowedVisibilityOptions) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dream_friends), color = Color.White) },
                            onClick = { visibility = "friends"; visibilityUserSet = true; visibilityExpanded = false }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dream_private), color = Color.White) },
                        onClick = { visibility = "private"; visibilityUserSet = true; visibilityExpanded = false }
                    )
                }
            }
            val dreamVisibilityNote = when {
                profileVisibility == "private" -> stringResource(R.string.visibility_locked_private_note)
                profileVisibility == "friends" -> stringResource(R.string.visibility_restricted_to_friends_note)
                else -> null
            }
            if (dreamVisibilityNote != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(dreamVisibilityNote, color = Color.Gray, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = inFeed,
                    onCheckedChange = { inFeed = it },
                    colors = CheckboxDefaults.colors(checkedColor = AstralGold, checkmarkColor = Void950, uncheckedColor = Color.Gray)
                )
                Text(stringResource(R.string.dream_shareInFeed), color = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (errorMessage != null) {
                Text(errorMessage!!, color = ShadowWorkRose, modifier = Modifier.padding(bottom = 16.dp))
            }

            // Ruyanin dili ve AI analizinin dili: cihaz dili degil, kullanicinin
            // uygulamada sectigi dil (bkz. util/AppLanguage).
            val currentLang = io.lunosfer.dreamap.util.AppLanguage.code()

            Button(
                onClick = {
                    if (content.isBlank()) {
                        errorMessage = context.getString(R.string.dream_validationContent)
                        return@Button
                    }
                    if (content.length > 12000) {
                        errorMessage = context.getString(R.string.create_dream_error_content_too_long)
                        return@Button
                    }
                    errorMessage = null
                    isSubmitting = true
                    
                    coroutineScope.launch {
                        try {
                            val user = supabaseClient.auth.currentUserOrNull()
                            if (user == null) {
                                errorMessage = context.getString(R.string.error_not_logged_in)
                                isSubmitting = false
                                return@launch
                            }

                            val dreamDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            val sentiments = selectedEmotions.joinToString(", ")
                            val finalLocation = location.trim().ifEmpty { unknownLocationStr }
                            val finalImageUrl = if (!aiImageUrl.isNullOrBlank()) {
                                dreamRepository.persistImageToStorage(aiImageUrl!!, "dream_image").getOrDefault(aiImageUrl!!)
                            } else null
                            
                            val insertData = DreamInsertPayload(
                                userId = user.id,
                                content = content.trim(),
                                locationName = finalLocation,
                                inFeed = inFeed,
                                visibility = VisibilityPolicy.clamp(visibility, profileVisibility),
                                userSelectedSentiment = sentiments,
                                dreamDate = dreamDate,
                                originalLanguage = currentLang,
                                tags = tags,
                                aiImageUrl = finalImageUrl,
                                imageSource = imageSource,
                                imageWidth = imageWidth,
                                imageHeight = imageHeight
                            )

                            // Type mismatch fix: ensure we deserialize as a Map returning Any values or define a specific class
                            val result = supabaseClient.postgrest["dreams"].insert(insertData) {
                                select()
                            }.decodeList<io.lunosfer.dreamap.data.model.DreamInsertResponse>()

                            val insertedId = result.firstOrNull()?.id
                            
                            if (insertedId != null) {
                                // Fire and forget analyze
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        NetworkModule.api.analyzeDream(
                                            AnalyzeDreamRequest(insertedId, content.trim(), currentLang)
                                        )
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            } else {
                                errorMessage = context.getString(R.string.dream_createFailed)
                                isSubmitting = false
                            }

                        } catch (e: Exception) {
                            errorMessage = context.getString(R.string.dream_createFailed) + (io.lunosfer.dreamap.util.safeMessage(e)?.let { ": $it" } ?: "")
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AstralGold, disabledContainerColor = Void800),
                enabled = !isSubmitting && content.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Void950, modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.dream_submit), color = Void950, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

private fun getSystemLocationName(context: Context): String {
    try {
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasCoarse) return ""

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return ""
        val providers = locationManager.getProviders(true)
        var bestLocation: android.location.Location? = null
        for (provider in providers) {
            // Her saglayici AYRI AYRI korunmali: yalnizca COARSE iznimiz
            // oldugu icin GPS_PROVIDER SecurityException firlatiyor; tek bir
            // dis try/catch olsaydi ilk saglayicida cikip NETWORK_PROVIDER'i
            // hic denemeden bos donerdik.
            val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                bestLocation = loc
            }
        }
        if (bestLocation != null) {
            // Cihaz dili degil, kullanicinin uygulama icinde sectigi dil:
            // sehir/ulke adi da arayuzun geri kalaniyla ayni dilde olmali.
            val geocoder = Geocoder(context, AppLanguage.locale())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(bestLocation.latitude, bestLocation.longitude, 1)
            val addr = addresses?.firstOrNull()
            if (addr != null) {
                val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                val country = addr.countryName ?: ""
                return listOf(city, country).filter { it.isNotBlank() }.joinToString(", ")
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("CreateDreamScreen", "System location error", e)
    }
    return ""
}
