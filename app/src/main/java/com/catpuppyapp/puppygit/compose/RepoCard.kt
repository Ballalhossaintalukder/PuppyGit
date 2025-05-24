package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.constants.Cons
import com.catpuppyapp.puppygit.constants.PageRequest
import com.catpuppyapp.puppygit.data.entity.RepoEntity
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.screen.functions.goToCommitListScreen
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.ui.theme.Theme
import com.catpuppyapp.puppygit.utils.AppModel
import com.catpuppyapp.puppygit.utils.FsUtils
import com.catpuppyapp.puppygit.utils.Libgit2Helper
import com.catpuppyapp.puppygit.utils.Msg
import com.catpuppyapp.puppygit.utils.UIHelper
import com.catpuppyapp.puppygit.utils.copyAndShowCopied
import com.catpuppyapp.puppygit.utils.dbIntToBool
import com.catpuppyapp.puppygit.utils.doJobThenOffLoading
import com.catpuppyapp.puppygit.utils.getFormatTimeFromSec
import com.catpuppyapp.puppygit.utils.state.CustomStateSaveable
import com.catpuppyapp.puppygit.utils.state.mutableCustomStateOf
import com.github.git24j.core.Repository
import kotlinx.coroutines.delay


/**
 * 注意：除了比较重要的关于仓库状态的那行以外，其他都用尽量 outlined 那种图标，这样一看卡片，注意力就会首先看到状态
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepoCard(
    itemWidth:Float,
    requireFillMaxWidth:Boolean,
    showBottomSheet: MutableState<Boolean>,
    curRepo: CustomStateSaveable<RepoEntity>,
    curRepoIndex: MutableIntState,
    repoDto: RepoEntity,
    repoDtoIndex:Int,
    itemSelected:Boolean,
    titleOnClick:(RepoEntity)->Unit,
    goToFilesPage:(path:String) -> Unit,
    requireBlinkIdx:MutableIntState,
    pageRequest:MutableState<String>,
    isSelectionMode:Boolean,
    onClick: (RepoEntity) -> Unit,
    onLongClick:(RepoEntity)->Unit,
    copyErrMsg: (String) -> Unit,
    requireDelRepo:(RepoEntity)->Unit,
    doCloneSingle:(RepoEntity)->Unit,
    workStatusOnclick:(clickedRepo:RepoEntity, status:Int)->Unit
) {
    val navController = AppModel.navController
    val haptic = LocalHapticFeedback.current
    val activityContext = LocalContext.current

    val inDarkTheme = Theme.inDarkTheme

    val repoNotReady = Libgit2Helper.isRepoStatusNotReady(repoDto)
    val repoErr = Libgit2Helper.isRepoStatusErr(repoDto)

    //如果仓库设了临时状态，说明仓库能正常工作，否则检查仓库状态
    //其实原本没判断临时状态，但是当仓库执行操作时，例如 fetching/pushing，gitRepoState会变成null，从而误认为仓库invalid，因此增加了临时状态的判断
    val repoStatusGood = !repoNotReady && (repoDto.tmpStatus.isNotBlank() || (repoDto.gitRepoState!=null && !repoErr))


    val cardColor = UIHelper.defaultCardColor()
    val highlightColor = remember(inDarkTheme) {if(inDarkTheme) Color(0xFF9D9C9C) else Color(0xFFFFFFFF)}

    val defaultFontWeight = remember { MyStyleKt.TextItem.defaultFontWeight() }

    val clipboardManager = LocalClipboardManager.current
    val viewDialogText = rememberSaveable { mutableStateOf("") }
    val showViewDialog = rememberSaveable { mutableStateOf(false) }
    if(showViewDialog.value) {
        CopyableDialog(
            title = stringResource(id = R.string.error_msg),
            text = viewDialogText.value,
            onCancel = {
                showViewDialog.value=false
            }
        ) { //复制到剪贴板
            showViewDialog.value=false
            clipboardManager.setText(AnnotatedString(viewDialogText.value))
            Msg.requireShow(activityContext.getString(R.string.copied))
        }
    }

    val showErrMsg = { repoId:String, errMsg:String ->
        doJobThenOffLoading {
            //显示弹窗
            viewDialogText.value = errMsg
            showViewDialog.value = true

            //清掉错误信息
            val repoDb = AppModel.dbContainer.repoRepository
            repoDb.updateErrFieldsById(repoId, Cons.dbCommonFalse, "")

            //这里就不刷新仓库了，暂时仍显示错误信息，除非手动刷新页面，这么设计是为了缓解用户偶然点开错误没看完就关了，再想点开发现错误信息已被清的问题
        }
    }

    val setCurRepo = {
        //设置当前仓库（如果不将repo先设置为无效值，可能会导致页面获取旧值，显示过时信息）
        curRepo.value = RepoEntity()  // change state to a new value, if delete this line, may cause page not refresh after changed repo
        curRepo.value = repoDto  // update state to target value

        curRepoIndex.intValue = repoDtoIndex
    }
    val lineHeight = 30.dp

    Column (
//        modifier = Modifier.fillMaxWidth(),
        modifier = if(requireFillMaxWidth) Modifier.fillMaxWidth() else Modifier.width(itemWidth.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        Card(
            //0.9f 占父元素宽度的百分之90
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(0.95F)

                //使卡片按下效果圆角，但和elevation冲突，算了，感觉elevation更有立体感比这个重要，所以禁用这个吧
//                .clip(CardDefaults.shape)  //使按下卡片的半透明效果符合卡片轮廓，不然卡片圆角，按下是尖角，丑陋

                .combinedClickable(
                    //只要仓库就绪就可启用长按菜单，不检查git仓库的state是否null，因为即使仓库为null，也需要长按菜单显示删除按钮，也不检查仓库是否出错，1是因为出错不会使用此组件而是另一个errcard，2是就算使用且可长按也仅显示删除和取消
//                    enabled = !repoNotReady,  //因为改成多选了，所以这个其实无所谓了，执行操作前会先检查仓库是否能执行操作
                    onClick = {
                        onClick(repoDto)
                    },
                    onLongClick = {
                        //震动反馈
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                        setCurRepo()

                        //显示底部菜单
                        onLongClick(repoDto)
                    },
                )
//            .defaultMinSize(minHeight = 100.dp)

            ,
            colors = CardDefaults.cardColors(
                //如果是请求闪烁的索引，闪烁一下
                containerColor = if (requireBlinkIdx.intValue != -1 && requireBlinkIdx.intValue == repoDtoIndex) {
                    //高亮2s后解除
                    doJobThenOffLoading {
                        delay(UIHelper.getHighlightingTimeInMills())  //解除高亮倒计时
                        requireBlinkIdx.intValue = -1  //解除高亮
                    }
                    highlightColor
                } else {
                    cardColor
                }

            ),
//        border = BorderStroke(1.dp, Color.Black),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
        ) {
            RepoTitle(
                haptic = haptic,
                repoDto = repoDto,
                isSelectionMode = isSelectionMode,
                itemSelected = itemSelected,
                titleOnClick = titleOnClick,
                titleOnLongClick = onLongClick
            )

            HorizontalDivider()

            //以下开始区分 正常仓库 和 出错的仓库(一般是克隆出错)
            if (Libgit2Helper.isRepoStatusNoErr(repoDto)) {
                Column(
                    modifier = Modifier.padding(start = 10.dp, top = 4.dp, end = 10.dp, bottom = 10.dp)
                ) {

                    //不等于NONE就显示状态，若状态为null，可能仓库文件夹被删了或改名了，这时提示invalid
                    //20240822 仓库执行fetching/pushing等操作时也会变成null，因此增加了临时状态的判断，仅当临时状态为空时，gitRepoState才可信，否则若设了临时状态，就当作仓库没问题（因为正在执行某个操作，所以肯定没损坏）
                    if(!repoNotReady && repoDto.gitRepoState != Repository.StateT.NONE && repoDto.tmpStatus.isBlank()) {  //没执行任何操作，状态又不为NONE，就代表仓库可能出问题了
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
//                            Text(text = stringResource(R.string.state) + ": ")

                            InLineIcon(
                                icon = Icons.Filled.Info,
                                tooltipText = stringResource(R.string.state)
                            )

                            Text(
                                //如果是detached，显示分支号，否则显示“本地分支:远程分支”
                                text = repoDto.getRepoStateStr(activityContext),  //状态为null显示错误，否则显示状态
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = defaultFontWeight,
                                modifier = MyStyleKt.ClickableText.modifier,

                            )
                        }
                    }

                    if(repoStatusGood) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
//                            Text(text = stringResource(R.string.repo_label_branch) + ": ")

                            InLineIcon(
                                icon = ImageVector.vectorResource(R.drawable.branch),
                                tooltipText = stringResource(R.string.repo_label_branch)
                            )

                            ClickableText (
                                //如果是detached，显示分支号，否则显示“本地分支:远程分支”
                                text = if(repoStatusGood) {if(dbIntToBool(repoDto.isDetached)) repoDto.lastCommitHashShort+"("+ stringResource(R.string.detached)+")" else repoDto.branch+":"+repoDto.upstreamBranch} else "",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = defaultFontWeight,
                                modifier = MyStyleKt.ClickableText.modifier.clickable(enabled = repoStatusGood) {
                                    navController.navigate(Cons.nav_BranchListScreen + "/" + repoDto.id)
                                },
                            )
                        }

                    }


                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
//                        Text(text = stringResource(R.string.repo_label_last_update_time) + ": ")
                        InLineIcon(
                            icon = Icons.Filled.AccessTime,
                            tooltipText = stringResource(R.string.repo_label_last_update_time)
                        )
                        Text(
                            text = getFormatTimeFromSec(repoDto.lastUpdateTime),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = MyStyleKt.ClickableText.modifier,
                            fontWeight = defaultFontWeight


                        )
                    }

                    if(repoStatusGood) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
//                            Text(text = stringResource(R.string.repo_label_last_commit) + ": ")

                            InLineIcon(
                                icon = Icons.Filled.Commit,
                                tooltipText = stringResource(R.string.repo_label_last_commit)
                            )
                            ClickableText (
                                text = if(repoStatusGood) repoDto.lastCommitHashShort ?: "" else "",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = MyStyleKt.ClickableText.modifier.combinedClickable(
                                    enabled = repoStatusGood,
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        copyAndShowCopied(activityContext, clipboardManager, repoDto.lastCommitHash)
                                    }
                                ) {
                                    //打开当前仓库的提交记录页面，话说，那个树形怎么搞？可以先不搞树形，以后再弄
                                    goToCommitListScreen(
                                        repoId = repoDto.id,
                                        fullOid = "",  //这里不需要传分支名，会通过HEAD解析当前分支
                                        shortBranchName = "",
                                        useFullOid = false,
                                        isHEAD = true
                                    )
                                },
                                fontWeight = defaultFontWeight

                            )
                        }
                    }


                    //所有情况都显示status
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
//                        Text(text = stringResource(R.string.repo_label_status) + ": ")

                        InLineIcon(
                            icon = Icons.Filled.Info,
                            tooltipText = stringResource(R.string.repo_label_status)
                        )
                        //如果不写入数据库的临时中间状态 pushing/pulling 之类的 不为空，显示中间状态，否则显示写入数据库的持久状态
                        val tmpStatus = repoDto.tmpStatus
                        if(repoErr || repoNotReady || tmpStatus.isNotBlank() || repoDto.workStatus == Cons.dbRepoWorkStatusUpToDate) {  //不可点击的状态
                            var nullNormalTrueUpToDateFalseError:Boolean? = null
                            val text = if(repoErr || (repoDto.gitRepoState==null && tmpStatus.isBlank())) { nullNormalTrueUpToDateFalseError = false; stringResource(R.string.error) }else tmpStatus.ifBlank { nullNormalTrueUpToDateFalseError = true; stringResource(R.string.repo_status_uptodate) }
                            Text(
                                //若tmpStatus不为空，显示；否则显示up-to-date。注：日后若添加更多状态，就不能这样简单判断了
                                text = text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = MyStyleKt.ClickableText.modifier,
                                fontWeight = defaultFontWeight,
                                //出错，红色；已是最新，绿色；"加载中..."之类的非点击临时状态，默认颜色。
                                color = if(nullNormalTrueUpToDateFalseError == null) Color.Unspecified else if(nullNormalTrueUpToDateFalseError == true) MyStyleKt.TextColor.highlighting_green else MyStyleKt.TextColor.error(),

                            )
                        } else {  //可点击的状态
                            ClickableText (
                                text = (
                                        if (repoDto.workStatus == Cons.dbRepoWorkStatusMerging
                                            || repoDto.workStatus==Cons.dbRepoWorkStatusRebasing
                                            || repoDto.workStatus==Cons.dbRepoWorkStatusCherrypicking
                                        ) {
                                            stringResource(R.string.require_actions)
                                        } else if (repoDto.workStatus == Cons.dbRepoWorkStatusHasConflicts) {
                                            stringResource(R.string.repo_status_has_conflict)
                                        } else if(repoDto.workStatus == Cons.dbRepoWorkStatusNeedCommit) {
                                            stringResource(R.string.repo_status_need_commit)
                                        } else if (repoDto.workStatus == Cons.dbRepoWorkStatusNeedSync) {
                                            stringResource(R.string.repo_status_need_sync)
                                        } else if (repoDto.workStatus == Cons.dbRepoWorkStatusNeedPull) {
                                            stringResource(R.string.repo_status_need_pull)
                                        } else if (repoDto.workStatus == Cons.dbRepoWorkStatusNeedPush) {
                                            stringResource(R.string.repo_status_need_push)
                                        }else if (repoDto.workStatus == Cons.dbRepoWorkStatusNoHEAD) {
                                            //便于用户理解，不提示no head，提示no commit
                                            stringResource(R.string.no_commit)
                                        } else {
                                            ""  // 未克隆仓库可能会抵达这里
                                        }
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = MyStyleKt.ClickableText.modifier.clickable(enabled = repoStatusGood) {
                                    workStatusOnclick(repoDto, repoDto.workStatus)  //让父级页面自己写callback吧，省得传参
                                },
                                fontWeight = defaultFontWeight

                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
//                        Text(text = stringResource(R.string.storage) + ": ")
                        InLineIcon(
                            icon = Icons.Outlined.Folder,
                            tooltipText = stringResource(R.string.storage)
                        )
                        ClickableText (
                            text = FsUtils.getPathWithInternalOrExternalPrefix(fullPath = repoDto.fullSavePath),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = MyStyleKt.ClickableText.modifier.combinedClickable(
                                onLongClick = { // long press will copy path
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    clipboardManager.setText(AnnotatedString(repoDto.fullSavePath))
                                    Msg.requireShow(activityContext.getString(R.string.copied))
                                }
                            ) {  // on click
                                goToFilesPage(repoDto.fullSavePath)
                            },
                            fontWeight = defaultFontWeight

                        )
                    }


                    //未就绪不显示错误条目，因为显示里面也没错误，repo表有个专门的字段存储未就绪仓库条目的错误信息，会在另一个ErrCard显示，与此组件无关
                    if(!repoNotReady) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            //错误信息不用检测仓库状态，因为显示错误信息只需要数据库中有对应条目即可，而正常情况下，如果有有效的错误信息，必然有数据库条目
                            val hasUncheckedErr = repoDto.latestUncheckedErrMsg.isNotBlank()

//                            Text(text = stringResource(R.string.repo_label_error) + ": ")
                            InLineIcon(
                                icon = Icons.Filled.WarningAmber,
                                tooltipText = stringResource(R.string.repo_label_error)
                            )
                            ClickableText (
                                text = if (hasUncheckedErr) repoDto.latestUncheckedErrMsg else stringResource(R.string.repo_err_no_err_or_all_checked),
                                maxLines = 1,
                                color = if (hasUncheckedErr) MyStyleKt.ClickableText.errColor else MyStyleKt.ClickableText.color,
                                fontWeight = defaultFontWeight,
                                modifier = MyStyleKt.ClickableText.modifier.combinedClickable(
                                    onLongClick = {
                                        //只有当错误信息不为空时才显示弹窗
                                        if (hasUncheckedErr) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            // "repo:xxx\n\n err:errinfo"
                                            val errMsg = StringBuilder("${activityContext.getString(R.string.repo)}: ")
                                                .appendLine(repoDto.repoName)
                                                .appendLine()
                                                .append("${activityContext.getString(R.string.error)}: ")
                                                .append(repoDto.latestUncheckedErrMsg)
                                                .toString()

                                            showErrMsg(repoDto.id, errMsg)
                                        }
                                    },
                                    onClick = {
                                        //如果有未检查过的错误信息，字变红色，点击进入二级页面，显示当前仓库的错误日志，右上角有“清除所有记录”的按钮。
                                        //如果不存在错误或者已经点过查看错误信息的按钮，就普通颜色，提示没错误或都已经检查过。
                                        //错误信息包含时间戳和简短的描述。

                                        //TODO 考虑下是从错误页面返回时更新仓库的 hasUncheckedErr和所有错误的isChecked字段，还是在进入错误页面时更新？哪个比较合适？
//                        if(repoDto.hasUncheckedErr) {  //在存在错误的情况下点了这个按钮，就当作已经检查过错误，把有存在未检错误设为false
//                            //TODO State有问题，改这个变量，不会触发重新渲染，得想办法解决一下
//                            // 得拿到仓库list集合，和当前元素的索引，然后修改集合里的数据，页面就会更新了，以后再改吧
//                            repoDto.hasUncheckedErr = false;
//                            //TODO 更新数据库
//                        }

                                        navController.navigate(Cons.nav_ErrorListScreen + "/" + repoDto.id)
                                    }) ,
                            )
                        }
                    }


                    if(repoStatusGood && repoDto.hasOther()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
//                            Text(text = stringResource(R.string.other) + ": ")
                            InLineIcon(
                                icon = Icons.AutoMirrored.Filled.Notes,
                                tooltipText = stringResource(R.string.other)
                            )
                            Text(
                                text = repoDto.getOther(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,

                                // for now the "other text" is short, if become long in future, make clicked show full "other text" in a dialog
                                //目前 other text短，如果以后长到无法在卡片完整显示，实现点击文字在弹窗显示完整other text
//                            style = MyStyleKt.ClickableText.style,
//                            color = MyStyleKt.ClickableText.color,
//                            modifier = MyStyleKt.ClickableText.modifier.clickable {  // on click
//                                setCurRepo()
//                                pageRequest.value = PageRequest.showOther
//                            },

                                fontWeight = defaultFontWeight,
                                modifier = MyStyleKt.ClickableText.modifier,


                            )
                        }
                    }


                    if(repoDto.parentRepoValid) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
//                            Text(text = stringResource(R.string.parent_repo) + ": ")
                            InLineIcon(
                                icon = Icons.Outlined.Home,
                                tooltipText = stringResource(R.string.parent_repo)
                            )
                            ClickableText(
                                text = repoDto.parentRepoName,
                                maxLines = 1,
                                modifier = MyStyleKt.ClickableText.modifier.clickable {  // on click
                                    setCurRepo()
                                    pageRequest.value = PageRequest.goParent
                                },
                                fontWeight = defaultFontWeight

                            )
                        }
                    }


                }
            }else {  // err repo card
                val iconSize = remember {28.dp}
                //按下图标，显示出的半透明阴影的size，通常是icon尺寸+8就行
                val iconPressedSize = remember {36.dp}

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Row(
//                modifier = Modifier.height(min=lineHeight.dp),
                        modifier = Modifier.fillMaxWidth(.9f).combinedClickable(onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            copyErrMsg(repoDto.createErrMsg)
                        }) {  },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.error)+": "+repoDto.createErrMsg,
                            color= MyStyleKt.TextColor.error(),
                            textAlign = TextAlign.Left,
                        )
                    }

//                    Spacer(Modifier.height(10.dp))



                    ScrollableRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InLineIcon(
                            icon = Icons.Filled.Delete,
                            tooltipText = stringResource(R.string.del_repo),
                            iconModifier = Modifier.size(iconSize),
                            pressedCircleSize = iconPressedSize,
                        ) { requireDelRepo(repoDto) }

                        InLineIcon(
                            icon = Icons.Filled.Replay,
                            tooltipText = stringResource(R.string.retry),
                            iconModifier = Modifier.size(iconSize),
                            pressedCircleSize = iconPressedSize,
                        ) { doCloneSingle(repoDto) }

                        InLineIcon(
                            icon = Icons.Filled.EditNote,
                            tooltipText = stringResource(R.string.edit_repo),
                            iconModifier = Modifier.size(iconSize),
                            pressedCircleSize = iconPressedSize,
                        ) { navController.navigate(Cons.nav_CloneScreen+"/"+repoDto.id) }

                    }
                }
            }

        }

    }

}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RepoTitle(
    haptic: HapticFeedback,
    repoDto: RepoEntity,
    isSelectionMode:Boolean,
    itemSelected: Boolean,
    // title的onClick和卡片主体的onClick区别在于卡片的onClick仅在启用选择模式时才可切换选择；title的即使没启用选择模式也可单击选择仓库并启用选择模式。
    titleOnClick: (RepoEntity) -> Unit,
    //title的onLongClick和卡片主体的没区别，都是长按启用选择模式，若选择模式已启用则执行区域选择
    titleOnLongClick:(RepoEntity) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .combinedClickable(onLongClick = {
                //震动反馈
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                //显示底部菜单
                titleOnLongClick(repoDto)
            }) {
                titleOnClick(repoDto)
            }
            .then(
                if (itemSelected) {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(.9F),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RepoCardTitleText(repoDto.repoName)
        }

        if(isSelectionMode) {
            Row(
                modifier = Modifier
                    .size(MyStyleKt.defaultIconSize)
                    .padding(end = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleCheckBox(
                    enabled = itemSelected,
                    modifier = Modifier.size(25.dp),
                )
            }
        }

    }
}

@Preview
@Composable
private fun Preview() {
    val repo = mutableCustomStateOf("test", "test", RepoEntity(
        id="abc", repoName = "test", branch = "main", upstreamBranch = "origin/main", lastCommitHash = "a312345",
    ).apply {
        gitRepoState = Repository.StateT.NONE
        fullSavePath = "test/workdir"
        workStatus = Cons.dbRepoWorkStatusUpToDate
    })
    val context = LocalContext.current
    AppModel.init_forPreview()
    RepoCard(
        itemWidth=392F,
        requireFillMaxWidth=false,
        showBottomSheet=remember {mutableStateOf(false)},
        curRepo= repo,
        curRepoIndex=remember { mutableIntStateOf(0) },
        repoDto=repo.value,
        repoDtoIndex=0,
        itemSelected=false,
        titleOnClick={},
        goToFilesPage={},
        //如果和curRepoIndex的值匹配则会高亮显示当前条目，用来跳转时实现闪烁一下目标条目的效果，但在preview时只会高亮，然后就不灭了，一直维持高亮状态。。。
        requireBlinkIdx=remember { mutableIntStateOf(-1) },
        pageRequest=remember { mutableStateOf("") },
        isSelectionMode=false,
        onClick={},
        onLongClick={},
        copyErrMsg={},
        requireDelRepo={},
        doCloneSingle={},
        workStatusOnclick={p1,p2->},
    )
}
