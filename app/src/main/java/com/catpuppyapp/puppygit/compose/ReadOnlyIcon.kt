package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.play.pro.R

@Composable
fun ReadOnlyIcon() {
    SmallIcon(
        imageVector = Icons.Filled.Lock,
        contentDescription = stringResource(R.string.read_only),
    )
}

@Composable
fun SmallIcon(
    modifier: Modifier = Modifier.size(12.dp).padding(end = 2.dp),
    imageVector: ImageVector,
    contentDescription: String?
) {
    Icon(
        modifier = modifier,
        imageVector = imageVector,
        contentDescription = contentDescription,
    )
}
