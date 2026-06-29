package com.eink.boardgames.go

/**
 * Bàn cờ vây NxN. Ô trống = 0, Đen = 1, Trắng = 2.
 * Người chơi cầm Đen (đi trước), máy cầm Trắng.
 */
class GoBoard(val n: Int) {

    val cells = IntArray(n * n)
    var toMove = 1            // 1 = Đen, 2 = Trắng
    var koPoint = -1
    var consecutivePasses = 0
    var lastMove = -1

    private class Snap(
        val cells: IntArray, val toMove: Int, val koPoint: Int,
        val passes: Int, val lastMove: Int
    )

    private val history = ArrayDeque<Snap>()

    // --- Hỗ trợ cho AI ---
    fun neighborsOf(idx: Int): IntArray = neighbors(idx)
    fun liberties(idx: Int): Int = if (cells[idx] == 0) 0 else group(cells, idx).second
    fun stoneCount(color: Int): Int = cells.count { it == color }

    fun reset() {
        cells.fill(0)
        toMove = 1
        koPoint = -1
        consecutivePasses = 0
        lastMove = -1
        history.clear()
    }

    fun canUndo(): Boolean = history.isNotEmpty()

    private fun rc(idx: Int) = intArrayOf(idx / n, idx % n)

    private fun neighbors(idx: Int): IntArray {
        val r = idx / n; val c = idx % n
        val list = ArrayList<Int>(4)
        if (r > 0) list.add(idx - n)
        if (r < n - 1) list.add(idx + n)
        if (c > 0) list.add(idx - 1)
        if (c < n - 1) list.add(idx + 1)
        return list.toIntArray()
    }

    /** Trả về (danh sách quân trong nhóm, số khí). */
    private fun group(board: IntArray, start: Int): Pair<MutableList<Int>, Int> {
        val color = board[start]
        val stones = ArrayList<Int>()
        val seen = HashSet<Int>()
        val libs = HashSet<Int>()
        val stack = ArrayDeque<Int>()
        stack.addLast(start); seen.add(start)
        while (stack.isNotEmpty()) {
            val s = stack.removeLast()
            stones.add(s)
            for (nb in neighbors(s)) {
                when (board[nb]) {
                    0 -> libs.add(nb)
                    color -> if (seen.add(nb)) stack.addLast(nb)
                }
            }
        }
        return stones to libs.size
    }

    /** Nước đặt quân tại [idx] của [color] có hợp lệ không. */
    fun isLegal(idx: Int, color: Int): Boolean {
        if (idx < 0 || idx >= cells.size) return false
        if (cells[idx] != 0) return false
        if (idx == koPoint && color == toMove) return false
        val test = cells.copyOf()
        test[idx] = color
        val opp = 3 - color
        var captured = 0
        for (nb in neighbors(idx)) {
            if (test[nb] == opp) {
                val (st, libs) = group(test, nb)
                if (libs == 0) {
                    captured += st.size
                    for (s in st) test[s] = 0
                }
            }
        }
        if (captured > 0) return true
        // Cấm tự sát
        return group(test, idx).second > 0
    }

    /** Đặt quân (giả định đã hợp lệ). */
    fun play(idx: Int, color: Int) {
        pushSnap()
        cells[idx] = color
        val opp = 3 - color
        var captured = 0
        var lastCapturedPoint = -1
        for (nb in neighbors(idx)) {
            if (cells[nb] == opp) {
                val (st, libs) = group(cells, nb)
                if (libs == 0) {
                    for (s in st) cells[s] = 0
                    captured += st.size
                    if (st.size == 1) lastCapturedPoint = st[0]
                }
            }
        }
        // Xác định điểm ko
        val (own, ownLibs) = group(cells, idx)
        koPoint = if (captured == 1 && own.size == 1 && ownLibs == 1) lastCapturedPoint else -1
        consecutivePasses = 0
        lastMove = idx
        toMove = opp
    }

    fun pass(color: Int) {
        pushSnap()
        koPoint = -1
        consecutivePasses += 1
        lastMove = -1
        toMove = 3 - color
    }

    private fun pushSnap() {
        history.addLast(Snap(cells.copyOf(), toMove, koPoint, consecutivePasses, lastMove))
    }

    fun undo() {
        val s = history.removeLast()
        System.arraycopy(s.cells, 0, cells, 0, cells.size)
        toMove = s.toMove
        koPoint = s.koPoint
        consecutivePasses = s.passes
        lastMove = s.lastMove
    }

    /**
     * Tính điểm theo luật "khu vực" (quân trên bàn + đất vây được).
     * Lưu ý: bản đơn giản, không tự loại quân chết.
     * Trả về (điểm Đen, điểm Trắng có cộng komi).
     */
    fun score(komi: Double): Pair<Double, Double> {
        var black = 0.0
        var white = komi
        for (i in cells.indices) {
            when (cells[i]) {
                1 -> black += 1
                2 -> white += 1
            }
        }
        // Đất trống
        val visited = BooleanArray(cells.size)
        for (i in cells.indices) {
            if (cells[i] != 0 || visited[i]) continue
            val region = ArrayList<Int>()
            val borders = HashSet<Int>()
            val stack = ArrayDeque<Int>()
            stack.addLast(i); visited[i] = true
            while (stack.isNotEmpty()) {
                val s = stack.removeLast()
                region.add(s)
                for (nb in neighbors(s)) {
                    when (cells[nb]) {
                        0 -> if (!visited[nb]) { visited[nb] = true; stack.addLast(nb) }
                        else -> borders.add(cells[nb])
                    }
                }
            }
            if (borders.size == 1) {
                if (borders.first() == 1) black += region.size else white += region.size
            }
        }
        return black to white
    }
}
