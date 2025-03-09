package com.example.notificacao_app

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.text.TextUtils
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayOutputStream

class MainActivity: FlutterActivity() {
    private val CHANNEL = "notification_service"
    private var cachedApps: List<Map<String, Any>>? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "isNotificationServiceEnabled" -> {
                    result.success(isNotificationServiceEnabled())
                }
                "openNotificationListenerSettings" -> {
                    openNotificationListenerSettings()
                    result.success(null)
                }
                "getInstalledApps" -> {
                    result.success(getInstalledApps())
                }
                "openPackageSettings" -> {
                    openPackageSettings()
                    result.success(null)
                }
                "hasPackagePermission" -> {
                    result.success(hasPackagePermission())
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val componentName = ComponentName.unflattenFromString(name)
                if (componentName != null && TextUtils.equals(pkgName, componentName.packageName)) {
                    return true
                }
            }
        }
        return false
    }

    private fun openNotificationListenerSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getInstalledApps(): List<Map<String, Any>> {
        // Retorna o cache se existir
        cachedApps?.let { return it }

        val pm = packageManager
        return try {
            val flags = PackageManager.GET_META_DATA or PackageManager.GET_SHARED_LIBRARY_FILES
            val apps = pm.getInstalledApplications(flags)
                .filter { appInfo ->
                    try {
                        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                        
                        // Lista de pacotes a serem ignorados
                        val ignoredPackages = setOf(
                            "com.android.", "com.google.android.", "com.sec.",
                            "android.", "com.samsung.", "com.oneui.",
                            "com.osp.", "com.microsoft.android."
                        )
                        
                        // Verifica se o pacote deve ser ignorado
                        val shouldIgnore = ignoredPackages.any { prefix ->
                            appInfo.packageName.startsWith(prefix)
                        }
                        
                        // Lista de palavras-chave para identificar apps relevantes
                        val relevantKeywords = setOf(
                            "whatsapp", "telegram", "instagram", "facebook",
                            "twitter", "tiktok", "snapchat", "netflix",
                            "spotify", "youtube", "bank", "banking", "bradesco",
                            "itau", "santander", "caixa", "nubank", "inter",
                            "pay", "wallet", "mail", "outlook", "gmail"
                        )
                        
                        // Verifica se é um app relevante
                        val isRelevant = relevantKeywords.any { keyword ->
                            appInfo.packageName.toLowerCase().contains(keyword)
                        }

                        // Inclui o app se:
                        // 1. Não é um app do sistema OU
                        // 2. É um app do sistema atualizado OU
                        // 3. É um app relevante E
                        // 4. Não está na lista de pacotes ignorados
                        (!isSystemApp || isUpdatedSystemApp || isRelevant) && !shouldIgnore
                    } catch (e: Exception) {
                        false
                    }
                }
                .mapNotNull { appInfo ->
                    try {
                        val icon = appInfo.loadIcon(pm)
                        val size = 48 // Tamanho fixo para os ícones
                        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        
                        icon.setBounds(0, 0, size, size)
                        icon.draw(canvas)
                        
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val iconBytes = stream.toByteArray()
                        bitmap.recycle()
                        
                        mapOf(
                            "name" to (pm.getApplicationLabel(appInfo)?.toString() ?: appInfo.packageName),
                            "packageName" to appInfo.packageName,
                            "icon" to iconBytes
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                .sortedBy { it["name"] as String }

            // Armazena o resultado em cache
            cachedApps = apps
            apps
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Limpa o cache quando o app é pausado
    override fun onPause() {
        super.onPause()
        cachedApps = null
    }

    private fun hasPackagePermission(): Boolean {
        return try {
            // Tenta acessar a lista de pacotes
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun openPackageSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
