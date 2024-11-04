package com.catpuppyapp.puppygit.screen.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.compose.CardButton
import com.catpuppyapp.puppygit.compose.DiffRow
import com.catpuppyapp.puppygit.constants.Cons
import com.catpuppyapp.puppygit.constants.LineNum
import com.catpuppyapp.puppygit.data.AppContainer
import com.catpuppyapp.puppygit.data.entity.RepoEntity
import com.catpuppyapp.puppygit.dev.FlagFileName
import com.catpuppyapp.puppygit.dev.detailsDiffTestPassed
import com.catpuppyapp.puppygit.dev.proFeatureEnabled
import com.catpuppyapp.puppygit.git.DiffItemSaver
import com.catpuppyapp.puppygit.git.PuppyHunkAndLines
import com.catpuppyapp.puppygit.git.PuppyLine
import com.catpuppyapp.puppygit.git.StatusTypeEntrySaver
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.settings.SettingsUtil
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.utils.Libgit2Helper
import com.catpuppyapp.puppygit.utils.MyLog
import com.catpuppyapp.puppygit.utils.changeStateTriggerRefreshPage
import com.catpuppyapp.puppygit.utils.compare.SimilarCompare
import com.catpuppyapp.puppygit.utils.compare.param.StringCompareParam
import com.catpuppyapp.puppygit.utils.createAndInsertError
import com.catpuppyapp.puppygit.utils.doJobThenOffLoading
import com.catpuppyapp.puppygit.utils.getHumanReadableSizeStr
import com.catpuppyapp.puppygit.utils.replaceStringResList
import com.catpuppyapp.puppygit.utils.state.CustomStateSaveable
import com.catpuppyapp.puppygit.utils.state.mutableCustomStateOf
import com.github.git24j.core.Diff
import com.github.git24j.core.Repository
import kotlinx.coroutines.channels.Channel
import java.io.File

private val TAG = "DiffContent"
private val stateKeyTag = "DiffContent"

@Composable
fun DiffContent(
    repoId: String,
    relativePathUnderRepoDecoded: String,
    fromTo: String,
    changeType: String,  //modification, new, del，之类的只有modification需要diff
    fileSize:Long,  //如果用来判断文件是否过大来决定是否加载的话，上级页面已经判断了，过大根本不会加载此组件，所以这变量可能没用，可以考虑以后显示下文件大小之类的？
    naviUp: () -> Boolean,
    dbContainer: AppContainer,
    contentPadding: PaddingValues,
    treeOid1Str:String,
    treeOid2Str:String,
    needRefresh:MutableState<String>,
    listState: LazyListState,
    curRepo:CustomStateSaveable<RepoEntity>,
    requireBetterMatchingForCompare:MutableState<Boolean>,
    fileFullPath:String,
    isSubmodule:Boolean,
    isDiffToLocal:Boolean,
    diffableItemList:List<StatusTypeEntrySaver>,
    curItemIndex:MutableIntState,
    switchItem:(StatusTypeEntrySaver, index:Int) -> Unit,
    clipboardManager:ClipboardManager,
    loadingOnParent:(String)->Unit,
    loadingOffParent:()->Unit,
) {
    //废弃，改用获取diffItem时动态计算实际需要显示的contentLen总和了
//    val fileSizeOverLimit = isFileSizeOverLimit(fileSize)

//    val scope = rememberCoroutineScope()
    val settings= remember { SettingsUtil.getSettingsSnapshot() }

    // remember for make sure only have one instance bundle with a composable function's one life time
    //用remember是为了确保组件生命周期内只创建一个channel实例
    val loadChannel = remember { mutableStateOf(Channel<Int>())  }
//    val loadChannelLock = Mutex()
    val refreshPageIfIsWorkTreePage={
        if(fromTo == Cons.gitDiffFromIndexToWorktree) {
            changeStateTriggerRefreshPage(needRefresh)
        }
    }

//    val appContext = AppModel.singleInstanceHolder.appContext
//    val inDarkTheme = Theme.inDarkTheme

    val diffItem = mutableCustomStateOf(keyTag = stateKeyTag, keyName = "diffItem", initValue = DiffItemSaver())

    val loading = rememberSaveable { mutableStateOf(true)}
    val submoduleIsDirty = remember { mutableStateOf(false)}
    val errMsgState = remember { mutableStateOf("")}

//    val oldLineAt = stringResource(R.string.old_line_at)
//    val newLineAt = stringResource(R.string.new_line_at)
    val errorStrRes = stringResource(R.string.error)


    val isFileAndExist = remember(isSubmodule, changeType, fileFullPath) {
        derivedStateOf {
            if(isSubmodule || (changeType != Cons.gitStatusNew && changeType != Cons.gitStatusModified)){
                false
            }else{
                val f= File(fileFullPath)
                f.exists() && f.isFile
            }
        }
    }


    //判断是否是支持预览的修改类型
    // 注意：冲突条目不能diff，会提示unmodified！所以支持预览冲突条目没意义，若支持的话，在当前判断条件后追加后面的代码即可: `|| changeType == Cons.gitStatusConflict`
    val isSupportedChangeType = (
            changeType == Cons.gitStatusModified
            || changeType == Cons.gitStatusNew
            || changeType == Cons.gitStatusDeleted
            || changeType == Cons.gitStatusTypechanged  // e.g. submodule folder path change to a file, will show type changed, view this is ok
    )

    val fileChangeTypeIsModified = changeType == Cons.gitStatusModified

    val closeChannelThenSwitchItem = {item:StatusTypeEntrySaver, index:Int ->
        doJobThenOffLoading {
            // send close signal to old channel to abort loading
            try {
                loadChannel.value.close()
            }catch (_:Exception) {
            }

            loadChannel.value = Channel()

            // switch new item
            switchItem(item, index)
        }

        Unit
    }

    //BAD: if not remember, will create instance every re-composing!
//    @SuppressLint("UnrememberedMutableState")
//    val job = mutableStateOf<Job?>(null)

    //点击屏幕开启精细diff相关变量，开始
//    val switchDiffMethodWhenCountToThisValue = 3  //需要连续点击屏幕这个次数才能切换精细diff开关
//    val tapCount = StateUtil.getRememberSaveableState(initValue = 0)
//    val limitInSec = 3  //单位秒，在限定时间内点击才会累加计数
//    val lastSec = StateUtil.getRememberSaveableState(initValue = 0L)  //上次点击时间
    //点击屏幕开启精细diff相关变量，结束


//    val hasError = StateUtil.getRememberSaveableState(initValue = false)
//    val errMsg = StateUtil.getRememberSaveableState(initValue = "")
//    if(hasError.value) {
//        showToast(AppModel.singleInstanceHolder.appContext, errOpenFileFailed+":"+errMsg.value)
//        return
//    }
    val loadingFinishedButHasErr = (loading.value.not() && errMsgState.value.isNotBlank())
    val unsupportedChangeType = !isSupportedChangeType
    val isBinary = diffItem.value.flags.contains(Diff.FlagT.BINARY)
    val fileNoChange = !diffItem.value.isFileModified

    //不支持预览二进制文件、超出限制大小、文件未修改
    if (loadingFinishedButHasErr || unsupportedChangeType || loading.value || isBinary || diffItem.value.isContentSizeOverLimit || fileNoChange) {
        Column(
            modifier = Modifier
                //fillMaxSize 必须在最上面！要不然，文字不会显示在中间！
                .fillMaxSize()
                .padding(contentPadding)
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
            ,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row {
                if(loadingFinishedButHasErr) {
                    Text(text = errMsgState.value, color = MyStyleKt.TextColor.error())
                } else if(unsupportedChangeType){
                    Text(text = stringResource(R.string.unknown_change_type))
                }else if(loading.value) {
                    Text(stringResource(R.string.loading))
                }else if(isBinary) {
                    Text(stringResource(R.string.doesnt_support_view_binary_file))
                }else if(diffItem.value.isContentSizeOverLimit) {
                    Text(text = stringResource(R.string.content_size_over_limit)+"("+ getHumanReadableSizeStr(settings.diff.diffContentSizeMaxLimit) +")")
                }else if(fileNoChange) {
                    if(isSubmodule && submoduleIsDirty.value) {  // submodule no diff for shown, give user a hint
                        Text(stringResource(R.string.submodule_is_dirty_note))
                    }else {
                        Text(stringResource(R.string.the_file_has_not_changed))
                    }
                }
            }


            Spacer(Modifier.height(100.dp))
            NaviButton(diffableItemList = diffableItemList, curItemIndex = curItemIndex, switchItem = closeChannelThenSwitchItem)

            Spacer(Modifier.height(100.dp))
        }
    }else {  //文本类型且没超过大小且文件修改过，正常显示diff信息
        val lastIndex = diffItem.value.hunks.size - 1

//        Column(
//            modifier = Modifier
//                //fillMaxSize 必须在最上面！要不然，文字不会显示在中间！
//                .fillMaxSize()
//                .verticalScroll(listState)
//                .padding(contentPadding)
//
//                //底部padding，把页面顶起来，观感更舒适（我感觉）
//                .padding(bottom = 150.dp),
//
//        ) {
        LazyColumn(modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            state = listState
        ){
                // show a notice make user know submodule has uncommitted changes
            if(submoduleIsDirty.value) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically

                    ) {
                        Text(stringResource(R.string.submodule_is_dirty_note_short), fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic)
                    }
                }
            }

            if(diffItem.value.hunks.isEmpty()) {
                item {
                    Row(modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 100.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically

                    ) {
                        Text(stringResource(R.string.file_is_empty), fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic)
                    }
                }
            }


            //数据结构是一个hunk header N 个行
            diffItem.value.hunks.forEachIndexed { index, hunkAndLines: PuppyHunkAndLines ->
                if(fileChangeTypeIsModified && proFeatureEnabled(detailsDiffTestPassed)) {  //增量diff
                    if(!settings.diff.groupDiffContentByLineNum || FlagFileName.flagFileExist(FlagFileName.disableGroupDiffContentByLineNum)) {
                        //this method need use some caches, clear them before iterate lines
                        //这种方式需要使用缓存，每次遍历lines前都需要先清下缓存，否则可能多显示或少显示某些行
                        hunkAndLines.clearCachesForShown()

                        hunkAndLines.lines.forEach printLine@{ line: PuppyLine ->
                            //若非 新增行、删除行、上下文 ，不显示
                            if (line.originType != Diff.Line.OriginType.ADDITION.toString()
                                && line.originType != Diff.Line.OriginType.DELETION.toString()
                                && line.originType != Diff.Line.OriginType.CONTEXT.toString()
                            ) {
                                return@printLine
                            }

                            val mergeAddDelLineResult = hunkAndLines.needShowAddOrDelLineAsContext(line.lineNum)
                            // ignore which lines has ADD and DEL 2 types, but only difference at has '\n' or has not
                            if(mergeAddDelLineResult.needShowAsContext) {
                                // 合并只有末尾是否有换行符的添加和删除行为context等于显示一个没修改的行，既然没修改，直接不显示不就行了？反正本来就自带context，顶多差一行
                                if(mergeAddDelLineResult.data!=null) {  // now showed this line before, show it
                                    item {
                                        DiffRow(
                                            //随便拷贝下del或add（不拷贝只改类型也行但不推荐以免有坏影响）把类型改成context，就行了
                                            line = mergeAddDelLineResult.data,
                                            fileFullPath=fileFullPath,
                                            isFileAndExist = isFileAndExist.value,
                                            clipboardManager=clipboardManager,
                                            loadingOn=loadingOnParent,
                                            loadingOff=loadingOffParent,
                                            refreshPageIfIsWorkTree=refreshPageIfIsWorkTreePage,
                                            repoId=repoId,
                                        )

                                    }
                                }

                                return@printLine
                            }


                            if(line.originType == Diff.Line.OriginType.CONTEXT.toString()) {
                                item {
                                    DiffRow(
                                        line = line,
                                        fileFullPath=fileFullPath,
                                        isFileAndExist = isFileAndExist.value,
                                        clipboardManager=clipboardManager,
                                        loadingOn=loadingOnParent,
                                        loadingOff=loadingOffParent,
                                        refreshPageIfIsWorkTree=refreshPageIfIsWorkTreePage,
                                        repoId=repoId,
                                    )
                                }
                            }else {  // add or del
                                val modifyResult = hunkAndLines.getModifyResult(line.lineNum, requireBetterMatchingForCompare.value)
                                if(modifyResult == null || !modifyResult.matched) {
                                    item {
                                        DiffRow(
                                            line = line,
                                            fileFullPath=fileFullPath,
                                            isFileAndExist = isFileAndExist.value,
                                            clipboardManager=clipboardManager,
                                            loadingOn=loadingOnParent,
                                            loadingOff=loadingOffParent,
                                            refreshPageIfIsWorkTree=refreshPageIfIsWorkTreePage,
                                            repoId=repoId,
                                        )
                                    }
                                }else{  // matched
                                    item {
                                        DiffRow(
                                            line = line,
                                            fileFullPath=fileFullPath,
                                            stringPartList = if(line.originType == Diff.Line.OriginType.ADDITION.toString()) modifyResult.add else modifyResult.del,
                                            isFileAndExist = isFileAndExist.value,
                                            clipboardManager=clipboardManager,
                                            loadingOn=loadingOnParent,
                                            loadingOff=loadingOffParent,
                                            refreshPageIfIsWorkTree=refreshPageIfIsWorkTreePage,
                                            repoId=repoId,
                                        )
                                    }
                                }
                            }

                        }
                    }else {  // grouped lines by line num

                        hunkAndLines.groupedLines.forEach printLine@{ (_lineNum:Int, lines:HashMap<String, PuppyLine>) ->
                            //若非 新增行、删除行、上下文 ，不显示
                            if (!(lines.contains(Diff.Line.OriginType.ADDITION.toString())
                                  || lines.contains(Diff.Line.OriginType.DELETION.toString())
                                  || lines.contains(Diff.Line.OriginType.CONTEXT.toString())
                                 )
                            ) {
                                return@printLine
                            }

                            val add = lines.get(Diff.Line.OriginType.ADDITION.toString())
                            val del = lines.get(Diff.Line.OriginType.DELETION.toString())
                            val context = lines.get(Diff.Line.OriginType.CONTEXT.toString())

                            //若 context del add同时存在，打印顺序为 context/del/add，不过不太可能3个同时存在，顶多两个同时存在

                            if(context!=null) {
                                item {
                                    //打印context
                                    DiffRow(
                                        line = context,
                                        fileFullPath=fileFullPath,
                                        isFileAndExist = isFileAndExist.value,
                                        clipboardManager=clipboardManager,
                                        loadingOn=loadingOnParent,
                                        loadingOff=loadingOffParent,
                                        refreshPageIfIsWorkTree=refreshPageIfIsWorkTreePage,
                                        repoId=repoId,
                                    )
                                }
                            }

                            if(add!=null && del!=null) {  //同样行号，同时存在删除和新增，执行增量diff
                                //解决：两行除了末尾换行符没任何区别的情况仍显示diff的bug（有红有绿但没区别，令人迷惑）
                                if(add.content.removeSuffix("\n").equals(del.content.removeSuffix("\n"))){
                                    item {
                                        DiffRow(
                                            //随便拷贝下del或add（不拷贝只改类型也行但不推荐以免有坏影响）把类型改成context，就行了
                                            line = del.copy(originType = Diff.Line.OriginType.CONTEXT.toString()),
                                            fileFullPath=fileFullPath,
                                            isFileAndExist = isFileAndExist.value,
                                            clipboardManager=clipboardManager,
                                            loadingOn=loadingOnParent,
                                            loadingOff=loadingOffParent,
                                            refreshPageIfIsWorkTree=refreshPageIfIsWorkTreePage,
                                            repoId=repoId,
                                        )

                                    }

                                }else {

                                    val modifyResult2 =
                                        SimilarCompare.INSTANCE.doCompare(
                                            StringCompareParam(add.content),
                                            StringCompareParam(del.content),

                                            //为true则对比更精细，但是，时间复杂度乘积式增加，不开 O(n)， 开了 O(nm)
                                            requireBetterMatching = requireBetterMatchingForCompare.value
                                        )

                                    if(modifyResult2.matched) {
                                        item {
                                            DiffRow(
                                                line = del,
                                                stringPartList = modifyResult2.del,
                                                fileFullPath=fileFullPath,
                                                isFileAndExist = isFileAndExist.value,
                                                clipboardManager=clipboardManager,
                                                loadingOn=loadingOnParent,
                                                loadingOff=loadingOffParent,
                                                refreshPageIfIsWorkTree=refreshPageIfIsWorkTreePage,
                                                repoId=repoId,

                                            )
                                        }

                                        item {
                                            DiffRow(
                                                line = add,
                                                stringPartList = modifyResult2.add,
                                                fileFullPath=fileFullPath,
                                                isFileAndExist = isFileAndExist.value,
                                                clipboardManager=clipboardManager,
                                                loadingOn=loadingOnParent,
                                                loadingOff=loadingOffParent,
                                                refreshPageIfIsWorkTree=refreshPageIfIsWorkTreePage,
                                                repoId=repoId,

                                            )
                                        }

                                    }else {
                                        // 直接使用addContent和delContent即可，不用遍历数组，虽然遍历数组也行，但直接使用字符串性能会稍微好一丢丢
                                        item {
                                            DiffRow(
                                                line = del,
                                                fileFullPath=fileFullPath,
                                                isFileAndExist = isFileAndExist.value,
                                                clipboardManager=clipboardManager,
                                                loadingOn=loadingOnParent,
                                                loadingOff=loadingOffParent,
                                                refreshPageIfIsWorkTree=refreshPageIfIsWorkTreePage,
                                                repoId=repoId,
                                            )
                                        }
                                        item {
                                            DiffRow(
                                                line = add,
                                                fileFullPath=fileFullPath,
                                                isFileAndExist = isFileAndExist.value,
                                                clipboardManager=clipboardManager,
                                                loadingOn=loadingOnParent,
                                                loadingOff=loadingOffParent,
                                                refreshPageIfIsWorkTree=refreshPageIfIsWorkTreePage,
                                                repoId=repoId,
                                            )
                                        }
                                    }
                                }
                            }else{ //有一个为null，不用对比
                                if(del!=null) {
                                    item {
                                        DiffRow(
                                            line = del,
                                            fileFullPath=fileFullPath,
                                            isFileAndExist = isFileAndExist.value,
                                            clipboardManager=clipboardManager,
                                            loadingOn=loadingOnParent,
                                            loadingOff=loadingOffParent,
                                            refreshPageIfIsWorkTree=refreshPageIfIsWorkTreePage,
                                            repoId=repoId,
                                        )
                                    }
                                }
                                if(add!=null) {
                                    item {
                                        DiffRow(
                                            line = add,
                                            fileFullPath=fileFullPath,
                                            isFileAndExist = isFileAndExist.value,
                                            clipboardManager=clipboardManager,
                                            loadingOn=loadingOnParent,
                                            loadingOff=loadingOffParent,
                                            refreshPageIfIsWorkTree=refreshPageIfIsWorkTreePage,
                                            repoId=repoId,
                                        )
                                    }
                                }
                            }
                        }

                    }



                }else { //普通预览，非pro或关闭细节compare时走这里

                    //libgit2 1.7.1 header末尾会加上下一行的内容，有点问题，暂时不显示header了，以后考虑要不要显示
//                if (it.hunk.header.isNotBlank()) {
//                    val color = Color.Gray
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(25.dp)
//                            .background(color)
////                            .clickable {
//////                            showToast(
//////                                appContext,
//////                                "TODO 显示 新增多少行，删除多少行", //TODO 解析hunkheader
//////                                Toast.LENGTH_SHORT
//////                            )
////                            },
//                    ) {
//                        Text(
//                            text = it.hunk.header,
//                            color = Color.Black
//                        )
////                    println("hunkheader:::::"+hal.hunk!!.header)
//                    }
//                }
                    //遍历行
                    hunkAndLines.lines.forEach printLine@{ line: PuppyLine ->
                        //若非 新增行、删除行、上下文 ，不显示
                        if (line.originType == Diff.Line.OriginType.ADDITION.toString()
                            || line.originType == Diff.Line.OriginType.DELETION.toString()
                            || line.originType == Diff.Line.OriginType.CONTEXT.toString()
                        ) {
                            item {
                                DiffRow(
                                    line = line,
                                    fileFullPath=fileFullPath,
                                    isFileAndExist = isFileAndExist.value,
                                    clipboardManager=clipboardManager,
                                    loadingOn=loadingOnParent,
                                    loadingOff=loadingOffParent,
                                    refreshPageIfIsWorkTree=refreshPageIfIsWorkTreePage,
                                    repoId=repoId,
                                )
                            }
                        }
                    }
                }


                //EOF_NL only appear at last hunk, so better check index avoid non-sense iterate
                if(index == lastIndex) {
                    // if delete EOFNL or add EOFNL , show it
                    val indexOfEOFNL = hunkAndLines.lines.indexOfFirst { it.originType ==  Diff.Line.OriginType.ADD_EOFNL.toString() || it.originType ==  Diff.Line.OriginType.DEL_EOFNL.toString()}
                    if(indexOfEOFNL != -1) {  // found originType EOFNL
                        val eofLine = hunkAndLines.lines.get(indexOfEOFNL)
                        item {
                            DiffRow(
                                line = LineNum.EOF.transLineToEofLine(eofLine, add = eofLine.originType ==  Diff.Line.OriginType.ADD_EOFNL.toString()),
                                fileFullPath=fileFullPath,
                                isFileAndExist = isFileAndExist.value,
                                clipboardManager=clipboardManager,
                                loadingOn=loadingOnParent,
                                loadingOff=loadingOffParent,
                                refreshPageIfIsWorkTree=refreshPageIfIsWorkTreePage,
                                repoId=repoId,
                            )
                        }
                    }
                }

                item {
                    //每个hunk之间显示个分割线
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 30.dp),
                        thickness = 3.dp
                    )
                }
            }

            item {
                Spacer(Modifier.height(50.dp))
                NaviButton(diffableItemList = diffableItemList, curItemIndex = curItemIndex, switchItem = closeChannelThenSwitchItem)
                Spacer(Modifier.height(100.dp))
            }

        }
    }



    LaunchedEffect(needRefresh.value) {
//        if(!fileSizeOverLimit) {  //这里其实没必要，上级页面已经判断了，但我还是不放心，所以在这里再加个判断以防文件过大时误加载这个代码块导致app卡死
            if (repoId.isNotBlank() && relativePathUnderRepoDecoded.isNotBlank()) {
//                MyLog.d(TAG, "#LauncedEffect: job==null: ${job.value == null}")
//                if(job.value!=null) {
//                    MyLog.d(TAG, "#LauncedEffect: job is not null, will cancel it")
//                    try {
//                        job.value?.cancel()
//                    }catch (e:Exception) {
//                        MyLog.e(TAG, "#LauncedEffect: cancel job err: ${e.localizedMessage}")
//                    }
//                }

                //      设置页面loading为true
                //      从数据库异步查询repo数据，调用diff方法获得diff内容，然后使用diff内容更新页面state
                //      最后设置页面loading 为false
                doJobThenOffLoading launch@{
                    val channelForThisJob = loadChannel.value

                    try {
                        // init
                        submoduleIsDirty.value = false
                        errMsgState.value = ""
                        loading.value=true


                        //从数据库查询repo，记得用会自动调用close()的use代码块
                        val repoDb = dbContainer.repoRepository
                        val repoFromDb = repoDb.getById(repoId)

                        if(channelForThisJob.tryReceive().isClosed) {
                            return@launch
                        }

                        repoFromDb?:return@launch

                        curRepo.value = repoFromDb

                        Repository.open(repoFromDb.fullSavePath).use { repo->
                            if(fromTo == Cons.gitDiffFromTreeToTree){  //从提交列表点击提交进入
                                val diffItemSaver = if(Libgit2Helper.CommitUtil.isLocalCommitHash(treeOid1Str) || Libgit2Helper.CommitUtil.isLocalCommitHash(treeOid2Str)) {  // tree to work tree, oid1 or oid2 is local, both local will cause err
                                    val reverse = Libgit2Helper.CommitUtil.isLocalCommitHash(treeOid1Str)
//                                    println("1:$treeOid1Str, 2:$treeOid2Str, reverse=$reverse")
                                    val tree1 = Libgit2Helper.resolveTree(repo, if(reverse) treeOid2Str else treeOid1Str)
                                    Libgit2Helper.getSingleDiffItem(
                                        repo,
                                        relativePathUnderRepoDecoded,
                                        fromTo,
                                        tree1,
                                        null,
                                        reverse=reverse,
                                        treeToWorkTree = true,
                                        maxSizeLimit = settings.diff.diffContentSizeMaxLimit,
                                        loadChannel = channelForThisJob,
                                        checkChannelLinesLimit = settings.diff.loadDiffContentCheckAbortSignalLines,
                                        checkChannelSizeLimit = settings.diff.loadDiffContentCheckAbortSignalSize,
                                    )
                                }else { // tree to tree, no local(worktree)
                                    val tree1 = Libgit2Helper.resolveTree(repo, treeOid1Str)
                                    val tree2 = Libgit2Helper.resolveTree(repo, treeOid2Str)
                                    Libgit2Helper.getSingleDiffItem(
                                        repo,
                                        relativePathUnderRepoDecoded,
                                        fromTo,
                                        tree1,
                                        tree2,
                                        maxSizeLimit = settings.diff.diffContentSizeMaxLimit,
                                        loadChannel = channelForThisJob,
                                        checkChannelLinesLimit = settings.diff.loadDiffContentCheckAbortSignalLines,
                                        checkChannelSizeLimit = settings.diff.loadDiffContentCheckAbortSignalSize,
                                    )
                                }

                                if(channelForThisJob.tryReceive().isClosed) {
                                    return@launch
                                }

                                diffItem.value = diffItemSaver
                            }else {  //indexToWorktree or headToIndex
                                val diffItemSaver = Libgit2Helper.getSingleDiffItem(
                                    repo,
                                    relativePathUnderRepoDecoded,
                                    fromTo,
                                    maxSizeLimit = settings.diff.diffContentSizeMaxLimit,
                                    loadChannel = channelForThisJob,
                                    checkChannelLinesLimit = settings.diff.loadDiffContentCheckAbortSignalLines,
                                    checkChannelSizeLimit = settings.diff.loadDiffContentCheckAbortSignalSize,
                                )

                                if(channelForThisJob.tryReceive().isClosed) {
                                    return@launch
                                }

                                diffItem.value = diffItemSaver
                            }


                            // only when compare to work tree need check submodule is or is not dirty. because only non-dirty(clean) submodule can be stage to index, and can be commit to log.
                            if(isDiffToLocal && isSubmodule) {
                                val submdirty = Libgit2Helper.submoduleIsDirty(parentRepo = repo, submoduleName = relativePathUnderRepoDecoded)

                                if(channelForThisJob.tryReceive().isClosed) {
                                    return@launch
                                }

                                submoduleIsDirty.value = submdirty
                            }

                        }

                        loading.value=false
                    }catch (e:Exception) {
                        if(channelForThisJob.tryReceive().isClosed) {
                            return@launch
                        }

                        val errMsg = errorStrRes + ":" + e.localizedMessage

                        errMsgState.value = errMsg
                        loading.value = false

//                        Msg.requireShowLongDuration(errMsg)
                        createAndInsertError(repoId, errMsg)
                        MyLog.e(TAG, "#LaunchedEffect err:"+e.stackTraceToString())
                    }

                }
            }

//        }


    }


    DisposableEffect(Unit) {
        onDispose {
            doJobThenOffLoading {
                loadChannel.value.close()
            }
        }
    }
}

@Composable
private fun NaviButton(
    diffableItemList: List<StatusTypeEntrySaver>,
    curItemIndex: MutableIntState,
    switchItem: (StatusTypeEntrySaver, index: Int) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val size = diffableItemList.size
        val previousIndex = curItemIndex.value - 1
        val nextIndex = curItemIndex.value + 1
        val hasPrevious = previousIndex >= 0 && previousIndex < size
        val hasNext = nextIndex >= 0 && nextIndex < size
        CardButton(
            text =  replaceStringResList(stringResource(R.string.prev_filename), listOf(if(hasPrevious) diffableItemList[previousIndex].fileName else stringResource(R.string.none))),
            enabled = hasPrevious
        ) {
            switchItem(diffableItemList[previousIndex], previousIndex)
        }
        Spacer(Modifier.height(10.dp))
        CardButton(
            text = replaceStringResList(stringResource(R.string.next_filename), listOf(if(hasNext) diffableItemList[nextIndex].fileName else stringResource(R.string.none))),
            enabled = hasNext
        ) {
            switchItem(diffableItemList[nextIndex], nextIndex)
        }

//        Spacer(Modifier.height(150.dp))

    }
}
