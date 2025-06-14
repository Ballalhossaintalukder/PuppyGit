package com.catpuppyapp.puppygit.dto

import com.catpuppyapp.puppygit.screen.shared.FuckSafFile

data class FileDetail(
    val file: FuckSafFile,
    val shortContent:String = "",
)
