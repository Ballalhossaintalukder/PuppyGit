package com.catpuppyapp.puppygit.utils

import androidx.compose.ui.graphics.Color

object PatchUtil {
    private val color_fileHeader_dark = Color(0xAB4D4414)
    private val color_fileHeader = Color(0xADC7BB81)
    private val color_add_dark = Color(0x9E1B5E20)
    private val color_add = Color(0xB481C784)
    private val color_del_dark = Color(0xAD6B3E3E)
    private val color_del = Color(0xA9B04242)
    private val color_hunkHeader_dark = Color(0xB401579B)
    private val color_hunkHeader = Color(0xB281B1C7)

    // 判断一段字符串有没有可能是 patch文件的内容。（也有叫diff文件的，总之就是git 导出的patch）
    fun maybeIsPatchLine(str:String) = isFileHeader(str) || isAdd(str) || isDelete(str) || isHunkHeader(str);

    fun isAdd(str: String) = str.startsWith("+");
    fun isDelete(str: String) = str.startsWith("-") || str.trimEnd() == "\\ No newline at end of file";
    fun isHunkHeader(str: String) = str.startsWith("@@");
    fun isFileHeader(str: String) = str.startsWith("diff --git") || str.startsWith("---") || str.startsWith("+++") || str.startsWith("index") || str.startsWith("new file");

    fun getColorOfLine(str: String, inDarkTheme:Boolean):Color? = if(isFileHeader(str)) {
        if(inDarkTheme) {
            color_fileHeader_dark
        }else {
            color_fileHeader
        }
    } else if(isAdd(str)) {
        if(inDarkTheme) {
            color_add_dark
        }else {
            color_add
        }
    }  else if(isDelete(str)) {
        if(inDarkTheme) {
            color_del_dark
        }else {
            color_del
        }
    } else if(isHunkHeader(str)) {
        if(inDarkTheme) {
            color_hunkHeader_dark
        }else {
            color_hunkHeader
        }
    }else null;

}
