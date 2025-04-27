package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.utils.AppModel


@Composable
fun AppIcon(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appIcon = remember { AppModel.getAppIcon(context) }
    Image(bitmap = appIcon, contentDescription = stringResource(R.string.app_icon), modifier = modifier)
}
