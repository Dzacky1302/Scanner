package com.example.scanner.utils

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LanguageOption(
    val code: String,
    val displayName: String
)

object TranslatorManager {

    val supportedLanguages = listOf(
        LanguageOption(TranslateLanguage.INDONESIAN, "Indonesia (Indonesian)"),
        LanguageOption(TranslateLanguage.ENGLISH, "Inggris (English)"),
        LanguageOption(TranslateLanguage.SPANISH, "Spanyol (Spanish)"),
        LanguageOption(TranslateLanguage.FRENCH, "Prancis (French)"),
        LanguageOption(TranslateLanguage.GERMAN, "Jerman (German)"),
        LanguageOption(TranslateLanguage.JAPANESE, "Jepang (Japanese)"),
        LanguageOption(TranslateLanguage.CHINESE, "China (Chinese)"),
        LanguageOption(TranslateLanguage.KOREAN, "Korea (Korean)"),
        LanguageOption(TranslateLanguage.ARABIC, "Arab (Arabic)"),
        LanguageOption(TranslateLanguage.RUSSIAN, "Rusia (Russian)")
    )

    suspend fun translateText(
        text: String,
        sourceLangCode: String,
        targetLangCode: String
    ): String = suspendCancellableCoroutine { continuation ->
        if (text.isBlank()) {
            continuation.resume("")
            return@suspendCancellableCoroutine
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLangCode)
            .setTargetLanguage(targetLangCode)
            .build()

        val translator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder().build()

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        translator.close()
                        continuation.resume(translatedText)
                    }
                    .addOnFailureListener { e ->
                        translator.close()
                        continuation.resumeWithException(e)
                    }
            }
            .addOnFailureListener { e ->
                translator.close()
                continuation.resumeWithException(e)
            }
    }
}
