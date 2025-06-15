package com.catpuppyapp.puppygit.screen.content.homescreen.innerpage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catpuppyapp.puppygit.compose.AppIcon
import com.catpuppyapp.puppygit.compose.AppIconMonoChrome
import com.catpuppyapp.puppygit.compose.ClickableText
import com.catpuppyapp.puppygit.compose.MyHorizontalDivider
import com.catpuppyapp.puppygit.compose.SpacerRow
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.utils.ActivityUtil
import com.catpuppyapp.puppygit.utils.AppModel
import com.catpuppyapp.puppygit.utils.ComposeHelper
import com.catpuppyapp.puppygit.utils.UIHelper
import com.catpuppyapp.puppygit.utils.baseVerticalScrollablePageModifier
import com.catpuppyapp.puppygit.utils.forEachBetter


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

data class Contributor(
    val name:String,
    val link:String,
    val desc:String,
)

private val openSourceList = listOf(
    OpenSource(projectName = "libgit2", projectLink = "https://github.com/libgit2/libgit2", licenseLink = "https://raw.githubusercontent.com/libgit2/libgit2/main/COPYING"),
    OpenSource(projectName = "git24j", projectLink = "https://github.com/git24j/git24j", licenseLink = "https://raw.githubusercontent.com/git24j/git24j/master/LICENSE"),
    OpenSource(projectName = "text-editor-compose", projectLink = "https://github.com/kaleidot725/text-editor-compose", licenseLink = "https://raw.githubusercontent.com/kaleidot725/text-editor-compose/main/LICENSE"),
    OpenSource(projectName = "OpenSSL", projectLink = "https://github.com/openssl/openssl", licenseLink = "https://raw.githubusercontent.com/openssl/openssl/master/LICENSE.txt"),
    OpenSource(projectName = "libssh2", projectLink = "https://github.com/libssh2/libssh2", licenseLink = "https://raw.githubusercontent.com/libssh2/libssh2/refs/heads/master/COPYING"),
    OpenSource(projectName = "compose-markdown", projectLink = "https://github.com/jeziellago/compose-markdown", licenseLink = "https://github.com/jeziellago/compose-markdown/blob/main/LICENSE"),
    OpenSource(projectName = "swipe", projectLink = "https://github.com/saket/swipe", licenseLink = "https://github.com/saket/swipe/blob/trunk/LICENSE.txt"),
)


private val contributorList = listOf(
    Contributor(name = "triksterr", link = "https://github.com/triksterr", desc = "Russian translator"),
    Contributor(name = "mikropsoft", link = "https://github.com/mikropsoft", desc = "Turkish translator"),
    Contributor(name = "Hussain96o", link = "https://github.com/Hussain96o", desc = "Arabic translator"),
    Contributor(name = "sebastien46", link = "https://github.com/sebastien46", desc = "Monochrome app icon"),
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

    val appLogoEasterEggOn = rememberSaveable { mutableStateOf(false) }
    val appLogoEasterEggIconColor = remember { mutableStateOf(Color.Magenta) }

    Column(
        modifier = Modifier
            .baseVerticalScrollablePageModifier(contentPadding, listState)
            .padding(10.dp)
        ,
        horizontalAlignment = Alignment.CenterHorizontally,

        //垂直应从上到下，不需要居中
//        verticalArrangement = Arrangement.Center
    ) {
        //图标，app名，contact
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.clickable {
                    //若启用则换颜色；否则启用。
                    if(appLogoEasterEggOn.value) {
                        appLogoEasterEggIconColor.value = UIHelper.getRandomColor()
                    }else {
                        appLogoEasterEggOn.value = true
                    }
                }
            ) {
                if(appLogoEasterEggOn.value) {
                    AppIconMonoChrome(tint = appLogoEasterEggIconColor.value)
                }else {
                    AppIcon()
                }
            }

        }

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

        MyHorizontalDivider(modifier = Modifier.padding(10.dp))

        //开源项目列表
        TitleRow(stringResource(id = R.string.powered_by_open_source))

        val licenseStrRes = stringResource(R.string.license)

        openSourceList.forEachBetter {
            DoubleClickableRow(
                row1Text = it.projectName,
                row2Text = licenseStrRes,
                row1OnClick = { ActivityUtil.openUrl(activityContext, it.projectLink) },
                row2OnClick = { ActivityUtil.openUrl(activityContext, it.licenseLink) },
            )
        }

        MyHorizontalDivider(modifier = Modifier.padding(10.dp))

        //开源项目列表
        TitleRow("Thanks")

        contributorList.forEachBetter {
            DoubleClickableRow(
                row1Text = it.name,
                row2Text = it.desc,
                row1OnClick = { ActivityUtil.openUrl(activityContext, it.link) },
                row2OnClick = { ActivityUtil.openUrl(activityContext, it.link) },
            )
        }

        SpacerRow()
    }

}

@Composable
private fun TitleRow(title:String) {
    Row (modifier = Modifier.padding(10.dp)){
        Text(text = title, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DoubleClickableRow(
    row1Text:String,
    row2Text:String,
    row1OnClick:()->Unit,
    row2OnClick:()->Unit,
) {
    Column (
        modifier = Modifier.padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        ClickableText(
            text = row1Text,
            modifier = MyStyleKt.ClickableText.modifierNoPadding.clickable {
                row1OnClick()
            },
        )
        Spacer(Modifier.height(2.dp))
        ClickableText(
            text = "($row2Text)",
            fontSize = 12.sp,
            modifier = MyStyleKt.ClickableText.modifierNoPadding.clickable {
                row2OnClick()
            },
        )

        Spacer(Modifier.height(10.dp))

    }
}
