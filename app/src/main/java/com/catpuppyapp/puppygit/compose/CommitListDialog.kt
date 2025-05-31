package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.catpuppyapp.puppygit.play.pro.R


@Composable
fun CommitListDialog(
    title:String,
    firstLineLabel:String,
    firstLineText:String,
    commitListLabel:String,
    commits:List<String>,
    closeDialog:()->Unit,
){
    ConfirmDialog2(
        title = title,
        requireShowTextCompose = true,
        textCompose = {
            DisableSelection {
                MySelectionContainer {
                    Column {
                        //用 \n 是为了在复制文本的时候包含换行符
                        Row {
                            Text(firstLineLabel+": ", fontWeight = FontWeight.ExtraBold)
                            Text(firstLineText+"\n")
                        }

                        HorizontalDivider()
                        Text("\n${commitListLabel}: \n", fontWeight = FontWeight.ExtraBold)

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
//                                horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            commits.forEach {
                                item { Text(it+"\n") }
                            }
                        }
                    }
                }
            }
        },
        onCancel = closeDialog,
        cancelBtnText = stringResource(R.string.close),
        showOk = false
    ) {}
}
