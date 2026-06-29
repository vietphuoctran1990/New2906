package com.eink.boardgames.go

import kotlin.random.Random

/**
 * AI cờ vây theo heuristic (không phải engine mạnh như KataGo, nhưng
 * đủ vui để chơi giải trí trên máy đọc sách): ưu tiên bắt quân, chiếu bí
 * (atari), tránh tự sát/tự lấp mắt, và bám theo thế cờ.
 */
object GoAI {

    /** Trả về ô để đặt quân, hoặc null nghĩa là bỏ lượt. */
    fun chooseMove(board: GoBoard, komi: Double): Int? {
        val color = board.toMove
        val opp = 3 - color
        val n = board.n

        val legal = ArrayList<Int>()
        for (i in 0 until n * n) if (board.isLegal(i, color)) legal.add(i)
        if (legal.isEmpty()) return null

        // Nếu đối thủ vừa bỏ lượt và ta đang dẫn điểm -> bỏ lượt để kết thúc.
        if (board.consecutivePasses >= 1) {
            val (b, w) = board.score(komi)
            val mine = if (color == 1) b else w
            val theirs = if (color == 1) w else b
            if (mine > theirs) return null
        }

        var best = -Double.MAX_VALUE
        var bestMove = -1
        val oppBefore = board.stoneCount(opp)

        for (idx in legal) {
            // Bỏ qua nước lấp mắt thật của mình
            if (isOwnEye(board, idx, color)) continue

            board.play(idx, color)
            val captured = oppBefore - board.stoneCount(opp)
            val ownLibs = board.liberties(idx)

            var v = 0.0
            v += captured * 12.0
            // Tự đưa mình vào thế bị bắt mà không ăn được gì -> rất tệ
            if (ownLibs == 1 && captured == 0) v -= 10.0
            else v += minOf(ownLibs, 4) * 0.5

            // Dồn quân đối thủ vào atari
            for (nb in board.neighborsOf(idx)) {
                if (board.cells[nb] == opp && board.liberties(nb) == 1) v += 5.0
            }
            board.undo()

            // Thưởng tiếp xúc / bám thế, phạt sát mép bàn (nước đầu ván)
            v += contactBonus(board, idx, color)
            v -= edgePenalty(board, idx)
            v += Random.nextDouble() * 0.5

            if (v > best) { best = v; bestMove = idx }
        }

        if (bestMove < 0) return null
        // Nếu không nước nào ra hồn và đối thủ đã bỏ lượt thì cũng bỏ lượt.
        if (best <= 0.0 && board.consecutivePasses >= 1) return null
        return bestMove
    }

    /** [idx] có phải là mắt (eye) của [color] không (4 cạnh đều là quân mình). */
    private fun isOwnEye(board: GoBoard, idx: Int, color: Int): Boolean {
        for (nb in board.neighborsOf(idx)) {
            if (board.cells[nb] != color) return false
        }
        return true
    }

    private fun contactBonus(board: GoBoard, idx: Int, color: Int): Double {
        var bonus = 0.0
        for (nb in board.neighborsOf(idx)) {
            when (board.cells[nb]) {
                color -> bonus += 0.6
                3 - color -> bonus += 1.2
            }
        }
        return bonus
    }

    private fun edgePenalty(board: GoBoard, idx: Int): Double {
        val n = board.n
        val r = idx / n; val c = idx % n
        val distEdge = minOf(r, c, n - 1 - r, n - 1 - c)
        return when (distEdge) {
            0 -> 1.5
            1 -> 0.0
            else -> -0.3  // khuyến khích chơi vùng giữa một chút
        }
    }
}
