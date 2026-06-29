package com.eink.boardgames.chess

/**
 * AI cờ vua: minimax + cắt tỉa alpha-beta, có sắp xếp nước đi đơn giản.
 * Đánh giá theo góc nhìn quân Trắng (dương = Trắng lợi).
 */
object ChessAI {

    private const val MATE = 1_000_000

    private val value = mapOf(
        'P' to 100, 'N' to 320, 'B' to 330, 'R' to 500, 'Q' to 900, 'K' to 20000
    )

    // Bảng vị trí (góc nhìn quân Trắng, index 0 = trên cùng).
    private val pawnPst = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5, 5, 10, 25, 25, 10, 5, 5,
        0, 0, 0, 20, 20, 0, 0, 0,
        5, -5, -10, 0, 0, -10, -5, 5,
        5, 10, 10, -20, -20, 10, 10, 5,
        0, 0, 0, 0, 0, 0, 0, 0
    )
    private val knightPst = intArrayOf(
        -50, -40, -30, -30, -30, -30, -40, -50,
        -40, -20, 0, 0, 0, 0, -20, -40,
        -30, 0, 10, 15, 15, 10, 0, -30,
        -30, 5, 15, 20, 20, 15, 5, -30,
        -30, 0, 15, 20, 20, 15, 0, -30,
        -30, 5, 10, 15, 15, 10, 5, -30,
        -40, -20, 0, 5, 5, 0, -20, -40,
        -50, -40, -30, -30, -30, -30, -40, -50
    )

    private fun evaluate(b: ChessBoard): Int {
        var score = 0
        for (i in 0 until 64) {
            val p = b.pieceAt(i)
            if (p == ' ') continue
            val white = p in 'A'..'Z'
            val v = value[p.uppercaseChar()] ?: 0
            var pos = 0
            when (p.uppercaseChar()) {
                'P' -> pos = if (white) pawnPst[i] else pawnPst[mirror(i)]
                'N' -> pos = if (white) knightPst[i] else knightPst[mirror(i)]
            }
            score += if (white) v + pos else -(v + pos)
        }
        return score
    }

    private fun mirror(i: Int): Int {
        val r = i / 8; val c = i % 8
        return (7 - r) * 8 + c
    }

    /** Tìm nước đi tốt nhất cho bên đang tới lượt. */
    fun bestMove(b: ChessBoard, depth: Int): Move? {
        val white = b.whiteToMove
        val color = if (white) 1 else -1
        val moves = orderMoves(b, b.legalMoves())
        if (moves.isEmpty()) return null
        var best: Move? = null
        var bestScore = Int.MIN_VALUE
        var alpha = -MATE * 2
        val beta = MATE * 2
        for (m in moves) {
            b.makeMove(m)
            val score = -negamax(b, depth - 1, -beta, -alpha, -color)
            b.undoMove()
            if (score > bestScore) {
                bestScore = score
                best = m
            }
            if (score > alpha) alpha = score
        }
        return best
    }

    private fun negamax(b: ChessBoard, depth: Int, alphaIn: Int, beta: Int, color: Int): Int {
        if (depth == 0) return color * evaluate(b)
        val moves = orderMoves(b, b.legalMoves())
        if (moves.isEmpty()) {
            return if (b.inCheck(b.whiteToMove)) -MATE - depth else 0
        }
        var alpha = alphaIn
        var best = -MATE * 2
        for (m in moves) {
            b.makeMove(m)
            val score = -negamax(b, depth - 1, -beta, -alpha, -color)
            b.undoMove()
            if (score > best) best = score
            if (best > alpha) alpha = best
            if (alpha >= beta) break
        }
        return best
    }

    /** Ưu tiên nước ăn quân để cắt tỉa hiệu quả hơn. */
    private fun orderMoves(b: ChessBoard, moves: List<Move>): List<Move> {
        return moves.sortedByDescending { m ->
            val target = b.pieceAt(m.to)
            if (target != ' ') (value[target.uppercaseChar()] ?: 0) else 0
        }
    }
}
