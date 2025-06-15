package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.style.MyStyleKt

private val minHeight = 40.dp

@Composable
fun TwoLineTextsAndIcons(
    text1:String,
    text2:String,
    trailIconWidth: Dp,
    trailIcons: @Composable BoxScope.(containerModifier: Modifier) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(5.dp)
            .padding(end = 5.dp)
            .fillMaxWidth()
            .heightIn(min = minHeight)
        ,
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(end = 5.dp)
                .padding(end = trailIconWidth)
//                .fillMaxWidth()  // no need fill max width
            ,
            verticalArrangement = Arrangement.Center,
        ) {
            SelectionRow(
                Modifier.horizontalScroll(rememberScrollState())
            ) {
                Text(text = text1, fontSize = MyStyleKt.Title.firstLineFontSizeSmall, fontWeight = FontWeight.Bold)
            }

            if(text2.isNotEmpty()) {
                SelectionRow(
                    Modifier.horizontalScroll(rememberScrollState())
                ) {
                    Text(text = text2, fontSize = MyStyleKt.Title.secondLineFontSize, fontWeight = FontWeight.Light)
                }
            }
        }

        trailIcons(Modifier.align(Alignment.CenterEnd))

    }
}
