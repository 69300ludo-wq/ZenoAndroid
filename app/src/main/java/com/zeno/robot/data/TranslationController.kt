package com.zeno.robot.data

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslationController {
    fun translate(
        text: String,
        sourceTag: String,
        targetTag: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val source = TranslateLanguage.fromLanguageTag(sourceTag)
        val target = TranslateLanguage.fromLanguageTag(targetTag)
        if (source == null || target == null) {
            onError("Langue non prise en charge")
            return
        }
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        val translator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translated ->
                        onResult(translated)
                        translator.close()
                    }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Erreur de traduction")
                        translator.close()
                    }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Téléchargement du modèle impossible")
                translator.close()
            }
    }
}
