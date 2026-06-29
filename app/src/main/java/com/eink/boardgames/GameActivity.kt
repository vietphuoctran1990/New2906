package com.eink.boardgames

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.eink.boardgames.chess.ChessView
import com.eink.boardgames.go.GoView
import com.eink.boardgames.xiangqi.XiangqiView

class GameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GAME = "game"
        const val EXTRA_LEVEL = "level"
        const val EXTRA_GO_SIZE = "go_size"
        const val GAME_CHESS = "chess"
        const val GAME_XIANGQI = "xiangqi"
        const val GAME_GO = "go"
    }

    private lateinit var view: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val game = intent.getStringExtra(EXTRA_GAME) ?: GAME_CHESS
        val level = intent.getIntExtra(EXTRA_LEVEL, 2)
        val goSize = intent.getIntExtra(EXTRA_GO_SIZE, 9)

        val status = findViewById<TextView>(R.id.status)
        val container = findViewById<FrameLayout>(R.id.boardContainer)

        view = when (game) {
            GAME_XIANGQI -> XiangqiView(this)
            GAME_GO -> GoView(this, goSize)
            else -> ChessView(this)
        }
        view.aiLevel = level
        view.onStatus = { text -> status.text = text }
        container.addView(view)

        title = when (game) {
            GAME_XIANGQI -> getString(R.string.game_xiangqi)
            GAME_GO -> getString(R.string.game_go)
            else -> getString(R.string.game_chess)
        }

        findViewById<Button>(R.id.btnNew).setOnClickListener { view.newGame() }
        findViewById<Button>(R.id.btnUndo).setOnClickListener { view.undo() }
        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            // Vẽ lại toàn màn hình để xoá bóng mờ (ghosting) của e-ink.
            view.invalidate()
        }

        val btnPass = findViewById<Button>(R.id.btnPass)
        if (view.supportsPass()) {
            btnPass.setOnClickListener { view.pass() }
        } else {
            btnPass.isEnabled = false
            btnPass.visibility = View.GONE
        }

        view.newGame()
    }
}
