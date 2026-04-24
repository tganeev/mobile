package org.readium.r2.testapp.history

import android.graphics.Color
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

        // 1. СНАЧАЛА ИТОГО (страницы) - серые строки В САМОМ ВЕРХУ
        fixedContainer.addView(createTotalFixedRow("ИТОГО (стр.)"))
        dynamicContainer.addView(createTotalDynamicRow(data.dates, data.totals) { value ->
            value.toString()
        })

        // 2. ИТОГО (часы) - вторая серая строка
        fixedContainer.addView(createTotalFixedRow("ИТОГО (часы)"))
        dynamicContainer.addView(createTotalDynamicRow(data.dates, data.totalTime) { value ->
            formatHours(value)
        })

        // 3. ЗАТЕМ Заголовки (синие)
        fixedContainer.addView(createFixedHeaderRow())
        dynamicContainer.addView(createDynamicHeaderRow(data.dates))

        // 4. ПОТОМ Данные книг (с двухстрочным отображением страницы/время)
        data.books.forEach { book ->
            fixedContainer.addView(createFixedRow(book))
            dynamicContainer.addView(createDynamicRow(book, data.dates))
        }

        alignRowHeights(fixedContainer, dynamicContainer)
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
                addView(createDynamicCell(displayText, 80, isTotal = true, isMultiLine = false))
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
            gravity = Gravity.CENTER
            maxLines = 1

            when {
                isHeader -> {
                    setTextColor(Color.WHITE)
                    setBackgroundColor(resources.getColor(R.color.purple_500, null))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                isTotal -> {
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#757575"))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                else -> {
                    setBackgroundColor(Color.TRANSPARENT)
                }
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
            textSize = if (isMultiLine) 11f else 13f
            gravity = Gravity.CENTER
            maxLines = if (isMultiLine) 2 else 1

            when {
                isHeader -> {
                    setTextColor(Color.WHITE)
                    setBackgroundColor(resources.getColor(R.color.purple_500, null))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                isTotal -> {
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#757575"))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                else -> {
                    setBackgroundColor(Color.TRANSPARENT)
                }
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

    private fun alignRowHeights(fixedContainer: LinearLayout, dynamicContainer: LinearLayout) {
        fixedContainer.post {
            val fixedChildren = fixedContainer.children.toList()
            val dynamicChildren = dynamicContainer.children.toList()

            val minSize = minOf(fixedChildren.size, dynamicChildren.size)
            for (i in 0 until minSize) {
                val fixedChild = fixedChildren[i]
                val dynamicChild = dynamicChildren[i]

                val maxHeight = maxOf(fixedChild.height, dynamicChild.height, rowHeightPx)
                fixedChild.layoutParams.height = maxHeight
                dynamicChild.layoutParams.height = maxHeight
                fixedChild.requestLayout()
                dynamicChild.requestLayout()
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * (fixedColumnLayout?.context?.resources?.displayMetrics?.density ?: 1f)).toInt()
    }
}