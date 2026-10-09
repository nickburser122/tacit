package app.tacit.sources

import android.provider.Settings

class CatalogEntry(val action: String, val label: String, val synonyms: List<String>)

object SettingsCatalog {

    private fun e(action: String, label: String, vararg synonyms: String) = CatalogEntry(action, label, synonyms.toList())

    val entries: List<CatalogEntry> = listOf(
        e(Settings.ACTION_SETTINGS, "Settings", "system settings", "preferences", "الإعدادات"),
        e(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi", "wifi", "wlan", "wireless", "network", "واي فاي"),
        e(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth", "bt", "pair", "headphones", "بلوتوث"),
        e(Settings.ACTION_WIRELESS_SETTINGS, "Network & internet", "mobile network", "connections"),
        e(Settings.ACTION_AIRPLANE_MODE_SETTINGS, "Airplane mode", "flight mode", "aeroplane", "وضع الطيران"),
        e(Settings.ACTION_DATA_ROAMING_SETTINGS, "Mobile data & roaming", "cellular", "sim", "roaming", "apn"),
        e(Settings.ACTION_DATA_USAGE_SETTINGS, "Data usage", "data saver", "data limit"),
        e(Settings.ACTION_NETWORK_OPERATOR_SETTINGS, "Network operators", "carrier"),
        e(Settings.ACTION_APN_SETTINGS, "Access point names", "apn"),
        e(Settings.ACTION_VPN_SETTINGS, "VPN", "virtual private network"),
        e(Settings.ACTION_NFC_SETTINGS, "NFC", "contactless", "tap to pay"),
        e(Settings.ACTION_NFC_PAYMENT_SETTINGS, "Contactless payments", "tap to pay", "wallet"),
        e(Settings.ACTION_CAST_SETTINGS, "Cast", "screen mirroring", "chromecast", "smart view"),
        e(Settings.ACTION_DISPLAY_SETTINGS, "Display", "screen", "brightness", "dark mode", "dark theme", "font size", "screen timeout", "الشاشة"),
        e(Settings.ACTION_NIGHT_DISPLAY_SETTINGS, "Night light", "blue light", "eye comfort", "night shift"),
        e(Settings.ACTION_DREAM_SETTINGS, "Screen saver", "daydream"),
        e(Settings.ACTION_SOUND_SETTINGS, "Sound & vibration", "volume", "ringtone", "vibrate", "الصوت"),
        e(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS, "Do Not Disturb", "dnd", "silence", "focus", "zen"),
        e(Settings.ACTION_NOTIFICATION_ASSISTANT_SETTINGS, "Notification assistant"),
        e("android.settings.NOTIFICATION_SETTINGS", "Notifications", "notification history", "lock screen notifications", "الإشعارات"),
        e("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS", "Notification access", "notification listener"),
        e(Settings.ACTION_APPLICATION_SETTINGS, "Apps", "applications", "installed apps", "التطبيقات"),
        e(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS, "All apps", "app list"),
        e(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, "Default apps", "default browser", "default launcher", "home app", "default sms", "default assistant"),
        e(Settings.ACTION_HOME_SETTINGS, "Home app", "default launcher", "launcher"),
        e(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "Install unknown apps", "unknown sources", "sideload", "apk"),
        e(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "Display over other apps", "overlay", "draw over"),
        e(Settings.ACTION_MANAGE_WRITE_SETTINGS, "Modify system settings", "write settings"),
        e(Settings.ACTION_USAGE_ACCESS_SETTINGS, "Usage access"),
        e(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, "Battery optimization", "doze", "background restriction"),
        e(Settings.ACTION_BATTERY_SAVER_SETTINGS, "Battery saver", "power saving", "low power", "البطارية"),
        e(Intent_ACTION_POWER_USAGE_SUMMARY, "Battery usage", "battery", "power usage", "battery health"),
        e(Settings.ACTION_INTERNAL_STORAGE_SETTINGS, "Storage", "disk", "free space", "التخزين"),
        e(Settings.ACTION_MEMORY_CARD_SETTINGS, "SD card", "memory card", "external storage"),
        e(Settings.ACTION_SECURITY_SETTINGS, "Security", "screen lock", "pin", "pattern", "password", "fingerprint", "face unlock", "الأمان"),
        e(Settings.ACTION_BIOMETRIC_ENROLL, "Biometrics", "fingerprint", "face unlock", "enroll"),
        e(Settings.ACTION_PRIVACY_SETTINGS, "Privacy", "permissions", "permission manager", "الخصوصية"),
        e(Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Location", "gps", "maps", "الموقع"),
        e(Settings.ACTION_ACCESSIBILITY_SETTINGS, "Accessibility", "talkback", "magnification", "a11y", "font size", "إمكانية الوصول"),
        e(Settings.ACTION_CAPTIONING_SETTINGS, "Captions", "subtitles", "live caption"),
        e(Settings.ACTION_LOCALE_SETTINGS, "Languages", "language", "locale", "اللغة"),
        e(Settings.ACTION_INPUT_METHOD_SETTINGS, "Keyboards", "input method", "ime", "gboard", "لوحة المفاتيح"),
        e(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS, "Keyboard languages"),
        e(Settings.ACTION_HARD_KEYBOARD_SETTINGS, "Physical keyboard", "hardware keyboard"),
        e(Settings.ACTION_USER_DICTIONARY_SETTINGS, "Personal dictionary", "user dictionary"),
        e(Settings.ACTION_DATE_SETTINGS, "Date & time", "time zone", "clock", "التاريخ والوقت"),
        e(Settings.ACTION_SYNC_SETTINGS, "Accounts", "sync", "google account", "الحسابات"),
        e(Settings.ACTION_ADD_ACCOUNT, "Add account"),
        e(Settings.ACTION_DEVICE_INFO_SETTINGS, "About phone", "device info", "build number", "android version", "model", "عن الهاتف"),
        e(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, "Developer options", "usb debugging", "adb", "dev options", "خيارات المطور"),
        e(Settings.ACTION_WIFI_IP_SETTINGS, "Wi-Fi advanced", "ip address", "static ip"),
        e("android.settings.TETHER_SETTINGS", "Hotspot & tethering", "hotspot", "tethering", "portable hotspot", "نقطة اتصال"),
        e(Settings.ACTION_PRINT_SETTINGS, "Printing", "printer", "print"),
        e(Settings.ACTION_SEARCH_SETTINGS, "Search settings"),
        e(Settings.ACTION_QUICK_LAUNCH_SETTINGS, "Quick launch"),
        e(Settings.ACTION_VOICE_INPUT_SETTINGS, "Voice input", "assistant", "speech"),
        e(Settings.ACTION_WEBVIEW_SETTINGS, "WebView implementation"),
        e(Settings.ACTION_FINGERPRINT_ENROLL, "Fingerprint", "enroll fingerprint"),
        e(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION, "All files access", "storage permission"),
        e("android.settings.REGIONAL_PREFERENCES_SETTINGS", "Regional preferences", "units", "temperature unit", "first day of week"),
        e("android.settings.AUTO_ROTATE_SETTINGS", "Auto-rotate", "rotation", "screen rotation"),
        e("android.settings.ZEN_MODE_SETTINGS", "Modes", "bedtime", "driving mode", "focus mode")
    )

    private const val Intent_ACTION_POWER_USAGE_SUMMARY = "android.intent.action.POWER_USAGE_SUMMARY"

    val panels: List<CatalogEntry> = listOf(
        e(Settings.Panel.ACTION_INTERNET_CONNECTIVITY, "Internet panel", "quick internet", "mobile data toggle", "wifi toggle"),
        e(Settings.Panel.ACTION_NFC, "NFC panel", "nfc toggle"),
        e(Settings.Panel.ACTION_VOLUME, "Volume panel", "volume", "media volume", "ring volume"),
        e(Settings.Panel.ACTION_WIFI, "Wi-Fi panel", "wifi toggle", "wifi on", "wifi off")
    )

    val skipped: Set<String> = setOf(
        "android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION",
        "android.settings.APPLICATION_DETAILS_SETTINGS",
        "android.settings.APP_NOTIFICATION_SETTINGS",
        "android.settings.CHANNEL_NOTIFICATION_SETTINGS",
        "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        "android.settings.action.MANAGE_OVERLAY_PERMISSION",
        "android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT",
        "android.settings.APP_OPEN_BY_DEFAULT_SETTINGS",
        "android.settings.APP_SEARCH_SETTINGS",
        "android.settings.REQUEST_MANAGE_MEDIA",
        "android.settings.REQUEST_SET_AUTOFILL_SERVICE",
        "android.settings.MANAGE_SUPERVISOR_RESTRICTED_SETTING",
        "android.settings.VOICE_CONTROL_AIRPLANE_MODE",
        "android.settings.VOICE_CONTROL_BATTERY_SAVER_MODE",
        "android.settings.VOICE_CONTROL_DO_NOT_DISTURB_MODE",
        "android.settings.STORAGE_VOLUME_ACCESS_SETTINGS",
        "android.settings.ADD_ACCOUNT_SETTINGS"
    )
}
