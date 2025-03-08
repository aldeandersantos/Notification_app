package com.example.notificacao_app

import android.content.Context
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor.DartEntrypoint
import org.json.JSONArray
import org.json.JSONObject

class NotificationService : NotificationListenerService() {
    private var channel: MethodChannel? = null
    private var flutterEngine: FlutterEngine? = null
    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        setupFlutterEngine()
        prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
    }

    private fun setupFlutterEngine() {
        if (flutterEngine == null) {
            flutterEngine = FlutterEngine(this).apply {
                dartExecutor.executeDartEntrypoint(DartEntrypoint.createDefault())
            }
            channel = MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, "notification_channel")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras
        val packageName = sbn.packageName
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""

        android.util.Log.d("NotificationService", "Notificação recebida: $packageName")

        // Carrega os filtros salvos
        val filtersJson = prefs.getString("flutter.app_filters", null)
        android.util.Log.d("NotificationService", "Filtros carregados: $filtersJson")
        
        if (filtersJson != null) {
            try {
                val filters = JSONArray(filtersJson)
                var shouldSend = false
                
                // Procura por um filtro que corresponda ao pacote
                for (i in 0 until filters.length()) {
                    val filter = filters.getJSONObject(i)
                    val filterPackageName = filter.getString("packageName") // Mudado de appName para packageName
                    val isEnabled = filter.getBoolean("isEnabled")
                    
                    android.util.Log.d("NotificationService", "Verificando filtro: packageName=$filterPackageName, isEnabled=$isEnabled")
                    
                    if (filterPackageName == packageName && isEnabled) {
                        // Verifica os filtros de texto
                        val textFilters = filter.getJSONArray("textFilters")
                        android.util.Log.d("NotificationService", "Filtros de texto: ${textFilters.length()} filtros")
                        
                        // Se não há filtros de texto, envia todas as notificações
                        if (textFilters.length() == 0) {
                            shouldSend = true
                            android.util.Log.d("NotificationService", "Sem filtros de texto, enviando notificação")
                            break
                        } else {
                            // Se há filtros, verifica se algum deles corresponde
                            for (j in 0 until textFilters.length()) {
                                val textFilter = textFilters.getString(j)
                                android.util.Log.d("NotificationService", "Verificando filtro de texto: $textFilter")
                                
                                if (title.contains(textFilter, ignoreCase = true) || 
                                    text.contains(textFilter, ignoreCase = true)) {
                                    shouldSend = true
                                    android.util.Log.d("NotificationService", "Filtro encontrado na mensagem")
                                    break
                                }
                            }
                        }
                        break
                    }
                }

                android.util.Log.d("NotificationService", "Decisão final: shouldSend=$shouldSend")

                // Se encontrou um filtro correspondente, envia a notificação
                if (shouldSend) {
                    val notificationData = mapOf(
                        "packageName" to packageName,
                        "title" to title,
                        "text" to text,
                        "timestamp" to sbn.postTime
                    )
                    
                    // Envia para o canal Flutter
                    channel?.invokeMethod("onNotificationReceived", notificationData)
                    
                    // Carrega os endpoints
                    val endpointsJson = prefs.getString("flutter.endpoints", null)
                    android.util.Log.d("NotificationService", "Endpoints carregados: $endpointsJson")
                    
                    if (!endpointsJson.isNullOrEmpty()) {
                        try {
                            val endpoints = JSONArray(endpointsJson)
                            for (i in 0 until endpoints.length()) {
                                val endpoint = endpoints.getJSONObject(i)
                                if (endpoint.getBoolean("isEnabled")) {
                                    val endpointUrl = endpoint.getString("url")
                                    val endpointType = endpoint.getString("type")
                                    val endpointName = endpoint.getString("name")
                                    
                                    android.util.Log.d("NotificationService", "Processando endpoint: $endpointName ($endpointType)")
                                    
                                    // Formata a data/hora
                                    val timestamp = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                                        .format(java.util.Date(sbn.postTime))
                                    
                                    // Cria a mensagem formatada
                                    val message = StringBuilder()
                                        .append("📱 **Aplicativo:** $packageName\n")
                                        .append("⏰ **Horário:** $timestamp\n")
                                        .append("📌 **Título:** $title\n")
                                        .append("💬 **Mensagem:** $text")
                                        .toString()

                                    Thread {
                                        try {
                                            when {
                                                endpointType.contains("webhook", ignoreCase = true) -> {
                                                    android.util.Log.d("NotificationService", "Enviando para webhook: $endpointUrl")
                                                    sendWebhook(endpointUrl, message)
                                                }
                                                endpointType.contains("ip", ignoreCase = true) -> {
                                                    android.util.Log.d("NotificationService", "Enviando para IP: $endpointUrl")
                                                    sendToIp(endpointUrl, notificationData)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("NotificationService", "Erro ao enviar para endpoint $endpointName: ${e.message}")
                                            e.printStackTrace()
                                        }
                                    }.start()
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationService", "Erro ao processar endpoints: ${e.message}")
                            e.printStackTrace()
                        }
                    } else {
                        android.util.Log.d("NotificationService", "Nenhum endpoint configurado")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendWebhook(url: String, message: String) {
        val client = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        try {
            client.requestMethod = "POST"
            client.setRequestProperty("Content-Type", "application/json")
            client.doOutput = true
            
            val jsonBody = JSONObject().apply {
                put("content", message)
            }.toString()
            
            client.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
            }
            
            val responseCode = client.responseCode
            if (responseCode !in 200..299) {
                android.util.Log.e("NotificationService", "Erro ao enviar para webhook: $responseCode")
            }
        } finally {
            client.disconnect()
        }
    }

    private fun sendToIp(url: String, data: Map<String, Any>) {
        val client = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        try {
            client.requestMethod = "POST"
            client.setRequestProperty("Content-Type", "application/json")
            client.doOutput = true
            
            val jsonBody = JSONObject(data).toString()
            
            client.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
            }
            
            val responseCode = client.responseCode
            if (responseCode !in 200..299) {
                android.util.Log.e("NotificationService", "Erro ao enviar para IP: $responseCode")
            }
        } finally {
            client.disconnect()
        }
    }

    override fun onDestroy() {
        channel = null
        flutterEngine?.destroy()
        flutterEngine = null
        super.onDestroy()
    }
} 