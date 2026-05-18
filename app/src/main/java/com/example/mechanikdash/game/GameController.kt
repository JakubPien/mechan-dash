package com.mechanikdash.game.game

import android.content.Context
import com.mechanikdash.game.R
import kotlin.random.Random

enum class CellKind { Wall, Bench, Sofa, Floor, Grade, Phone, Book, Quiz, Balloon, Exit }
enum class EnemyKind { Teacher, MiniBoss, Boss }
enum class Axis { Horizontal, Vertical }

enum class GameState {
    StartScreen,
    Playing,
    GameOver,
    Won,
    Endless,
    EndlessGameOver,
    LevelComplete,
    EndlessIntro
}

enum class MessageKind { Neutral, Positive, Negative }

data class CellPos(val x: Int, val y: Int)

data class EnemySnapshot(
    val kind: EnemyKind,
    val pos: CellPos,
    val axis: Axis,
    val direction: Int
)

data class GameSnapshot(
    val title: String,
    val levelIndex: Int,
    val levelCount: Int,
    val levelName: String,
    val requiredGrades: Int,
    val totalGrades: Int,
    val lives: Int,
    val message: String,
    val messageKind: MessageKind,
    val gameOver: Boolean,
    val gameWon: Boolean,
    val board: List<List<CellKind>>,
    val player: CellPos,
    val enemies: List<EnemySnapshot>,
    val playerDirection: String,
    val state: GameState,
    val startScreenText: String,
    val endlessLevelsCompleted: Int,
    val endlessUnlocked: Boolean
)

data class LevelDefinition(
    val nameRes: Int,
    val requiredGrades: Int,
    val introRes: Int,
    val interiorRows: List<String>
)

private data class Enemy(
    var pos: CellPos,
    val kind: EnemyKind,
    var axis: Axis = Axis.Horizontal,
    var direction: Int = 1
)

class GameController(private val context: Context, private val levels: List<LevelDefinition>) {
    private val random = Random(System.currentTimeMillis())

    private var levelIndex = 0
    private var totalGrades = 0
    private var lives = 3
    private var message = ""
    private var messageKind = MessageKind.Neutral
    private var gameOver = false
    private var gameWon = false
    private var endlessLevelsCompleted = 0
    private var endlessUnlocked = false
    private var playerDirection = "right"
    private var graceMoves = 0
    private var state: GameState = GameState.StartScreen
    private var tookDamageThisTurn = false
    private var lastLevelIndex = -1
    private var repeatCount = 0

    private var board: Array<MutableList<CellKind>> = emptyArray()
    private var enemies: MutableList<Enemy> = mutableListOf()
    private var playerStart = CellPos(1, 1)
    private var player = CellPos(1, 1)

    init {
        require(levels.isNotEmpty()) { context.getString(R.string.err_levels_empty) }
        loadLevel(0, resetProgress = true)
    }

    fun restartGame(): GameSnapshot {
        levelIndex = 0
        totalGrades = 0
        lives = 3
        gameOver = false
        gameWon = false
        endlessLevelsCompleted = 0
        state = GameState.Playing
        loadLevel(0, resetProgress = true)
        message = context.getString(levels.first().introRes)
        messageKind = MessageKind.Neutral
        return snapshot()
    }

    fun startGame(): GameSnapshot {
        state = GameState.Playing
        message = context.getString(levels[levelIndex].introRes)
        messageKind = MessageKind.Neutral
        return snapshot()
    }

    fun goToMainMenu(): GameSnapshot {
        levelIndex = 0
        totalGrades = 0
        lives = 3
        gameOver = false
        gameWon = false
        endlessLevelsCompleted = 0
        state = GameState.StartScreen
        loadLevel(0, resetProgress = true)
        message = ""
        messageKind = MessageKind.Neutral
        return snapshot()
    }

    fun move(dx: Int, dy: Int): GameSnapshot {
        if (state != GameState.Playing && state != GameState.Endless) return snapshot()
        if (gameOver || gameWon) return snapshot()

        if (dx == 1) playerDirection = "right"
        if (dx == -1) playerDirection = "left"

        val targetX = player.x + dx
        val targetY = player.y + dy

        if (!inside(targetX, targetY)) {
            message = context.getString(R.string.msg_no_pass)
            messageKind = MessageKind.Negative
            return snapshot()
        }

        val targetTile = board[targetY][targetX]
        val enemyAtTarget = enemyAt(targetX, targetY)

        when {
            enemyAtTarget != null -> {
                when (enemyAtTarget.kind) {
                    EnemyKind.Teacher -> loseLife(context.getString(R.string.msg_teacher_caught))
                    EnemyKind.MiniBoss -> loseLife(context.getString(R.string.msg_miniboss_blocked), amount = 2)
                    EnemyKind.Boss -> {
                        if (totalGrades >= currentRequiredGrades()) {
                            gameWon = true
                            state = GameState.Won
                            endlessUnlocked = true
                            message = context.getString(R.string.msg_scholarship_won)
                            messageKind = MessageKind.Positive
                        } else {
                            message = context.getString(R.string.msg_need_more_grades, currentRequiredGrades() - totalGrades)
                            messageKind = MessageKind.Neutral
                            loseLife(context.getString(R.string.msg_no_grades_no_pass))
                        }
                    }
                }
            }
            targetTile == CellKind.Wall || targetTile == CellKind.Bench || targetTile == CellKind.Sofa -> {
                message = context.getString(R.string.msg_obstacle_blocked)
                messageKind = MessageKind.Neutral
            }
            targetTile == CellKind.Balloon -> {
                if (dy == 0) {
                    val pushX = targetX + dx
                    val pushY = targetY
                    if (inside(pushX, pushY) && board[pushY][pushX] == CellKind.Floor) {
                        board[pushY][pushX] = CellKind.Balloon
                        board[targetY][targetX] = CellKind.Floor
                        player = CellPos(targetX, targetY)
                        message = context.getString(R.string.msg_balloon_pushed)
                        messageKind = MessageKind.Positive
                        afterPlayerAction()
                    } else {
                        message = context.getString(R.string.msg_balloon_cannot_push)
                        messageKind = MessageKind.Negative
                    }
                } else {
                    message = context.getString(R.string.msg_balloon_push_side)
                    messageKind = MessageKind.Negative
                }
            }
            targetTile == CellKind.Exit -> {
                if (totalGrades >= currentRequiredGrades()) advanceLevel()
                else {
                    message = context.getString(R.string.msg_need_grades_exit, currentRequiredGrades() - totalGrades)
                    messageKind = MessageKind.Negative
                }
            }
            targetTile == CellKind.Grade -> {
                player = CellPos(targetX, targetY)
                board[targetY][targetX] = CellKind.Floor
                totalGrades += 1
                graceMoves = 1
                message = context.getString(R.string.msg_grade_collected)
                messageKind = MessageKind.Positive
                afterPlayerAction()
            }
            targetTile == CellKind.Phone -> {
                board[targetY][targetX] = CellKind.Floor
                val levelName = context.getString(levels[levelIndex].nameRes)
                val inSala = levelName.contains("Sala", ignoreCase = true)
                val reason = if (inSala) {
                    if (totalGrades > 0) totalGrades--
                    context.getString(R.string.msg_phone_sala)
                } else context.getString(R.string.msg_phone_distracted)
                loseLife(reason)
            }
            targetTile == CellKind.Book -> {
                board[targetY][targetX] = CellKind.Floor
                player = CellPos(targetX, targetY)
                if (lives < 5) lives += 1
                message = context.getString(R.string.msg_extra_life)
                messageKind = MessageKind.Positive
                afterPlayerAction()
            }
            targetTile == CellKind.Quiz -> {
                board[targetY][targetX] = CellKind.Floor
                loseLife(context.getString(R.string.msg_quiz_lost_life))
            }
            targetTile == CellKind.Floor -> {
                player = CellPos(targetX, targetY)
                message = ""
                messageKind = MessageKind.Neutral
                afterPlayerAction()
            }
        }
        if (graceMoves > 0) graceMoves--
        return snapshot()
    }

    private fun afterPlayerAction() {
        if (gameOver || gameWon) return
        moveEnemies()
        moveBalloons()
        if (gameOver || gameWon) return
        val collidingEnemy = enemyAt(player.x, player.y)
        if (collidingEnemy != null) {
            when (collidingEnemy.kind) {
                EnemyKind.Teacher -> loseLife(context.getString(R.string.msg_teacher_reached))
                EnemyKind.MiniBoss -> loseLife(context.getString(R.string.msg_miniboss_caught), amount = 2)
                EnemyKind.Boss -> {
                    if (totalGrades >= currentRequiredGrades()) {
                        gameWon = true
                        state = GameState.Won
                        endlessUnlocked = true
                        message = context.getString(R.string.msg_scholarship_won)
                        messageKind = MessageKind.Positive
                    } else loseLife(context.getString(R.string.msg_boss_need_grades))
                }
            }
        }
    }

    private fun moveEnemies() {
        tookDamageThisTurn = false
        val updated = mutableListOf<Enemy>()
        for (enemy in enemies) {
            if (gameOver || gameWon) break
            when (enemy.kind) {
                EnemyKind.Teacher -> moveTeacher(enemy)
                EnemyKind.MiniBoss -> moveMiniBoss(enemy)
                EnemyKind.Boss -> {}
            }
            if (enemy.pos == player) {
                when (enemy.kind) {
                    EnemyKind.Teacher -> loseLife(context.getString(R.string.msg_teacher_caught_player))
                    EnemyKind.MiniBoss -> loseLife(context.getString(R.string.msg_miniboss_caught_player), amount = 2)
                    EnemyKind.Boss -> loseLife(context.getString(R.string.msg_boss_caught_player), amount = 3)
                }
            }
            updated.add(enemy)
        }
        enemies = updated
    }

    private fun moveMiniBoss(enemy: Enemy) {
        val dx = player.x - enemy.pos.x
        val dy = player.y - enemy.pos.y
        val stepX = if (dx > 0) 1 else if (dx < 0) -1 else 0
        val stepY = if (dy > 0) 1 else if (dy < 0) -1 else 0
        val tryHorizontalFirst = random.nextBoolean()
        val moved = if (tryHorizontalFirst) tryMove(enemy, stepX, 0) || tryMove(enemy, 0, stepY)
        else tryMove(enemy, 0, stepY) || tryMove(enemy, stepX, 0)
        if (!moved) {
            val directions = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1).shuffled()
            for ((dxRand, dyRand) in directions) if (tryMove(enemy, dxRand, dyRand)) break
        }
    }

    private fun tryMove(enemy: Enemy, dx: Int, dy: Int): Boolean {
        val newX = enemy.pos.x + dx
        val newY = enemy.pos.y + dy
        if (!canEnemyMoveTo(newX, newY)) return false
        enemy.pos = CellPos(newX, newY)
        return true
    }

    private fun moveTeacher(enemy: Enemy) {
        val stepX = if (enemy.axis == Axis.Horizontal) enemy.direction else 0
        val stepY = if (enemy.axis == Axis.Vertical) enemy.direction else 0
        val firstX = enemy.pos.x + stepX
        val firstY = enemy.pos.y + stepY
        if (canEnemyMoveTo(firstX, firstY)) enemy.pos = CellPos(firstX, firstY)
        else enemy.direction *= -1
    }

    private fun moveBalloons() {
        for (y in board.lastIndex - 1 downTo 0) {
            for (x in board[0].indices) {
                if (board[y][x] != CellKind.Balloon) continue
                val belowY = y + 1
                if (board[belowY][x] != CellKind.Floor) continue
                val enemyBelow = enemyAt(x, belowY)
                board[y][x] = CellKind.Floor
                board[belowY][x] = CellKind.Balloon
                if (player.x == x && player.y == belowY) {
                    if (graceMoves > 0) {
                        graceMoves--
                        message = context.getString(R.string.msg_balloon_avoided)
                        messageKind = MessageKind.Positive
                    }
                    else loseLife(context.getString(R.string.msg_balloon_hit_player))
                }
                if (enemyBelow != null && enemyBelow.kind == EnemyKind.Teacher) {
                    enemies.remove(enemyBelow)
                    message = context.getString(R.string.msg_balloon_hit_teacher)
                    messageKind = MessageKind.Positive
                }
            }
        }
    }

    private fun canEnemyMoveTo(x: Int, y: Int): Boolean {
        if (!inside(x, y)) return false
        if (player.x == x && player.y == y) return true
        if (enemyAt(x, y) != null) return false
        return board[y][x] == CellKind.Floor
    }

    private fun enemyAt(x: Int, y: Int): Enemy? =
        enemies.firstOrNull { it.pos.x == x && it.pos.y == y }

    private fun loseLife(reason: String, amount: Int = 1) {
        if (gameOver || gameWon) return
        if (tookDamageThisTurn) return
        tookDamageThisTurn = true
        lives -= amount
        message = reason
        messageKind = MessageKind.Negative
        if (lives <= 0) {
            lives = 0
            gameOver = true
            state = if (state == GameState.Endless) GameState.EndlessGameOver else GameState.GameOver
            message = context.getString(R.string.msg_game_over_grades)
        }
    }

    private fun advanceLevel() {
        if (levelIndex >= levels.lastIndex && state != GameState.Endless) {
            gameWon = true
            state = GameState.Won
            endlessUnlocked = true
            return
        }
        if (state == GameState.Endless) {
            endlessLevelsCompleted++
            var newIndex: Int
            do { newIndex = random.nextInt(levels.size - 1) } while (newIndex == lastLevelIndex && repeatCount >= 2)
            if (newIndex == lastLevelIndex) repeatCount++ else repeatCount = 1
            lastLevelIndex = newIndex
            levelIndex = newIndex
            loadLevel(newIndex, resetProgress = false)
            message = context.getString(R.string.msg_new_random_level)
            messageKind = MessageKind.Positive
            return
        }
        state = GameState.LevelComplete
        message = context.getString(R.string.msg_level_complete_msg)
        messageKind = MessageKind.Positive
    }

    private fun loadLevel(index: Int, resetProgress: Boolean) {
        val level = levels[index]
        val interiorWidth = level.interiorRows.firstOrNull()?.length ?: 0
        val wrapped = buildList {
            val border = "#".repeat(interiorWidth + 2)
            add(border)
            level.interiorRows.forEach { add("#$it#") }
            add(border)
        }
        board = Array(wrapped.size) { MutableList(wrapped[0].length) { CellKind.Floor } }
        enemies = mutableListOf()
        playerStart = CellPos(1, 1)
        wrapped.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                board[y][x] = when (ch) {
                    '#' -> CellKind.Wall
                    's' -> CellKind.Sofa
                    'l' -> CellKind.Bench
                    'p' -> { playerStart = CellPos(x, y); CellKind.Floor }
                    'g' -> CellKind.Grade
                    'o' -> {
                        val phoneChance = if (state == GameState.Endless || state == GameState.EndlessIntro) 0.75f else 0.5f
                        if (random.nextFloat() < phoneChance) CellKind.Phone else CellKind.Book
                    }
                    'q' -> CellKind.Quiz
                    'b' -> CellKind.Balloon
                    'e' -> CellKind.Exit
                    'n' -> { enemies.add(Enemy(CellPos(x, y), EnemyKind.Teacher, if ((x + y) % 2 == 0) Axis.Horizontal else Axis.Vertical)); CellKind.Floor }
                    'm' -> { enemies.add(Enemy(CellPos(x, y), EnemyKind.MiniBoss)); CellKind.Floor }
                    'd' -> { enemies.add(Enemy(CellPos(x, y), EnemyKind.Boss)); CellKind.Floor }
                    else -> CellKind.Floor
                }
            }
        }
        player = playerStart
        if (resetProgress) {
            totalGrades = 0
            lives = 3
            gameOver = false
            gameWon = false
            endlessLevelsCompleted = 0
        }
    }

    private fun inside(x: Int, y: Int): Boolean =
        y in board.indices && x in board[y].indices

    private fun currentRequiredGrades(): Int =
        if (state == GameState.Endless || state == GameState.EndlessIntro)
            3 + (endlessLevelsCompleted * 3)
        else
            levels[levelIndex].requiredGrades

    fun snapshot(): GameSnapshot {
        val desc = if (state == GameState.EndlessIntro) {
            context.getString(R.string.rules_endless)
        } else {
            context.getString(R.string.rules_main)
        }
        val rawName = context.getString(levels[levelIndex].nameRes)
        return GameSnapshot(
            title = context.getString(R.string.app_title),
            levelIndex = levelIndex,
            levelCount = levels.size,
            levelName = if (state == GameState.Endless) context.getString(R.string.stats_endless_prefix, rawName) else rawName,
            requiredGrades = currentRequiredGrades(),
            totalGrades = totalGrades,
            lives = lives,
            message = message,
            messageKind = messageKind,
            gameOver = gameOver,
            gameWon = gameWon,
            board = board.map { it.toList() },
            player = player,
            enemies = enemies.map { EnemySnapshot(it.kind, it.pos, it.axis, it.direction) },
            playerDirection = playerDirection,
            state = state,
            startScreenText = desc,
            endlessLevelsCompleted = endlessLevelsCompleted,
            endlessUnlocked = endlessUnlocked
        )
    }

    fun startEndless(): GameSnapshot {
        state = GameState.EndlessIntro
        val randomIndex = random.nextInt(levels.size - 1)
        levelIndex = randomIndex
        lastLevelIndex = randomIndex
        repeatCount = 1
        totalGrades = 0
        lives = 3
        gameOver = false
        gameWon = false
        endlessLevelsCompleted = 0
        loadLevel(levelIndex, resetProgress = true)
        return snapshot()
    }

    fun confirmEndless(): GameSnapshot {
        state = GameState.Endless
        message = context.getString(R.string.msg_endless_start)
        messageKind = MessageKind.Positive
        return snapshot()
    }

    fun nextLevel(): GameSnapshot {
        levelIndex += 1
        loadLevel(levelIndex, resetProgress = false)
        state = GameState.Playing
        message = context.getString(levels[levelIndex].introRes)
        return snapshot()
    }
}
