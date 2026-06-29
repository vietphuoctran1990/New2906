package com.eink.boardgames

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

/**
 * Màn hình chính: chọn loại cờ, độ khó và cỡ bàn (cho cờ vây).
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val levelGroup = findViewById<RadioGroup>(R.id.levelGroup)
        val goSizeGroup = findViewById<RadioGroup>(R.id.goSizeGroup)

        fun level(): Int = when (levelGroup.checkedRadioButtonId) {
            R.id.levelEasy -> 1
            R.id.levelHard -> 3
            else -> 2
        }

        fun goSize(): Int = when (goSizeGroup.checkedRadioButtonId) {
            R.id.size13 -> 13
            R.id.size19 -> 19
            else -> 9
        }

        fun launch(game: String) {
            val i = Intent(this, GameActivity::class.java)
            i.putExtra(GameActivity.EXTRA_GAME, game)
            i.putExtra(GameActivity.EXTRA_LEVEL, level())
            i.putExtra(GameActivity.EXTRA_GO_SIZE, goSize())
            startActivity(i)
        }

        findViewById<Button>(R.id.btnChess).setOnClickListener { launch(GameActivity.GAME_CHESS) }
        findViewById<Button>(R.id.btnXiangqi).setOnClickListener { launch(GameActivity.GAME_XIANGQI) }
        findViewById<Button>(R.id.btnGo).setOnClickListener { launch(GameActivity.GAME_GO) }
    }
}
