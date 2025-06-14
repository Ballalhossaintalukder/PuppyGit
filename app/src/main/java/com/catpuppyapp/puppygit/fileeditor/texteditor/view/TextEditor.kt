package com.catpuppyapp.puppygit.fileeditor.texteditor.view

import android.os.Parcelable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.compose.AcceptButtons
import com.catpuppyapp.puppygit.compose.ClickableText
import com.catpuppyapp.puppygit.compose.ConfirmDialog
import com.catpuppyapp.puppygit.compose.ConfirmDialog2
import com.catpuppyapp.puppygit.constants.LineNum
import com.catpuppyapp.puppygit.constants.PageRequest
import com.catpuppyapp.puppygit.dev.bug_Editor_GoToColumnCantHideKeyboard_Fixed
import com.catpuppyapp.puppygit.dev.bug_Editor_SelectColumnRangeOfLine_Fixed
import com.catpuppyapp.puppygit.dto.UndoStack
import com.catpuppyapp.puppygit.fileeditor.texteditor.state.FindDirection
import com.catpuppyapp.puppygit.fileeditor.texteditor.state.SelectionOption
import com.catpuppyapp.puppygit.fileeditor.texteditor.state.TextEditorState
import com.catpuppyapp.puppygit.fileeditor.texteditor.state.TextFieldState
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.screen.shared.FilePath
import com.catpuppyapp.puppygit.settings.AppSettings
import com.catpuppyapp.puppygit.settings.FileEditedPos
import com.catpuppyapp.puppygit.settings.SettingsUtil
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.ui.theme.Theme
import com.catpuppyapp.puppygit.utils.AppModel
import com.catpuppyapp.puppygit.utils.Msg
import com.catpuppyapp.puppygit.utils.MyLog
import com.catpuppyapp.puppygit.utils.PatchUtil
import com.catpuppyapp.puppygit.utils.UIHelper
import com.catpuppyapp.puppygit.utils.cache.Cache
import com.catpuppyapp.puppygit.utils.doJobThenOffLoading
import com.catpuppyapp.puppygit.utils.fileopenhistory.FileOpenHistoryMan
import com.catpuppyapp.puppygit.utils.forEachIndexedBetter
import com.catpuppyapp.puppygit.utils.replaceStringResList
import com.catpuppyapp.puppygit.utils.state.CustomBoxSaveable
import com.catpuppyapp.puppygit.utils.state.CustomStateSaveable
import com.catpuppyapp.puppygit.utils.state.mutableCustomStateOf
import kotlinx.parcelize.Parcelize

private const val TAG ="TextEditor"

@Parcelize
class ExpectConflictStrDto(
    var conflictStartStr: String = "",
    var conflictSplitStr: String = "",
    var conflictEndStr: String = "",

    var curConflictStr: String = conflictStartStr,
    var curConflictStrMatched: Boolean = false,
):Parcelable {
    fun reset() {
        curConflictStr = conflictStartStr
        curConflictStrMatched = false
    }

    fun getNextExpectConflictStr():String{
        return if(curConflictStr == conflictStartStr) {
            conflictSplitStr
        }else if(curConflictStr == conflictSplitStr) {
            conflictEndStr
        }else { // curStr == settings.editor.conflictEndStr
            conflictStartStr
        }
    }

    fun getCurAndNextExpect():Pair<Int,Int> {
        val curExpect = if(curConflictStr.startsWith(conflictStartStr)){
            0
        }else if(curConflictStr.startsWith(conflictSplitStr)) {
            1
        }else {
            2
        }

        val nextExcept = if(curExpect + 1 > 2) 0 else curExpect+1

        return Pair(curExpect, nextExcept)
    }
}

typealias DecorationBoxComposable = @Composable (
    index: Int,
    size: Int,  // list.size
    isSelected: Boolean,
    currentField: TextFieldState,
    focusingIdx:Int,
    isMultiSelectionMode: Boolean,
    innerTextField: @Composable (modifier: Modifier) -> Unit
) -> Unit

//光标handle和选中文本颜色设置
private val customTextSelectionColors = MyStyleKt.TextSelectionColor.customTextSelectionColors
private val customTextSelectionColors_darkMode = MyStyleKt.TextSelectionColor.customTextSelectionColors_darkMode

//隐藏光标，没选中文本时使用这个
private val customTextSelectionColors_hideCursorHandle = MyStyleKt.TextSelectionColor.customTextSelectionColors_hideCursorHandle




@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TextEditor(
    stateKeyTag:String,

    ignoreFocusOnce: CustomBoxSaveable<Boolean>,
    undoStack:UndoStack,
    curPreviewScrollState: ScrollState,
    requireEditorScrollToPreviewCurPos:MutableState<Boolean>,
    editorPageShowingFileName:String?,
    requestFromParent:MutableState<String>,
    fileFullPath:FilePath,
    lastEditedPos: FileEditedPos,
    textEditorState: TextEditorState,
//    onChanged:(newState:TextEditorState, trueSaveToUndoFalseRedoNullNoSave:Boolean?, clearRedoStack:Boolean)->Unit,
    modifier: Modifier = Modifier,
    contentPaddingValues: PaddingValues = PaddingValues(),
    lastScrollEvent: CustomStateSaveable<ScrollEvent?>,
    listState: LazyListState,
    editorPageIsInitDone:MutableState<Boolean>,
    goToLine:Int,  // is line number, not line index
    readOnlyMode:Boolean,
    searchMode:MutableState<Boolean>,
    mergeMode:Boolean,
    patchMode:Boolean,
    searchKeyword:String,
    fontSize: MutableIntState,
    fontColor: Color,

    decorationBox: DecorationBoxComposable = { _, _, _, _, _,_, innerTextField -> innerTextField(Modifier) },
) {
    val stateKeyTag = Cache.getComponentKey(stateKeyTag, TAG)

    val density = LocalDensity.current
    val deviceConfiguration = AppModel.getCurActivityConfig()

    val conflictOursBlockBgColor = UIHelper.getConflictOursBlockBgColor()
    val conflictTheirsBlockBgColor = UIHelper.getConflictTheirsBlockBgColor()
    val conflictStartLineBgColor = UIHelper.getConflictStartLineBgColor()
    val conflictSplitLineBgColor = UIHelper.getConflictSplitLineBgColor()
    val conflictEndLineBgColor = UIHelper.getConflictEndLineBgColor()

    val acceptOursBtnColor = UIHelper.getAcceptOursIconColor()
    val acceptTheirsBtnColor = UIHelper.getAcceptTheirsIconColor()
    val acceptBothBtnColor = UIHelper.getAcceptBothIconColor()
    val rejectBothBtnColor = UIHelper.getRejectBothIconColor()

    val activityContext = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val inDarkTheme = Theme.inDarkTheme
//    val textEditorState by rememberUpdatedState(newValue = textEditorState)
    val (virtualWidth, virtualHeight) = UIHelper.Size.editorVirtualSpace()

//    val focusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
//    val focusRequesters = remember { mutableStateListOf<FocusRequester>() }  //不能用list，滚动两下页面就会报错

    val settings = remember { SettingsUtil.getSettingsSnapshot() }
    val conflictKeyword = remember(settings.editor.conflictStartStr) { mutableStateOf(settings.editor.conflictStartStr) }




    //最后显示屏幕范围的第一行的索引
//    var lastFirstVisibleLineIndexState  by remember { mutableIntStateOf(lastEditedPos.firstVisibleLineIndex) }

    val showGoToLineDialog  = rememberSaveable { mutableStateOf(false) }
    val goToLineValue  = mutableCustomStateOf(stateKeyTag, "goToLineValue") { TextFieldValue("") }
    val lastVisibleLineState  = rememberSaveable { mutableStateOf(0) }

    //是否显示光标拖手(cursor handle
    val needShowCursorHandle = rememberSaveable { mutableStateOf(false) }

    //这俩值会在组件销毁时写入配置文件以记录滚动位置(当前画面第一个可见行)和最后编辑位置
    val lastEditedLineIndexState  = rememberSaveable { mutableIntStateOf(lastEditedPos.lineIndex) }
    val lastEditedColumnIndexState = rememberSaveable { mutableIntStateOf(lastEditedPos.columnIndex) }

    //此值为假或readOnly为真则不显示键盘
    //没找到合适的方法手动启用，因此默认启用，暂时没更改的场景
    val allowKeyboard = rememberSaveable { mutableStateOf(true) }

    val expectConflictStrDto = rememberSaveable(settings.editor.conflictStartStr, settings.editor.conflictSplitStr, settings.editor.conflictEndStr) {
        mutableStateOf(
            ExpectConflictStrDto(
                conflictStartStr = settings.editor.conflictStartStr,
                conflictSplitStr = settings.editor.conflictSplitStr,
                conflictEndStr = settings.editor.conflictEndStr
            )
        )
    }


    val nextSearchPos = rememberSaveable { mutableStateOf(SearchPos.NotFound) }

    val lastFoundPos = rememberSaveable { mutableStateOf(SearchPos.NotFound) }

    val initSearchPos = {
        //把起始搜索位置设置为当前第一个可见行的第一列
//        lastSearchPos.value = SearchPos(lazyColumnState.firstVisibleItemIndex, 0)

        //从上次编辑位置开始搜索
        if(!searchMode.value || lastFoundPos.value== SearchPos.NotFound  //没开搜索模式，或没匹配到关键字，一律使用上次编辑行+列
            || lastFoundPos.value.lineIndex!=lastEditedLineIndexState.intValue  //搜索并匹配后，用户点了其他行
            || lastFoundPos.value.columnIndex!=lastEditedColumnIndexState.intValue  //搜索并匹配后，用户点了其他列
        ) {
            nextSearchPos.value = SearchPos(lastEditedLineIndexState.intValue, lastEditedColumnIndexState.intValue)
        }
//        println("lasteditcis:"+lastEditedColumnIndexState.intValue)  //test1791022120240812
//        println("startPos:"+nextSearchPos.value) //test1791022120240812
    }

    fun jumpToLineIndex(
        lineIndex:Int,
        goColumn: Boolean=false,
        columnStartIndex:Int=0,
        columnEndIndexExclusive:Int=columnStartIndex,
        highlightingStartIndex:Int = -1,
        highlightingEndExclusiveIndex:Int = -1,
        requireHideKeyboard:Boolean = false
    ){
        lastScrollEvent.value = ScrollEvent(
            index = lineIndex,
            forceGo = true,
            goColumn = goColumn,
            columnStartIndexInclusive = columnStartIndex,
            columnEndIndexExclusive = columnEndIndexExclusive,
            highlightingStartIndex = highlightingStartIndex,
            highlightingEndExclusiveIndex = highlightingEndExclusiveIndex,
            requireHideKeyboard = requireHideKeyboard
        )
    }

    suspend fun doSearch(key:String, toNext:Boolean, startPos: SearchPos) {
        val keyLen = key.length

        val posResult = textEditorState.doSearch(key.lowercase(), toNext = toNext, startPos = startPos)
        val foundPos = posResult.foundPos
        if(foundPos == SearchPos.NotFound) {
            if(!searchMode.value && mergeMode) {
                Msg.requireShow(activityContext.getString(R.string.no_conflict_found))
            }else {
                Msg.requireShow(activityContext.getString(R.string.not_found))
            }
        }else {  //查找到了关键字

//            println("found:$foundPos")//test1791022120240812

            //显示选中文本背景颜色
            needShowCursorHandle.value = true

            val keywordStartAtLine = foundPos.columnIndex
            val keywordEndExclusiveAtLine = foundPos.columnIndex + keyLen
            //跳转到对应行并选中关键字
            jumpToLineIndex(
                lineIndex = foundPos.lineIndex,
                goColumn = true,


                //注：让开头和结束索引调换是为了让输入光标在选中内容末尾而不是开头
                //注：让开头和结束索引调换是为了让输入光标在选中内容末尾而不是开头
                //注：让开头和结束索引调换是为了让输入光标在选中内容末尾而不是开头

                //有开头和结尾索引，原本是想做区域选中的，就像手动长按选中某段文本一样，但忘了是什么问题了，反正选中不了，所以最后仅定位某列，无区域选中了
                //若想定位到关键字开头，用上面的，定位到末尾用下面的
//                columnStartIndex = keywordStartAtLine, // to keyword start
                columnStartIndex = keywordEndExclusiveAtLine, // to keyword end

                //这个与定位到关键字开头还是末尾无关，不用动，因为有bug，所以就算这个和开头索引不同，也不会选中区域，实际还是会定位到开头那
//                columnEndIndexExclusive = keywordEndExclusiveAtLine,
                columnEndIndexExclusive = keywordStartAtLine,

                //这个高亮关键字也有bug，闪一下就没了，因为TextFieldValue更新后会丢样式
                //高亮关键字
                highlightingStartIndex = keywordStartAtLine,
                highlightingEndExclusiveIndex = keywordEndExclusiveAtLine,

                //请求关掉键盘，不然高亮会被textFieldValue的onValueChange事件刷新掉，不过就算关了键盘，当前正在编辑的行也无法取消聚焦，还是有可能会被刷新掉高亮颜色
//                requireHideKeyboard = true
                //无所谓，高亮颜色bug触发就触发吧，关了键盘，光标不在关键字后面，不方便修改，若不想编辑只想看关键字附近的内容，可开只读模式，那样就可避免弹出键盘，也可避免输入发触发TextField的onValueChange导致高亮闪一下就消失的bug
                requireHideKeyboard = false,  // requireLossFocus 相关
            )


            lastFoundPos.value = posResult.foundPos.copy()

            //更新最后编辑行和列
            lastEditedLineIndexState.intValue = lastFoundPos.value.lineIndex
            lastEditedColumnIndexState.intValue = lastFoundPos.value.columnIndex
            //选中关键字(有缺陷)
//            editableController.selectTextInLine(foundPos.lineIndex, foundPos.columnIndex, endIndexExclusive)

            //更新下次搜索起点
            nextSearchPos.value = posResult.nextPos.copy()
        }
    }

    //每次计算依赖的状态变化就会重新计算这个变量的值
//    val firstLineIndexState = remember { derivedStateOf {
//        val visibleItems= lazyColumnState.layoutInfo.visibleItemsInfo
//        if(visibleItems.isEmpty()) {
//            0
//        }else {
//            //按索引排序，取出索引最小的元素的索引
//            visibleItems.minBy { it.index }.index
//        }
//    } }

    //执行一次LaunchedEffected后，此值才应为真，放到LaunchedEffect末尾修改为true即可。（目前 20240419 用来判断是否需要保存可见的第一行索引，刚打开文件要么默认0，要么恢复上次滚动位置，都不需要保存，只有打开文件后（初始化后）再滚动才需要保存）
    val isInitDone = editorPageIsInitDone

//    editableController.syncState(textEditorState)

//    val clipboardManager = LocalClipboardManager.current

    //上级页面发来的request，请求执行某些操作
    if(requestFromParent.value==PageRequest.editorQuitSelectionMode) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            doJobThenOffLoading {
                textEditorState.quitSelectionMode()
            }
        }
    }
    if(requestFromParent.value==PageRequest.requestUndo) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            doJobThenOffLoading {
                textEditorState.undo(undoStack = undoStack)
            }
        }
    }
    if(requestFromParent.value==PageRequest.requestRedo) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            doJobThenOffLoading {
                textEditorState.redo(undoStack=undoStack)
            }
        }
    }
    if(requestFromParent.value==PageRequest.goToTop) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            lastScrollEvent.value = ScrollEvent(0, forceGo = true)
        }
    }
    if(requestFromParent.value==PageRequest.goToLine) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            goToLineValue.value = goToLineValue.value.let { it.copy(selection = TextRange(0, it.text.length)) }
            showGoToLineDialog.value=true
        }
    }
    if(requestFromParent.value==PageRequest.requireSearch) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            //初始化搜索位置
//            initSearchPos()
            //开启搜索模式
            searchMode.value = true
        }
    }
    if(requestFromParent.value==PageRequest.findPrevious) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            doJobThenOffLoading {
                initSearchPos()
                doSearch(searchKeyword, toNext = false, nextSearchPos.value)
            }
        }
    }
    if(requestFromParent.value==PageRequest.findNext) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            doJobThenOffLoading {
                initSearchPos()
                doSearch(searchKeyword, toNext = true, nextSearchPos.value)
            }
        }
    }
    //显示总共有多少关键字（关键字计数）
    if(requestFromParent.value==PageRequest.showFindNextAndAllCount) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            doJobThenOffLoading {
                val allCount = textEditorState.getKeywordCount(searchKeyword)
                Msg.requireShow(replaceStringResList(activityContext.getString(R.string.find_next_all_count), listOf(allCount.toString())))
            }
        }
    }


    if(requestFromParent.value==PageRequest.previousConflict) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            doJobThenOffLoading {
                initSearchPos()

                val nextSearchLine = textEditorState.fields.get(nextSearchPos.value.lineIndex).value.text
                if(nextSearchLine.startsWith(settings.editor.conflictStartStr)) {
                    conflictKeyword.value = settings.editor.conflictStartStr
                }else if(nextSearchLine.startsWith(settings.editor.conflictSplitStr)) {
                    conflictKeyword.value = settings.editor.conflictSplitStr
                }else if(nextSearchLine.startsWith(settings.editor.conflictEndStr)) {
                    conflictKeyword.value = settings.editor.conflictEndStr
                }

                val previousKeyWord = getPreviousKeyWordForConflict(conflictKeyword.value, settings)
                conflictKeyword.value = previousKeyWord
                doSearch(previousKeyWord, toNext = false, nextSearchPos.value)
            }
        }
    }
    if(requestFromParent.value==PageRequest.nextConflict) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            doJobThenOffLoading {
                initSearchPos()

                // update cur conflict keyword, if cursor on conflict str line, if dont do this, UX bad, e.g. I clicked conflict splict line, then click prev conflict, expect is go conflict start line, but if last search is start line, this time will go to end line, anti-intuition
                // 如果光标在冲突开始、分割、结束行之一，更新搜索关键字，如果不这样做，会出现一些反直觉的bug：我点击了conflict split line，然后点上，期望是查找conflict start line，但如果上次搜索状态是start line，那这次就会去搜索end line，反直觉
                val nextSearchLine = textEditorState.fields.get(nextSearchPos.value.lineIndex).value.text
                if(nextSearchLine.startsWith(settings.editor.conflictStartStr)) {
                    conflictKeyword.value = settings.editor.conflictStartStr
                }else if(nextSearchLine.startsWith(settings.editor.conflictSplitStr)) {
                    conflictKeyword.value = settings.editor.conflictSplitStr
                }else if(nextSearchLine.startsWith(settings.editor.conflictEndStr)) {
                    conflictKeyword.value = settings.editor.conflictEndStr
                }

                val nextKeyWord = getNextKeyWordForConflict(conflictKeyword.value, settings)
                conflictKeyword.value = nextKeyWord
                doSearch(nextKeyWord, toNext = true, nextSearchPos.value)
            }
        }
    }
    if(requestFromParent.value==PageRequest.showNextConflictAndAllConflictsCount) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
            doJobThenOffLoading {
//                val allCount = editableController.getKeywordCount(conflictKeyword.value)  // this keyword is dynamic change when press next or previous, used for count conflict is ok though, but use conflict start str count can be better
                val allCount = textEditorState.getKeywordCount(settings.editor.conflictStartStr)
                Msg.requireShow(replaceStringResList(activityContext.getString(R.string.next_conflict_all_count), listOf(allCount.toString())))
            }
        }
    }
    //20240507: 这个其实已经没用了，改用在第一行和最后编辑位置切换了，所以先注释了
//    if(requestFromParent.value==PageRequest.backLastEditedLine) {
//        PageRequest.clearStateThenDoAct(requestFromParent) {
//            val settings = SettingsUtil.getSettingsSnapshot()
//            val lastPos = settings.editor.filesLastEditPosition[fileFullPath]
////            if(debugModeOn) {
////                println("back to lastLineIndex: "+lastPos?.lineIndex)
////            }
//            lastScrollEvent = ScrollEvent(lastPos?.lineIndex?:0, forceGo = true)
////            lastScrollEvent = ScrollEvent(23, forceGo = true)  //测试跳转是否好使，结果：passed
//        }
//    }

    if(requestFromParent.value==PageRequest.switchBetweenFirstLineAndLastEditLine) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
//            println("firstline:"+firstLineIndexState.value)
//            println("lastEditedLineIndexState:"+lastEditedLineIndexState)

            val notAtTop = listState.firstVisibleItemIndex != 0
            // if 不在顶部，go to 顶部 else go to 上次编辑位置
            val position = if(notAtTop) 0 else lastEditedLineIndexState.intValue
            lastScrollEvent.value = ScrollEvent(position, forceGo = true)
        }
    }

    if(requestFromParent.value==PageRequest.switchBetweenTopAndLastPosition) {
        PageRequest.clearStateThenDoAct(requestFromParent) {
//            println("firstline:"+firstLineIndexState.value)
//            println("lastEditedLineIndexState:"+lastEditedLineIndexState)

            val lastVisibleLine = listState.firstVisibleItemIndex
            val notAtTop = lastVisibleLine != 0
            // if 不在顶部，go to 顶部 else go to 上次编辑位置
            val position = if(notAtTop) {
                lastVisibleLineState.value = lastVisibleLine
                0
            } else {
                lastVisibleLineState.value
            }

            lastScrollEvent.value = ScrollEvent(position, forceGo = true)
        }
    }

    // get line number(line index+1)
    val getLineVal = {i:String ->
        try {
            //删下首尾空格，增加容错率，然后尝试转成int
            val line = i.trim().toInt()
            if(line==LineNum.EOF.LINE_NUM) {  // if is EOF, return last line number, then can go to end of file
                textEditorState.fields.size
            }else{
                Math.max(1, line)
            }
        }catch (e:Exception) {
            // parse int failed, then go first line
            1
        }

    }

    /**
     * 入参是行号，不是索引，最小是1
     * param is line number, not index, min is 1
     */
    val doGoToLine = { line:String ->
        //x 会报错，提示index必须为非负数) 测试下如果是-1会怎样？是否会报错？
//        val lineIntVal = -1
        val lineIntVal = getLineVal(line)
        //行号减1即要定位行的索引
        lastScrollEvent.value = ScrollEvent(index = lineIntVal-1, forceGo=true)
    }

    if(showGoToLineDialog.value) {
        val focusRequester = remember { FocusRequester() }

        val firstLine = "1"
        val lastLine = ""+textEditorState.fields.size
        val lineNumRange = "$firstLine-$lastLine"
        ConfirmDialog(title = stringResource(R.string.go_to_line),
            requireShowTextCompose = true,
            textCompose = {
                Column(
                    modifier= Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                    ,
                ) {

                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            .focusRequester(focusRequester)
                        ,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            showGoToLineDialog.value = false
                            doGoToLine(goToLineValue.value.text)
                        }),
                        singleLine = true,

                        value = goToLineValue.value,
                        onValueChange = {
                            goToLineValue.value=it
                        },
                        label = {
                            Text(stringResource(R.string.line_number)+"($lineNumRange)")
                        },
                        placeholder = {
                            //显示行号范围，例如："Range: 1-123"
                            Text(stringResource(R.string.range) + ": $lineNumRange")
                        }
                    )

                    Column(
                        modifier= Modifier
                            .fillMaxWidth()
                            .padding(end = 10.dp)
                        ,
                        horizontalAlignment = Alignment.End
                    ) {

                        ClickableText(
                            text = stringResource(R.string.first_line),
                            modifier = MyStyleKt.ClickableText.modifier.clickable {
                                goToLineValue.value = TextFieldValue(firstLine)
                            },
                            fontWeight = FontWeight.Light
                        )

                        Spacer(Modifier.height(15.dp))

                        ClickableText(
                            text = stringResource(R.string.last_line),
                            modifier = MyStyleKt.ClickableText.modifier.clickable {
                                goToLineValue.value = TextFieldValue(lastLine)
                            },
                            fontWeight = FontWeight.Light
                        )

                        Spacer(Modifier.height(10.dp))

                    }
                }


            },
            okBtnEnabled = goToLineValue.value.text.isNotBlank(),
            okBtnText = stringResource(id = R.string.go),
            cancelBtnText = stringResource(id = R.string.cancel),
            onCancel = { showGoToLineDialog.value = false }
        ) {
            showGoToLineDialog.value = false
            doGoToLine(goToLineValue.value.text)
        }

        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    }

//    val scrollTo = { lineIndex:Int ->
//        scope.launch {
//            lazyColumnState.scrollToItem(Math.max(0, lineIndex))
//        }
//        Unit
//    }


    // actually only need know which lines will delete, no need know which will kept
//    val keepStartIndex = remember { mutableStateOf(-1) }
//    val keepEndIndex = remember { mutableStateOf(-1) }

    val delStartIndex = rememberSaveable { mutableIntStateOf(-1) }
    val delEndIndex = rememberSaveable { mutableIntStateOf(-1) }
    val delSingleIndex = rememberSaveable { mutableIntStateOf(-1) }
    val acceptOursState = rememberSaveable { mutableStateOf(false) }
    val acceptTheirsState = rememberSaveable { mutableStateOf(false) }
    val showAcceptConfirmDialog = rememberSaveable { mutableStateOf(false) }

    /**
     * find accept block indecies,
     * find direction:
     * if curLineText starts with conflict start str: conflict start -> split -> end (all go down)
     * if starts with split str: split -> start -> end (go down, then go up)
     * if starts with end str: end -> split -> start (all go up)
     */
    val prepareAcceptBlock= label@{acceptOurs: Boolean, acceptTheirs:Boolean, index: Int, curLineText: String ->
//        println("index=$index, curLine=$curLineText")

        val curStartsWithStart = curLineText.startsWith(settings.editor.conflictStartStr)
        val curStartsWithSplit = curLineText.startsWith(settings.editor.conflictSplitStr)
        val curStartsWithEnd = curLineText.startsWith(settings.editor.conflictEndStr)
        if(!(curStartsWithStart || curStartsWithSplit || curStartsWithEnd)) {
            Msg.requireShow(activityContext.getString(R.string.invalid_conflict_block))
            return@label
        }


        val firstFindDirection = if(curStartsWithStart) {
            FindDirection.DOWN
        }else {
            FindDirection.UP
        }

        val firstExpectStr = if(curStartsWithStart) {
            settings.editor.conflictSplitStr
        }else if(curStartsWithSplit) {
            settings.editor.conflictStartStr
        }else {  // conflict end str like ">x7 branchName"
            settings.editor.conflictSplitStr
        }


        val (firstIndex, _) = textEditorState.indexAndValueOf(startIndex=index, direction=firstFindDirection, predicate={it.startsWith(firstExpectStr)}, includeStartIndex = false)

        if(firstIndex == -1) {
            Msg.requireShow(activityContext.getString(R.string.invalid_conflict_block))
            return@label
        }

        val secondFindDirection = if(curStartsWithEnd) {
            FindDirection.UP
        }else {
            FindDirection.DOWN
        }

        val secondExpectStr = if(curStartsWithEnd) {
            settings.editor.conflictStartStr
        }else {
            settings.editor.conflictEndStr
        }

        val secondStartFindIndexAt =if(curStartsWithSplit) {
            index
        }else {
            firstIndex
        }
        val (secondIndex, _) = textEditorState.indexAndValueOf(startIndex=secondStartFindIndexAt, direction=secondFindDirection, predicate={it.startsWith(secondExpectStr)}, includeStartIndex = false)

        if(secondIndex==-1) {
            Msg.requireShow(activityContext.getString(R.string.invalid_conflict_block))
            return@label
        }

        val startConflictLineIndex = if(curStartsWithStart) index else if(curStartsWithSplit) firstIndex else secondIndex  //this is start conflict str index
        val splitConflictLineIndex = if(curStartsWithStart || curStartsWithEnd) firstIndex else index  // this is split conflict str index
        val endConflictLineIndex = if(curStartsWithStart || curStartsWithSplit) secondIndex else index  // this is end conflict str index

        // special case: start may larger than end index, e.g. will keep 30 to 20, but, only shown wrong, will not err when deleting
        // 特殊情况：start index可能大于end index，例如：30 到 20，不过只是显示有误，实际执行无误
        if(acceptOurs && acceptTheirs.not()) {
            acceptOursState.value = true
            acceptTheirsState.value = false

            // remove single index and range [start, end]
            delSingleIndex.value = startConflictLineIndex
            delStartIndex.value = splitConflictLineIndex
            delEndIndex.value = endConflictLineIndex

//            keepStartIndex.value = startConflictLineIndex + 1  // this is startIndex+1
//            keepEndIndex.value = splitConflictLineIndex - 1  // this is splitIndex-1
        }else if(acceptOurs.not() && acceptTheirs) {
            acceptOursState.value = false
            acceptTheirsState.value = true

            // remove single index and range [start, end]
            delSingleIndex.value = endConflictLineIndex
            delStartIndex.value = startConflictLineIndex
            delEndIndex.value = splitConflictLineIndex

//            keepStartIndex.value = splitConflictLineIndex + 1  //this is split index +1
//            keepEndIndex.value = endConflictLineIndex - 1  // this is end index -1
        }else if(acceptOurs && acceptTheirs) {  // accept both
            acceptOursState.value = true
            acceptTheirsState.value = true

            // remove 3 indices, no range
            delSingleIndex.value = startConflictLineIndex
            delStartIndex.value= splitConflictLineIndex
            delEndIndex.value = endConflictLineIndex
        }else { // reject both
            acceptOursState.value = false
            acceptTheirsState.value = false

            // remove range [start, end]
            delStartIndex.value = startConflictLineIndex
            delEndIndex.value = endConflictLineIndex
        }

        // show dialog, make sure user confirm
        showAcceptConfirmDialog.value = true
    }

    if(showAcceptConfirmDialog.value) {
        ConfirmDialog2(
            title = if(acceptOursState.value && acceptTheirsState.value.not()) stringResource(R.string.accept_ours)
            else if(acceptOursState.value.not() && acceptTheirsState.value) stringResource(R.string.accept_theirs)
            else if(acceptOursState.value && acceptTheirsState.value) stringResource(R.string.accept_both)
            else stringResource(R.string.reject_both),

            text = if(acceptOursState.value && acceptTheirsState.value.not()) replaceStringResList(stringResource(R.string.will_accept_ours_and_delete_lines_line_indexs), listOf(""+(delSingleIndex.value + 1), ""+(delStartIndex.value + 1), ""+(delEndIndex.value + 1)))
            else if(acceptOursState.value.not() && acceptTheirsState.value) replaceStringResList(stringResource(R.string.will_accept_theirs_and_delete_lines_line_indexs), listOf(""+(delSingleIndex.value + 1), ""+(delStartIndex.value + 1), ""+(delEndIndex.value + 1)))
            else if(acceptOursState.value && acceptTheirsState.value) replaceStringResList(stringResource(R.string.will_accept_both_and_delete_lines_line_indexs), listOf(""+(delSingleIndex.value + 1), ""+(delStartIndex.value + 1), ""+(delEndIndex.value + 1)))
            else replaceStringResList(stringResource(R.string.will_reject_both_and_delete_lines_line_indexs), listOf(""+(delStartIndex.value + 1), ""+(delEndIndex.value + 1))),

            onCancel = {showAcceptConfirmDialog.value = false}
        ) {
            showAcceptConfirmDialog.value=false

            doJobThenOffLoading{
                val indicesWillDel = if((acceptOursState.value && acceptTheirsState.value.not()) || (acceptOursState.value.not() && acceptTheirsState.value)){
                    //accept ours/theirs
                    val tmp = mutableListOf(delSingleIndex.value)
                    tmp.addAll(IntRange(start = delStartIndex.value, endInclusive = delEndIndex.value).toList())
                    tmp
                }else if(acceptOursState.value && acceptTheirsState.value) {  // accept both
                    val tmp = mutableListOf(delSingleIndex.value)
                    tmp.add(delStartIndex.value)
                    tmp.add(delEndIndex.value)
                    tmp
                }else {  // reject both
                    IntRange(start = delStartIndex.value, endInclusive = delEndIndex.value).toList()
                }

                textEditorState.deleteLineByIndices(indicesWillDel)
            }
        }
    }


    if(requireEditorScrollToPreviewCurPos.value) {
        //一次性变量，立刻重置
        requireEditorScrollToPreviewCurPos.value = false

        try {
            val fontSizeInPx = UIHelper.spToPx(sp = fontSize.intValue, density = density)
            val screenWidthInPx = UIHelper.dpToPx(dp = deviceConfiguration.screenWidthDp, density = density)
            val screenHeightInPx = UIHelper.dpToPx(dp = deviceConfiguration.screenHeightDp, density = density)
            val previewCurAt = curPreviewScrollState.value

            //计算目标滚动位置
            val targetLineIndex = textEditorState.pxToLineIdx(targetPx=previewCurAt, fontSizeInPx=fontSizeInPx, screenWidthInPx=screenWidthInPx, screenHeightInPx=screenHeightInPx)

            //滚动
            doGoToLine((targetLineIndex + 1).toString()) //index + 1变成行号
        }catch (e:Exception) {
            //并非很严重的错误，debug级别吧
            MyLog.d(TAG, "let editor scroll to current preview position failed: ${e.stackTraceToString()}")
        }
    }

    LaunchedEffect(lastScrollEvent.value) TextEditorLaunchedEffect@{
        try {
            val lastScrollEvent = lastScrollEvent.value
//        if(debugModeOn) {
            //还行不是很长
//            println("lastScrollEvent.toString() + firstLineIndexState.value:"+(lastScrollEvent.toString() + firstLineIndexState.value))
//        }

//        if(debugModeOn) {
//            println("滚动事件更新了："+lastScrollEvent)
////            println("最后编辑行："+lastEditedPos)
//            println("第一个可见行："+firstLineIndexState.value)
//        }
            //如果值不是-1将会保存
//        var maybeWillSaveEditedLineIndex = -1  //最后编辑行
//        var maybeWillSaveFirstVisibleLineIndex = -1  //首个可见行

            //初始化（组件创建，第一次执行LaunchedEffect）之后，更新最新可见行
//        if(isInitDone.value) {
//            lastFirstVisibleLineIndexState = Math.max(0, firstLineIndexState.value)
//        }

            //刚打开文件，定位到上次记录的行，这个滚动只在初始化时执行一次
            if(lastScrollEvent==null && !isInitDone.value) {
                //放到第一行是为了避免重入
                isInitDone.value=true

                //goToLine触发场景：预览diff，发现某行需要改，点击行号，就会直接定位到对应行了
                //会用goToLine的值减1得到索引，所以0也不行
                val useLastEditPos = LineNum.shouldRestoreLastPosition(goToLine)

                //滚动一定要放到scope里执行，不然这个东西一滚动，整个LaunchedEffect代码块后面就不执行了
                //如果goToLine大于0，把行号减1换成索引；否则跳转到上次退出前的第一可见行
                UIHelper.scrollToItem(
                    coroutineScope = scope,
                    listState = listState,
                    index = if(useLastEditPos) {
                        lastEditedPos.firstVisibleLineIndex
                    } else if(goToLine == LineNum.EOF.LINE_NUM) {
                        textEditorState.fields.size - 1
                    } else {
                        goToLine - 1
                    }
                )

                //如果定位到上次退出位置，进一步检查是否需要定位到最后编辑列
                //因为定位column会弹出键盘所以暂时不定位了，我不想一打开编辑器自动弹出键盘，因为键盘会自动读取上下文，可能意外获取屏幕上的文本泄漏隐私
                if(bug_Editor_GoToColumnCantHideKeyboard_Fixed && useLastEditPos) {
                    //是否需要定位到上次编辑的列，若否，只定位到最后退出前的首个可见行
                    val restoreLastEditColumn = SettingsUtil.getSettingsSnapshot().editor.restoreLastEditColumn
    //                val restoreLastEditColumn = true  //test2024081116726433

                    //如果是readOnly模式，就没必要定位到对应列了，就算定位了也无效，多此一举
                    if(!readOnlyMode && useLastEditPos && restoreLastEditColumn) {
                        //定位到指定列。注意：会弹出键盘！没找到好的不弹键盘的方案，所以我把定位列功能默认禁用了
                        textEditorState.selectField(
                            lastEditedPos.lineIndex,
                            option = SelectionOption.CUSTOM,
                            columnStartIndexInclusive = lastEditedPos.columnIndex
                        )
                    }

                }

                return@TextEditorLaunchedEffect


                //只有当滚动事件不为null且isConsumed为假时，才执行下面的代码块
            }else if(lastScrollEvent?.isConsumed == false) {  //编辑了文件，行号有更新，更新配置文件记录的行并定位到对应的行
                //消费以避免重复执行（设置isConsumed为true）
                lastScrollEvent?.consume()

                //检查是否不检查行是否可见，直接强制跳转
                val forceGo = lastScrollEvent?.forceGo == true
                lastScrollEvent?.index?.let { index ->
//                val safeIndex = Math.max(0, index)
//                maybeWillSaveEditedLineIndex = safeIndex

                    //先跳转，然后更新配置文件
                    //跳转
                    //强制跳转，无论是否可见
                    if(forceGo) {
                        //强制跳转不应该更新最后编辑行，因为跳转后并不会自动focus跳转到的那行，最后编辑行其实还是之前的那个，所以，只更新可见行即可
//                    maybeWillSaveEditedLineIndex = -1

                        //强制跳转的话，第一个可见行就是跳转的那行
//                    lastFirstVisibleLineIndexState = index
                        //定位行
                        UIHelper.scrollToItem(scope, listState, index)

//                        println("lastScrollEvent!!.columnStartIndexInclusive:${lastScrollEvent!!.columnStartIndexInclusive}")  //test1791022120240812
                        //定位列，如果请求定位列的话
                        if(lastScrollEvent?.goColumn==true) {
                            //选中关键字
                            textEditorState.selectField(
                                targetIndex = index,
                                option = SelectionOption.CUSTOM,
                                columnStartIndexInclusive = lastScrollEvent!!.columnStartIndexInclusive,
                                //如果选中某行子字符串的功能修复了，就使用正常的endIndex；否则使用startIndex，定位光标到关键字出现的位置但不选中关键字
                                columnEndIndexExclusive = if(bug_Editor_SelectColumnRangeOfLine_Fixed) lastScrollEvent!!.columnEndIndexExclusive else lastScrollEvent!!.columnStartIndexInclusive,
                                requireSelectLine = false,
                                highlightingStartIndex = lastScrollEvent.highlightingStartIndex,
                                highlightingEndExclusiveIndex = lastScrollEvent.highlightingEndExclusiveIndex,
                            )

                            //请求失焦则关闭键盘
                            if(lastScrollEvent.requireHideKeyboard) {
                                keyboardController?.hide()
                            }
                        }
                    }else {
                        //更新最后编辑行状态
                        lastEditedLineIndexState.intValue = index
//                        println("index="+index)
                        //检查一下，如果对应索引不可见则跳转
                        // list.minBy(item.index) 用元素的index排序，取出index最小的元素，后面跟.index，即取出最小index元素的index,maxBy和minBy异曲同工
//                    val first = lazyColumnState.layoutInfo.visibleItemsInfo.minBy { it.index }.index
//                    lastFirstVisibleLineIndexState = first

                        val first = listState.firstVisibleItemIndex
//                    if(debugModeOn) { //期望 true，结果 true，测试通过
//                        println("firstLineIndexState.value == first:"+(firstLineIndexState.value == first))
//                    }
                        val end = listState.layoutInfo.visibleItemsInfo.maxBy { it.index }.index
                        //如果指定行不在可见范围内，滚动到指定行以使其可见
                        if (index < first || index > end) {
                            //滚动到指定行
                            UIHelper.scrollToItem(scope, listState, index)
                        }
                    }
                }
            }

        }catch (e:Exception) {
            MyLog.e(TAG, "#TextEditorLaunchedEffect@ , err: "+e.stackTraceToString())
        }

//        if(debugModeOn) {
//            println("maybeSaveEditedLineIndex=$maybeWillSaveEditedLineIndex")
//            println("maybeSaveFirstVisibleLineIndex=$maybeWillSaveFirstVisibleLineIndex")
//            println("lastEditedLineIndexState=$lastEditedLineIndexState")
//            println("lastFirstVisibleLineIndexState=$lastFirstVisibleLineIndexState")
//        }

    }

    DisposableEffect(Unit) {
        onDispose TextEditorOnDispose@{
            try {

                //更新配置文件记录的滚动位置（当前屏幕可见第一行）和最后编辑行
                //如果当前索引不是上次定位的索引，更新页面状态变量和配置文件中记录的最后编辑行索引，不管是否需要滚动，反正先记上
                //先比较一下，Settings对象从内存取不用读文件，很快，如果没变化，就不用更新配置文件了，省了磁盘IO，所以有必要检查下
                val oldLinePos = FileOpenHistoryMan.get(fileFullPath.ioPath)
                val needUpdateLastEditedLineIndex = oldLinePos?.lineIndex != lastEditedLineIndexState.intValue
                val currentFirstVisibleIndex = listState.firstVisibleItemIndex
                val needUpdateFirstVisibleLineIndex = oldLinePos?.firstVisibleLineIndex != currentFirstVisibleIndex

                //记住最后编辑列
                val editedColumnIndex = lastEditedColumnIndexState.intValue
//                println("lasteditcolumnindex:"+editedColumnIndex) //test2024081116726433
                val needUpdateLastEditedColumnIndex = oldLinePos?.columnIndex != editedColumnIndex

                if((needUpdateLastEditedLineIndex || needUpdateFirstVisibleLineIndex || needUpdateLastEditedColumnIndex)) {
//                    println("will save possssssssssssssssssssss")
//                    SettingsUtil.update {
                    val pos = oldLinePos
//                        if(pos==null) {
//                            pos = FileEditedPos()
//                        }
                    if(needUpdateLastEditedLineIndex) {
                        pos.lineIndex = lastEditedLineIndexState.intValue
                    }
                    if(needUpdateFirstVisibleLineIndex) {
                        pos.firstVisibleLineIndex = currentFirstVisibleIndex
                    }
                    if(needUpdateLastEditedColumnIndex) {
                        pos.columnIndex = editedColumnIndex
                    }
//                if(debugModeOn) {
//                    println("editorPos will save: "+pos)
//                }
//                        println(pos) //test2024081116726433
                    FileOpenHistoryMan.set(fileFullPath.ioPath, pos)
//                    }
                }
            }catch (e:Exception) {
                MyLog.e(TAG, "#TextEditorOnDispose@ , err: "+e.stackTraceToString())
            }
        }
    }

    CompositionLocalProvider(
        LocalTextInputService provides (if(allowKeyboard.value && !readOnlyMode) LocalTextInputService.current else null),  //为null可阻止弹出键盘
        LocalTextSelectionColors provides (if(!needShowCursorHandle.value) customTextSelectionColors_hideCursorHandle else if(inDarkTheme) customTextSelectionColors_darkMode else customTextSelectionColors),
    ) {

        expectConflictStrDto.value.reset()

        LazyColumn(
            state = listState,
            //fillMaxSize是为了点哪都能触发滚动，这样点哪都能隐藏顶栏
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPaddingValues
        ) {
            val size = textEditorState.fields.size

            //fields本身就是toList()出来的，无需再toList()
            textEditorState.fields.forEachIndexedBetter { index, textFieldState ->
                val curLineText = textFieldState.value.text

                // patch开头的行（+ -）和merge开头的行（7个=号）并不冲突
                val patchColor = if(patchMode) PatchUtil.getColorOfLine(curLineText, inDarkTheme) else null;
                val bgColor = if(patchMode && patchColor != null) {
                    patchColor
                } else if(mergeMode) {
                    UIHelper.getBackgroundColorForMergeConflictSplitText(
                        text = curLineText,
                        settings = settings,
                        expectConflictStrDto = expectConflictStrDto.value,
                        oursBgColor = conflictOursBlockBgColor,
                        theirsBgColor = conflictTheirsBlockBgColor,
                        startLineBgColor= conflictStartLineBgColor,
                        splitLineBgColor= conflictSplitLineBgColor,
                        endLineBgColor= conflictEndLineBgColor
                    )
                } else {
                    Color.Unspecified
                }


                if(mergeMode && curLineText.startsWith(settings.editor.conflictStartStr)) {
                    item {
                        AcceptButtons(
                            lineIndex = index,
                            lineText = curLineText,
                            acceptOursColor = acceptOursBtnColor,
                            acceptTheirsColor = acceptTheirsBtnColor,
                            acceptBothColor = acceptBothBtnColor,
                            rejectBothColor = rejectBothBtnColor,
                            prepareAcceptBlock = prepareAcceptBlock,
                        )
                    }
                }

                //很多时候改了内容，但没改id，懒得一个个改了，直接弃用id作key，让compose自己判断什么时候需要重组，有问题再说
//                    item(key = textFieldState.id) {
                item {
                    decorationBox(
                        index,
                        size,
                        textFieldState.isSelected,
                        textFieldState,

                        //用来判断是否选中当前行，若选中则加背景颜色，如果为null就当作-1，-1为无效索引，这样就不会选中任何行
                        textEditorState.focusingLineIdx ?: -1,
                        textEditorState.isMultipleSelectionMode,

                    ) { modifier ->
                        // FileEditor里的innerTextFiled()会执行这的代码
                        Box(
                            modifier = Modifier
                                .background(bgColor)
                                .combinedClickable(
                                    //不显示点击效果（闪烁动画）
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,

                                    onLongClick = clickable@{
                                        if (!textEditorState.isMultipleSelectionMode) return@clickable

                                        //(20250325新版compose似乎不会同时触发选择文本和当前行了，所以不会震两下了) 震动反馈，和长按选择文本的震动反馈冲突了，若开会振两下
//                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                        //执行区域选择
                                        doJobThenOffLoading {
                                            textEditorState.selectFieldSpan(index)
                                        }
                                    }
                                ) clickable@{
                                    if (!textEditorState.isMultipleSelectionMode) return@clickable

                                    doJobThenOffLoading {
                                        textEditorState.selectField(targetIndex = index)

                                    }
                                }.then(modifier)
                        ) {
                            MyTextField(
                                ignoreFocusOnce = ignoreFocusOnce,
                                //搜索模式已经没必要聚焦了，因为不需要光标定位行了，直接高亮关键字了，而且搜索模式会把focusingLineIdx设为null以避免聚焦行弹出键盘误判内容已改变从而触发重组导致高亮关键字功能失效
//                                    focusThisLine = if(textEditorState.isContentEdited.value) index == textEditorState.focusingLineIdx else false,
                                //仅当搜索模式，或者内容发生变化（比如换行）时光标才会自动聚焦，否则不聚焦，这样是为了避免切换页面再回来自动弹出键盘
//                                    focusThisLine = if(searchMode.value || textEditorState.isContentEdited.value) index == textEditorState.focusingLineIdx else false,
                                focusThisLine = index == textEditorState.focusingLineIdx,
                                //默认不自动聚焦任何行，不然一切换页面再回来弹出键盘，恶心
//                                    focusThisLine = false,

                                mergeMode=mergeMode,
                                searchMode = searchMode.value,
                                lastEditedColumnIndexState=lastEditedColumnIndexState,
                                needShowCursorHandle = needShowCursorHandle,
                                textFieldState = textFieldState,
                                enabled = !textEditorState.isMultipleSelectionMode && !readOnlyMode,
                                fontSize = fontSize.intValue,
                                fontColor = fontColor,
//                                    bgColor = bgColor,
//                                    bgColor = Color.Unspecified,
                                onUpdateText = { newText ->

                                    doJobThenOffLoading {
                                        try{

                                            textEditorState.updateField(
                                                targetIndex = index,
                                                textFieldValue = newText
                                            )

                                        }catch (e:IndexOutOfBoundsException) {
                                            // Undo/Redo后可能 出现 索引错误，没必要显示给用户，只记下日志就行
                                            MyLog.e(TAG, "#onUpdateText err: "+e.localizedMessage)
                                        }catch (e:Exception) {
                                            // 其他错误，显示给用户
                                            Msg.requireShowLongDuration("#onUpdateText err: "+e.localizedMessage)

                                            //log
                                            MyLog.e(TAG, "#onUpdateText err: "+e.stackTraceToString())
                                        }
                                    }

                                    //改用onFocus定位最后编辑行了，这里不需要了，实际上现在的最后编辑行就是光标最后所在行
//                                            lastScrollEvent = ScrollEvent(index)
                                },
                                onContainNewLine = cb@{ newText ->
                                    //这里为什么要判断这个东西？无所谓，反正没毛病，不用改
                                    if (lastScrollEvent.value?.isConsumed == false) return@cb

                                    doJobThenOffLoading {
                                        try {
                                            textEditorState.splitNewLine(
                                                targetIndex = index,
                                                textFieldValue = newText
                                            )

                                            lastScrollEvent.value = ScrollEvent(index + 1)
                                        }catch (e:Exception) {
                                            Msg.requireShowLongDuration("#onContainNewLine err: "+e.localizedMessage)

                                            MyLog.e(TAG, "#onContainNewLine err: "+e.stackTraceToString())
                                        }
                                    }

                                },
                                onAddNewLine = cb@{ newText ->

                                    if (lastScrollEvent.value?.isConsumed == false) return@cb

                                    doJobThenOffLoading {
                                        try {
                                            textEditorState.splitNewLine(
                                                targetIndex = index,
                                                textFieldValue = newText
                                            )

                                            lastScrollEvent.value = ScrollEvent(index + 1)
                                        }catch (e:Exception) {
                                            Msg.requireShowLongDuration("#onAddNewLine err: "+e.localizedMessage)
                                            MyLog.e(TAG, "#onAddNewLine err: "+e.stackTraceToString())
                                        }
                                    }

                                },
                                onDeleteNewLine = cb@{
                                    //这是要确保上一个滚动事件已被消费？有必要吗？
                                    if (lastScrollEvent.value?.isConsumed == false) return@cb

                                    doJobThenOffLoading {
                                        try {
                                            textEditorState.deleteNewLine(targetIndex = index)
                                            if (index != 0) lastScrollEvent.value = ScrollEvent(index - 1)
                                        }catch (e:Exception) {
                                            Msg.requireShowLongDuration("#onDeleteNewLine err: "+e.localizedMessage)
                                            MyLog.e(TAG, "#onDeleteNewLine err: "+e.stackTraceToString())
                                        }
                                    }

                                },
                                onFocus = {
                                    doJobThenOffLoading {
                                        try {
                                            textEditorState.selectField(index)

                                            //更新最后聚焦行(最后编辑行)
                                            lastScrollEvent.value = ScrollEvent(index)
                                        }catch (e:Exception) {
                                            Msg.requireShowLongDuration("#onFocus err: "+e.localizedMessage)
                                            MyLog.e(TAG, "#onFocus err: "+e.stackTraceToString())
                                        }
                                    }

                                },
                                onUpFocus = cb@{
                                    if (lastScrollEvent.value?.isConsumed == false) return@cb

                                    doJobThenOffLoading {
                                        try {
                                            textEditorState.selectPreviousField()
                                            if (index != 0) lastScrollEvent.value = ScrollEvent(index - 1)
                                        }catch (e:Exception) {
                                            Msg.requireShowLongDuration("#onUpFocus err: "+e.localizedMessage)
                                            MyLog.e(TAG, "#onUpFocus err: "+e.stackTraceToString())
                                        }
                                    }

                                },
                                onDownFocus = cb@{
                                    if (lastScrollEvent.value?.isConsumed == false) return@cb
                                    doJobThenOffLoading {
                                        try {
                                            textEditorState.selectNextField()
                                            if (index != textEditorState.fields.lastIndex) lastScrollEvent.value = ScrollEvent(index + 1)
                                        }catch (e:Exception) {
                                            Msg.requireShowLongDuration("#onDownFocus err: "+e.localizedMessage)
                                            MyLog.e(TAG, "#onDownFocus err: "+e.stackTraceToString())
                                        }
                                    }

                                },
                            )
                        }
                    }
                }

                if(mergeMode && curLineText.startsWith(settings.editor.conflictEndStr)) {
                    item {
                        AcceptButtons(
                            lineIndex = index,
                            lineText = curLineText,
                            acceptOursColor = acceptOursBtnColor,
                            acceptTheirsColor = acceptTheirsBtnColor,
                            acceptBothColor = acceptBothBtnColor,
                            rejectBothColor = rejectBothBtnColor,
                            prepareAcceptBlock = prepareAcceptBlock,
                        )
                    }
                }

            }

            item {
                Spacer(modifier = Modifier
                    .width(virtualWidth)
                    //设高度为屏幕高度-50dp，基本上能滚动到顶部，但会留出最后一行多一些的空间
                    .height(virtualHeight)
                    .clickable(
                        //隐藏点击效果（就是一点屏幕明暗变化一下那个效果）
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        //非选择模式，点空白区域，聚焦最后一行
                        if(textEditorState.isMultipleSelectionMode.not()) {
                            doJobThenOffLoading {
                                //点击空白区域定位到最后一行最后一个字符后面
                                //第1个参数是行索引；第2个参数是当前行的哪个位置
                                textEditorState.selectField(
                                    textEditorState.fields.lastIndex,
                                    SelectionOption.LAST_POSITION
                                )


                                //确保弹出键盘，不加的话“点击空白区域，关闭键盘，再点击空白区域”就不弹出键盘了
                                //显示键盘，不在主线程运行也可以
                                keyboardController?.show()

                                //显示键盘（在主线程运行，可选，其实不在主线程也行）
//                                    withMainContext {
//                                        keyboardController?.show()
//                                    }
                            }

                        }
                    }
//                    .background(Color.Red)  //debug
                    ,
                )
//                PaddingRow()
            }
        }

    }
}




fun getNextKeyWordForConflict(curKeyWord:String, settings: AppSettings):String {
    if(curKeyWord == settings.editor.conflictStartStr) {
        return settings.editor.conflictSplitStr
    }else if(curKeyWord == settings.editor.conflictSplitStr) {
        return settings.editor.conflictEndStr
    }else { // curKeyWord == settings.editor.conflictEndStr
        return settings.editor.conflictStartStr
    }
}
fun getPreviousKeyWordForConflict(curKeyWord:String, settings: AppSettings):String {
    if(curKeyWord == settings.editor.conflictStartStr) {
        return settings.editor.conflictEndStr
    }else if(curKeyWord == settings.editor.conflictEndStr) {
        return settings.editor.conflictSplitStr
    }else { // curKeyWord == settings.editor.conflictSplitStr
        return settings.editor.conflictStartStr
    }
}

data class ScrollEvent(
    val index: Int = -1,
    //这为什么还要time？
    val time: Long = System.currentTimeMillis(),
    val forceGo:Boolean = false,
    val goColumn:Boolean=false,
    val columnStartIndexInclusive:Int=0,
    val columnEndIndexExclusive:Int=columnStartIndexInclusive,

    val highlightingStartIndex:Int = -1,
    val highlightingEndExclusiveIndex:Int = -1,
    val requireHideKeyboard:Boolean = false

) {
    var isConsumed: Boolean = false
        private set

    fun consume() {
        isConsumed = true
    }
}

@Parcelize
data class SearchPos(var lineIndex:Int=-1, var columnIndex:Int=-1):Parcelable {
    companion object{
        val NotFound = SearchPos(-1, -1)
    }
}

data class SearchPosResult(val foundPos: SearchPos = SearchPos.NotFound, val nextPos: SearchPos = SearchPos.NotFound) {
    companion object{
        val NotFound = SearchPosResult(foundPos = SearchPos.NotFound, nextPos = SearchPos.NotFound)
    }
}
