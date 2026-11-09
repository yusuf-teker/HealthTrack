package com.yusufteker.healthtrack.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class AndroidExternalAppLauncher(private val context: Context) : ExternalAppLauncher {
    override fun isAppInstalled(app: ExternalApp): Boolean {
        return try {
            val pm = context.packageManager
            // 1. Check if launch intent exists by package name
            if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                return true
            }
            // 2. Check if deep link scheme resolves
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.scheme))
            intent.resolveActivity(pm) != null
        } catch (_: Exception) {
            false
        }
    }

    override fun openApp(app: ExternalApp): Boolean {
        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(app.scheme)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (viewIntent.resolveActivity(pm) != null) {
                    context.startActivity(viewIntent)
                    true
                } else {
                    false
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun shareToWhatsApp(text: String, phoneNumber: String?): Boolean {
        val cleanedPhone = phoneNumber?.filter { it.isDigit() }?.let { digits ->
            when {
                digits.startsWith("0") && digits.length == 11 -> "90${digits.drop(1)}"
                digits.length == 10 -> "90$digits"
                else -> digits
            }
        }?.takeIf { it.length >= 10 }

        return try {
            val encodedText = Uri.encode(text)
            val uriString = if (cleanedPhone != null) {
                "https://api.whatsapp.com/send?phone=$cleanedPhone&text=$encodedText"
            } else {
                "whatsapp://send?text=$encodedText"
            }
            val directIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(directIntent)
            true
        } catch (_: Exception) {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(shareIntent)
                true
            } catch (_: Exception) {
                try {
                    val chooser = Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        },
                        "Raporu Paylaş"
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }
    }
}

@Composable
actual fun rememberExternalAppLauncher(): ExternalAppLauncher {
    val context = LocalContext.current
    return remember(context) {
        AndroidExternalAppLauncher(context)
    }
}
