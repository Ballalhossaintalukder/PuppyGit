package com.catpuppyapp.puppygit.fileeditor.texteditor.state

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue


private val lineChangeType_NEW_dark =  Color(0xFF33691E)
private val lineChangeType_NEW =  Color(0xFF8BC34A)
private val lineChangeType_UPDATED_dark =  Color(0xFF0277BD)
private val lineChangeType_UPDATED = Color(0xFF03A9F4)


//这个stable注解，我看了下应该符合条件，若更新状态出问题，可取消注解试试
@Stable
class TextFieldState(
    //这个id本来是作为LazyColumn的item key的，可能和触发重组有关，但实际上我很多地方都忘了在修改这个对象后更换此id，所以最后取消用这个值在做key了，不知道compose怎么判断是否需要重组，不过能用同时不卡就行了，有问题再处理
//    val id: String = getRandomUUID(),

    val value: TextFieldValue = TextFieldValue(),

    //为避免已选择list和条目list数量都过大时判断某行是否选中的性能差，所以保留这个字段，耗内存但在选中行和文件全部行数都很大时性能更好，不然判断一行是否被选中需要最大遍历两个list.size相乘的次数
    //另外，不要用此变量判断某行是否被聚焦，改用TextEditorState的focusingLineIndex去判断，
    // 不然每次聚焦一行都要先解除其他所有行的isSelected状态，这种方式需要全量浅拷贝，浪费性能
    val isSelected: Boolean = false,

    //用来指示当前行是编辑过，还是新增行，就像notepad++那样，这个状态和git无关，只针对当前会话，临时的，若实现成和git有关的话，有点麻烦，算了
    var changeType:LineChangeType = LineChangeType.NONE,

) {
    fun copy(
//        id: String = this.id,
        value: TextFieldValue = this.value,
        isSelected: Boolean = this.isSelected,
        changeType: LineChangeType = this.changeType,

    ) = TextFieldState(
//        id = id,
        value = value,
        isSelected = isSelected,
        changeType = changeType,

    );

    override fun toString(): String {
        return "TextFieldState(value=$value, isSelected=$isSelected, changeType=$changeType)"
    }


    //不太确定：非常极端的情况下，如果一个文件不换行，就一行，有非常多内容（可能几M），那这样比较不如直接比较地址？
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        // or `other !is TextFieldState`
        if (javaClass != other?.javaClass) return false

        other as TextFieldState

        if (isSelected != other.isSelected) return false
        if (changeType != other.changeType) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isSelected.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + changeType.hashCode()
        return result
    }


    fun updateLineChangeTypeIfNone(targetChangeType: LineChangeType) {
        //若行修改类型不是NONE，则代表已经确定当前行是新增还是修改了，就无需再更新其类型
        if(changeType == LineChangeType.NONE) {
            changeType = targetChangeType
        }
    }

    fun updateLineChangeType(targetChangeType: LineChangeType) {
        changeType = targetChangeType
    }


    fun getColorOfChangeType(inDarkTheme: Boolean):Color {
        return if(changeType == LineChangeType.NEW) {
            if(inDarkTheme) lineChangeType_NEW_dark else lineChangeType_NEW
        }else if(changeType == LineChangeType.UPDATED) {
            if(inDarkTheme) lineChangeType_UPDATED_dark else lineChangeType_UPDATED
        }else {
            //和背景颜色一样，所以等应该看不出差别，如果能显示出差异，可改成透明
            Color.Unspecified
        }
    }
}

enum class LineChangeType {
    NONE,
    NEW,
    UPDATED,

    //逻辑上来说其实还应该有个deleted，但删了就看不到了，无意义，所以实际不需要

}
