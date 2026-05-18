package com.mechanikdash.game

import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.mechanikdash.game.game.CellKind
import com.mechanikdash.game.game.EnemyKind
import com.mechanikdash.game.game.GameController
import com.mechanikdash.game.game.GameSnapshot
import com.mechanikdash.game.game.GameState
import com.mechanikdash.game.game.LevelRepository
import com.mechanikdash.game.game.MessageKind
import com.mechanikdash.game.game.MusicController

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
        
        // Włącza system Edge-to-Edge dla Androida 15 i niższych
        enableEdgeToEdge()
        
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

    override fun onPause()  { super.onPause(); if (::musicController.isInitialized) musicController.pause() }
    override fun onResume() { super.onResume(); if (::musicController.isInitialized) musicController.resume() }
    override fun onDestroy(){ super.onDestroy(); if (::musicController.isInitialized) musicController.stop() }
}

@Composable
fun MechanikDashApp(musicController: MusicController) {
    val context = LocalContext.current
    val controller = remember { GameController(context, LevelRepository.levels) }
    var snapshot by remember { mutableStateOf(controller.snapshot()) }
    var isMuted by remember { mutableStateOf(false) }

    if (snapshot.state == GameState.StartScreen || snapshot.state == GameState.EndlessIntro) {
        val rulesText = if (snapshot.state == GameState.EndlessIntro) {
            stringResource(R.string.rules_endless)
        } else {
            stringResource(R.string.rules_main)
        }

        StartScreen(
            text = rulesText,
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
            // safeDrawingPadding() gwarantuje, że UI nie nachodzi na paski systemowe
            .safeDrawingPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameHeader(
            snapshot = snapshot,
            isMuted = isMuted,
            onToggleMute = { isMuted = musicController.toggleMute() },
            onMainMenu = { snapshot = controller.goToMainMenu() }
        )

        Spacer(Modifier.height(8.dp))
        StatsBar(snapshot = snapshot)
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.2.dp, Border, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(BgBoard)
        ) {
            GameBoard(snapshot = snapshot, modifier = Modifier.fillMaxSize())

            if (snapshot.gameOver || snapshot.gameWon || snapshot.state == GameState.LevelComplete) {
                Box(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF0060C17))))
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (snapshot.state == GameState.EndlessGameOver) {
                            Text(
                                text = stringResource(R.string.endless_completed_count, snapshot.endlessLevelsCompleted),
                                color = AccentAmber, fontSize = 18.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center
                            )
                        }

                        when (snapshot.state) {
                            GameState.GameOver -> {
                                ActionButton(stringResource(R.string.btn_try_again), AccentCyan) { snapshot = controller.restartGame() }
                            }
                            GameState.Won -> {
                                ActionButton(stringResource(R.string.btn_try_endless), AccentAmber) { snapshot = controller.startEndless() }
                                ActionButton(stringResource(R.string.btn_restart_full), AccentCyan) { snapshot = controller.restartGame() }
                            }
                            GameState.LevelComplete -> {
                                ActionButton(stringResource(R.string.btn_next_level), AccentGreen) { snapshot = controller.nextLevel() }
                            }
                            GameState.EndlessGameOver -> {
                                ActionButton(stringResource(R.string.btn_repeat_endless), AccentAmber) { snapshot = controller.startEndless() }
                                ActionButton(stringResource(R.string.btn_back_to_levels), AccentCyan) { snapshot = controller.restartGame() }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
            if (snapshot.message.isNotBlank()) {
                Text(
                    text = snapshot.message,
                    color = when (snapshot.messageKind) {
                        MessageKind.Negative -> AccentRed
                        MessageKind.Positive -> AccentGreen
                        MessageKind.Neutral  -> TextMuted
                    },
                    textAlign = TextAlign.Center, fontSize = 12.sp, modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        DirectionPad(
            onMove = { dx, dy -> snapshot = controller.move(dx, dy) },
            onRestart = {
                snapshot = if (snapshot.state == GameState.Endless) controller.startEndless() else controller.restartGame()
            },
            gameActive = !snapshot.gameOver && !snapshot.gameWon && snapshot.state != GameState.LevelComplete
        )
        // Odstęp od dolnej krawędzi (np. dla paska gestów)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun GameHeader(snapshot: GameSnapshot, isMuted: Boolean, onToggleMute: () -> Unit, onMainMenu: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = stringResource(R.string.app_title), fontSize = 18.sp, fontWeight = FontWeight.Black, color = AccentCyan, letterSpacing = 1.2.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.background(BgCard, RoundedCornerShape(8.dp)).border(1.dp, Border, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp).clickable { onMainMenu() }) {
                Text(text = stringResource(R.string.icon_home), fontSize = 16.sp)
            }
            Box(modifier = Modifier.background(BgCard, RoundedCornerShape(8.dp)).border(1.dp, Border, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp).clickable { onToggleMute() }) {
                Text(text = if (isMuted) stringResource(R.string.icon_mute) else stringResource(R.string.icon_unmute), fontSize = 16.sp)
            }
            when (snapshot.state) {
                GameState.Playing, GameState.LevelComplete -> Badge(stringResource(R.string.stats_level, snapshot.levelIndex + 1, snapshot.levelCount), AccentAmber)
                GameState.Endless, GameState.EndlessGameOver -> Badge(stringResource(R.string.stats_endless_levels, snapshot.endlessLevelsCompleted), AccentAmber)
                GameState.Won -> Badge(stringResource(R.string.status_completed), AccentGreen)
                else -> {}
            }
        }
    }
}

@Composable
private fun Badge(label: String, color: Color) {
    Box(modifier = Modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatsBar(snapshot: GameSnapshot) {
    Row(modifier = Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(12.dp)).border(1.dp, Border, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = snapshot.levelName, color = TextPri, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatPill(stringResource(R.string.stats_grades), "${snapshot.totalGrades}/${snapshot.requiredGrades}", AccentGreen)
            StatPill(stringResource(R.string.stats_lives), "${snapshot.lives}", AccentRed)
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(modifier = Modifier.background(color.copy(alpha = 0.10f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun GameBoard(snapshot: GameSnapshot, modifier: Modifier = Modifier) {
    val balloonImg       = ImageBitmap.imageResource(R.drawable.balon)
    val femaleTeacherImg = ImageBitmap.imageResource(R.drawable.femaletecher)
    val femaleTeacherImg2= ImageBitmap.imageResource(R.drawable.femaletecher2)
    val maleTeacherImg   = ImageBitmap.imageResource(R.drawable.maletecher)
    val bookImg          = ImageBitmap.imageResource(R.drawable.ksiazk1)
    val gradeImg         = ImageBitmap.imageResource(R.drawable.ocena)
    val quizImg          = ImageBitmap.imageResource(R.drawable.kartkowka)
    val playerImg        = ImageBitmap.imageResource(R.drawable.uczen)
    val exitImg          = ImageBitmap.imageResource(R.drawable.exit)
    val phoneImg         = ImageBitmap.imageResource(R.drawable.telefon)
    val floorImg         = ImageBitmap.imageResource(R.drawable.floor)
    val wallImg          = ImageBitmap.imageResource(R.drawable.wall)
    val benchImg         = ImageBitmap.imageResource(R.drawable.lawka)
    val sofaImg          = ImageBitmap.imageResource(R.drawable.sofa)

    Canvas(modifier = modifier) {
        val rows = snapshot.board.size
        val cols = snapshot.board.firstOrNull()?.size ?: 1
        val tile = minOf(size.width / cols.toFloat(), size.height / rows.toFloat())
        val offsetX = (size.width  - cols * tile)  / 2f
        val offsetY = (size.height - rows * tile) / 2f

        drawRect(BgBoard)
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val cell = snapshot.board[y][x]
                val left = offsetX + x * tile
                val top  = offsetY + y * tile
                when (cell) {
                    CellKind.Wall    -> drawTile(wallImg, left, top, tile)
                    CellKind.Bench   -> drawTile(benchImg, left, top, tile)
                    CellKind.Sofa    -> drawTile(sofaImg, left, top, tile)
                    CellKind.Floor   -> drawTile(floorImg, left, top, tile)
                    CellKind.Grade   -> drawTile(gradeImg, left, top, tile)
                    CellKind.Phone   -> drawTile(phoneImg, left, top, tile)
                    CellKind.Book    -> drawTile(bookImg, left, top, tile)
                    CellKind.Quiz    -> drawTile(quizImg, left, top, tile)
                    CellKind.Balloon -> drawTile(balloonImg, left, top, tile)
                    CellKind.Exit    -> drawTile(exitImg, left, top, tile)
                }
            }
        }

        snapshot.enemies.forEach { enemy ->
            val left = offsetX + enemy.pos.x * tile
            val top  = offsetY + enemy.pos.y * tile
            when (enemy.kind) {
                EnemyKind.Teacher  -> drawTile(femaleTeacherImg, left, top, tile)
                EnemyKind.MiniBoss -> drawTile(femaleTeacherImg2, left, top, tile)
                EnemyKind.Boss     -> drawTile(maleTeacherImg, left, top, tile)
            }
        }

        val pL = offsetX + snapshot.player.x * tile
        val pT = offsetY + snapshot.player.y * tile
        if (snapshot.playerDirection == "left") {
            scale(scaleX = -1f, scaleY = 1f, pivot = Offset(pL + tile / 2f, pT + tile / 2f)) {
                drawTile(playerImg, pL, pT, tile)
            }
        } else drawTile(playerImg, pL, pT, tile)

        if (snapshot.gameOver || snapshot.gameWon || snapshot.state == GameState.LevelComplete) {
            drawRect(Color(0x88000000))
            val text = when {
                snapshot.gameWon -> "GRATULACJE!"
                snapshot.state == GameState.LevelComplete -> "POZIOM ZALICZONY!"
                else -> "KONIEC GRY"
            }
            drawCenteredLabel(text, size.width / 2f, size.height / 2f, minOf(size.width, size.height) * 0.1f, if (snapshot.gameOver) Color.Red else Color.Green)
        }
    }
}

private fun DrawScope.drawTile(img: ImageBitmap, left: Float, top: Float, tile: Float) {
    drawImage(image = img, dstOffset = IntOffset(left.toInt(), top.toInt()), dstSize = IntSize(tile.toInt(), tile.toInt()), filterQuality = FilterQuality.None)
}

private fun DrawScope.drawCenteredLabel(text: String, x: Float, y: Float, size: Float, color: Color) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb(); this.textAlign = Paint.Align.CENTER; this.textSize = size; this.isFakeBoldText = true }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

@Composable
private fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(0.8f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.2f)), border = BorderStroke(1.2.dp, color.copy(alpha = 0.5f))) {
        Text(text, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DirectionPad(onMove: (Int, Int) -> Unit, onRestart: () -> Unit, gameActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PadButton("↑") { if (gameActive) onMove(0, -1) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadButton("←") { if (gameActive) onMove(-1, 0) }
            PadButton("↻", isRestart = true) { onRestart() }
            PadButton("→") { if (gameActive) onMove(1, 0) }
        }
        PadButton("↓") { if (gameActive) onMove(0, 1) }
    }
}

@Composable
private fun PadButton(label: String, isRestart: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick, 
        modifier = Modifier.size(72.dp, 62.dp), 
        shape = RoundedCornerShape(12.dp), 
        colors = ButtonDefaults.buttonColors(containerColor = BgCard), 
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Text(label, fontSize = if (isRestart) 18.sp else 28.sp, color = if (isRestart) AccentAmber else AccentCyan)
    }
}

@Composable
fun StartScreen(text: String, endlessUnlocked: Boolean, onStart: () -> Unit, onStartEndless: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(BgDeep).safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MECHANIK", fontSize = 48.sp, fontWeight = FontWeight.Black, color = AccentCyan)
            Text("DASH", fontSize = 48.sp, fontWeight = FontWeight.Black, color = AccentAmber)
            Spacer(Modifier.height(24.dp))
            Card(colors = CardDefaults.cardColors(containerColor = BgCard), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Border)) {
                Text(text, color = TextMuted, modifier = Modifier.padding(20.dp), textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(40.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)) {
                Text("START", color = BgDeep, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            if (endlessUnlocked) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onStartEndless, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)) {
                    Text("TRYB ENDLESS", color = BgDeep, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }
    }
}
