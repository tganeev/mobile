package org.readium.r2.testapp.translation

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.mlkit.nl.translate.TranslateLanguage
import org.readium.r2.testapp.R

class LanguageSettingsDialog : DialogFragment() {

    private var sourceLanguageCode: String = TranslateLanguage.ENGLISH
    private var targetLanguageCode: String = TranslateLanguage.RUSSIAN
    private var onLanguagesSelected: ((source: String, target: String) -> Unit)? = null

    companion object {
        fun newInstance(
            currentSource: String,
            currentTarget: String,
            onLanguagesSelected: (source: String, target: String) -> Unit
        ): LanguageSettingsDialog {
            val dialog = LanguageSettingsDialog()
            dialog.sourceLanguageCode = currentSource
            dialog.targetLanguageCode = currentTarget
            dialog.onLanguagesSelected = onLanguagesSelected
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, androidx.appcompat.R.style.Theme_AppCompat_DayNight_NoActionBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_language_settings, container, false)
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

        val sourceSpinner = view.findViewById<Spinner>(R.id.sourceLanguageSpinner)
        val targetSpinner = view.findViewById<Spinner>(R.id.targetLanguageSpinner)

        val languages = TranslationService.SUPPORTED_LANGUAGES
        val languageNames = languages.map { it.second }.toList()
        val languageCodes = languages.map { it.first }.toList()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languageNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        sourceSpinner.adapter = adapter
        targetSpinner.adapter = adapter

        val sourceIndex = languageCodes.indexOfFirst { it == sourceLanguageCode }
        val targetIndex = languageCodes.indexOfFirst { it == targetLanguageCode }

        if (sourceIndex >= 0) sourceSpinner.setSelection(sourceIndex)
        if (targetIndex >= 0) targetSpinner.setSelection(targetIndex)

        view.findViewById<View>(R.id.saveButton).setOnClickListener {
            val newSourceIndex = sourceSpinner.selectedItemPosition
            val newTargetIndex = targetSpinner.selectedItemPosition

            if (newSourceIndex >= 0 && newTargetIndex >= 0) {
                val newSource = languageCodes[newSourceIndex]
                val newTarget = languageCodes[newTargetIndex]
                onLanguagesSelected?.invoke(newSource, newTarget)
            }
            dismiss()
        }

        view.findViewById<View>(R.id.cancelButton).setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.8).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}