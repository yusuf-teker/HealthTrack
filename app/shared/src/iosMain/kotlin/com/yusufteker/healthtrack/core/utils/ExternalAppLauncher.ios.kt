package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.ktor.http.encodeURLParameter
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosExternalAppLauncher : ExternalAppLauncher {
    override fun isAppInstalled(app: ExternalApp): Boolean {
        val url = NSURL.URLWithString(app.scheme) ?: return false
        return UIApplication.sharedApplication.canOpenURL(url)
    }

    override fun openApp(app: ExternalApp): Boolean {
        val url = NSURL.URLWithString(app.scheme) ?: return false
        if (UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
            return true
        }
        return false
    }

    override fun shareToWhatsApp(text: String, phoneNumber: String?): Boolean {
        val cleanedPhone = phoneNumber?.filter { it.isDigit() }?.let { digits ->
            when {
                digits.startsWith("0") && digits.length == 11 -> "90${digits.drop(1)}"
                digits.length == 10 -> "90$digits"
                else -> digits
            }
        }?.takeIf { it.length >= 10 }

        val encoded = text.encodeURLParameter()
        val urlStr = if (cleanedPhone != null) {
            "https://wa.me/$cleanedPhone?text=$encoded"
        } else {
            "whatsapp://send?text=$encoded"
        }
        val url = NSURL.URLWithString(urlStr) ?: return false
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
        return true
    }

    override fun sendToPlanoraTodo(title: String, items: List<String>): Boolean {
        if (items.isEmpty()) return false
        val formattedItems = items.joinToString(";") { it.trim() }
        val urlStr = "planora://todo/add?title=${title.encodeURLParameter()}&items=${formattedItems.encodeURLParameter()}"
        val url = NSURL.URLWithString(urlStr) ?: return false
        if (UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
            return true
        }
        return false
    }
}

@Composable
actual fun rememberExternalAppLauncher(): ExternalAppLauncher {
    return remember {
        IosExternalAppLauncher()
    }
}
