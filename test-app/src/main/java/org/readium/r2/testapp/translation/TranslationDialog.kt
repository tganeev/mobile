package org.readium.r2.testapp.translation

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View

import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.launch
import org.readium.r2.testapp.R
import kotlin.math.min

class TranslationDialog : DialogFragment() {

    private var selectedText: String = ""
    private var onTranslationComplete: ((String) -> Unit)? = null

    private lateinit var translationService: TranslationService

    private var sourceLanguageCode: String = TranslateLanguage.ENGLISH
    private var targetLanguageCode: String = TranslateLanguage.RUSSIAN

    private lateinit var downloadButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var resultText: TextView
    private lateinit var loadingText: TextView
    private lateinit var originalText: TextView
    private lateinit var sourceLanguageText: TextView
    private lateinit var targetLanguageText: TextView

    companion object {
        fun newInstance(selectedText: String): TranslationDialog {
            val fragment = TranslationDialog()
            val args = Bundle()
            args.putString("selected_text", selectedText)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedText = arguments?.getString("selected_text") ?: ""
        translationService = TranslationService(requireContext())

        loadLanguageSettings()

        setStyle(STYLE_NO_FRAME, androidx.appcompat.R.style.Theme_AppCompat_DayNight_NoActionBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_translation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)

            val drawable = GradientDrawable().apply {
                setColor(ContextCompat.getColor(requireContext(), R.color.background_dialog))
                cornerRadius = 24f
            }
            window.setBackgroundDrawable(drawable)
        }

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        val settingsButton = view.findViewById<View>(R.id.settingsButton)

        downloadButton = view.findViewById(R.id.downloadButton)
        progressBar = view.findViewById(R.id.progressBar)
        resultText = view.findViewById(R.id.resultText)
        loadingText = view.findViewById(R.id.loadingText)
        originalText = view.findViewById(R.id.originalText)
        sourceLanguageText = view.findViewById(R.id.sourceLanguageText)
        targetLanguageText = view.findViewById(R.id.targetLanguageText)

        // Включаем скролл для TextView
        originalText.movementMethod = ScrollingMovementMethod()
        originalText.isVerticalScrollBarEnabled = true

        resultText.movementMethod = ScrollingMovementMethod()
        resultText.isVerticalScrollBarEnabled = true

        toolbar.setNavigationOnClickListener {
            dismiss()
        }

        settingsButton.setOnClickListener {
            showLanguageSettings()
        }

        setupButtons()

        originalText.text = selectedText
        updateLanguageDisplay()

        startTranslation()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val displayMetrics = resources.displayMetrics
            val maxWidthDp = 400
            val maxWidthPx = (maxWidthDp * displayMetrics.density).toInt()
            val width = min((displayMetrics.widthPixels * 0.85).toInt(), maxWidthPx)

            val params = window.attributes
            params.width = width
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = params
        }
    }

    private fun loadLanguageSettings() {
        val prefs = requireContext().getSharedPreferences("translation_prefs", android.content.Context.MODE_PRIVATE)
        sourceLanguageCode = prefs.getString("source_language", TranslateLanguage.ENGLISH) ?: TranslateLanguage.ENGLISH
        targetLanguageCode = prefs.getString("target_language", TranslateLanguage.RUSSIAN) ?: TranslateLanguage.RUSSIAN
    }

    private fun saveLanguageSettings() {
        val prefs = requireContext().getSharedPreferences("translation_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("source_language", sourceLanguageCode)
            .putString("target_language", targetLanguageCode)
            .apply()
    }

    private fun updateLanguageDisplay() {
        val languages = TranslationService.SUPPORTED_LANGUAGES
        val sourceName = languages.find { it.first == sourceLanguageCode }?.second ?: "Английский"
        val targetName = languages.find { it.first == targetLanguageCode }?.second ?: "Русский"

        sourceLanguageText.text = sourceName
        targetLanguageText.text = targetName
    }

    private fun showLanguageSettings() {
        val dialog = LanguageSettingsDialog.newInstance(
            sourceLanguageCode,
            targetLanguageCode
        ) { newSource, newTarget ->
            sourceLanguageCode = newSource
            targetLanguageCode = newTarget
            saveLanguageSettings()
            updateLanguageDisplay()
            startTranslation()
        }
        dialog.show(childFragmentManager, "LanguageSettings")
    }

    private fun setupButtons() {
        downloadButton.setOnClickListener {
            downloadModel()
        }
    }

    private fun startTranslation() {
        lifecycleScope.launch {
            val isDownloaded = translationService.isModelDownloaded(sourceLanguageCode, targetLanguageCode)

            if (!isDownloaded) {
                downloadButton.visibility = View.VISIBLE
                downloadButton.isEnabled = true
                resultText.text = "Для перевода необходимо скачать языковую модель (требуется Wi-Fi)"
                resultText.visibility = View.VISIBLE
                return@launch
            }

            downloadButton.visibility = View.GONE
            setLoading(true)

            val translationResult = translationService.translate(selectedText, sourceLanguageCode, targetLanguageCode)

            setLoading(false)

            when (translationResult) {
                is TranslationResult.Success -> {
                    resultText.text = translationResult.translatedText
                    resultText.visibility = View.VISIBLE
                    onTranslationComplete?.invoke(translationResult.translatedText)
                }
                is TranslationResult.ModelNotDownloaded -> {
                    downloadButton.visibility = View.VISIBLE
                    downloadButton.isEnabled = true
                    resultText.text = "Языковая модель не загружена. Нажмите 'Скачать модель'"
                    resultText.visibility = View.VISIBLE
                }
                is TranslationResult.Error -> {
                    resultText.text = "Ошибка перевода: ${translationResult.message}"
                    resultText.visibility = View.VISIBLE
                }
                TranslationResult.Downloading -> {}
            }
        }
    }

    private fun downloadModel() {
        lifecycleScope.launch {
            downloadButton.isEnabled = false
            progressBar.visibility = View.VISIBLE
            loadingText.visibility = View.VISIBLE
            loadingText.text = "Скачивание модели... 0%"

            try {
                val success = translationService.downloadModel(sourceLanguageCode, targetLanguageCode)

                progressBar.visibility = View.GONE
                loadingText.visibility = View.GONE

                if (success) {
                    downloadButton.visibility = View.GONE
                    startTranslation()
                } else {
                    resultText.text = "Не удалось скачать модель. Проверьте подключение к интернету."
                    resultText.visibility = View.VISIBLE
                    downloadButton.isEnabled = true
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                loadingText.visibility = View.GONE
                resultText.text = "Ошибка скачивания: ${e.message}"
                resultText.visibility = View.VISIBLE
                downloadButton.isEnabled = true
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        loadingText.visibility = if (isLoading) View.VISIBLE else View.GONE
        loadingText.text = if (isLoading) "Перевод..." else ""
    }

    fun setOnTranslationComplete(callback: (String) -> Unit) {
        onTranslationComplete = callback
    }

    override fun onDestroyView() {
        super.onDestroyView()
        translationService.close()
    }
}