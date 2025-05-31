package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.ui.theme.Theme


@Composable
fun MySelectionContainer(
    modifier: Modifier=Modifier,
    content:@Composable ()->Unit
) {
    val inDarkMode = Theme.inDarkTheme

    CompositionLocalProvider(
        LocalTextSelectionColors provides (if(inDarkMode) MyStyleKt.TextSelectionColor.customTextSelectionColors_darkMode else MyStyleKt.TextSelectionColor.customTextSelectionColors),
    ) {
        //旧版m3，这个东西有bug，如果结束光标超过开始光标，会直接崩溃，但新版1.2.1已解决！
        // 目前用的版本是没问题的，不过好像只能复制，没有翻译之类的选项，可能还是有bug，或者我哪里设置的不对？（不是bug，就是故意的，想要更多选项需自己实现）
        // 20250518更新：用禁用选择先包一下，不然可能有bug，例如页面用选择容器包围，内部弹窗再加选择容器且没禁用选择容器包裹，长按，app崩溃，这应该算bug，目前的解决方案就是用选择容器前先用禁用选择容器包一下
        DisableSelection {
            SelectionContainer(modifier) {
                content()
            }
        }

    }
}
