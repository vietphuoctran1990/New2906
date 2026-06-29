package com.eink.boardgames.xiangqi

/** Nước đi cờ tướng. */
data class XMove(val from: Int, val to: Int)

private data class XUndo(val from: Int, val to: Int, val captured: Char, val redToMove: Boolean)

/**
 * Bàn cờ tướng: 9 cột (0..8) x 10 hàng (0..9). index = row*9 + col.
 * Hàng 0 ở trên (Đen), hàng 9 ở dưới (Đỏ). Quân Đỏ viết HOA, quân Đen viết thường.
 *   K/k Tướng, A/a Sĩ, E/e Tượng, H/h Mã, R/r Xe, C/c Pháo, S/s Tốt.
 */
class XiangqiBoard {
    val sq = CharArray(90) { ' ' }
    var redToMove = true
    private val history = ArrayDeque<XUndo>()

    fun reset() {
        for (i in 0 until 90) sq[i] = ' '
        val backBlack = "rheakaehr"
        val backRed = "RHEAKAEHR"
        for (c in 0 until 9) {
            sq[c] = backBlack[c]
            sq[9 * 9 + c] = backRed[c]
        }
        sq[2 * 9 + 1] = 'c'; sq[2 * 9 + 7] = 'c'
        sq[7 * 9 + 1] = 'C'; sq[7 * 9 + 7] = 'C'
        for (c in intArrayOf(0, 2, 4, 6, 8)) {
            sq[3 * 9 + c] = 's'
            sq[6 * 9 + c] = 'S'
        }
        redToMove = true
        history.clear()
    }

    fun canUndo(): Boolean = history.isNotEmpty()

    private fun row(s: Int) = s / 9
    private fun col(s: Int) = s % 9
    private fun isRed(c: Char) = c in 'A'..'Z'
    private fun isBlack(c: Char) = c in 'a'..'z'
    private fun on(r: Int, c: Int) = r in 0..9 && c in 0..8
    private fun own(p: Char, red: Boolean) = if (red) isRed(p) else isBlack(p)
    private fun enemy(p: Char, red: Boolean) = if (red) isBlack(p) else isRed(p)

    fun pieceAt(s: Int): Char = sq[s]

    fun generalSquare(red: Boolean): Int {
        val k = if (red) 'K' else 'k'
        for (i in 0 until 90) if (sq[i] == k) return i
        return -1
    }

    private fun generalsFacing(): Boolean {
        val kr = generalSquare(true)
        val kb = generalSquare(false)
        if (kr < 0 || kb < 0) return false
        if (col(kr) != col(kb)) return false
        val c = col(kr)
        val r1 = minOf(row(kr), row(kb)) + 1
        val r2 = maxOf(row(kr), row(kb)) - 1
        for (r in r1..r2) if (sq[r * 9 + c] != ' ') return false
        return true
    }

    fun inCheck(red: Boolean): Boolean {
        if (generalsFacing()) return true
        val g = generalSquare(red)
        if (g < 0) return true
        for (m in pseudoMoves(!red)) if (m.to == g) return true
        return false
    }

    fun pseudoMoves(red: Boolean): MutableList<XMove> {
        val moves = ArrayList<XMove>(48)
        for (s in 0 until 90) {
            val p = sq[s]
            if (p == ' ' || !own(p, red)) continue
            when (p.uppercaseChar()) {
                'K' -> kingMoves(s, red, moves)
                'A' -> advisorMoves(s, red, moves)
                'E' -> elephantMoves(s, red, moves)
                'H' -> horseMoves(s, red, moves)
                'R' -> chariotMoves(s, red, moves)
                'C' -> cannonMoves(s, red, moves)
                'S' -> soldierMoves(s, red, moves)
            }
        }
        return moves
    }

    private fun add(s: Int, r: Int, c: Int, red: Boolean, moves: MutableList<XMove>) {
        if (!on(r, c)) return
        val t = sq[r * 9 + c]
        if (t == ' ' || enemy(t, red)) moves.add(XMove(s, r * 9 + c))
    }

    private fun inPalace(r: Int, c: Int, red: Boolean): Boolean {
        if (c < 3 || c > 5) return false
        return if (red) r in 7..9 else r in 0..2
    }

    private fun kingMoves(s: Int, red: Boolean, moves: MutableList<XMove>) {
        val r = row(s); val c = col(s)
        for (d in arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))) {
            val rr = r + d[0]; val cc = c + d[1]
            if (inPalace(rr, cc, red)) add(s, rr, cc, red, moves)
        }
    }

    private fun advisorMoves(s: Int, red: Boolean, moves: MutableList<XMove>) {
        val r = row(s); val c = col(s)
        for (d in arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1))) {
            val rr = r + d[0]; val cc = c + d[1]
            if (inPalace(rr, cc, red)) add(s, rr, cc, red, moves)
        }
    }

    private fun elephantMoves(s: Int, red: Boolean, moves: MutableList<XMove>) {
        val r = row(s); val c = col(s)
        val steps = arrayOf(intArrayOf(2, 2), intArrayOf(2, -2), intArrayOf(-2, 2), intArrayOf(-2, -2))
        for (d in steps) {
            val rr = r + d[0]; val cc = c + d[1]
            if (!on(rr, cc)) continue
            // Không qua sông
            if (red && rr < 5) continue
            if (!red && rr > 4) continue
            // Không bị cản mắt tượng
            val mr = r + d[0] / 2; val mc = c + d[1] / 2
            if (sq[mr * 9 + mc] != ' ') continue
            add(s, rr, cc, red, moves)
        }
    }

    private fun horseMoves(s: Int, red: Boolean, moves: MutableList<XMove>) {
        val r = row(s); val c = col(s)
        // target dr,dc và ô cản chân (leg)
        val data = arrayOf(
            intArrayOf(-2, -1, -1, 0), intArrayOf(-2, 1, -1, 0),
            intArrayOf(2, -1, 1, 0), intArrayOf(2, 1, 1, 0),
            intArrayOf(-1, -2, 0, -1), intArrayOf(1, -2, 0, -1),
            intArrayOf(-1, 2, 0, 1), intArrayOf(1, 2, 0, 1)
        )
        for (d in data) {
            val lr = r + d[2]; val lc = c + d[3]
            if (!on(lr, lc) || sq[lr * 9 + lc] != ' ') continue
            add(s, r + d[0], c + d[1], red, moves)
        }
    }

    private fun chariotMoves(s: Int, red: Boolean, moves: MutableList<XMove>) {
        val r = row(s); val c = col(s)
        for (d in arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))) {
            var rr = r + d[0]; var cc = c + d[1]
            while (on(rr, cc)) {
                val t = sq[rr * 9 + cc]
                if (t == ' ') moves.add(XMove(s, rr * 9 + cc))
                else {
                    if (enemy(t, red)) moves.add(XMove(s, rr * 9 + cc))
                    break
                }
                rr += d[0]; cc += d[1]
            }
        }
    }

    private fun cannonMoves(s: Int, red: Boolean, moves: MutableList<XMove>) {
        val r = row(s); val c = col(s)
        for (d in arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))) {
            var rr = r + d[0]; var cc = c + d[1]
            var screen = false
            while (on(rr, cc)) {
                val t = sq[rr * 9 + cc]
                if (!screen) {
                    if (t == ' ') moves.add(XMove(s, rr * 9 + cc))
                    else screen = true
                } else {
                    if (t != ' ') {
                        if (enemy(t, red)) moves.add(XMove(s, rr * 9 + cc))
                        break
                    }
                }
                rr += d[0]; cc += d[1]
            }
        }
    }

    private fun soldierMoves(s: Int, red: Boolean, moves: MutableList<XMove>) {
        val r = row(s); val c = col(s)
        val fwd = if (red) -1 else 1
        add(s, r + fwd, c, red, moves)
        val crossed = if (red) r <= 4 else r >= 5
        if (crossed) {
            add(s, r, c - 1, red, moves)
            add(s, r, c + 1, red, moves)
        }
    }

    /** Nước đi hợp lệ (không tự chiếu / không lộ mặt tướng). */
    fun legalMoves(): List<XMove> {
        val red = redToMove
        val out = ArrayList<XMove>(48)
        for (m in pseudoMoves(red)) {
            makeMove(m)
            if (!inCheck(red)) out.add(m)
            undoMove()
        }
        return out
    }

    fun makeMove(m: XMove) {
        val captured = sq[m.to]
        history.addLast(XUndo(m.from, m.to, captured, redToMove))
        sq[m.to] = sq[m.from]
        sq[m.from] = ' '
        redToMove = !redToMove
    }

    fun undoMove() {
        val u = history.removeLast()
        sq[u.from] = sq[u.to]
        sq[u.to] = u.captured
        redToMove = u.redToMove
    }
}
