package com.catpuppyapp.puppygit.play.pro

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.catpuppyapp.puppygit.compose.CopyableDialog2
import com.catpuppyapp.puppygit.compose.LoadingText
import com.catpuppyapp.puppygit.compose.SshUnknownHostDialog
import com.catpuppyapp.puppygit.dev.DevFeature
import com.catpuppyapp.puppygit.jni.SshAskUserUnknownHostRequest
import com.catpuppyapp.puppygit.screen.AppScreenNavigator
import com.catpuppyapp.puppygit.screen.RequireMasterPasswordScreen
import com.catpuppyapp.puppygit.screen.functions.KnownHostRequestStateMan
import com.catpuppyapp.puppygit.screen.shared.IntentHandler
import com.catpuppyapp.puppygit.screen.shared.MainActivityLifeCycle
import com.catpuppyapp.puppygit.screen.shared.setByPredicate
import com.catpuppyapp.puppygit.screen.shared.setMainActivityLifeCycle
import com.catpuppyapp.puppygit.ui.theme.PuppyGitAndroidTheme
import com.catpuppyapp.puppygit.ui.theme.Theme
import com.catpuppyapp.puppygit.user.UserUtil
import com.catpuppyapp.puppygit.utils.AppModel
import com.catpuppyapp.puppygit.utils.ContextUtil
import com.catpuppyapp.puppygit.utils.Lg2HomeUtils
import com.catpuppyapp.puppygit.utils.Msg
import com.catpuppyapp.puppygit.utils.MyLog
import com.catpuppyapp.puppygit.utils.pref.PrefMan
import com.catpuppyapp.puppygit.utils.RndText
import com.catpuppyapp.puppygit.utils.doJobThenOffLoading
import com.catpuppyapp.puppygit.utils.showToast
import com.catpuppyapp.puppygit.utils.state.mutableCustomStateListOf
import com.catpuppyapp.puppygit.utils.state.mutableCustomStateOf
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay


private const val TAG = "MainActivity"

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

////
//fun Context.setAppLocale(language: String): Context {
//    val locale = Locale(language)
//    Locale.setDefault(locale)
//    val config = resources.configuration
//    config.setLocale(locale)
//    config.setLayoutDirection(locale)
//    return createConfigurationContext(config)
//}

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val funName = "onCreate"

        //20240519 上午: start: 尝试解决谷歌自动测试时的bug，什么gms err之类的
        //20240519 下午：更新：注释了这段代码，再上传，没报错。
        //20240519: 好像和这个无关，？参见未解决的问题文档，搜“play console测试莫名其妙报错 gms相关 原因不明 20240519”
//        val threadPolicy = StrictMode.ThreadPolicy.Builder()
//            .permitDiskReads()
//            .permitCustomSlowCalls()
////            .permitDiskWrites() // If you also want to ignore DiskWrites, Set this line too.
//            .build();
//        StrictMode.setThreadPolicy(threadPolicy);
        //20240519: end: 尝试解决谷歌自动测试时的bug，什么gms err之类的
        MyLog.d(TAG, "#onCreate called")

        super.onCreate(savedInstanceState)

        setMainActivityLifeCycle(MainActivityLifeCycle.ON_CREATE)


        //如果是初次创建Activity，onNewIntent不会被调用，只能在这里设置一下，要不然有可能漏外部传来的intent（分享文件、编辑文件）
        IntentHandler.setNewIntent(intent)

        //打印广告id，需要google play service
//        doJobThenOffLoading {
//            val adClient = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext)
//            // AdLimit为true代表用户退出个性化广告，即限制广告跟踪
//            MyLog.d(TAG, "AdId:${adClient.id}, AdLimit:${adClient.isLimitAdTrackingEnabled}")
//        }

//        println("applicationContext.filesDir:"+applicationContext.filesDir)
//        println("applicationContext.dataDir:"+applicationContext.dataDir)

//        println("Environment.getExternalStorageDirectory()="+Environment.getExternalStorageDirectory())  // /storage/emulated/0
//        println("applicationContext.filesDir="+applicationContext.filesDir)  // /data/user/0/com.catpuppyapp.puppygit/files
//        println("applicationContext.getExternalFilesDir(null)="+applicationContext.getExternalFilesDir(null))  // /storage/emulated/0/Android/data/com.catpuppyapp.puppygit/files
//        println("getShortUuid():::"+getShortUuid())

        // applicationContext, life time with app process, save reference has low risk of memory leak,
        // but some cases can't get properly resources (such as you switched language, but use applicationContext.getString() still get english),
//      //  AppModel.init_1(applicationContext = applicationContext, exitApp = {finish()})

        // baseContext, life time with activity, can get properly resources, but save reference to static field will increase risk of memory leak
//        AppModel.init_1(activityContext = baseContext, realAppContext = applicationContext, exitApp = {finish()})
        AppModel.init_1(realAppContext = applicationContext, exitApp = {finish()}, initActivity = true)

        //for make imePadding() work
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // for catch exception, block start。( refer: https://stackoverflow.com/questions/76061623/how-to-restart-looper-when-exception-throwed-in-jetpack-compose
        val recomposer = window.decorView.createLifecycleAwareWindowRecomposer(
            CoroutineExceptionHandler { coroutineContext, throwable ->
                try {
    //                throwable.printStackTrace();
                    val errMsg = throwable.stackTraceToString()
                    MyLog.e(TAG, "#$funName err: $errMsg")

                    //出错提示下用户就行，经我测试，画面会冻结，但数据不会丢，问题不大
                    showToast(applicationContext, getString(R.string.err_restart_app_may_resolve), Toast.LENGTH_LONG)  //测试了下，能显示Toast

                    //不重新创建Activity的话，页面会freeze，按什么都没响应，不过系统导航键还是可用的
                    //重新创建不一定成功，有可能会返回桌面
//                    ActivityUtil.restartActivityByIntent(this)

                    //启动crash activity显示错误信息
                    startCrashActivity(this, errMsg)

                    // 不重建Activity，直接退出
                    finish()

                    // 如果想显示错误弹窗，参见文档 “下一步-20240120.txt” 中的："compose错误处理 compose出错弹窗实现 20240505"

                }catch (e:Exception) {
                    e.printStackTrace()  //再出异常，管不了了，随便吧，打印下就行
                }
            }, lifecycle)

        // set window use our recomposer
        window.decorView.compositionContext = recomposer
        // for catch exception, block end

//        // val settings = SettingsUtil.getSettingsSnapshot()


        setContent {
            val theme = rememberSaveable { mutableStateOf(""+PrefMan.getInt(applicationContext, PrefMan.Key.theme, Theme.defaultThemeValue)) }
            AppModel.theme = theme
            PuppyGitAndroidTheme(
                theme = theme.value,
            ) {
                MainCompose()
                //                Greeting(baseContext)
            }
        }

    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ContextUtil.getLocalizedContext(newBase))
    }

    override fun onDestroy() {
        super.onDestroy()

        AppModel.destroyer()
    }

    override fun onNewIntent(intent: Intent) {
        MyLog.d(TAG, "#onNewIntent() called")
        super.onNewIntent(intent)

        // 如果intent需要消费，取消执行本次editor on resume事件，
        // 因为如果需要载入新文件，home screen会触发，
        // 如果需要导入文件或者跳转到非editor页面，
        // 也不需要载入文件，所以这里无论如何都不需要再执行editor的on resume
        if(IntentHandler.needConsume(intent)) {
            setMainActivityLifeCycle(MainActivityLifeCycle.IGNORE_ONCE_ON_RESUME)
            //为文本编辑器取消一次on resume事件，不然会重载占用锁导致intent中的文件不被加载
            MyLog.d(TAG, "has new intent need consume, will cancel ON_RESUME event once for Editor")
        }

        //Activity改单例了，得靠这个获取新intent
        IntentHandler.setNewIntent(intent)
    }

    //这函数不能删，我做了处理，忽略on create后第一个on resume，依赖这个函数触发状态变化才能成功设置on resume
    override fun onPause() {
        MyLog.d(TAG, "#onPause: called")
        super.onPause()

        // compose 可通过对应的get方法获取到 Activity 的生命周期事件
        setMainActivityLifeCycle(MainActivityLifeCycle.ON_PAUSE)
    }

    override fun onResume() {
        MyLog.d(TAG, "#onResume: called")
        super.onResume()

        // compose 可通过对应的get方法获取到 Activity 的生命周期事件
        // 仅当 pause后才设置on resume以忽略on create时第一个 on resume事件从而避免 compose(例如EditorInnerPage)的 LaunchedEffect和生命周期函数被重复调用
        // 会重复调用是因为Activity on create时会创建compose，然后就会触发LaunchedEffect，同时还会触发compose生命周期on resume，而切换到后台再切换回来时，
        // 只会触发compose on resume，所以，忽略创建Activity时的第一个on resume就能避免重复触发了，换句话说，得先切到后台后再触发compose on resume才执行就没问题了
        setByPredicate(MainActivityLifeCycle.ON_RESUME) {
            //忽略创建Activity后的第一个 on resume 事件
            it != MainActivityLifeCycle.ON_CREATE && it != MainActivityLifeCycle.IGNORE_ONCE_ON_RESUME
        }
    }

}


@Composable
private fun MainCompose() {
    val stateKeyTag = remember { "MainCompose" }
    val funName = remember { "MainCompose" }

    val clipboardManager = LocalClipboardManager.current
    val activityContext = LocalContext.current
    val loadingText = rememberSaveable { mutableStateOf(activityContext.getString(R.string.launching))}

    val sshCertRequestListenerChannel = remember { Channel<Int>() }
    val isInitDone = rememberSaveable { mutableStateOf(false) };
    val requireMasterPassword = rememberSaveable { mutableStateOf(false) };


    //start: init user and billing state, 这代码位置别随便挪，必须早于调用Billing.init()，不然相关的集合无法得到更新
    val isProState = rememberSaveable { mutableStateOf(false) }
    UserUtil.updateUserStateToRememberXXXForPage(newIsProState = isProState)

    //end: init user and billing state

    val sshAskUserUnknownHostRequestList = mutableCustomStateListOf(stateKeyTag, "sshAskUserUnknownHostRequestList", listOf<SshAskUserUnknownHostRequest>())
    val currentSshAskUserUnknownHostRequest = mutableCustomStateOf<SshAskUserUnknownHostRequest?>(stateKeyTag, "currentSshAskUserUnknownHostRequest", null)

    val iTrustTheHost = rememberSaveable { mutableStateOf(false) }
    val showSshDialog = rememberSaveable { mutableStateOf(false) }
    val closeSshDialog ={
        showSshDialog.value=false
    }
    val allowOrRejectSshDialogCallback={
        //设置当前条目为null以接受新条目
        currentSshAskUserUnknownHostRequest.value = null
        // 设置“我信任此主机”勾选框为未勾选
        iTrustTheHost.value = false
    }
    if(showSshDialog.value) {
        SshUnknownHostDialog(
            currentSshAskUserUnknownHostRequest = currentSshAskUserUnknownHostRequest,
            iTrustTheHost = iTrustTheHost,
            closeSshDialog = closeSshDialog,
            allowOrRejectSshDialogCallback = allowOrRejectSshDialogCallback,
            appContext = activityContext
        )
    }


    val showRandomLoadingTextDialog = rememberSaveable { mutableStateOf(false) }
    if(showRandomLoadingTextDialog.value) {
        CopyableDialog2(
            requireShowTextCompose = true,
            textCompose = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(text = loadingText.value, fontSize = 20.sp)
                }
            },
//            onDismiss = {},  //x 算了，这个弹窗不重要，应该设置的关闭比保持开启更容易才合理) 避免如果有人想点文字，狂点屏幕，然后显示弹窗，但点到外部，弹窗消失，所以把这个dismiss函数设为空
            onCancel = { showRandomLoadingTextDialog.value = false }
        ) {
            showRandomLoadingTextDialog.value=false

            clipboardManager.setText(AnnotatedString(loadingText.value))
            Msg.requireShow(activityContext.getString(R.string.copied))
        }
    }

    //初始化完成显示app界面，否则显示loading界面
    if(isInitDone.value) {
        if(requireMasterPassword.value) {
            RequireMasterPasswordScreen(requireMasterPassword)
        }else {
            AppScreenNavigator()
        }
    }else {
        //这个东西太阴间了，除非是真的需要确保阻止用户操作，例如保存文件，否则尽量别用这个
//        LoadingDialog(loadingText.value)
//        LoadingDialog()

        //x 废弃，安卓12以上有专门的splash屏幕，已经设置，以下看文字吧，不弄图标了免得和12的冲突，显示两次就尴尬了）xTODO 把文字loading替换成有App Logo 的精美画面
        //这里用Scaffold是因为其会根据系统是否暗黑模式调整背景色，就不需要我自己判断了
        Scaffold { contentPadding ->
            LoadingText(
                contentPadding = contentPadding,
                text = {
                    // 点文本可弹窗显示文本并可拷贝，不过一般人应该不会点，如果加载快也很难点到
                    Text(
                        text = loadingText.value,
                        modifier = Modifier.clickable { showRandomLoadingTextDialog.value = true },
                    )
                }
            )
        }
    }

    //compose创建时的副作用
    LaunchedEffect(Unit) {
//        println("LaunchedEffect传Unit只会执行一次，由于maincompose是app其他compose的根基，不会被反复创建销毁，所以maincompose里的launchedEffect只会执行一次，可以用来执行读取配置文件之类的初始化操作")
        try {

            //获取随机 loading text
            if(DevFeature.showRandomLaunchingText.state.value) {
                loadingText.value = RndText.getOne();
            }

//        读取配置文件，初始化状态之类的操作，初始化时显示一个loading页面，完成后更新状态变量，接着加载app页面
            //初始化完成之后，设置变量，显示应用界面
            doJobThenOffLoading {
                isInitDone.value = false

                //test passed
//                assert(!MyLog.isInited)
                //test

                //初始化AppSettings、MyLog等
                AppModel.init_2()

                //因为主密码需要用到AppSettings设置项，所以需要在init_2之后再调用才准
                requireMasterPassword.value = AppModel.requireMasterPassword()

                //如果无需主密码，直接在这检查是否需要迁移密码，一般迁移密码发生在加密解密器升级之后，可能换了实现，之类的
                if(requireMasterPassword.value.not()) {
                    //更新下loading文案
//                    loadingText.value = activityContext.getString(R.string.checking_creds_migration)

                    AppModel.dbContainer.credentialRepository.migrateEncryptVerIfNeed(AppModel.masterPassword.value)
                }

                //test passed
//                assert(MyLog.isInited)
                //test

                isInitDone.value = true

            }

            KnownHostRequestStateMan.init(sshAskUserUnknownHostRequestList.value)

            doJobThenOffLoading {
                while (true) {
                    if(sshCertRequestListenerChannel.tryReceive().isClosed) {
                        break
                    }

                    if(showSshDialog.value.not() && currentSshAskUserUnknownHostRequest.value==null) {
                        val item = KnownHostRequestStateMan.getFirstThenRemove()
                        if(item != null) {
                            if(Lg2HomeUtils.itemInUserKnownHostsFile(item.sshCert).not()) {
                                currentSshAskUserUnknownHostRequest.value = item
                                iTrustTheHost.value = false
                                showSshDialog.value = true
                            }
                        }
                    }

                    delay(1000)
                }
            }

        } catch (e: Exception) {
            MyLog.e(TAG, "#$funName err: "+e.stackTraceToString())
        }

        //test passed
//        delay(30*1000)
//        AppModel.exitApp()  //测试exitApp()，Editor未保存的数据是否会保存，结论：会
//        appContext.findActivity()?.recreate()  // 测试重建是否会保存数据，结论：会
//        throw RuntimeException("throw exception test")
        //test
    }

    //compose被销毁时执行的副作用
    DisposableEffect(Unit) {
//        ("DisposableEffect: entered main")
        onDispose {
//            ("DisposableEffect: exited main")
            sshCertRequestListenerChannel.close()
        }
    }

}

/**
 * （没严格测试）不能传context，因为除非有悬浮窗权限，否则只有Activity可以启动Activity，Service之类的不行
 */
fun startMainActivity(fromActivity: Activity) {
    val intent = Intent(ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        setClass(fromActivity, MainActivity::class.java)
    }

    fromActivity.startActivity(intent)
}

