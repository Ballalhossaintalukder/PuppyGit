package com.catpuppyapp.puppygit.play.pro

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catpuppyapp.puppygit.compose.AppIcon
import com.catpuppyapp.puppygit.compose.CardButton
import com.catpuppyapp.puppygit.compose.GoToTopAndGoToBottomFab
import com.catpuppyapp.puppygit.compose.MySelectionContainer
import com.catpuppyapp.puppygit.compose.SpacerRow
import com.catpuppyapp.puppygit.constants.IntentCons
import com.catpuppyapp.puppygit.screen.content.homescreen.innerpage.reportBugsLink
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.ui.theme.PuppyGitAndroidTheme
import com.catpuppyapp.puppygit.ui.theme.Theme
import com.catpuppyapp.puppygit.utils.ActivityUtil
import com.catpuppyapp.puppygit.utils.ContextUtil
import com.catpuppyapp.puppygit.utils.MyLog
import com.catpuppyapp.puppygit.utils.pref.PrefMan
import com.catpuppyapp.puppygit.utils.showToast


private const val TAG = "CrashActivity"


class CrashActivity : ComponentActivity() {
    companion object {
        val ACTION_SHOW_ERR_MSG = IntentCons.Action.SHOW_ERR_MSG
        const val INTENT_EXTRA_KEY_ERR_MSG = IntentCons.ExtrasKey.errMsg
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val funName = "onCreate"

        super.onCreate(savedInstanceState)

        MyLog.d(TAG, "#onCreate called")

        val errMsg = getErrMsg(intent)


        setContent {
            //这页面只使用主题，不更新主题，所以不需要创建state变量
            val theme = ""+PrefMan.getInt(applicationContext, PrefMan.Key.theme, Theme.defaultThemeValue);

            PuppyGitAndroidTheme(
                theme = theme,
            ) {
                MainCompose(activity = this, appContext = applicationContext, errMsg) {
                    finish()
                }
            }
        }

    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ContextUtil.getLocalizedContext(newBase))
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}


@Composable
private fun MainCompose(activity: Activity, appContext: Context, errMsg: String, exit:()->Unit) {
    val activityContext = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val spacerHeight = 20.dp
    val errMsgZonePadding = 20.dp

    val scrollState = rememberScrollState()
    //设为true，默认显示fab，可临时隐藏，超时后将再次显示
    val showFab = rememberSaveable { mutableStateOf(true) }
    val lastScrollPosition = rememberSaveable { mutableStateOf(0) }

    Scaffold(
        floatingActionButton = {
            if(showFab.value) {
                GoToTopAndGoToBottomFab(
                    scope = scope,
                    listState = scrollState,
                    listLastPosition = lastScrollPosition,
                    showFab = showFab
                )
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding).verticalScroll(scrollState),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,

        ) {
            Spacer(Modifier.height(spacerHeight))
            AppIcon()
            Spacer(Modifier.height(spacerHeight))

            Text("App Crashed!!", fontSize = 30.sp)

            MySelectionContainer {
                Text(errMsg, color = MyStyleKt.TextColor.error(), modifier = Modifier.padding(errMsgZonePadding))
            }

            HorizontalDivider()
            Spacer(Modifier.height(spacerHeight))


            CardButton(
                text = stringResource(R.string.copy)
            ) {
                clipboardManager.setText(AnnotatedString(errMsg))
                showToast(activityContext, activityContext.getString(R.string.copied))
            }

            Spacer(Modifier.height(spacerHeight))

            CardButton(
                text = stringResource(R.string.report_bugs)
            ) {
                ActivityUtil.openUrl(activityContext, reportBugsLink)
            }

            Spacer(Modifier.height(spacerHeight))

            CardButton(
                text = stringResource(R.string.restart_app)
            ) {
                //启动App主Activity
                //注：必须传Activity，applicationContext无法启动Activity，可能需要悬浮窗权限，但也不一定，总之一般通过Activity启动Activity是没问题的
                startMainActivity(activity)

                //退出CrashActivity（当前Activity）
                exit()
            }

            Spacer(Modifier.height(spacerHeight))

            CardButton(
                text = stringResource(R.string.exit)
            ) {
                exit()
            }

            SpacerRow()
        }
    }

}

fun startCrashActivity(fromActivity: Activity, errMsg:String) {
    val intent = Intent(CrashActivity.ACTION_SHOW_ERR_MSG).apply {
        //携带错误信息
        putExtra(CrashActivity.INTENT_EXTRA_KEY_ERR_MSG, errMsg)
        setClass(fromActivity, CrashActivity::class.java)
    }

    fromActivity.startActivity(intent)
}

private fun getErrMsg(intent: Intent):String {
    return intent.extras?.getString(CrashActivity.INTENT_EXTRA_KEY_ERR_MSG) ?: ""
}
