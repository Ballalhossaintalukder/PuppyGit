package com.catpuppyapp.puppygit.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.style.MyStyleKt


@Composable
fun Modifier.fabBasePadding(
    density: Density = LocalDensity.current,
    direction: LayoutDirection=LocalLayoutDirection.current,
): Modifier {
    return padding(
        UIHelper.getNaviBarsPadding(density, direction).let {
            PaddingValues(
                //底部不用加，因为脚手架替你加上了，也可能是fab组件自己加的，我不知道，反正加上了
                bottom = 0.dp,
                top = it.calculateTopPadding(),
                start = it.calculateLeftPadding(direction),
                end = it.calculateRightPadding(direction),
            )
        }
    )
}


@Composable
fun Modifier.addTopPaddingIfIsFirstLine(index:Int, topPadding:Dp = MyStyleKt.Padding.firstLineTopPaddingValuesInDp):Modifier {
    return if(index == 0) padding(top = topPadding) else this
}

