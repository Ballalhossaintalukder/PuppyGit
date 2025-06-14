package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.style.MyStyleKt


/**
 * condition为true显示图标和文字，否则显示 else content
 */
@Composable
fun CenterIconButton(
    icon:ImageVector,
    text:String,
    iconDesc:String? = text.ifBlank { null },
    attachContent: @Composable () -> Unit = {},
    condition: Boolean = true,  //显示图标还是其他内容
    elseContent: @Composable ()->Unit = {},  // condition为false显示此内容
    enabled:Boolean = true,
    onClick:(()->Unit)? = null
) {
    if(condition) {
        Column(
            modifier = if(onClick != null) {
                Modifier.clickable(
                    enabled = enabled,
                    //interactionSource和indication的作用是隐藏按下时的背景半透明那个按压效果，很难看，所以隐藏
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onClick()
                }
            }else {
                Modifier
            },

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
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
                    color = MyStyleKt.IconColor.normal,
//                    style = MyStyleKt.ClickableText.getStyle(),
//                    color = MyStyleKt.ClickableText.getColor(),
//                    fontSize = MyStyleKt.TextSize.default
                )
            }

            attachContent()
        }
    }else {
        elseContent()
    }

}
