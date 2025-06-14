package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.catpuppyapp.puppygit.style.MyStyleKt

@Composable
fun DropDownMenuItemText(
    text:String,
    selected:Boolean,

    secondLineText:String = "",
    maxLines: Int = Int.MAX_VALUE,

) {
    ScrollableRow {
        DropDownMenuItemTextColumn(
            text = text,
            selected = selected,
            secondLineText = secondLineText,
            maxLines = maxLines,
        )
    }
}


@Composable
private fun DropDownMenuItemTextColumn(
    text:String,
    selected:Boolean,

    secondLineText:String = "",
    maxLines: Int = Int.MAX_VALUE,

) {
//    val fontColor = if(selected) MyStyleKt.DropDownMenu.selectedItemColor() else Color.Unspecified
//    val fontWeight = if(selected) FontWeight.ExtraBold else null  //注意默认是 null，不是 Normal，实测默认的字体比Normal粗

    Column(
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
//            color = fontColor,
//            fontWeight = fontWeight,
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
        )

        if(secondLineText.isNotBlank()) {
            Text(
                text = secondLineText,
//                color = fontColor,
                overflow = TextOverflow.Ellipsis,
                maxLines = maxLines,

                fontWeight = FontWeight.Light,
                fontSize = MyStyleKt.Title.secondLineFontSize,
            )
        }
    }
}
