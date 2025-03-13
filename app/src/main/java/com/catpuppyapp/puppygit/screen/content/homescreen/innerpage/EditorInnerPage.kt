package com.catpuppyapp.puppygit.screen.content.homescreen.innerpage

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.catpuppyapp.puppygit.compose.ConfirmDialog
import com.catpuppyapp.puppygit.compose.ConfirmDialog2
import com.catpuppyapp.puppygit.compose.LongPressAbleIconBtn
import com.catpuppyapp.puppygit.compose.MySelectionContainer
import com.catpuppyapp.puppygit.compose.OpenAsAskReloadDialog
import com.catpuppyapp.puppygit.compose.OpenAsDialog
import com.catpuppyapp.puppygit.constants.Cons
import com.catpuppyapp.puppygit.constants.LineNum
import com.catpuppyapp.puppygit.constants.PageRequest
import com.catpuppyapp.puppygit.dto.FileSimpleDto
import com.catpuppyapp.puppygit.dto.UndoStack
import com.catpuppyapp.puppygit.fileeditor.texteditor.state.TextEditorState
import com.catpuppyapp.puppygit.fileeditor.texteditor.view.ScrollEvent
import com.catpuppyapp.puppygit.fileeditor.ui.composable.FileEditor
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.screen.functions.goToFileHistory
import com.catpuppyapp.puppygit.settings.SettingsUtil
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.utils.AppModel
import com.catpuppyapp.puppygit.utils.FsUtils
import com.catpuppyapp.puppygit.utils.Msg
import com.catpuppyapp.puppygit.utils.MyLog
import com.catpuppyapp.puppygit.utils.cache.Cache
import com.catpuppyapp.puppygit.utils.changeStateTriggerRefreshPage
import com.catpuppyapp.puppygit.utils.doJobThenOffLoading
import com.catpuppyapp.puppygit.utils.fileopenhistory.FileOpenHistoryMan
import com.catpuppyapp.puppygit.utils.getFileNameFromCanonicalPath
import com.catpuppyapp.puppygit.utils.getHumanReadableSizeStr
import com.catpuppyapp.puppygit.utils.getSecFromTime
import com.catpuppyapp.puppygit.utils.getShortUUID
import com.catpuppyapp.puppygit.utils.isFileSizeOverLimit
import com.catpuppyapp.puppygit.utils.showToast
import com.catpuppyapp.puppygit.utils.snapshot.SnapshotFileFlag
import com.catpuppyapp.puppygit.utils.snapshot.SnapshotUtil
import com.catpuppyapp.puppygit.utils.state.CustomStateSaveable
import com.catpuppyapp.puppygit.utils.state.mutableCustomStateListOf
import com.catpuppyapp.puppygit.utils.withMainContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

private const val TAG = "EditorInnerPage"
private const val stateKeyTag = "EditorInnerPage"

private var justForSaveFileWhenDrawerOpen = getShortUUID()

@Composable
fun EditorInnerPage(
    editorPageShowingFileName:String?,
    contentPadding: PaddingValues,
    currentHomeScreen: MutableIntState,
//    editorPageRequireOpenFilePath:MutableState<String>,
    editorPageShowingFilePath:MutableState<String>,
    editorPageShowingFileIsReady:MutableState<Boolean>,
    editorPageTextEditorState:CustomStateSaveable<TextEditorState>,
    /**
     * 此变量严格来说并不是Editor的上个状态，而是最后一个记录到Undo/Redo stack的状态
     */
    lastTextEditorState:CustomStateSaveable<TextEditorState>,
//    editorPageShowSaveDoneToast:MutableState<Boolean>,
    needRefreshEditorPage:MutableState<String>,
    isSaving:MutableState<Boolean>,  //这个变量只是用来判断是否可用保存按钮的
    isEdited:MutableState<Boolean>,
    showReloadDialog: MutableState<Boolean>,
    isSubPageMode:Boolean,
    showCloseDialog:MutableState<Boolean>,
    closeDialogCallback:CustomStateSaveable<(Boolean)->Unit>,
    loadingOn:(String)->Unit,
    loadingOff:()->Unit,
    saveOnDispose:Boolean,
    doSave: suspend ()->Unit,
    naviUp: () -> Unit,
    requestFromParent:MutableState<String>,
    editorPageShowingFileDto:CustomStateSaveable<FileSimpleDto>,
    lastFilePath:MutableState<String>,
    editorLastScrollEvent:CustomStateSaveable<ScrollEvent?>,
    editorListState:LazyListState,
    editorPageIsInitDone:MutableState<Boolean>,
    editorPageIsContentSnapshoted:MutableState<Boolean>,
    goToFilesPage:(path:String) -> Unit,
    drawerState: DrawerState? = null,  //只有editor作为顶级页面时用到这个变量，子页面没用，用来在drawer打开时保存文件
    goToLine:Int = LineNum.lastPosition,  //若大于0，打开文件后跳转到指定行；否则还是旧逻辑（跳转到上次退出前的第一行）。SubPageEditor会用到此变量，HomeScreen跳转来Editor的话，目前用不到，以后若有需要再传参
    editorSearchMode:MutableState<Boolean>,
    editorSearchKeyword:CustomStateSaveable<TextFieldValue>,
    readOnlyMode:MutableState<Boolean>,
    editorMergeMode:MutableState<Boolean>,
    editorShowLineNum:MutableState<Boolean>,
    editorLineNumFontSize:MutableIntState,
    editorFontSize:MutableIntState,
    editorAdjustLineNumFontSizeMode:MutableState<Boolean>,
    editorAdjustFontSizeMode:MutableState<Boolean>,
    editorLastSavedLineNumFontSize:MutableIntState,
    editorLastSavedFontSize:MutableIntState,

    openDrawer:()->Unit,
    editorOpenFileErr:MutableState<Boolean>,
    undoStack:CustomStateSaveable<UndoStack?>,

) {
    val allRepoParentDir = AppModel.allRepoParentDir;
//    val appContext = AppModel.appContext;
    val activityContext = LocalContext.current
    val exitApp = {
        AppModel.lastEditFile.value = ""
        AppModel.lastEditFileWhenDestroy.value = ""

        AppModel.exitApp()

        Unit
    }

    val saveLock = remember(editorPageShowingFilePath.value) { Cache.getOrPutByType(generateKeyForSaveLock(editorPageShowingFilePath.value), default = { Mutex() }) }
//    val saveLock = Mutex()  //其实这样也行，不过根据路径创建锁更严谨，跨页面也适用，比如如果首页的Editor正在保存，然后打开子页面，这时子页面必须等首页保存完成，但如果用这个和页面生命周期一样的锁，就无法实现那种效果了，但和页面生命周期一样的锁其实也够用

//    val isEdited = rememberSaveable{ mutableStateOf(false) }

    //BasicTextField用的变量
//    val editorPageShowingFileText = rememberSaveable{ mutableStateOf("") }  //BasicTextField用的文本，用来存储打开的文件的所有内容
//    val editorPageEditorFocusRequester = remember{ FocusRequester() }  //BasicTextField用的focusRequester
//    val lastFilePath = StateUtil.getRememberSaveableState(initValue = "")
    val editorPageShowingFileHasErr = rememberSaveable { mutableStateOf(false)}  //BasicTextField用的文本，用来存储打开的文件的所有内容
    val editorPageShowingFileErrMsg = rememberSaveable { mutableStateOf("")}  //BasicTextField用的文本，用来存储打开的文件的所有内容

    val editorPageFileSavedSuccess = stringResource(R.string.file_saved)
    val unknownErrStrRes = stringResource(R.string.unknown_err)

    //在编辑器弹出键盘用的，不过后来用simple editor库了，就不需要这个了
//    val keyboardCtl = LocalSoftwareKeyboardController.current

//    val editorPageOpenedFileMap = rememberSaveable{ mutableStateOf("{}") } //{canonicalPath:fileName}

//    val editorPageShowingFileCanonicalPath = rememberSaveable{ mutableStateOf("") } //当前展示的文件的真实路径

    val editorPageSetShowingFileErrWhenLoading:(errMsg:String)->Unit = { errMsg->
        editorPageShowingFileHasErr.value=true
        editorPageShowingFileErrMsg.value=errMsg
    }
    val editorPageClearShowingFileErrWhenLoading = {
        editorPageShowingFileHasErr.value=false
        editorPageShowingFileErrMsg.value=""
    }

    val saveFontSizeAndQuitAdjust = {
        editorAdjustFontSizeMode.value = false

        if(editorLastSavedFontSize.intValue != editorFontSize.intValue) {
            editorLastSavedFontSize.intValue = editorFontSize.intValue

            SettingsUtil.update {
                it.editor.fontSize = editorFontSize.intValue
            }
        }

        Unit
    }

    val saveLineNumFontSizeAndQuitAdjust = {
        editorAdjustLineNumFontSizeMode.value = false

        if(editorLastSavedLineNumFontSize.intValue != editorLineNumFontSize.intValue) {
            editorLastSavedLineNumFontSize.intValue = editorLineNumFontSize.intValue

            SettingsUtil.update {
                it.editor.lineNumFontSize = editorLineNumFontSize.intValue
            }
        }

        Unit

    }


    editorOpenFileErr.value = remember {
        derivedStateOf {editorPageShowingFileHasErr.value && !editorPageShowingFileIsReady.value}
    }.value

    //打开主页抽屉的时候，触发保存文件。子页面永远不会触发，子页面的保存已经写到点返回箭头里了，这里不用管它
    val justForSave = remember {
        derivedStateOf {
            val drawIsOpen = drawerState?.isOpen == true
            val needRequireSave = drawIsOpen && isEdited.value && !readOnlyMode.value
            if (needRequireSave) {
                requestFromParent.value = PageRequest.requireSave
            }
            "justForSave:"+"uuid="+getShortUUID() + ", drawerIsOpen=" + drawIsOpen + ", isEdited=" + isEdited.value +", needRequireSave="+needRequireSave
        }
    }
    justForSaveFileWhenDrawerOpen = justForSave.value  //得获取state值，不然不会触发计算，也就不会保存
//    println(justForSaveFileWhenDrawerOpen)  //test

//    val needAndReadyDoSave = remember{derivedStateOf { isEdited.value && !isSaving.value }}
//    val needAndReadyDoSave:()->Boolean = { isEdited.value && !isSaving.value }
    val needAndReadyDoSave:()->Boolean = { isEdited.value && !readOnlyMode.value }  //因为用了锁，只要拿到锁，肯定别人没在保存，所以无需判断isSaving，只判断文件是否编辑过即可，若没编辑过，说明之前保存成功后没动过，否则，应保存文件

    val doSaveInCoroutine = {
//        if(readyForSave.value) {
        //离开页面时，保存文件
        doJobThenOffLoading {
            saveLock.withLock {
                if(needAndReadyDoSave()) {
                    doSave()
                    MyLog.d(TAG, "#doSaveInCoroutine: file saved")
                }else{
                    MyLog.w(TAG, "#doSaveInCoroutine: will not save file, cause maybe other job already saved or saving")
                }
            }
        }
//        }
    }

    val doSaveNoCoroutine = suspend {
//        if(readyForSave.value) {
            //离开页面时，保存文件
        saveLock.withLock {
            if(needAndReadyDoSave()) {
                doSave()
                MyLog.d(TAG, "#doSaveNoCoroutine: file saved")
            }else{
                MyLog.w(TAG, "#doSaveNoCoroutine: will not save file, cause maybe other job already saved or saving")
            }
        }

//        }
    }

    //requireShowMsgToUser这个变量是为后台静默自动保存做铺垫
    val doSimpleSafeFastSaveInCoroutine = { requireShowMsgToUser:Boolean, requireBackupContent:Boolean, requireBackupFile:Boolean, contentSnapshotFlag:SnapshotFileFlag, fileSnapshotFlag:SnapshotFileFlag ->
//        if(readyForSave.value) {  //不做检查，别人有可能存，但万一存失败了呢？他失败了，我发现他在存，于是我也没存，那数据不就丢了？快速检查必然损失准确性。所以不如先拿锁，再做检查
            //离开页面时，保存文件
        doJobThenOffLoading {
            saveLock.withLock {
                if(needAndReadyDoSave()) {
                    try {
                        isSaving.value=true

                        val filePath = editorPageShowingFilePath.value
//                        val fileContent = editorPageTextEditorState.value.getAllText()
//                        val fileContent = null

                        val ret = FsUtils.simpleSafeFastSave(
//                            content = fileContent,
                            content = null,
                            editorState = editorPageTextEditorState.value,
                            trueUseContentFalseUseEditorState = false,
                            targetFilePath = filePath,
                            requireBackupContent = requireBackupContent,
                            requireBackupFile = requireBackupFile,
                            contentSnapshotFlag = contentSnapshotFlag,
                            fileSnapshotFlag = fileSnapshotFlag
                        )

                        if(ret.success()) {
                            isEdited.value=false
                            MyLog.d(TAG, "#doSimpleSafeFastSaveInCoroutine: file saved")
                            if(requireShowMsgToUser){
                                Msg.requireShow(activityContext.getString(R.string.file_saved))
                            }
                        }else {
                            isEdited.value=true
                            MyLog.e(TAG, "#doSimpleSafeFastSaveInCoroutine: save file err: ${ret.msg}")
                            if(requireShowMsgToUser) {
                                Msg.requireShow(ret.msg)
                            }
                        }

                    }finally {
                        isSaving.value=false
                    }
                }else{
                    MyLog.w(TAG, "#doSimpleSafeFastSaveInCoroutine: will not save file, cause maybe other job already saved or saving")
                }
            }
        }
//        }
    }

//    if(!isSubPageMode) {  //如果是子页面模式，不注册back handler，因为不需要双击退出
    //back handler block start
    val isBackHandlerEnable = rememberSaveable { mutableStateOf(true) }

    val backHandlerOnBack = getBackHandler(
        activityContext = activityContext,
        textEditorState = editorPageTextEditorState,
        isSubPage = isSubPageMode,
        isEdited = isEdited,
        readOnlyMode = readOnlyMode,
//        doSaveInCoroutine,
        doSaveNoCoroutine = doSaveNoCoroutine,
        searchMode = editorSearchMode,
        needAndReadyDoSave = needAndReadyDoSave,
        naviUp = naviUp,
        adjustFontSizeMode=editorAdjustFontSizeMode,
        adjustLineNumFontSizeMode=editorAdjustLineNumFontSizeMode,
        saveFontSizeAndQuitAdjust = saveFontSizeAndQuitAdjust,
        saveLineNumFontSizeAndQuitAdjust = saveLineNumFontSizeAndQuitAdjust,
        exitApp = exitApp,
        openDrawer=openDrawer,
        requestFromParent = requestFromParent

    )

    //更新最后打开文件状态变量并保存到配置文件（注：重复打开同一文件不会重复更新）
    val saveLastOpenPath = {path:String->
        if(path.isNotBlank() && lastFilePath.value != path) {
            lastFilePath.value = path
            //更新配置文件中记录的最后打开文件
            SettingsUtil.update {
                it.editor.lastEditedFilePath = path
            }
        }
    }

    //注册BackHandler，拦截返回键，实现双击返回和返回上级目录
    BackHandler(enabled = isBackHandlerEnable.value, onBack = {backHandlerOnBack()})
    //back handler block end

//    }
    val closeFile = {
//        showCloseDialog.value=false


        AppModel.lastEditFile.value = ""
        AppModel.lastEditFileWhenDestroy.value = ""



        isEdited.value = false
        isSaving.value=false
//        editorPageRequireOpenFilePath.value = ""
        //存上当前文件路径，要不然reOpen时还得从配置文件查，当然，app销毁后，此变量作废，依然需要从配置文件查
        saveLastOpenPath(editorPageShowingFilePath.value)

        editorPageShowingFilePath.value = ""
        editorPageShowingFileDto.value.fullPath=""
        editorPageClearShowingFileErrWhenLoading()  //关闭文件清除错误，不然文件标题变了，错误还在显示
        editorPageShowingFileIsReady.value = false

        undoStack.value=null
//        changeStateTriggerRefreshPage(needRefreshEditorPage)
    }

    if(showCloseDialog.value) {
        //若没编辑过直接关闭，否则需要弹窗确认
        if(!isEdited.value) {
            showCloseDialog.value=false
            closeFile()
        }else {
            ConfirmDialog(
                title = stringResource(id = R.string.close),
                text = stringResource(id = R.string.will_close_file_are_u_sure),
                okTextColor = MyStyleKt.TextColor.danger(),
                onCancel = { showCloseDialog.value=false }
            ) {
                showCloseDialog.value=false
                closeFile()
            }
        }
    }

    val reloadFile={
//        showReloadDialog.value=false

        //重新加载文件，需要弹窗确认“重新加载文件将丢失未保存的修改，确定？”，加载时需要有遮罩加载动画避免加载时用户操作
        //设置当前文件为请求打开的文件，然后走打开文件流程
        isEdited.value=false
        isSaving.value=false

        //确保重载：清空文件路径，这样和showingFilePath对比就永远不会为真，也就会百分百重载文件
        editorPageShowingFileDto.value.fullPath=""

//        editorPageRequireOpenFilePath.value = editorPageShowingFilePath.value
        editorPageShowingFileIsReady.value = false  //设置文件状态为未就绪，显示loading界面，好像有bug，TODO 需要测试能不能正常显示loading，整个大文件，测试一下

        //重载文件没必要清undo stack
//        undoStack.value = null

        changeStateTriggerRefreshPage(needRefreshEditorPage)
    }

    //重新加载文件确认弹窗
    if(showReloadDialog.value) {
        //未编辑过文件，直接重载
        if(!isEdited.value) {
            showReloadDialog.value=false  //立即关弹窗避免重入

            //检查源文件是否被外部修改过，若修改过，创建快照，然后再重载
            val newDto = FileSimpleDto.genByFile(File(editorPageShowingFilePath.value))

            if (newDto.lastModifiedTime != editorPageShowingFileDto.value.lastModifiedTime
                || newDto.sizeInBytes != editorPageShowingFileDto.value.sizeInBytes
            ) {
                val fileName = editorPageShowingFileDto.value.name
                MyLog.d(TAG,"#showReloadDialog: file '${fileName}' may changed by external, will save content snapshot before reload")
//                val content = editorPageTextEditorState.value.getAllText()
//                val content = null
                doJobThenOffLoading {
                    val snapRet = SnapshotUtil.createSnapshotByContentAndGetResult(
                        srcFileName = fileName,
                        fileContent = null,
                        editorState = editorPageTextEditorState.value,
                        trueUseContentFalseUseEditorState = false,
                        flag = SnapshotFileFlag.editor_content_BeforeReloadFoundSrcFileChanged
                    )
                    if(snapRet.hasError()) {
                        MyLog.e(TAG, "#showReloadDialog: save content snapshot before reload, err: "+snapRet.msg)
                    }
                }
            }

            //重载文件
            reloadFile()
        }else {
            // 编辑过文件，弹窗询问是否确认重载
            ConfirmDialog(
                title = stringResource(id = R.string.reload_file),
                text = stringResource(id = R.string.will_reload_file_are_u_sure),
                okTextColor = MyStyleKt.TextColor.danger(),
                onCancel = { showReloadDialog.value=false }
            ) {
                showReloadDialog.value=false
                reloadFile()
            }
        }
    }


    val showClearRecentFilesDialog = rememberSaveable { mutableStateOf(false) }
    if(showClearRecentFilesDialog.value) {
        ConfirmDialog2(
            title = stringResource(R.string.confirm),
            text = stringResource(R.string.clear_recent_files_confirm_text),
            okTextColor = MyStyleKt.TextColor.danger(),
            onCancel = {showClearRecentFilesDialog.value = false}
        ) {
            showClearRecentFilesDialog.value = false
            doJobThenOffLoading {
                try {
                    FileOpenHistoryMan.reset()
                    Msg.requireShow(activityContext.getString(R.string.cleared))
                }catch (e:Exception) {
                    Msg.requireShowLongDuration("err: ${e.localizedMessage}")
                    MyLog.e(TAG, "#ClearRecentFilesListFromEditor err: ${e.stackTraceToString()}")
                }

            }
        }
    }


    val showRecentFilesList = rememberSaveable { mutableStateOf(false) }
    // Pair(fileName, fileFullPath)
    val recentFileList = mutableCustomStateListOf(stateKeyTag, "recentFileList") { listOf<Pair<String, String>>() }




    val showBackFromExternalAppAskReloadDialog = rememberSaveable { mutableStateOf(false) }
    if(showBackFromExternalAppAskReloadDialog.value) {
        OpenAsAskReloadDialog(
            onCancel = { showBackFromExternalAppAskReloadDialog.value=false }
        ) {  // doReload
            //检查源文件是否被外部修改过，若修改过，创建快照，然后再重载
            val newDto = FileSimpleDto.genByFile(File(editorPageShowingFilePath.value))

            if (newDto.lastModifiedTime != editorPageShowingFileDto.value.lastModifiedTime
                || newDto.sizeInBytes != editorPageShowingFileDto.value.sizeInBytes
            ) {
                val fileName = editorPageShowingFileDto.value.name
                MyLog.d(TAG,"#showBackFromExternalAppAskReloadDialog: file '${fileName}' may changed by external, will save content snapshot before reload")
//                val content = editorPageTextEditorState.value.getAllText()
//                val content = null
                doJobThenOffLoading {
                    val snapRet = SnapshotUtil.createSnapshotByContentAndGetResult(
                        srcFileName = fileName,
                        fileContent = null,
                        editorState = editorPageTextEditorState.value,
                        trueUseContentFalseUseEditorState = false,
                        flag = SnapshotFileFlag.editor_content_BeforeReloadFoundSrcFileChanged_ReloadByBackFromExternalDialog
                    )
                    if(snapRet.hasError()) {
                        MyLog.e(TAG, "#showBackFromExternalAppAskReloadDialog: save content snapshot before reload, err: "+snapRet.msg)
                    }
                }
            }

            //reload文件
            showBackFromExternalAppAskReloadDialog.value=false
            reloadFile()
        }
    }

    val showOpenAsDialog = rememberSaveable { mutableStateOf(false)}
    val readOnlyForOpenAsDialog = rememberSaveable { mutableStateOf(false)}
    val openAsDialogFilePath = rememberSaveable { mutableStateOf("")}
    val fileName = remember{ derivedStateOf { getFileNameFromCanonicalPath(openAsDialogFilePath.value) }}
//    val showOpenInEditor = StateUtil.getRememberSaveableState(initValue = false)
    if(showOpenAsDialog.value) {
        OpenAsDialog(readOnly = readOnlyForOpenAsDialog, fileName = fileName.value, filePath = openAsDialogFilePath.value,
            openSuccessCallback = {
                //x 废弃，废案，万一用户就想保留陈旧内容呢？还是询问用户吧) 如果成功请求外部打开文件，把文件就绪设为假，下次返回就会重新加载文件，避免显示陈旧内容
                //如果请求外部打开成功，不管用户有无选择app（想实现成选择才询问是否重新加载，但无法判断）都询问是否重载文件
                showBackFromExternalAppAskReloadDialog.value=true  // 显示询问是否重载的弹窗
            }
        ) {
            //onClose
            showOpenAsDialog.value=false
        }
    }

    val checkPathThenGoToFilesPage = {
        val path = editorPageShowingFilePath.value
        if(path.isBlank()) {
            Msg.requireShow(activityContext.getString(R.string.invalid_path))
        }else {
            //如果文件不存在，显示个提示，然后跳转到file页面但不选中任何条目，否则会选中当前editor打开的文件
            if(!File(path).exists()) {
                Msg.requireShow(activityContext.getString(R.string.file_doesnt_exist))
            }

            goToFilesPage(path)
        }

    }

    //用不着这个了，在内部处理了
//    if(requestFromParent.value == PageRequest.backFromExternalAppAskReloadFile) {
//        PageRequest.clearStateThenDoAct(requestFromParent) {
//            showBackFromExternalAppAskReloadDialog.value=true
//        }
//    }
    if(requestFromParent.value == PageRequest.requireSave) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            if(needAndReadyDoSave()){
                doSaveInCoroutine()
            }
        }
    }
    if(requestFromParent.value == PageRequest.requireSaveFontSizeAndQuitAdjust) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            saveFontSizeAndQuitAdjust()
        }
    }
    if(requestFromParent.value == PageRequest.requireSaveLineNumFontSizeAndQuitAdjust) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            saveLineNumFontSizeAndQuitAdjust()
        }
    }
    if(requestFromParent.value == PageRequest.doSaveIfNeedThenSwitchReadOnly) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            doJobThenOffLoading {
                //开启readonly前保存文件
                if(needAndReadyDoSave()) {
                    doSaveNoCoroutine()
                }

                readOnlyMode.value = !readOnlyMode.value
            }
        }

    }
    //这里没必要用else，以免漏判，例如我在上面可能改了request变量，如果用else，可能会漏或等下次刷新才能收到请求，但如果全用if，前面改的后面立刻就能收到
    if(requestFromParent.value == PageRequest.requireOpenAs) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            if(editorPageShowingFilePath.value.isNotBlank()) {
                doJobThenOffLoading {
                    //如果编辑过文件，先保存再请求外部打开
                    //保存
                    doSaveNoCoroutine()

                    //请求外部程序打开文件
//                    readOnlyForOpenAsDialog.value = FsUtils.isReadOnlyDir(editorPageShowingFilePath.value)
                    openAsDialogFilePath.value = editorPageShowingFilePath.value
                    showOpenAsDialog.value=true

                }

            }else{
                Msg.requireShow(activityContext.getString(R.string.file_path_invalid))
            }
        }
    }

    if(requestFromParent.value == PageRequest.showInFiles) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            checkPathThenGoToFilesPage()
        }
    }

    if(requestFromParent.value == PageRequest.requireGoToFileHistory) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            goToFileHistory(editorPageShowingFilePath.value, activityContext)
        }
    }


//    if(needRefreshEditorPage.value) {
//        initEditorPage()
//        needRefreshEditorPage.value=false
//    }


    //别忘了底部加padding，目的是最后一行也可以放到屏幕中间编辑
    //render page
    /*
    LazyColumn(modifier = Modifier
        .padding(contentPadding)
        .fillMaxSize()
        .clickable {
            //点击，聚焦textfield
            editorPageEditorFocusRequester.requestFocus()
            keyboardCtl?.show()
        }
    ) {
        item {
            BasicTextField(
                modifier = Modifier
                .padding(bottom = 600.dp)
                .focusRequester(editorPageEditorFocusRequester),
                value = editorPageShowingFileText.value,
                onValueChange = { newText->
                    editorPageShowingFileText.value = newText
                }
           )
        }
    }
     */

//                Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
//                    TextEditor(
//                        modifier = Modifier.padding(bottom = 600.dp),
//                        textEditorState = editorPageTextEditorState.value,
//                        onChanged = { editorPageTextEditorState.value = it },
//                        contentPaddingValues = contentPadding,
//                    )
//                }

    //由父页面负责显示loading
//    val requireShowLoadingDialog = rememberSaveable { mutableStateOf(false) }
//    if(requireShowLoadingDialog.value) {
//        LoadingDialog()
//    }

//放父页面了
//    val doSave = {
//        //让页面知道正在保存文件
//        loadingOn(appContext.getString(R.string.saving))
//        isSaving.value=true
//        changeStateTriggerRefreshPage(needRefreshEditorPage)
//
//        //保存文件
//        FsUtils.saveFile(editorPageShowingFilePath.value, editorPageTextEditorState.value.getAllText())
//
//        //提示保存成功
//        Msg.requireShow(appContext.getString(R.string.file_saved))
////        requireShowLoadingDialog.value= false
//        isSaving.value=false
//        isEdited.value=false
//        loadingOff()
//        changeStateTriggerRefreshPage(needRefreshEditorPage)
//
//    }
//    if (editorPageShowingFilePath.value.isNotBlank() && editorPageShowingFileIsReady.value) {
//    if (editorPageShowingFileIsReady.value) {

//    } else {  // file not ready

    val ifLastPathOkThenDoOkActElseDoNoOkAct:((String) -> Unit, (String) ->Unit)->Unit = { okAct:(last:String)->Unit, noOkAct:(last:String)->Unit ->
        var last = lastFilePath.value
        if(last.isBlank()) {  //如果内存中有上次关闭文件的路径，直接使用，否则从配置文件加载
            last = SettingsUtil.getSettingsSnapshot().editor.lastEditedFilePath
        }

        //如果查无上次打开文件，吐司提示 "last file not found!"；否则打开文件
        //x 废弃，应在设置页面添加一个手动清除编辑器记录的位置信息的功能而不是一出异常就清除) 注：这里不要判断文件是否存在，留到reload时判断，在那里如果发现文件不存在将清除文件的上次编辑位置等信息
        if(last.isNotBlank() && File(last).exists()) {
            okAct(last)
        }else {
           noOkAct(last)
        }
    }



    val notOpenFile = !editorPageShowingFileHasErr.value && !editorPageShowingFileIsReady.value && editorPageShowingFilePath.value.isBlank()
    val loadingFile = !editorPageShowingFileHasErr.value && !editorPageShowingFileIsReady.value && editorPageShowingFilePath.value.isNotBlank()
    val somethingWrong = editorPageShowingFileHasErr.value || !editorPageShowingFileIsReady.value || editorPageShowingFilePath.value.isBlank()

    // open file err or no file opened or loading file
    if(
        ((editorOpenFileErr.value) // open file err
                || (notOpenFile)  // no opened any file
                || (loadingFile))  // loading file
        && somethingWrong  // load file err or file not ready or file path is blank
    ){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
            ,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val fontSize = MyStyleKt.TextSize.default
            val iconModifier = MyStyleKt.Icon.modifier

            //20240429: 这里必须把判断用到的状态变量写全，不然目前的Compose的刷新机制可能有bug，当if的第一个判断条件值没变时，可能会忽略后面的判断，有待验证
//        if((""+editorPageShowingFileHasErr.value+editorPageShowingFileIsReady.value+editorPageShowingFilePath.value).isNotEmpty()) {
            //下面的判断其实可用if else，但为了确保状态改变能刷新对应分支，全用的if
            // open file err
            if (editorOpenFileErr.value) {  //如果文件未加载就绪，加载出错显示错误，否则显示Loading...
                // err text
                MySelectionContainer {
                    Row {
                        Text(
                            text = stringResource(id = R.string.open_file_failed)+"\n"+editorPageShowingFileErrMsg.value,
                            color = MyStyleKt.ClickableText.errColor,
                            fontSize = fontSize

                        )

                    }

                }
                Spacer(modifier = Modifier.height(15.dp))

                // actions
                Row {
                    LongPressAbleIconBtn(
                        enabled = true,
                        iconModifier = iconModifier,
                        tooltipText = stringResource(R.string.open_as),
                        icon =  Icons.AutoMirrored.Filled.OpenInNew,
                        iconContentDesc = stringResource(id = R.string.open_as),
                    ) {
                        //点击用外部程序打开文件
                        requestFromParent.value = PageRequest.requireOpenAs
                    }

                    LongPressAbleIconBtn(
                        enabled = true,
                        iconModifier = iconModifier,
                        tooltipText = stringResource(R.string.reload),
                        icon =  Icons.Filled.Refresh,
                        iconContentDesc = stringResource(id = R.string.reload),
                    ) {
                        reloadFile()
                    }

                    //只有顶级页面的editor才显示show in files
                    if(!isSubPageMode){
                        LongPressAbleIconBtn(
                            enabled = true,
                            iconModifier = iconModifier,
                            tooltipText = stringResource(R.string.show_in_files),
                            icon =  Icons.Filled.DocumentScanner,
                            iconContentDesc = stringResource(id = R.string.show_in_files),
                        ) {
                            checkPathThenGoToFilesPage()
                        }

                    }

                    LongPressAbleIconBtn(
                        enabled = true,
                        iconModifier = iconModifier,
                        tooltipText = stringResource(R.string.close),
                        icon =  Icons.Filled.Close,
                        iconContentDesc = stringResource(id = R.string.close),
                    ) {
                        closeFile()
                    }
                }

            }

            //not open file (and no err)
            if (notOpenFile) {  //文件未就绪且无正在显示的文件且没错误
                Row {
//                    val spacerHeight=15.dp

                    if(!isSubPageMode) {  //仅在主页导航来的情况下才显示选择文件，否则显示了也不好使，因为显示子页面的时候，主页可能被销毁了，或者被覆盖了，改状态跳转页面不行，除非导航，但没必要导航，直接隐藏即可
                        LongPressAbleIconBtn(
                            enabled = true,
                            iconModifier = iconModifier,
                            tooltipText = stringResource(R.string.select_file),
                            icon =  Icons.Filled.Folder,
                            iconContentDesc = stringResource(id = R.string.select_file),
                        ) {
                            currentHomeScreen.intValue = Cons.selectedItem_Files
                        }

                        LongPressAbleIconBtn(
                            enabled = true,
                            iconModifier = iconModifier,
                            tooltipText = stringResource(R.string.show_last_in_files),
                            icon =  Icons.Filled.DocumentScanner,
                            iconContentDesc = stringResource(id = R.string.show_last_in_files),
                        ) {
                            ifLastPathOkThenDoOkActElseDoNoOkAct(okAct@{ last ->
                                goToFilesPage(last)
                            }) noOkAct@{
                                Msg.requireShowLongDuration(activityContext.getString(R.string.file_not_found))
                            }
                        }

                    }

                    //打开上个文件，常驻条目，但显示文案根据是否子页面有所不同
                    val reopenOrOpenLastText = if(isSubPageMode) stringResource(R.string.reopen) else stringResource(R.string.open_last)
                    LongPressAbleIconBtn(
                        enabled = true,
                        iconModifier = iconModifier,
                        tooltipText = reopenOrOpenLastText,
                        icon = ImageVector.vectorResource(R.drawable.outline_reopen_window_24),
                        iconContentDesc = reopenOrOpenLastText,
                    ) {
                        ifLastPathOkThenDoOkActElseDoNoOkAct(okAct@{ last ->
                            //x 废弃，如果用户在主编辑器打开一个只读文件，后在子编辑器打开一个非只读文件，再切换回主编辑器，用open last，那么就会以只读打开一个不需要只读的文件，既然无法正确维护，不如每次都重置) 只读关闭时，检查是否需要开启。（因为仅存在需要自动打开只读的情况（打开不允许编辑的目录下文件时），不存在需要自动关闭只读的情况，所以，仅在只读关闭时检查是否需要开启只读，若只读开启，用户想关可打开文件后关（当然，不允许编辑的文件除外，这种文件只读选项将保持开启并禁止关闭））
//                            if (!readOnlyMode.value) {
//                                readOnlyMode.value = FsUtils.isReadOnlyDir(last)  //避免打开文件，退出app，直接从editor点击 open last然后可编辑本不应该允许编辑的app内置目录下的文件
//                            }
//                                editorMergeMode.value = false  //此值在这无需重置
//                            readOnlyMode.value = FsUtils.isReadOnlyDir(last)  //避免打开文件，退出app，直接从editor点击 open last然后可编辑本不应该允许编辑的app内置目录下的文件

                            editorPageShowingFilePath.value = last
                            reloadFile()
                        }) noOkAct@{
                            Msg.requireShowLongDuration(activityContext.getString(R.string.file_not_found))
                        }
                    }

                    LongPressAbleIconBtn(
                        enabled = true,
                        iconModifier = iconModifier,
                        tooltipText = stringResource(R.string.recent_files),
                        icon = Icons.AutoMirrored.Filled.List,
                        iconContentDesc = stringResource(id = R.string.recent_files),
                    ) onclick@{
                        try {
                            val historyMap = FileOpenHistoryMan.getHistory().storage
                            if (historyMap.isEmpty()) {
                                Msg.requireShowLongDuration(activityContext.getString(R.string.recent_files_is_empty))
                                recentFileList.value.clear()
                                return@onclick
                            }

                            val recentFiles = historyMap
                                // sort
                                .toSortedMap({ k1, k2 ->
                                    val v1 = historyMap.get(k1)!!
                                    val v2 = historyMap.get(k2)!!
                                    // lastUsedTime descend sort
                                    if (v1.lastUsedTime > v2.lastUsedTime) -1 else 1
                                })
                                // map to list
                                .map { (k, v) ->
                                    // Pair(fileName, fileFullPath)
                                    Pair(getFileNameFromCanonicalPath(k), k)
                                }.let {
                                    // limit list size to 10
                                    it.subList(0, it.size.coerceAtMost(10))  // I think, recent file list, show 10 is good, if more, feels bad, to much files = no files, just make headache

                                }

                            // add sorted list to page state variable
                            recentFileList.value.clear()
                            recentFileList.value.addAll(recentFiles)

                            // show list
                            showRecentFilesList.value = true
                        }catch (e:Exception) {
                            Msg.requireShowLongDuration(e.localizedMessage ?: "get recent files err")
                            MyLog.e(TAG, "Recent Files onclick err: ${e.stackTraceToString()}")
                        }
                    }

                    //得离按钮近点，不然菜单出现位置很偏
                    if(showRecentFilesList.value) {
                        DropdownMenu(
                            expanded = showRecentFilesList.value,
                            offset = DpOffset(x=50.dp, y=0.dp),
                            onDismissRequest = { showRecentFilesList.value = false }
                        ) {
                            for((fileName, filePath) in recentFileList.value) {
                                DropdownMenuItem(
                                    text = { Text(fileName) },
                                    onClick = {
                                        // close dropdown menu
                                        showRecentFilesList.value=false

                                        //open file
                                        //只读关闭时，检查是否需要开启。（因为仅存在需要自动打开只读的情况（打开不允许编辑的目录下文件时），不存在需要自动关闭只读的情况，所以，仅在只读关闭时检查是否需要开启只读，若只读开启，用户想关可打开文件后关（当然，不允许编辑的文件除外，这种文件只读选项将保持开启并禁止关闭））
//                        if (!readOnlyMode.value) {
//                            readOnlyMode.value = FsUtils.isReadOnlyDir(filePath)  //避免打开文件，退出app，直接从editor点击 open last然后可编辑本不应该允许编辑的app内置目录下的文件
//                        }
//                                editorMergeMode.value = false  //此值在这无需重置
//                        readOnlyMode.value = FsUtils.isReadOnlyDir(filePath)  //避免打开文件，退出app，直接从editor点击 open last然后可编辑本不应该允许编辑的app内置目录下的文件

                                        editorPageShowingFilePath.value = filePath
                                        reloadFile()

                                    }
                                )

                            }

                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.clear)) },
                                onClick = {
                                    // close dropdown menu
                                    showRecentFilesList.value=false

                                    showClearRecentFilesDialog.value = true
                                }
                            )

                        }

                    }

                }
            }

            // loading file
            //没错误且文件未就绪且正在显示的文件路径不为空，那就是正在加载，显示loading
            if(loadingFile) {
                Text(stringResource(R.string.loading))
            }
//        }
        }
    }


    // file loaded (load file successfully)
    if(!editorPageShowingFileHasErr.value && editorPageShowingFileIsReady.value && editorPageShowingFilePath.value.isNotBlank()){
        val fileFullPath = editorPageShowingFilePath.value
//        val settingsSnapshot = SettingsUtil.getSettingsSnapshot()
        val fileEditedPos = FileOpenHistoryMan.get(fileFullPath)
        //每次打开文件，更新最后使用文件定位信息的时间，日后可实现删除超过指定期限没使用过的文件定位信息
//        fileEditedPos.lastUsedTime= getSecFromTime()
        //更新配置文件
//        SettingsUtil.update {
//            it.editor.filesLastEditPosition[fileFullPath] = fileEditedPos
//        }
        FileEditor(
            openDrawer = openDrawer,
            editorPageShowingFileName = editorPageShowingFileName,
            requestFromParent = requestFromParent,
            fileFullPath = fileFullPath,
            lastEditedPos = fileEditedPos,
            textEditorState = editorPageTextEditorState,
            onChanged = {newState:TextEditorState, trueSaveToUndoFalseRedoNullNoSave:Boolean?, clearRedoStack:Boolean ->
                editorPageTextEditorState.value = newState

                val lastState = lastTextEditorState.value
                // last state == null || 不等于新state的filesId，则入栈
                //这个fieldsId只是个粗略判断，即使一样也不能保证fields完全一样
                if(lastState.maybeNotEquals(newState)) {
//                    if(lastTextEditorState.value?.fields != newState.fields) {
                    //true或null，存undo; false存redo。null本来是在选择行之类的场景的，没改内容，可以不存，但我后来感觉存上比较好
                    val saved = if(trueSaveToUndoFalseRedoNullNoSave != false) {  // null or true
                        // redo的时候，添加状态到undo，不清redo stack，平时编辑文件的时候更新undo stack需清空redo stack
                        // trueSaveToUndoFalseRedoNullNoSave为null时是选择某行之类的不修改内容的状态变化，因此不用清redoStack
//                        if(trueSaveToUndoFalseRedoNullNoSave!=null && clearRedoStack) {
                        //改了下调用函数时传的这个值，在不修改内容时更新状态清除clearReadStack传了false，所以不需要额外判断trueSaveToUndoFalseRedoNullNoSave是否为null了
                        if(clearRedoStack) {
                            undoStack.value?.redoStackClear()
                        }

                        undoStack.value?.undoStackPush(lastState) ?: false
                    }else {  // false
                        undoStack.value?.redoStackPush(lastState) ?: false
                    }

                    if(saved) {
                        lastTextEditorState.value = newState
                    }
                }

//                isEdited.value=true
            },

            contentPadding = contentPadding,
            //如果外部Column进行Padding了，这里padding传0即可
//            contentPadding = PaddingValues(0.dp),

            isContentChanged = isEdited,   //谁调用onChanged，谁检查内容是否改变
            editorLastScrollEvent=editorLastScrollEvent,
            editorListState=editorListState,
            editorPageIsInitDone = editorPageIsInitDone,
            editorPageIsContentSnapshoted = editorPageIsContentSnapshoted,
            goToLine=goToLine,
            readOnlyMode=readOnlyMode.value,
            searchMode = editorSearchMode,
            searchKeyword=editorSearchKeyword.value.text,
            mergeMode=editorMergeMode.value,
            showLineNum=editorShowLineNum,
            lineNumFontSize=editorLineNumFontSize,
            fontSize=editorFontSize,
            undoStack = undoStack.value
        )
    }
//    }

    //按Home键把app切到后台时保存文件（准确地说是当前Activity失焦就自动保存，注意是Activity不是compose，这个函数监听的是Activity的生命周期事件)
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        val requireShowMsgToUser = true

        val requireBackupContent = true
        val requireBackupFile = true
        val contentSnapshotFlag = SnapshotFileFlag.editor_content_OnPause
        val fileSnapshotFlag = SnapshotFileFlag.editor_file_OnPause

        doSimpleSafeFastSaveInCoroutine(
            requireShowMsgToUser,
            requireBackupContent,
            requireBackupFile,
            contentSnapshotFlag,
            fileSnapshotFlag
        )
    }

    LaunchedEffect(needRefreshEditorPage.value) {
        try {
            //这里不需要loadingOn和loadingOff，靠editorPageShowingFileIsReady来判断是否加载完毕文件，历史遗留问题，这个页面的loading有点混乱
            // doJobThenOffLoading(loadingOn, loadingOff, appContext.getString(R.string.loading)){
            doJobThenOffLoading {
                try {
                    doInit(
                        activityContext = activityContext,
                        editorPageShowingFilePath = editorPageShowingFilePath,
                        editorPageShowingFileIsReady = editorPageShowingFileIsReady,
                        editorPageClearShowingFileErrWhenLoading = editorPageClearShowingFileErrWhenLoading,
                        editorPageTextEditorState = editorPageTextEditorState,
                        unknownErrStrRes = unknownErrStrRes,
                        editorPageSetShowingFileErrWhenLoading = editorPageSetShowingFileErrWhenLoading,
                        loadingOn = loadingOn,
                        loadingOff = loadingOff,
                        appContext = activityContext,
                        pageRequest = requestFromParent,
                        editorPageShowingFileDto = editorPageShowingFileDto,
                        isSubPage = isSubPageMode,
                        editorLastScrollEvent = editorLastScrollEvent,
                        editorPageIsInitDone = editorPageIsInitDone,
                        isEdited = isEdited,
                        isSaving = isSaving,
                        isContentSnapshoted = editorPageIsContentSnapshoted,
                        readOnlyMode = readOnlyMode,
                        mergeMode = editorMergeMode,
                        saveLastOpenPath = saveLastOpenPath,
                        undoStack = undoStack,
                        lastTextEditorState = lastTextEditorState
                    )
                }catch (e:Exception) {
                    Msg.requireShowLongDuration("init Editor err: ${e.localizedMessage}")
                    MyLog.e(TAG, "#init Editor page err: ${e.stackTraceToString()}")
                }
            }

        } catch (e: Exception) {
            MyLog.e(TAG, "EditorInnerPage#LaunchedEffect() err: ${e.stackTraceToString()}")
        }

        //test passed
//        delay(10*1000)
//        AppModel.exitApp()  // 测试正常退出能否保存，结果：能
//        appContext.findActivity()?.recreate()
//        throw RuntimeException("throw exception test")  // 测试发生异常能否保存，结果：能
        //test
    }

    DisposableEffect(Unit) {
        onDispose {
            // 由于是绝对路径，所以至少有个"/"，因此用isNotBlank()判断而不是isNotEmpty()
            //打开成功或打开失败，设置下文件路径，这样在旋转屏幕后就会再次重试打开文件以恢复之前的状态
            // 由于存在 fileIsReady或openFileErr还没更新但文件路径不为空就销毁组件的情况，
            //  所以，这里不检查文件是否成功打开，直接赋值，理论上来说，只要在关闭文件的逻辑清空路径就可确保这里不会出现反直觉的用户体验，而我已在关闭文件时清空了路径，但我不确定效果如何，
            //  测试一下，若没问题就这样，否则改成先检测文件ready或err，再保存路径
            //改成在读取完后保存此路径了
//            AppModel.lastEditFile = editorPageShowingFilePath.value

            //20240327:尝试解决加载文件内容未更新的bug
            editorPageShowingFileIsReady.value = false

            if(saveOnDispose) {
                doSaveInCoroutine()
            }

            //保存最后打开文件路径
            saveLastOpenPath(editorPageShowingFilePath.value)


//            editorPageShowingFilePath.value = ""
        }
    }
}

private suspend fun doInit(
    activityContext:Context,

//    editorPageRequireOpenFilePath: MutableState<String>,
    editorPageShowingFilePath: MutableState<String>,
    editorPageShowingFileIsReady: MutableState<Boolean>,
    editorPageClearShowingFileErrWhenLoading: () -> Unit,
    editorPageTextEditorState: CustomStateSaveable<TextEditorState>,
    unknownErrStrRes: String,
    editorPageSetShowingFileErrWhenLoading: (errMsg: String) -> Unit,
    loadingOn: (String) -> Unit,
    loadingOff: () -> Unit,
    appContext: Context,
    pageRequest:MutableState<String>,
    editorPageShowingFileDto: CustomStateSaveable<FileSimpleDto>,
    isSubPage: Boolean,
    editorLastScrollEvent:CustomStateSaveable<ScrollEvent?>,
    editorPageIsInitDone:MutableState<Boolean>,
    isEdited:MutableState<Boolean>,
    isSaving:MutableState<Boolean>,
    isContentSnapshoted:MutableState<Boolean>,
    readOnlyMode: MutableState<Boolean>,
    mergeMode: MutableState<Boolean>,
    saveLastOpenPath:(path:String)->Unit,
    undoStack: CustomStateSaveable<UndoStack?>,
    lastTextEditorState: CustomStateSaveable<TextEditorState>,

) {

    //目前这个函数只有加载文件的逻辑，所以判断下，如果已加载，就直接返回，以后如果添加其他逻辑，把这行注释即可，其他不用改，因为加载文件前又做了一次此判断
    if (editorPageShowingFileIsReady.value) return;



    //保存后不改变needrefresh就行了，没必要传这个变量
    //保存文件时会设置这个变量，因为保存的内容本来就是最新的，不需要重新加载
//        if(pageRequest.value ==PageRequest.needNotReloadFile) {
//            PageRequest.clearStateThenDoAct(pageRequest){}
//            return@doJobThenOffLoading
//        }

    //打开文件
    //告知组件文件还未就绪（例如 未加载完毕）(20240326:尝试解决加载文件内容未更新的bug，把这行挪到了上面)
//        editorPageShowingFileIsReady.value = false  //20240429:文件未就绪应归调用者设置
    //如果存在待打开的文件，则打开，文件可能来自从文件管理器的点击

    // 加载文件 (load file)
    if (!editorPageShowingFileIsReady.value) {  //从文件管理器跳转到editor 或 打开文件后从其他页面跳转到editor
        //准备文件路径，开始
        //优先打开从文件管理器跳转来的文件，如果不是跳转来的，打开之前显示的文件
        if(editorPageShowingFilePath.value.isBlank()) {
            editorPageShowingFilePath.value = AppModel.lastEditFileWhenDestroy.value
            AppModel.lastEditFileWhenDestroy.value = ""
        }
        //保存上次打开文件路径
        AppModel.lastEditFile.value = editorPageShowingFilePath.value

        //到这，文件路径就确定了
        val requireOpenFilePath = editorPageShowingFilePath.value
        if (requireOpenFilePath.isBlank()) {
            //这时页面会显示选择文件和打开上次文件，这里无需处理
            return
        }
        //准备文件路径，结束

        //执行到这里，一定有一个非空的文件路径

        // 之前为null或打开了另一个文件，创建个新UndoStack
        if(undoStack.value==null || undoStack.value?.filePath != requireOpenFilePath) {
            undoStack.value = UndoStack(requireOpenFilePath)
        }

        //清除错误信息，如果打开文件时出错，会重新设置错误信息
        editorPageClearShowingFileErrWhenLoading()
        //读取文件内容
        try {
            val file = File(requireOpenFilePath)
            //如果文件不存在，抛异常，然后会显示错误信息给用户
            if (!file.exists()) {
                //如果当前显示的内容不为空，为当前显示的内容创建个快照，然后抛异常
//                val content = editorPageTextEditorState.value.getAllText()
//                val content = null
                if(editorPageTextEditorState.value.contentIsEmpty().not() && !isContentSnapshoted.value) {
                    MyLog.w(TAG, "#loadFile: file doesn't exist anymore, but content is not empty, will create snapshot for content")
                    doJobThenOffLoading {
                        val fileName = File(requireOpenFilePath).name
                        val snapRet = SnapshotUtil.createSnapshotByContentAndGetResult(
                            srcFileName = fileName,
                            fileContent = null,
                            editorState = editorPageTextEditorState.value,
                            trueUseContentFalseUseEditorState = false,
                            flag = SnapshotFileFlag.editor_content_FileNonExists_Backup
                        )
                        if (snapRet.hasError()) {
                            MyLog.e(TAG, "#loadFile: create content snapshot for '$requireOpenFilePath' err: ${snapRet.msg}")

                            Msg.requireShowLongDuration("save content snapshot for '$fileName' err:" + snapRet.msg)
                        }else {
                            isContentSnapshoted.value=true
                        }
                    }
                }
                //抛异常
                throw RuntimeException(activityContext.getString(R.string.err_file_doesnt_exist_anymore))
            }

            if (!file.isFile) {
                throw RuntimeException(activityContext.getString(R.string.err_target_is_not_a_file))
            }

            //检查文件大小，太大了打开会有问题，要么崩溃，要么无法保存
            //如果文件大小超出app支持的最大限制，提示错误
            val settings = SettingsUtil.getSettingsSnapshot()
            val maxSizeLimit = settings.editor.maxFileSizeLimit
            if (isFileSizeOverLimit(file.length(), limit = maxSizeLimit)) {
//                    editorPageSetShowingFileErrWhenLoading("Err: Doesn't support open file over "+Cons.editorFileSizeMaxLimitForHumanReadable)
                throw RuntimeException(activityContext.getString(R.string.err_doesnt_support_open_file_over_limit) + "(" + getHumanReadableSizeStr(maxSizeLimit) + ")")
            }


//            if(debugModeOn) {
//                println("editorPageShowingFileDto.value.fullPath: "+editorPageShowingFileDto.value.fullPath)
//            }

            //如果文件修改时间和大小没变，不重新加载文件
            //x 废弃 20240503 subPage为什么要百分百重载呢？再说subPage本来就是百分百重载啊，因为一关页面再开不就重载了吗？没必要在这特意区分是否subPage！) subPage百分百重载文件；
            // 注：从Files点击百分百重载，因为请求打开文件时清了dto
            if (editorPageShowingFileDto.value.fullPath.isNotBlank() && editorPageShowingFileDto.value.fullPath == requireOpenFilePath) {
                val newDto = FileSimpleDto.genByFile(file)
                if (newDto.lastModifiedTime == editorPageShowingFileDto.value.lastModifiedTime
                    && newDto.sizeInBytes == editorPageShowingFileDto.value.sizeInBytes
                ) {
                    MyLog.d(TAG,"EditorInnerPage#loadFile: file '${editorPageShowingFileDto.value.name}' not change, skip reload")
                    //文件可能没改变，放弃加载
                    editorPageShowingFileIsReady.value = true
                    return
                }
            }

            //TODO 其实漏了一种情况，不过不是很重要而且发生的概率几乎为0，那就是：如果用户编辑了文件但没保存，
            // 然后切换窗口，在外部修改文件，再切换回来，editor发现文件被修改过，这时会自动重载。
            // 问题就在于自动重载，应该改成询问用户是否重载，或者自动重载前先创建当前未保存内容的快照。
            // 但由于目前一切换出editor就自动保存了，
            // 所以其实不会发生“内容没保存的状态下发现文件被修改过”的情况，只会发生文件保存了，
            // 然后发现文件被外部修改过的情况，而这种情况直接重载就行，因为在之前保存的时候就已经创建了内容
            // 和当时的源文件的快照，而那个内容快照就是重载前的内容(因为保存后在editor没修改过，所以当时保存的后重载前editor显示的是同一内容)。

            MyLog.d(TAG,"EditorInnerPage#loadFile: will load file '${requireOpenFilePath}'")

            //重新读取文件把滚动位置设为null以触发定位到配置文件记录的滚动位置
            //如果打开文件报错，这几个值也该是false，所以在打开文件前就设置这几个值
            isEdited.value=false
            isSaving.value=false

            editorPageIsInitDone.value=false
            editorLastScrollEvent.value=null

            //读取文件内容
//            editorPageTextEditorState.value = TextEditorState.create(FsUtils.readFile(requireOpenFilePath))
//                editorPageTextEditorState.value = TextEditorState.create(FsUtils.readLinesFromFile(requireOpenFilePath))
            editorPageTextEditorState.value = TextEditorState.create(file = File(requireOpenFilePath), fieldsId = TextEditorState.newId())
            lastTextEditorState.value = editorPageTextEditorState.value
//                lastTextEditorState.value = editorPageTextEditorState.value

            isContentSnapshoted.value=false
            //文件就绪
            editorPageShowingFileIsReady.value = true
//                editorPageShowingFilePath.value = requireOpenFilePath  //左值本身就是右值，无变化，无需赋值

            //更新dto，这个dto和重载有关，和视图无关，页面是否发现它修改都无所谓，所以用更新其实也可以。
//            FileSimpleDto.updateDto(editorPageShowingFileDto.value, file)
            //这的file是只读，没改过，所以直接用file即可，若改过，我不确定是否能获取到最新修改，应该能，若没把握，可重新创建个file
            editorPageShowingFileDto.value = FileSimpleDto.genByFile(file)

            //子页面不记路径到配置文件 (20240821 废弃，原因：很多时候，我用子页面打开文件，然后我期望在首页editor用open last打开那个文件，结果没记住
//            if(!isSubPage) {
            //若不想让子页面editor记住上次打开文件路径，把更新配置文件中记录最后打开文件的代码放这里即可
//            }

            //更新最后打开文件状态变量（注：重复打开同一文件不会重复更新）
            //20240823: 改成关闭文件或销毁组件时保存了，打开时没必要保存了
//            saveLastOpenPath(requireOpenFilePath)

            // update file last used time
            FileOpenHistoryMan.touch(requireOpenFilePath)
        } catch (e: Exception) {
            editorPageShowingFileIsReady.value = false
            //设置错误信息
            //显示提示
            editorPageSetShowingFileErrWhenLoading(e.localizedMessage ?: unknownErrStrRes)
            //清除配置文件中记录的当前文件编辑位置信息
            //应该提供一个选项来移除保存的最后编辑位置信息而不是一出异常就移除，万一用户只是临时把文件改下名，然后又改回来呢？或者用户手动改了权限，导致无法读取文件然后又改回来了，这些情况下位置信息就没必要删，总之删除位置信息应改成手动删除，而不是一出异常就删
//            SettingsUtil.update {
//                it.editor.filesLastEditPosition.remove(requireOpenFilePath)
//            }
            //记录日志
            MyLog.e(TAG, "EditorInnerPage#loadFile(): " + e.stackTraceToString())
        }

        //如果文件加载成功，添加它到打开的文件列表
//            if (editorPageShowingFileIsReady.value) {
//                //如果当前请求打开的文件不在编辑器的已打开文件列表，则添加
//                val openedFileMap = JSONObject(editorPageOpenedFileMap.value)
//                if (!openedFileMap.has(requireOpenFilePath)) {
//                    openedFileMap.put(
//                        requireOpenFilePath,
//                        getFileNameFromCanonicalPath(requireOpenFilePath)
//                    )
//                    editorPageRequireOpenFilePath.value = ""  //清空待打开的文件列表
//                    editorPageOpenedFileMap.value = openedFileMap.toString()  //存储当前列表到字符串
//                }
//            }

    }

//        else {  //从抽屉菜单点击Editor项进入Editor
//            //读取文件列表，展示当前打开的文件

//            //update editorOpenedFileMap from db
//            //update editorCurShowFile from db
//            //update editorPageShowingFileIsReady to true
//        }

}


@Composable
private fun getBackHandler(
    activityContext: Context,
    textEditorState: CustomStateSaveable<TextEditorState>,
    isSubPage: Boolean,
    isEdited: MutableState<Boolean>,
    readOnlyMode: MutableState<Boolean>,
//    doSaveInCoroutine: () -> Unit,
    doSaveNoCoroutine:suspend ()->Unit,
    searchMode:MutableState<Boolean>,
    needAndReadyDoSave:()->Boolean,
    naviUp:()->Unit,
    adjustFontSizeMode:MutableState<Boolean>,
    adjustLineNumFontSizeMode:MutableState<Boolean>,
    saveFontSizeAndQuitAdjust:()->Unit,
    saveLineNumFontSizeAndQuitAdjust:()->Unit,
    exitApp: () -> Unit,
    openDrawer:()->Unit,
    requestFromParent: MutableState<String>

): () -> Unit {
    val backStartSec = rememberSaveable { mutableLongStateOf( 0) }
    val pressBackAgainForExitText = stringResource(R.string.press_back_again_to_exit);
    val showTextAndUpdateTimeForPressBackBtn = {
        openDrawer()
        showToast(activityContext, pressBackAgainForExitText, Toast.LENGTH_SHORT)
        backStartSec.longValue = getSecFromTime() + Cons.pressBackDoubleTimesInThisSecWillExit
    }

    val backHandlerOnBack = {
        //是多选模式则退出多选，否则检查是否编辑过文件，若编辑过则保存，然后判断是否子页面，是子页面则返回上级页面，否则显示再按返回退出的提示
        if(textEditorState.value.isMultipleSelectionMode) {  //退出编辑器多选模式
            requestFromParent.value = PageRequest.editorCreateCancelledState
        }else if(searchMode.value){
            searchMode.value = false
        }else if(adjustFontSizeMode.value){
            saveFontSizeAndQuitAdjust()
        }else if(adjustLineNumFontSizeMode.value){
            saveLineNumFontSizeAndQuitAdjust()
        }else {  //双击返回逻辑
            doJobThenOffLoading {
                if(needAndReadyDoSave()) {  //文件编辑过，先保存，再允许退出
                    doSaveNoCoroutine()
                    //20240509: 修改成保存完文件就返回，这样就可把返回键当保存功能用了
                    return@doJobThenOffLoading
                }

                //保存文件后再按返回键则执行返回逻辑
                if(!isSubPage) {  //一级页面
                    //如果在两秒内按返回键，就会退出，否则会提示再按一次可退出程序
                    if (backStartSec.longValue > 0 && getSecFromTime() <= backStartSec.longValue) {  //大于0说明不是第一次执行此方法，那检测是上次获取的秒数，否则直接显示“再按一次退出app”的提示
                        exitApp()
                    } else {
                        withMainContext {
                            showTextAndUpdateTimeForPressBackBtn()
                        }
                    }
                }else {  //作为子页面
                    withMainContext {
                        naviUp()
                    }
                }
            }
        }

        Unit
    }

//    if(isSubPage) {  //作为子页面
//        backHandlerOnBack = {
//            //如果编辑过，保存然后返回上级页面，否则直接返回上级页面
//            if(textEditorState.value.isMultipleSelectionMode) {  //退出编辑器多选模式
//                textEditorState.value = textEditorState.value.createCancelledState()
//            }else {
//                doJobThenOffLoading {
//                    //如果文件编辑过，保存，然后再返回上级页面
//                    if (isEdited.value) {
//                        doSaveNoCoroutine()
//                    }
//
//                    withMainContext {
//                        naviUp()
//                    }
//                }
//            }
//        }
//    }

    return backHandlerOnBack
}

/**
 * generate cache key for save lock, you can use this key get lock obj from Cache for saving file
 * @return a cache key, e.g. "editor_save_lock:/path/to/file"
 */
private fun generateKeyForSaveLock(filePath:String):String {
    return Cache.Key.editorPageSaveLockPrefix + Cache.keySeparator + filePath
}
