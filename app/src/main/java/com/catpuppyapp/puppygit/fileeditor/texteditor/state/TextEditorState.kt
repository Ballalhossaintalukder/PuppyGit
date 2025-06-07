package com.catpuppyapp.puppygit.fileeditor.texteditor.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import com.catpuppyapp.puppygit.dto.UndoStack
import com.catpuppyapp.puppygit.etc.Ret
import com.catpuppyapp.puppygit.fileeditor.texteditor.view.SearchPos
import com.catpuppyapp.puppygit.fileeditor.texteditor.view.SearchPosResult
import com.catpuppyapp.puppygit.screen.shared.FuckSafFile
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.utils.EditCache
import com.catpuppyapp.puppygit.utils.FsUtils
import com.catpuppyapp.puppygit.utils.Msg
import com.catpuppyapp.puppygit.utils.MyLog
import com.catpuppyapp.puppygit.utils.UIHelper
import com.catpuppyapp.puppygit.utils.doActIfIndexGood
import com.catpuppyapp.puppygit.utils.generateRandomString
import com.catpuppyapp.puppygit.utils.isGoodIndexForList
import com.catpuppyapp.puppygit.utils.isGoodIndexForStr
import com.catpuppyapp.puppygit.utils.isStartInclusiveEndExclusiveRangeValid
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.OutputStream
import java.security.InvalidParameterException


private const val TAG = "TextEditorState"
private const val lb = "\n"

private fun targetIndexValidOrThrow(targetIndex:Int, listSize:Int) {
    if (targetIndex < 0 || targetIndex >= listSize) {
        throw IndexOutOfBoundsException("targetIndex '$targetIndex' out of range '[0, $listSize)'")
    }
}

//不实现equals，直接比较指针地址，反而性能可能更好，不过状态更新可能并不准确，例如fields没更改的情况也会触发页面刷新
@Immutable
class TextEditorState private constructor(

    /**
     the `fieldsId` only about fields, same fieldsId should has same fields，but isn't enforce (但不强制要求)
     */
    val fieldsId: String,
    val fields: List<TextFieldState>,

    val selectedIndices: List<Int>,
    val isMultipleSelectionMode: Boolean,

    val focusingLineIdx: Int?,

    //话说这几个状态如果改变是否会触发重组？不过就算重组也不会创建新数据拷贝，性能影响应该不大
    //这个东西可以每个状态独有，但会增加额外拷贝，也可所有状态共享，但无法判断每个状态是否已经创建快照或者是否有未保存修改，目前采用方案2，所有Editor状态共享相同的指示内容是否修改的状态
    val isContentEdited: MutableState<Boolean>,  // old name `isContentChanged`
    val editorPageIsContentSnapshoted:MutableState<Boolean>,

    val onChanged: (newState:TextEditorState, trueSaveToUndoFalseRedoNullNoSave:Boolean?, clearRedoStack:Boolean) -> Unit,

) {
    private val lock = Mutex()

    fun copy(
        fieldsId: String = this.fieldsId,
        fields: List<TextFieldState> = this.fields,
        selectedIndices: List<Int> = this.selectedIndices,
        isMultipleSelectionMode: Boolean = this.isMultipleSelectionMode,
        focusingLineIdx: Int? = this.focusingLineIdx,
        isContentEdited: MutableState<Boolean> = this.isContentEdited,
        editorPageIsContentSnapshoted:MutableState<Boolean> = this.editorPageIsContentSnapshoted,
        onChanged: (newState:TextEditorState, trueSaveToUndoFalseRedoNullNoSave:Boolean?, clearRedoStack:Boolean) -> Unit = this.onChanged,
    ):TextEditorState = create(
        fieldsId = fieldsId,
        fields = fields,
        selectedIndices = selectedIndices,
        isMultipleSelectionMode = isMultipleSelectionMode,
        focusingLineIdx = focusingLineIdx,
        isContentEdited = isContentEdited,
        editorPageIsContentSnapshoted = editorPageIsContentSnapshoted,
        onChanged = onChanged
    )


    suspend fun undo(undoStack: UndoStack) {
        undoOrRedo(undoStack, true)
    }

    suspend fun redo(undoStack: UndoStack) {
        undoOrRedo(undoStack, false)
    }

    private suspend fun undoOrRedo(undoStack: UndoStack, trueUndoFalseRedo:Boolean) {
        lock.withLock {
            val lastState = if(trueUndoFalseRedo) undoStack.undoStackPop() else undoStack.redoStackPop()
            if(lastState != null) {
                isContentEdited?.value=true
                editorPageIsContentSnapshoted?.value=false
//                _fieldsId = TextEditorState.newId()

                //无论执行redo还是undo，都不需要清redoStack，所以这里此值传false
                val clearRedoStack = false
                // 第2个值需要取反，因为执行redo的时候期望更新undoStack，执行undo的时候期望更新redoStack
                onChanged(lastState, !trueUndoFalseRedo, clearRedoStack)
            }
        }
    }


    /**
     * x 已修复） 20241203: 发现有bug，无法找到 " w " 这样字符，已经修复，在无匹配时将被查找的文件中的字符串索引向后移一位再和关键字从头匹配即可（反向查找时是向前移一位然后和关键字末尾匹配）
    @param toNext : if `toNext` is false, will search to previous
    缺陷：不支持查找包含换行符的关键字
     * @return SearchPos : if not found, will return SearchPos(-1,-1)
     */
    fun doSearch(keyword:String, toNext:Boolean, startPos: SearchPos): SearchPosResult {
        val funName="doSearch"

        fun getCurTextOfIndex(idx:Int, list:List<TextFieldState>):String{
            return list[idx].value.text.lowercase()
        }

        try {
            //搜索关键字
            //开始从当前位置查找关键字，直到找到，或者从头到尾都找不到
            val f = fields
            if(f.isEmpty() || keyword.isEmpty()) {
                return SearchPosResult.NotFound
            }

            val goodIndex = isGoodIndexForList(startPos.lineIndex, f)
            //如果行号有误，把列号重置为0或最后一行把列号重置为0或最后一列；否则使用原行号和列号（不过列号仍有可能有误
            val curPos = startPos.copy(lineIndex = if(goodIndex) startPos.lineIndex else {if(toNext) 0 else f.size-1})
            var curText = getCurTextOfIndex(curPos.lineIndex, f)
            curPos.columnIndex = if(goodIndex) startPos.columnIndex else {if(toNext) 0 else curText.length-1}
            val endPosOverThis = curPos.copy()  //越过这一行这一列，就查到开头了，结束了
            var curIndexOfKeyword= if(toNext) 0 else keyword.length-1

            curPos.columnIndex = if(isGoodIndexForStr(curPos.columnIndex, curText)){
                curPos.columnIndex
            }else{
                if(toNext){
                    0
                }else {
                    curText.length-1
                }
            }

            var char = if(curText.isEmpty()) null else curText[curPos.columnIndex]
            var charOfKeyword = keyword[curIndexOfKeyword]
            var looped = false

            val invalidIndex = -1

            // last value of matched a char
            var resetIndex = invalidIndex
            while(true) {
//                println("在搜索:lineIndex=${curPos.lineIndex}, columnIndex=${curPos.columnIndex}")  //test1791022120240812
                //matched 1 char
                if(char != null && char == charOfKeyword) {
//                    println(curPos) //test1791022120240812
                    if(toNext){
                        curIndexOfKeyword++
                    }else {
                        curIndexOfKeyword--
                    }

                    //found!
                    if(!isGoodIndexForStr(curIndexOfKeyword, keyword)) {
                        val foundLineIdx = curPos.lineIndex
                        val foundColumnIdx = if(toNext) curPos.columnIndex+1-keyword.length else curPos.columnIndex

                        var nextColumn = if(toNext) foundColumnIdx+keyword.length else foundColumnIdx-1
                        var nextLineIdx = foundLineIdx
                        //需要换行
                        if(!isGoodIndexForStr(nextColumn, curText)) {
                            nextLineIdx = if(toNext) nextLineIdx+1 else nextLineIdx-1
                            if(!isGoodIndexForList(nextLineIdx, f)) {
                                nextLineIdx = if(toNext) 0 else f.size-1
                            }

                            nextColumn = if(toNext) 0 else getCurTextOfIndex(nextLineIdx, f).length-1
                        }
//                        println("posResult:${SearchPosResult(foundPos = curPos.copy(lineIndex = foundLineIdx, columnIndex = foundColumnIdx), nextPos = SearchPos(lineIndex =  nextLineIdx, columnIndex = nextColumn))}")  //test1791022120240812
                        return SearchPosResult(
                            foundPos = curPos.copy(lineIndex = foundLineIdx, columnIndex = foundColumnIdx),
                            nextPos = SearchPos(lineIndex =  nextLineIdx, columnIndex = nextColumn)
                        )
                    }


                    //更新被搜索的字符串索引
                    if(toNext) {
                        curPos.columnIndex++
                    }else {
                        curPos.columnIndex--
                    }

                    // 第一次匹配，若匹配关键字失败，重置到当前字符的下一个字符(若反向查找，则是上一个字符)
                    if(resetIndex == invalidIndex) {
                        resetIndex = curPos.columnIndex
                    }


                }else {  // not matched!
                    // reset index of keyword
                    curIndexOfKeyword= if(toNext) 0 else keyword.length-1

                    // if last is matched, keep target text index at same place for matching to head or tail of keyword

                    //如果上次未匹配到，正常往前推进，否则重置索引指向上次匹配到的字符串之后的第一个字符
                    if(resetIndex == invalidIndex) {  //上次未匹配到
                        //更新被搜索的字符串索引
                        if(toNext) {
                            curPos.columnIndex++
                        }else {
                            curPos.columnIndex--
                        }
                    }else {  //上次匹配到过部分字符
                        //匹配过，将索引重置为下一个索引，例如：被搜索字符串：abbbbcd，关键字bbc，若在索引1开始匹配到，索引3匹配失败，那下一个被搜索的索引应该是2
                        curPos.columnIndex = resetIndex
                        //重置索引每次使用一次后，需将其置为无效索引
                        resetIndex = invalidIndex
                    }

                }

                //这里取出的字符有两种情况，要么是关键字中的下一个字符，要么是重置后的开头或末尾字符
                // 情况1： 索引仍有效，说明只匹配了部分关键字，需要继续查找
                // 情况2： 无匹配，索引已重置到关键字开头或末尾
                charOfKeyword = keyword[curIndexOfKeyword]

                //查找到当前行开头或末尾了，该换行了
                if(!isGoodIndexForStr(curPos.columnIndex, curText)) {  //需要换行
                    if(looped) {  //过了最初查找的那行且需要换行，说明查找完了，可返回了
//                        val lineOverLooped = if(toNext) curPos.lineIndex > endPosOverThis.lineIndex else curPos.lineIndex < endPosOverThis.lineIndex
//                        if(lineOverLooped) {
//                            return SearchPos.NotFound
//                        }

                        //如果循环查找，还是进入到换行的这块代码，就可以直接返回了，已经搜索过头了
                        return SearchPosResult.NotFound

                    }

                    //执行到这，looped必定为假，意味着还没查找到起点，还得继续找

                    //换行后将重置索引重新设为无效
                    resetIndex = invalidIndex

                    //换行，需要reset index of keyword，不然搜索时行末尾和下行开头会被认为是连续的而导致结果出错
                    curIndexOfKeyword= if(toNext) 0 else keyword.length-1
                    charOfKeyword = keyword[curIndexOfKeyword]


                    //换行
                    if(toNext) curPos.lineIndex++ else curPos.lineIndex--

                    //查找到文件第一行或最后一行了，该从头或从尾重新查了 （循环查找）
                    if(!isGoodIndexForList(curPos.lineIndex, f)){
                        curPos.lineIndex = if(toNext) 0 else f.size-1
                    }

                    curText = getCurTextOfIndex(curPos.lineIndex, f)

                    curPos.columnIndex = if(toNext) 0 else curText.length-1

                    //换行换到最初查找的那行了
                    if(curPos.lineIndex==endPosOverThis.lineIndex) { //隐含 `&& !looped`
                        looped = true
                    }
                }

                //注释这段代码可确保完整查找起始搜索那行以免光标在起始行的关键字中间时漏掉关键字
                //光标在关键字中间时搜索会有bug：如果启用此代码块，将会在匹配到起始搜索行的起始列时终止，导致无法匹配以下情况：例如：关键字"1234"，全文只有一行匹配文本为"1[光标]2345"，这时，向上搜索，将会无法匹配到"1234"，因为代码会在抵达光标处时返回not found，只要光标在关键字中间皆可复现此bug（例如把前面的匹配文本改为"12[光标]345"，也会not found），不过注意：因为搜索模式下更新列索引有bug所以禁用了搜索模式下更新列索引的机制，所以需要在非搜索模式把光标放到关键字中间才能复现此bug。
                //找了一轮了，在起始行，检查是否已经超过起始列，如果超了直接返回，如果没超，继续搜索，直到超了起始列（在这返回）或者超了起始行（在上面的代码返回）
//                if(looped) {
//                    //循环查找，行
//                    //这里加等号是正确的，另外：即使把等号去掉，也无法修复上面提到的光标在关键字中间时搜索会有bug的问题，因为只要光标位置离起始列的距离大于1个字符，就还是无法完整匹配整行，所以还是会有那个bug
//                    val end = if(toNext) curPos.columnIndex >= endPosOverThis.columnIndex else curPos.columnIndex <= endPosOverThis.columnIndex
//                    if(end){
//                        return SearchPosResult.NotFound
//                    }
//                }

                //把空行当作null，无匹配即可
                char = if(curText.isEmpty()) null else curText[curPos.columnIndex]
            }


        }catch (e:Exception) {
            Msg.requireShowLongDuration("err: "+e.localizedMessage)
            MyLog.e(TAG, "#$funName err: keyword=$keyword, toNext=$toNext, startPos=$startPos, err=${e.stackTraceToString()}")
            return SearchPosResult.NotFound
        }

    }


    suspend fun splitNewLine(targetIndex: Int, textFieldValue: TextFieldValue) {
        lock.withLock {
            targetIndexValidOrThrow(targetIndex, fields.size)

            if (!textFieldValue.text.contains('\n')) {
                throw InvalidParameterException("textFieldValue doesn't contains newline")
            }

            val newText = textFieldValue.text

            //分割当前行
            val splitFieldValues = splitTextsByNL(newText)

            //创建新字段集合
            val newFields = fields.toMutableList()

            //处理第一行
            val splitFirstLine = splitFieldValues.first()
            // 如果第一行为空，则说明在旧行的开头插入了换行符，可能是粘贴了多行，也可能是输入或粘贴了单行，这两种情况有个共同点，
            // 就是新内容的最后一行就是旧内容本身，而且这一行的changeType状态应该保持不变
            val oldLine = newFields[targetIndex]
            //这里加oldLine text 非空判断是为了使旧行为空时按回车让旧行changeType保持不变，追加的行为新增
            val newLineAtOldLineHead = oldLine.value.text.isNotEmpty() && splitFirstLine.text.isEmpty()
            newFields[targetIndex] = oldLine.copy(value = splitFirstLine, isSelected = false).apply {
                if(newLineAtOldLineHead) {  //在旧行头部添加了一换行符
                    // 如果新行开头是空行，必然是新行，所以强制更新而不用if none则更新
                    updateLineChangeType(LineChangeType.NEW)

                    //这里用oldLine或this的changeType都行，因为 this是oldLine的copy，而且没有拷贝changeType，所以他们两个的changeType应该相同，但从逻辑上来说，应该用oldLine的
                }else if(oldLine.changeType == LineChangeType.NONE && this.value.text != oldLine.value.text) {  //为真则是在旧行中间添加的换行符，否则是在旧行末尾（无需处理，维持旧行changeType即可）
                    // this是拷贝了新内容的新字段，如果新字段的文本和旧字段的文本不同，说明内容改变了，更新changeType，否则不用更新
                    updateLineChangeType(LineChangeType.UPDATED)
                } // else 在旧行末尾添加了换行符，无需处理
            }

            //追加新行（注意：新行从第二行开始，并且，如果新行是从旧行的中间分割的，则新行的第一行可能有旧行的后半段内容）
            val newSplitFieldValues = splitFieldValues.subList(1, splitFieldValues.count())
            //把新内容转换成text field对象，其类型必然是NEW，因为是新增的
            val newSplitFieldStates = newSplitFieldValues.map { TextFieldState(value = it, isSelected = false, changeType = LineChangeType.NEW) }

            //如果是从头部插入的新行，则需要检查最后一行的内容是否和旧行的内容相同，如果相同，其changeType应维持旧行，否则改成updated，但不管怎么，它都不应该是NEW，因为本质上它是原来的旧行修改而来的
            if(newLineAtOldLineHead) {
                newSplitFieldStates[newSplitFieldStates.size - 1].apply {
                    //注意这里是强制更新行changeType，因为上面已经更新成NEW了，这里如果不强制，则无法更新
                    updateLineChangeType(oldLine.changeType)
                    //在旧行的基础上，决定最终状态
                    if(oldLine.changeType == LineChangeType.NONE && this.value.text != oldLine.value.text) {
                        updateLineChangeType(LineChangeType.UPDATED)
                    }
                }
            }

            //addAll若待插入值，最大值是size，不会越界，若超过就越了
            newFields.addAll(targetIndex + 1, newSplitFieldStates)

            //追加行后的目标索引，实际对应的是 newSplitFieldValues 最后一个元素在当前文件的所有行中的位置
            val lastNewSplitFieldIndex = targetIndex + newSplitFieldValues.count()

//            val newFocusingLineIdx = mutableStateOf(focusingLineIdx)
//            val newSelectedIndices = selectedIndices.toMutableList()

            // 判断是定位到新内容最后一行开头还是末尾
            val oldTargetText = fields[targetIndex].value.text  //旧行
            val newTargetFirstText = splitFirstLine.text  //分割后的第一行对应旧的目标行
            val newTargetLastText = splitFieldValues.last().text  //分割后的最后一行，光标会focus到此行，此行与 newSplitFieldValues 的最后一个元素相同，因为 newSplitFieldValues 是 splitFieldValues 的一个sub list

            val sfiRet = selectFieldInternal(
                init_fields = newFields,
                init_selectedIndices = selectedIndices,
                isMutableFields = true,
                isMutableSelectedIndices = false,
                targetIndex = lastNewSplitFieldIndex,

                // 如果自定义位置有bug，就禁用下面的改用这个，注释option和columnStartIndexInclusive
//                option = SelectionOption.FIRST_POSITION,

                option = SelectionOption.CUSTOM,
                columnStartIndexInclusive = if (oldTargetText == newTargetFirstText) {  //新旧第一行相等，定位到新内容最后一行末尾
                    newTargetLastText.length
                } else { //新旧内容不相等，从新内容最后一行和旧内容的末尾开始匹配，定位到从尾到头第一个不匹配的字符后面
                    var nl = newTargetLastText.length
                    var ol = oldTargetText.length
                    var bothLen = Math.min(ol, nl)
                    while (bothLen-- > 0) {
                        if(newTargetLastText[--nl] != oldTargetText[--ol]) {
                            nl++
                            break
                        }
                    }

                    //要定位到倒数第一个不匹配的字符后面，所以需要加1
                    nl
                },
            )


            //更新状态变量
            isContentEdited?.value=true
            editorPageIsContentSnapshoted?.value=false
            EditCache.writeToFile(newText)


            val newState = internalCreate(
                fields = sfiRet.fields,
                fieldsId = newId(),
                selectedIndices = sfiRet.selectedIndices,
                isMultipleSelectionMode = isMultipleSelectionMode,
                focusingLineIdx = sfiRet.focusingLineIdx
            )

            onChanged(newState, true, true)
        }
    }


//    @Deprecated("这个和splitNewLine一样，可转化为splitNewLien处理，省得维护两个相同的逻辑，头疼")
//    suspend fun splitAtCursor(targetIndex: Int, textFieldValue: TextFieldValue) {
//        lock.withLock {
//
//            targetIndexValidOrThrow(targetIndex, fields.size)
//
//            val splitPosition = textFieldValue.selection.start  //光标位置，有可能在行末尾，这时和text.length相等，并不会越界
//            val maxOfPosition = textFieldValue.text.length
//            if (splitPosition < 0 || splitPosition > maxOfPosition) {  //第2个判断没错，就是大于最大位置，不是大于等于，没写错，光标位置有可能在行末尾，这时其索引和text.length相等，所以只有大于才有问题
//                throw InvalidParameterException("splitPosition '$splitPosition' out of range '[0, $maxOfPosition]'")
//            }
//
//            val newText = textFieldValue.text
//            val firstStart = 0
//            val firstEnd = if (splitPosition == 0) 0 else splitPosition
//            val first = newText.substring(firstStart, firstEnd)
//
//            val newLineAtOldLineHead = first.isEmpty()  // or `firstEnd == 0`
//
//            val secondEnd = newText.length
//            val second = newText.substring(firstEnd, secondEnd)
//
//            val firstValue = textFieldValue.copy(first)
//            val newFields = fields.toMutableList()
//
//            //更新第一行
//            val oldField = newFields[targetIndex]
//            newFields[targetIndex] = oldField.copy(value = firstValue, isSelected = false).apply {
//                if(newLineAtOldLineHead) {
//                    updateLineChangeType(LineChangeType.NEW)
//                }else if(this.value.text != oldField.value.text) {
//                    // 如果新内容不等于旧内容，说明发生了修改
//                    // 注：这两个内容都不包含换行符
//                    updateLineChangeTypeIfNone(LineChangeType.UPDATED)
//                }  // else，就是在旧行末尾插入了换行符，保持当前行changeType不变即可，无需修改
//            }
//
//            //添加第2行
//            newFields.add(
//                targetIndex + 1,
//                TextFieldState(
//                    value = TextFieldValue(second, TextRange.Zero),
//                    isSelected = false,
//                ).apply {
//                    if(新旧text相等则使用原changetype，否则使用新的)
//                    updateLineChangeTypeIfNone(if(newLineAtOldLineHead) this.changeType else LineChangeType.NEW)
//                }
//            )
//
//
////            val newFocusingLineIdx = mutableStateOf(focusingLineIdx)
////            val newSelectedIndices = selectedIndices.toMutableList()
//
//            val sfiRet = selectFieldInternal(
//                init_fields = newFields,
//                init_selectedIndices = selectedIndices,
//                isMutableFields = true,
//                isMutableSelectedIndices = false,
////                out_focusingLineIdx = newFocusingLineIdx,
////                init_focusingLineIdx = focusingLineIdx,
//
//                targetIndex = targetIndex + 1
//            )
//
//
//            isContentEdited?.value=true
//            editorPageIsContentSnapshoted?.value=false
//
//            val newState = internalCreate(
//                fields = sfiRet.fields,
//                fieldsId = newId(),
//                selectedIndices = sfiRet.selectedIndices,
//                isMultipleSelectionMode = isMultipleSelectionMode,
//                focusingLineIdx = sfiRet.focusingLineIdx
//            )
//
//            onChanged(newState, true, true)
//        }
//    }


    suspend fun updateField(targetIndex: Int, textFieldValue: TextFieldValue) {
        lock.withLock {
            targetIndexValidOrThrow(targetIndex, fields.size)

            if (textFieldValue.text.contains('\n')) {
                throw InvalidParameterException("textFieldValue contains newline")  // contains new line应调用splitNewLine
            }

            val newText = textFieldValue.text

            val oldField = fields[targetIndex]
            //检查字段内容是否改变，由于没改变也会调用此方法，所以必须判断下才能知道内容是否改变
            val contentChanged = oldField.value.text != newText  // 旧值 != 新值

            //没什么意义，两者总是不相等，似乎相等根本不会触发updateField
//            if(oldField.equals(textFieldValue)) {
//                return
//            }

            var maybeNewId = fieldsId

            val newFields = fields.toMutableList()

            val updatedField = newFields[targetIndex].copy(value = textFieldValue);

            //判断文本是否相等，注意：就算文本不相等也不能在这返回，不然页面显示有问题，比如光标位置会无法更新
            newFields[targetIndex] = if(contentChanged) {
                isContentEdited?.value = true
                editorPageIsContentSnapshoted?.value = false
                maybeNewId = newId()

                //缓存新值
                EditCache.writeToFile(newText)

                //更新当前行状态为已修改
                updatedField.apply { updateLineChangeTypeIfNone(LineChangeType.UPDATED) }
            }else {
                updatedField
            }



            val newState = internalCreate(
                fields = newFields,
                fieldsId = maybeNewId,
                selectedIndices = selectedIndices,
                isMultipleSelectionMode = isMultipleSelectionMode,
//                focusingLineIdx = targetIndex,
                // -1 to skip focusing, cause `onFocus` callback of `com.catpuppyapp.puppygit.fileeditor.texteditor.view.TextField` already focused target line, usually the onFocus call `selectField()` of this class
                // 传 -1 跳过聚焦某行，因为onFocus()已经聚焦了，另外，通常onFocus()会调用这里的selectField()
                focusingLineIdx = -1,
            )

            onChanged(newState, if(contentChanged) true else null, contentChanged)
        }
    }


    suspend fun appendOrReplaceFields(targetIndex: Int, text: String, trueAppendFalseReplace:Boolean) {
        lock.withLock {
            if(targetIndex < 0) {
                return
            }

            val textFiledStates = mutableListOf<TextFieldState>()


            // 创建对象并更新 change type
            //changeType: 先全初始化为new，如果是replace，首行状态后面会和旧行比较来判断是修改还是新增还是没变
            text.lines().forEach { textFiledStates.add(TextFieldState(value = TextFieldValue(text = it), changeType = LineChangeType.NEW)) }



            //若超过size，追加到末尾
            val targetIndex = if(trueAppendFalseReplace) targetIndex+1 else targetIndex

            var targetLineSelected = false

            val newFields = fields.toMutableList()

            val targetIndexOverSize = targetIndex >= newFields.size
            //若超过有效索引则追加到末尾；否则追加到目标行（原本的目标行会后移）
            if(targetIndexOverSize) {
                newFields.addAll(textFiledStates)
            }else {
                //如果是replace，保留之前行的选择状态
                //到这才能确定索引没越界，所以在这取而不是在if else之前取
                if(trueAppendFalseReplace.not()) {
                    //replace: 保留旧行选择状态，后面会恢复旧行选择状态
                    val oldFirstLine = newFields[targetIndex]
                    targetLineSelected = oldFirstLine.isSelected

                    //replace: 如果新旧首行相同，维持旧行changeType；否则更新为UPDATED
                    val newFirstLine = textFiledStates.first()
                    textFiledStates[0] = newFirstLine.copy(changeType = if(oldFirstLine.value.text == newFirstLine.value.text) oldFirstLine.changeType else LineChangeType.UPDATED)
                }

                newFields.addAll(targetIndex, textFiledStates)
            }

            //若是replace则移除之前的当前行
            if(trueAppendFalseReplace.not()) {
                //更新目标行的选择状态使其和旧行一致
                if(targetLineSelected) {
                    newFields[targetIndex] = newFields[targetIndex].copy(isSelected = true)
                }

                //粘贴之后，之前的目标行会被顶到当前要粘贴的内容后面，如果是replace，则把这行删除
                newFields.removeAt(targetIndex + textFiledStates.size)
            }

            var newFocusingLineIdx = targetIndex
            val newSelectedIndices = selectedIndices.toMutableList()

            //如果目标索引没超过原集合大小，则判断下是否需要更新选中索引列表；否则不用判断，因为超过大小的话，等于无效索引，肯定不会在已选中列表
            if(targetIndexOverSize.not()) {
                val selectedIndexOffsetValue = textFiledStates.size.let {if(trueAppendFalseReplace) it else (it-1)}
                val indexNeedOffset:(selectedIdx:Int)->Boolean = if(trueAppendFalseReplace){{ it >= targetIndex }} else {{ it > targetIndex }}

                val focusIndex = newFocusingLineIdx
                // 更新聚焦行索引
                if(focusIndex != null && indexNeedOffset(focusIndex)) {
                    newFocusingLineIdx = focusIndex+selectedIndexOffsetValue
                }

                //更新已选中索引
                if(newSelectedIndices.isNotEmpty()) {
                    //更新选中索引集合
                    for((i, selectedIndex) in newSelectedIndices.withIndex()) {
                        if(indexNeedOffset(selectedIndex)) {
                            newSelectedIndices[i] = selectedIndex + selectedIndexOffsetValue
                        }
                    }
                }
            }



            //通知页面状态变化
            isContentEdited?.value = true
            editorPageIsContentSnapshoted?.value = false

            val newState = internalCreate(
                fields = newFields,
                fieldsId = newId(),
                selectedIndices = newSelectedIndices,
                isMultipleSelectionMode = isMultipleSelectionMode,
                focusingLineIdx = newFocusingLineIdx

            )

            //更新状态
            onChanged(newState, true, true)
        }
    }

    suspend fun deleteNewLine(targetIndex: Int) {
        lock.withLock {
            targetIndexValidOrThrow(targetIndex, fields.size)

            if (targetIndex == 0) {
                return
            }

            val newFields = fields.toMutableList()

            val toField = newFields[targetIndex - 1]
            val fromField = newFields[targetIndex]
            val toText = toField.value.text
            val fromText = fromField.value.text

            val concatText = StringBuilder(toText.length + fromText.length).append(toText).append(fromText).toString()

            val concatSelection = TextRange(toText.count())  // end of `toText`
            val concatTextFieldValue = TextFieldValue(text = concatText, selection = concatSelection)
            val toTextFieldState = newFields[targetIndex - 1].copy(value = concatTextFieldValue, isSelected = false).apply {
                val newChangeType = if(toText.isEmpty() && fromText.isNotEmpty()) {
                    fromField.changeType
                }else if(toText.isNotEmpty() && fromText.isEmpty()) {
                    toField.changeType
                }else if(toText.isEmpty() && fromText.isEmpty()) {
                    toField.changeType
                }else { // if(toText.isNotEmpty() && fromText.isNotEmpty())
                    if(toField.changeType == LineChangeType.NONE) LineChangeType.UPDATED else toField.changeType
                }

                updateLineChangeType(newChangeType)
            }

            newFields[targetIndex - 1] = toTextFieldState

            newFields.removeAt(targetIndex)

//            val newFocusingLineIdx = mutableStateOf(focusingLineIdx)
//            val newSelectedIndices = selectedIndices.toMutableList()
            val sfiRet = selectFieldInternal(
                init_fields = newFields,
                init_selectedIndices = selectedIndices,
//                out_focusingLineIdx = newFocusingLineIdx,
                isMutableFields = true,
                isMutableSelectedIndices = false,
//                init_focusingLineIdx = focusingLineIdx,
                targetIndex = targetIndex - 1
            )

            isContentEdited?.value=true
            editorPageIsContentSnapshoted?.value= false

            val newState = internalCreate(
                fields = sfiRet.fields,
                fieldsId = newId(),
                selectedIndices = sfiRet.selectedIndices,
                isMultipleSelectionMode = isMultipleSelectionMode,
                focusingLineIdx = sfiRet.focusingLineIdx
            )

            onChanged(newState, true, true)
        }
    }

    //第1个参数是行索引；第2个参数是当前行的哪个位置
    //若只传columnStartIndexInclusive，相当于定位到某一行的某一列；若传columnStartIndexInclusive 和 columnEndIndexExclusive，相当于选中某行的某一段文字
    suspend fun selectField(
        targetIndex: Int,
        option: SelectionOption = SelectionOption.NONE,
        columnStartIndexInclusive:Int=0,
        columnEndIndexExclusive:Int=columnStartIndexInclusive,
        requireSelectLine: Boolean=true, // if true, will add targetIndex into selected indices, set false if you don't want to select this line(e.g. when you just want go to line)
        highlightingStartIndex: Int = -1,
        highlightingEndExclusiveIndex: Int = -1,


    ) {

        lock.withLock {
//            val newFields = fields.toMutableList()
//            val newFocusingLineIdx = mutableStateOf(focusingLineIdx)
//            val newSelectedIndices = selectedIndices.toMutableList()
            val sfiRet = selectFieldInternal(
                init_fields = fields,
                init_selectedIndices = selectedIndices,
                isMutableFields = false,
                isMutableSelectedIndices = false,
//                out_focusingLineIdx = newFocusingLineIdx,
//                init_focusingLineIdx = focusingLineIdx,
                targetIndex = targetIndex,
                option = option,
                columnStartIndexInclusive=columnStartIndexInclusive,
                columnEndIndexExclusive=columnEndIndexExclusive,
                requireSelectLine = requireSelectLine,
                highlightingStartIndex = highlightingStartIndex,
                highlightingEndExclusiveIndex = highlightingEndExclusiveIndex,
            )

            val newState = internalCreate(
                fields = sfiRet.fields,
                fieldsId = fieldsId,  //选择某行，fields实际内容未改变，顶多影响光标位置或者选中字段之类的，所以不需要生成新fieldsId
                selectedIndices = sfiRet.selectedIndices,
                isMultipleSelectionMode = isMultipleSelectionMode,
                focusingLineIdx = sfiRet.focusingLineIdx
            )

            onChanged(newState, null, false)
        }
    }

    //第1个参数是行索引；第2个参数是当前行的哪个位置
    suspend fun selectFieldSpan(targetIndex: Int) {
        lock.withLock {
            var sfiRet:SelectFieldInternalRet? = null

            if(selectedIndices.isEmpty()) {  //如果选中列表为空，仅选中当前行
                sfiRet = selectFieldInternal(
                    init_fields = fields,
                    init_selectedIndices = selectedIndices,
                    isMutableFields = false,
                    isMutableSelectedIndices = false,
//                    out_focusingLineIdx = newFocusingLineIdx,
//                    init_focusingLineIdx = focusingLineIdx,
                    targetIndex = targetIndex,
                    forceAdd = true
                )
            }else {  //选中列表不为空，执行区域选择
                val lastSelectedIndex = selectedIndices.last()
                val startIndex = Math.min(targetIndex, lastSelectedIndex)
                val endIndexExclusive = Math.max(targetIndex, lastSelectedIndex) + 1

                if(startIndex >= endIndexExclusive
                    || startIndex<0 || startIndex>fields.lastIndex  // lastIndex is list.size - 1
                    || endIndexExclusive<0 || endIndexExclusive>fields.size
                ) {
                    return
                }

                val newFields = fields.toMutableList()
                val newSelectedIndices = selectedIndices.toMutableList()
                for(i in startIndex..<endIndexExclusive) {
                    sfiRet = selectFieldInternal(
                        init_fields = newFields,
                        init_selectedIndices = newSelectedIndices,
                        isMutableFields = true,
                        isMutableSelectedIndices = true,
//                        out_focusingLineIdx = newFocusingLineIdx,
//                        init_focusingLineIdx = focusingLineIdx,
                        targetIndex = i,
                        forceAdd = true
                    )
                }
            }

            val newState = internalCreate(
                fields = sfiRet?.fields ?: fields,
                fieldsId = fieldsId,
                selectedIndices = sfiRet?.selectedIndices ?: selectedIndices,
                isMultipleSelectionMode = isMultipleSelectionMode,
//                focusingLineIdx = newFocusingLineIdx.value
                focusingLineIdx = sfiRet?.focusingLineIdx ?: focusingLineIdx
            )

            onChanged(newState, null, false)
        }
    }

    suspend fun selectPreviousField() {
        lock.withLock {
            if (isMultipleSelectionMode) return
            val selectedIndex = selectedIndices.firstOrNull() ?: return
            if (selectedIndex == 0) return
            val previousIndex = selectedIndex - 1

            //更新状态
//            val newFields = fields.toMutableList()
//            val newSelectedIndices = selectedIndices.toMutableList()
//            val newFocusingLineIdx = mutableStateOf(focusingLineIdx)
            val sfiRet = selectFieldInternal(
                init_fields = fields,
                init_selectedIndices = selectedIndices,
                isMutableFields = false,
                isMutableSelectedIndices = false,
//                out_focusingLineIdx = newFocusingLineIdx,
//                init_focusingLineIdx = focusingLineIdx,
                targetIndex = previousIndex,
                option = SelectionOption.LAST_POSITION
            )

            val newState = internalCreate(
                fields = sfiRet.fields,
                fieldsId = fieldsId,
                selectedIndices = sfiRet.selectedIndices,
                isMultipleSelectionMode = isMultipleSelectionMode,
//                focusingLineIdx = newFocusingLineIdx.value
                focusingLineIdx = sfiRet.focusingLineIdx
            )

            onChanged(newState, null, false)
        }
    }

    suspend fun selectNextField() {
        lock.withLock {
            if (isMultipleSelectionMode) return
            val selectedIndex = selectedIndices.firstOrNull() ?: return
            if (selectedIndex == fields.lastIndex) return

            val nextIndex = selectedIndex + 1

            //更新状态
//            val newFocusingLineIdx = mutableStateOf(focusingLineIdx)
            val sfiRet = selectFieldInternal(
                init_fields = fields,
                init_selectedIndices = selectedIndices,
//                out_focusingLineIdx = newFocusingLineIdx,
                isMutableFields = false,
                isMutableSelectedIndices = false,
//                init_focusingLineIdx = focusingLineIdx,
                targetIndex = nextIndex,
                option = SelectionOption.FIRST_POSITION
            )

            val newState = internalCreate(
                fields = sfiRet.fields,
                fieldsId = fieldsId,
                selectedIndices = sfiRet.selectedIndices,
                isMultipleSelectionMode = isMultipleSelectionMode,
//                focusingLineIdx = newFocusingLineIdx.value
                focusingLineIdx = sfiRet.focusingLineIdx

            )

            onChanged(newState, null, false)
        }
    }


    private fun getCopyTargetValue(
        targetValue: TextFieldValue,
        selection:TextRange,
        highlightingStartIndex:Int,
        highlightingEndExclusiveIndex:Int,
    ):TextFieldValue {
        val targetTextLen = targetValue.text.length

        // highlight range if require
        //检查开闭索引是否都为有效索引
        return if(isStartInclusiveEndExclusiveRangeValid(start = highlightingStartIndex, endExclusive = highlightingEndExclusiveIndex, size = targetTextLen)) {
            val targetText = targetValue.text
            val before = targetText.substring(0, highlightingStartIndex)
            val highlighting = targetText.substring(highlightingStartIndex, highlightingEndExclusiveIndex)
            val after = targetText.substring(highlightingEndExclusiveIndex)

            targetValue.copy(
                selection = selection,
                annotatedString = buildAnnotatedString {
                    append(before)
                    withStyle(SpanStyle(background = MyStyleKt.Editor.highlightingBgColor)) {
                        append(highlighting)
                    }
                    append(after)
                })

        }else {
            targetValue.copy(selection = selection)
        }
    }

    /**
     * 区域选中有bug，高亮也有bug，就算选中或高亮，后面会自动触发TextField OnValueChange，然后就丢失选中或样式了，不过写代码的时候如果需要选中区域或高亮区域，正常传参就行，这样以后修复bug就不需要再改了
     */
    private fun selectFieldInternal(
        init_fields:List<TextFieldState>,
        init_selectedIndices:List<Int>,
        isMutableFields:Boolean,
        isMutableSelectedIndices:Boolean,
//        init_focusingLineIdx:Int?,//废弃

        targetIndex: Int,
        option: SelectionOption = SelectionOption.NONE,
        forceAdd:Boolean=false,  // 为true：若没添加则添加，添加了则什么都不做；为false：已添加则移除，未添加则添加
        columnStartIndexInclusive:Int=0,
        columnEndIndexExclusive:Int=columnStartIndexInclusive,
        requireSelectLine:Boolean = true,

        // -1 或其他无效索引，就是啥都不高亮，或者两个值相等，也是啥都不高亮，或者start大于end，也不高亮，总之就是无效左闭右开区间就不高亮
        highlightingStartIndex: Int = -1,
        highlightingEndExclusiveIndex: Int = -1,
    ):SelectFieldInternalRet {
        //后面会根据需要决定是否创建拷贝
        var ret_fields = init_fields
        var ret_selectedIndices = init_selectedIndices
        var ret_focusingLineIdx = targetIndex

        // avoid mistake using
        val out_fileds = Unit
        val out_selectedIndices = Unit
        val init_focusingLineIdx = Unit

        targetIndexValidOrThrow(targetIndex, ret_fields.size)

        val target = ret_fields[targetIndex]

        val selection = when (option) {
            SelectionOption.CUSTOM -> {
                TextRange(columnStartIndexInclusive, columnEndIndexExclusive)
            }
            SelectionOption.NONE -> target.value.selection
            SelectionOption.FIRST_POSITION -> TextRange.Zero
            SelectionOption.LAST_POSITION -> {
                TextRange(target.value.text.length)
            }
        }


        val requireHighlighting = isStartInclusiveEndExclusiveRangeValid(start = highlightingStartIndex, endExclusive = highlightingEndExclusiveIndex, size = target.value.text.length)
        val deHighlightingNonTarget = requireHighlighting
//        val requireLossFocus = requireHighlighting  // 高亮关键字不聚焦是为了避免有的语言（例如英语）输入法，明明没编辑，也会触发onValueChange导致字段被更新，然后高亮闪一下消失，但不聚焦又不会弹出键盘和定位光标，用起来不方便，所以取消，如果想避免高亮关键字闪一下就消失，可开只读模式再搜索
        val requireLossFocus = false  //定位光标到关键字后面，英语可能会闪烁一下就消失，具体应该取决于输入法是否自动读写光标范围内的输入

        //如果请求取消高亮非当前行，把非当前行的样式都更新下
        val unFocusNonTarget = { mutableFields:MutableList<TextFieldState> ->
            val ret_fields = Unit  // avoid mistake using

            for(i in mutableFields.indices) {
                if(i != targetIndex) {
                    mutableFields[i] = mutableFields[i].let {
                        //重新拷贝下text以取消高亮；重新拷贝下selection以取消选中。

                        // （因为这里的选中范围并非像ide那样指示关键字被匹配到了，
                        // 这里出现多个选中范围，仅仅是因为切换到下个匹配位置后没清上个而出现的“残留”，
                        // 若不清，会对用户造成困扰，感觉上会像蓝色选中的字是匹配关键字但非当前聚焦，
                        // 黄色高亮的是选中关键字且是当前聚焦条目，但实际并非如此）
                        it.copy(value = it.value.copy(selection = TextRange.Zero, text = it.value.text))
                    }
                }
            }

        }


        if(isMultipleSelectionMode && requireSelectLine) {  //多选模式，添加选中行列表或从列表移除
            ret_fields = if(isMutableFields) (ret_fields as MutableList) else ret_fields.toMutableList()
            ret_selectedIndices = if(isMutableSelectedIndices) (ret_selectedIndices as MutableList) else ret_selectedIndices.toMutableList()

            if(forceAdd) {  //强制添加
                val notSelected = ret_fields[targetIndex].isSelected.not()
                val notInSelectedIndices = ret_selectedIndices.contains(targetIndex).not()
                //未添加则添加，否则什么都不做

                if(notSelected || requireHighlighting) {
                    ret_fields[targetIndex] = target.copy(
                        isSelected = true,  //重点
                        value = getCopyTargetValue(
                            targetValue = target.value,
                            selection = selection,
                            highlightingStartIndex = highlightingStartIndex,
                            highlightingEndExclusiveIndex = highlightingEndExclusiveIndex
                        )
                    )
                }

                if(notInSelectedIndices) {
                    ret_selectedIndices.add(targetIndex)
                }

            }else {  //切换添加和移除
                val isSelected = !ret_fields[targetIndex].isSelected
                ret_fields[targetIndex] = target.copy(
                    isSelected = isSelected,  //重点
                    value = getCopyTargetValue(
                        targetValue = target.value,
                        selection = selection,
                        highlightingStartIndex = highlightingStartIndex,
                        highlightingEndExclusiveIndex = highlightingEndExclusiveIndex
                    )
                )

                if (isSelected) {
                    if(ret_selectedIndices.contains(targetIndex).not()) {
                        ret_selectedIndices.add(targetIndex)
                    }
                } else {
                    ret_selectedIndices.remove(targetIndex)
                }
            }

            if(deHighlightingNonTarget) {
                unFocusNonTarget(ret_fields)
            }

        }else{  //非多选模式，定位光标到对应行，并选中行，或者定位到同一行但选中不同范围，原本是想用来高亮查找的关键字的，但有点毛病，目前实现成仅将光标定位到关键字前面，但无法选中范围，不过我忘了现在定位到关键字前面是不是依靠的这段代码了
            //更新行选中范围并focus行
            val selectionRangeChanged = selection != target.value.selection
            if(selectionRangeChanged || requireHighlighting) {
                ret_fields = if(isMutableFields) (ret_fields as MutableList) else ret_fields.toMutableList()

                //这里不要判断 selection != target.value.selection ，因为即使选择范围没变，也有可能请求高亮关键字，还是需要更新targetIndex对应的值
                ret_fields[targetIndex] = target.copy(
                    value = getCopyTargetValue(
                        targetValue = target.value,
                        selection = selection,
                        highlightingStartIndex = highlightingStartIndex,
                        highlightingEndExclusiveIndex = highlightingEndExclusiveIndex
                ))

                if(deHighlightingNonTarget) {
                    unFocusNonTarget(ret_fields)
                }
            }

            ret_focusingLineIdx = targetIndex

        }



        return SelectFieldInternalRet(
            fields = ret_fields,
            selectedIndices = ret_selectedIndices,
            focusingLineIdx = if(requireLossFocus) null else ret_focusingLineIdx

            //如果不传null，会自动弹键盘，会破坏请求高亮时设置的颜色，高亮颜色在弹出键盘后，闪下就没了
//            focusingLineIdx = ret_focusingLineIdx
        )
    }

    private fun splitTextsByNL(text: String): List<TextFieldValue> {
        return text.lines().mapIndexed { index, text ->
            if (index == 0) {  //第一行，光标在换行的位置
                TextFieldValue(text, TextRange(text.length))
            } else {  //后续行，光标在开头
                TextFieldValue(text)
            }
        }
    }

    /**
     * 本函数可能会很费时间，所以加了suspend
     */
    suspend fun getKeywordCount(keyword: String): Int {
        val f = fields
        var count = 0

        f.forEach {
            val text = it.value.text
            var startIndex=0

            while (true) {
                //text为空字符串或索引加过头都会在这返回
                if(!isGoodIndexForStr(startIndex, text)) {
                    break
                }

                //执行到这，索引一定good，所以可放心indexOf
                val indexOf = text.indexOf(string=keyword, startIndex=startIndex, ignoreCase=true)

                if(indexOf == -1) {  //未找到
                    break
                }else {  //如果找到了，更新计数，更新下次查找起点
//                    println("line=${idx+1}, column=$indexOf")  //debug

                    count++
                    startIndex = indexOf+keyword.length
                }
            }
        }

        return count
    }

    /**
     * @return Pair(chars count, lines count)
     */
    fun getCharsAndLinesCount(): Pair<Int, Int> {
        val f= fields
        val lines = f.size

        var chars = 0
        f.forEach { chars+=it.value.text.length }

        return Pair(chars, lines)
    }

    /**
     * 从startIndex开始根据FindDirection查找，找到predicate返回true的行后，返回其索引和内容
     */
    fun indexAndValueOf(startIndex:Int, direction: FindDirection, predicate: (text:String) -> Boolean, includeStartIndex:Boolean): Pair<Int, String> {
        val list = fields
        var retPair = Pair(-1, "")

        try {
            val range = if(direction== FindDirection.UP) {
                val endIndex = if(includeStartIndex) startIndex else (startIndex-1)
                if(!isGoodIndexForList(endIndex, list)) {
                    throw RuntimeException("bad index range")
                }

                (0..endIndex).reversed()
            }else {
                val tempStartIndex =if(includeStartIndex) startIndex else (startIndex+1)
                if(!isGoodIndexForList(tempStartIndex, list)) {
                    throw RuntimeException("bad index range")
                }

                tempStartIndex..list.lastIndex
            }

            for(i in range) {
                val item = list[i]
                val text = item.value.text
                if(predicate(text)) {
                    retPair = Pair(i, text)
                    break
                }
            }

        }catch (_:Exception) {

        }

        return retPair

    }

    suspend fun deleteLineByIndices(indices:List<Int>) {
        if(indices.isEmpty()) {
            return
        }

        lock.withLock {
            val newList = fields.filterIndexed {index, _ ->
                !indices.contains(index)
            }

            val newFields = mutableListOf<TextFieldState>()
            newFields.addAll(newList)

            isContentEdited?.value=true
            editorPageIsContentSnapshoted?.value= false

            val newState = internalCreate(
                fields = newFields,
                fieldsId = newId(),
                selectedIndices = selectedIndices,
                isMultipleSelectionMode = isMultipleSelectionMode,
                focusingLineIdx = focusingLineIdx
            )

            onChanged(newState, true, true)
        }
    }


    /**
     * 注：若 targetIndex 为无效索引，初始不会选择任何行
     */
    suspend fun createMultipleSelectionModeState(targetIndex:Int) {
        lock.withLock {
            //用14431行的近2mb文件简单测试了下，性能还行
            //进入选择模式，数据不变，只是 MultipleSelectionMode 设为true且对应行的isSelected设为true

            //必须创建拷贝，只要调用synsState的地方都必须创建拷贝，不然会和源list冲突
            val newFields = fields.toMutableList()
            val newSelectedIndices = mutableListOf<Int>()
            //若索引有效，选中对应行
            if(targetIndex >= 0 && targetIndex < newFields.size) {
                newFields[targetIndex] = newFields[targetIndex].copy(isSelected = true)
                newSelectedIndices.add(targetIndex)
            }

            val newState = internalCreate(
                fieldsId = fieldsId,  //这个似乎是用来判断是否需要将当前状态入撤销栈的，如果文件内容不变，不用更新此字段
                fields = newFields,  //把当前点击而开启选择行模式的那行的选中状态设为真了
                selectedIndices = newSelectedIndices,  //默认选中的索引包含当前选中行即可，因为肯定是点击某一行开启选中模式的，所以只会有一个索引
                isMultipleSelectionMode = true,
                focusingLineIdx = targetIndex

            )

            onChanged(newState, null, false)
        }

    }


//    suspend fun createCopiedState() {
//        lock.withLock {
//            val newState = create(
//                fieldsId = fieldsId,
//                fields = fields.toList(),
//                selectedIndices = selectedIndices,
//                isMultipleSelectionMode = isMultipleSelectionMode,  //拷贝和删除不要退出选择模式(准确来说是不要改变是否处于选择模式，若以后以非多选模式创建CopiedState，也不会自动进入选择模式)，这样用户可间接实现剪切功能，因为选择很多行有时候很废力，所以除非用户明确按取消，否则不要自动解除选择模式
//                focusingLineIdx = focusingLineIdx
//
//            )
//
//            onChanged(newState, null, false)
//        }
//
//    }


    suspend fun createSelectAllState() {
        lock.withLock {
            val initialCapacity = fields.size
            val selectedIndexList = ArrayList<Int>(initialCapacity)
            val selectedFieldList =  ArrayList<TextFieldState>(initialCapacity)  // param is `initialCapacity`
            for((idx, f) in fields.withIndex()) {
                selectedIndexList.add(idx)
                //这个id要不要重新生成？我忘了这个id是指示内容是否改变还是只要任何字段变了就换新id了，不过全选能正常工作，所以这代码就这样吧，不改了
                selectedFieldList.add(f.copy(isSelected = true))
            }
            //我不太确定 data类的copy是深还是浅，但我可以确定这里不需要深拷贝，所以用下面创建浅拷贝的方法创建对象
            val newState = internalCreate(
                fieldsId = fieldsId,
                fields = selectedFieldList,
                selectedIndices = selectedIndexList,
                isMultipleSelectionMode = true,
                focusingLineIdx = focusingLineIdx

            )

            onChanged(newState, null, false)
        }
    }


    suspend fun createDeletedState() {
        if(selectedIndices.isEmpty()) {
            return
        }

        lock.withLock {
            //把没选中的行取出来，作为新的文件内容
            val newFields = fields.filterIndexed { index, _ ->
                !selectedIndices.contains(index)
            }

            //如果选中行列表selectedIndices无重复索引且不可能有错误索引的话，
            // 可简单对比已选中行selectedIndices.size和fields.size来判断有无选中所有行，
            // 但我不确定selectedIndices是否会有重复索引和是否会有无效索引，
            // 所以这里暂时不用size判断是否选中了所有行

            //判断是否删除了所有行，如果新的文件内容列表newFields一个元素都没有，则说明全删了
            val isDeletedAll = newFields.isEmpty()

            //如果是删除所有，创建一个空状态；否则创建删除选中行后的状态
            val newState = if(isDeletedAll) {
                create(
                    fieldsId = newId(),
                    text = "",
                    isMultipleSelectionMode = isMultipleSelectionMode,
                    isContentEdited = isContentEdited,
                    editorPageIsContentSnapshoted = editorPageIsContentSnapshoted,
                    onChanged = onChanged,
                    focusingLineIdx = null, //全删了就一行都不用聚焦了，不过其实还会保留一行供输入，想聚焦可以自己点下那一行
                )
            }else {
                //非全删，创建新状态，不要影响选择模式，要不然有的情况自动退选择模式，有的不退，容易让人感到混乱
                internalCreate(
                    fieldsId = newId(),
                    fields = newFields,
                    selectedIndices = emptyList(),
                    isMultipleSelectionMode = isMultipleSelectionMode,  //一般来说此值在这会是true，不过，这里的语义是“不修改是否选择模式”，所以把这个字段传过去比直接设为true要合适
                    focusingLineIdx = focusingLineIdx

                )
            }

            isContentEdited?.value=true
            editorPageIsContentSnapshoted?.value=false

            onChanged(newState, true, true)
        }
    }


    /**
     * 清空选中行的内容
     */
    suspend fun clearSelectedFields(){
        if(selectedIndices.isEmpty()) {
            return
        }

        lock.withLock {
            //清空已选中行
            val newFields = fields.mapIndexed { index, field ->
                if(selectedIndices.contains(index)) {
                    field.copy(value = field.value.copy(text = ""))
                }else {
                    field
                }
            }


            //即使全删了，完全创建新状态，也不要影响选择模式，要不然有的情况自动退选择模式，有的不退，容易让人感到混乱
            val newState = internalCreate(
                fieldsId = newId(),
                fields = newFields,
                selectedIndices = selectedIndices,
                isMultipleSelectionMode = isMultipleSelectionMode,  //一般来说此值在这会是true，不过，这里的语义是“不修改是否选择模式”，所以把这个字段传过去比直接设为true要合适
                focusingLineIdx = focusingLineIdx
            )

            isContentEdited?.value = true
            editorPageIsContentSnapshoted?.value = false

            onChanged(newState, true, true)
        }
    }

    suspend fun quitSelectionMode(keepSelectionModeOn:Boolean = false){
        lock.withLock {
            val newState = internalCreate(
                fieldsId = fieldsId,
                fields = fields.map { it.copy(isSelected = false) },
                selectedIndices = emptyList(),
                isMultipleSelectionMode = keepSelectionModeOn,
                focusingLineIdx = focusingLineIdx

            )

            onChanged(newState, null, false)
        }
    }

    fun clearSelectedItemList(){
        runBlocking { quitSelectionMode(keepSelectionModeOn = true) }
    }

    /**
     * 计算行索引对应的以像素为单位大概滚动位置。
     * 注：只能算个大概，如果有图片之类的，文字可能只占一行，但预览可能占很多行，这时滚动可能会有较大偏差
     */
    fun lineIdxToPx(lineIndex: Int, fontSizeInPx:Float, screenWidthInPx:Float, screenHeightInPx:Float):Float {
        // 好，最简单的情况，直接在开头，不用换算
        if(lineIndex == 0) {
            return  0f
        }

        var count = 0

        val lineHeight = UIHelper.guessLineHeight(fontSizeInPx)
        val (oneLineHowManyChars, luckyOffset) = getOneLineHowManyCharsAndLuckyOffset(lineHeight = lineHeight, screenWidthInPx = screenWidthInPx, screenHeightInPx = screenHeightInPx, indexToPx = true)
        var targetPx = luckyOffset

        val iterator = fields.iterator()
        while(iterator.hasNext()) {
            if(count++ > lineIndex) {
                break
            }

            val it = iterator.next()

            val lineChars = it.value.text.length
            //软换行行数 (soft-wrap lines)
            val softLineCount = (lineChars / oneLineHowManyChars).coerceAtLeast(1)
            //行数 乘 字体大小，粗略得到当前物理行占用多少像素
            targetPx += (softLineCount * lineHeight)
        }

        return targetPx
    }


    /**
     * 计算索引对应的大概行索引。
     * 注：只能算个大概，如果有图片之类的，文字可能只占一行，但预览可能占很多行，这时滚动可能会有较大偏差
     */
    fun pxToLineIdx(targetPx: Int, fontSizeInPx:Float, screenWidthInPx:Float, screenHeightInPx:Float):Int {
        // 好，最简单的情况，直接在开头，不用换算
        if(targetPx == 0) {
            return  0
        }

        val lineHeight = UIHelper.guessLineHeight(fontSizeInPx)
        val (oneLineHowManyChars, luckyOffset) = getOneLineHowManyCharsAndLuckyOffset(lineHeight = lineHeight, screenWidthInPx = screenWidthInPx, screenHeightInPx = screenHeightInPx, indexToPx = false)

        var pos = luckyOffset

        var targetLineIndex = 0
        val iterator = fields.iterator()
        while (iterator.hasNext()) {
            if(pos >= targetPx) {
                break
            }

            targetLineIndex++

            val it = iterator.next()

            val lineChars = it.value.text.length
            //软换行行数 (soft-wrap lines)
            val softLineCount = (lineChars / oneLineHowManyChars).coerceAtLeast(1)
            //行数 乘 字体大小，粗略得到当前物理行占用多少像素
            pos += (softLineCount * lineHeight)
        }

        return targetLineIndex
    }

    private fun getOneLineHowManyCharsAndLuckyOffset(lineHeight: Float, screenWidthInPx: Float, screenHeightInPx: Float, indexToPx:Boolean): Pair<Int, Float> {
        val oneLineHowManyChars = (screenWidthInPx / lineHeight).toInt()
        val luckyOffset = UIHelper.getLuckyOffset(indexToPx = indexToPx, screenWidthInPx = screenWidthInPx, screenHeightInPx = screenHeightInPx)
        return Pair(oneLineHowManyChars, luckyOffset)
    }

    /**
     * 若forceAppend为假：如果当前行为空行，用text替换当前行；否则追加到当前行后面。
     * 若forceAppend为真：强制追加，无论当前行是否为空行。
     */
    suspend fun appendTextToLastSelectedLine(text: String, forceAppend: Boolean = true) {
        //若取消注释这个，复制空行再粘贴会作废，除非在复制那里处理下，ifEmpty返回个空行，
        // 但是不如在这处理，直接忽略这个条件，
        // 然后用空字符串调用.lines()会得到一个空行，就符合复制一个空行粘贴一个空行的需求了，
        // 别的地方也省得改了
//        if(text.isEmpty()) {
//            return
//        }

        //因为要粘贴到选中行后边，所以必须得有选中条目
        val selectedIndices = selectedIndices
        if(selectedIndices.isEmpty()) {
            return
        }

        //selectedIndices里存的是fields的索引，若是无效索引，直接返回
        val lastSelectedIndexOfLine = selectedIndices.last()
        if(lastSelectedIndexOfLine < 0 || lastSelectedIndexOfLine >= fields.size) {
            MyLog.w(TAG, "#appendTextToLastSelectedLine: invalid index `$lastSelectedIndexOfLine` of `_fields`")
            return
        }


        //空字符串会拆分出一个空行，一个"\n"会拆分出两个空行
//        val lines = text.lines()

        //目标若是空行则replace，否则append
        val trueAppendFalseReplace = if(forceAppend) true else fields[lastSelectedIndexOfLine].value.text.isNotEmpty()

        //执行添加，到这才真正修改Editor的数据
        appendOrReplaceFields(targetIndex = lastSelectedIndexOfLine, text = text, trueAppendFalseReplace = trueAppendFalseReplace)

    }

    fun getContentOfLineIndex(lineIndex: Int): String {
        val f = fields
        return if(lineIndex >= 0 && lineIndex < f.size) {
            f[lineIndex].value.text
        }else {
            ""
        }
    }



    /**
     * note: if you want to save lines to file, recommend to use `dumpLines` instead
     */
    fun getAllText(): String {
        val sb = StringBuilder()
        fields.forEach { sb.append(it.value.text).append(lb) }

        //移除遍历时多添加的末尾换行符，然后返回
        return sb.removeSuffix(lb).toString()

        //below code very slow when file over 1MB，主要原因是字符串拼接，次要是隐含多次循环
//        return fields.map { it.value.text }.foldIndexed("") { index, acc, s ->
//            if (index == 0) acc + s else acc + lb + s
//        }
    }

    fun dumpLinesAndGetRet(output: OutputStream, lineBreak:String=lb): Ret<Unit?> {
        try {
            dumpLines(output, lineBreak)
            return Ret.createSuccess(null)
        }catch (e:Exception) {
            return Ret.createError(null, e.localizedMessage ?: "dump lines err", exception = e)
        }
    }

    fun dumpLines(output: OutputStream, lineBreak:String=lb) {
        val fieldsSize = fields.size
        var count = 0
        output.bufferedWriter().use { bw ->
            for(f in fields) {
                if(++count != fieldsSize) {
                    bw.write("${f.value.text}$lineBreak")
                }else {
                    bw.write(f.value.text)
                }
            }
        }
    }

    fun getSelectedText(): String {
        // 把索引排序，然后取出文本，拼接，返回
        val sb = StringBuilder()
        selectedIndices.toSortedSet().forEach { selectedLineIndex->
            doActIfIndexGood(selectedLineIndex, fields) { field ->
                sb.append(field.value.text).append(lb)
            }
        }

        return sb.removeSuffix(lb).toString()

        //废弃：保留末尾多出来的换行符，就是要让它多一个，不然复制多行时粘贴后会定位到最后一行开头，反直觉，要解决这个问题需要改掉整个行处理机制，太麻烦了，所以暂时这样规避下，其实这样倒合理，在粘贴内容到一行的中间部位时，感觉比之前还合理
//        return sb.toString()

    }

    //获取选择行记数（获取选择了多少行）
    fun getSelectedCount():Int{
//        return selectedIndices.toSet().filter{ it>=0 }.size  //toSet()是为了去重，我不确定是否一定没重复，去下保险；filter {it>=0} 是为了避免里面有-1，我记得初始值好像是往selectedIndices里塞个-1。
        return selectedIndices.size  //toSet()是为了去重，我不确定是否一定没重复，去下保险；filter {it>=0} 是为了避免里面有-1，我记得初始值好像是往selectedIndices里塞个-1。
    }

    fun contentIsEmpty(): Boolean {
        return fields.isEmpty() || (fields.size == 1 && fields[0].value.text.isEmpty())
    }

    /**
     * 此函数不比较fields数组，缺点是返回结果并不准确(真或假都不靠谱)，优点是快
     * 用来粗略判断当前状态是否需要入撤销栈（undoStack）
     */
    fun maybeNotEquals(other: TextEditorState):Boolean {
        return this.fieldsId != other.fieldsId || this.isMultipleSelectionMode != other.isMultipleSelectionMode || this.selectedIndices != other.selectedIndices
    }

    private fun internalCreate(
        fields: List<TextFieldState>,
        fieldsId: String,
        selectedIndices: List<Int>,
        isMultipleSelectionMode: Boolean,
        focusingLineIdx: Int?,
    ): TextEditorState {
        return TextEditorState(
            fieldsId= fieldsId,
            fields = fields,
            selectedIndices = selectedIndices,
            isMultipleSelectionMode = isMultipleSelectionMode,
            focusingLineIdx = focusingLineIdx,

            //这几个变量是共享的，一般不用改
            isContentEdited = isContentEdited,
            editorPageIsContentSnapshoted = editorPageIsContentSnapshoted,
            onChanged = onChanged
        )
    }

    companion object {
        fun create(
            text: String,
            fieldsId: String,
            isMultipleSelectionMode:Boolean,
            focusingLineIdx:Int?,

            isContentEdited: MutableState<Boolean>,
            editorPageIsContentSnapshoted:MutableState<Boolean>,
            onChanged: (newState:TextEditorState, trueSaveToUndoFalseRedoNullNoSave:Boolean?, clearRedoStack:Boolean) -> Unit,
        ): TextEditorState {
            return create(
                lines = text.lines(),
                fieldsId= fieldsId,
                isMultipleSelectionMode = isMultipleSelectionMode,
                focusingLineIdx = focusingLineIdx,

                isContentEdited = isContentEdited,
                editorPageIsContentSnapshoted = editorPageIsContentSnapshoted,
                onChanged = onChanged

            )
        }

        fun create(
            lines: List<String>,
            fieldsId: String,
            isMultipleSelectionMode:Boolean,
            focusingLineIdx:Int?,

            isContentEdited: MutableState<Boolean>,
            editorPageIsContentSnapshoted:MutableState<Boolean>,
            onChanged: (newState:TextEditorState, trueSaveToUndoFalseRedoNullNoSave:Boolean?, clearRedoStack:Boolean) -> Unit,
        ): TextEditorState {
            return create(
                fields = createInitTextFieldStates(lines),
                fieldsId= fieldsId,
                selectedIndices = listOf(),
                isMultipleSelectionMode = isMultipleSelectionMode,
                focusingLineIdx = focusingLineIdx,

                isContentEdited = isContentEdited,
                editorPageIsContentSnapshoted = editorPageIsContentSnapshoted,
                onChanged = onChanged

            )
        }

        fun create(
            file: FuckSafFile,
            fieldsId: String,
            isMultipleSelectionMode:Boolean,
            focusingLineIdx:Int?,
            isContentEdited: MutableState<Boolean>,
            editorPageIsContentSnapshoted:MutableState<Boolean>,
            onChanged: (newState:TextEditorState, trueSaveToUndoFalseRedoNullNoSave:Boolean?, clearRedoStack:Boolean) -> Unit,
        ): TextEditorState {
            //这里`addNewLineIfFileEmpty`必须传true，以确保和String.lines()行为一致，不然若文件末尾有空行，读取出来会少一行
            return create(
                lines = FsUtils.readLinesFromFile(file, addNewLineIfFileEmpty = true),
                fieldsId= fieldsId,
                isMultipleSelectionMode = isMultipleSelectionMode,
                focusingLineIdx = focusingLineIdx,

                isContentEdited = isContentEdited,
                editorPageIsContentSnapshoted = editorPageIsContentSnapshoted,
                onChanged = onChanged
            )
        }

        fun create(
            fields: List<TextFieldState>,
            fieldsId: String,
            selectedIndices: List<Int>,
            isMultipleSelectionMode: Boolean,
            focusingLineIdx: Int?,
            isContentEdited: MutableState<Boolean>,
            editorPageIsContentSnapshoted:MutableState<Boolean>,
            onChanged: (newState:TextEditorState, trueSaveToUndoFalseRedoNullNoSave:Boolean?, clearRedoStack:Boolean) -> Unit,
        ): TextEditorState {
            return TextEditorState(
                fieldsId= fieldsId,
                fields = fields,
                selectedIndices = selectedIndices,
                isMultipleSelectionMode = isMultipleSelectionMode,
                focusingLineIdx = focusingLineIdx,
                isContentEdited = isContentEdited,
                editorPageIsContentSnapshoted = editorPageIsContentSnapshoted,
                onChanged = onChanged
            )
        }

        fun newId():String {
            return generateRandomString(20)
        }
    }
}


private fun createInitTextFieldStates(list:List<String>): List<TextFieldState> {
    if (list.isEmpty()) return listOf(TextFieldState(isSelected = false))
    return list.mapIndexed { _, s ->
        TextFieldState(
            value = TextFieldValue(s, TextRange.Zero),
            isSelected = false
        )
    }
}


enum class SelectionOption {
    FIRST_POSITION,
    LAST_POSITION,
    NONE,
    CUSTOM, // should provide start and end index when use this value
}

enum class FindDirection {
    UP,
    DOWN
}

// return type of `selectFieldInternal`
private class SelectFieldInternalRet(
    val fields: List<TextFieldState>,
    val selectedIndices: List<Int>,
    val focusingLineIdx:Int?,
)
