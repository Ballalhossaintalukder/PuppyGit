package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.catpuppyapp.puppygit.style.MyStyleKt

@Composable
fun ClickableText(text:String, onClick:(()->Unit)?) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontWeight = MyStyleKt.TextItem.defaultFontWeight(),
        style = MyStyleKt.ClickableText.getStyle(),
        color = MyStyleKt.ClickableText.color,
        fontSize = 16.sp,
        modifier = if(onClick == null) {
            Modifier
        } else {
            MyStyleKt.ClickableText.modifierNoPadding.clickable { onClick() }
        },
    )
}


@Composable
fun ClickableText(
    text:String,
    maxLines:Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    fontWeight: FontWeight? = null,
    style:TextStyle = MyStyleKt.ClickableText.getStyle(),
    color:Color = MyStyleKt.ClickableText.color,
    fontSize:TextUnit = 16.sp,
    modifier: Modifier
) {
    Text(
        text = text,
        maxLines = maxLines,
        overflow = overflow,
        fontWeight = fontWeight,
        style = style,
        color = color,
        fontSize = fontSize,
        modifier = modifier,
    )
}
