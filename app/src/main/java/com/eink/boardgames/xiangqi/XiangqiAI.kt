package com.eink.boardgames.xiangqi

/**
 * AI cờ tướng: minimax + alpha-beta, tìm kiếm theo nước "giả hợp lệ"
 * (ai ăn được Tướng đối phương sẽ thắng) để chạy nhanh trên CPU yếu.
 */
object XiangqiAI {

    private const val MATE = 1_000_000

    private fun value(p: Char): Int = when (p.uppercaseChar()) {
        'K' -> 10000
        'R' -> 600
        'C' -> 300
        'H' -> 270
        'E' -> 120
        'A' -> 120
        'S' -> 50
        else -> 0
    }

    private fun evaluate(b: XiangqiBoard): Int {
        var score = 0
        for (i in 0 until 90) {
            val p = b.pieceAt(i)
            if (p == ' ') continue
            val red = p in 'A'..'Z'
            var v = value(p)
            // Tốt qua sông mạnh hơn
            if (p.uppercaseChar() == 'S') {
                val r = i / 9
                val crossed = if (red) r <= 4 else r >= 5
                if (crossed) v += 50
            }
            score += if (red) v else -v
        }
        return score
    }

    fun bestMove(b: XiangqiBoard, depth: Int): XMove? {
        val red = b.redToMove
        val color = if (red) 1 else -1
        // Ở gốc cây dùng nước hợp lệ thật để không chọn nước phạm luật.
        val moves = order(b, b.legalMoves())
        if (moves.isEmpty()) return null
        var best: XMove? = null
        var bestScore = Int.MIN_VALUE
        var alpha = -MATE * 2
        val beta = MATE * 2
        for (m in moves) {
            b.makeMove(m)
            val score = -negamax(b, depth - 1, -beta, -alpha, -color)
            b.undoMove()
            if (score > bestScore) { bestScore = score; best = m }
            if (score > alpha) alpha = score
        }
        return best
    }

    private fun negamax(b: XiangqiBoard, depth: Int, alphaIn: Int, beta: Int, color: Int): Int {
        if (depth == 0) return color * evaluate(b)
        val moves = order(b, b.pseudoMoves(b.redToMove))
        if (moves.isEmpty()) return -MATE - depth
        var alpha = alphaIn
        var best = -MATE * 2
        for (m in moves) {
            val target = b.pieceAt(m.to)
            if (target.uppercaseChar() == 'K') {
                // Ăn được Tướng -> thắng ngay
                return MATE + depth
            }
            b.makeMove(m)
            val score = -negamax(b, depth - 1, -beta, -alpha, -color)
            b.undoMove()
            if (score > best) best = score
            if (best > alpha) alpha = best
            if (alpha >= beta) break
        }
        return best
    }

    private fun order(b: XiangqiBoard, moves: List<XMove>): List<XMove> =
        moves.sortedByDescending { m ->
            val t = b.pieceAt(m.to)
            if (t != ' ') value(t) else 0
        }
}
