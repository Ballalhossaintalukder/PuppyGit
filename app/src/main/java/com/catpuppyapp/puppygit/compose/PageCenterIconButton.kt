package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.style.MyStyleKt

@Composable
fun PageCenterIconButton(
    contentPadding: PaddingValues,
    onClick: ()->Unit,
    condition: Boolean = true,  //显示图标还是其他内容
    elseContent: @Composable ()->Unit = {},  // condition为false显示此内容
    content: @Composable () ->Unit,  // condition为true显示此内容
) {
    FullScreenScrollableColumn(contentPadding) {
        if(condition) {
            //interactionSource和indication的作用是隐藏按下时的背景半透明那个按压效果，很难看，所以隐藏
            Column(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onClick()
            },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ){
                content()
            }
        }else {
            elseContent()
        }
    }
}


@Composable
fun PageCenterIconButton(
    contentPadding: PaddingValues,
    onClick: ()->Unit,
    icon:ImageVector,
    iconDesc:String?,
    text:String,
    elseContent: @Composable () -> Unit = {},  //这参数别放最后，避免和另一个重载版本的content搞混
    condition: Boolean = true,
) {
    // condition为true显示图标和文字，否则显示其他内容
    PageCenterIconButton(contentPadding = contentPadding, condition = condition, elseContent = elseContent, onClick = onClick) {
        Row{
            Icon(
                modifier = Modifier.size(50.dp),
                imageVector = icon,
                contentDescription = iconDesc,
                tint = MyStyleKt.IconColor.normal
            )
        }

        Row {
            Text(
                text = text,
                style = MyStyleKt.ClickableText.getStyle(),
                color = MyStyleKt.ClickableText.color,
                fontSize = MyStyleKt.TextSize.default
            )
        }
    }
}
