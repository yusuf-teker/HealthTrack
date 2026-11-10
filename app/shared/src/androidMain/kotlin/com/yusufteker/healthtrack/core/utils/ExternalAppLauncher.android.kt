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

    override fun sendToPlanoraTodo(title: String, items: List<String>): Boolean {
        if (items.isEmpty()) return false
        val noteBody = items.joinToString("\n") { "• $it" }
        val formattedBody = buildString {
            appendLine(title)
            appendLine()
            append(noteBody)
        }

        val isInstalled = isAppInstalled(ExternalApp.PLANORA)

        // 1. Try Planora custom deep link
        if (isInstalled) {
            try {
                val encodedTitle = Uri.encode(title)
                val encodedNote = Uri.encode(noteBody)
                val encodedItems = Uri.encode(items.joinToString(";") { it.trim() })
                val planoraUri = Uri.parse("planora://share/task?title=$encodedTitle&note=$encodedNote&items=$encodedItems")
                val viewIntent = Intent(Intent.ACTION_VIEW, planoraUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (viewIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(viewIntent)
                    return true
                }
            } catch (_: Exception) {}

            // 2. Try explicit Intent targeted at Planora app package
            try {
                val directIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    `package` = ExternalApp.PLANORA.packageName
                    putExtra(Intent.EXTRA_TITLE, title)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, formattedBody)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(directIntent)
                return true
            } catch (_: Exception) {}
        }

        // 3. Planora is NOT installed or deep link could not be dispatched:
        // Copy list to system clipboard for instant paste
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(title, formattedBody)
            clipboard?.setPrimaryClip(clip)
        } catch (_: Exception) {}

        // Fallback: Open system chooser so user can share with any app
        try {
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, title)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, formattedBody)
                },
                "Listeyi Paylaş"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {}

        return false
    }
}

@Composable
actual fun rememberExternalAppLauncher(): ExternalAppLauncher {
    val context = LocalContext.current
    return remember(context) {
        AndroidExternalAppLauncher(context)
    }
}
