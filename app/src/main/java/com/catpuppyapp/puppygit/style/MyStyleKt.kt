package com.catpuppyapp.puppygit.style

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catpuppyapp.puppygit.dto.DeviceWidthHeight
import com.catpuppyapp.puppygit.ui.theme.Theme
import com.catpuppyapp.puppygit.utils.UIHelper
import com.catpuppyapp.puppygit.utils.fabBasePadding

object MyStyleKt{
    val defaultHorizontalPadding = 10.dp
    val defaultIconSize = 40.dp


    object TextItem {
        fun defaultFontWeight(): FontWeight? {
            //难看，看不清，恶心！
//            return FontWeight.Light

            // Text 组件默认值
            return null
        }
    }

    object ClickableText{
        @Composable
        fun getStyle() = LocalTextStyle.current;  // 默认
//        fun getStyle() = TextStyle(textDecoration = TextDecoration.Underline);  // 加下划线

        val color = Color(0xFF0096FF)
        val errColor = Color(0xFFFF5733)
        val minClickableSize = 25.dp
//        val modifier = Modifier.padding(start = 1.dp,top=15.dp, bottom = 0.dp, end=1.dp)
        //defaultMinSize为了确保基本的可点击范围，避免分支名字很小点不到的情况发生
        val modifierNoPadding = Modifier.defaultMinSize(minClickableSize)
        val modifier = modifierNoPadding
//        val modifier = Modifier.padding(start = 3.dp,top=0.dp, bottom = 0.dp, end=1.dp).defaultMinSize(minClickableSize)
        val fontSize = 16.sp

        val textAlign = TextAlign.Center
    }

//    object NormalText{
//        val modifier = Modifier.padding(start = 1.dp,top=15.dp, bottom = 0.dp, end=1.dp)
        //defaultMinSize为了确保基本的可点击范围，避免分支名字很小点不到的情况发生
//        val modifier = Modifier.padding(start = 3.dp,top=0.dp, bottom = 0.dp, end=1.dp).defaultMinSize(25.dp)
//        val fontSize = 15.sp
//    }

    object ChangeListItemColor {
        /**
         * item change type color
         */
        val changeTypeAdded = Color(0xFF117C21)
        val changeTypeAdded_darkTheme = Color(0xFF78ab78)
        val changeTypeModified = Color(0xFF2B6FC2)
        val changeTypeModified_darkTheme = Color(0xFF5A9AD9)
        val changeTypeDeleted = Color(0xFFBE4040)
        val changeTypeDeleted_darkTheme = Color(0xFFBE4040)
        val changeTypeConflict = Color(0xFF8A40BE)
        val changeTypeConflict_darkTheme = Color(0xFF8E40BE)

        /**
         * x 废弃，用TextColor里的条目替代了）DiffScreen background color
         */
//        val bg_added = Color(0xFF78ab78)
//        val bg_added_darkTheme = Color(0xFF204820)
//        val bg_deleted = Color(0xFF8A8A8A)
//        val bg_deleted_darkTheme = Color(0xFF4F4F4F)
//        val bg_modified = Color(0xFF5A9AD9)
//        val bg_modified_darkTheme = Color(0xFF183653)
//        val bg_conflict = Color(0xFFBE4040)
//        val bg_conflict_darkTheme = Color(0xFF692525)
//        val conflict = Color(0xFF913FA8)
//        val conflict_darkTheme = Color(0xFF621E75)



        fun getConflictColor(inDarkTheme: Boolean = Theme.inDarkTheme): Color {
            return if(inDarkTheme) changeTypeConflict_darkTheme else changeTypeConflict
        }
    }

    object IconColor {
        val enable = Color(0xFF0F479B)
        val disable = Color.LightGray
        val disable_DarkTheme = Color(0xFF505050)
        val normal = Color(0xFF5F5F5F)
    }

    object TextColor {
        val enable = Color.Unspecified
        val disable = Color.LightGray
        val disable_DarkTheme = Color(0xFF505050)
        val highlighting_green =Color(0xFF1FAB26)

        //Editor font color
        private val lineNum_forEditorInLightTheme = Color.Gray
        private val lineNum_forEditorInDarkTheme = Color(0xFF5D5D5D)

        // 行号背景色
        private val lineNumBg_forEditorInLightTheme = Color(0x65E7E7E7)
        private val lineNumBg_forEditorInDarkTheme = Color(0x1B414141)
//        val lineNum_forEditorInDarkTheme = Color(0xFF535353)

        //DiffContent line number color
        val lineNum_forDiffInLightTheme = Color.Gray
        val lineNum_forDiffInDarkTheme = Color(0xFF4D4D4D)

        // DiffScreen: 有匹配的背景颜色
        val hasMatchedAddedLineBgColorForDiffInLightTheme = Color(0x1B2F9B09)
        val hasMatchedAddedLineBgColorForDiffInDarkTheme = Color(0x12016505)
        val hasMatchedDeletedLineBgColorForDiffInLightTheme = Color(0x1E640000)
        val hasMatchedDeletedLineBgColorForDiffInDarkTheme = Color(0x17910000)

        // DiffScreen: 无匹配的背景颜色
        val addedLineBgColorForDiffInLightTheme = Color(0x45089613)
        val addedLineBgColorForDiffInDarkTheme = Color(0x31009B0B)
        val deletedLineBgColorForDiffInLightTheme = Color(0x3B7E0000)
        val deletedLineBgColorForDiffInDarkTheme = Color(0x489F0303)

        val fontColor = Color.Unspecified
        val darkThemeFontColor = Color.Gray

        fun error() = if(Theme.inDarkTheme) Color(0xFFF53737) else Color.Red.copy(alpha = .8f)
//        fun error() = Color.Red.copy(alpha = .8f)

        fun danger() = error()

        fun lineNumColor(inDarkTheme:Boolean = Theme.inDarkTheme):Color {
            return if(inDarkTheme) lineNum_forEditorInDarkTheme else lineNum_forEditorInLightTheme
        }

        fun lineNumColorForDiff(inDarkTheme:Boolean = Theme.inDarkTheme):Color {
            return if(inDarkTheme) lineNum_forDiffInDarkTheme else lineNum_forDiffInLightTheme
        }

        fun lineNumBgColor(inDarkTheme:Boolean = Theme.inDarkTheme):Color {
            return if(inDarkTheme) lineNumBg_forEditorInDarkTheme else lineNumBg_forEditorInLightTheme
        }
    }

    object TextSize {
        val default = 16.sp
        val lineNumSize = 10.sp
    }

    object Editor {
//        val fontSize = 16.sp
//        val bottomBarHeight = 80.dp  //这是以前你个底栏高度，后来改用和其他页面一样的了 由 BottomBar.height 控制

        //高亮文本的背景颜色
        val highlightingBgColor = Color(0xFFFFEB3B)
    }

    //只能在compose下获取这个颜色
//    bgColor = MaterialTheme.colorScheme.primaryContainer,  //标题栏背景色
//    titleColor = MaterialTheme.colorScheme.primary,  //标题栏文字颜色

    object BottomBar{
        val height=60.dp

        //使用BottomBar的那个页面，需要padding出这个高度，否则列表内容会被BottomBar盖住
        val outsideContentPadding = height+20.dp

        // fab自带点padding，所以不光不需要加，还需要减点，但不能减到负数，不然用作padding值时会报错
//        val fabBottomPadding = height-10.dp
        //后来发现还是不要减高度比较好，不然和底栏距离太近，难受
        val fabBottomPadding = height
    }


    object Fab {

        @Composable
        fun getFabModifierForEditor(isMultipleSelectionMode:Boolean, isPortrait:Boolean):Modifier {
            val naviPadding = UIHelper.getNaviBarsPadding()
            //如果底部有导航栏，脚手架的fab会自动加padding，这时为了避免太高，需要减去navibar高度；否则不需要（不然键盘会盖住fab）
            val kbOffset = naviPadding.calculateBottomPadding().value.toInt().let { if(it == 0) 0 else -it }
            val kbHeight = UIHelper.getSoftkeyboardHeightInDp(offsetInDp = kbOffset)

            //多选模式不会显示键盘，就算显示也无法编辑文本内容，所以不需要ime padding
            return addNavPaddingIfNeed(
                isPortrait,

                //如果显示软键盘，则使用软键盘高度，否则，如果是多选模式，加BottomBar padding，否则不额外加padding
                Modifier.fabBasePadding().then(
                    if(kbHeight.value > 0) {  //显示软键盘
                        Modifier.padding(bottom = kbHeight)
                    }else {  //不显示软键盘
                        if(isMultipleSelectionMode) Modifier.padding(bottom = BottomBar.height) else Modifier
                    }
                )
            )
        }

        // go to top/bottom fab使用的这个modifier
        @Composable
        fun getFabModifier(isPortrait:Boolean, deviceWidthHeight: DeviceWidthHeight):Modifier {
            //貌似Fab自带一点Padding，所以这里直接用BottomBar的高度即可，不需要再额外加padding
            // end 20dp 是为了避免浮动按钮盖住条目的三个点菜单按钮（例如Files页面，每个条目后面都有个3点菜单）
//            return addNavPaddingIfNeed(isPortrait, Modifier.padding(bottom = BottomBar.height, end = (deviceWidthHeight.width * 0.1f).dp))
            // 底部加padding是为了高过多选模式的bottom bar，并不是所有页面都有多选模式的底栏，但懒得判断了，直接加固定高度
            // end 30.dp 是为了避免遮盖列表条目的菜单按钮
            return addNavPaddingIfNeed(isPortrait, Modifier.fabBasePadding().padding(bottom = BottomBar.fabBottomPadding, end = 30.dp))
        }

        //若是横屏，添加导航栏padding，不然浮动按钮会被导航栏盖住
        private fun addNavPaddingIfNeed(isPortrait:Boolean, modifier:Modifier = Modifier):Modifier {
            return if(isPortrait) {
                modifier
            }else {
                //平板不管横屏竖屏，导航键都在底部，所以如果在平板，这个navigationBarsPadding()会有问题，底部很高，右边没差，但期望的是横屏时右边加padding错开导航键，所以用这个判断不准
                //这个不需要了，我自己做了处理，完美兼容平板
//                modifier.navigationBarsPadding()

                  // 这个padding加得少和没加一样，加得多离屏幕边缘太远，索性不加了
//                modifier.padding(end = 5.dp)
                modifier
            }
        }
    }


    object RadioOptions{
        val minHeight=30.dp
        val middleHeight=35.dp
    }

    object Title {
        val lineHeight = 20.dp

        val firstLineFontSize = 18.sp
        val firstLineFontSizeSmall = 15.sp

        //标题大字下面那行小字的字体大小
        val secondLineFontSize = 12.sp

        //可点击title的最小尺寸
        //注：应放到点击函数之后，不然虽然会变宽但多出的部分点击无效
        val clickableTitleMinWidth = 40.dp
//        val clickableTitleMinWidth = 50.dp
    }

    object SettingsItem {

        val itemFontSize = 20.sp
        val itemDescFontSize = 15.sp
        val switcherIconSize = 60.dp
        val selectorWidth = MyStyleKt.DropDownMenu.minWidth.dp

        val itemLeftWidthForSwitcher = .8f
        val itemLeftWidthForSelector = .6f

    }

    object Padding {
        //这个padding是为了使密码框不在最底部，看着顺眼点
        val PageBottom = 50.dp

        // editor和diff页面的首行顶部padding
        val firstLineTopPaddingValuesInDp = 5.dp

    }

    object TextSelectionColor {
        val customTextSelectionColors = TextSelectionColors(
            handleColor = Color(0xff4b6cc6),  //光标拖手颜色
            backgroundColor = Color(0xFF6A86D1),  //选中文本背景颜色
        )
        val customTextSelectionColors_darkMode = TextSelectionColors(
            handleColor = Color(0xFF1F3368),  //光标拖手颜色
            backgroundColor = Color(0xFF23376F),  //选中文本背景颜色
        )

        //隐藏光标拖手（拖柄）。注意：只是透明了，但实际仍存在且可点击和拖动
        val customTextSelectionColors_hideCursorHandle = TextSelectionColors(
            handleColor = Color.Transparent,  //光标拖手颜色
            backgroundColor = Color.Transparent,  //选中文本背景颜色
        )
    }

    object Icon {
        val size = 25.dp
        val modifier = Modifier.size(size)
    }


    object CheckoutBox {
//        val height = 56.dp
        val height = 40.dp

    }


    object TopBar {
        val dropDownMenuTopPaddingSize = 70.dp
    }

    object BottomSheet{
        val skipPartiallyExpanded = true
    }

     object DropDownMenu {
         // unit is dp, but for more flexibility
         // 单位是dp，但为了灵活性，这里就不写dp了，只写数字
         const val minWidth = 160

         @Composable
         fun selectedItemColor() = MaterialTheme.colorScheme.primary;
//         val selectedItemColor = TextColor.highlighting_green
    }

    object ToggleButton {

        fun defaultShape() = RectangleShape;

        @Composable
        fun defaultColors(): IconToggleButtonColors {
            //容器颜色都改成透明
            return IconButtonDefaults.filledTonalIconToggleButtonColors().copy(
                containerColor = Color.Transparent,
                checkedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            )
        }
    }

}
