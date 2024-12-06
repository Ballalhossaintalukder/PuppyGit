package com.catpuppyapp.puppygit.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.compose.ClearMasterPasswordDialog
import com.catpuppyapp.puppygit.compose.MySelectionContainer
import com.catpuppyapp.puppygit.compose.PasswordTextFiled
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.settings.SettingsUtil
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.ui.theme.Theme
import com.catpuppyapp.puppygit.utils.AppModel
import com.catpuppyapp.puppygit.utils.HashUtil
import com.catpuppyapp.puppygit.utils.MyLog
import com.catpuppyapp.puppygit.utils.UIHelper
import com.catpuppyapp.puppygit.utils.doJobThenOffLoading

private const val stateKeyTag = "RequireMasterPasswordScreen"
private const val TAG = "RequireMasterPasswordScreen"

@Composable
fun RequireMasterPasswordScreen(
    requireMasterPassword:MutableState<Boolean>
) {
    val activityContext = LocalContext.current
    val errMsg = rememberSaveable { mutableStateOf("") }

    val password = rememberSaveable { mutableStateOf("") }
    val passwordVisible = rememberSaveable { mutableStateOf(false) }
    val showClearMasterPasswordDialog = rememberSaveable { mutableStateOf(false) }

    val focusRequest = remember {FocusRequester()}

    val settings = remember { SettingsUtil.getSettingsSnapshot() }

    if(showClearMasterPasswordDialog.value) {
        ClearMasterPasswordDialog(
            onCancel = {showClearMasterPasswordDialog.value = false},
            onOk = {
                showClearMasterPasswordDialog.value = false
                requireMasterPassword.value = false
            }
        )
    }

    val initLoadingText = stringResource(R.string.loading)
    val loading = rememberSaveable { mutableStateOf(false) }
    val loadingText = rememberSaveable { mutableStateOf(initLoadingText) }

    val loadingOn = { text:String->
        loadingText.value = text
        loading.value = true
    }

    val loadingOff = {
        loading.value = false
        loadingText.value = initLoadingText
    }

    val inputPassCallback = {
        val pass = password.value

        doJobThenOffLoading(loadingOn, loadingOff, loadingText.value) {
            try {
                loadingText.value = activityContext.getString(R.string.verifying)
                val verified = HashUtil.verify(pass, settings.masterPasswordHash)
                if(verified) {
                    loadingText.value = activityContext.getString(R.string.checking_creds_migration)
                    //一般不会迁移失败，这里不try...catch了，就算迁移失败，用户还能清主密码，不包了
                    AppModel.singleInstanceHolder.dbContainer.credentialRepository.migrateEncryptVerIfNeed(pass)

                    loadingText.value = activityContext.getString(R.string.updating_master_password)
                    AppModel.singleInstanceHolder.masterPassword.value = pass

                    //完了！可以进入app了
                    requireMasterPassword.value = false
                }else {
                    errMsg.value = activityContext.getString(R.string.wrong_password)
                }

            }catch (e:Exception) {
                errMsg.value = e.localizedMessage ?: (activityContext.getString(R.string.wrong_password) + ", (err msg is null)")
            }
        }
    }

    Column(
        modifier = Modifier
            //fillMaxSize 必须在最上面！要不然，文字不会显示在中间！
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .then(
                if(Theme.inDarkTheme) Modifier.background(Color.Black) else Modifier
            )
        ,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Image(bitmap = AppModel.getAppIcon(activityContext), contentDescription = stringResource(R.string.app_icon),
            modifier = Modifier.size(100.dp)
            )

        Spacer(modifier = Modifier.height(30.dp))

//        Row { Text(stringResource(R.string.master_password), fontSize = 30.sp) }
//        Spacer(modifier = Modifier.height(60.dp))


        PasswordTextFiled(
            password = password,
            passwordVisible = passwordVisible,
            label = stringResource(R.string.master_password),
            placeholder = stringResource(R.string.input_your_master_password),
            focusRequest = focusRequest,
            paddingValues = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp),
            errMsg = errMsg,
            enabled = loading.value.not(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                inputPassCallback()
            })
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.i_forgot_my_master_password),
            style = MyStyleKt.ClickableText.style,
            color = if(loading.value) UIHelper.getDisableTextColor() else MyStyleKt.ClickableText.color,
            fontWeight = FontWeight.Light,
            modifier = MyStyleKt.ClickableText.modifier.clickable(
                enabled = loading.value.not(),
                onClick = { showClearMasterPasswordDialog.value = true }
            ),
        )

        Spacer(Modifier.height(20.dp))
        // ok btn
        Button(
            enabled = loading.value.not(),
            onClick = {
                inputPassCallback()
            }
        ) {
            Text(stringResource(R.string.confirm))
        }

        Spacer(Modifier.height(20.dp))

        if(loading.value) {
            MySelectionContainer {
                Text(loadingText.value, fontWeight = FontWeight.Light, color = MyStyleKt.TextColor.highlighting_green)
            }
        }

        Spacer(Modifier.height(100.dp))
    }

    LaunchedEffect(Unit) {
        try {
            focusRequest.requestFocus()
        }catch (e:Exception) {
            MyLog.e(TAG, "request focus failed: ${e.stackTraceToString()}")
        }
    }

}
