package com.happymax.notificationsync

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.happymax.basicscodelab.ui.theme.NotificationSyncTheme
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream


enum class NotiSyncScreen(@StringRes val title:Int) {
    Welcome(title = R.string.welcome_screen_title),
    Server(title = R.string.server),
    Client(title = R.string.client),
    Settings(title = R.string.settings),
    Reset(title = R.string.reset_screen_title),
    AppList(title = R.string.appList)
}

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

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

    override fun onResume() {
        super.onResume()

        val extras = intent.extras
        if(extras != null){
            val packageName = extras?.getString("packageName")
            //val appName = extras?.getString("appName")
            if(packageName != null){
                val packageManager = this.packageManager
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    startActivity(intent)
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
fun ShowAppInfo(appInfo: AppInfo, modifier: Modifier = Modifier, onCheckedChange:()->Unit, onEnabledChange:()->Unit) {
    var isEnabled by remember {
        mutableStateOf(appInfo.enable)
    }

    var isChecked by remember {
        mutableStateOf(appInfo.clearNotification)
    }

    Surface(
        modifier = modifier,
        content =  {
            Column(modifier=modifier.fillMaxWidth()) {
                Row(modifier = modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    , horizontalArrangement = Arrangement.SpaceBetween){
                    Box(modifier=modifier.weight(1f)){
                        Column {
                            Row {
                                if(appInfo.icon != null)
                                    Image(bitmap = drawableToBitmap(appInfo.icon).asImageBitmap(), contentDescription = appInfo.appName,
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(60.dp)
                                            .padding(10.dp))
                                Column(modifier = Modifier
                                    .align(Alignment.CenterVertically)) {
                                    Text(
                                        text = appInfo.appName,
                                        modifier = modifier
                                    )
                                    Text(
                                        text = appInfo.packageName,
                                        modifier = modifier,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Row{
                        Switch(checked = isEnabled,
                            onCheckedChange = {
                                isEnabled = it
                                appInfo.enable = isEnabled
                                onEnabledChange()
                            })
                    }
                }
                if(isEnabled){
                    Row(modifier = modifier
                        .fillMaxWidth()
                        .padding(10.dp, 0.dp)
                        , horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier=modifier.weight(1f)){

                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End){
                            Text(text = stringResource(id = R.string.clear_notification_after_foward), fontSize = 10.sp)
                            Checkbox(checked = isChecked, onCheckedChange = {
                                isChecked = it
                                appInfo.clearNotification = isChecked
                                onCheckedChange()
                            })
                        }
                    }

                }
            }

        })
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }

    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppListScreen(sharedPreferences: SharedPreferences, modifier: Modifier = Modifier, navigateUp: () -> Unit){
    val context = LocalContext.current

    val listState = rememberLazyListState()
    var searchText by rememberSaveable { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var hideSystemApp by remember { mutableStateOf(true) }

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var appList by rememberSaveable {mutableStateOf(getAppList(sharedPreferences, context))}

    val queryAppList = appList
        .filter { it.appName.contains(searchText) }.filter { it.isSystem == !hideSystemApp }
        .groupBy { it.appName.first()
        }

    Box(modifier = modifier.fillMaxSize()){
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(title = {
                        if(!isSearchActive)
                            Text(stringResource(id = R.string.appList), style = MaterialTheme.typography.titleLarge)
                    },
                        modifier = Modifier, colors = TopAppBarColors(containerColor = MaterialTheme.colorScheme.primary, actionIconContentColor = Color.White, navigationIconContentColor = Color.White,
                            scrolledContainerColor = Color.White, titleContentColor = Color.White), navigationIcon = {
                            IconButton(onClick = {
                                navigateUp()
                            } ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(id = R.string.titlebar_goback)
                                )
                            }
                        }, actions = {
                            if(!isSearchActive) {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = stringResource(id = R.string.search))
                                }
                            }else
                            {
                                Row {
                                    TextField(
                                        value = searchText,
                                        onValueChange = { query ->
                                            searchText = query

                                        },
                                        placeholder = { Text(stringResource(id = R.string.search)) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .width(200.dp)
                                            .background(Color.Transparent),
                                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, disabledPlaceholderColor = Color.White, unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
                                    )
                                    IconButton(onClick = {
                                        isSearchActive = false
                                        searchText = ""
                                    }, modifier = Modifier
                                        .align(Alignment.CenterVertically)) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.close))
                                    }
                                }
                            }
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "more")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = stringResource(id = R.string.hide_system_app))

                                        Checkbox(
                                            checked = hideSystemApp,
                                            onCheckedChange = {
                                                hideSystemApp = it
                                            })
                                    }

                                }, onClick = { hideSystemApp = !hideSystemApp })
                            }
                        })
                }
            }
        ) { innerPadding ->
            Row {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(innerPadding),
                )
                {
                    LazyColumn(state = listState) {
                        queryAppList.forEach{(name, list) ->
                            stickyHeader {
                                Text(
                                    text = name.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(8.dp)
                                )
                            }
                                items(items = list) { item ->
                                ShowAppInfo(item,
                                    onEnabledChange = {
                            if ( isSearchActive) {
                                val app = appList.firstOrNull { it.appName == item.appName }
                                if (app != null) {
                                    app.enable = item.enable
                                    app.clearNotification = item.clearNotification
                                }

                            }

                            val index = appList.indexOf(item)
                            appList[index] = item.copy(enable = item.enable, clearNotification = item.clearNotification)
                                    },
                                    onCheckedChange = {
                                        if ( isSearchActive) {
                                            val app = appList.firstOrNull { it.appName == item.appName }
                                            if (app != null) {
                                                app.enable = item.enable
                                                app.clearNotification = item.clearNotification
                                            }

                                        }

                                        val index = appList.indexOf(item)
                                        appList[index] = item.copy(enable = item.enable, clearNotification = item.clearNotification)

                                    })
                            }

                        }

                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                saveEnabledPackages(appList, sharedPreferences)
                navigateUp()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Done, contentDescription = "Add")
        }
    }
}

fun saveEnabledPackages(appList:ArrayList<AppInfo>, sharedPreferences: SharedPreferences){
    val enabledPackages = mutableListOf<EnabledApp>()
    for (appInfo in appList){
        if(appInfo.enable){
            enabledPackages.add(EnabledApp(appInfo.packageName, appInfo.clearNotification))
        }
    }

    val gson = Gson()
    val json = gson.toJson(enabledPackages)
    val editor = sharedPreferences.edit()
    editor.putString("EnabledPackages", json)
    editor.apply()
}

@Composable
fun ProgressDialog(showDialog: Boolean, onDismiss: () -> Unit) {
    if (showDialog) {
        Dialog(
            onDismissRequest = { onDismiss() },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
            ) {
                CircularProgressIndicator()
            }
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
            }, onGotoAppListButtonClicked = {
                navController.navigate(NotiSyncScreen.AppList.name)
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

        composable(route = NotiSyncScreen.AppList.name){
            AppListScreen(sharedPreferences, navigateUp = { navController.navigateUp() })
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
    var openQRCodeDialog by remember { mutableStateOf(false) }

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

    when {
        openQRCodeDialog -> {
            TokenQRCodeScreen(
                sharedPreferences,
                onDismissRequest = { openQRCodeDialog = false },
                onConfirmation = {
                    openQRCodeDialog = false
                },
            )
        }
    }

    Scaffold(topBar = { TopAppBar( title = { Text(stringResource(id = R.string.client), style = MaterialTheme.typography.titleLarge)}, modifier = Modifier, colors = TopAppBarColors(containerColor = MaterialTheme.colorScheme.primary, actionIconContentColor = Color.White, navigationIconContentColor = Color.White,
        scrolledContainerColor = Color.White, titleContentColor = Color.White), actions = {
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

            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween){
                Box(modifier= Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)){
                    Row {
                        IconButton(onClick = { openQRCodeDialog = true }) {
                            Icon(imageVector = Icons.Filled.Share,
                                contentDescription = stringResource(id = R.string.share_token)
                            )
                        }
                    }
                }
                Button(onClick = {
                    val annotatedString: AnnotatedString = AnnotatedString(token)
                    clipboardManager.setText(annotatedString)
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()

                }, modifier = Modifier
                    .padding(10.dp)) {
                    Text(text = stringResource(id = R.string.client_screen_copy))
                }
            }
            }
        }
    }
}

@Composable
fun TokenQRCodeScreen(
    sharedPreferences: SharedPreferences, onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
){
    val token = sharedPreferences.getString("Token", "")
    val blackColor: Int = 0x000000
    val whiteColor: Int = 0xFFFFFF

    AlertDialog(
        title = {
            Text(text = stringResource(R.string.share_token))
        },
        text = {
            if(!token.isNullOrEmpty()){
                val barcodeEncoder = BarcodeEncoder()
                val bitMatrix = barcodeEncoder.encode(token, BarcodeFormat.QR_CODE, 800, 800)
                val bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.RGB_565)
                for (x in 0..799) {
                    for (y in 0..799) {
                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) blackColor else whiteColor)
                    }
                }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center){
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = stringResource(id = R.string.token_qrcode))
                }
            }
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text(text = stringResource(id = R.string.token_qrcode_confirm))
            }
        },
    )
}

fun deleteDirectory(directory: File) {
    if (directory.exists()) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deleteDirectory(file)
            } else {
                file.delete()
            }
        }
        directory.delete()
    }
}

fun SyncAppIcons(context: Context){
    val fileDir = "${context.getFilesDir().getAbsolutePath()}/Icons"
    val directory = File(fileDir)
    deleteDirectory(directory)

    val packageManager = context.packageManager
    val packageInfos = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    for (info in packageInfos){
        val packageInfo: PackageInfo =
            packageManager.getPackageInfo(info.packageName, PackageManager.GET_PERMISSIONS)
        val requestedPermissions = packageInfo.requestedPermissions;
        if(requestedPermissions != null) {
            if(requestedPermissions.contains("android.permission.POST_NOTIFICATIONS")){
                val label = info.loadLabel(packageManager)
                val appName = label.toString()
                val packageName = info.packageName
                val icon: Drawable? = info.loadIcon(packageManager);
                if(icon != null)
                {
                    val bitmap: Bitmap = (icon as BitmapDrawable).bitmap
                    val outputStream: FileOutputStream = FileOutputStream(File("${fileDir}/${packageName}.png"))
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.close()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(sharedPreferences: SharedPreferences, viewModel: MainScreenViewModel,
                 onSettingsButtonClicked:() -> Unit, onGotoAppListButtonClicked:() -> Unit){
    val context = LocalContext.current
    var enable by rememberSaveable {mutableStateOf(false)}
    var token by rememberSaveable {mutableStateOf(sharedPreferences.getString("Token", ""))}
    var loading by remember { mutableStateOf(false) }
    val assetManager = context.assets
    val filename = "notificationsync-e95aa-firebase-adminsdk-yuwd4-33db92faa1.json" // 替换为你的 asset 文件名

    val inputStream: InputStream = assetManager.open(filename)
    val outFile = File(context.getExternalFilesDir(null), filename)
    
    if(!outFile.exists())
    {
        val outputStream: OutputStream = FileOutputStream(outFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
    }

    val packageName:String = context.packageName

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 更新状态以触发重组
                val flat = Settings.Secure.getString(context.contentResolver,"enabled_notification_listeners");
                if (flat != null) {
                    enable = flat.contains(packageName);
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ProgressDialog(showDialog = loading, onDismiss = { loading = false })
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(id = R.string.server), style = MaterialTheme.typography.titleLarge)}, modifier = Modifier, colors = TopAppBarColors(containerColor = MaterialTheme.colorScheme.primary, actionIconContentColor = Color.White, navigationIconContentColor = Color.White,
                scrolledContainerColor = Color.White, titleContentColor = Color.White)
                , actions = {
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

            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier= Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)){
                        Row {
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse("google.lens://")
                                if (intent.resolveActivity(context.packageManager) != null)
                                {
                                    context.startActivity(intent)
                                } }) {
                                Icon(painter = painterResource(id = R.drawable.baseline_photo_camera_24),
                                    contentDescription = stringResource(id = R.string.share_token)
                                )
                            }
                        }
                    }
                    Button(
                        onClick  = {
                            val editor = sharedPreferences.edit()
                            editor.putString("Token", token)
                            editor.apply()
                        }, modifier = Modifier
                            .padding(10.dp)
                    ){
                        Text(text = stringResource(R.string.save_btn_text))
                    }
                }
            }

            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(R.string.status_switch_text), modifier = Modifier.align(Alignment.CenterVertically))
                    Switch(checked = enable, onCheckedChange = {
                        //enable = it
                        // 跳转到设置页开启权限
                        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    }, modifier = Modifier
                        .padding(10.dp)
                    )
                }
            }

            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween){
                    Text(text = stringResource(R.string.add_apps), modifier = Modifier.align(Alignment.CenterVertically))
                    Button(
                        onClick = {
                            loading = true
                            onGotoAppListButtonClicked()
                        }, modifier = Modifier
                            .padding(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = stringResource(id = R.string.add_apps) )
                    }
                }
            }

        }
    }
}

private fun getAppList(sharedPreferences: SharedPreferences, context: Context):ArrayList<AppInfo>{
    val appList = ArrayList<AppInfo>()

    val packageManager = context.packageManager
    val packageInfos = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    var enabledPackages = mutableListOf<EnabledApp>()
    val json = sharedPreferences.getString("EnabledPackages", null)
    if(json != null){
        val type = object : TypeToken<List<EnabledApp>>() {}.type
        val gson = Gson()
        enabledPackages = gson.fromJson(json, type)
    }

    for (info in packageInfos){
        val packageInfo: PackageInfo =
            packageManager.getPackageInfo(info.packageName, PackageManager.GET_PERMISSIONS)
        val requestedPermissions = packageInfo.requestedPermissions;
        if(requestedPermissions != null) {
            if(requestedPermissions.contains("android.permission.POST_NOTIFICATIONS")){
                val label = info.loadLabel(packageManager)
                val appName = label.toString()
                val packageName = info.packageName
                val isSystemApp = (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val icon: Drawable? = info.loadIcon(packageManager);
                val savedAppInfo = enabledPackages.find { it.packageName.equals(packageName) }
                val enable = savedAppInfo != null
                val clearNoti = if(enable) savedAppInfo?.clearNotification else false
                val appInfo = AppInfo(appName, packageName, icon, isSystemApp, enable, if(clearNoti==null) false else clearNoti)
                appList.add(appInfo)
            }
        }
    }

    appList.sortWith(compareBy{ it.appName })
    return  appList
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(sharedPreferences: SharedPreferences, viewModel: MainScreenViewModel,
                   navigateUp: () -> Unit, onResetBtnClicked: () -> Unit){
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarColors(containerColor = MaterialTheme.colorScheme.primary, actionIconContentColor = Color.White, navigationIconContentColor = Color.White,
                    scrolledContainerColor = Color.White, titleContentColor = Color.White),
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EmbeddedSearchBar(
    listView: @Composable () -> Unit,
    onQueryChange: (String) -> Unit,
    isSearchActive: Boolean,
    onActiveChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onSearch: ((String) -> Unit),
) {

    var searchQuery by rememberSaveable { mutableStateOf("") }
    // 1
    val activeChanged: (Boolean) -> Unit = { active ->
        searchQuery = ""
        onQueryChange("")
        onActiveChanged(active)
    }
    SearchBar(
        query = searchQuery,
        // 2
        onQueryChange = { query ->
            searchQuery = query
            onQueryChange(query)
        },
        // 3
        onSearch = onSearch,
        active = isSearchActive,
        onActiveChange = activeChanged,
        // 4
        modifier = modifier
            .padding(start = 12.dp, top = 2.dp, end = 12.dp, bottom = 12.dp)
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessHigh)),
        placeholder = { Text(stringResource(id = R.string.search)) },
        leadingIcon = {
            if (isSearchActive) {
                IconButton(
                    onClick = { activeChanged(false) },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.navigation_action_back_cd),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingIcon = if (isSearchActive && searchQuery.isNotEmpty()) {
            {
                IconButton(
                    onClick = {
                        searchQuery = ""
                        onQueryChange("")
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.search_text_field_clear),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else {
            null
        },
        // 5
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0.dp)
    ) {
        // Search suggestions or results
        listView()
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
        }, onGotoAppListButtonClicked = {
            navController.navigate(NotiSyncScreen.AppList.name)
        })
    }
}

@Preview
@Composable
fun AppListScreenPreview(){
    val context = LocalContext.current

    val sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)

    val viewModel : MainScreenViewModel = viewModel()

    viewModel.isInited.value = sharedPreferences.getBoolean("IsInited", true)

    val navController = rememberNavController()

    NotificationSyncTheme{
        AppListScreen(sharedPreferences,
            navigateUp = { navController.navigateUp() })
    }
}