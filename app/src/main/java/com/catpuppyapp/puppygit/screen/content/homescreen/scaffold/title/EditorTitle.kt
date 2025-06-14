package com.catpuppyapp.puppygit.screen.content.homescreen.scaffold.title

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import com.catpuppyapp.puppygit.compose.FilterTextField
import com.catpuppyapp.puppygit.compose.ReadOnlyIcon
import com.catpuppyapp.puppygit.compose.ScrollableRow
import com.catpuppyapp.puppygit.compose.SmallIcon
import com.catpuppyapp.puppygit.constants.PageRequest
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.screen.functions.defaultTitleDoubleClick
import com.catpuppyapp.puppygit.screen.functions.defaultTitleDoubleClickRequest
import com.catpuppyapp.puppygit.screen.shared.EditorPreviewNavStack
import com.catpuppyapp.puppygit.screen.shared.FilePath
import com.catpuppyapp.puppygit.screen.shared.FuckSafFile
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.ui.theme.Theme
import com.catpuppyapp.puppygit.utils.FsUtils
import com.catpuppyapp.puppygit.utils.state.CustomStateSaveable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorTitle(
    recentFileListIsEmpty:Boolean,
    recentFileListFilterModeOn:Boolean,
    recentListFilterKeyword: CustomStateSaveable<TextFieldValue>,
    getActuallyRecentFilesListState:()-> LazyStaggeredGridState,
    getActuallyRecentFilesListLastPosition: ()->MutableState<Int>,

    patchModeOn:Boolean,
    previewNavStack: EditorPreviewNavStack,
    previewingPath:String,
    isPreviewModeOn:Boolean,
    previewLastScrollPosition:MutableState<Int>,
    scope:CoroutineScope,

    // 若指定文件名，将使用，否则解析路径取出文件名。
    // 应用场景：用来在打开uri时指定文件名，因为uri不是/开头的规范路径，而且还可能有%2F之类的编码，不一定能通过拆分路径取出文件名，所以干脆让上级页面解析好了传过来即可。
    editorPageShowingFileName: String?,

    editorPageShowingFilePath: MutableState<FilePath>,
    editorPageRequestFromParent:MutableState<String>,
    editorSearchMode:Boolean,
    editorSearchKeyword: CustomStateSaveable<TextFieldValue>,
    editorPageMergeMode:Boolean,
    readOnly:Boolean,
    editorOpenFileErr:Boolean
) {
    val haptic = LocalHapticFeedback.current
    val activityContext = LocalContext.current

    val inDarkTheme = remember { Theme.inDarkTheme }

    // file opened
    if(editorPageShowingFilePath.value.isNotBlank()) {
        val fileName = if(isPreviewModeOn && editorPageShowingFilePath.value.ioPath != previewingPath) FuckSafFile(activityContext, FilePath(previewingPath)).name else if(editorPageShowingFileName.isNullOrEmpty()) FuckSafFile(activityContext, editorPageShowingFilePath.value).name else editorPageShowingFileName
//        val filePath = getFilePathStrBasedRepoDir(editorPageShowingFilePath.value, returnResultStartsWithSeparator = true)

        val filePathNoFileNameNoEndSlash = FsUtils.getPathWithInternalOrExternalPrefixAndRemoveFileNameAndEndSlash(
            path = if(isPreviewModeOn) previewingPath else editorPageShowingFilePath.value.ioPath,
            fileName
        )

        Column(
            //双击标题回到文件顶部；长按可跳转到指定行；点击显示路径
            modifier = Modifier
                .combinedClickable(
                    //打开文件没出错 或 预览模式则启用，预览模式不管打开出没出错，都尝试显示弹窗，不过如果文件无法打开，
//                    enabled = !editorOpenFileErr || isPreviewModeOn,

                    onDoubleClick = {
                        if(isPreviewModeOn) {
                            runBlocking {
                                defaultTitleDoubleClick(scope, previewNavStack.getCurrentScrollState(), previewLastScrollPosition)
                            }
                        }else {
                            defaultTitleDoubleClickRequest(editorPageRequestFromParent)
                        }
                    },
                    onLongClick = {
                        if(isPreviewModeOn.not()) {
//                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            editorPageRequestFromParent.value = PageRequest.goToLine
                        }
                    }
                ) {  //onClick
                        //显示仓库开头的相对路径
    //                    Msg.requireShowLongDuration(filePath)

                    //显示文件详情（大小、路径、字数，等）
                    editorPageRequestFromParent.value = PageRequest.showDetails
                    //点按显示文件名
    //                showToast(AppModel.appContext, fileName)
                }.widthIn(min=MyStyleKt.Title.clickableTitleMinWidth)
        ) {
            if(editorSearchMode) {
                    FilterTextField(filterKeyWord = editorSearchKeyword)
            }else {
                ScrollableRow {
                    if(isPreviewModeOn) {
                        SmallIcon(
                            imageVector = Icons.Filled.RemoveRedEye,
                            contentDescription = stringResource(R.string.preview),
                        )
                    }else {
                        if(editorPageMergeMode) {
                            SmallIcon(
                                imageVector = Icons.Filled.Merge,
                                contentDescription = stringResource(R.string.merge_mode),
                            )
                        }

                        if(patchModeOn) {
                            SmallIcon(
                                imageVector = Icons.Outlined.Difference,
                                contentDescription = stringResource(R.string.patch_mode),
                            )
                        }

                        if(readOnly) {
                            ReadOnlyIcon()
                        }
                    }



                    Text(
                        text =fileName,
                        fontSize = MyStyleKt.Title.firstLineFontSizeSmall,
                        maxLines=1,
                        overflow = TextOverflow.Ellipsis,
//                        color = if(editorPageMergeMode) MyStyleKt.ChangeListItemColor.getConflictColor(inDarkTheme) else Color.Unspecified
                    )
                }
                ScrollableRow  {
                    Text(
                        text = filePathNoFileNameNoEndSlash,
                        fontSize = MyStyleKt.Title.secondLineFontSize,
                        maxLines=1,
                        overflow = TextOverflow.Ellipsis

                    )
                }

            }

        }

    }else {
        // file not opened
        if(recentFileListFilterModeOn) {
            FilterTextField(filterKeyWord = recentListFilterKeyword)
        }else {
            ScrollableRow(
                modifier = Modifier
                    .combinedClickable(
                        //打开文件没出错 或 预览模式则启用，预览模式不管打开出没出错，都尝试显示弹窗，不过如果文件无法打开，
    //                    enabled = !editorOpenFileErr || isPreviewModeOn,

                        onDoubleClick = {
                            defaultTitleDoubleClick(scope, getActuallyRecentFilesListState(), getActuallyRecentFilesListLastPosition())
                        },
                ) { }.widthIn(min=MyStyleKt.Title.clickableTitleMinWidth)
            ) {
                Text(
                    text = stringResource(if(recentFileListIsEmpty) R.string.editor else R.string.recent_files),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
