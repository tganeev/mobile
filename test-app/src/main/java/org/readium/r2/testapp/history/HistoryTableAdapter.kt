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
        dpToPx(60)
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

        // ===== 1. ИТОГО (над заголовками) =====
        fixedContainer.addView(createTotalFixedRow("ИТОГО (стр.)"))
        dynamicContainer.addView(createTotalDynamicRow(data.dates, data.totalsByDate, data.totalPagesSum) { value ->
            value.toString()
        })

        fixedContainer.addView(createTotalFixedRow("ИТОГО (часы)"))
        dynamicContainer.addView(createTotalDynamicRow(data.dates, data.totalTimeByDate, data.totalHoursSum) { value ->
            formatHoursShort(value)
        })

        // ===== 2. ЗАГОЛОВКИ ТАБЛИЦЫ =====
        fixedContainer.addView(createFixedHeaderRow())
        dynamicContainer.addView(createDynamicHeaderRow(data.dates))

        // ===== 3. ДАННЫЕ КНИГ =====
        data.books.forEach { book ->
            fixedContainer.addView(createFixedRow(book))
            dynamicContainer.addView(createDynamicRow(book, data.dates))
        }

        alignRowHeights(fixedContainer, dynamicContainer)
    }

    private fun createFixedHeaderRow(): LinearLayout {
        return LinearLayout(fixedColumnLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(createFixedCell("Название", 200, isHeader = true, gravity = Gravity.CENTER))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                rowHeightPx
            )
        }
    }

    private fun createDynamicHeaderRow(dates: List<LocalDate>): LinearLayout {
        return LinearLayout(dynamicColumnsLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(createDynamicCell("Автор", 150, isHeader = true, gravity = Gravity.CENTER))
            addView(createDynamicCell("Статус", 100, isHeader = true, gravity = Gravity.CENTER))
            addView(createDynamicCell("Категория", 80, isHeader = true, gravity = Gravity.CENTER))
            dates.forEach { date ->
                addView(createDynamicCell(dateFormatter.format(date), 80, isHeader = true, gravity = Gravity.CENTER))
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
            addView(createFixedCell(title, 200, isTotal = true, gravity = Gravity.CENTER_VERTICAL or Gravity.START, backgroundTransparent = true, textBlack = true))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                rowHeightPx
            )
        }
    }

    private fun createTotalDynamicRow(
        dates: List<LocalDate>,
        totalsByDate: Map<LocalDate, Double>,
        totalSum: Double,
        formatter: (Double) -> String
    ): LinearLayout {
        return LinearLayout(dynamicColumnsLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL

            addView(createMergedCell(
                text = formatter(totalSum),
                widthDp = 150 + 100 + 80,
                isTotal = true,
                gravity = Gravity.CENTER_VERTICAL or Gravity.START,
                backgroundTransparent = true,
                textBlack = true
            ))

            dates.forEach { date ->
                val value = totalsByDate[date] ?: 0.0
                val displayText = if (value > 0) formatter(value) else "—"
                addView(createDynamicCell(displayText, 80, isTotal = true, gravity = Gravity.CENTER, backgroundTransparent = true, textBlack = true))
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                rowHeightPx
            )
        }
    }

    private fun createMergedCell(
        text: String,
        widthDp: Int,
        isTotal: Boolean = false,
        gravity: Int = Gravity.CENTER,
        backgroundTransparent: Boolean = false,
        textBlack: Boolean = false
    ): TextView {
        return TextView(dynamicColumnsLayout!!.context).apply {
            this.text = text
            setPadding(12, 12, 12, 12)
            textSize = 14f
            this.gravity = gravity
            maxLines = 1

            if (backgroundTransparent) {
                setBackgroundColor(Color.TRANSPARENT)
                if (textBlack) setTextColor(Color.BLACK)
            } else if (isTotal) {
                setBackgroundColor(Color.parseColor("#757575"))
                setTextColor(Color.WHITE)
            }

            if (isTotal && !textBlack && !backgroundTransparent) {
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            layoutParams = LinearLayout.LayoutParams(dpToPx(widthDp), rowHeightPx)
        }
    }

    private fun createFixedRow(book: BookProgress): LinearLayout {
        return LinearLayout(fixedColumnLayout!!.context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(createFixedCell(
                text = book.title,
                widthDp = 200,
                isClickable = true,
                onClick = { onBookClick(book.bookId) },
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
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

            addView(createDynamicCell(book.author, 150, gravity = Gravity.CENTER_VERTICAL or Gravity.START))
            addView(createDynamicCell(book.status, 100, gravity = Gravity.CENTER))
            addView(createDynamicCell(book.category, 80, gravity = Gravity.CENTER))

            dates.forEach { date ->
                val pages = book.dailyProgress[date] ?: 0
                val hours = book.dailyTime[date] ?: 0.0
                val displayText = if (pages > 0) {
                    "${pages} стр.\n${formatHoursShort(hours)}"
                } else {
                    "—"
                }
                addView(createDynamicCell(displayText, 80, isMultiLine = true, gravity = Gravity.CENTER))
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
        isTotal: Boolean = false,
        gravity: Int = Gravity.CENTER,
        backgroundTransparent: Boolean = false,
        textBlack: Boolean = false
    ): TextView {
        return TextView(fixedColumnLayout!!.context).apply {
            this.text = text
            setPadding(12, 12, 12, 12)
            textSize = 14f
            this.gravity = gravity
            maxLines = 1

            when {
                isHeader -> {
                    setTextColor(Color.WHITE)
                    setBackgroundColor(resources.getColor(R.color.purple_500, null))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                backgroundTransparent -> {
                    setBackgroundColor(Color.TRANSPARENT)
                    if (textBlack) setTextColor(Color.BLACK)
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
        isMultiLine: Boolean = false,
        gravity: Int = Gravity.CENTER,
        backgroundTransparent: Boolean = false,
        textBlack: Boolean = false
    ): TextView {
        return TextView(dynamicColumnsLayout!!.context).apply {
            this.text = text
            setPadding(12, 12, 12, 12)
            textSize = if (isMultiLine) 11f else 13f
            this.gravity = gravity
            maxLines = if (isMultiLine) 2 else 1

            when {
                isHeader -> {
                    setTextColor(Color.WHITE)
                    setBackgroundColor(resources.getColor(R.color.purple_500, null))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                backgroundTransparent -> {
                    setBackgroundColor(Color.TRANSPARENT)
                    if (textBlack) setTextColor(Color.BLACK)
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

    private fun formatHoursShort(hours: Double): String {
        val totalMinutes = (hours * 60).toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h > 0 && m > 0 -> "${h}ч ${m} мин"
            h > 0 -> "${h} ч"
            m > 0 -> "${m} мин"
            else -> "0 мин"
        }
    }

    private fun alignRowHeights(fixedContainer: LinearLayout, dynamicContainer: LinearLayout) {
        fixedContainer.post {
            val fixedChildren = fixedContainer.children.toList()
            val dynamicChildren = dynamicContainer.children.toList()

            val minSize = minOf(fixedChildren.size, dynamicChildren.size)
            for (i in 0 until minSize) {
                val fixedChild = fixedChildren[i] as? LinearLayout ?: continue
                val dynamicChild = dynamicChildren[i] as? LinearLayout ?: continue

                var maxHeight = rowHeightPx

                for (j in 0 until fixedChild.childCount) {
                    val view = fixedChild.getChildAt(j)
                    maxHeight = maxOf(maxHeight, view.height)
                }

                for (j in 0 until dynamicChild.childCount) {
                    val view = dynamicChild.getChildAt(j)
                    maxHeight = maxOf(maxHeight, view.height)
                }

                for (j in 0 until fixedChild.childCount) {
                    fixedChild.getChildAt(j).layoutParams.height = maxHeight
                }
                for (j in 0 until dynamicChild.childCount) {
                    dynamicChild.getChildAt(j).layoutParams.height = maxHeight
                }

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