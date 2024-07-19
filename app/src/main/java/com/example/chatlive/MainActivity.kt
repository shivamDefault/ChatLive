package com.example.chatlive

import SingleChatScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chatlive.Screens.ChatListScreen
import com.example.chatlive.Screens.LoginScreen
import com.example.chatlive.Screens.ProfileScreen
import com.example.chatlive.Screens.SignUpScreen
import com.example.chatlive.Screens.SingleStatusScreen
import com.example.chatlive.Screens.StatusScreen
import com.example.chatlive.ui.theme.ChatLiveTheme
import dagger.hilt.android.AndroidEntryPoint

sealed class DestinationScreen(var route: String) {
    object SignUp : DestinationScreen("signup")
    object Login : DestinationScreen("login")
    object Profile : DestinationScreen("profile")
    object ChatList : DestinationScreen("chatList")
    object SingleChat : DestinationScreen("singleChat/{chatId}") {
        fun createRoute(id: String) = "singleChat/$id"
    }

    object StatusList : DestinationScreen("StatusList")
    object SingleStatus : DestinationScreen("singleStatus/{userId}") {
        fun createRoute(userId: String) = "singleStatus/$userId"
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatLiveTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatAppNavigation()
                }
            }
        }
    }

    @Composable
    fun ChatAppNavigation() {
        val navController = rememberNavController()
        val vm = hiltViewModel<ViewModel>()
        NavHost(navController = navController, startDestination = DestinationScreen.SignUp.route) {
            composable(DestinationScreen.SignUp.route) {
                SignUpScreen(navController, vm)
            }
            composable(DestinationScreen.Login.route) {
                LoginScreen(navController = navController, vm = vm)
            }
            composable(DestinationScreen.ChatList.route) {
                ChatListScreen(navController = navController, vm = vm)
            }
            composable(
                DestinationScreen.SingleChat.route,
                arguments = listOf(navArgument("chatId") {
                    type = NavType.StringType
                })
            ) { navBackStackEntry ->
                val chatId = navBackStackEntry.arguments?.getString("chatId")
                chatId?.let {
                    SingleChatScreen(navController = navController, vm = vm, chatId = chatId)
                } ?: kotlin.run {
                    // Handle null chatId
                }
            }
            composable(DestinationScreen.StatusList.route) {
                StatusScreen(navController = navController, vm = vm)
            }
            composable(DestinationScreen.Profile.route) {
                ProfileScreen(navController = navController, vm = vm)
            }
            composable(
                DestinationScreen.SingleStatus.route,
                arguments = listOf(navArgument("userId") {
                    type = NavType.StringType
                })
            ) {
                val userId = it.arguments?.getString("userId")
                userId?.let {
                    SingleStatusScreen(navController = navController, vm = vm, userId = it)
                }
            }
        }
    }
}
