package com.example.chatlive.Screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chatlive.DestinationScreen
import com.example.chatlive.ViewModel
import com.example.chatlive.ui.theme.CommonDivider
import com.example.chatlive.ui.theme.CommonImage
import com.example.chatlive.ui.theme.CommonProgressBar
import com.example.chatlive.ui.theme.navigateTo

@Composable
fun ProfileScreen(navController: NavController, vm: ViewModel) {
    val inProcess = vm.inProcess.value
    if (inProcess) {
        CommonProgressBar()
    } else {
        val userData = vm.userData.value
        var name by rememberSaveable {
            mutableStateOf(userData?.name ?: "")
        }
        var number by rememberSaveable {
            mutableStateOf(userData?.number ?: "")
        }
        Column {
            ProfileContent(modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
                vm = vm,
                name = name,
                number = number,
                onNameChange = { name = it },
                onNumberChange = { number = it },
                onSave = {
                    vm.createOrUpdateProfile(name = name, number = number)
                },
                onBack = {
                    navigateTo(
                        navController = navController, route = DestinationScreen.ChatList.route
                    )
                },
                onLogOut = {
                    vm.logOut()
                    navigateTo(navController = navController, route = DestinationScreen.Login.route)
                }

            )
            BottomNavigationMenu(
                selectedItem = BottomNavigationItem.PROFILE, navController = navController
            )
        }

    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    modifier: Modifier,
    vm: ViewModel,
    onBack: () -> Unit,
    onSave: () -> Unit,
    name: String,
    number: String,
    onNameChange: (String) -> Unit,
    onNumberChange: (String) -> Unit,
    onLogOut: () -> Unit
) {
    val imageUrl = vm.userData.value?.imageUrl
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Back", Modifier.clickable {
                    onBack.invoke()
                }, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Save",
                Modifier.clickable {
                    onSave.invoke()
                },
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        CommonDivider()
        ProfileImage(imageUrl, vm)
        CommonDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Name",
                modifier = Modifier.width(100.dp),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold
            )
            TextField(
                value = name,
                onValueChange = onNameChange,
                colors = TextFieldDefaults.textFieldColors(
                    focusedTextColor = Color.Black, containerColor = Color.Transparent

                )
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Number",
                modifier = Modifier.width(100.dp),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
            )
            TextField(
                value = number,
                onValueChange = onNumberChange,
                colors = TextFieldDefaults.textFieldColors(
                    focusedTextColor = Color.Black, containerColor = Color.Transparent
                )
            )
        }

        CommonDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "LOG OUT",
                modifier = Modifier.clickable { onLogOut.invoke() },
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold
            )

        }


    }
}


@Composable
fun ProfileImage(imageUrl: String?, vm: ViewModel) {
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                vm.uploadProfileImage(uri)
            }
        }
    Box(modifier = Modifier.height(intrinsicSize = IntrinsicSize.Min)) {
        Column(
            modifier = Modifier
                .padding(40.dp)
                .fillMaxWidth()
                .clickable {
                    launcher.launch("image/*")
                }, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = CircleShape, modifier = Modifier
                    .padding(8.dp)
                    .size(200.dp)
            ) {
                CommonImage(data = imageUrl) // Pass imageUrl here
            }
            Text(
                text = "Change Profile Picture",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold
            )
        }
        if (vm.inProcess.value) {
            CommonProgressBar()
        }
    }
}
