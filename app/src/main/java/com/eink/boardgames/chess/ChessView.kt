package com.eink.boardgames.chess

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import com.eink.boardgames.GameView
import kotlin.concurrent.thread
import kotlin.math.min

/**
 * Cờ vua. Người chơi cầm quân Trắng (phía dưới), máy cầm quân Đen.
 * Vẽ kiểu tương phản cao cho màn e-ink, không animation.
 */
class ChessView(context: Context) : GameView(context) {

    private val board = ChessBoard()
    private var selected = -1
    private var targets = listOf<Move>()
    private var aiThinking = false
    private var gameOver = false

    private var cell = 0f
    private var left = 0f
    private var top = 0f

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val darkPaint = Paint().apply { color = Color.rgb(200, 200, 200) }
    private val lightPaint = Paint().apply { color = Color.WHITE }
    private val whitePiece = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val blackPiece = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
    private val pieceRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val textBlack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val textWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(90, 90, 90) }
    private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 7f
    }

    override fun newGame() {
        board.reset()
        selected = -1
        targets = emptyList()
        aiThinking = false
        gameOver = false
        status("Lượt của bạn (Trắng)")
        invalidate()
    }

    override fun undo() {
        if (aiThinking) return
        // Lùi cả nước của máy lẫn nước của người để về lượt người chơi.
        if (board.canUndo()) board.undoMove()
        if (board.canUndo() && !board.whiteToMove) board.undoMove()
        selected = -1
        targets = emptyList()
        gameOver = false
        status("Lượt của bạn (Trắng)")
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cell = min(w, h) / 8f
        left = (w - cell * 8) / 2f
        top = (h - cell * 8) / 2f
        textBlack.textSize = cell * 0.6f
        textWhite.textSize = cell * 0.6f
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        for (r in 0 until 8) for (c in 0 until 8) {
            val x = left + c * cell
            val y = top + r * cell
            val dark = (r + c) % 2 == 1
            canvas.drawRect(x, y, x + cell, y + cell, if (dark) darkPaint else lightPaint)
            canvas.drawRect(x, y, x + cell, y + cell, linePaint)
        }
        // Gợi ý nước đi hợp lệ
        for (m in targets) {
            val r = m.to / 8; val c = m.to % 8
            val cx = left + c * cell + cell / 2
            val cy = top + r * cell + cell / 2
            if (board.pieceAt(m.to) != ' ') {
                canvas.drawCircle(cx, cy, cell * 0.45f, selPaint)
            } else {
                canvas.drawCircle(cx, cy, cell * 0.14f, hintPaint)
            }
        }
        // Ô đang chọn
        if (selected >= 0) {
            val r = selected / 8; val c = selected % 8
            val x = left + c * cell; val y = top + r * cell
            canvas.drawRect(x + 3, y + 3, x + cell - 3, y + cell - 3, selPaint)
        }
        // Quân cờ
        for (i in 0 until 64) {
            val p = board.pieceAt(i)
            if (p == ' ') continue
            drawPiece(canvas, i, p)
        }
    }

    private fun drawPiece(canvas: Canvas, sq: Int, p: Char) {
        val r = sq / 8; val c = sq % 8
        val cx = left + c * cell + cell / 2
        val cy = top + r * cell + cell / 2
        val radius = cell * 0.4f
        val white = p in 'A'..'Z'
        if (white) {
            canvas.drawCircle(cx, cy, radius, whitePiece)
            canvas.drawCircle(cx, cy, radius, pieceRing)
        } else {
            canvas.drawCircle(cx, cy, radius, blackPiece)
        }
        val letter = p.uppercaseChar().toString()
        val tp = if (white) textBlack else textWhite
        val ty = cy - (tp.descent() + tp.ascent()) / 2
        canvas.drawText(letter, cx, ty, tp)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true
        if (aiThinking || gameOver) return true
        val sq = squareAt(event.x, event.y) ?: return true

        val p = board.pieceAt(sq)
        if (selected == -1) {
            if (p != ' ' && p in 'A'..'Z' && board.whiteToMove) {
                selected = sq
                targets = board.legalMoves().filter { it.from == sq }
                invalidate()
            }
            return true
        }

        // Đang có quân được chọn
        val move = targets.firstOrNull { it.to == sq && (it.promo == ' ' || it.promo == 'Q') }
        if (move != null) {
            board.makeMove(move)
            selected = -1
            targets = emptyList()
            invalidate()
            if (checkEnd(forWhite = false)) return true
            triggerAI()
        } else if (p != ' ' && p in 'A'..'Z') {
            selected = sq
            targets = board.legalMoves().filter { it.from == sq }
            invalidate()
        } else {
            selected = -1
            targets = emptyList()
            invalidate()
        }
        return true
    }

    private fun squareAt(x: Float, y: Float): Int? {
        if (x < left || y < top) return null
        val c = ((x - left) / cell).toInt()
        val r = ((y - top) / cell).toInt()
        if (r !in 0..7 || c !in 0..7) return null
        return r * 8 + c
    }

    private fun triggerAI() {
        aiThinking = true
        status("Máy đang nghĩ…")
        val depth = when (aiLevel) {
            1 -> 2
            3 -> 4
            else -> 3
        }
        thread {
            val m = ChessAI.bestMove(board, depth)
            post {
                if (m != null) board.makeMove(m)
                aiThinking = false
                invalidate()
                checkEnd(forWhite = true)
            }
        }
    }

    /** Kiểm tra kết thúc ván cho bên sắp đi ([forWhite]). Trả về true nếu đã kết thúc. */
    private fun checkEnd(forWhite: Boolean): Boolean {
        val moves = board.legalMoves()
        if (moves.isEmpty()) {
            gameOver = true
            status(
                if (board.inCheck(forWhite)) {
                    if (forWhite) "Chiếu hết! Bạn thua." else "Chiếu hết! Bạn thắng!"
                } else "Hòa cờ (hết nước đi)."
            )
            invalidate()
            return true
        }
        status(if (board.inCheck(forWhite)) "Chiếu! Lượt của bạn" else "Lượt của bạn (Trắng)")
        return false
    }
}
