package org.readium.r2.testapp.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.testapp.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HistoryTableAdapter(
    private val onBookClick: (Long) -> Unit
) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM")
    private var data: HistoryTableData? = null
    private var fixedColumnsLayout: LinearLayout? = null
    private var dynamicColumnsLayout: LinearLayout? = null

    fun setData(
        data: HistoryTableData,
        fixedContainer: LinearLayout,
        dynamicContainer: LinearLayout
    ) {
        this.data = data
        this.fixedColumnsLayout = fixedContainer
        this.dynamicColumnsLayout = dynamicContainer
        render()
    }

    private fun render() {
        val data = this.data ?: return
        val fixedContainer = fixedColumnsLayout ?: return
        val dynamicContainer = dynamicColumnsLayout ?: return

        // Очищаем контейнеры
        fixedContainer.removeAllViews()
        dynamicContainer.removeAllViews()

        // Создаём заголовки фиксированных колонок
        val headerFixed = createHeaderFixedRow()
        fixedContainer.addView(headerFixed)

        // Создаём заголовки динамических колонок
        val headerDynamic = createHeaderDynamicRow(data.dates)
        dynamicContainer.addView(headerDynamic)

        // Создаём строки для каждой книги
        data.books.forEach { book ->
            val rowFixed = createBookFixedRow(book)
            val rowDynamic = createBookDynamicRow(book, data.dates)
            fixedContainer.addView(rowFixed)
            dynamicContainer.addView(rowDynamic)
        }

        // Создаём строку ИТОГО
        val totalFixed = createTotalFixedRow()
        val totalDynamic = createTotalDynamicRow(data.dates, data.totals)
        fixedContainer.addView(totalFixed)
        dynamicContainer.addView(totalDynamic)

        // Устанавливаем одинаковую высоту для соответствующих строк
        matchRowHeights(fixedContainer, dynamicContainer)
    }

    private fun createHeaderFixedRow(): LinearLayout {
        return LinearLayout(fixedColumnsLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            addView(createHeaderCell("Название", 200))
            addView(createHeaderCell("Автор", 150))
            addView(createHeaderCell("Статус", 100))
            addView(createHeaderCell("Категория", 80))
        }
    }

    private fun createHeaderDynamicRow(dates: List<LocalDate>): LinearLayout {
        return LinearLayout(dynamicColumnsLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            dates.forEach { date ->
                addView(createHeaderCell(dateFormatter.format(date), 60))
            }
        }
    }

    private fun createBookFixedRow(book: BookProgress): LinearLayout {
        return LinearLayout(fixedColumnsLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            addView(createCell(book.title, 200, true) { onBookClick(book.bookId) })
            addView(createCell(book.author, 150, false))
            addView(createCell(book.status, 100, false))
            addView(createCell(book.category, 80, false))
        }
    }

    private fun createBookDynamicRow(book: BookProgress, dates: List<LocalDate>): LinearLayout {
        return LinearLayout(dynamicColumnsLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            dates.forEach { date ->
                val pages = book.dailyProgress[date] ?: 0
                addView(createCell(pages.toString(), 60, false))
            }
        }
    }

    private fun createTotalFixedRow(): LinearLayout {
        return LinearLayout(fixedColumnsLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            addView(createTotalCell("ИТОГО", 200))
            addView(createTotalCell("", 150))
            addView(createTotalCell("", 100))
            addView(createTotalCell("", 80))
        }
    }

    private fun createTotalDynamicRow(dates: List<LocalDate>, totals: Map<LocalDate, Int>): LinearLayout {
        return LinearLayout(dynamicColumnsLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            dates.forEach { date ->
                val total = totals[date] ?: 0
                addView(createTotalCell(total.toString(), 60))
            }
        }
    }

    private fun createHeaderCell(text: String, widthDp: Int): TextView {
        return TextView(fixedColumnsLayout!!.context).apply {
            this.text = text
            setPadding(12, 12, 12, 12)
            setTextColor(resources.getColor(android.R.color.white, null))
            setBackgroundColor(resources.getColor(R.color.purple_500, null))
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(dpToPx(widthDp), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun createCell(text: String, widthDp: Int, isClickable: Boolean = false, onClick: (() -> Unit)? = null): TextView {
        return TextView(fixedColumnsLayout!!.context).apply {
            this.text = text
            setPadding(12, 12, 12, 12)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(dpToPx(widthDp), LinearLayout.LayoutParams.WRAP_CONTENT)

            if (isClickable && onClick != null) {
                setOnClickListener { onClick() }
                setTextColor(resources.getColor(R.color.purple_500, null))
            }
        }
    }

    private fun createTotalCell(text: String, widthDp: Int): TextView {
        return TextView(fixedColumnsLayout!!.context).apply {
            this.text = text
            setPadding(12, 12, 12, 12)
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            layoutParams = LinearLayout.LayoutParams(dpToPx(widthDp), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun matchRowHeights(fixedContainer: LinearLayout, dynamicContainer: LinearLayout) {
        val fixedChildren = fixedContainer.children.toList()
        val dynamicChildren = dynamicContainer.children.toList()

        val minSize = minOf(fixedChildren.size, dynamicChildren.size)
        for (i in 0 until minSize) {
            val fixedChild = fixedChildren[i]
            val dynamicChild = dynamicChildren[i]

            fixedChild.post {
                val height = maxOf(fixedChild.height, dynamicChild.height)
                fixedChild.layoutParams.height = height
                dynamicChild.layoutParams.height = height
                fixedChild.requestLayout()
                dynamicChild.requestLayout()
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * fixedColumnsLayout!!.context.resources.displayMetrics.density).toInt()
    }
}