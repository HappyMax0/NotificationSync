package com.happymax.notificationsync

import android.Manifest
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.happymax.basicscodelab.ui.theme.NotificationSyncTheme
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

enum class NotiSyncScreen(@StringRes val title:Int) {
    Welcome(title = R.string.welcome_screen_title),
    Server(title = R.string.server),
    Client(title = R.string.client),
    Settings(title = R.string.settings),
    Reset(title = R.string.reset_screen_title)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askNotificationPermission()

        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)

        setContent {
            NotificationSyncTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    color = MaterialTheme.colorScheme.background
                ){
                    MainScreen(sharedPreferences = sharedPreferences)
                }

            }
        }

    }

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(sharedPreferences: SharedPreferences, viewModel: MainScreenViewModel, modifier: Modifier = Modifier){
    //创建一个绑定MoviesScreen生命周期的协程作用域
    val scope = rememberCoroutineScope()

    val options = mapOf(R.string.server to R.string.serverDescription, R.string.client to R.string.clientDescription)
    var selectedOption by remember { mutableStateOf(R.string.server) }

    Column(modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally) {
        TopAppBar(title = {
            Text(text = stringResource(id = R.string.welcome_screen_title), modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 10.dp)
                .width(200.dp),
                style = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.Bold))
        }, modifier = Modifier, colors = TopAppBarColors(containerColor = MaterialTheme.colorScheme.primary, actionIconContentColor = Color.White, navigationIconContentColor = Color.White,
            scrolledContainerColor = Color.White, titleContentColor = Color.White))

        Spacer(modifier = Modifier
            .width(20.dp)
            .height(50.dp))
        options.forEach { option ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp),
                color = if (option.key.equals(selectedOption)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSecondary
            ){
                Row(modifier = Modifier
                    .width(200.dp)
                    .height(150.dp)
                    .align(Alignment.Start)) {
                    RadioButton(
                        selected = option.key.equals(selectedOption),
                        onClick = {
                            selectedOption = option.key
                        },
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                        Text(text = stringResource(option.key), modifier = Modifier
                            .padding(0.dp, 10.dp)
                            .align(Alignment.Start),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize =20.sp)
                        )
                        Text(text = stringResource(option.value), modifier = Modifier
                            .weight(4f)
                            .padding(0.dp, 10.dp),
                            style = MaterialTheme.typography.bodyLarge)
                    }

                }
            }

            Spacer(modifier = Modifier
                .width(20.dp)
                .height(10.dp))
        }
        Button(modifier = Modifier.padding(vertical = 24.dp), onClick = {
            var isClient = selectedOption.equals(R.string.client)

            val editor = sharedPreferences.edit()
            editor.putBoolean("IsInited", true)
            editor.putBoolean("IsClient", isClient)
            editor.commit()

            viewModel.isClient.value = isClient
            viewModel.isInited.value = true

            val token = sharedPreferences.getString("Token", "")
            if(token != null){
                viewModel.token.value = token
            }
        }) {
            Text(stringResource(id = R.string.continue_button_text))
        }
    }
}

@Composable
fun ShowNotication(name: String, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable {mutableStateOf(false)}
    val extraPadding by animateDpAsState(if (expanded) 48.dp else 0.dp, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow)
    )

    Surface(color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 4.dp, horizontal = 8.dp),
        content =  {
            Row(modifier = Modifier
                .padding(24.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )){
                Column(modifier = Modifier
                    .weight(1f)) {
                    Text(
                        text = "Hello",
                    )
                    Text(
                        text = "$name!",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
                ElevatedButton(onClick = { expanded = !expanded }) {
                    Text(if(expanded) "Show less" else "Show more")
                }
            }

        })
}


@Composable
fun NoticationList(modifier: Modifier = Modifier, names:List<String> = List(50){ "$it" }){
    LazyColumn(modifier = modifier.padding(vertical = 4.dp)) {
        items(items = names){name ->
            ShowNotication(name = name)
        }
    }
}

@Composable
fun MainScreen(navController: NavHostController = rememberNavController(),
               sharedPreferences: SharedPreferences,
               modifier: Modifier = Modifier.fillMaxSize()){
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = NotiSyncScreen.valueOf(
        backStackEntry?.destination?.route ?: NotiSyncScreen.Welcome.name
    )

    val viewModel : MainScreenViewModel = viewModel()
    viewModel.isInited.value = sharedPreferences.getBoolean("IsInited", false)

    var startDestination = NotiSyncScreen.Welcome.name

    if(!viewModel.isInited.value){
        startDestination = NotiSyncScreen.Welcome.name
    }
    else{
        viewModel.isClient.value = sharedPreferences.getBoolean("IsClient", false)

        if(viewModel.isClient.value){
            startDestination = NotiSyncScreen.Client.name
        }
        else{
            startDestination = NotiSyncScreen.Server.name
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier
    ) {
        composable(route = NotiSyncScreen.Welcome.name) {
            WelcomeScreen(sharedPreferences, viewModel)
        }

        composable(route = NotiSyncScreen.Server.name) {
            ServerScreen(sharedPreferences, viewModel, onSettingsButtonClicked = {
                navController.navigate(NotiSyncScreen.Settings.name)
            })
        }

        composable(route = NotiSyncScreen.Client.name) {
            ClientScreen(sharedPreferences, viewModel, onSettingsButtonClicked = {
                navController.navigate(NotiSyncScreen.Settings.name) })
        }

        composable(route = NotiSyncScreen.Settings.name) {
            SettingsScreen(sharedPreferences, viewModel,
                navigateUp = { navController.navigateUp() },
                onResetBtnClicked = { navController.navigate(NotiSyncScreen.Reset.name) })
        }

        composable(route = NotiSyncScreen.Reset.name) {
            ResetScreen(sharedPreferences, viewModel, onOKButtonClicked = {
                val editor = sharedPreferences.edit()
                editor.putBoolean("IsInited", false)
                editor.putBoolean("IsClient", false)
                editor.putString("Token", "")
                editor.apply()
                navController.navigate(NotiSyncScreen.Welcome.name)
            })
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScreen(sharedPreferences: SharedPreferences, viewModel: MainScreenViewModel, onSettingsButtonClicked:() -> Unit){
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val token = sharedPreferences.getString("Token", "")
    if(token != null){
        viewModel.token.value = token
    }

    FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
        if (!task.isSuccessful) {
            Log.w(TAG, "Fetching FCM registration token failed", task.exception)
            return@OnCompleteListener
        }

        // Get new FCM registration token
        val tokenStr = task.result.toString()

        val editor = sharedPreferences.edit()
        editor.putString("Token", tokenStr)
        editor.apply()
    })

    Scaffold(topBar = { TopAppBar( title = { Text(stringResource(id = R.string.client), style = MaterialTheme.typography.titleLarge)}, modifier = Modifier, colors = topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        titleContentColor = MaterialTheme.colorScheme.primary,
    ), actions = {
        IconButton(onClick = onSettingsButtonClicked) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(id = R.string.settings)
            )
        }
    })}){ innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ){
            if(token!=null)
            {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
                {
                    Text(text = token, modifier = Modifier
                        .padding(10.dp, 20.dp)
                        .fillMaxWidth())
                }

                Button(onClick = {
                    val textToCopy = token.toString()
                    val annotatedString: AnnotatedString = AnnotatedString(token)
                    clipboardManager.setText(annotatedString)
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()

                }, modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.CenterHorizontally)) {
                    Text(text = stringResource(id = R.string.client_screen_copy))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(sharedPreferences: SharedPreferences, viewModel: MainScreenViewModel,
                 onSettingsButtonClicked:() -> Unit){
    val context = LocalContext.current

    var token by rememberSaveable {mutableStateOf(sharedPreferences.getString("Token", ""))}

    val assetManager = context.assets
    val filename = "notificationsync-e95aa-firebase-adminsdk-yuwd4-33db92faa1.json" // 替换为你的 asset 文件名

    val inputStream: InputStream = assetManager.open(filename)
    val outFile = File(context.getExternalFilesDir(null), filename)

    val outputStream: OutputStream = FileOutputStream(outFile)
    inputStream.copyTo(outputStream)
    inputStream.close()
    outputStream.close()


    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(id = R.string.server), style = MaterialTheme.typography.titleLarge)}, modifier = Modifier, colors = topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ), actions = {
                IconButton(onClick = onSettingsButtonClicked) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(id = R.string.settings)
                    )
                }
            })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            token?.let {
                TextField(
                    value = it,
                    onValueChange = {
                        token = it
                    },
                    label = { Text(stringResource(id = R.string.server_screen_input_token)) },
                    modifier = Modifier
                        .padding(10.dp, 30.dp)
                        .fillMaxWidth()
                )
            }
            Button(
                onClick = {
                    val editor = sharedPreferences.edit()
                    editor.putString("Token", token)
                    editor.apply()

                    if (NotificationManagerCompat.getEnabledListenerPackages(context)
                            .contains("com.happymax.notificationsync")
                    ) {
                        val pm: PackageManager = context.packageManager
                        pm.setComponentEnabledSetting(
                            ComponentName(context, NotiSyncNotificationListenerService::class.java),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    } else {
                        // 跳转到设置页开启权限
                        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    }
                }, modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(text = stringResource(R.string.save_btn_text))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(sharedPreferences: SharedPreferences, viewModel: MainScreenViewModel,
                   navigateUp: () -> Unit, onResetBtnClicked: () -> Unit){
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Settings")
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.titlebar_goback)
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Button(
                onClick = onResetBtnClicked, modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.Start)
            ) {
                Text(text = stringResource(R.string.settings_reset))
            }
        }
    }
}

@Composable
fun ResetScreen(sharedPreferences: SharedPreferences, viewModel: MainScreenViewModel, onOKButtonClicked: ()->Unit) {
    val openDialog = remember { mutableStateOf(true) }

    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {
                // 当用户点击对话框以外的地方或者按下系统返回键将会执行的代码
                openDialog.value = false
            },
            title = {
                Text(
                    text = stringResource(id = R.string.reset_screen_title),
                    fontWeight = FontWeight.W700,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.reset_screen_body),
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onOKButtonClicked()

                        openDialog.value = false
                    },
                ) {
                    Text(
                        stringResource(id = R.string.reset_screen_ok),
                        fontWeight = FontWeight.W700,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                    }
                ) {
                    Text(
                        stringResource(id = R.string.reset_screen_cancel),
                        fontWeight = FontWeight.W700,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )
    }
}

@Preview
@Composable
fun WelcomeScreenPreview(){
    val context = LocalContext.current

    val sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)

    val viewModel : MainScreenViewModel = viewModel()

    viewModel.isInited.value = sharedPreferences.getBoolean("IsInited", false)

    val navController = rememberNavController()

    if(!viewModel.isInited.value){
        NotificationSyncTheme{
            WelcomeScreen(sharedPreferences, viewModel)
        }
    }
}

@Preview
@Composable
fun ServerSettingsPreview(){
    val context = LocalContext.current

    val sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)

    val viewModel : MainScreenViewModel = viewModel()

    viewModel.isInited.value = sharedPreferences.getBoolean("IsInited", true)

    val navController = rememberNavController()

    NotificationSyncTheme{
        ServerScreen(sharedPreferences, viewModel, onSettingsButtonClicked = {
            navController.navigate(NotiSyncScreen.Settings.name)
        })
    }
}

@Preview
@Composable
fun ClientScreenPreview(){
    val context = LocalContext.current

    val sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)

    val viewModel : MainScreenViewModel = viewModel()

    viewModel.isInited.value = sharedPreferences.getBoolean("IsInited", true)

    val navController = rememberNavController()

    NotificationSyncTheme{
        ClientScreen(sharedPreferences, viewModel, onSettingsButtonClicked = {
            navController.navigate(NotiSyncScreen.Settings.name)
        })
    }
}

@Preview
@Composable
fun SettingsScreenPreview(){
    val context = LocalContext.current

    val sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)

    val viewModel : MainScreenViewModel = viewModel()

    viewModel.isInited.value = sharedPreferences.getBoolean("IsInited", true)

    val navController = rememberNavController()

    NotificationSyncTheme{
        SettingsScreen(sharedPreferences, viewModel,
            navigateUp = { navController.navigateUp() },
            onResetBtnClicked = { navController.navigate(NotiSyncScreen.Reset.name) })
    }
}