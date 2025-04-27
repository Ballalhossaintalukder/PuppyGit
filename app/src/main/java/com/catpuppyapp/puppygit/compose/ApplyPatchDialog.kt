package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.data.entity.RepoEntity
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.utils.Libgit2Helper
import com.catpuppyapp.puppygit.utils.doJobThenOffLoading
import com.catpuppyapp.puppygit.utils.state.CustomStateSaveable
import com.github.git24j.core.Repository
import java.io.File



@Composable
fun ApplyPatchDialog(
    showDialog: MutableState<Boolean>,
    selectedRepo:CustomStateSaveable<RepoEntity>,
    checkOnly:MutableState<Boolean>,
    patchFileFullPath:String,
    repoList:List<RepoEntity>,
    onCancel: () -> Unit,
    onErrCallback:suspend (err:Exception, selectedRepoId:String)->Unit,
    onFinallyCallback:()->Unit,
    onOkCallback:()->Unit,
) {

    ConfirmDialog2(
        okBtnEnabled = repoList.isNotEmpty() && selectedRepo.value.id.isNotBlank(),
        title = stringResource(R.string.apply_patch),
        requireShowTextCompose = true,
        textCompose = {
            ScrollableColumn {
                Text(text = stringResource(R.string.select_target_repo)+":")
                Spacer(modifier = Modifier.height(10.dp))

                SingleSelectList(optionsList = repoList,
                    menuItemSelected = {idx, value -> value.id == selectedRepo.value.id},
                    menuItemOnClick = {idx, value -> selectedRepo.value = value},
                    menuItemFormatter = {idx, value -> value?.repoName ?: ""},
                    selectedOptionIndex = null,
                    selectedOptionValue = selectedRepo.value
                )

                Spacer(modifier = Modifier.height(10.dp))

                MyCheckBox(stringResource(R.string.check_only), checkOnly)
                if(checkOnly.value) {
                    DefaultPaddingText(stringResource(R.string.apply_patch_check_note))
                }
                Spacer(modifier = Modifier.height(10.dp))

//
//                MyLazyColumn(
//                    modifier = Modifier.heightIn(max=150.dp),
//                    requireUseParamModifier = true,
//                    contentPadding = PaddingValues(0.dp),
//                    list = repoList.value,
//                    listState = StateUtil.getRememberLazyListState(),
//                    requireForEachWithIndex = true,
//                    requirePaddingAtBottom =false
//                ) {k, it ->
//                    Row(
//                        Modifier
//                            .fillMaxWidth()
//                            .heightIn(min = MyStyleKt.RadioOptions.minHeight)
//
//                            .selectable(
//                                selected = it.id == selectedRepo.value.id,
//                                onClick = {
//                                    //更新选择值
//                                    selectedRepo.value = it
//                                },
//                                role = Role.RadioButton
//                            )
//                            .padding(horizontal = 10.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        RadioButton(
//                            selected = it.id == selectedRepo.value.id,
//                            onClick = null // null recommended for accessibility with screenreaders
//                        )
//                        Text(
//                            text = it.repoName,
//                            style = MaterialTheme.typography.bodyLarge,
//                            modifier = Modifier.padding(start = 10.dp)
//                        )
//                    }
//
//                }

            }
        },
        onCancel = { onCancel() }
    ) {  // onOK

        doJobThenOffLoading {
            try {
                Repository.open(selectedRepo.value.fullSavePath).use { repo ->
                    /*
                     *(
                            inputFile:File,
                            repo:Repository,
                            applyOptions: Apply.Options?=null,
                            location:Apply.LocationT = Apply.LocationT.WORKDIR,  // default same as `git apply`
                            checkWorkdirCleanBeforeApply: Boolean = true,
                            checkIndexCleanBeforeApply: Boolean = false
                        )
                     */
                    val inputFile = File(patchFileFullPath)
                    val ret = Libgit2Helper.applyPatchFromFile(
                        inputFile,
                        repo,
                        checkOnlyDontRealApply = checkOnly.value
                    )

                    if(ret.hasError()) {
                        if(ret.exception!=null) {
                            throw ret.exception!!
                        }else {
                            throw RuntimeException(ret.msg)
                        }
                    }
                }


                onOkCallback()
            }catch (e:Exception){
                onErrCallback(e, selectedRepo.value.id)
            }finally {
                onFinallyCallback()
            }
        }
    }
}
