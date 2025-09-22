package com.micolom.traducico

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class MainActivity : AppCompatActivity() {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: android.content.Intent
    private lateinit var tvResult: TextView
    private lateinit var btnStart: Button
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tvResult)
        btnStart = findViewById(R.id.btnStart)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = RecognizerIntent().apply {
            action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        btnStart.setOnClickListener {
            if (!isListening) {
                checkAudioPermissionAndStart()
            } else {
                stopListening()
            }
        }
    }

    private fun checkAudioPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            startListening()
        }
    }

    private fun startListening() {
        isListening = true
        btnStart.text = "Detener Traducción"
        tvResult.text = "Escuchando..."
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                if (isListening) startListening() // Reinicia en caso de error
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    processText(matches[0])
                }
                if (isListening) startListening() // Reinicia para escuchar continuamente
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer.startListening(recognizerIntent)
    }

    private fun stopListening() {
        isListening = false
        btnStart.text = "Iniciar Traducción Continua"
        speechRecognizer.stopListening()
        tvResult.text = "Traducción detenida"
    }

    private fun processText(text: String) {
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode != "es" && languageCode != "und") {
                    translateText(text, languageCode)
                } else {
                    tvResult.text = text // Ya está en español
                }
            }
            .addOnFailureListener {
                tvResult.text = "No se pudo identificar el idioma."
            }
    }

    private fun translateText(text: String, sourceLang: String) {
        val sourceLangCode = TranslateLanguage.fromLanguageTag(sourceLang) ?: return
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLangCode)
            .setTargetLanguage(TranslateLanguage.SPANISH)
            .build()
        val translator = Translation.getClient(options)
        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        tvResult.text = translatedText
                    }
                    .addOnFailureListener {
                        tvResult.text = "Error al traducir."
                    }
            }
            .addOnFailureListener {
                tvResult.text = "No se pudo descargar el modelo de traducción."
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            tvResult.text = "Permiso de micrófono denegado."
        }
    }
}