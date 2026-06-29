package com.eink.boardgames.xiangqi

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
 * Cờ tướng. Người chơi cầm quân Đỏ (phía dưới), máy cầm quân Đen.
 * Quân Đỏ vẽ vòng tròn kép để phân biệt với quân Đen trên màn e-ink.
 */
class XiangqiView(context: Context) : GameView(context) {

    private val board = XiangqiBoard()
    private var selected = -1
    private var targets = listOf<XMove>()
    private var aiThinking = false
    private var gameOver = false

    private var step = 0f
    private var left = 0f
    private var top = 0f

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val hint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(90, 90, 90) }
    private val sel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 7f
    }

    override fun newGame() {
        board.reset()
        selected = -1; targets = emptyList()
        aiThinking = false; gameOver = false
        status("Lượt của bạn (Đỏ)")
        invalidate()
    }

    override fun undo() {
        if (aiThinking) return
        if (board.canUndo()) board.undoMove()
        if (board.canUndo() && !board.redToMove) board.undoMove()
        selected = -1; targets = emptyList(); gameOver = false
        status("Lượt của bạn (Đỏ)")
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val margin = 0.08f
        step = min((w * (1 - margin)) / 8f, (h * (1 - margin)) / 9f)
        left = (w - step * 8) / 2f
        top = (h - step * 9) / 2f
        text.textSize = step * 0.62f
    }

    private fun x(c: Int) = left + c * step
    private fun y(r: Int) = top + r * step

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        // Đường ngang
        for (r in 0..9) canvas.drawLine(x(0), y(r), x(8), y(r), line)
        // Đường dọc (chừa sông ở các cột giữa)
        for (c in 0..8) {
            if (c == 0 || c == 8) {
                canvas.drawLine(x(c), y(0), x(c), y(9), line)
            } else {
                canvas.drawLine(x(c), y(0), x(c), y(4), line)
                canvas.drawLine(x(c), y(5), x(c), y(9), line)
            }
        }
        // Cung (đường chéo)
        canvas.drawLine(x(3), y(0), x(5), y(2), line)
        canvas.drawLine(x(5), y(0), x(3), y(2), line)
        canvas.drawLine(x(3), y(7), x(5), y(9), line)
        canvas.drawLine(x(5), y(7), x(3), y(9), line)

        // Gợi ý nước đi
        for (m in targets) {
            val r = m.to / 9; val c = m.to % 9
            if (board.pieceAt(m.to) != ' ') {
                canvas.drawCircle(x(c), y(r), step * 0.46f, sel)
            } else {
                canvas.drawCircle(x(c), y(r), step * 0.14f, hint)
            }
        }
        // Ô đang chọn
        if (selected >= 0) {
            val r = selected / 9; val c = selected % 9
            canvas.drawCircle(x(c), y(r), step * 0.46f, sel)
        }
        // Quân cờ
        for (i in 0 until 90) {
            val p = board.pieceAt(i)
            if (p == ' ') continue
            drawPiece(canvas, i, p)
        }
    }

    private fun drawPiece(canvas: Canvas, sq: Int, p: Char) {
        val r = sq / 9; val c = sq % 9
        val cx = x(c); val cy = y(r)
        val radius = step * 0.42f
        canvas.drawCircle(cx, cy, radius, fill)
        canvas.drawCircle(cx, cy, radius, ring)
        val red = p in 'A'..'Z'
        if (red) canvas.drawCircle(cx, cy, radius * 0.82f, ring) // vòng kép = quân Đỏ
        val ch = charFor(p)
        val ty = cy - (text.descent() + text.ascent()) / 2
        canvas.drawText(ch, cx, ty, text)
    }

    private fun charFor(p: Char): String = when (p) {
        'K' -> "帥"; 'A' -> "仕"; 'E' -> "相"; 'H' -> "傌"; 'R' -> "俥"; 'C' -> "炮"; 'S' -> "兵"
        'k' -> "將"; 'a' -> "士"; 'e' -> "象"; 'h' -> "馬"; 'r' -> "車"; 'c' -> "砲"; 's' -> "卒"
        else -> "?"
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true
        if (aiThinking || gameOver) return true
        val sq = nearest(event.x, event.y) ?: return true

        val p = board.pieceAt(sq)
        if (selected == -1) {
            if (p != ' ' && p in 'A'..'Z' && board.redToMove) {
                selected = sq
                targets = board.legalMoves().filter { it.from == sq }
                invalidate()
            }
            return true
        }

        val move = targets.firstOrNull { it.to == sq }
        if (move != null) {
            board.makeMove(move)
            selected = -1; targets = emptyList()
            invalidate()
            if (checkEnd(forRed = false)) return true
            triggerAI()
        } else if (p != ' ' && p in 'A'..'Z') {
            selected = sq
            targets = board.legalMoves().filter { it.from == sq }
            invalidate()
        } else {
            selected = -1; targets = emptyList(); invalidate()
        }
        return true
    }

    private fun nearest(px: Float, py: Float): Int? {
        val c = Math.round((px - left) / step)
        val r = Math.round((py - top) / step)
        if (r !in 0..9 || c !in 0..8) return null
        if (abs(px - x(c)) > step * 0.5f || abs(py - y(r)) > step * 0.5f) return null
        return r * 9 + c
    }

    private fun triggerAI() {
        aiThinking = true
        status("Máy đang nghĩ…")
        val depth = when (aiLevel) {
            1 -> 1
            3 -> 3
            else -> 2
        }
        thread {
            val m = XiangqiAI.bestMove(board, depth)
            post {
                if (m != null) board.makeMove(m)
                aiThinking = false
                invalidate()
                checkEnd(forRed = true)
            }
        }
    }

    private fun checkEnd(forRed: Boolean): Boolean {
        val moves = board.legalMoves()
        if (moves.isEmpty()) {
            gameOver = true
            status(
                if (board.inCheck(forRed)) {
                    if (forRed) "Chiếu bí! Bạn thua." else "Chiếu bí! Bạn thắng!"
                } else "Hết nước đi - bạn thua."
            )
            invalidate()
            return true
        }
        status(if (board.inCheck(forRed)) "Chiếu tướng! Lượt của bạn" else "Lượt của bạn (Đỏ)")
        return false
    }
}
