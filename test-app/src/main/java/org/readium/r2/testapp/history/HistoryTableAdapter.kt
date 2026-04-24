package org.readium.r2.testapp.history

import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import org.readium.r2.testapp.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HistoryTableAdapter(
    private val onBookClick: (Long) -> Unit
) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM")
    private var data: HistoryTableData? = null
    private var fixedColumnLayout: LinearLayout? = null
    private var dynamicColumnsLayout: LinearLayout? = null

    private val rowHeightPx: Int by lazy {
        dpToPx(48)
    }

    fun setData(
        data: HistoryTableData,
        fixedContainer: LinearLayout,
        dynamicContainer: LinearLayout
    ) {
        this.data = data
        this.fixedColumnLayout = fixedContainer
        this.dynamicColumnsLayout = dynamicContainer
        render()
    }

    private fun render() {
        val data = this.data ?: return
        val fixedContainer = fixedColumnLayout ?: return
        val dynamicContainer = dynamicColumnsLayout ?: return

        fixedContainer.removeAllViews()
        dynamicContainer.removeAllViews()

        val fixedHeader = createFixedHeaderRow()
        val dynamicHeader = createDynamicHeaderRow(data.dates)
        fixedContainer.addView(fixedHeader)
        dynamicContainer.addView(dynamicHeader)

        data.books.forEach { book ->
            val fixedRow = createFixedRow(book)
            val dynamicRow = createDynamicRow(book, data.dates)
            fixedContainer.addView(fixedRow)
            dynamicContainer.addView(dynamicRow)
        }

        val totalPagesFixed = createTotalFixedRow("ИТОГО (стр.)")
        val totalPagesDynamic = createTotalDynamicRow(data.dates, data.totals) { value ->
            value.toString()
        }
        fixedContainer.addView(totalPagesFixed)
        dynamicContainer.addView(totalPagesDynamic)

        val totalTimeFixed = createTotalFixedRow("ИТОГО (часы)")
        val totalTimeDynamic = createTotalDynamicRow(data.dates, data.totalTime) { value ->
            formatHours(value)
        }
        fixedContainer.addView(totalTimeFixed)
        dynamicContainer.addView(totalTimeDynamic)

        alignAllRows(fixedContainer, dynamicContainer)
    }

    private fun createFixedHeaderRow(): LinearLayout {
        return LinearLayout(fixedColumnLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(createFixedCell("Название", 200, isHeader = true))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                rowHeightPx
            )
        }
    }

    private fun createDynamicHeaderRow(dates: List<LocalDate>): LinearLayout {
        return LinearLayout(dynamicColumnsLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL

            addView(createDynamicCell("Автор", 150, isHeader = true))
            addView(createDynamicCell("Статус", 100, isHeader = true))
            addView(createDynamicCell("Категория", 80, isHeader = true))

            dates.forEach { date ->
                addView(createDynamicCell(dateFormatter.format(date), 80, isHeader = true))
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                rowHeightPx
            )
        }
    }

    private fun createFixedRow(book: BookProgress): LinearLayout {
        return LinearLayout(fixedColumnLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(createFixedCell(
                text = book.title,
                widthDp = 200,
                isClickable = true,
                onClick = { onBookClick(book.bookId) }
            ))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                rowHeightPx
            )
        }
    }

    private fun createDynamicRow(book: BookProgress, dates: List<LocalDate>): LinearLayout {
        return LinearLayout(dynamicColumnsLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL

            addView(createDynamicCell(book.author, 150))
            addView(createDynamicCell(book.status, 100))
            addView(createDynamicCell(book.category, 80))

            dates.forEach { date ->
                val pages = book.dailyProgress[date] ?: 0
                val hours = book.dailyTime[date] ?: 0.0
                val displayText = if (pages > 0) {
                    "$pages стр.\n${formatHours(hours)}"
                } else {
                    "—"
                }
                addView(createDynamicCell(displayText, 80, isMultiLine = true))
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                rowHeightPx
            )
        }
    }

    private fun createTotalFixedRow(title: String): LinearLayout {
        return LinearLayout(fixedColumnLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(createFixedCell(title, 200, isTotal = true))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                rowHeightPx
            )
        }
    }

    private fun <T : Number> createTotalDynamicRow(
        dates: List<LocalDate>,
        totals: Map<LocalDate, T>,
        formatter: (Double) -> String
    ): LinearLayout {
        return LinearLayout(dynamicColumnsLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL

            addView(createDynamicCell("", 150, isTotal = true))
            addView(createDynamicCell("", 100, isTotal = true))
            addView(createDynamicCell("", 80, isTotal = true))

            dates.forEach { date ->
                val value = totals[date]?.toDouble() ?: 0.0
                val displayText = if (value > 0) formatter(value) else "—"
                addView(createDynamicCell(displayText, 80, isTotal = true, isMultiLine = true))
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                rowHeightPx
            )
        }
    }

    private fun createFixedCell(
        text: String,
        widthDp: Int,
        isClickable: Boolean = false,
        onClick: (() -> Unit)? = null,
        isHeader: Boolean = false,
        isTotal: Boolean = false
    ): TextView {
        return TextView(fixedColumnLayout!!.context).apply {
            this.text = text
            setPadding(12, 12, 12, 12)
            textSize = 14f
            // ВСЕГДА ЦЕНТРИРУЕМ
            gravity = Gravity.CENTER

            if (isHeader) {
                setTextColor(resources.getColor(android.R.color.white, null))
                setBackgroundColor(resources.getColor(R.color.purple_500, null))
                setTypeface(null, android.graphics.Typeface.BOLD)
            } else if (isTotal) {
                setTypeface(null, android.graphics.Typeface.BOLD)
                setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            }

            if (isClickable && onClick != null) {
                setOnClickListener { onClick() }
                setTextColor(resources.getColor(R.color.purple_500, null))
            }

            layoutParams = LinearLayout.LayoutParams(dpToPx(widthDp), rowHeightPx)
        }
    }

    private fun createDynamicCell(
        text: String,
        widthDp: Int,
        isClickable: Boolean = false,
        onClick: (() -> Unit)? = null,
        isHeader: Boolean = false,
        isTotal: Boolean = false,
        isMultiLine: Boolean = false
    ): TextView {
        return TextView(dynamicColumnsLayout!!.context).apply {
            this.text = text
            setPadding(12, 12, 12, 12)
            textSize = if (isMultiLine) 11f else 14f
            // ВСЕГДА ЦЕНТРИРУЕМ
            gravity = Gravity.CENTER
            maxLines = if (isMultiLine) 2 else 1

            if (isHeader) {
                setTextColor(resources.getColor(android.R.color.white, null))
                setBackgroundColor(resources.getColor(R.color.purple_500, null))
                setTypeface(null, android.graphics.Typeface.BOLD)
            } else if (isTotal) {
                setTypeface(null, android.graphics.Typeface.BOLD)
                setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            }

            if (isClickable && onClick != null) {
                setOnClickListener { onClick() }
                setTextColor(resources.getColor(R.color.purple_500, null))
            }

            layoutParams = LinearLayout.LayoutParams(dpToPx(widthDp), rowHeightPx)
        }
    }

    private fun formatHours(hours: Double): String {
        val totalMinutes = (hours * 60).toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return if (h > 0) "${h}ч ${m}мин" else "${m}мин"
    }

    private fun alignAllRows(fixedContainer: LinearLayout, dynamicContainer: LinearLayout) {
        val fixedChildren = fixedContainer.children.toList()
        val dynamicChildren = dynamicContainer.children.toList()

        val minSize = minOf(fixedChildren.size, dynamicChildren.size)
        for (i in 0 until minSize) {
            val fixedChild = fixedChildren[i]
            val dynamicChild = dynamicChildren[i]

            fixedChild.layoutParams.height = rowHeightPx
            dynamicChild.layoutParams.height = rowHeightPx
            fixedChild.requestLayout()
            dynamicChild.requestLayout()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * (fixedColumnLayout?.context?.resources?.displayMetrics?.density ?: 1f)).toInt()
    }
}