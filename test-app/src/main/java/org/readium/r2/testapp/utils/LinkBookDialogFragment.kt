package org.readium.r2.testapp.utils

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.model.Book

class LinkBookDialogFragment : DialogFragment() {

    private var onLinkConfirmed: (() -> Unit)? = null
    private var onCreateNewConfirmed: (() -> Unit)? = null
    private var existingBook: Book? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        existingBook = arguments?.getSerializable(ARG_EXISTING_BOOK) as? Book

        val title = existingBook?.title ?: "книге"
        val author = existingBook?.author?.let { " ($it)" } ?: ""

        return AlertDialog.Builder(requireContext())
            .setTitle("Связать с историей?")
            .setMessage("Найдена существующая запись о книге «$title$author» с историей чтения. Связать с импортируемым файлом?")
            .setPositiveButton("Связать") { _, _ ->
                onLinkConfirmed?.invoke()
            }
            .setNegativeButton("Создать новую") { _, _ ->
                onCreateNewConfirmed?.invoke()
            }
            .setNeutralButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    fun setOnLinkConfirmed(callback: () -> Unit) {
        onLinkConfirmed = callback
    }

    fun setOnCreateNewConfirmed(callback: () -> Unit) {
        onCreateNewConfirmed = callback
    }

    companion object {
        private const val ARG_EXISTING_BOOK = "existing_book"

        fun newInstance(existingBook: Book): LinkBookDialogFragment {
            val fragment = LinkBookDialogFragment()
            val args = Bundle()
            args.putSerializable(ARG_EXISTING_BOOK, existingBook)
            fragment.arguments = args
            return fragment
        }
    }
}