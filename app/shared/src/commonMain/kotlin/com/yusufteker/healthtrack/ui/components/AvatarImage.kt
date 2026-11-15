package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage

/**
 * Optimizes Cloudinary avatar URLs for crisp circular thumbnail rendering.
 */
fun getOptimizedCloudinaryUrl(url: String): String {
    if (!url.contains("res.cloudinary.com") || !url.contains("/upload/")) return url
    if (url.contains("/upload/w_")) return url
    return url.replaceFirst("/upload/", "/upload/w_200,h_200,c_fill,q_auto,f_auto/")
}

/**
 * Renders a user's remote [profileImageUrl] via Coil 3 if available,
 * falling back to an initial-letter avatar badge.
 */
@Composable
fun AvatarImage(
    profileImageUrl: String?,
    fallbackInitial: String,
    modifier: Modifier = Modifier,
    fallbackColor: Color = MaterialTheme.colorScheme.primary,
    fallbackBackgroundBrush: Brush? = null,
    fallbackBackgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
    textStyle: TextStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
) {
    val cleanUrl = profileImageUrl?.trim()?.takeIf { it.isNotEmpty() }
    if (cleanUrl != null) {
        AsyncImage(
            model = getOptimizedCloudinaryUrl(cleanUrl),
            contentDescription = "Profile Image",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val baseMod = modifier.clip(CircleShape)
        val bgMod = if (fallbackBackgroundBrush != null) {
            baseMod.background(fallbackBackgroundBrush)
        } else {
            baseMod.background(fallbackBackgroundColor)
        }
        Box(
            modifier = bgMod,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackInitial.take(1).uppercase().ifBlank { "?" },
                style = textStyle,
                color = fallbackColor
            )
        }
    }
}

