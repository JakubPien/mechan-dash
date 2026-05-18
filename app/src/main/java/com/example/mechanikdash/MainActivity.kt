package com.mechanikdash.game

import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mechanikdash.game.GameController
import com.mechanikdash.game.GameState
import com.mechanikdash.game.LevelRepository
import com.mechanikdash.game.MusicController
import com.mechanikdash.game.MessageType
import com.mechanikdash.game.GameSnapshot
import com.mechanikdash.game.EnemyKind
import com.mechanikdash.game.CellKind

// ── Paleta kolorów ───────────────────────────────────────────────────────────
private val BgDeep      = Color(0xFF060C17)
private val BgCard      = Color(0xFF0D1829)
private val BgBoard     = Color(0xFF080F1C)
private val AccentCyan  = Color(0xFF00C8F0)
private val AccentAmber = Color(0xFFFFB300)
private val AccentGreen = Color(0xFF00D47A)
private val AccentRed   = Color(0xFFFF4455)
private val TextPri     = Color(0xFFDCEEFF)
private val TextMuted   = Color(0xFF4A6A8A)
private val Border      = Color(0xFF162236)

class MainActivity : ComponentActivity() {

    private lateinit var musicController: MusicController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicController = MusicController(this)
        musicController.start()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = BgDeep) {
                    MechanikDashApp(musicController = musicController)
                }
            }
        }
    }

    override fun onPause()  { super.onPause();  musicController.pause()  }
    override fun onResume() { super.onResume(); musicController.resume() }
    override fun onDestroy(){ super.onDestroy(); musicController.stop()  }
}

@Composable
fun MechanikDashApp(musicController: MusicController) {
    val controller = remember { GameController(LevelRepository.levels) }
    var snapshot by remember { mutableStateOf(controller.snapshot()) }
    var isMuted by remember { mutableStateOf(false) }

    if (snapshot.state == GameState.StartScreen || snapshot.state == GameState.EndlessIntro) {
        StartScreen(
            text = stringResource(snapshot.startScreenTextRes),
            endlessUnlocked = snapshot.endlessUnlocked && snapshot.state == GameState.StartScreen,
            onStart = {
                snapshot = if (snapshot.state == GameState.EndlessIntro) {
                    controller.confirmEndless()
                } else {
                    controller.startGame()
                }
            },
            onStartEndless = {
                snapshot = controller.startEndless()
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameHeader(
            snapshot = snapshot,
            isMuted = isMuted,
            onToggleMute = { isMuted = musicController.toggleMute() },
            onMainMenu = { snapshot = controller.goToMainMenu() }
        )

        Spacer(Modifier.height(6.dp))

        StatsBar(snapshot = snapshot)

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, Border, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(BgBoard)
        ) {
            GameBoard(snapshot = snapshot, modifier = Modifier.fillMaxSize())

            if (snapshot.gameOver || snapshot.gameWon || snapshot.state == GameState.LevelComplete) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xF0060C17))
                            )
                        )
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (snapshot.state == GameState.EndlessGameOver) {
                            Text(
                                text = stringResource(R.string.endless_completed_count, snapshot.endlessLevelsCompleted),
                                color = AccentAmber,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                        }

                        when (snapshot.state) {
                            GameState.GameOver -> {
                                ActionButton(stringResource(R.string.btn_try_again), AccentCyan) {
                                    snapshot = controller.restartGame()
                                }
                            }
                            GameState.Won -> {
                                ActionButton(stringResource(R.string.btn_try_endless), AccentAmber) {
                                    snapshot = controller.startEndless()
                                }
                                ActionButton(stringResource(R.string.btn_restart_full), AccentCyan) {
                                    snapshot = controller.restartGame()
                                }
                            }
                            GameState.LevelComplete -> {
                                ActionButton(stringResource(R.string.btn_next_level), AccentGreen) {
                                    snapshot = controller.nextLevel()
                                }
                            }
                            GameState.EndlessGameOver -> {
                                ActionButton(stringResource(R.string.btn_repeat_endless), AccentAmber) {
                                    snapshot = controller.startEndless()
                                }
                                ActionButton(stringResource(R.string.btn_back_to_levels), AccentCyan) {
                                    snapshot = controller.restartGame()
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            snapshot.messageRes?.let { resId ->
                val args = snapshot.messageArgs.map { arg ->
                    if (arg is Int && arg > 2130000000) context.getString(arg) else arg
                }.toTypedArray()

                Text(
                    text = stringResource(resId, *args),
                    color = when (snapshot.messageType) {
                        MessageType.Error -> AccentRed
                        MessageType.Success -> AccentGreen
                        MessageType.Normal -> TextMuted
                    },
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        DirectionPad(
            onMove    = { dx, dy -> snapshot = controller.move(dx, dy) },
            onRestart = {
                snapshot = if (snapshot.state == GameState.Endless) {
                    controller.startEndless()
                } else {
                    controller.restartGame()
                }
            },
            gameActive = !snapshot.gameOver && !snapshot.gameWon && snapshot.state != GameState.LevelComplete
        )

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun GameHeader(
    snapshot: GameSnapshot,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onMainMenu: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(snapshot.titleRes),
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = AccentCyan,
            letterSpacing = 1.5.sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(BgCard, RoundedCornerShape(6.dp))
                    .border(1.dp, Border, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .clickable { onMainMenu() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🏠", fontSize = 14.sp)
            }

            Box(
                modifier = Modifier
                    .background(BgCard, RoundedCornerShape(6.dp))
                    .border(1.dp, Border, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .clickable { onToggleMute() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (isMuted) "🔇" else "🔊", fontSize = 14.sp)
            }

            when (snapshot.state) {
                GameState.Playing,
                GameState.LevelComplete   -> Badge(stringResource(R.string.stats_level, snapshot.levelIndex + 1, snapshot.levelCount), AccentAmber)
                GameState.Endless,
                GameState.EndlessGameOver -> Badge(stringResource(R.string.stats_endless_levels, snapshot.endlessLevelsCompleted), AccentAmber)
                GameState.Won             -> Badge(stringResource(R.string.status_completed), AccentGreen)
                else -> {}
            }
        }
    }
}

@Composable
private fun Badge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatsBar(snapshot: GameSnapshot) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard, RoundedCornerShape(10.dp))
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        val levelNameArgs = snapshot.levelNameArgs.map { arg ->
            if (arg is Int && arg > 2130000000) context.getString(arg) else arg
        }.toTypedArray()

        Text(
            text = stringResource(snapshot.levelNameRes, *levelNameArgs),
            color = TextPri,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill(stringResource(R.string.stats_grades), "${snapshot.totalGrades}/${snapshot.requiredGrades}", AccentGreen)
            StatPill(stringResource(R.string.stats_lives), "${snapshot.lives}", AccentRed)
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = color.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun GameBoard(snapshot: GameSnapshot, modifier: Modifier = Modifier) {
    val balloonImg       = ImageBitmap.imageResource(R.drawable.balon)
    val femaleTeacherImg = ImageBitmap.imageResource(R.drawable.femaletecher)
    val femaleTeacherImg2= ImageBitmap.imageResource(R.drawable.femaletecher2)
    val maleTeacherImg   = ImageBitmap.imageResource(R.drawable.maletecher)
    val bookImg          = ImageBitmap.imageResource(R.drawable.ksiazka)
    val gradeImg         = ImageBitmap.imageResource(R.drawable.ocena)
    val quizImg          = ImageBitmap.imageResource(R.drawable.kartkowka)
    val playerImg        = ImageBitmap.imageResource(R.drawable.uczen)
    val exitImg          = ImageBitmap.imageResource(R.drawable.exit)
    val phoneImg         = ImageBitmap.imageResource(R.drawable.telefon)
    val floorImg         = ImageBitmap.imageResource(R.drawable.floor)
    val wallImg          = ImageBitmap.imageResource(R.drawable.wall)
    val benchImg         = ImageBitmap.imageResource(R.drawable.lawka)
    val sofaImg          = ImageBitmap.imageResource(R.drawable.sofa)

    val gameWinText = stringResource(R.string.game_win)
    val levelCompleteText = stringResource(R.string.level_complete_overlay)
    val gameOverText = stringResource(R.string.game_over)

    Canvas(modifier = modifier) {
        val rows = snapshot.board.size
        val cols = snapshot.board.firstOrNull()?.size ?: 1
        val tile = minOf(size.width / cols.toFloat(), size.height / rows.toFloat())
        val boardWidth  = cols * tile
        val boardHeight = rows * tile
        val offsetX = (size.width  - boardWidth)  / 2f
        val offsetY = (size.height - boardHeight) / 2f

        drawRect(BgBoard)

        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val cell = snapshot.board[y][x]
                val left = offsetX + x * tile
                val top  = offsetY + y * tile
                when (cell) {
                    CellKind.Wall    -> drawTile(wallImg,    left, top, tile)
                    CellKind.Bench   -> drawTile(benchImg,   left, top, tile)
                    CellKind.Sofa    -> drawTile(sofaImg,    left, top, tile)
                    CellKind.Floor   -> drawTile(floorImg,   left, top, tile)
                    CellKind.Grade   -> drawTile(gradeImg,   left, top, tile)
                    CellKind.Phone   -> drawTile(phoneImg,   left, top, tile)
                    CellKind.Book    -> drawTile(bookImg,    left, top, tile)
                    CellKind.Quiz    -> drawTile(quizImg,    left, top, tile)
                    CellKind.Balloon -> drawTile(balloonImg, left, top, tile)
                    CellKind.Exit    -> drawTile(exitImg,    left, top, tile)
                }
                drawRect(
                    color = Color(0x18000000),
                    topLeft = Offset(left, top),
                    size = Size(tile, tile),
                    style = Stroke(width = 1f)
                )
            }
        }

        snapshot.enemies.forEach { enemy ->
            val left = offsetX + enemy.pos.x * tile
            val top  = offsetY + enemy.pos.y * tile
            when (enemy.kind) {
                EnemyKind.Teacher  -> drawTile(femaleTeacherImg,  left, top, tile)
                EnemyKind.MiniBoss -> drawTile(femaleTeacherImg2, left, top, tile)
                EnemyKind.Boss     -> drawTile(maleTeacherImg,    left, top, tile)
            }
        }

        val playerLeft = offsetX + snapshot.player.x * tile
        val playerTop  = offsetY + snapshot.player.y * tile

        when (snapshot.playerDirection) {
            "left" -> scale(
                scaleX = -1f, scaleY = 1f,
                pivot  = Offset(playerLeft + tile / 2f, playerTop + tile / 2f)
            ) {
                drawImage(
                    image = playerImg,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(playerImg.width, playerImg.height),
                    dstOffset = IntOffset(playerLeft.toInt(), playerTop.toInt()),
                    dstSize = IntSize(tile.toInt(), tile.toInt()),
                    filterQuality = FilterQuality.None
                )
            }
            else -> drawImage(
                image = playerImg,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(playerImg.width, playerImg.height),
                dstOffset = IntOffset(playerLeft.toInt(), playerTop.toInt()),
                dstSize = IntSize(tile.toInt(), tile.toInt()),
                filterQuality = FilterQuality.None
            )
        }

        if (snapshot.gameOver || snapshot.gameWon || snapshot.state == GameState.LevelComplete) {
            drawRect(Color(0x88000000))
            val text = when {
                snapshot.gameWon -> gameWinText
                snapshot.state == GameState.LevelComplete -> levelCompleteText
                else -> gameOverText
            }
            drawCenteredLabel(
                text, size.width / 2f, size.height / 2f - 60f,
                minOf(size.width, size.height) * 0.09f,
                if (snapshot.gameWon || snapshot.state == GameState.LevelComplete) Color(0xFF00D47A) else Color(0xFFFF4455)
            )
        }
    }
}

private fun DrawScope.drawCenteredLabel(
    text: String, centerX: Float, centerY: Float, textSize: Float, color: Color
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        this.textAlign = Paint.Align.CENTER
        this.textSize = textSize
        this.isFakeBoldText = true
    }
    val fm = paint.fontMetrics
    drawContext.canvas.nativeCanvas.drawText(
        text, centerX, centerY - (fm.ascent + fm.descent) / 2f, paint
    )
}

private fun DrawScope.drawTile(
    image: ImageBitmap,
    left: Float,
    top: Float,
    tile: Float
) {
    drawImage(
        image = image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset(left.toInt(), top.toInt()),
        dstSize = IntSize(tile.toInt(), tile.toInt()),
        filterQuality = FilterQuality.None
    )
}

@Composable
private fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.8f),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.18f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.7f)),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DirectionPad(
onMove: (Int, Int) -> Unit,
onRestart: () -> Unit,
gameActive: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PadButton("↑") { onMove(0, -1) }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PadButton("←") { onMove(-1, 0) }
            if (gameActive) {
                PadButton("R", isRestart = true, onClick = onRestart)
            } else {
                Spacer(Modifier.size(width = 60.dp, height = 50.dp))
            }
            PadButton("→") { onMove(1, 0) }
        }
        Spacer(Modifier.height(4.dp))
        PadButton("↓") { onMove(0, 1) }
    }
}

@Composable
private fun PadButton(label: String, isRestart: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(width = 60.dp, height = 50.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BgCard),
        border = BorderStroke(1.dp, if (isRestart) AccentAmber.copy(0.4f) else Border),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = label,
            fontSize = if (isRestart) 11.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isRestart) AccentAmber else AccentCyan
        )
    }
}

@Composable
fun StartScreen(
    text: String,
    endlessUnlocked: Boolean,
    onStart: () -> Unit,
    onStartEndless: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(BgDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.title_mechanik), fontSize = 42.sp, fontWeight = FontWeight.Black,
                color = AccentCyan, letterSpacing = 4.sp)
            Text(stringResource(R.string.title_dash), fontSize = 42.sp, fontWeight = FontWeight.Black,
                color = AccentAmber, letterSpacing = 8.sp)

            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier.width(140.dp).height(2.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, AccentCyan, Color.Transparent))
                )
            )

            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Border)
            ) {
                Text(
                    text = text, color = TextMuted, fontSize = 13.sp,
                    textAlign = TextAlign.Center, lineHeight = 20.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(Modifier.height(36.dp))

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(stringResource(R.string.btn_start), fontSize = 18.sp, fontWeight = FontWeight.Black,
                    color = BgDeep, letterSpacing = 4.sp)
            }

            if (endlessUnlocked) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onStartEndless,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(stringResource(R.string.btn_try_endless), fontSize = 18.sp, fontWeight = FontWeight.Black,
                        color = BgDeep, letterSpacing = 2.sp)
                }
            }
        }
    }
}
