package com.eink.boardgames

import android.content.Context
import android.view.View

/**
 * Lớp nền cho cả ba loại bàn cờ. Tối ưu cho màn e-ink:
 * không animation, vẽ lại toàn bộ khi có thay đổi.
 */
abstract class GameView(context: Context) : View(context) {

    /** Callback cập nhật dòng trạng thái phía trên. */
    var onStatus: ((String) -> Unit)? = null

    /** 1 = Dễ, 2 = Vừa, 3 = Khó. */
    var aiLevel: Int = 2

    abstract fun newGame()

    abstract fun undo()

    /** Bỏ lượt (chỉ dùng cho cờ vây). */
    open fun pass() {}

    open fun supportsPass(): Boolean = false

    protected fun status(text: String) {
        onStatus?.invoke(text)
    }
}
