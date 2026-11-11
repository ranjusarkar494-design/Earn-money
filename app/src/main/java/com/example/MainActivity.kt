package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.ProfileAvatarScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LudoTheme
import com.example.ui.theme.RoyalPurpleDark
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

sealed interface ScreenState {
    data object Home : ScreenState
    data class Game(val gameMode: String, val botPersonality: String = "Aggressive", val stake: Long = 50L) : ScreenState
    data object Wallet : ScreenState
    data object Profile : ScreenState
    data object Leaderboard : ScreenState
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Mandatory App Check wiring with launch intent before UI/ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            LudoTheme {
                AppRootContent()
            }
        }
    }
}

@Composable
fun AppRootContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val user = currentUser!!
        key(user.uid) {
            MainFeatureFlow(user = user)
        }
    }
}

@Composable
fun MainFeatureFlow(user: FirebaseUser) {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val userRepository = remember(user.uid) { UserRepository(context) }

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.Home) }

    // Initialize or fetch profile from Firestore
    LaunchedEffect(user.uid) {
        val profile = userRepository.getOrCreateProfile(
            displayName = user.displayName ?: "Player",
            avatarId = "avatar_guy_smile"
        )
        userProfile = profile
    }

    // Real-time observation of profile
    val liveProfile by userRepository.observeUserProfile(user.uid)
        .collectAsStateWithLifecycle(initialValue = userProfile)

    val transactions by userRepository.observeTransactions(user.uid)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val activeProfile = liveProfile ?: userProfile

    if (activeProfile == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RoyalPurpleDark),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = GoldPrimary)
        }
        return
    }

    when (val screen = currentScreen) {
        is ScreenState.Home -> {
            HomeScreen(
                profile = activeProfile,
                onStartBotMatch = { personality, stake ->
                    currentScreen = ScreenState.Game(
                        gameMode = "bot",
                        botPersonality = personality,
                        stake = stake
                    )
                },
                onStartMultiplayerMatch = { stake ->
                    currentScreen = ScreenState.Game(
                        gameMode = "multiplayer_quick",
                        stake = stake
                    )
                },
                onJoinRoomCode = { _ ->
                    currentScreen = ScreenState.Game(
                        gameMode = "room_code",
                        stake = 50L
                    )
                },
                onOpenWallet = { currentScreen = ScreenState.Wallet },
                onOpenProfile = { currentScreen = ScreenState.Profile },
                onOpenLeaderboard = { currentScreen = ScreenState.Leaderboard },
                onRewardWon = { reward ->
                    coroutineScope.launch {
                        userRepository.recordGameReward(won = true, rewardAmount = reward)
                    }
                }
            )
        }

        is ScreenState.Game -> {
            BackHandler {
                currentScreen = ScreenState.Home
            }
            GameScreen(
                userProfile = activeProfile,
                gameMode = screen.gameMode,
                botPersonality = screen.botPersonality,
                prizePool = screen.stake * 2 - 10,
                onBackClicked = { currentScreen = ScreenState.Home },
                onRewardEarned = { reward ->
                    coroutineScope.launch {
                        userRepository.recordGameReward(won = true, rewardAmount = reward)
                    }
                }
            )
        }

        is ScreenState.Wallet -> {
            BackHandler {
                currentScreen = ScreenState.Home
            }
            WalletScreen(
                userProfile = activeProfile,
                transactions = transactions,
                onDepositCash = { amount, method ->
                    coroutineScope.launch {
                        userRepository.addDeposit(amount, method)
                    }
                },
                onWithdrawCash = { amount, method ->
                    coroutineScope.launch {
                        userRepository.withdrawRewards(amount, method)
                    }
                },
                onBackClicked = { currentScreen = ScreenState.Home }
            )
        }

        is ScreenState.Profile -> {
            BackHandler {
                currentScreen = ScreenState.Home
            }
            ProfileAvatarScreen(
                userProfile = activeProfile,
                onAvatarUpdated = { avatarId, customUri ->
                    coroutineScope.launch {
                        userRepository.updateAvatar(avatarId, customUri)
                    }
                },
                onBackClicked = { currentScreen = ScreenState.Home }
            )
        }

        is ScreenState.Leaderboard -> {
            BackHandler {
                currentScreen = ScreenState.Home
            }
            LeaderboardScreen(
                userProfile = activeProfile,
                onBackClicked = { currentScreen = ScreenState.Home }
            )
        }
    }
}
