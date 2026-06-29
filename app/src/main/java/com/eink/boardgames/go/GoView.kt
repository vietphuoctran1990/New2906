package com.eink.boardgames.go

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import com.eink.boardgames.GameView
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.min

/**
 * Cờ vây. Người chơi cầm Đen (đi trước), máy cầm Trắng.
 * Đặt quân bằng cách chạm vào giao điểm. Nút "Bỏ lượt" để pass;
 * hai lần pass liên tiếp thì kết thúc và tính điểm.
 */
class GoView(context: Context, private val size: Int) : GameView(context) {

    private val board = GoBoard(size)
    private val komi = if (size <= 9) 5.5 else 6.5
    private var aiThinking = false
    private var gameOver = false

    private var step = 0f
    private var left = 0f
    private var top = 0f

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2.5f
    }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val black = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2.5f
    }
    private val markB = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val markW = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f
    }

    override fun supportsPass(): Boolean = true

    override fun newGame() {
        board.reset()
        aiThinking = false; gameOver = false
        status("Lượt của bạn (Đen)")
        invalidate()
    }

    override fun undo() {
        if (aiThinking) return
        if (board.canUndo()) board.undo()
        if (board.canUndo() && board.toMove != 1) board.undo()
        gameOver = false
        status("Lượt của bạn (Đen)")
        invalidate()
    }

    override fun pass() {
        if (aiThinking || gameOver) return
        board.pass(1)
        if (board.consecutivePasses >= 2) { endGame(); return }
        triggerAI()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val pad = 0.06f
        val usable = min(w, h) * (1 - pad)
        step = usable / size
        left = (w - step * (size - 1)) / 2f
        top = (h - step * (size - 1)) / 2f
    }

    private fun x(c: Int) = left + c * step
    private fun y(r: Int) = top + r * step

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        for (i in 0 until size) {
            canvas.drawLine(x(0), y(i), x(size - 1), y(i), line)
            canvas.drawLine(x(i), y(0), x(i), y(size - 1), line)
        }
        // Sao (hoshi)
        for (p in starPoints()) {
            canvas.drawCircle(x(p % size), y(p / size), step * 0.08f, starPaint)
        }
        // Quân
        val radius = step * 0.46f
        for (i in 0 until size * size) {
            when (board.cells[i]) {
                1 -> canvas.drawCircle(x(i % size), y(i / size), radius, black)
                2 -> {
                    canvas.drawCircle(x(i % size), y(i / size), radius, white)
                    canvas.drawCircle(x(i % size), y(i / size), radius, ring)
                }
            }
        }
        // Đánh dấu nước vừa đi
        val lm = board.lastMove
        if (lm >= 0) {
            val c = lm % size; val r = lm / size
            val paint = if (board.cells[lm] == 1) markB else markW
            canvas.drawCircle(x(c), y(r), radius * 0.45f, paint)
        }
    }

    private fun starPoints(): List<Int> {
        val edge = if (size >= 13) 3 else 2
        val mid = size / 2
        val coords = when {
            size >= 13 -> listOf(edge, mid, size - 1 - edge)
            size == 9 -> listOf(2, mid, size - 1 - 2)
            else -> listOf(mid)
        }
        val pts = ArrayList<Int>()
        for (r in coords) for (c in coords) {
            if (size % 2 == 0 && (r == mid || c == mid)) continue
            pts.add(r * size + c)
        }
        return pts
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true
        if (aiThinking || gameOver) return true
        val idx = nearest(event.x, event.y) ?: return true
        if (board.toMove != 1) return true
        if (!board.isLegal(idx, 1)) return true
        board.play(idx, 1)
        invalidate()
        triggerAI()
        return true
    }

    private fun nearest(px: Float, py: Float): Int? {
        val c = Math.round((px - left) / step)
        val r = Math.round((py - top) / step)
        if (r !in 0 until size || c !in 0 until size) return null
        if (abs(px - x(c)) > step * 0.5f || abs(py - y(r)) > step * 0.5f) return null
        return r * size + c
    }

    private fun triggerAI() {
        aiThinking = true
        status("Máy đang nghĩ…")
        thread {
            val move = GoAI.chooseMove(board, komi)
            post {
                if (move != null) board.play(move, 2) else board.pass(2)
                aiThinking = false
                invalidate()
                if (board.consecutivePasses >= 2) endGame()
                else status(if (move == null) "Máy bỏ lượt. Lượt của bạn" else "Lượt của bạn (Đen)")
            }
        }
    }

    private fun endGame() {
        gameOver = true
        val (b, w) = board.score(komi)
        val result = when {
            b > w -> "Bạn (Đen) thắng!"
            w > b -> "Máy (Trắng) thắng."
            else -> "Hòa."
        }
        status("Đen %.1f - Trắng %.1f. %s".format(b, w, result))
        invalidate()
    }
}
