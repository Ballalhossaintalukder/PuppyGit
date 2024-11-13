package com.catpuppyapp.puppygit.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.utils.FsUtils
import com.catpuppyapp.puppygit.utils.MyLog

private val TAG = "SystemFolderChooser"

/**
 * A Folder Chooser depend System File Chooser, may not work if system removed internal file picker, in that case, can input path instead
 */
@Composable
fun SystemFolderChooser(
    path:MutableState<String>,
    pathTextFieldLabel:String=stringResource(R.string.path),
    pathTextFieldPlaceHolder:String=stringResource(R.string.eg_storage_emulate_0_repos),
    chosenPathCallback:(realPath:String, uri: Uri?)->Unit = {realPath, uri-> path.value = realPath}
) {



    val chooseDirLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if(uri!=null) {
            val realPath = FsUtils.getRealPathFromUri(uri)
            if(realPath.isNotBlank()) {
                chosenPathCallback(realPath, uri)
            }

            MyLog.d(TAG, "#chooseDirLauncher, uri.path=${uri.path}, realPath=$realPath")
        }
    }


    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextField(
                modifier = Modifier.fillMaxWidth(.8f),
                value = path.value,
                maxLines = 6,
                onValueChange = {
                    path.value=it.trim('\n').trimEnd('/')
                },
                label = {
                    Text(pathTextFieldLabel)
                },
                placeholder = {
                    Text(pathTextFieldPlaceHolder)
                }
            )

            IconButton(
                onClick = {
                    //show folder chooser
                    chooseDirLauncher.launch(null)
                }

            ) {
                Icon(imageVector = Icons.Filled.MoreHoriz, contentDescription = stringResource(R.string.three_dots_icon_for_choose_folder))
            }

        }


        Spacer(Modifier.height(15.dp))
        Text(stringResource(R.string.if_unable_choose_a_path_just_copy_paste_instead), fontWeight = FontWeight.Light)

    }
}
