package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yusufteker.healthtrack.core.model.FollowRequestItemDto
import com.yusufteker.healthtrack.core.model.FriendUserDto
import com.yusufteker.healthtrack.core.utils.CapturedPhoto
import com.yusufteker.healthtrack.core.utils.rememberCameraPhotoLauncher
import com.yusufteker.healthtrack.theme.str
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Planora 1:1 Profile Hero Card with centered 96dp avatar, photo viewer/upload,
 * gradient name, @username, and 3-column stats row (Seri | Takipçi | Takip Edilen).
 */
@OptIn(ExperimentalEncodingApi::class)
@Composable
fun PlanoraProfileHeroCard(
    name: String,
    username: String,
    profileImageUrl: String?,
    streakDays: Int,
    followersCount: Int,
    followingCount: Int,
    isPremium: Boolean,
    isUploadingImage: Boolean,
    onUploadProfileImageBytes: (ByteArray) -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onStreakClick: (() -> Unit)? = null,
    onEditBodyClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cameraLauncher = rememberCameraPhotoLauncher()
    var showPhotoViewerDialog by remember { mutableStateOf(false) }
    var pendingCropPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }

    val handleCapturedPhoto: (CapturedPhoto) -> Unit = { captured ->
        showPhotoViewerDialog = false
        if (captured.imageBitmap != null) {
            pendingCropPhoto = captured
        } else if (!captured.base64.isNullOrBlank()) {
            runCatching { Base64.decode(captured.base64) }.onSuccess(onUploadProfileImageBytes)
        }
    }

    if (showPhotoViewerDialog) {
        ProfilePhotoViewerDialog(
            name = name,
            profileImageUrl = profileImageUrl,
            isCameraAvailable = cameraLauncher.isCameraAvailable,
            isUploading = isUploadingImage,
            onPickFromGallery = {
                cameraLauncher.launchGallery(handleCapturedPhoto)
            },
            onTakeWithCamera = {
                cameraLauncher.launchCamera(handleCapturedPhoto)
            },
            onDismiss = { showPhotoViewerDialog = false }
        )
    }

    pendingCropPhoto?.imageBitmap?.let { bitmap ->
        ImageCropDialog(
            photo = bitmap,
            isProcessing = cameraLauncher.isScanning || isUploadingImage,
            titleText = str("Profil Fotoğrafını Ayarla", "Adjust Profile Photo"),
            subtitleText = str("Kırpma alanını sürükleyip kareye oturtun", "Drag the frame to center your photo"),
            fullActionLabel = str("Tamamını Kullan", "Use Full"),
            confirmActionLabel = str("Kırp ve Kaydet", "Crop & Save"),
            initialRatioMode = CropRatioMode.SQUARE,
            availableRatioModes = listOf(CropRatioMode.SQUARE, CropRatioMode.FULL, CropRatioMode.FREE),
            onConfirmCrop = { rect, rotation ->
                cameraLauncher.cropAndScan(rect, rotation) { cropped ->
                    pendingCropPhoto = null
                    val b64 = cropped.base64
                    if (!b64.isNullOrBlank()) {
                        runCatching { Base64.decode(b64) }.onSuccess(onUploadProfileImageBytes)
                    }
                }
            },
            onScanFull = {
                val b64 = pendingCropPhoto?.base64
                pendingCropPhoto = null
                if (!b64.isNullOrBlank()) {
                    runCatching { Base64.decode(b64) }.onSuccess(onUploadProfileImageBytes)
                }
            },
            onDismiss = { pendingCropPhoto = null }
        )
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        borderGradient = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val avatarBorderBrush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                )

                Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .bounceClick()
                            .clickable { showPhotoViewerDialog = true }
                            .border(width = 3.dp, brush = avatarBorderBrush, shape = CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarImage(
                            profileImageUrl = profileImageUrl,
                            fallbackInitial = name.firstOrNull()?.uppercase()
                                ?: username.firstOrNull()?.uppercase() ?: "U",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            fallbackColor = MaterialTheme.colorScheme.primary,
                            fallbackBackgroundBrush = Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)
                                )
                            ),
                            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            imageSizePx = 300
                        )
                        if (isUploadingImage) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    // Camera edit badge at bottom-end
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .bounceClick()
                            .clickable { showPhotoViewerDialog = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = str("Fotoğrafı Değiştir", "Change Photo"),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                GradientText(
                    text = name.ifBlank { str("Kullanıcı", "User") },
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    ),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black
                    )
                )

                if (username.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "@$username",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3-Column Stats Row matching Planora ProfileScreen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .then(if (onStreakClick != null) Modifier.clickable(onClick = onStreakClick) else Modifier)
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (streakDays > 0) "🔥 $streakDays" else "0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = str("SERİ", "STREAK"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onFollowersClick)
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = followersCount.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = str("TAKİPÇİ", "FOLLOWERS"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onFollowingClick)
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = followingCount.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = str("TAKİP EDİLEN", "FOLLOWING"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            if (isPremium) {
                val premColor = Color(0xFFF59E0B)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = premColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, premColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = premColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "PRO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            ),
                            color = premColor
                        )
                    }
                }
            }

            if (onEditBodyClick != null) {
                Surface(
                    onClick = onEditBodyClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .bounceClick()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = str("Fiziksel Bilgileri Düzenle", "Edit Body Stats"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = str("Düzenle", "Edit"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePhotoViewerDialog(
    name: String,
    profileImageUrl: String?,
    isCameraAvailable: Boolean,
    isUploading: Boolean,
    onPickFromGallery: () -> Unit,
    onTakeWithCamera: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = str("Profil Fotoğrafı", "Profile Photo"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = str("Kapat", "Close"))
                    }
                }

                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(CircleShape)
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            shape = CircleShape
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarImage(
                        profileImageUrl = profileImageUrl,
                        fallbackInitial = name.firstOrNull()?.uppercase() ?: "U",
                        modifier = Modifier.fillMaxSize(),
                        textStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                        imageSizePx = 600
                    )
                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickFromGallery,
                        enabled = !isUploading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .bounceClick()
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = str("Galeri", "Gallery"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    if (isCameraAvailable) {
                        Button(
                            onClick = onTakeWithCamera,
                            enabled = !isUploading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .bounceClick()
                        ) {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = str("Kamera", "Camera"),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-width sleek Find Friends button matching Planora ProfileScreen.
 */
@Composable
fun PlanoraFindFriendsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .bounceClick(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.PersonAdd,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = str("Arkadaş Bul & Ekle", "Find & Add Friends"),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Planora 1:1 Incoming Follow Requests Card.
 */
@Composable
fun PlanoraIncomingRequestsCard(
    requests: List<FollowRequestItemDto>,
    onAccept: (Int) -> Unit,
    onReject: (Int) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (requests.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = str("Gelen İstekler", "Incoming Requests"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = requests.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = str("Takip İstekleri", "Follow Requests"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                requests.take(4).forEach { request ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarImage(
                            profileImageUrl = request.requesterProfileImageUrl,
                            fallbackInitial = request.requesterName.firstOrNull()?.uppercase() ?: "?",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = request.requesterName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (request.requesterUsername.isNotBlank()) {
                                Text(
                                    text = "@${request.requesterUsername}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { onAccept(request.id) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = str("Kabul Et", "Accept"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            OutlinedButton(
                                onClick = { onReject(request.id) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = str("Reddet", "Decline"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            if (requests.size > 4 && onSeeAllClick != null) {
                OutlinedButton(
                    onClick = onSeeAllClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(str("Tümünü Gör (${requests.size})", "See All (${requests.size})"))
                }
            }
        }
    }
}

/**
 * Planora 1:1 3-Tab Follow List Sheet (Takipçiler | Takip Edilenler | İstekler).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanoraFollowListSheet(
    initialTab: Int,
    followers: List<FriendUserDto>,
    following: List<FriendUserDto>,
    requests: List<FollowRequestItemDto>,
    isLoading: Boolean,
    pendingActionUserIds: Set<Int> = emptySet(),
    onRemoveFollower: (Int) -> Unit,
    onUnfollow: (Int) -> Unit,
    onAcceptRequest: (Int) -> Unit,
    onRejectRequest: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }

    val tabTitles = listOf(
        str("Takipçiler", "Followers"),
        str("Takip Edilenler", "Following"),
        str("İstekler", "Requests")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = str("Geri", "Back"))
                    }
                    GradientText(
                        text = str("Arkadaşlar & İstekler", "Friends & Requests"),
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(onClick = {
                    onDismiss()
                    onOpenSearch()
                }) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = str("Arkadaş Bul", "Find Friends"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val requestCount = if (index == 2) requests.size else 0
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            if (index == 2 && requestCount > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(title, maxLines = 1, softWrap = false)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text(requestCount.toString())
                                    }
                                }
                            } else {
                                Text(title, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    if (isLoading && followers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (followers.isEmpty()) {
                        SocialEmptyState(
                            icon = Icons.Default.Group,
                            title = str("Henüz takipçiniz bulunmuyor.", "No followers yet."),
                            actionLabel = str("Arkadaş Bul & Ekle", "Find & Add Friends"),
                            onAction = {
                                onDismiss()
                                onOpenSearch()
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(followers, key = { it.id }) { user ->
                                val isBusy = pendingActionUserIds.contains(user.id)
                                PlanoraFollowUserRow(
                                    profileImageUrl = user.profileImageUrl,
                                    name = user.name,
                                    username = user.username,
                                    actionButton = {
                                        OutlinedButton(
                                            onClick = { if (!isBusy) onRemoveFollower(user.id) },
                                            enabled = !isBusy,
                                            shape = RoundedCornerShape(20.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text(str("Çıkar", "Remove"), maxLines = 1, softWrap = false)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (isLoading && following.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (following.isEmpty()) {
                        SocialEmptyState(
                            icon = Icons.Default.PersonSearch,
                            title = str("Henüz kimseyi takip etmiyorsunuz.", "You are not following anyone yet."),
                            actionLabel = str("Arkadaş Bul & Ekle", "Find & Add Friends"),
                            onAction = {
                                onDismiss()
                                onOpenSearch()
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(following, key = { it.id }) { user ->
                                val isBusy = pendingActionUserIds.contains(user.id)
                                PlanoraFollowUserRow(
                                    profileImageUrl = user.profileImageUrl,
                                    name = user.name,
                                    username = user.username,
                                    actionButton = {
                                        Button(
                                            onClick = { if (!isBusy) onUnfollow(user.id) },
                                            enabled = !isBusy,
                                            shape = RoundedCornerShape(20.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        ) {
                                            Text(str("Takibi Bırak", "Unfollow"), maxLines = 1, softWrap = false)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                else -> {
                    if (isLoading && requests.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (requests.isEmpty()) {
                        SocialEmptyState(
                            icon = Icons.Default.PersonAdd,
                            title = str("Bekleyen takip isteği yok.", "No pending follow requests."),
                            actionLabel = str("Arkadaş Bul & Ekle", "Find & Add Friends"),
                            onAction = {
                                onDismiss()
                                onOpenSearch()
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(requests, key = { it.id }) { req ->
                                PlanoraFollowUserRow(
                                    profileImageUrl = req.requesterProfileImageUrl,
                                    name = req.requesterName,
                                    username = req.requesterUsername,
                                    actionButton = {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onAcceptRequest(req.id) },
                                                shape = RoundedCornerShape(20.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Text(str("Kabul Et", "Accept"), maxLines = 1, softWrap = false)
                                            }
                                            OutlinedButton(
                                                onClick = { onRejectRequest(req.id) },
                                                shape = RoundedCornerShape(20.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(str("Reddet", "Decline"), maxLines = 1, softWrap = false)
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
}

/**
 * Planora 1:1 SearchUsersScreen Sheet with pill-bar search input & Follow/Requested/Following buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanoraSearchUsersSheet(
    query: String,
    results: List<FriendUserDto>,
    isLoading: Boolean,
    pendingActionUserIds: Set<Int>,
    onQueryChanged: (String) -> Unit,
    onToggleFollow: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchInput by remember { mutableStateOf(query) }
    LaunchedEffect(query) {
        if (searchInput != query) searchInput = query
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .imePadding()
                .clearFocusOnTap()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = str("Geri", "Back"))
                }

                TextField(
                    value = searchInput,
                    onValueChange = {
                        searchInput = it
                        onQueryChanged(it)
                    },
                    placeholder = {
                        Text(
                            text = str("Kullanıcı ara (isim veya @kullanıcı)...", "Search users (name or @username)..."),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape),
                    trailingIcon = {
                        if (searchInput.isNotEmpty()) {
                            IconButton(onClick = {
                                searchInput = ""
                                onQueryChanged("")
                            }) {
                                Icon(Icons.Rounded.Clear, contentDescription = str("Temizle", "Clear"))
                            }
                        } else {
                            Icon(Icons.Rounded.Search, contentDescription = str("Ara", "Search"))
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp)
            ) {
                when {
                    isLoading && results.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    searchInput.trim().length >= 2 && results.isEmpty() && !isLoading -> {
                        SocialEmptyState(
                            icon = Icons.Rounded.Search,
                            title = str("Kullanıcı bulunamadı", "No users found"),
                            description = str(
                                "Farklı bir isim veya kullanıcı adı deneyin.",
                                "Try searching with a different name or username."
                            )
                        )
                    }
                    searchInput.trim().length < 2 && results.isEmpty() -> {
                        SocialEmptyState(
                            icon = Icons.Rounded.Search,
                            title = str("Arkadaşlarını Bul", "Find Friends"),
                            description = str(
                                "Arkadaş eklemek için en az 2 harf yazarak arama yap.",
                                "Type at least 2 characters to search and follow friends."
                            )
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(results, key = { it.id }) { user ->
                                val isBusy = pendingActionUserIds.contains(user.id)
                                val isPending = user.followRequestStatus == "PENDING"
                                val isFollowing = user.isFollowedByMe

                                val buttonText = when {
                                    isFollowing -> str("Takip Ediliyor", "Following")
                                    isPending -> str("İstek Gönderildi", "Requested")
                                    else -> str("Takip Et", "Follow")
                                }

                                val containerColor = if (isFollowing || isPending) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                                val contentColor = if (isFollowing || isPending) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarImage(
                                        profileImageUrl = user.profileImageUrl,
                                        fallbackInitial = user.name.firstOrNull()?.uppercase() ?: "?",
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (user.username.isNotBlank()) {
                                            Text(
                                                text = "@${user.username}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { if (!isBusy) onToggleFollow(user.id) },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = containerColor,
                                            contentColor = contentColor
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                        shape = CircleShape
                                    ) {
                                        Text(buttonText, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanoraFollowUserRow(
    profileImageUrl: String?,
    name: String,
    username: String,
    actionButton: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            profileImageUrl = profileImageUrl,
            fallbackInitial = name.firstOrNull()?.uppercase() ?: "?",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (username.isNotBlank()) {
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        actionButton()
    }
}

@Composable
private fun SocialEmptyState(
    icon: ImageVector,
    title: String,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.bounceClick()
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(actionLabel, maxLines = 1, softWrap = false)
            }
        }
    }
}
