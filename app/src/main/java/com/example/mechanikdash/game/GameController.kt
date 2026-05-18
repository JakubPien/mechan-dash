package com.mechanikdash.game

import kotlin.random.Random
import com.mechanikdash.game.R

enum class CellKind { Wall, Bench, Sofa, Floor, Grade, Phone, Book, Quiz, Balloon, Exit }
enum class EnemyKind { Teacher, MiniBoss, Boss }
enum class Axis { Horizontal, Vertical }
enum class MessageType { Normal, Success, Error }

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

data class CellPos(val x: Int, val y: Int)

data class EnemySnapshot(
    val kind: EnemyKind,
    val pos: CellPos,
    val axis: Axis,
    val direction: Int
)

data class GameSnapshot(
    val titleRes: Int,
    val levelIndex: Int,
    val levelCount: Int,
    val levelNameRes: Int,
    val levelNameArgs: List<Any> = emptyList(),
    val requiredGrades: Int,
    val totalGrades: Int,
    val lives: Int,
    val messageRes: Int?,
    val messageArgs: List<Any> = emptyList(),
    val messageType: MessageType = MessageType.Normal,
    val gameOver: Boolean,
    val gameWon: Boolean,
    val board: List<List<CellKind>>,
    val player: CellPos,
    val enemies: List<EnemySnapshot>,
    val playerDirection: String,
    val state: GameState,
    val startScreenTextRes: Int,
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

class GameController(private val levels: List<LevelDefinition>) {
    private val random = Random(System.currentTimeMillis())

    private var levelIndex = 0
    private var totalGrades = 0
    private var lives = 3
    private var messageRes: Int? = null
    private var messageArgs: List<Any> = emptyList()
    private var messageType: MessageType = MessageType.Normal
    private var gameOver = false
    private var gameWon = false
    private var endlessLevelsCompleted = 0
    private var endlessUnlocked = true
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
        require(levels.isNotEmpty()) { "Lista poziomów nie może być pusta." }
        loadLevel(0, resetProgress = true)
    }

    private fun setMessage(resId: Int?, vararg args: Any, type: MessageType = MessageType.Normal) {
        messageRes = resId
        messageArgs = args.toList()
        messageType = type
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
        setMessage(levels.first().introRes)
        return snapshot()
    }

    fun startGame(): GameSnapshot {
        state = GameState.Playing
        setMessage(levels[levelIndex].introRes)
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
            setMessage(R.string.msg_no_path)
            return snapshot()
        }

        val targetTile = board[targetY][targetX]
        val enemyAtTarget = enemyAt(targetX, targetY)

        when {
            enemyAtTarget != null -> {
                when (enemyAtTarget.kind) {
                    EnemyKind.Teacher -> loseLife(R.string.msg_teacher_caught)
                    EnemyKind.MiniBoss -> loseLife(R.string.msg_miniboss_blocked, amount = 2)
                    EnemyKind.Boss -> {
                        if (totalGrades >= currentRequiredGrades()) {
                            gameWon = true
                            state = GameState.Won
                            setMessage(R.string.msg_boss_win, type = MessageType.Success)
                        } else {
                            setMessage(R.string.msg_boss_need_grades, currentRequiredGrades() - totalGrades)
                            loseLife(R.string.msg_boss_lose)
                        }
                    }
                }
            }
            targetTile == CellKind.Wall || targetTile == CellKind.Bench || targetTile == CellKind.Sofa -> {
                setMessage(R.string.msg_wall_blocked)
            }
            targetTile == CellKind.Balloon -> {
                if (dy == 0) {
                    val pushX = targetX + dx
                    val pushY = targetY
                    if (inside(pushX, pushY) && board[pushY][pushX] == CellKind.Floor) {
                        board[pushY][pushX] = CellKind.Balloon
                        board[targetY][targetX] = CellKind.Floor
                        player = CellPos(targetX, targetY)
                        setMessage(R.string.msg_balloon_pushed, type = MessageType.Success)
                        afterPlayerAction()
                    } else setMessage(R.string.msg_balloon_fail)
                } else setMessage(R.string.msg_balloon_side)
            }
            targetTile == CellKind.Exit -> {
                if (totalGrades >= currentRequiredGrades()) advanceLevel()
                else setMessage(R.string.msg_need_more_grades, currentRequiredGrades() - totalGrades)
            }
            targetTile == CellKind.Grade -> {
                player = CellPos(targetX, targetY)
                board[targetY][targetX] = CellKind.Floor
                totalGrades += 1
                graceMoves = 1
                setMessage(R.string.msg_grade_collected, type = MessageType.Success)
                afterPlayerAction()
            }
            targetTile == CellKind.Phone -> {
                board[targetY][targetX] = CellKind.Floor
                val nameRes = levels[levelIndex].nameRes
                val inSala = nameRes == R.string.level_1_name || nameRes == R.string.level_3_name || nameRes == R.string.level_7_name
                val reason = if (inSala) {
                    if (totalGrades > 0) totalGrades--
                    R.string.msg_phone_sala
                } else R.string.msg_phone_distraction
                loseLife(reason)
            }
            targetTile == CellKind.Book -> {
                board[targetY][targetX] = CellKind.Floor
                player = CellPos(targetX, targetY)
                if (lives < 5) lives += 1
                setMessage(R.string.msg_extra_life, type = MessageType.Success)
                afterPlayerAction()
            }
            targetTile == CellKind.Quiz -> {
                board[targetY][targetX] = CellKind.Floor
                loseLife(R.string.msg_quiz_fail)
            }
            targetTile == CellKind.Floor -> {
                player = CellPos(targetX, targetY)
                setMessage(null)
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
                EnemyKind.Teacher -> loseLife(R.string.msg_teacher_collided)
                EnemyKind.MiniBoss -> loseLife(R.string.msg_miniboss_collided, amount = 2)
                EnemyKind.Boss -> {
                    if (totalGrades >= currentRequiredGrades()) {
                        gameWon = true
                        state = GameState.Won
                        setMessage(R.string.msg_boss_win, type = MessageType.Success)
                    } else loseLife(R.string.msg_boss_need_grades_move)
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
                    EnemyKind.Teacher -> loseLife(R.string.msg_teacher_caught_move)
                    EnemyKind.MiniBoss -> loseLife(R.string.msg_miniboss_caught_move, amount = 2)
                    EnemyKind.Boss -> loseLife(R.string.msg_boss_collided, amount = 3)
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
                    if (graceMoves > 0) { graceMoves--; setMessage(R.string.msg_balloon_avoided, type = MessageType.Success) }
                    else loseLife(R.string.msg_balloon_crush)
                }
                if (enemyBelow != null && enemyBelow.kind == EnemyKind.Teacher) {
                    enemies.remove(enemyBelow)
                    setMessage(R.string.msg_balloon_teacher, type = MessageType.Success)
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

    private fun enemyAt(x: Int, y: Int): Enemy? = enemies.firstOrNull { it.pos.x == x && it.pos.y == y }

    private fun loseLife(reasonRes: Int, amount: Int = 1) {
        if (gameOver || gameWon) return
        if (tookDamageThisTurn) return
        tookDamageThisTurn = true
        lives -= amount
        setMessage(reasonRes, type = MessageType.Error)
        if (lives <= 0) {
            lives = 0
            gameOver = true
            state = if (state == GameState.Endless) GameState.EndlessGameOver else GameState.GameOver
            setMessage(R.string.msg_game_over_final, type = MessageType.Error)
        }
    }

    private fun advanceLevel() {
        if (levelIndex >= levels.lastIndex && state != GameState.Endless) {
            gameWon = true
            state = GameState.Won
            return
        }
        if (state == GameState.Endless) {
            endlessLevelsCompleted++
            var newIndex: Int
            do { newIndex = random.nextInt(levels.size - 1) } while (newIndex == lastLevelIndex && repeatCount >= 2)
            if (newIndex == lastLevelIndex) repeatCount++ else repeatCount = 1
            lastLevelIndex = newIndex
            levelIndex = newIndex
            loadLevel(levelIndex, resetProgress = false)
            setMessage(R.string.msg_new_random_level, type = MessageType.Success)
            return
        }
        state = GameState.LevelComplete
        setMessage(R.string.msg_level_complete, type = MessageType.Success)
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

    private fun inside(x: Int, y: Int): Boolean = y in board.indices && x in board[y].indices

    private fun currentRequiredGrades(): Int = if (state == GameState.Endless || state == GameState.EndlessIntro) 3 + (endlessLevelsCompleted * 3) else levels[levelIndex].requiredGrades

    fun snapshot(): GameSnapshot {
        return GameSnapshot(
            titleRes = R.string.app_title,
            levelIndex = levelIndex,
            levelCount = levels.size,
            levelNameRes = if (state == GameState.Endless) R.string.label_endless_name else levels[levelIndex].nameRes,
            levelNameArgs = if (state == GameState.Endless) listOf(levels[levelIndex].nameRes) else emptyList(),
            requiredGrades = currentRequiredGrades(),
            totalGrades = totalGrades,
            lives = lives,
            messageRes = messageRes,
            messageArgs = messageArgs,
            messageType = MessageType.Normal,
            gameOver = gameOver,
            gameWon = gameWon,
            board = board.map { it.toList() },
            player = player,
            enemies = enemies.map { EnemySnapshot(it.kind, it.pos, it.axis, it.direction) },
            playerDirection = playerDirection,
            state = state,
            startScreenTextRes = if (state == GameState.EndlessIntro) R.string.rules_endless else R.string.rules_main,
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
        setMessage(R.string.msg_endless_start, type = MessageType.Success)
        return snapshot()
    }

    fun nextLevel(): GameSnapshot {
        levelIndex += 1
        loadLevel(levelIndex, resetProgress = false)
        state = GameState.Playing
        setMessage(levels[levelIndex].introRes)
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
        setMessage(null)
        return snapshot()
    }
}
