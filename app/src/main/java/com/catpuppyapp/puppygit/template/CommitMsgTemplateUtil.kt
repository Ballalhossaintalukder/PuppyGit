package com.catpuppyapp.puppygit.template

import com.catpuppyapp.puppygit.git.StatusTypeEntrySaver
import com.catpuppyapp.puppygit.utils.Libgit2Helper
import com.catpuppyapp.puppygit.utils.getFormatTimeFromSec
import com.catpuppyapp.puppygit.utils.getSecFromTime
import com.github.git24j.core.Repository
import java.time.format.DateTimeFormatter

object CommitMsgTemplateUtil {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    // yyyy-MM-dd
    val datePlaceHolder = PlaceHolder(pattern = "{{date}}", example = "2024-05-20")
    // HH:mm:ss
    val timePlaceHolder = PlaceHolder(pattern = "{{time}}", example = "22:10:05")
    val usernamePlaceHolder = PlaceHolder(pattern = "{{username}}", example = "Tony")
    val emailPlaceHolder = PlaceHolder(pattern = "{{email}}", example = "tony@example.com")
    val filesCountPlaceHolder = PlaceHolder(pattern = "{{filesCount}}", example = "5")
    val fileNamesPlaceHolder = PlaceHolder(pattern = "{{fileNames}}", example = "file1.txt, file2.txt")

    val phList = listOf(
        datePlaceHolder,
        timePlaceHolder,
        usernamePlaceHolder,
        emailPlaceHolder,
        filesCountPlaceHolder,
        fileNamesPlaceHolder,
    )

    fun replace(
        repo:Repository,
        itemList: List<StatusTypeEntrySaver>?,
        msgTemplate:String,
    ):String {
        val (username, email) = Libgit2Helper.getGitUsernameAndEmail(repo)
        val now = getSecFromTime()
        return msgTemplate.replace(datePlaceHolder.pattern, getFormatTimeFromSec(now, dateFormatter))
            .replace(timePlaceHolder.pattern, getFormatTimeFromSec(now, timeFormatter))
            .replace(usernamePlaceHolder.pattern, username)
            .replace(emailPlaceHolder.pattern, email)
            .replace(filesCountPlaceHolder.pattern, ""+(itemList?.size?:0))
            .replace(fileNamesPlaceHolder.pattern,
                if(itemList.isNullOrEmpty()) {
                    ""
                }else {
                    //默认 ", " 分隔，不需要改
                    itemList.joinToString { it.fileName }
                }
            )
    }
}
