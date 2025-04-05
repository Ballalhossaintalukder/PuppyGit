package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.ui.theme.Theme
import com.catpuppyapp.puppygit.utils.UIHelper


// private const val TAG = "LoadMore"

@Composable
fun LoadMore(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(30.dp),
    text:String= stringResource(R.string.load_more),
//    loadToEndText:String= stringResource(R.string.load_to_end),
    loadToEndText:String= stringResource(R.string.load_all),
    enableLoadMore:Boolean=true,
    enableAndShowLoadToEnd:Boolean=true,
    loadToEndOnClick:()->Unit={},
    pageSize:MutableState<Int>,
    rememberPageSize:MutableState<Boolean>,
    showSetPageSizeDialog:MutableState<Boolean>,
    pageSizeForDialog:MutableState<String>,
    btnUpsideText:String?=null, // text at upside of buttons, usually show count of items etc...
    onClick:()->Unit
) {
    val inDarkTheme = Theme.inDarkTheme

//    val appContext = AppModel.appContext

    val cardColor = UIHelper.defaultCardColor()

    val buttonHeight = 50



    Column(modifier= Modifier
        .fillMaxWidth()
        .padding(paddingValues)
        .padding(start = 10.dp, end = 10.dp)
        .then(modifier)
    ) {
        if(btnUpsideText!=null) {
            Row (
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ){
                Text(btnUpsideText, fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic, color = UIHelper.getSecondaryFontColor())
            }
        }
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Card(
                //0.9f 占父元素宽度的百分之90
                modifier = Modifier
                    .clickable(enabled = enableLoadMore) {  //如果有更多，则启用点击加载更多，否则禁用
                        onClick()
                    }
                ,
                colors = CardDefaults.cardColors(
                    containerColor = cardColor,
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                )

            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(.85f)
                        .height(buttonHeight.dp)
                    ,

                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = text,
                        color = if(enableLoadMore) MyStyleKt.TextColor.enable else if(inDarkTheme) MyStyleKt.TextColor.disable_DarkTheme else MyStyleKt.TextColor.disable
                    )
                }

            }


            LongPressAbleIconBtn(
                tooltipText = stringResource(R.string.set_page_size),
                icon =  Icons.Filled.Settings,
                iconContentDesc = stringResource(R.string.set_page_size),
//                iconModifier = Modifier.fillMaxWidth(.2f),
//                enabled = currentPage.intValue>1,
            ) {
                pageSizeForDialog.value = ""+pageSize.value
                showSetPageSizeDialog.value = true
            }

        }

        if(enableAndShowLoadToEnd) {
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                //0.9f 占父元素宽度的百分之90
                modifier = Modifier
                    .clickable{  //如果有更多，则启用点击加载更多，否则禁用
                        loadToEndOnClick()
                    }
                ,
                colors = CardDefaults.cardColors(
                    containerColor = cardColor,
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                )

            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight.dp)
                    ,
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = loadToEndText,
                        color = MyStyleKt.TextColor.enable
                    )
                }

            }
        }

        Spacer(modifier = Modifier.height(95.dp))

    }

}
