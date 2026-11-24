package com.restapp.app

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/** Identifica redes sociales por paquete conocido o por la categoría que declara la app. */
object Social {
    private val KNOWN = setOf(
        "com.zhiliaoapp.musically",      // TikTok
        "com.ss.android.ugc.trill",      // TikTok (Asia)
        "com.instagram.android",         // Instagram
        "com.instagram.barcelona",       // Threads
        "com.facebook.katana",           // Facebook
        "com.facebook.lite",             // Facebook Lite
        "com.facebook.orca",             // Messenger
        "com.whatsapp",                  // WhatsApp
        "com.whatsapp.w4b",              // WhatsApp Business
        "com.snapchat.android",          // Snapchat
        "com.twitter.android",           // X / Twitter
        "org.telegram.messenger",        // Telegram
        "com.discord",                   // Discord
        "com.reddit.frontpage",          // Reddit
        "com.pinterest",                 // Pinterest
        "com.tumblr",                    // Tumblr
        "com.bereal.ft",                 // BeReal
        "com.google.android.youtube"     // YouTube (Shorts)
    )

    fun isSocial(pm: PackageManager, pkg: String): Boolean {
        if (pkg in KNOWN) return true
        return try {
            pm.getApplicationInfo(pkg, 0).category == ApplicationInfo.CATEGORY_SOCIAL
        } catch (e: Exception) {
            false
        }
    }
}
