package com.yusufteker.healthtrack.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.GradientText
import com.yusufteker.healthtrack.ui.components.HealthBrandLogo
import com.yusufteker.healthtrack.ui.components.LanguageSelectorPill
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import com.yusufteker.healthtrack.ui.components.keepVisibleAboveIme
import com.yusufteker.healthtrack.ui.components.rememberKeyboardDismisser

@Composable
fun AuthScreen(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    var identifierInput by remember { mutableStateOf(state.identifier) }
    var passwordInput by remember { mutableStateOf(state.password) }
    var registerNameInput by remember { mutableStateOf(state.registerName) }
    var registerUsernameInput by remember { mutableStateOf(state.registerUsername) }
    var registerEmailInput by remember { mutableStateOf(state.registerEmail) }
    var registerPasswordInput by remember { mutableStateOf(state.registerPassword) }
    var registerCodeInput by remember { mutableStateOf(state.registerCode) }

    LaunchedEffect(state.identifier) {
        if (identifierInput != state.identifier) identifierInput = state.identifier
    }
    LaunchedEffect(state.password) {
        if (passwordInput != state.password) passwordInput = state.password
    }
    LaunchedEffect(state.registerName) {
        if (registerNameInput != state.registerName) registerNameInput = state.registerName
    }
    LaunchedEffect(state.registerUsername) {
        if (registerUsernameInput != state.registerUsername) registerUsernameInput = state.registerUsername
    }
    LaunchedEffect(state.registerEmail) {
        if (registerEmailInput != state.registerEmail) registerEmailInput = state.registerEmail
    }
    LaunchedEffect(state.registerPassword) {
        if (registerPasswordInput != state.registerPassword) registerPasswordInput = state.registerPassword
    }
    LaunchedEffect(state.registerCode) {
        if (registerCodeInput != state.registerCode) registerCodeInput = state.registerCode
    }

    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    val dismissKeyboard = rememberKeyboardDismisser()
    val otpFocusRequester = remember { FocusRequester() }

    val canLogin = !state.isLoading && identifierInput.isNotBlank() && passwordInput.isNotBlank()
    val canSendCode = !state.isLoading &&
        registerNameInput.isNotBlank() &&
        registerUsernameInput.isNotBlank() &&
        registerEmailInput.isNotBlank() &&
        registerPasswordInput.isNotBlank()
    val canRegister = !state.isLoading && registerCodeInput.length == 6

    val submitLogin = {
        if (canLogin) {
            dismissKeyboard()
            onIntent(AuthIntent.OnLoginClicked)
        }
    }
    val submitSendCode = {
        if (canSendCode) {
            dismissKeyboard()
            onIntent(AuthIntent.OnSendRegisterCodeClicked)
        }
    }
    val submitRegister = {
        if (canRegister) {
            dismissKeyboard()
            onIntent(AuthIntent.OnRegisterClicked)
        }
    }

    // Kod gönderildiğinde doğrulama alanına otomatik odaklan (klavye direkt açılsın)
    LaunchedEffect(state.isRegisterMode, state.isRegisterCodeSent) {
        if (state.isRegisterMode && state.isRegisterCodeSent && registerCodeInput.length < 6) {
            runCatching { otpFocusRequester.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clearFocusOnTap()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(32.dp),
                elevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        LanguageSelectorPill(
                            selectedLanguage = state.language,
                            onLanguageSelected = { onIntent(AuthIntent.OnLanguageChanged(it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // HealthTrack Luxury Brand Mark
                    HealthBrandLogo(size = 64.dp)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Brand Title
                    GradientText(
                        text = "HealthTrack",
                        colors = HealthColors.GradientPrimary,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "by Planora",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mode Switcher: Giriş Yap / Üye Ol
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val loginSelected = !state.isRegisterMode
                            Surface(
                                onClick = { onIntent(AuthIntent.OnToggleAuthMode(false)) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (loginSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = str("Giriş Yap", "Sign In"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (loginSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                            Surface(
                                onClick = { onIntent(AuthIntent.OnToggleAuthMode(true)) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (!loginSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = str("Üye Ol", "Sign Up"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!loginSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (state.isRegisterMode) {
                            if (state.isRegisterCodeSent) {
                                str("E-postanıza gelen 6 haneli kodu girin", "Enter the 6-digit code sent to your email")
                            } else {
                                str("Yeni bir Planora & HealthTrack hesabı oluşturun", "Create a new Planora & HealthTrack account")
                            }
                        } else {
                            str("Planora hesabınız ile giriş yapın", "Sign in with your Planora account")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error Banner
                    if (state.errorMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = state.errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    // Info Banner
                    if (state.infoMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = state.infoMessage,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    if (!state.isRegisterMode) {
                        // ── LOGIN FORM ──
                        Column(modifier = Modifier.fillMaxWidth().keepVisibleAboveIme()) {
                        OutlinedTextField(
                            value = identifierInput,
                            onValueChange = {
                                identifierInput = it
                                onIntent(AuthIntent.OnIdentifierChanged(it))
                            },
                            label = { Text(str("Kullanıcı Adı veya E-posta", "Username or Email")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                onIntent(AuthIntent.OnPasswordChanged(it))
                            },
                            label = { Text(str("Şifre", "Password")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isPasswordVisible) str("Şifreyi gizle", "Hide password") else str("Şifreyi göster", "Show password"),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                if (canLogin) submitLogin() else dismissKeyboard()
                            }),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = submitLogin,
                            enabled = canLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = str("Giriş Yap", "Sign In"),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        }
                    } else if (!state.isRegisterCodeSent) {
                        // ── REGISTER STEP 1: Account Info ──
                        OutlinedTextField(
                            value = registerNameInput,
                            onValueChange = {
                                registerNameInput = it
                                onIntent(AuthIntent.OnRegisterNameChanged(it))
                            },
                            label = { Text(str("Ad Soyad", "Full Name")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = registerUsernameInput,
                            onValueChange = {
                                registerUsernameInput = it
                                onIntent(AuthIntent.OnRegisterUsernameChanged(it))
                            },
                            label = { Text(str("Kullanıcı Adı", "Username")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.AlternateEmail,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = registerEmailInput,
                            onValueChange = {
                                registerEmailInput = it
                                onIntent(AuthIntent.OnRegisterEmailChanged(it))
                            },
                            label = { Text(str("E-posta", "Email")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(modifier = Modifier.fillMaxWidth().keepVisibleAboveIme()) {
                        OutlinedTextField(
                            value = registerPasswordInput,
                            onValueChange = {
                                registerPasswordInput = it
                                onIntent(AuthIntent.OnRegisterPasswordChanged(it))
                            },
                            label = { Text(str("Şifre (en az 6 karakter)", "Password (min 6 chars)")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                if (canSendCode) submitSendCode() else dismissKeyboard()
                            }),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = submitSendCode,
                            enabled = canSendCode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = str("Doğrulama Kodu Gönder", "Send Verification Code"),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        }
                    } else {
                        // ── REGISTER STEP 2: OTP Verification Code ──
                        Column(modifier = Modifier.fillMaxWidth().keepVisibleAboveIme()) {
                        OutlinedTextField(
                            value = registerCodeInput,
                            onValueChange = {
                                val sanitized = it.filter { ch -> ch.isDigit() }.take(6)
                                val justCompleted = sanitized.length == 6 && registerCodeInput.length < 6
                                registerCodeInput = sanitized
                                onIntent(AuthIntent.OnRegisterCodeChanged(sanitized))
                                // 6. hane girildiğinde (veya kod yapıştırıldığında) klavyeyi kapat ve otomatik doğrula
                                if (justCompleted && !state.isLoading) {
                                    dismissKeyboard()
                                    onIntent(AuthIntent.OnRegisterClicked)
                                }
                            },
                            label = { Text(str("6 Haneli Doğrulama Kodu", "6-Digit Verification Code")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Password,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                if (canRegister) submitRegister() else dismissKeyboard()
                            }),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(otpFocusRequester),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = submitRegister,
                            enabled = canRegister,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = str("Doğrula ve Üye Ol", "Verify & Sign Up"),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { onIntent(AuthIntent.OnEditRegisterInfoClicked) },
                                enabled = !state.isLoading
                            ) {
                                Text(str("Bilgileri Düzenle", "Edit Info"))
                            }
                            TextButton(
                                onClick = { onIntent(AuthIntent.OnSendRegisterCodeClicked) },
                                enabled = !state.isLoading
                            ) {
                                Text(str("Kodu Tekrar Gönder", "Resend Code"))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { uriHandler.openUri("https://pulse-7b4z.onrender.com/privacy") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = str("Gizlilik Politikası", "Privacy Policy"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { uriHandler.openUri("https://pulse-7b4z.onrender.com/terms") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = str("Kullanım Koşulları", "Terms of Service"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = str(
                            "Bu uygulama tıbbi tavsiye veya teşhis amacı taşımaz.",
                            "This app is not intended for medical advice or diagnosis."
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
