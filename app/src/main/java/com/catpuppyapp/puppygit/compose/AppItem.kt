package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.catpuppyapp.puppygit.dto.AppInfo


@Composable
fun AppItem(
    appInfo:AppInfo,
    trailIcons: @Composable ()->Unit,
) {

    Row(
        modifier = Modifier.fillMaxWidth(),

        verticalAlignment = Alignment.CenterVertically,
    ) {

        Image(
            bitmap = appInfo.appIcon.toBitmap().asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .padding(start = 5.dp)
        )

        TwoLineTextsAndIcons(appInfo.appName, appInfo.packageName, trailIcons)


    }

}
