package org.readium.r2.testapp.vocabulary

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.model.Vocabulary
import org.readium.r2.testapp.databinding.FragmentVocabularyBinding

class VocabularyFragment : Fragment() {

    private var _binding: FragmentVocabularyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VocabularyViewModel by viewModels()
    private lateinit var adapter: VocabularyAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVocabularyBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = VocabularyAdapter(
            onItemLongClick = { word -> showDeleteConfirmDialog(word) }
        )
        binding.vocabularyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.vocabularyRecyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.searchWords(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    viewModel.loadAllWords()
                } else {
                    viewModel.searchWords(newText)
                }
                return true
            }
        })
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.words.collect { words ->
                adapter.submitList(words)
                binding.emptyView.visibility = if (words.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showDeleteConfirmDialog(word: Vocabulary) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить слово")
            .setMessage("Удалить слово \"${word.sourceWord}\" из банка слов?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteWord(word.id)
                    Snackbar.make(binding.root, "Слово удалено", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_vocabulary, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_all -> {
                confirmDeleteAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmDeleteAll() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить все слова")
            .setMessage("Все слова будут удалены без возможности восстановления")
            .setPositiveButton("Удалить все") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteAllWords()
                    Snackbar.make(binding.root, "Все слова удалены", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}