package com.example.towerdefense

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.LinkedList
import java.util.PriorityQueue
import java.util.Queue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Step 1: Initialize app with Material 3 theme
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF6200EE),
                    secondary = Color(0xFF03DAC6),
                    background = Color(0xFF121212)
                )
            ) {
                TowerDefenseGame()
            }
        }
    }
}
@Composable
fun TowerDefenseGame(viewModel: GameViewModel = viewModel()) {
    // Step 1: Collect game state
    // Step 2: Tower placement dialog
    // Step 5: Tower upgrade dialog
    // Step 6: Game status
    val gameState by viewModel.gameState.collectAsState()
    var showTowerDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showUpgradeDialog by remember { mutableStateOf<Tower?>(null) }

    // Step 1: Path animation
    // Step 6: Wave transition
    val pathAnimationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 2000, easing = LinearEasing),
        label = "PathAnimation"
    )
    val waveTransitionAlpha by animateFloatAsState(
        targetValue = if (gameState.showWaveTransition) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "WaveTransition"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF121212), Color(0xFF1F1F1F))
                )
            )
    ) {
        if (gameState.gameStatus == GameStatus.Playing) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step 1: Stats card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gold: ${gameState.gold}",
                            color = Color.Yellow,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Base HP: ${gameState.baseHP}",
                            color = Color.Red,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Wave: ${gameState.wave}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Step 1–5: Game grid
                // Step 6: Pass wave parameter
                GameGrid(
                    grid = gameState.grid,
                    path = gameState.path,
                    towers = gameState.towers,
                    enemies = gameState.enemies,
                    pathAnimationProgress = pathAnimationProgress,
                    attackLines = gameState.attackLines,
                    wave = gameState.wave,
                    onTileClick = { row, col ->
                        if (!gameState.isWaveActive) {
                            val tower = gameState.towers.find { it.position == Pair(row, col) }
                            if (tower != null) {
                                showUpgradeDialog = tower // Step 5
                            } else {
                                showTowerDialog = Pair(row, col) // Step 2
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    enemyAttackLines = gameState.enemyAttackLines
                )

                // Step 3: Start Wave button
                Button(
                    onClick = { viewModel.startWave() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    enabled = !gameState.isWaveActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6200EE),
                        disabledContainerColor = Color.Gray
                    )
                ) {
                    Text(
                        text = if (gameState.isWaveActive) "Wave in Progress" else "Start Wave",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Step 2: Tower placement dialog
                showTowerDialog?.let { tile ->
                    TowerSelectionDialog(
                        onDismiss = { showTowerDialog = null },
                        onPlaceTower = {
                            viewModel.placeTower(tile.first, tile.second)
                            showTowerDialog = null
                        },
                        canAffordTower = gameState.gold >= 50
                    )
                }

                // Step 5: Tower upgrade dialog
                showUpgradeDialog?.let { tower ->
                    UpgradeDialog(
                        tower = tower,
                        onDismiss = { showUpgradeDialog = null },
                        onUpgrade = { upgrade ->
                            viewModel.upgradeTower(tower, upgrade)
                            showUpgradeDialog = null
                        },
                        gold = gameState.gold
                    )
                }
            }

            // Step 6: Wave transition
            if (gameState.showWaveTransition) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f * waveTransitionAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Wave ${gameState.wave}",
                        color = Color.White.copy(alpha = waveTransitionAlpha),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            // Step 6: Result screen
            ResultScreen(
                isVictory = gameState.gameStatus == GameStatus.Victory,
                onReplay = { viewModel.resetGame() }
            )
        }
    }
}

@Composable
fun GameGrid(
    grid: Array<Array<Tile>>,
    path: List<Pair<Int, Int>>,
    towers: List<Tower>,
    enemies: List<Enemy>,
    pathAnimationProgress: Float,
    attackLines: List<AttackLine>,
    enemyAttackLines: List<EnemyAttackLine>,
    wave: Int,
    onTileClick: (Int, Int) -> Unit,
    modifier: Modifier
) {
    Canvas(modifier = modifier
        .clickable(enabled = true, onClick = {})
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                val tileWidth = size.width / 15
                val tileHeight = size.height / 15
                val tileSize= max(tileWidth, tileHeight)
                val col = (offset.x / tileSize).toInt()
                val row = (offset.y / tileSize).toInt()
                if (row in 0..14 && col in 0..14) {
                    onTileClick(row, col)
                }
            }
        }
    ) {
        val tileWidth = size.width / 15
        val tileHeight = size.height / 15
        val tileSize= max(tileWidth, tileHeight)
        // Draw grid
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val isPath = path.indexOf(Pair(row, col)) != -1 &&
                        path.indexOf(Pair(row, col)) <= (path.size * pathAnimationProgress).toInt()
                val tower = towers.find { it.position == Pair(row, col) }
                val hasTower = tower != null
                drawRect(
                    color = when {
                        hasTower && tower!!.skillTree.isFullyUpgraded -> Color(0xFFFF9800)
                        hasTower -> Color(0xFFFF5722)
                        else -> Color(0xFF616161)
                    },
                    topLeft = Offset(col * tileSize, row * tileSize),
                    size = Size(tileSize, tileSize)
                )
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(col * tileSize, row * tileSize),
                    size = Size(tileSize, tileSize),
                    style = Stroke(width = 2f)
                )
                // Draw tower HP bar
                if (hasTower) {
                    val hpRatio = tower!!.hp.toFloat() / tower.maxHp
                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(col * tileSize + tileSize / 4, row * tileSize + tileSize / 8 - 10),
                        size = Size(tileSize / 2 * hpRatio, 5f)
                    )
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(col * tileSize + tileSize / 4, row * tileSize + tileSize / 8 - 10),
                        size = Size(tileSize / 2, 5f),
                        style = Stroke(width = 1f)
                    )
                }
            }
        }

        // Draw enemies
        Log.d("GameGrid", "Rendering ${enemies.size} enemies")
        enemies.forEachIndexed { index, enemy ->
            val (x, y) = enemy.position
            drawCircle(
                color = if (enemy.type == EnemyType.TowerEnemy) Color.Blue else Color.Red,
                radius = tileSize / 4,
                center = Offset(y * tileSize + tileSize / 2, x * tileSize + tileSize / 2)
            )
            // Draw enemy HP bar
            val hpRatio = enemy.hp.toFloat() / (50 + (wave - 1) * 10)
            drawRect(
                color = Color.Green,
                topLeft = Offset(y * tileSize + tileSize / 4, x * tileSize + tileSize / 8 - 10),
                size = Size(tileSize / 2 * hpRatio, 5f)
            )
            drawRect(
                color = Color.Black,
                topLeft = Offset(y * tileSize + tileSize / 4, x * tileSize + tileSize / 8 - 10),
                size = Size(tileSize / 2, 5f),
                style = Stroke(width = 1f)
            )
        }

        // Draw tower attack lines (yellow)
        for (line in attackLines) {
            drawLine(
                color = Color.Yellow,
                start = Offset(
                    (line.towerPos.second + 0.5f) * tileSize,
                    (line.towerPos.first + 0.5f) * tileSize
                ),
                end = Offset(
                    (line.enemyPos.second + 0.5f) * tileSize,
                    (line.enemyPos.first + 0.5f) * tileSize
                ),
                strokeWidth = 2f,
                alpha = line.alpha
            )
        }

        // Draw enemy attack lines (blue)
        for (line in enemyAttackLines) {
            drawLine(
                color = Color.Blue,
                start = Offset(
                    (line.enemyPos.second + 0.5f) * tileSize,
                    (line.enemyPos.first + 0.5f) * tileSize
                ),
                end = Offset(
                    (line.towerPos.second + 0.5f) * tileSize,
                    (line.towerPos.first + 0.5f) * tileSize
                ),
                strokeWidth = 2f,
                alpha = line.alpha
            )
        }
    }
}

@Composable
fun TowerSelectionDialog(
    onDismiss: () -> Unit,
    onPlaceTower: () -> Unit,
    canAffordTower: Boolean
) {
    // Step 2: Placement dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Place Tower", style = MaterialTheme.typography.titleLarge) },
        text = { Text("Cost: 50 gold", style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onPlaceTower,
                enabled = canAffordTower,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Place Basic Tower")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
            ) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

@Composable
fun UpgradeDialog(
    tower: Tower,
    onDismiss: () -> Unit,
    onUpgrade: (Upgrade) -> Unit,
    gold: Int
) {
    // Step 5: Upgrade dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upgrade Tower", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text("Available Upgrades:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                tower.skillTree.upgrades.forEach { upgrade ->
                    val canAfford = gold >= upgrade.cost
                    val isLocked = !tower.skillTree.isUpgradeUnlocked(upgrade) && upgrade.dependencies.any { !tower.skillTree.isUpgradeUnlocked(it) }
                    Button(
                        onClick = { onUpgrade(upgrade) },
                        enabled = canAfford && !isLocked,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLocked) Color.Gray else Color(0xFF4CAF50)
                        )
                    ) {
                        Text(
                            text = "${upgrade.name} (${upgrade.cost} gold)" +
                                    if (isLocked) " [Locked]" else "",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
            ) {
                Text("Close")
            }
        },
        containerColor = Color(0xFF1E1E1E),


        )
}

@Composable
fun ResultScreen(isVictory: Boolean, onReplay: () -> Unit) {
    // Step 6: Result screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isVictory) "Victory!" else "Game Over",
                color = if (isVictory) Color.Green else Color.Red,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = onReplay,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
            ) {
                Text("Replay", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}



class GameViewModel : ViewModel() {
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState

    private var enemyIdCounter = 0

    init {
        initializeGame()
    }

    fun startWave() {
        Log.d("GameViewModel", "Starting wave ${_gameState.value.wave}")
        val currentState = _gameState.value

        val newPath = computePathBFS(currentState.grid)
        if (newPath.firstOrNull() != Pair(0, 0) || newPath.lastOrNull() != Pair(14
                , 14)) {
            _gameState.update { it.copy(showInvalidPathMessage = true) }
            Log.e("GameViewModel", "Cannot start wave: No valid path from (0,0) to (9,9)")
            return
        }

        _gameState.update {
            it.copy(
                path = newPath,
                showWaveTransition = true,
                isInPreparationPhase = false
            )
        }
        viewModelScope.launch {
            delay(1500)
            _gameState.update { it.copy(showWaveTransition = false, isWaveActive = true) }
            Log.d("GameViewModel", "Wave transition ended, isWaveActive: true")
            val wave = _gameState.value.wave
            val baseHP = 50 + (wave - 1) * 10
            val baseSpeed = 1f + (wave - 1) * 0.1f
            val waveEnemies = listOf(
                Enemy(id = enemyIdCounter++, hp = baseHP, speed = baseSpeed, position = Pair(0f, 0f), type = EnemyType.BaseEnemy),
                Enemy(id = enemyIdCounter++, hp = baseHP, speed = baseSpeed + 0.2f, position = Pair(0f, 9f), type = EnemyType.TowerEnemy),
                Enemy(id = enemyIdCounter++, hp = baseHP, speed = baseSpeed, position = Pair(0f, 0f), type = EnemyType.BaseEnemy),
                Enemy(id = enemyIdCounter++, hp = baseHP, speed = baseSpeed, position = Pair(0f, 0f), type = EnemyType.BaseEnemy),
                Enemy(id = enemyIdCounter++, hp = baseHP, speed = baseSpeed + 0.2f, position = Pair(5f, 9f), type = EnemyType.TowerEnemy),
                Enemy(id = enemyIdCounter++, hp = baseHP, speed = baseSpeed + 0.2f, position = Pair(5f, 0f), type = EnemyType.TowerEnemy),
            )
            waveEnemies.forEachIndexed { index, enemy ->
                Log.d("GameViewModel", "Spawning enemy ${index + 1} (${enemy.type}) at position ${enemy.position}")
                _gameState.update { state ->
                    state.copy(enemies = state.enemies + enemy)
                }
                delay(500)
            }
            Log.d("GameViewModel", "Finished spawning ${waveEnemies.size} enemies")

            while (_gameState.value.isWaveActive) {
                updateGame()
                delay(100)
            }
            Log.d("GameViewModel", "Wave ended, isWaveActive: ${_gameState.value.isWaveActive}")
        }
    }

    fun placeTower(row: Int, col: Int) {
        Log.d("TOWER_POSITION", "placeTower: $row, $col")
        val currentState = _gameState.value
        if (row in 0..14 && col in 0..14 && currentState.gold >= 50 && !currentState.path.contains(Pair(row, col)) &&
            !currentState.towers.any { it.position == Pair(row, col) } && currentState.isInPreparationPhase) {
            val newGrid = currentState.grid.map { it.copyOf() }.toTypedArray()
            newGrid[row][col] = Tile(isObstacle = true)

            val newTower = Tower(position = Pair(row, col), skillTree = createSkillTree())
            _gameState.update { state ->
                state.copy(
                    gold = state.gold - 50,
                    grid = newGrid,
                    towers = state.towers + newTower
                )
            }
            Log.d("GameViewModel", "Tower placed at ($row, $col) with lastShotTime: ${newTower.lastShotTime}, hp: ${newTower.hp}, grid updated, path unchanged")
        } else {
            Log.d("GameViewModel", "Cannot place tower at ($row, $col): Invalid placement")
        }
    }

    fun upgradeTower(tower: Tower, upgrade: Upgrade) {
        val currentState = _gameState.value
        if (currentState.gold >= upgrade.cost && !tower.skillTree.isUpgradeUnlocked(upgrade) &&
            upgrade.dependencies.all { tower.skillTree.isUpgradeUnlocked(it) }) {
            _gameState.update { state ->
                val newTowers = state.towers.map { t ->
                    if (t == tower) {
                        val newDamage = t.damage + when (upgrade.name) {
                            "Damage +5" -> 5
                            "Damage +10" -> 10
                            else -> 0
                        }
                        t.copy(
                            damage = newDamage,
                            skillTree = t.skillTree.copy(
                                unlockedUpgrades = t.skillTree.unlockedUpgrades + upgrade
                            )
                        )
                    } else {
                        t
                    }
                }
                state.copy(
                    gold = state.gold - upgrade.cost,
                    towers = newTowers
                )
            }
        }
    }

    fun resetGame() {
        initializeGame()
    }

    private suspend fun updateGame() {
        _gameState.update { state ->
            val curr= LocalTime.now();
            val currentTime = curr.toSecondOfDay()+curr.nano/ 1_000_000_000.0
            // Move enemies
            val updatedEnemies = state.enemies.mapNotNull { enemy ->
                val (newX, newY, isAlive) = moveEnemy(enemy, state.path, state.towers)
                Log.d("GameViewModel", "Moving enemy ${enemy.id} (${enemy.type}) from ${enemy.position} to ($newX, $newY), isAlive: $isAlive")
                if (isAlive) {
                    enemy.copy(position = Pair(newX, newY))
                } else {
                    null
                }
            }

            val newBaseHP = if (updatedEnemies.size < state.enemies.size) {
                state.baseHP - (state.enemies.size - updatedEnemies.size)
            } else {
                state.baseHP
            }

            val newEnemies = updatedEnemies.toList()
            Log.d("GameViewModel", "Processing towers with ${newEnemies.size} enemies")
            val attackLines = mutableListOf<AttackLine>()
            val enemyAttackLines = mutableListOf<EnemyAttackLine>()

            // Process tower attacks asynchronously
            val towerAttackResults = coroutineScope {
                state.towers.map { tower ->
                    async {
                        updateTower(tower, newEnemies, state.path, currentTime)
                    }
                }.map { it.await() }
            }

            // Aggregate tower attack damage
            val damageMap = mutableMapOf<Int, Int>() // enemy.id -> total damage
            val attackingTowers = mutableMapOf<Int, List<Pair<Pair<Int, Int>, Int>>>() // enemy.id -> List<(tower.position, damage)>
            towerAttackResults.forEach { (updatedTower, targetEnemy, damageDealt) ->
                if (damageDealt > 0 && targetEnemy != null) {
                    damageMap[targetEnemy.id] = (damageMap[targetEnemy.id] ?: 0) + damageDealt
                    attackingTowers[targetEnemy.id] = (attackingTowers[targetEnemy.id] ?: emptyList()) + Pair(updatedTower.position, damageDealt)
                    attackLines.add(
                        AttackLine(
                            towerPos = updatedTower.position,
                            enemyPos = targetEnemy.position,
                            alpha = 1f
                        )
                    )
                }
            }

            // Log tower attacks
            damageMap.forEach { (enemyId, totalDamage) ->
                val towers = attackingTowers[enemyId]?.joinToString { "Tower at ${it.first} (damage: ${it.second})" } ?: "None"
                Log.d("GameViewModel", "Enemy $enemyId takes $totalDamage damage from: $towers")
            }

            // Process tower damage from TowerEnemies
            val towerDamageResults = updateTowers(state.towers, newEnemies, currentTime, enemyAttackLines)

            // Apply tower attack damage to enemies
            val finalEnemies = newEnemies.mapNotNull { enemy ->
                val totalDamage = damageMap[enemy.id] ?: 0
                if (totalDamage > 0) {
                    val newHP = enemy.hp - totalDamage
                    if (newHP <= 0) {
                        Log.d("GameViewModel", "Enemy ${enemy.id} (${enemy.type}) killed at ${enemy.position}")
                        null
                    } else {
                        enemy.copy(hp = newHP)
                    }
                } else {
                    enemy
                }
            }

            // Collect updated towers (combine attack and damage results)
            val updatedTowersFromAttacks = towerAttackResults.map { it.first }
            val finalTowers = towerDamageResults.first.map { tower ->
                updatedTowersFromAttacks.find { it.position == tower.position }?.copy(hp = tower.hp) ?: tower
            }

            val kills = state.enemies.size - finalEnemies.size - (state.baseHP - newBaseHP)
            val newGold = state.gold + kills * 10
            val isWaveActive = finalEnemies.isNotEmpty()
            val newWave = if (!isWaveActive && state.wave < 10) state.wave + 1 else state.wave
            val gameStatus = when {
                newBaseHP <= 0 -> GameStatus.GameOver
                newWave > 10 -> GameStatus.Victory
                else -> GameStatus.Playing
            }

            state.copy(
                enemies = finalEnemies,
                towers = finalTowers,
                baseHP = newBaseHP,
                gold = newGold,
                isWaveActive = isWaveActive,
                wave = newWave,
                attackLines = attackLines,
                enemyAttackLines = enemyAttackLines,
                gameStatus = gameStatus
            )
        }
    }

    private fun updateTower(tower: Tower, enemies: List<Enemy>, path: List<Pair<Int, Int>>, currentTime: Double): Triple<Tower, Enemy?, Int> {
        Log.d("E_KILL", "updateTower: tower at ${tower.position}, time: $currentTime, lastShot: ${tower.lastShotTime}, fireRate: ${tower.fireRate}")
        if (currentTime - tower.lastShotTime < 0.5f) {
            Log.d("E_KILL", "updateTower: tower at ${tower.position} cannot fire yet")
            return Triple(tower, null, 0)
        }

        val pq = PriorityQueue<Enemy> { e1, e2 ->
            val index1 = path.indexOfFirst { (x, y) ->
                abs(e1.position.first - x) < 0.5f && abs(e1.position.second - y) < 0.5f
            }.takeIf { it != -1 } ?: path.size
            val index2 = path.indexOfFirst { (x, y) ->
                abs(e2.position.first - x) < 0.5f && abs(e2.position.second - y) < 0.5f
            }.takeIf { it != -1 } ?: path.size
            Log.d("KILL", "Comparing enemies ${e1.id} (index $index1) and ${e2.id} (index $index2) for tower ${tower.position}")
            index2.compareTo(index1)
        }

        enemies.forEach { enemy ->
            val distance = abs(tower.position.first - enemy.position.first) +
                    abs(tower.position.second - enemy.position.second)
            Log.d("KILL", "Checking enemy ${enemy.id} (${enemy.type}) at ${enemy.position}, distance from tower ${tower.position}: $distance")
            if (distance <= tower.range) {
                pq.offer(enemy)
                Log.d("KILL", "Enemy ${enemy.id} added to PQ for tower ${tower.position}")
            }
        }

        Log.d("KILL", "Tower at ${tower.position} has ${pq.size} enemies in PQ")
        val target = pq.poll()
        if (target != null) {
            Log.d("CHECK_TIME", "Tower at ${tower.position} targeting enemy ${target.id} (${target.type}) at $currentTime, lastShot: ${tower.lastShotTime}")
            return Triple(
                tower.copy(lastShotTime = currentTime),
                target,
                tower.damage
            )
        }
        Log.d("KILL", "Tower at ${tower.position} found no valid target")
        return Triple(tower, null, 0)
    }

    private fun updateTowers(towers: List<Tower>, enemies: List<Enemy>, currentTime: Double, enemyAttackLines: MutableList<EnemyAttackLine>): Pair<List<Tower>, List<Enemy>> {
        val towerDamageMap = mutableMapOf<Pair<Int, Int>, Int>() // tower.position -> total damage
        val updatedEnemies = enemies.toMutableList()

        enemies.forEachIndexed { index, enemy ->
            if (enemy.type == EnemyType.TowerEnemy) {
                if (currentTime - enemy.lastShotTime < 1f) {
                    Log.d("GameViewModel", "Enemy ${enemy.id} (TowerEnemy) cannot attack yet, lastShot: ${enemy.lastShotTime}")
                    return@forEachIndexed
                }
                val towerPos = towers.minByOrNull {
                    abs(it.position.first - enemy.position.first) +
                            abs(it.position.second - enemy.position.second)
                }?.position
                if (towerPos != null) {
                    val distance = abs(towerPos.first - enemy.position.first) +
                            abs(towerPos.second - enemy.position.second)
                    if (distance <= enemy.enemyRange) { // Enemy is in range of tower
                        towerDamageMap[towerPos] = (towerDamageMap[towerPos] ?: 0) + enemy.towerDamage
                        updatedEnemies[index] = enemy.copy(lastShotTime = currentTime)
                        enemyAttackLines.add(
                            EnemyAttackLine(
                                enemyPos = enemy.position,
                                towerPos = towerPos,
                                alpha = 1f
                            )
                        )
                        Log.d("GameViewModel", "Enemy ${enemy.id} (TowerEnemy) dealing ${enemy.towerDamage} damage to tower at $towerPos at time $currentTime")
                    }
                }
            }
        }

        val updatedTowers = towers.mapNotNull { tower ->
            val damageTaken = towerDamageMap[tower.position] ?: 0
            if (damageTaken > 0) {
                val newHP = tower.hp - damageTaken
                if (newHP <= 0) {
                    Log.d("GameViewModel", "Tower at ${tower.position} destroyed")
                    null
                } else {
                    tower.copy(hp = newHP)
                }
            } else {
                tower
            }
        }

        return Pair(updatedTowers, updatedEnemies)
    }

    private fun moveEnemy(enemy: Enemy, path: List<Pair<Int, Int>>, towers: List<Tower>): Triple<Float, Float, Boolean> {
        if (path.size < 2) {
            Log.e("GameViewModel", "Invalid path: $path")
            return Triple(enemy.position.first, enemy.position.second, true)
        }

        val currentPos = enemy.position
        val speed = enemy.speed * 0.1f

        if (enemy.type == EnemyType.TowerEnemy && towers.isNotEmpty()) {
            // Find nearest tower
            val targetTower = towers.minByOrNull {
                abs(it.position.first - currentPos.first) +
                        abs(it.position.second - currentPos.second)
            }
            if (targetTower != null) {
                val (targetX, targetY) = targetTower.position
                Log.d("GameViewModel", "Enemy ${enemy.id} (TowerEnemy) targeting tower at ($targetX, $targetY)")
                val dx = targetX.toFloat() - currentPos.first
                val dy = targetY.toFloat() - currentPos.second
                val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (distance < 0.01f) {
                    return Triple(targetX.toFloat(), targetY.toFloat(), true)
                }
                if (distance <= speed) {
                    return Triple(targetX.toFloat(), targetY.toFloat(), true)
                } else {
                    val t = speed / distance
                    val newX = currentPos.first + dx * t
                    val newY = currentPos.second + dy * t
                    Log.d("GameViewModel", "Enemy ${enemy.id} (TowerEnemy) moving to ($newX, $newY)")
                    return Triple(newX, newY, true)
                }
            }
        }

        // BaseEnemy or TowerEnemy with no towers left
        val currentIndex = path.indices.minByOrNull { i ->
            val (x, y) = path[i]
            val dx = currentPos.first - x
            val dy = currentPos.second - y
            dx * dx + dy * dy
        } ?: 0

        Log.d("GameViewModel", "Enemy ${enemy.id} (${enemy.type}) at $currentPos, path index: $currentIndex")

        if (currentIndex >= path.size - 1) {
            Log.d("GameViewModel", "Enemy ${enemy.id} (${enemy.type}) reached end of path at ${enemy.position}")
            return Triple(currentPos.first, currentPos.second, false)
        }

        val nextPoint = path[currentIndex + 1]
        val (targetX, targetY) = nextPoint
        Log.d("GameViewModel", "Enemy ${enemy.id} (${enemy.type}) targeting next point: ($targetX, $targetY)")
        val dx = targetX.toFloat() - currentPos.first
        val dy = targetY.toFloat() - currentPos.second
        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (distance < 0.01f) {
            return Triple(targetX.toFloat(), targetY.toFloat(), true)
        }
        if (distance <= speed) {
            return Triple(targetX.toFloat(), targetY.toFloat(), true)
        } else {
            val t = speed / distance
            val newX = currentPos.first + dx * t
            val newY = currentPos.second + dy * t
            Log.d("GameViewModel", "Enemy ${enemy.id} (${enemy.type}) moving to ($newX, $newY)")
            return Triple(newX, newY, true)
        }
    }

    private fun initializeGame() {
        val grid = Array(15) { Array(15) { Tile(isObstacle = false) } }

        _gameState.value = GameState(
            grid = grid,
            gold = 300,
            baseHP = 10,
            wave = 1,
            isInPreparationPhase = true,
            showInvalidPathMessage = false
        )
    }

    private fun computePathBFS(grid: Array<Array<Tile>>): List<Pair<Int, Int>> {
        val start = Pair(0, 0)
        val end = Pair(14, 14)
        val queue: Queue<Pair<Int, Int>> = LinkedList()
        val visited = mutableSetOf<Pair<Int, Int>>()
        val parent = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()
        queue.add(start)
        visited.add(start)

        if (grid[0][0].isObstacle || grid[14][14].isObstacle) {
            Log.e("GameViewModel", "Start or end is blocked, using fallback path")
            return generateFallbackPath()
        }

        val directions = listOf(
            Pair(0, 1), Pair(1, 0), Pair(0, -1), Pair(-1, 0)
        )

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            if (current == end) break
            for (dir in directions) {
                val next = Pair(current.first + dir.first, current.second + dir.second)
                if (next.first in 0..14 && next.second in 0..14 && !visited.contains(next) && !grid[next.first][next.second].isObstacle) {
                    queue.add(next)
                    visited.add(next)
                    parent[next] = current
                }
            }
        }

        val path = mutableListOf<Pair<Int, Int>>()
        var current: Pair<Int, Int>? = end
        while (current != null) {
            path.add(current)
            current = parent[current]
        }
        val finalPath = path.reversed()
        if (finalPath.firstOrNull() != start || finalPath.lastOrNull() != end) {
            Log.e("GameViewModel", "BFS failed, using fallback path")
            return generateFallbackPath()
        }
        Log.d("GameViewModel", "Computed path: $finalPath")
        return finalPath
    }

    private fun generateFallbackPath(): List<Pair<Int, Int>> {
        val path = mutableListOf<Pair<Int, Int>>()
        // Simple path: down to (16,0), then right to (16,9)
        for (i in 0..14) {
            path.add(Pair(i, 0))
        }
        for (j in 1..14) {
            path.add(Pair(14, j))
        }
        Log.d("GameViewModel", "Generated fallback path: $path")
        return path
    }

    private fun createSkillTree(): SkillTree {
        val upgrade1 = Upgrade(name = "Damage +5", cost = 20, dependencies = emptyList())
        val upgrade2 = Upgrade(name = "Damage +10", cost = 30, dependencies = listOf(upgrade1))
        return SkillTree(upgrades = listOf(upgrade1, upgrade2))
    }
}


data class GameState(
    val grid: Array<Array<Tile>> = Array(15) { Array(15) { Tile() } },
    val path: List<Pair<Int, Int>> = emptyList(),
    val gold: Int = 300,
    val baseHP: Int = 10,
    val wave: Int = 1,
    val towers: List<Tower> = emptyList(),
    val enemies: List<Enemy> = emptyList(),
    val isWaveActive: Boolean = false,
    val attackLines: List<AttackLine> = emptyList(),
    val enemyAttackLines: List<EnemyAttackLine> = emptyList(), // New: TowerEnemy attack lines
    val showWaveTransition: Boolean = false,
    val gameStatus: GameStatus = GameStatus.Playing,
    val isInPreparationPhase: Boolean = true,
    val showInvalidPathMessage: Boolean = false
)

data class Tile(val isObstacle: Boolean = false)

data class Tower(
    val position: Pair<Int, Int>,
    val range: Float = 2f,
    val damage: Int = 10,
    val fireRate: Float = 1f,
    val lastShotTime: Double = 0.0,
    val skillTree: SkillTree = SkillTree(),
    val hp: Int = 50,
    val maxHp: Int = 50
)

data class Enemy(
    val id: Int,
    val hp: Int,
    val speed: Float,
    val position: Pair<Float, Float>,
    val type: EnemyType = EnemyType.BaseEnemy,
    val towerDamage: Int = if (type == EnemyType.TowerEnemy) 5 else 0,
    val enemyRange: Float = if (type == EnemyType.TowerEnemy) 1f else 0f,
    val lastShotTime: Double = 0.0
)

data class AttackLine(
    val towerPos: Pair<Int, Int>,
    val enemyPos: Pair<Float, Float>,
    val alpha: Float
)

data class EnemyAttackLine( // New: For TowerEnemy attacking towers
    val enemyPos: Pair<Float, Float>,
    val towerPos: Pair<Int, Int>,
    val alpha: Float
)

data class Upgrade(
    val name: String,
    val cost: Int,
    val dependencies: List<Upgrade>
)

data class SkillTree(
    val upgrades: List<Upgrade> = emptyList(),
    val unlockedUpgrades: List<Upgrade> = emptyList()
) {
    val isFullyUpgraded: Boolean
        get() = upgrades.all { isUpgradeUnlocked(it) }

    fun isUpgradeUnlocked(upgrade: Upgrade): Boolean {
        return unlockedUpgrades.contains(upgrade)
    }
}

enum class GameStatus {
    Playing, Victory, GameOver
}

enum class EnemyType {
    BaseEnemy,
    TowerEnemy
}
