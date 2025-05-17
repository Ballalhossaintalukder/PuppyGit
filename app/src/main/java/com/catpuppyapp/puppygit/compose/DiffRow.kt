package com.catpuppyapp.puppygit.compose

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.catpuppyapp.puppygit.constants.Cons
import com.catpuppyapp.puppygit.constants.LineNum
import com.catpuppyapp.puppygit.dev.DevFeature
import com.catpuppyapp.puppygit.git.CompareLinePair
import com.catpuppyapp.puppygit.git.CompareLinePairHelper
import com.catpuppyapp.puppygit.git.CompareLinePairResult
import com.catpuppyapp.puppygit.git.PuppyLine
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.screen.functions.getClipboardText
import com.catpuppyapp.puppygit.screen.functions.openFileWithInnerSubPageEditor
import com.catpuppyapp.puppygit.screen.shared.FilePath
import com.catpuppyapp.puppygit.settings.AppSettings
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.ui.theme.Theme
import com.catpuppyapp.puppygit.utils.FsUtils
import com.catpuppyapp.puppygit.utils.Libgit2Helper
import com.catpuppyapp.puppygit.utils.Msg
import com.catpuppyapp.puppygit.utils.addTopPaddingIfIsFirstLine
import com.catpuppyapp.puppygit.utils.cache.Cache
import com.catpuppyapp.puppygit.utils.compare.result.IndexStringPart
import com.catpuppyapp.puppygit.utils.createAndInsertError
import com.catpuppyapp.puppygit.utils.doJobThenOffLoading
import com.catpuppyapp.puppygit.utils.paddingLineNumber
import com.catpuppyapp.puppygit.utils.replaceStringResList
import com.catpuppyapp.puppygit.utils.state.mutableCustomStateOf
import com.github.git24j.core.Diff


private const val TAG = "DiffRow"

/**
 * 注意：这个组件会在LazyColumn的item里使用，所以不能用rememberSaveable，
 * 否则有概率崩溃，报 "java.lang.ClassCastException" ，是个bug，参见：https://issuetracker.google.com/issues/181880855
 *
 *
 * @param stringPartList 如果用不到，可传null或使用默认值（null）
 */
@Composable
fun DiffRow (
    stateKeyTag:String,
    index:Int,
    line:PuppyLine,
    lineNumExpectLength:Int,
    stringPartList:List<IndexStringPart>? = null,
    fileFullPath:String,
    enableLineEditActions:Boolean,
    clipboardManager: ClipboardManager,
    loadingOn:(String)->Unit,
    loadingOff:()->Unit,
    refreshPage:()->Unit,
    repoId:String,
    showLineNum:Boolean,
    showOriginType:Boolean,
    fontSize:Int,
    lineNumSize:Int,
    getComparePairBuffer:() -> CompareLinePair,
    setComparePairBuffer: (CompareLinePair) -> Unit,
//    comparePair:CustomStateSaveable<CompareLinePair>,
    betterCompare:Boolean,
    reForEachDiffContent:()->Unit,
    indexStringPartListMap:MutableMap<String, CompareLinePairResult>,
    enableSelectCompare: Boolean,
    matchByWords:Boolean,
    settings:AppSettings,
    navController:NavController,
    activityContext:Context

) {
    val stateKeyTag = Cache.getComponentKey(stateKeyTag, TAG)

    // disable for EOF, the EOF showing sometimes false-added
    // 禁用EOF点击菜单，EOF有时候假添加，就是明明没有eof，但显示新增了eof，可能是libgit2 bug
    val isNotEof = line.lineNum != LineNum.EOF.LINE_NUM
    // line edit 选项，对eof禁用（不过 拷贝 还是启用的）
    val enableLineEditActions = enableLineEditActions && isNotEof
    val enableSelectCompare = enableSelectCompare && isNotEof
    val enableLineCopy = true
    val lineClickable = enableLineCopy || enableLineEditActions || enableSelectCompare


    //不能用默认的rememberSaveable，但能用重写了saver的
    val showEditLineDialog = mutableCustomStateOf(keyTag = stateKeyTag, keyName = "showEditLineDialog") { false }
    val showRestoreLineDialog = mutableCustomStateOf(keyTag = stateKeyTag, keyName = "showRestoreLineDialog") { false }

//    println("diffrow: $stringPartList")

    val view = LocalView.current
    val density = LocalDensity.current

    val isKeyboardVisible = remember { mutableStateOf(false) }
    //indicate keyboard covered component
    val isKeyboardCoveredComponent = remember { mutableStateOf(false) }
    // which component expect adjust heghit or padding when softkeyboard shown
    val componentHeight = remember { mutableIntStateOf(0) }
    // the padding value when softkeyboard shown
    val keyboardPaddingDp = remember { mutableIntStateOf(0) }

    // this code gen by chat-gpt, wow
    // except: this code may not work when use use a float keyboard or softkeyboard with single-hand mode
    // 监听键盘的弹出和隐藏 (listening keyboard visible/hidden)
    SoftkeyboardVisibleListener(
        view = view,
        isKeyboardVisible = isKeyboardVisible,
        isKeyboardCoveredComponent = isKeyboardCoveredComponent,
        componentHeight = componentHeight,
        keyboardPaddingDp = keyboardPaddingDp,
        density = density,
        skipCondition = {
            !(showEditLineDialog.value || showRestoreLineDialog.value)
        }
    )



//    val useStringPartList = !stringPartList.isNullOrEmpty()
    // 如果为null或空 或者 所有元素都是modified，则不使用string part，否则使用
    val useStringPartList = !(stringPartList.isNullOrEmpty() || (stringPartList.indexOfFirst { it.modified.not() } == -1))



    val inDarkTheme = Theme.inDarkTheme
    //libgit2会把连续行整合到一起，这里用getLines()获取拆分后的行
//                    puppyLineBase.getLines().forEach { line ->
    val bgColor = Libgit2Helper.getDiffLineBgColor(line, inDarkTheme)
    val textColor = Libgit2Helper.getDiffLineTextColor(line, inDarkTheme)
//                        val lineTypeStr = getDiffLineTypeStr(line)
    val lineNumColor = MyStyleKt.TextColor.lineNumColorForDiff(inDarkTheme)

    val lineNum = paddingLineNumber(if(line.lineNum == LineNum.EOF.LINE_NUM) LineNum.EOF.TEXT else line.lineNum.toString(), lineNumExpectLength)

//    var prefix = ""
    val content = line.content
    //我发现明明新旧都没末尾行，但是originType却是添加了末尾行 '>'， 很奇怪，所以把行相关的背景颜色改了，文字颜色一律灰色，另外，因为patch输出会包含 no new line at end 之类的东西，所以不需要我再特意添加那句话了
    //只显示新增换行符、删除换行符、新旧文件都没换行符、新增行、删除行、上下文
//                    if (line.originType == Diff.Line.OriginType.DEL_EOFNL.toString() || line.originType == Diff.Line.OriginType.CONTEXT_EOFNL.toString()) { //新文件删除了换行符 和 新旧都没换行符
////                            prefix=line.originType+ ":"
////                            content = stringResource(R.string.no_new_line_at_end)  //不赋值的话，content什么都没有，用户看到的就是个带箭头(代表没新行的originType)的红色空行
//                    } else if (line.originType == Diff.Line.OriginType.ADD_EOFNL.toString()) {  //新增换行符
////                            prefix=line.originType+ ":"
//
//                    } else

//    prefix = line.originType + lineNum + ":"  // show add or del and line num, e.g. "+123:" or "-123:"
//    var prefix = if(showOriginType) line.originType else ""
//    prefix += (if(showLineNum) "$lineNum:" else ":")  // only show line num (can use color figure add or del), e.g. "123:"

    val prefix = if(showOriginType && showLineNum.not()) {
        "${line.originType}:"
    } else if(showOriginType.not() && showLineNum) {
        "$lineNum:"
    } else if(showOriginType && showLineNum) {
        "${line.originType}$lineNum:"
    } else {
        ""
    }


    val lineContentOfEditLineDialog = mutableCustomStateOf(keyTag = stateKeyTag, keyName = "lineContentOfEditLineDialog") { "" }
    val lineNumOfEditLineDialog = mutableCustomStateOf(keyTag = stateKeyTag, keyName = "lineNumOfEditLineDialog") { LineNum.invalidButNotEof }  // this is line number not index, should start from 1
    val lineNumStrOfEditLineDialog = mutableCustomStateOf(keyTag = stateKeyTag, keyName = "lineNumStrOfEditLineDialog") { "" }  // this is line number not index, should start from 1

    //因为自定义存储器会把数据存到mutableState里，所以Cache里存的实际不是null，因此可以存null值
    val truePrependFalseAppendNullReplace = mutableCustomStateOf<Boolean?>(keyTag = stateKeyTag, keyName = "truePrependFalseAppendNullReplace") { null }
    val showDelLineDialog = mutableCustomStateOf(keyTag = stateKeyTag, keyName = "showDelLineDialog") { false }
    val trueRestoreFalseReplace = mutableCustomStateOf(keyTag = stateKeyTag, keyName = "trueRestoreFalseReplace") { false }

    val initEditLineDialog = {content:String, lineNum:Int, prependOrApendOrReplace:Boolean? ->
        if(lineNum == LineNum.invalidButNotEof){
            Msg.requireShowLongDuration(activityContext.getString(R.string.invalid_line_number))
        }else {
            truePrependFalseAppendNullReplace.value = prependOrApendOrReplace
            lineContentOfEditLineDialog.value = content
            lineNumOfEditLineDialog.value = lineNum
            showEditLineDialog.value = true
        }
    }
    val initDelLineDialog = {lineNum:Int ->
        if(lineNum == LineNum.invalidButNotEof){
            Msg.requireShowLongDuration(activityContext.getString(R.string.invalid_line_number))
        }else {
            lineNumOfEditLineDialog.value = lineNum
            showDelLineDialog.value = true
        }
    }
    val initRestoreLineDialog = {content:String, lineNum:Int, trueRestoreFalseReplace_param:Boolean ->
        if(lineNum == LineNum.invalidButNotEof){
            Msg.requireShowLongDuration(activityContext.getString(R.string.invalid_line_number))
        }else {
            lineContentOfEditLineDialog.value = content
            lineNumOfEditLineDialog.value = lineNum
            lineNumStrOfEditLineDialog.value = ""+lineNum
            trueRestoreFalseReplace.value = trueRestoreFalseReplace_param
            showRestoreLineDialog.value = true
        }
    }


    if(showEditLineDialog.value) {
        ConfirmDialogAndDisableSelection(
            //禁用点击弹窗外部区域或按返回键关闭弹窗，这样可避免误操作而丢失正在编辑的数据
            onDismiss = {},

            title = if(truePrependFalseAppendNullReplace.value == true) stringResource(R.string.insert) else if(truePrependFalseAppendNullReplace.value == false) stringResource(R.string.append) else stringResource(R.string.edit),
            requireShowTextCompose = true,
            textCompose = {
                Column(
                    // get height for add bottom padding when showing softkeyboard
                    modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
//                                println("layoutCoordinates.size.height:${layoutCoordinates.size.height}")
                        // 获取组件的高度
                        // unit is px ( i am not very sure)
                        componentHeight.intValue = layoutCoordinates.size.height
                    }
                ) {
                    MySelectionContainer {
                        Text(
                            replaceStringResList(
                                stringResource(if (truePrependFalseAppendNullReplace.value == null) R.string.line_at_n else R.string.new_line_at_n),
                                listOf(""+(if(lineNumOfEditLineDialog.value == LineNum.EOF.LINE_NUM) LineNum.EOF.TEXT else if (truePrependFalseAppendNullReplace.value != false) lineNumOfEditLineDialog.value else { lineNumOfEditLineDialog.value + 1 }))
                            )
                        )
                    }


                    Spacer(modifier = Modifier.height(10.dp))

                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isKeyboardCoveredComponent.value) Modifier.padding(bottom = keyboardPaddingDp.intValue.dp) else Modifier
                            ),
                        value = lineContentOfEditLineDialog.value,
                        onValueChange = {
                            lineContentOfEditLineDialog.value = it
                        },
                        label = {
                            Text(stringResource(R.string.content))
                        },
                    )
                }
            },
            okBtnText = stringResource(R.string.save),
            cancelTextColor = MyStyleKt.TextColor.danger(),
            onCancel = {showEditLineDialog.value = false}
        ) {
            showEditLineDialog.value = false
            doJobThenOffLoading(loadingOn, loadingOff, activityContext.getString(R.string.saving)) job@{
                try {
                    val lineNum = lineNumOfEditLineDialog.value
                    if(lineNum<1 && lineNum!=LineNum.EOF.LINE_NUM) {
                        Msg.requireShowLongDuration(activityContext.getString(R.string.invalid_line_number))
                        return@job
                    }

                    val lines = FsUtils.stringToLines(lineContentOfEditLineDialog.value)
                    val file = FilePath(fileFullPath).toFuckSafFile(activityContext)
                    if(truePrependFalseAppendNullReplace.value == true) {
                        FsUtils.prependLinesToFile(file, lineNum, lines, settings)
                    }else if (truePrependFalseAppendNullReplace.value == false) {
                        FsUtils.appendLinesToFile(file, lineNum, lines, settings)
                    }else {
                        FsUtils.replaceLinesToFile(file, lineNum, lines, settings)
                    }

                    Msg.requireShow(activityContext.getString(R.string.success))

                    refreshPage()
                }catch (e:Exception) {
                    val errMsg = e.localizedMessage ?:"err"
                    Msg.requireShowLongDuration(errMsg)
                    createAndInsertError(repoId, errMsg)
                }
            }
        }

    }

    if(showDelLineDialog.value) {
        ConfirmDialogAndDisableSelection(
            title = stringResource(R.string.delete),
            requireShowTextCompose = true,
            textCompose = {
                ScrollableColumn {
                    MySelectionContainer {
                        Text(
                            replaceStringResList(
                                stringResource(R.string.line_at_n),
                                listOf(
                                    if (lineNumOfEditLineDialog.value != LineNum.EOF.LINE_NUM) {
                                        "" + lineNumOfEditLineDialog.value
                                    } else {
                                        LineNum.EOF.TEXT
                                    }
                                )
                            )
                        )
                    }
                }
            },
            okBtnText = stringResource(R.string.delete),
            okTextColor = MyStyleKt.TextColor.danger(),
            onCancel = {showDelLineDialog.value = false}
        ) {
            showDelLineDialog.value = false

            doJobThenOffLoading(loadingOn, loadingOff, activityContext.getString(R.string.deleting)) job@{
                try {
                    val lineNum = lineNumOfEditLineDialog.value
                    if(lineNum<1 && lineNum!=LineNum.EOF.LINE_NUM) {
                        Msg.requireShowLongDuration(activityContext.getString(R.string.invalid_line_number))
                        return@job
                    }

                    val file = FilePath(fileFullPath).toFuckSafFile(activityContext)
                    FsUtils.deleteLineToFile(file, lineNum, settings)

                    Msg.requireShow(activityContext.getString(R.string.success))

                    refreshPage()

                }catch (e:Exception) {
                    val errMsg = e.localizedMessage ?:"err"
                    Msg.requireShowLongDuration(errMsg)
                    createAndInsertError(repoId, errMsg)
                }
            }
        }
    }

    if(showRestoreLineDialog.value) {
        ConfirmDialogAndDisableSelection(
            onDismiss = {},

            title = if(trueRestoreFalseReplace.value) stringResource(R.string.restore) else stringResource(R.string.replace),
            requireShowTextCompose = true,
            textCompose = {
                Column(
                    // get height for add bottom padding when showing softkeyboard
                    modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
//                                println("layoutCoordinates.size.height:${layoutCoordinates.size.height}")
                        // 获取组件的高度
                        // unit is px ( i am not very sure)
                        componentHeight.intValue = layoutCoordinates.size.height
                    }
                ) {
                    MySelectionContainer {
                        Text(stringResource(R.string.note_if_line_number_doesnt_exist_will_append_content_to_the_end_of_the_file), color = MyStyleKt.TextColor.highlighting_green)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        value = lineNumStrOfEditLineDialog.value,
                        onValueChange = {
                            lineNumStrOfEditLineDialog.value = it
                        },
                        label = {
                            Text(stringResource(R.string.line_number))
                        },
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isKeyboardCoveredComponent.value) Modifier.padding(bottom = keyboardPaddingDp.intValue.dp) else Modifier
                            ),
                        value = lineContentOfEditLineDialog.value,
                        onValueChange = {
                            lineContentOfEditLineDialog.value = it
                        },
                        label = {
                            Text(stringResource(R.string.content))
                        },
                    )
                }
            },
            okBtnText = if(trueRestoreFalseReplace.value) stringResource(R.string.restore) else stringResource(R.string.replace),
            cancelTextColor = MyStyleKt.TextColor.danger(),

            onCancel = {showRestoreLineDialog.value = false}
        ) {
            showRestoreLineDialog.value = false

            doJobThenOffLoading(loadingOn, loadingOff, activityContext.getString(R.string.restoring)) job@{
                try {
                    var lineNum = try {
                        lineNumStrOfEditLineDialog.value.toInt()
                    }catch (_:Exception) {
                        LineNum.invalidButNotEof
                    }

                    if(lineNum<1) {
                        // for append content to EOF
                        lineNum=LineNum.EOF.LINE_NUM
                    }

                    val lines = FsUtils.stringToLines(lineContentOfEditLineDialog.value)
                    val file = FilePath(fileFullPath).toFuckSafFile(activityContext)
                    if(trueRestoreFalseReplace.value) {
                        FsUtils.prependLinesToFile(file, lineNum, lines, settings)
                    }else {
                        FsUtils.replaceLinesToFile(file, lineNum, lines, settings)
                    }

                    Msg.requireShow(activityContext.getString(R.string.success))

                    refreshPage()

                }catch (e:Exception) {
                    val errMsg = e.localizedMessage ?:"err"
                    Msg.requireShowLongDuration(errMsg)
                    createAndInsertError(repoId, errMsg)
                }
            }
        }
    }

    val expandedMenu = mutableCustomStateOf(keyTag = stateKeyTag, keyName = "expandedMenu") { false }


    //用来实现设置行为no matched和all matched
    //不要用line.content替换contentOfLine，因为我搞不好有可能覆盖content的值
    val compareLineToText = { contentOfLine:String, line:PuppyLine, text:String ->
        doJobThenOffLoading {
            val newcp = CompareLinePair(
                line1 = contentOfLine,
                line1OriginType = line.originType,
                line1Num = line.lineNum,
                line1Key = line.key,

                //没有行号的文本，当作剪贴板内容来比较即可，懒得再添加其他类型了
                line2 = text,
                line2OriginType = CompareLinePairHelper.clipboardLineOriginType,
                line2Num = CompareLinePairHelper.clipboardLineNum,
                line2Key = CompareLinePairHelper.clipboardLineKey,
            )

            newcp.compare(
                betterCompare = betterCompare,
                matchByWords = matchByWords,
                map = indexStringPartListMap
            )

            setComparePairBuffer(CompareLinePair())

            reForEachDiffContent()
        }
    }

    val compareToClipboard = label@{ content:String, line:PuppyLine, trueContentToClipboardFalseClipboardToContent:Boolean ->
        if(content.isEmpty()) {
            Msg.requireShowLongDuration(activityContext.getString(R.string.can_t_compare_empty_line))
            return@label
        }

        if(line.originType == Diff.Line.OriginType.CONTEXT.toString()) {
            // context line no color, compare clipboard to it show nothing, nonsense
            Msg.requireShowLongDuration(activityContext.getString(R.string.can_t_compare_clipboard_to_context_line))
            return@label
        }

        val clipboardText = getClipboardText(clipboardManager)
        if(clipboardText.isNullOrEmpty()) {
            Msg.requireShowLongDuration(activityContext.getString(R.string.clipboard_is_empty))
            return@label
        }

        Msg.requireShow(activityContext.getString(R.string.comparing))

        doJobThenOffLoading {
            val newcp = if(trueContentToClipboardFalseClipboardToContent){  // content to clipboard
                CompareLinePair(
                    line1 = content,
                    line1OriginType = line.originType,
                    line1Num = line.lineNum,
                    line1Key = line.key,

                    line2 = clipboardText,
                    line2OriginType = CompareLinePairHelper.clipboardLineOriginType,
                    line2Num = CompareLinePairHelper.clipboardLineNum,
                    line2Key = CompareLinePairHelper.clipboardLineKey,
                )
            } else {  // clipboard to content
                CompareLinePair(
                    line1 = clipboardText,
                    line1OriginType = CompareLinePairHelper.clipboardLineOriginType,
                    line1Num = CompareLinePairHelper.clipboardLineNum,
                    line1Key = CompareLinePairHelper.clipboardLineKey,

                    line2 = content,
                    line2OriginType = line.originType,
                    line2Num = line.lineNum,
                    line2Key = line.key,
                )
            }

            newcp.compare(
                betterCompare = betterCompare,
                matchByWords = matchByWords,
                map = indexStringPartListMap
            )

            setComparePairBuffer(CompareLinePair())

            reForEachDiffContent()
        }

    }

    // this check include `originType` check, it is redundant actually, because when into this page, the line originType always one of CONTEXT or ADDITION or DELETION,
    //  because ADD_EOFNL/DEL_EOFNL already trans to ADDITION/DELETION, and the CONTEXT_EOFNL is deleted, will not shown
//    val enableLineActions = remember {
//            isFileAndExist
//            && (line.originType == Line.OriginType.CONTEXT.toString()
//                  || line.originType == Line.OriginType.ADDITION.toString()
//                  || line.originType == Line.OriginType.DELETION.toString()
//               )
//    }


    //因为下面用Row换行了，所以不需要内容以换行符结尾
//    prefix = prefix.removeSuffix(Cons.lineBreak)
//    content = content.removeSuffix(Cons.lineBreak)




    Row(
        modifier = Modifier
            .then(
                if (lineClickable) {
                    Modifier.clickable { expandedMenu.value = true }
                } else {
                    Modifier
                }
            )
            .fillMaxWidth()
            //如果是经过compare的添加或删除行，背景半透明，然后真修改的内容用不透明，这样就能突出真修改的内容
            //alpha值越大越不透明
            .background(if (useStringPartList) Libgit2Helper.getMatchedTextBgColorForDiff(inDarkTheme, line) else bgColor)
            .padding(end = 5.dp)

            //首行加顶部padding，其余不加
            // x 已经解决，给每个行计算了虚拟的索引，然后就解决了）按行分组时，首行若是一对 add/del，两个都会加顶部padding，但看起来感觉并不难受，所以不用改，就这样吧
            .addTopPaddingIfIsFirstLine(index)
//            .background(color)
//                            .clickable {
//
//                                //没卵用，这东西只会让人看了头疼，还不如加个点击弹出菜单可以让用户复制行之类的东西
//                                //显示新旧行号
////                                    showToast(
////                                        appContext,
////                                        "$lineTypeStr,$oldLineAt: ${line.oldLineNum},$newLineAt: ${line.newLineNum}",
////                                        Toast.LENGTH_SHORT
////                                    )
//                            },
    ) {
        //show add/del and line number, e.g. +123, or only show line num e.g. 123, it should make a settings item for it
        Text(
            text = prefix,
            color = lineNumColor,
            fontSize = lineNumSize.sp,
            fontFamily = FontFamily.Monospace, // 使用系统自带的等宽字体，不然那个+和-不等宽，看着难受
            modifier = Modifier
//                .background(MyStyleKt.TextColor.lineNumBgColor(inDarkTheme))
                .clickable {
                    openFileWithInnerSubPageEditor(
                        context = activityContext,
                        filePath = fileFullPath,
                        mergeMode = false,
                        readOnly = false,
                        //if jump line is EOF, should go to last line of file, but didn't know the line num, so set line num to a enough big number
                        goToLine = if(lineNum == LineNum.EOF.TEXT) LineNum.EOF.LINE_NUM else line.lineNum
                    )
                }

                //这个和changeType加行号(prefix)左边的padding构成完整每行左右padding
                //如果有前缀，padding小点，否则大点
                //这个放到clickable后面，这样点击padding区域也可触发onClick
                .padding(start = (if(prefix.isNotEmpty()) 2.dp else 5.dp))

        )

        if(useStringPartList) {
            //StringPart是比较过后的解析出哪些部分是真修改，哪些不是的一个数组，每个元素都包含完整字符串一部分，按序拼接即可得到原字符串
            val lastIndex = stringPartList!!.lastIndex  //用来判断，最后一个条目，需要移除末尾换行符

            //注意：这里不能改成用多个Text组件，不然若超过屏幕宽度软换行会失效
            Text(
                text = buildAnnotatedString {
                    stringPartList.forEachIndexed {idx, it ->
                        val text = content.substring(it.start, it.end)
                        //末尾会有个换行符，移除下，不然显示会多个行
                        val textNoLineSeparator = if(idx == lastIndex) text.removeSuffix(Cons.lineBreak) else text

                        if(it.modified) {  //为修改的内容设置高亮颜色
                            withStyle(style = SpanStyle(background = bgColor)) {
                                append(textNoLineSeparator)
                            }
                        }else {  //没修改的内容不用设置颜色，直接用默认的背景色即可
                            append(textNoLineSeparator)
                        }
                    }
                },

                color = textColor,
                overflow = TextOverflow.Visible,
                softWrap = true,
                fontSize = fontSize.sp,



            )
        }else {
            //文本内容
            Text(
                text = line.getContentNoLineBreak(),
                color = textColor,
                overflow = TextOverflow.Visible,
                softWrap = true,
                fontSize = fontSize.sp,


            )

        }


        if(lineClickable) {
            //必须禁用长按选择，否则若外部启用了长按选择，长按菜单项会崩溃
            DisableSelection {
                DropdownMenu(
                    expanded = expandedMenu.value,
                    onDismissRequest = { expandedMenu.value = false },
                    offset = DpOffset(x=150.dp, y=0.dp)
                ) {
                    //显示行修改类型和行号，例如 "+123"
                    DropdownMenuItem(
                        text = { Text(line.originType+lineNum)},
                        enabled = false,
                        onClick ={}
                    )

                    if(enableLineEditActions) {

                        // EOFNL status maybe wrong, before Edit or Del, must check it actually exists or is not, when edit line num is EOF and EOFNL is not exists, then prepend a LineBreak before users input
                        //编辑或删除前，如果行号是EOF，必须检查EOF NL是否实际存在，如果EOFNL不存在，则先添加一个空行，再写入用户的实际内容，如果执行删除EOF且文件末尾无空行，则不执行任何删除；
                        // 如果EOF为删除，则不用检查，点击恢复后直接在文件末尾添加一个空行即可
                        if(line.originType == Diff.Line.OriginType.ADDITION.toString() || line.originType == Diff.Line.OriginType.CONTEXT.toString()){
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit))},
                                onClick = {
                                    initEditLineDialog(line.getContentNoLineBreak(), line.lineNum, null)

                                    expandedMenu.value = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.insert))},
                                onClick = {
                                    initEditLineDialog("", line.lineNum, true)

                                    expandedMenu.value = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.append))},
                                onClick = {
                                    initEditLineDialog("", line.lineNum, false)

                                    expandedMenu.value = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete))},
                                onClick = {
                                    initDelLineDialog(line.lineNum)
                                    expandedMenu.value = false
                                }
                            )
                        }else if(line.originType == Diff.Line.OriginType.DELETION.toString()) {
                            // prepend(insert)
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.restore))},
                                onClick = {
                                    initRestoreLineDialog(line.getContentNoLineBreak(), line.lineNum, true)
                                    expandedMenu.value = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.replace))},
                                onClick = {
                                    initRestoreLineDialog(line.getContentNoLineBreak(), line.lineNum, false)
                                    expandedMenu.value = false
                                }
                            )
                        }

                    }


                    if(enableSelectCompare) {
                        val cp = getComparePairBuffer()
                        val line1ready = cp.line1ReadyForCompare()
                        //这里不需要判断content.isNotEmpty()，因为只有eof才空，而eof会禁用此菜单项，所以只要显示此菜单项，就必定非eof非空字符串
                        DropdownMenuItem(
                            // disable compare for same line number
                            enabled = line.key != cp.line1Key,
                            text = { Text(
                                if(line1ready) replaceStringResList(stringResource(R.string.compare_to_origintype_linenum), listOf(cp.line1OriginType + cp.line1Num))
                                else { stringResource(R.string.select_compare) }
                            )},
                            onClick = label@{
                                expandedMenu.value = false

                                if(content.isEmpty()) {
                                    Msg.requireShow(activityContext.getString(R.string.can_t_compare_empty_line))
                                    return@label
                                }

                                if(line1ready) {

                                    // (20241114 change to no check, force re-compare, cause sometimes, maybe "a compare to b" has difference result with "b compare to a")
                                    // same line num already compared in normal procudure
//                                    if(line.lineNum == cp.line1Num && (
//                                                (line.originType == OriginType.ADDITION.toString() && cp.line1OriginType == OriginType.DELETION.toString())
//                                                        ||  (line.originType == OriginType.DELETION.toString() && cp.line1OriginType == OriginType.ADDITION.toString())
//                                                  )
//                                    ) {
//                                        Msg.requireShow(activityContext.getString(R.string.selected_lines_already_compared))
//                                        return@label
//                                    }

                                    // both are CONTEXT
                                    if(line.originType == Diff.Line.OriginType.CONTEXT.toString() && cp.line1OriginType == line.originType) {
                                        Msg.requireShow(activityContext.getString(R.string.can_t_compare_both_context_type_lines))
                                        return@label
                                    }

                                    cp.line2 = content
                                    cp.line2Num = line.lineNum
                                    cp.line2OriginType = line.originType
                                    cp.line2Key = line.key

                                    Msg.requireShow(activityContext.getString(R.string.comparing))

                                    doJobThenOffLoading {
                                        cp.compare(
                                            betterCompare = betterCompare,
                                            matchByWords = matchByWords,
                                            map = indexStringPartListMap
                                        )

                                        // clear buffer
                                        setComparePairBuffer(CompareLinePair())

                                        // re-render view
                                        reForEachDiffContent()
                                    }

                                }else {
                                    cp.line1 = content
                                    cp.line1Num = line.lineNum
                                    cp.line1OriginType = line.originType
                                    cp.line1Key = line.key
                                    Msg.requireShow(replaceStringResList(activityContext.getString(R.string.added_line_for_compare), listOf(line.originType+lineNum)) )
                                }

                            }
                        )


                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.compare_to_clipboard)+" ->")},
                            onClick = {
                                expandedMenu.value = false

                                compareToClipboard(content, line, true)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.compare_to_clipboard)+" <-")},
                            onClick = {
                                expandedMenu.value = false

                                compareToClipboard(content, line, false)
                            }
                        )


                        if(getComparePairBuffer().isEmpty().not()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.clear_compare))},
                                onClick = {
                                    expandedMenu.value = false

                                    setComparePairBuffer(CompareLinePair())
                                }
                            )
                        }
                    }

                    if(enableLineCopy) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.copy))},
                            onClick = {
                                clipboardManager.setText(AnnotatedString(content))
                                Msg.requireShow(activityContext.getString(R.string.copied))

                                expandedMenu.value = false
                            }
                        )
                    }

                    //开发者选项，一般用不到
                    if(DevFeature.showMatchedAllAtDiff.state.value) {
                        //让选中行变成无匹配的状态的颜色（对空行无效）
                        DropdownMenuItem(
                            text = { Text(DevFeature.setDiffRowToNoMatched)},
                            onClick = {
                                expandedMenu.value = false

                                //空字符串有做处理，不会进行比较，所以得整成空行
                                compareLineToText(content, line, "\n")
                            }
                        )

                        //让选中行变成完全匹配的状态的颜色
                        DropdownMenuItem(
                            text = { Text(DevFeature.setDiffRowToAllMatched)},
                            onClick = {
                                expandedMenu.value = false

                                //自己和自己比较永远完全匹配，而且有做全等判断，仅比较地址，性能绝佳
                                compareLineToText(content, line, content)
                            }
                        )
                    }
                }
            }
        }

    }

}
