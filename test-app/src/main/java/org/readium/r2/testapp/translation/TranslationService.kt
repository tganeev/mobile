package org.readium.r2.testapp.translation

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed class TranslationResult {
    data class Success(val translatedText: String) : TranslationResult()
    data class Error(val message: String) : TranslationResult()
    object ModelNotDownloaded : TranslationResult()
    object Downloading : TranslationResult()
}

class TranslationService(private val context: Context) {

    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            TranslateLanguage.ENGLISH to "Английский",
            TranslateLanguage.RUSSIAN to "Русский",
            TranslateLanguage.GERMAN to "Немецкий",
            TranslateLanguage.FRENCH to "Французский",
            TranslateLanguage.SPANISH to "Испанский",
            TranslateLanguage.ITALIAN to "Итальянский"
        )
    }

    suspend fun isModelDownloaded(sourceLanguage: String, targetLanguage: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()

            val translator = Translation.getClient(options)
            translator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    translator.close()
                    continuation.resume(true)
                }
                .addOnFailureListener {
                    translator.close()
                    continuation.resume(false)
                }
        }
    }

    suspend fun downloadModel(sourceLanguage: String, targetLanguage: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()

            val translator = Translation.getClient(options)

            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    translator.close()
                    continuation.resume(true)
                }
                .addOnFailureListener {
                    translator.close()
                    continuation.resume(false)
                }
        }
    }

    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslationResult {
        return suspendCancellableCoroutine { continuation ->
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()

            val translator = Translation.getClient(options)

            translator.translate(text)
                .addOnSuccessListener { translatedText ->
                    translator.close()
                    continuation.resume(TranslationResult.Success(translatedText))
                }
                .addOnFailureListener { e ->
                    translator.close()
                    val errorMessage = e.message ?: "Ошибка перевода"
                    if (errorMessage.contains("model") || errorMessage.contains("Model")) {
                        continuation.resume(TranslationResult.ModelNotDownloaded)
                    } else {
                        continuation.resume(TranslationResult.Error(errorMessage))
                    }
                }
        }
    }

    fun close() {
        // Ничего не делаем
    }
}