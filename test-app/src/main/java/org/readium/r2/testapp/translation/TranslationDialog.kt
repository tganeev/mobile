package org.readium.r2.testapp.translation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.launch

class TranslationDialog : DialogFragment() {

    private var selectedText: String = ""
    private var onTranslationComplete: ((String) -> Unit)? = null

    private lateinit var translationService: TranslationService
    private lateinit var sourceLanguageSpinner: Spinner
    private lateinit var targetLanguageSpinner: Spinner
    private lateinit var translateButton: Button
    private lateinit var downloadButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var resultText: TextView
    private lateinit var loadingText: TextView
    private lateinit var originalText: TextView

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
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(org.readium.r2.testapp.R.layout.dialog_translation, container, false)
    }

    @Suppress("DEPRECATION")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sourceLanguageSpinner = view.findViewById(org.readium.r2.testapp.R.id.sourceLanguageSpinner)
        targetLanguageSpinner = view.findViewById(org.readium.r2.testapp.R.id.targetLanguageSpinner)
        translateButton = view.findViewById(org.readium.r2.testapp.R.id.translateButton)
        downloadButton = view.findViewById(org.readium.r2.testapp.R.id.downloadButton)
        progressBar = view.findViewById(org.readium.r2.testapp.R.id.progressBar)
        resultText = view.findViewById(org.readium.r2.testapp.R.id.resultText)
        loadingText = view.findViewById(org.readium.r2.testapp.R.id.loadingText)
        originalText = view.findViewById(org.readium.r2.testapp.R.id.originalText)

        setupLanguageSpinners()
        setupButtons()

        originalText.text = selectedText

        // Автоматически проверяем и загружаем модель для выбранных языков
        checkAndDownloadModel()
    }

    private fun setupLanguageSpinners() {
        val languages = TranslationService.SUPPORTED_LANGUAGES
        val languageNames = languages.map { it.second }.toList()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languageNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        sourceLanguageSpinner.adapter = adapter
        targetLanguageSpinner.adapter = adapter

        val englishIndex = languages.indexOfFirst { it.first == TranslateLanguage.ENGLISH }
        val russianIndex = languages.indexOfFirst { it.first == TranslateLanguage.RUSSIAN }

        if (englishIndex >= 0) sourceLanguageSpinner.setSelection(englishIndex)
        if (russianIndex >= 0) targetLanguageSpinner.setSelection(russianIndex)

        // Слушатели для перезагрузки модели при смене языка
        sourceLanguageSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                checkAndDownloadModel()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        targetLanguageSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                checkAndDownloadModel()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
        translateButton.setOnClickListener {
            performTranslation()
        }

        downloadButton.setOnClickListener {
            downloadModel()
        }
    }

    private fun checkAndDownloadModel() {
        lifecycleScope.launch {
            val languages = TranslationService.SUPPORTED_LANGUAGES
            val sourceIndex = sourceLanguageSpinner.selectedItemPosition
            val targetIndex = targetLanguageSpinner.selectedItemPosition

            if (sourceIndex < 0 || targetIndex < 0) return@launch

            val sourceLanguage = languages[sourceIndex].first
            val targetLanguage = languages[targetIndex].first

            // Проверяем, загружена ли модель
            val isDownloaded = translationService.isModelDownloaded(sourceLanguage, targetLanguage)

            if (isDownloaded) {
                downloadButton.visibility = View.GONE
                translateButton.isEnabled = true
                translateButton.text = "Перевести"
            } else {
                downloadButton.visibility = View.VISIBLE
                downloadButton.isEnabled = true
                translateButton.isEnabled = false
                translateButton.text = "Сначала скачайте модель"
                resultText.text = "Для перевода с ${languages[sourceIndex].second} на ${languages[targetIndex].second} необходимо скачать языковую модель (требуется Wi-Fi)"
                resultText.visibility = View.VISIBLE
            }
        }
    }

    private fun performTranslation() {
        val languages = TranslationService.SUPPORTED_LANGUAGES
        val sourceIndex = sourceLanguageSpinner.selectedItemPosition
        val targetIndex = targetLanguageSpinner.selectedItemPosition

        if (sourceIndex < 0 || targetIndex < 0) return

        val sourceLanguage = languages[sourceIndex].first
        val targetLanguage = languages[targetIndex].first

        lifecycleScope.launch {
            setLoading(true)

            val result = translationService.translate(selectedText, sourceLanguage, targetLanguage)

            setLoading(false)

            when (result) {
                is TranslationResult.Success -> {
                    resultText.text = result.translatedText
                    resultText.visibility = View.VISIBLE
                    onTranslationComplete?.invoke(result.translatedText)
                }
                is TranslationResult.ModelNotDownloaded -> {
                    resultText.text = "Языковая модель не загружена. Нажмите 'Скачать модель'"
                    resultText.visibility = View.VISIBLE
                    downloadButton.visibility = View.VISIBLE
                    downloadButton.isEnabled = true
                }
                is TranslationResult.Error -> {
                    resultText.text = "Ошибка перевода: ${result.message}"
                    resultText.visibility = View.VISIBLE
                }
                TranslationResult.Downloading -> {}
            }
        }
    }

    private fun downloadModel() {
        val languages = TranslationService.SUPPORTED_LANGUAGES
        val sourceIndex = sourceLanguageSpinner.selectedItemPosition
        val targetIndex = targetLanguageSpinner.selectedItemPosition

        if (sourceIndex < 0 || targetIndex < 0) return

        val sourceLanguage = languages[sourceIndex].first
        val targetLanguage = languages[targetIndex].first
        val sourceName = languages[sourceIndex].second
        val targetName = languages[targetIndex].second

        // Показываем подтверждение о скачивании
        AlertDialog.Builder(requireContext())
            .setTitle("Скачать языковую модель")
            .setMessage("Для перевода с $sourceName на $targetName необходимо скачать языковую модель (размер ~50 МБ). Рекомендуется использовать Wi-Fi. Продолжить?")
            .setPositiveButton("Скачать") { _, _ ->
                performDownload(sourceLanguage, targetLanguage)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performDownload(sourceLanguage: String, targetLanguage: String) {
        lifecycleScope.launch {
            downloadButton.isEnabled = false
            translateButton.isEnabled = false
            progressBar.visibility = View.VISIBLE
            loadingText.visibility = View.VISIBLE
            loadingText.text = "Скачивание модели... 0%"

            try {
                val success = translationService.downloadModel(sourceLanguage, targetLanguage)

                progressBar.visibility = View.GONE
                loadingText.visibility = View.GONE

                if (success) {
                    downloadButton.visibility = View.GONE
                    translateButton.isEnabled = true
                    translateButton.text = "Перевести"
                    resultText.text = "Модель успешно загружена. Теперь можно перевести текст."
                    resultText.visibility = View.VISIBLE
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
        translateButton.isEnabled = !isLoading
        sourceLanguageSpinner.isEnabled = !isLoading
        targetLanguageSpinner.isEnabled = !isLoading
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        loadingText.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    fun setOnTranslationComplete(callback: (String) -> Unit) {
        onTranslationComplete = callback
    }

    override fun onDestroyView() {
        super.onDestroyView()
        translationService.close()
    }
}