package io.github.martystev.motionsentinel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class TelegramSender(
    private val settings: SettingsManager
) {
    private val client = OkHttpClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun sendMessage(text: String) {
        val token = settings.getTelegramToken()
        val chatId = settings.getTelegramChatId()
        if (token.isBlank() || chatId.isBlank()) return

        val url = "https://api.telegram.org/bot$token/sendMessage"
        val body = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", text)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        scope.launch {
            try {
                client.newCall(request).execute().use { }
            } catch (_: Exception) {
                // Swallow network errors; no UI surface in service
            }
        }
    }
}
