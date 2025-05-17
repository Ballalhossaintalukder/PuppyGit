package com.catpuppyapp.puppygit.screen.content.homescreen.innerpage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catpuppyapp.puppygit.compose.AppIcon
import com.catpuppyapp.puppygit.compose.ClickableText
import com.catpuppyapp.puppygit.compose.SpacerRow
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.utils.ActivityUtil
import com.catpuppyapp.puppygit.utils.AppModel
import com.catpuppyapp.puppygit.utils.ComposeHelper


const val authorMail = "luckyclover33xx@gmail.com"
const val authorMailLink = "mailto:$authorMail"

const val madeBy = "Made by Bandeapart1964 of catpuppyapp"
const val madeByLink = "https://github.com/Bandeapart1964"

const val sourceCodeLink = "https://github.com/catpuppyapp/PuppyGit"
const val privacyPolicyLink = "$sourceCodeLink/blob/main/PrivacyPolicy.md"
const val discussionLink = "$sourceCodeLink/discussions"
const val reportBugsLink = "$sourceCodeLink/issues/new"
const val donateLink = "$sourceCodeLink/blob/main/donate.md"
const val faqLink = "$sourceCodeLink/blob/main/FAQ.md"
const val httpServiceApiUrl = "$sourceCodeLink/blob/main/http_service_api.md"
const val automationDocUrl = "$sourceCodeLink/blob/main/automation_doc.md"


var versionCode: Int = AppModel.getAppVersionCode()
var versionName: String = AppModel.getAppVersionName()

data class OpenSource(
    val projectName:String,
    val projectLink:String,
    val licenseLink:String,
)

private val openSourceList= listOf(
    OpenSource(projectName = "libgit2", projectLink = "https://github.com/libgit2/libgit2", licenseLink = "https://raw.githubusercontent.com/libgit2/libgit2/main/COPYING"),
    OpenSource(projectName = "git24j", projectLink = "https://github.com/git24j/git24j", licenseLink = "https://raw.githubusercontent.com/git24j/git24j/master/LICENSE"),
    OpenSource(projectName = "text-editor-compose", projectLink = "https://github.com/kaleidot725/text-editor-compose", licenseLink = "https://raw.githubusercontent.com/kaleidot725/text-editor-compose/main/LICENSE"),
    OpenSource(projectName = "OpenSSL", projectLink = "https://github.com/openssl/openssl", licenseLink = "https://raw.githubusercontent.com/openssl/openssl/master/LICENSE.txt"),
    OpenSource(projectName = "libssh2", projectLink = "https://github.com/libssh2/libssh2", licenseLink = "https://raw.githubusercontent.com/libssh2/libssh2/refs/heads/master/COPYING"),
    OpenSource(projectName = "compose-markdown", projectLink = "https://github.com/jeziellago/compose-markdown", licenseLink = "https://github.com/jeziellago/compose-markdown/blob/main/LICENSE"),
    OpenSource(projectName = "swipe", projectLink = "https://github.com/saket/swipe", licenseLink = "https://github.com/saket/swipe/blob/trunk/LICENSE.txt"),
)

@Composable
fun AboutInnerPage(
    listState:ScrollState,
    contentPadding: PaddingValues,
    openDrawer:() -> Unit,
){

    val activityContext = LocalContext.current
    val exitApp = AppModel.exitApp;


//    val clipboardManager = LocalClipboardManager.current

//    val copy={text:String ->
//        clipboardManager.setText(AnnotatedString(text))
//        Msg.requireShow(activityContext.getString(R.string.copied))
//    }

    //back handler block start
    val isBackHandlerEnable = rememberSaveable { mutableStateOf(true)}
    val backHandlerOnBack = ComposeHelper.getDoubleClickBackHandler(context = activityContext, openDrawer = openDrawer, exitApp= exitApp)
    //注册BackHandler，拦截返回键，实现双击返回和返回上级目录
    BackHandler(enabled = isBackHandlerEnable.value, onBack = {backHandlerOnBack()})
    //back handler block end


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(top = 10.dp)
            .verticalScroll(listState)
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
    ){
        //图标，app名，contact
        AppIcon()

        Column(modifier = Modifier.padding(10.dp)

            ,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(id = R.string.app_name), fontWeight = FontWeight.ExtraBold)
            Text(text ="$versionName ($versionCode)", fontSize = 12.sp)
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    ActivityUtil.openUrl(activityContext, madeByLink)
                }
            ) {
                Text(
                    text = madeBy,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
//
//        Row(
//            horizontalArrangement = Arrangement.Center,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            TextButton(
//                onClick = {
//                    ActivityUtil.openUrl(appContext, donateLink)
//                }
//            ) {
//                Text(
//                    text = stringResource(R.string.donate),
//                    fontStyle = FontStyle.Italic,
//                )
//            }
//        }

//        Spacer(Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClickableText(
                text = stringResource(R.string.source_code),
                modifier = MyStyleKt.ClickableText.modifierNoPadding.clickable {
                    ActivityUtil.openUrl(activityContext, sourceCodeLink)
                },

            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClickableText(
                text = stringResource(R.string.discussions),
                modifier = MyStyleKt.ClickableText.modifierNoPadding.clickable {
                    //                    copy(authorMail)
                    ActivityUtil.openUrl(activityContext, discussionLink)
                },

            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClickableText(
                text = stringResource(R.string.report_bugs),
                modifier = MyStyleKt.ClickableText.modifierNoPadding.clickable {
                    ActivityUtil.openUrl(activityContext, reportBugsLink)
                },

            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
//            Text(text = stringResource(R.string.contact_author)+":")
            ClickableText(
                text = stringResource(R.string.contact_author),
                modifier = MyStyleKt.ClickableText.modifierNoPadding.clickable {
//                    copy(authorMail)
                    ActivityUtil.openUrl(activityContext, authorMailLink)
                },

            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
//            Text(text = stringResource(R.string.contact_author)+":")
            ClickableText(
                text = "💖"+stringResource(R.string.donate)+"💖",
                modifier = MyStyleKt.ClickableText.modifierNoPadding.clickable {
//                    copy(authorMail)
                    ActivityUtil.openUrl(activityContext, donateLink)
                },

            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClickableText(
                text = stringResource(R.string.faq),
                modifier = MyStyleKt.ClickableText.modifierNoPadding.clickable {
                    ActivityUtil.openUrl(activityContext, faqLink)
                },

            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClickableText(
                text = stringResource(R.string.privacy_policy),
                modifier = MyStyleKt.ClickableText.modifierNoPadding.clickable {
//                    copy(authorMail)
                    ActivityUtil.openUrl(activityContext, privacyPolicyLink)
                },

            )
        }
        HorizontalDivider(modifier = Modifier.padding(10.dp))
        //开源项目列表
        Row (modifier = Modifier.padding(10.dp)){
            Text(text = stringResource(id = R.string.powered_by_open_source)+":")
        }
        openSourceList.forEach {
            Column (
                modifier = Modifier.padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ){
                ClickableText(
                    text = it.projectName,
//                    fontSize = 14.sp,
                    modifier = MyStyleKt.ClickableText.modifierNoPadding.clickable {
                        //                        copy(it.projectLink)
                        ActivityUtil.openUrl(activityContext, it.projectLink)
                    },
                )
                Spacer(Modifier.height(2.dp))
                ClickableText(
                    text = "("+stringResource(R.string.license)+")",
                    fontSize = 12.sp,
                    modifier = MyStyleKt.ClickableText.modifierNoPadding.clickable {
                        //                        copy(it.projectLink)
                        ActivityUtil.openUrl(activityContext, it.licenseLink)
                    },
                )

                Spacer(Modifier.height(10.dp))

            }
        }

        SpacerRow()
    }

}
