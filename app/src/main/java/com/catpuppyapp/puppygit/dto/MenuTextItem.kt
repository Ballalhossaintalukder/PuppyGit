package com.catpuppyapp.puppygit.dto

data class MenuTextItem (
    val text:String,

    //点击条目后是否关闭菜单
    val closeMenuAfterClick:()->Boolean = {true},

    val enabled:()->Boolean = {true},
    val visible:()->Boolean = {true},
    val onClick:()->Unit,
)
