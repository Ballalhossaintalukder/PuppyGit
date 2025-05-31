package com.catpuppyapp.puppygit.git

import com.catpuppyapp.puppygit.constants.Cons
import com.catpuppyapp.puppygit.utils.compare.CmpUtil
import com.catpuppyapp.puppygit.utils.compare.param.StringCompareParam
import com.catpuppyapp.puppygit.utils.compare.result.IndexModifyResult
import com.catpuppyapp.puppygit.utils.getShortUUID
import com.github.git24j.core.Diff
import java.util.EnumSet
import java.util.TreeMap

data class DiffItemSaver (
    var relativePathUnderRepo:String="",  //仓库下相对路径
    var keyForRefresh:String= getShortUUID(),

    // 这个字段好像可有可无
    var fromTo:String= Cons.gitDiffFromIndexToWorktree,

//    var fileHeader:String="";  // file好像没有header
    var oldFileOid:String="",
    var newFileOid:String="",
    var newFileSize:Long=0L,
    var oldFileSize:Long=0L,
    //diff 所有 line.content总和有无超过限制大小
    var isContentSizeOverLimit:Boolean=false,
//    var efficientFileSize=0L
    var flags: EnumSet<Diff.FlagT> = EnumSet.of(Diff.FlagT.NOT_BINARY),
    var hunks:MutableList<PuppyHunkAndLines> = mutableListOf(),  //key是hunk的指针地址

    //指示文件是否修改过，因为有时候会错误的diff没修改过的文件，所以需要判断下
    var isFileModified:Boolean=false,
    var addedLines:Int=0,  //添加了多少行。（不包含EOF，因为那个东西判断不太准，有时候明明删了却显示添加，让人困惑，而且一个空行感觉好像意义不大？）
    var deletedLines:Int=0,  //删除了多少行
    var allLines:Int=0,  //总共多少行，包含添加、删除、上下文，如果有eof，也包含eof

    //最大的行号。（用来算行号padding的）
    var maxLineNum:Int=0,
    var hasEofLine:Boolean = false,


    // 比较的左右两边的文件的类型，如果是图片，则按图片预览，若都是text，则按text预览
    var oldFileType: DiffItemSaverType = DiffItemSaverType.TEXT,
    var newFileType: DiffItemSaverType = DiffItemSaverType.TEXT,

    // 把blob文件存到本地的path，一般存到缓存目录供临时查看，预览图片时会用到
    var oldBlobSavePath:String="",
    var newBlobSavePath:String="",

    //根据delta比较出来的实际的修改类型，最终在diff页面显示的修改类型以这个为准
    var changeType:String = Cons.gitStatusUnmodified,
){



    //获取实际生效的文件大小
    //ps:如果想判断文件大小有无超过限制，用此方法返回值作为 isFileSizeOverLimit() 的入参做判断即可
    fun getEfficientFileSize():Long {
        return if(newFileSize>0) newFileSize else oldFileSize
    }


    /**
     * 为line生成假索引，有可能会用来判断一些东西，目前只用来在预览diff内容时首行加top padding
     */
    fun generateFakeIndexForGroupedLines() {
        for(h in hunks) {
            //每个hunk的索引都应从0开始数（这里-1没写错，写成-1后面直接用++index一路往下走就行，省事）
            var index = -1

            for((_, lines) in h.groupedLines) {
                //顺序是context/del/add
                lines.get(Diff.Line.OriginType.CONTEXT.toString())?.let { it.fakeIndexOfGroupedLine = ++index }
                lines.get(Diff.Line.OriginType.CONTEXT_EOFNL.toString())?.let { it.fakeIndexOfGroupedLine = ++index }
                lines.get(Diff.Line.OriginType.DELETION.toString())?.let { it.fakeIndexOfGroupedLine = ++index }
                lines.get(Diff.Line.OriginType.DEL_EOFNL.toString())?.let { it.fakeIndexOfGroupedLine = ++index }
                lines.get(Diff.Line.OriginType.ADDITION.toString())?.let { it.fakeIndexOfGroupedLine = ++index }
                lines.get(Diff.Line.OriginType.ADD_EOFNL.toString())?.let { it.fakeIndexOfGroupedLine = ++index }

            }
        }
    }
}

class PuppyHunkAndLines {
    var hunk:PuppyHunk=PuppyHunk();
    var lines:MutableList<PuppyLine> = mutableListOf()


    //根据行号分组
    //{lineNum: {originType:line}}, 其中 originType预期有3种类型：context/del/add
    var groupedLines:TreeMap<Int, Map<String, PuppyLine>> = TreeMap()

    // {lineNum: IndexModifyResult}
    private val modifyResultMap:MutableMap<Int, IndexModifyResult> = mutableMapOf()

    // {linNum: Unit}, if map.get(lineNum) != null, means already showed add or del line as context, need not show one more
    // add and del only difference at end has "/n" or not, in that case, show 1 of them as context
    // 同一行，包含添加和删除，区别只在于末尾是否有换行符，仅显示对应行号一次，且类型为context
    private val mergedAddDelLine:MutableSet<Int> = mutableSetOf()

    class MergeAddDelLineResult (
        //是否已经显示过此行，若已显示过，不会在显示，例如第12行只有末尾是否有换行符的区别，遍历到+12时，显示12行作为context，下次遍历到-12，则直接不显示
        // if already showed this line as context, next time same line will not showed again, eg: if +12 and -12 only diff at end of line has "\n" or not, then only show context line 12 once
        // instead by field `data`
//        val alreadyShowedAsContext:Boolean,

        //是否需要显示此行为context
        // need show this line as context or not, if not, will show origin line, else set line origin type to context, then show it
        val needShowAsContext:Boolean,

        // if need show data as context, set text to this field, else set it to null
        // 如果这行已经作为上下文显示过，设其为null，否则设置上原本add或del的内容
        val line:PuppyLine?=null,
    )

    /**
     * should clear caches if page re-render
     */
    fun clearCachesForShown(){
        mergedAddDelLine.clear()
        modifyResultMap.clear()
    }

    fun addLineToGroup(puppyLine: PuppyLine) {
        val lineNum = puppyLine.lineNum
        val line = groupedLines.get(lineNum)
        if(line==null) {
            val map = mutableMapOf<String, PuppyLine>()
            map.put(puppyLine.originType, puppyLine)
            groupedLines.put(lineNum, map)
        }else {
            (line as MutableMap).put(puppyLine.originType, puppyLine)
        }
    }


    /**
     * 仅对类型为add或del的行调用此函数，若返回true，代表两者除了末尾换行符没区别，这时将其转换为context显示，
     */
    fun needShowAddOrDelLineAsContext(lineNum: Int):MergeAddDelLineResult {
        val groupedLine = groupedLines.get(lineNum)
        val add = groupedLine?.get(Diff.Line.OriginType.ADDITION.toString())
        val del = groupedLine?.get(Diff.Line.OriginType.DELETION.toString())
        if(add!=null && del!=null && add.getContentNoLineBreak().equals(del.getContentNoLineBreak())) {
            val alreadyShowed = mergedAddDelLine.add(lineNum).not()

            return MergeAddDelLineResult(
                needShowAsContext = true,
                line = if(alreadyShowed) null else del.copy(originType = Diff.Line.OriginType.CONTEXT.toString()),
            )
        }else {
            return MergeAddDelLineResult(needShowAsContext = false)
        }
    }


    fun needShowAddOrDelLineAsContext_2(lineNum: Int):Boolean {
        val groupedLine = groupedLines.get(lineNum)
        val add = groupedLine?.get(Diff.Line.OriginType.ADDITION.toString())
        val del = groupedLine?.get(Diff.Line.OriginType.DELETION.toString())
        return add!=null && del!=null && add.getContentNoLineBreak().equals(del.getContentNoLineBreak())
    }

    fun getModifyResult(lineNum: Int, requireBetterMatchingForCompare:Boolean, matchByWords:Boolean):IndexModifyResult? {
        val r = modifyResultMap.get(lineNum)

        if(r!=null) {
            return r
        }

        // r is null, try generate
        val groupedLine = groupedLines.get(lineNum)

        val add = groupedLine?.get(Diff.Line.OriginType.ADDITION.toString())
        val del = groupedLine?.get(Diff.Line.OriginType.DELETION.toString())

        if(add!=null && del!=null) {
            val modifyResult2 = CmpUtil.compare(
                add = StringCompareParam(add.content, add.content.length),
                del = StringCompareParam(del.content, del.content.length),

                //为true则对比更精细，但是，时间复杂度乘积式增加，不开 O(n)， 开了 O(nm)
                requireBetterMatching = requireBetterMatchingForCompare,
                matchByWords = matchByWords,

                //20250210之后：我发现调换后，又有很多add在前，del在后匹配率更高的情况，所以我觉得没必要调换了，可能匹配率差不太多，调换反而影响性能
                //(20250210之前)我发现del在前面add在后面匹配率更高，所以swap传true
//                swap = true
            )

            modifyResultMap.put(lineNum, modifyResult2)
            return modifyResult2
        }else {
            return null
        }

    }
}

class PuppyHunk {
    /**
     * 参见 `hunk_header_format.md`
     */
    var header:String=""

    private var cachedHeader:String? = null

    //若不trimEnd()，末尾有空行，影响排版，感觉像多了padding，不好看
    fun cachedNoLineBreakHeader() = (cachedHeader ?: header.trimEnd().let { cachedHeader = it; it });

}

data class PuppyLine (
    var key:String = getShortUUID(),

    // group line时，按行号把不同origin type的都放一组，实际上没索引，所以用这个生成一个索引替代
    var fakeIndexOfGroupedLine:Int = 0,

    var originType:String="",  //这个当初实现的时候考虑不周，既然原始类型是char我为什么要用String存呢？
    var oldLineNum:Int=-1,
    var newLineNum:Int=-1,
    var contentLen:Int=0,
    var content:String="",  //根据字节数分割后的内容
//    var rawContent:String="";  //原始内容
    var lineNum:Int=1,  // getLineNum(PuppyLine) 的返回值，实际的行号
    var howManyLines:Int=0  // content里有多少行
//    private val lines:MutableList<PuppyLine> = mutableListOf();  //针对content处理，把content的每个行都存成了一个PuppyLine（content默认只有多个连续行的第一个行有行号
//
//    /**
//     * 由于libgit2会把多个连续行合并成一个，只有第一个显示行号，所以需要处理下
//     * 注意：这个方法的正确性有待验证。
//     */
//    fun getLines():MutableList<PuppyLine> {
//        if (lines.isNotEmpty()) {
//            return lines
//        }
//        //TODO 把这个diff整清楚
//        if(debugModeOn) {
//            println("startLine=${lineNum}，多少行："+howManyLines)
//            println("content分割出多少行:"+content.lines().size)
//        }
//        lines.add(this)
////
////        //只处理 上下文（白色）和 新增（绿色）和 删除（红色）三种类型，其他的比如添加删除文件末尾行之类的不做处理
////        if(originType != Diff.Line.OriginType.CONTEXT.toString()
////            && originType != Diff.Line.OriginType.ADDITION.toString()
////            && originType!=Diff.Line.OriginType.DELETION.toString()
////        ) {
////            lines.add(this)
////        }else {
////            var cnt = lineNum
//////        println(content.lines().size-1)
////            val cls = content.lines()
////            for((idx, it) in cls.withIndex()) {
////                //多余的行。判断条件为“最后一行且为空”，可能是 lines()函数返回的空行，比如 abc\n，lines() 会返回两行，但有时候这和函数又不会返回多余的行，我凌乱了，目前先这样吧
////                if(idx==cls.size-1 && it.isEmpty()){
////                    break
////                }
////                val p = PuppyLine()
////                p.originType = originType
////                p.lineNum=cnt++
////                p.content = it
////                lines.add(p)
////            }
////        }
//
//        return lines
//    }

){
    private var contentNoBreak:String? = null
    fun getContentNoLineBreak():String {  // not safe for concurrency
        if(contentNoBreak == null) {
            contentNoBreak = content.removeSuffix(Cons.lineBreak)
        }
        return contentNoBreak ?: content.removeSuffix(Cons.lineBreak)
    }



    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PuppyLine

        if (originType != other.originType) return false
        if (oldLineNum != other.oldLineNum) return false
        if (newLineNum != other.newLineNum) return false
        if (contentLen != other.contentLen) return false
        if (content != other.content) return false
        if (lineNum != other.lineNum) return false
        if (howManyLines != other.howManyLines) return false

        return true
    }

    override fun hashCode(): Int {
        var result = originType.hashCode()
        result = 31 * result + oldLineNum
        result = 31 * result + newLineNum
        result = 31 * result + contentLen
        result = 31 * result + content.hashCode()
        result = 31 * result + lineNum
        result = 31 * result + howManyLines
        return result
    }


}

enum class DiffItemSaverType {
    TEXT,
    IMG,
}
