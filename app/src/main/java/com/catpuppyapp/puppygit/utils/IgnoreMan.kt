package com.catpuppyapp.puppygit.utils

import java.io.File
import java.io.FileWriter

/**
 * app specified ignore files manager, it under every repo's ".git/PuppyGit/ignore_v2.txt"
 *
 * usage:
 *   1 call `getAllValidPattern()` get a rules list
 *   2 for each file path call `matchedPatternList(relativePathUnderRepo, rules)`, if return true, means should be ignore
 */
@Deprecated("改用git自带的忽略了，仅需在添加到.gitignore之前先remove form git一下就能实现相同效果")
object IgnoreMan {
    private const val commentBegin = "//"
    private const val newFileContent = "$commentBegin a line start with \"$commentBegin\" will treat as comment\n$commentBegin each line one relative path under repo, support simple wildcard like *.log match all files has .log suffix\n\n"
    private const val fileName = "ignore_v2.txt"  // 1.0.5.3v29 was used file name: ignores.txt

    private fun getFile(repoDotGitDir: String): File {
        val f = File(AppModel.PuppyGitUnderGitDirManager.getDir(repoDotGitDir).canonicalPath, fileName)
        if(!f.exists()){
            f.createNewFile()
            f.bufferedWriter().use {
                it.write(newFileContent)
            }
        }

        return f
    }

    fun getAllValidPattern(repoDotGitDir: String):List<String> {
        val f = getFile(repoDotGitDir)
        val retList = mutableListOf<String>()
        f.bufferedReader().use { br ->
            while (true){
                val rline = br.readLine()?.trim() ?: break
                if(isValidLine(rline)){
                    retList.add(rline)
                }
            }
        }

        return retList
    }

    fun matchedPatternList(path:String, patternList:List<String>):Boolean {
        //这里的文件名匹配应大小写敏感，因为linux是大小写敏感的（不过windows不是）
        return RegexUtil.matchByPredicate(path, patternList, ignoreCase = false) { path, pattern ->
            RegexUtil.matchForIgnoreFile(path, pattern)
        }
    }

    private fun isComment(str:String):Boolean {
        return str.trimStart().startsWith(commentBegin)
    }

    private fun isValidLine(line:String):Boolean {
        return line.isNotEmpty() && !isComment(line)
    }

    fun getFileFullPath(repoDotGitDir: String):String {
        return getFile(repoDotGitDir).canonicalPath
    }

    fun appendLinesToIgnoreFile(repoDotGitDir: String, lines:List<String>) {
        if(lines.isEmpty()) {
            return
        }
        val curTime = getNowInSecFormatted()
        val ignoreFile = getFile(repoDotGitDir)
        val append = true
        val filerWriter = FileWriter(ignoreFile, append)
        filerWriter.buffered().use { writer ->
            // if no this head line and file was not ends with new line, content will concat as unexpected
            writer.write("\n$commentBegin $curTime\n")  // newLine + timestamp
            lines.forEachBetter { ln ->
                writer.write(ln+"\n")
            }
        }
    }

}
