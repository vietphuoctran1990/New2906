package com.eink.boardgames.chess

/** Một nước đi cờ vua. */
data class Move(
    val from: Int,
    val to: Int,
    val promo: Char = ' ',
    val isEnPassant: Boolean = false,
    val isCastle: Boolean = false
)

private data class Undo(
    val move: Move,
    val movedPiece: Char,
    val capturedPiece: Char,
    val capturedSquare: Int,
    val wK: Boolean, val wQ: Boolean, val bK: Boolean, val bQ: Boolean,
    val enPassant: Int,
    val whiteToMove: Boolean
)

/**
 * Bàn cờ vua 8x8. index = row*8 + col, row 0 ở trên cùng (quân đen),
 * row 7 ở dưới cùng (quân trắng). Quân trắng viết HOA, quân đen viết thường.
 */
class ChessBoard {
    val sq = CharArray(64) { ' ' }
    var whiteToMove = true
    var wK = true; var wQ = true; var bK = true; var bQ = true
    var enPassant = -1
    private val history = ArrayDeque<Undo>()

    fun reset() {
        val back = "rnbqkbnr"
        for (c in 0 until 8) {
            sq[c] = back[c]              // row 0 đen
            sq[8 + c] = 'p'             // row 1 tốt đen
            for (r in 2 until 6) sq[r * 8 + c] = ' '
            sq[6 * 8 + c] = 'P'         // row 6 tốt trắng
            sq[7 * 8 + c] = back[c].uppercaseChar()
        }
        whiteToMove = true
        wK = true; wQ = true; bK = true; bQ = true
        enPassant = -1
        history.clear()
    }

    fun canUndo(): Boolean = history.isNotEmpty()

    private fun row(s: Int) = s / 8
    private fun col(s: Int) = s % 8
    private fun isWhite(c: Char) = c in 'A'..'Z'
    private fun isBlack(c: Char) = c in 'a'..'z'
    private fun onBoard(r: Int, c: Int) = r in 0..7 && c in 0..7

    fun pieceAt(s: Int): Char = sq[s]

    /** Vua của bên trắng/đen đang ở ô nào. */
    fun kingSquare(white: Boolean): Int {
        val k = if (white) 'K' else 'k'
        for (i in 0 until 64) if (sq[i] == k) return i
        return -1
    }

    fun inCheck(white: Boolean): Boolean {
        val ks = kingSquare(white)
        if (ks < 0) return false
        return isAttacked(ks, !white)
    }

    /** Ô [s] có bị bên [byWhite] tấn công không? */
    fun isAttacked(s: Int, byWhite: Boolean): Boolean {
        val r = row(s); val c = col(s)
        // Tốt
        if (byWhite) {
            if (onBoard(r + 1, c - 1) && sq[(r + 1) * 8 + (c - 1)] == 'P') return true
            if (onBoard(r + 1, c + 1) && sq[(r + 1) * 8 + (c + 1)] == 'P') return true
        } else {
            if (onBoard(r - 1, c - 1) && sq[(r - 1) * 8 + (c - 1)] == 'p') return true
            if (onBoard(r - 1, c + 1) && sq[(r - 1) * 8 + (c + 1)] == 'p') return true
        }
        // Mã
        val kn = intArrayOf(-2, -1, -2, 1, -1, -2, -1, 2, 1, -2, 1, 2, 2, -1, 2, 1)
        var i = 0
        while (i < kn.size) {
            val rr = r + kn[i]; val cc = c + kn[i + 1]
            if (onBoard(rr, cc)) {
                val p = sq[rr * 8 + cc]
                if (byWhite && p == 'N') return true
                if (!byWhite && p == 'n') return true
            }
            i += 2
        }
        // Vua
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val rr = r + dr; val cc = c + dc
            if (onBoard(rr, cc)) {
                val p = sq[rr * 8 + cc]
                if (byWhite && p == 'K') return true
                if (!byWhite && p == 'k') return true
            }
        }
        // Tịnh tiến thẳng (Xe/Hậu)
        val rook = if (byWhite) charArrayOf('R', 'Q') else charArrayOf('r', 'q')
        val bishop = if (byWhite) charArrayOf('B', 'Q') else charArrayOf('b', 'q')
        val straight = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
        for (d in straight) {
            var rr = r + d[0]; var cc = c + d[1]
            while (onBoard(rr, cc)) {
                val p = sq[rr * 8 + cc]
                if (p != ' ') {
                    if (p == rook[0] || p == rook[1]) return true
                    break
                }
                rr += d[0]; cc += d[1]
            }
        }
        val diag = arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1))
        for (d in diag) {
            var rr = r + d[0]; var cc = c + d[1]
            while (onBoard(rr, cc)) {
                val p = sq[rr * 8 + cc]
                if (p != ' ') {
                    if (p == bishop[0] || p == bishop[1]) return true
                    break
                }
                rr += d[0]; cc += d[1]
            }
        }
        return false
    }

    private fun ownPiece(p: Char, white: Boolean) =
        if (white) isWhite(p) else isBlack(p)

    private fun enemyPiece(p: Char, white: Boolean) =
        if (white) isBlack(p) else isWhite(p)

    fun pseudoLegalMoves(white: Boolean): MutableList<Move> {
        val moves = ArrayList<Move>(48)
        for (s in 0 until 64) {
            val p = sq[s]
            if (p == ' ' || !ownPiece(p, white)) continue
            val r = row(s); val c = col(s)
            when (p.uppercaseChar()) {
                'P' -> pawnMoves(s, r, c, white, moves)
                'N' -> {
                    val kn = intArrayOf(-2, -1, -2, 1, -1, -2, -1, 2, 1, -2, 1, 2, 2, -1, 2, 1)
                    var i = 0
                    while (i < kn.size) {
                        val rr = r + kn[i]; val cc = c + kn[i + 1]
                        if (onBoard(rr, cc)) {
                            val t = sq[rr * 8 + cc]
                            if (t == ' ' || enemyPiece(t, white)) moves.add(Move(s, rr * 8 + cc))
                        }
                        i += 2
                    }
                }
                'B' -> slide(s, r, c, white, moves, arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)))
                'R' -> slide(s, r, c, white, moves, arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1)))
                'Q' -> slide(s, r, c, white, moves, arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1), intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1)))
                'K' -> {
                    for (dr in -1..1) for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val rr = r + dr; val cc = c + dc
                        if (onBoard(rr, cc)) {
                            val t = sq[rr * 8 + cc]
                            if (t == ' ' || enemyPiece(t, white)) moves.add(Move(s, rr * 8 + cc))
                        }
                    }
                    castleMoves(white, moves)
                }
            }
        }
        return moves
    }

    private fun slide(s: Int, r: Int, c: Int, white: Boolean, moves: MutableList<Move>, dirs: Array<IntArray>) {
        for (d in dirs) {
            var rr = r + d[0]; var cc = c + d[1]
            while (onBoard(rr, cc)) {
                val idx = rr * 8 + cc
                val t = sq[idx]
                if (t == ' ') moves.add(Move(s, idx))
                else {
                    if (enemyPiece(t, white)) moves.add(Move(s, idx))
                    break
                }
                rr += d[0]; cc += d[1]
            }
        }
    }

    private fun pawnMoves(s: Int, r: Int, c: Int, white: Boolean, moves: MutableList<Move>) {
        val dir = if (white) -1 else 1
        val startRow = if (white) 6 else 1
        val promoRow = if (white) 0 else 7
        val one = r + dir
        if (onBoard(one, c) && sq[one * 8 + c] == ' ') {
            addPawn(s, one * 8 + c, one == promoRow, moves)
            val two = r + 2 * dir
            if (r == startRow && sq[two * 8 + c] == ' ') {
                moves.add(Move(s, two * 8 + c))
            }
        }
        for (dc in intArrayOf(-1, 1)) {
            val cc = c + dc
            if (!onBoard(one, cc)) continue
            val idx = one * 8 + cc
            val t = sq[idx]
            if (t != ' ' && enemyPiece(t, white)) {
                addPawn(s, idx, one == promoRow, moves)
            } else if (idx == enPassant) {
                moves.add(Move(s, idx, isEnPassant = true))
            }
        }
    }

    private fun addPawn(from: Int, to: Int, promo: Boolean, moves: MutableList<Move>) {
        if (promo) {
            val white = isWhite(sq[from])
            for (pc in charArrayOf('Q', 'R', 'B', 'N')) {
                moves.add(Move(from, to, if (white) pc else pc.lowercaseChar()))
            }
        } else {
            moves.add(Move(from, to))
        }
    }

    private fun castleMoves(white: Boolean, moves: MutableList<Move>) {
        if (inCheck(white)) return
        if (white) {
            // e1 = 60
            if (wK && sq[61] == ' ' && sq[62] == ' ' && sq[63] == 'R' &&
                !isAttacked(61, false) && !isAttacked(62, false)
            ) moves.add(Move(60, 62, isCastle = true))
            if (wQ && sq[59] == ' ' && sq[58] == ' ' && sq[57] == ' ' && sq[56] == 'R' &&
                !isAttacked(59, false) && !isAttacked(58, false)
            ) moves.add(Move(60, 58, isCastle = true))
        } else {
            // e8 = 4
            if (bK && sq[5] == ' ' && sq[6] == ' ' && sq[7] == 'r' &&
                !isAttacked(5, true) && !isAttacked(6, true)
            ) moves.add(Move(4, 6, isCastle = true))
            if (bQ && sq[3] == ' ' && sq[2] == ' ' && sq[1] == ' ' && sq[0] == 'r' &&
                !isAttacked(3, true) && !isAttacked(2, true)
            ) moves.add(Move(4, 2, isCastle = true))
        }
    }

    /** Nước đi hợp lệ (đã lọc để không tự chiếu vua mình). */
    fun legalMoves(): List<Move> {
        val white = whiteToMove
        val out = ArrayList<Move>(48)
        for (m in pseudoLegalMoves(white)) {
            makeMove(m)
            if (!inCheck(white)) out.add(m)
            undoMove()
        }
        return out
    }

    fun makeMove(m: Move) {
        val moved = sq[m.from]
        val white = isWhite(moved)
        val capturedSquare = if (m.isEnPassant) row(m.from) * 8 + col(m.to) else m.to
        val captured = sq[capturedSquare]

        history.addLast(
            Undo(m, moved, captured, capturedSquare, wK, wQ, bK, bQ, enPassant, whiteToMove)
        )

        // Di chuyển quân
        sq[m.from] = ' '
        if (m.isEnPassant) sq[capturedSquare] = ' '
        sq[m.to] = if (m.promo != ' ') m.promo else moved

        // Nhập thành: di chuyển xe
        if (m.isCastle) {
            when (m.to) {
                62 -> { sq[63] = ' '; sq[61] = 'R' }
                58 -> { sq[56] = ' '; sq[59] = 'R' }
                6 -> { sq[7] = ' '; sq[5] = 'r' }
                2 -> { sq[0] = ' '; sq[3] = 'r' }
            }
        }

        // Cập nhật quyền nhập thành
        when (moved) {
            'K' -> { wK = false; wQ = false }
            'k' -> { bK = false; bQ = false }
        }
        if (m.from == 63 || m.to == 63) wK = false
        if (m.from == 56 || m.to == 56) wQ = false
        if (m.from == 7 || m.to == 7) bK = false
        if (m.from == 0 || m.to == 0) bQ = false

        // Ô bắt tốt qua đường (en passant)
        enPassant = if (moved.uppercaseChar() == 'P' && Math.abs(row(m.to) - row(m.from)) == 2) {
            (row(m.from) + row(m.to)) / 2 * 8 + col(m.from)
        } else -1

        whiteToMove = !white
    }

    fun undoMove() {
        val u = history.removeLast()
        val m = u.move
        whiteToMove = u.whiteToMove
        wK = u.wK; wQ = u.wQ; bK = u.bK; bQ = u.bQ
        enPassant = u.enPassant

        sq[m.from] = u.movedPiece
        sq[m.to] = ' '
        if (m.isEnPassant) {
            sq[u.capturedSquare] = u.capturedPiece
        } else {
            sq[m.to] = u.capturedPiece
        }
        if (m.isCastle) {
            when (m.to) {
                62 -> { sq[63] = 'R'; sq[61] = ' ' }
                58 -> { sq[56] = 'R'; sq[59] = ' ' }
                6 -> { sq[7] = 'r'; sq[5] = ' ' }
                2 -> { sq[0] = 'r'; sq[3] = ' ' }
            }
        }
    }
}
