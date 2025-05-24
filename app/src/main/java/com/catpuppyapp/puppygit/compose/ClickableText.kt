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
import com.catpuppyapp.puppygit.style.MyStyleKt

@Composable
fun SingleLineClickableText(
    text:String,
    onClick:(()->Unit)?
) {
    ClickableText(
        text = text,
        maxLines = 1,
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
    fontWeight: FontWeight? = MyStyleKt.TextItem.defaultFontWeight(),
    style:TextStyle = MyStyleKt.ClickableText.getStyle(),
    color:Color = MyStyleKt.ClickableText.color,
    fontSize:TextUnit = MyStyleKt.ClickableText.fontSize,
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
