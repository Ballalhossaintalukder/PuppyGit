package com.catpuppyapp.puppygit.screen.content.homescreen.scaffold.actions

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.catpuppyapp.puppygit.compose.FontSizeAdjuster
import com.catpuppyapp.puppygit.compose.LongPressAbleIconBtn
import com.catpuppyapp.puppygit.compose.SimpleCheckBox
import com.catpuppyapp.puppygit.constants.PageRequest
import com.catpuppyapp.puppygit.dev.dev_EnableUnTestedFeature
import com.catpuppyapp.puppygit.dev.editorEnableLineSelecteModeFromMenuTestPassed
import com.catpuppyapp.puppygit.dev.editorFontSizeTestPassed
import com.catpuppyapp.puppygit.dev.editorHideOrShowLineNumTestPassed
import com.catpuppyapp.puppygit.dev.editorLineNumFontSizeTestPassed
import com.catpuppyapp.puppygit.dev.editorMergeModeTestPassed
import com.catpuppyapp.puppygit.dev.editorSearchTestPassed
import com.catpuppyapp.puppygit.dev.proFeatureEnabled
import com.catpuppyapp.puppygit.dto.UndoStack
import com.catpuppyapp.puppygit.fileeditor.texteditor.state.TextEditorState
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.screen.shared.EditorPreviewNavStack
import com.catpuppyapp.puppygit.screen.shared.FilePath
import com.catpuppyapp.puppygit.settings.SettingsCons
import com.catpuppyapp.puppygit.settings.SettingsUtil
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.user.UserUtil
import com.catpuppyapp.puppygit.utils.Msg
import com.catpuppyapp.puppygit.utils.state.CustomStateSaveable
import kotlinx.coroutines.runBlocking

@Composable
fun EditorPageActions(
    initPreviewMode:()->Unit,
    requireEditorScrollToPreviewCurPos:MutableState<Boolean>,
    isPreviewModeOn:Boolean,
    previewNavStack: EditorPreviewNavStack,
    previewPath: String,
    previewPathChanged: String,

    editorPageShowingFilePath: MutableState<FilePath>,
//    editorPageRequireOpenFilePath: MutableState<String>,
    editorPageShowingFileIsReady: MutableState<Boolean>,
    needRefreshEditorPage: MutableState<String>,
    editorPageTextEditorState: CustomStateSaveable<TextEditorState>,
//    editorPageShowSaveDoneToast: MutableState<Boolean>,
    isSaving: MutableState<Boolean>,
    isEdited: MutableState<Boolean>,
    showReloadDialog: MutableState<Boolean>,
    showCloseDialog: MutableState<Boolean>,
    closeDialogCallback:CustomStateSaveable<(Boolean)->Unit>,
    doSave:suspend ()->Unit,
    loadingOn:(String)->Unit,
    loadingOff:()->Unit,
    editorPageRequest:MutableState<String>,
    editorPageSearchMode:MutableState<Boolean>,
    editorPageMergeMode:MutableState<Boolean>,
    editorPagePatchMode:MutableState<Boolean>,
    readOnlyMode:MutableState<Boolean>,
    editorSearchKeyword:String,
    isSubPageMode:Boolean,
    fontSize:MutableIntState,
    lineNumFontSize:MutableIntState,
    adjustFontSizeMode:MutableState<Boolean>,
    adjustLineNumFontSizeMode:MutableState<Boolean>,
    showLineNum:MutableState<Boolean>,
    showUndoRedo:MutableState<Boolean>,
    undoStack:UndoStack
) {
    /*
        注意：如果以后需要同一个EditorInnerPage配合多个title，就不要在这执行操作了，把这里的action逻辑放到EditorInnerPage执行，在这只发request，类似ChangeList页面请求执行pull/push那样
     */

    val haptic = LocalHapticFeedback.current
//    val scope = rememberCoroutineScope()

    val hasGoodKeyword = editorSearchKeyword.isNotEmpty()

    //这几个模式互斥，其实可以做成枚举

    if(isPreviewModeOn) {
        val currentIsNotAtHome = remember(previewPathChanged) { derivedStateOf { runBlocking { previewNavStack.currentIsRoot().not() } } }
        val canGoBack = remember(previewPathChanged) { derivedStateOf { runBlocking { previewNavStack.backStackIsNotEmpty() } } }
        val canGoForward = remember(previewPathChanged) { derivedStateOf { runBlocking { previewNavStack.aheadStackIsNotEmpty() } } }

        LongPressAbleIconBtn(
            tooltipText = stringResource(R.string.edit),
            icon = Icons.Filled.Edit,
        ) {
            requireEditorScrollToPreviewCurPos.value = true
            editorPageRequest.value = PageRequest.requireEditPreviewingFile
        }
        LongPressAbleIconBtn(
            //若没在首页则启用首页按钮
            enabled = currentIsNotAtHome.value,

            tooltipText = stringResource(R.string.home),
            icon = Icons.Filled.Home,
        ) {
            editorPageRequest.value = PageRequest.requireBackToHome
        }
        LongPressAbleIconBtn(
            enabled = canGoBack.value,
            tooltipText = stringResource(R.string.go_back),
            icon = Icons.AutoMirrored.Filled.ArrowBackIos,
        ) {
            editorPageRequest.value = PageRequest.editorPreviewPageGoBack
        }

        LongPressAbleIconBtn(
            enabled = canGoForward.value,
            tooltipText = stringResource(R.string.go_forward),
            icon = Icons.AutoMirrored.Filled.ArrowForwardIos,
        ) {
            editorPageRequest.value = PageRequest.editorPreviewPageGoForward
        }

        LongPressAbleIconBtn(
            enabled = true,
            tooltipText = stringResource(R.string.refresh),
            icon = Icons.Filled.Refresh,
        ) {
            editorPageRequest.value = PageRequest.editor_RequireRefreshPreviewPage
        }

        return  //返回，以免显示菜单项
    }else if(editorPageSearchMode.value) {
        LongPressAbleIconBtn(
            enabled = hasGoodKeyword,
            tooltipText = stringResource(R.string.find_previous),
            icon = Icons.Filled.ArrowUpward,
            iconContentDesc = stringResource(R.string.find_previous),
        ) {
            editorPageRequest.value = PageRequest.findPrevious
        }

        LongPressAbleIconBtn(
            enabled = hasGoodKeyword,
            tooltipText = stringResource(R.string.find_next),
            icon = Icons.Filled.ArrowDownward,
            iconContentDesc = stringResource(R.string.find_next),

            onLongClick = {
                //震动反馈
//                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                //显示功能提示和所有记数“FindNext(all:40)”，其中40是全文查找到的匹配关键字数量，文案尽量紧凑，避免toast显示不全
                editorPageRequest.value = PageRequest.showFindNextAndAllCount

            }
        ) {
            editorPageRequest.value = PageRequest.findNext
        }

        return  //返回，以免显示菜单项
    }else if(adjustFontSizeMode.value) {
        FontSizeAdjuster(fontSize = fontSize, resetValue = SettingsCons.defaultFontSize)

        return
    }else if(adjustLineNumFontSizeMode.value) {
        FontSizeAdjuster(fontSize = lineNumFontSize, resetValue = SettingsCons.defaultLineNumFontSize)

        return
    }


    val dropDownMenuExpandState = rememberSaveable { mutableStateOf(false) }

    val closeMenu = {dropDownMenuExpandState.value = false}

    val enableMenuItem = editorPageShowingFilePath.value.isNotBlank()

    //是否显示三点菜单图标，以后可能会增加其他判断因素，所以单独弄个变量
    val showMenuIcon = enableMenuItem

    if(enableMenuItem) {
        if(showUndoRedo.value) {
            val enableUndo = remember(undoStack.undoStackIsEmpty()) { undoStack.undoStackIsEmpty().not() }
            val undoStr = stringResource(R.string.undo)
            LongPressAbleIconBtn(
                enabled = enableUndo,
                tooltipText = undoStr,
                icon = Icons.AutoMirrored.Filled.Undo,
                iconContentDesc = undoStr,
                onLongClick = {
                    //震动反馈
//                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    // 显示 "Undo(saved steps count)"
                    Msg.requireShow("$undoStr(${undoStack.undoStackSize()})")
                }
            ) {
                editorPageRequest.value = PageRequest.requestUndo
            }

            val enableRedo = remember(undoStack.redoStackIsEmpty()) { undoStack.redoStackIsEmpty().not() }
            val redoStr = stringResource(R.string.redo)
            LongPressAbleIconBtn(
                enabled = enableRedo,
                tooltipText = redoStr,
                icon = Icons.AutoMirrored.Filled.Redo,
                iconContentDesc = redoStr,
                onLongClick = {
                    //震动反馈
//                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    // 显示 "Undo(saved steps count)"
                    Msg.requireShow("$redoStr(${undoStack.redoStackSize()})")
                }
            ) {
                editorPageRequest.value = PageRequest.requestRedo
            }
        }

        if(editorPageMergeMode.value) {
            LongPressAbleIconBtn(
                tooltipText = stringResource(R.string.previous_conflict),
                icon = Icons.Filled.ArrowUpward,
                iconContentDesc = stringResource(R.string.previous_conflict),
            ) {
                editorPageRequest.value = PageRequest.previousConflict
            }

            LongPressAbleIconBtn(
                tooltipText = stringResource(R.string.next_conflict),
                icon = Icons.Filled.ArrowDownward,
                iconContentDesc = stringResource(R.string.next_conflict),

                onLongClick = {
                    //震动反馈
//                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    //显示功能提示和所有记数“NextConflict(all:40)”，其中40是全文查找到的所有冲突数量，文案尽量紧凑，避免toast显示不全
                    editorPageRequest.value = PageRequest.showNextConflictAndAllConflictsCount

                }
            ) {
                editorPageRequest.value = PageRequest.nextConflict
            }
        }
    }

    if(showMenuIcon) {
        //菜单图标
        LongPressAbleIconBtn(
            //这种需展开的菜单，禁用内部的选项即可
//        enabled = enableAction.value,

            tooltipText = stringResource(R.string.menu),
            icon = Icons.Filled.MoreVert,
            iconContentDesc = stringResource(R.string.menu),
            onClick = {
                //切换菜单展开状态
                dropDownMenuExpandState.value = !dropDownMenuExpandState.value
            }
        )
    }

    //菜单列表
    Row(modifier = Modifier.padding(top = MyStyleKt.TopBar.dropDownMenuTopPaddingSize)) {
//        val enableMenuItem = enableAction.value && !changeListPageNoRepo.value  && !hasTmpStatus

        //菜单列表
        DropdownMenu(
            expanded = dropDownMenuExpandState.value,
            onDismissRequest = { closeMenu() }
        ) {
            DropdownMenuItem(
                enabled = enableMenuItem,

                text = { Text(stringResource(R.string.close)) },
                onClick = {
                    showCloseDialog.value=true

                    closeMenu()
                }
            )

            DropdownMenuItem(
                enabled = enableMenuItem,

                text = { Text(stringResource(R.string.reload_file)) },
                onClick = {
                    showReloadDialog.value = true

                    closeMenu()
                }
            )

            DropdownMenuItem(
                enabled = enableMenuItem && editorPageShowingFileIsReady.value && isEdited.value && !isSaving.value && !readOnlyMode.value,  //文件未就绪时不能保存,

                text = { Text(stringResource(R.string.save)) },
                onClick = {
                    editorPageRequest.value = PageRequest.requireSave

                    closeMenu()
                }
            )

            DropdownMenuItem(
                enabled = enableMenuItem && editorPageShowingFileIsReady.value,

                text = { Text(stringResource(R.string.preview)) },
                onClick = {
                    initPreviewMode()

                    closeMenu()
                }
            )

            DropdownMenuItem(
                enabled = enableMenuItem,

                text = { Text(stringResource(R.string.open_as)) },
                onClick = {
                    editorPageRequest.value = PageRequest.requireOpenAs

                    closeMenu()
                }
            )

            DropdownMenuItem(
                enabled = enableMenuItem,

                text = { Text(stringResource(R.string.file_history)) },
                onClick = {
                    editorPageRequest.value = PageRequest.requireGoToFileHistory

                    closeMenu()
                }
            )

            DropdownMenuItem(
                enabled = enableMenuItem,
                text = { Text(stringResource(R.string.go_to_line)) },
                onClick = {
                    editorPageRequest.value = PageRequest.goToLine  //发请求，由TextEditor组件开启搜索模式
                    closeMenu()
                }
            )

            if(UserUtil.isPro() && (dev_EnableUnTestedFeature || editorSearchTestPassed)){
                DropdownMenuItem(
                    enabled = enableMenuItem,

//                    text = { Text(stringResource(R.string.search)) },  //纠结一番，感觉search不如find合适
                    text = { Text(stringResource(R.string.find)) },
                    onClick = {
//                        editorPageSearchMode.value = true  //需要初始化搜索位置，所以不能简单设为true开启，不过可以简单设为false来关闭搜索模式
                        editorPageRequest.value = PageRequest.requireSearch  //发请求，由TextEditor组件开启搜索模式
                        closeMenu()
                    }
                )
            }

            if(UserUtil.isPro() && (dev_EnableUnTestedFeature || editorMergeModeTestPassed)){
                DropdownMenuItem(
                    enabled = enableMenuItem,
                    text = { Text(stringResource(R.string.merge_mode)) },
                    trailingIcon = {
                        SimpleCheckBox(editorPageMergeMode.value)
                    },
                    onClick = {
                        editorPageMergeMode.value = !editorPageMergeMode.value

                        closeMenu()
                    }

                )
            }

            DropdownMenuItem(
                enabled = enableMenuItem,
                text = { Text(stringResource(R.string.patch_mode)) },
                trailingIcon = {
                    SimpleCheckBox(editorPagePatchMode.value)
                },
                onClick = {
                    val newValue = !editorPagePatchMode.value
                    editorPagePatchMode.value = newValue

                    //更新配置文件
                    SettingsUtil.update {
                        it.editor.patchModeOn = newValue
                    }

                    closeMenu()
                }

            )

            DropdownMenuItem(
                //非readOnly目录才允许开启或关闭readonly状态，否则强制启用readonly状态且不允许关闭
//                enabled = enableMenuItem && !FsUtils.isReadOnlyDir(editorPageShowingFilePath.value),
                enabled = enableMenuItem,
                text = { Text(stringResource(R.string.read_only)) },
                trailingIcon = {
                    SimpleCheckBox(readOnlyMode.value)
                },
                onClick = {
                    //如果是从非readonly mode切换到readonly mode，则执行一次保存，然后再切换readonly mode
                    editorPageRequest.value = PageRequest.doSaveIfNeedThenSwitchReadOnly

                    closeMenu()
                }

            )

            DropdownMenuItem(
                //非readOnly目录才允许开启或关闭readonly状态，否则强制启用readonly状态且不允许关闭
//                enabled = enableMenuItem && !FsUtils.isReadOnlyDir(editorPageShowingFilePath.value),
                enabled = enableMenuItem,
                text = { Text(stringResource(R.string.show_undo_redo)) },
                trailingIcon = {
                    SimpleCheckBox(showUndoRedo.value)
                },
                onClick = {
                    val newValue = !showUndoRedo.value

                    // 更新页面变量
                    showUndoRedo.value = newValue

                    //更新配置文件
                    SettingsUtil.update {
                        it.editor.showUndoRedo = newValue
                    }

                    //关闭菜单
                    closeMenu()
                }

            )

            if(!isSubPageMode) {
                DropdownMenuItem(
                    //非readOnly目录才允许开启或关闭readonly状态，否则强制启用readonly状态且不允许关闭
                    enabled = enableMenuItem,
                    text = { Text(stringResource(R.string.show_in_files)) },
                    onClick = {
                        editorPageRequest.value = PageRequest.showInFiles

                        closeMenu()
                    }

                )

            }

            if(proFeatureEnabled(editorFontSizeTestPassed)) {
                DropdownMenuItem(
                    enabled = enableMenuItem,
                    text = { Text(stringResource(R.string.font_size)) },
                    onClick = {
                        closeMenu()

                        adjustFontSizeMode.value = true
                    }
                )
            }

            if(proFeatureEnabled(editorLineNumFontSizeTestPassed)) {
                DropdownMenuItem(
                    enabled = enableMenuItem && showLineNum.value,
                    text = { Text(stringResource(R.string.line_num_size)) },
                    onClick = {
                        closeMenu()

                        adjustLineNumFontSizeMode.value = true
                    }

                )
            }

            if(proFeatureEnabled(editorHideOrShowLineNumTestPassed)) {
                DropdownMenuItem(
                    //非readOnly目录才允许开启或关闭readonly状态，否则强制启用readonly状态且不允许关闭
                    enabled = enableMenuItem,
                    text = { Text(stringResource(R.string.show_line_num)) },
                    trailingIcon = {
                        SimpleCheckBox(showLineNum.value)
                    },
                    onClick = {
                        closeMenu()

                        //切换
                        showLineNum.value = !showLineNum.value

                        //保存
                        SettingsUtil.update {
                            it.editor.showLineNum = showLineNum.value
                        }
                    }
                )
            }

            if(proFeatureEnabled(editorEnableLineSelecteModeFromMenuTestPassed)) {
                val selectModeOn = editorPageTextEditorState.value.isMultipleSelectionMode

                DropdownMenuItem(
                    //非readOnly目录才允许开启或关闭readonly状态，否则强制启用readonly状态且不允许关闭
                    enabled = enableMenuItem,
                    text = { Text(stringResource(R.string.select_mode)) },
                    trailingIcon = {
                        SimpleCheckBox(selectModeOn)
                    },
                    onClick = {
                        closeMenu()

                        //如果是从非readonly mode切换到readonly mode，则执行一次保存，然后再切换readonly mode
                        editorPageRequest.value = PageRequest.editorSwitchSelectMode
                    }

                )
            }

        }
    }
}
