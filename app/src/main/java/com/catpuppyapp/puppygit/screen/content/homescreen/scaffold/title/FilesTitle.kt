package com.catpuppyapp.puppygit.screen.content.homescreen.scaffold.title

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import com.catpuppyapp.puppygit.compose.FilterTextField
import com.catpuppyapp.puppygit.compose.ScrollableRow
import com.catpuppyapp.puppygit.constants.PageRequest
import com.catpuppyapp.puppygit.dto.FileItemDto
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.screen.functions.defaultTitleDoubleClickRequest
import com.catpuppyapp.puppygit.screen.functions.getFilesScreenTitle
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.utils.changeStateTriggerRefreshPage
import com.catpuppyapp.puppygit.utils.replaceStringResList
import com.catpuppyapp.puppygit.utils.state.CustomStateSaveable
import java.io.File


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilesTitle(
    currentPath: ()->String,
    goToPath:(String)->Unit,
    allRepoParentDir: File,
    needRefreshFilesPage: MutableState<String>,
    filesPageGetFilterMode: ()->Int,
    filterKeyWord:CustomStateSaveable<TextFieldValue>,
    filterModeOn:()->Unit,
    doFilter:(String)->Unit,
    requestFromParent:MutableState<String>,
    filterKeywordFocusRequester: FocusRequester,
    filesPageSimpleFilterOn: Boolean,
    filesPageSimpleFilterKeyWord:CustomStateSaveable<TextFieldValue>,
    curPathItemDto: FileItemDto,
    searching:Boolean
) {
    val haptic = LocalHapticFeedback.current
    val activityContext = LocalContext.current

    if(filesPageSimpleFilterOn) {
        FilterTextField(filterKeyWord = filesPageSimpleFilterKeyWord, loading = searching)
    } else {  //filesPageGetFilterMode()==0 , 搜索模式关闭
            //render page
        Column(
            modifier = Modifier
            .combinedClickable(
                onDoubleClick = {
                    defaultTitleDoubleClickRequest(requestFromParent)
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    //长按标题回到仓库根目录
                    goToPath(allRepoParentDir.canonicalPath)
                }
            ) {  //onClick
                requestFromParent.value = PageRequest.requireShowPathDetails
            },
        ) {
            ScrollableRow {
                Text(
                    text = getFilesScreenTitle(currentPath(), activityContext),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = MyStyleKt.Title.firstLineFontSize,
                )
            }

            ScrollableRow {
                Text(
                    text = replaceStringResList(stringResource(R.string.folder_n_file_m), listOf(""+curPathItemDto.folderCount, ""+curPathItemDto.fileCount)),
                    fontSize = MyStyleKt.Title.secondLineFontSize
                )
            }
        }

    }

    LaunchedEffect( filesPageGetFilterMode() ) {
        try {
            if(filesPageGetFilterMode()==1) {
                filterKeywordFocusRequester.requestFocus()
            }

        }catch (_:Exception) {
            //顶多聚焦失败，没啥好记的
        }
    }
}
