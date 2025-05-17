package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.git.TagDto
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.utils.Msg
import com.catpuppyapp.puppygit.utils.UIHelper
import com.catpuppyapp.puppygit.utils.time.TimeZoneUtil


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagItem(
    thisObj:TagDto,
    lastClickedItemKey:MutableState<String>,
    shouldShowTimeZoneInfo:Boolean,
    showDetails:(List<TagDto>) -> Unit,
    isItemInSelected:(TagDto) -> Boolean,
    onLongClick:(TagDto)->Unit,
    onClick:(TagDto)->Unit
) {

    val clipboardManager = LocalClipboardManager.current
    val activityContext = LocalContext.current

    val haptic = LocalHapticFeedback.current

    val defaultFontWeight = remember { MyStyleKt.TextItem.defaultFontWeight() }

    Column(
        //0.9f 占父元素宽度的百分之90
        modifier = Modifier
            .fillMaxWidth()
//            .defaultMinSize(minHeight = 100.dp)
            .combinedClickable(
                enabled = true,
                onClick = {
                    lastClickedItemKey.value = thisObj.name
                    onClick(thisObj)
                },
                onLongClick = {
                    lastClickedItemKey.value = thisObj.name

                    //震动反馈
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    onLongClick(thisObj)
                },
            )
            .then(
                //如果条目被选中，切换高亮颜色
                if (isItemInSelected(thisObj)) Modifier.background(
                    MaterialTheme.colorScheme.primaryContainer

                    //then 里传 Modifier不会有任何副作用，还是当前的Modifier(即调用者自己：this)，相当于什么都没改，后面可继续链式调用其他方法
                ) else if(thisObj.name == lastClickedItemKey.value){
                    Modifier.background(UIHelper.getLastClickedColor())
                }  else Modifier
            )
            //padding要放到 combinedClickable后面，不然点按区域也会padding；
            // padding要放到背景颜色后面，不然padding的区域不会着色
            .padding(10.dp)




    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,

        ){
            InLineIcon(
                icon = Icons.AutoMirrored.Filled.Label,
                tooltipText = stringResource(R.string.tag)
            )

//            Text(text = stringResource(R.string.name) +": ")

            Text(text = thisObj.shortName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = defaultFontWeight

            )
        }

        Row (
            verticalAlignment = Alignment.CenterVertically,

        ){

            InLineIcon(
                icon = Icons.Filled.Commit,
                tooltipText = stringResource(R.string.target)
            )

//            Text(text = stringResource(R.string.target) +": ")

            Text(text = thisObj.getCachedTargetShortOidStr(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = defaultFontWeight
            )

            InLineCopyIcon {
                clipboardManager.setText(AnnotatedString(thisObj.targetFullOidStr))
                Msg.requireShow(activityContext.getString(R.string.copied))
            }
        }
        Row (
            verticalAlignment = Alignment.CenterVertically,

        ){


            InLineIcon(
                icon = Icons.Filled.Category,
                tooltipText = stringResource(R.string.type)
            )

//            Text(text = stringResource(R.string.type) +": ")

            Text(text = thisObj.getType(activityContext, false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = defaultFontWeight
            )
        }

        //如果是本地分支，检查是否是当前活跃的分支。（远程分支就不需要检查了，因为远程分支一checkout就变成detached了，根本不可能是current活跃分支
        if(thisObj.isAnnotated) {
            Row (
                verticalAlignment = Alignment.CenterVertically,
            ){


                InLineIcon(
                    icon = Icons.Filled.Person,
                    tooltipText = stringResource(R.string.tagger)
                )

//                Text(text = stringResource(R.string.tagger) +": ")

                Text(text = thisObj.getFormattedTaggerNameAndEmail(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = defaultFontWeight

                )
            }
            Row (
                verticalAlignment = Alignment.CenterVertically,
            ){


                InLineIcon(
                    icon = Icons.Filled.CalendarMonth,
                    tooltipText = stringResource(R.string.date)
                )

//                Text(text = stringResource(R.string.date) +": ")

                Text(text = if(shouldShowTimeZoneInfo) TimeZoneUtil.appendUtcTimeZoneText(thisObj.getFormattedDate(), thisObj.originTimeOffsetInMinutes) else thisObj.getFormattedDate(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = defaultFontWeight

                )
            }
            Row (
                verticalAlignment = Alignment.CenterVertically,
            ){

                InLineIcon(
                    icon = Icons.AutoMirrored.Filled.Message,
                    tooltipText = stringResource(R.string.msg)
                )

//                Text(text = stringResource(R.string.msg) +": ")

                ClickableText(text = thisObj.msg) {
                    showDetails(listOf(thisObj))
                }
            }
        }
     }
}
