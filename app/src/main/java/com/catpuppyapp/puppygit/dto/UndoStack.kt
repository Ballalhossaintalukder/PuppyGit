package com.catpuppyapp.puppygit.dto

import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import com.catpuppyapp.puppygit.fileeditor.texteditor.state.TextEditorState
import com.catpuppyapp.puppygit.utils.MyLog
import com.catpuppyapp.puppygit.utils.getSecFromTime
import java.util.LinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val TAG = "UndoStack"

private const val defaultSizeLimit = 100

class UndoStack(
    /**
    用来标记这是哪个文件的stack
     */
    var filePath:String,
    /**
     * 记多少步
     */
    var sizeLimit: Int = defaultSizeLimit,

    /**
     * 保存间隔，秒数，为0则不限只要状态变化就立即存一版
     */
//    val undoSaveIntervalInSec:Int = 5,
    //5秒的用户体验并不好，有可能漏选，用0秒了
    var undoSaveIntervalInSec:Int = 0,

    /**
     * utc秒数，上次保存时间，用来和时间间隔配合实现在几秒内只保存一次，若为0，无视时间间隔，立即存一版本，然后更新时间为当前秒数
     */
    val undoLastSaveAt:MutableLongState = mutableLongStateOf(0),


    private var undoStack:LinkedList<TextEditorState> = LinkedList(),
    private var redoStack:LinkedList<TextEditorState> = LinkedList(),
    private var undoLock: ReentrantLock = ReentrantLock(true),
    private var redoLock: ReentrantLock = ReentrantLock(true),
) {

    fun reset(filePath:String) {
        this.filePath = filePath
        sizeLimit = defaultSizeLimit
        undoSaveIntervalInSec = 0
        undoLastSaveAt.longValue = 0L
        undoStack = LinkedList()
        redoStack = LinkedList()
        undoLock = ReentrantLock(true)
        redoLock = ReentrantLock(true)
    }

    fun copyFrom(other:UndoStack) {
        filePath = other.filePath
        sizeLimit = other.sizeLimit
        undoSaveIntervalInSec = other.undoSaveIntervalInSec
        undoLastSaveAt.longValue = other.undoLastSaveAt.longValue
        undoStack = other.undoStack
        redoStack = other.redoStack
        undoLock = other.undoLock
        redoLock = other.redoLock
    }

    fun undoStackIsEmpty():Boolean {
        return undoStack.isEmpty()
    }

    fun redoStackIsEmpty():Boolean {
        return redoStack.isEmpty()
    }

    fun undoStackSize():Int {
        return undoStack.size
    }

    fun redoStackSize():Int {
        return redoStack.size
    }

    /**
     * @return true saved, false not saved
     */
    fun undoStackPush(state: TextEditorState):Boolean {
        undoLock.withLock {
            val now = getSecFromTime()
            val snapshotLastSaveAt = undoLastSaveAt.longValue
            //在时间间隔内只存一版
            if(undoSaveIntervalInSec == 0 || snapshotLastSaveAt == 0L || (now - snapshotLastSaveAt) > undoSaveIntervalInSec) {
                push(undoStack, state)
                undoLastSaveAt.longValue = now

                //若超过数量限制移除第一个
                if(undoStack.size > sizeLimit) {
                    undoStack.removeAt(0)
                }

                return true
            }

            return false
        }
    }

    fun undoStackPop(): TextEditorState? {
        undoLock.withLock {
            return pop(undoStack)
        }
    }

    /**
     * @return true saved, false not saved
     */
    fun redoStackPush(state: TextEditorState):Boolean {
        redoLock.withLock {
            push(redoStack, state)
            return true
        }
    }

    fun redoStackPop(): TextEditorState? {
        redoLock.withLock {
            undoLock.withLock {
                //为使弹窗的状态可立刻被undo stack存上，所以将上次存储时间清0
                undoLastSaveAt.longValue = 0
            }

//            remainOnceRedoStackCount()

//            val last = pop(redoStack)
//            redoLastPop.value = last
//            return last

            return pop(redoStack)
        }
    }

    fun redoStackClear() {
        redoLock.withLock {
            redoStack.clear()
        }
    }

    private fun push(stack: MutableList<TextEditorState>, state: TextEditorState) {
        try {
            // add to tail
            stack.add(state)
        }catch (e:Exception) {
            MyLog.e(TAG, "#push, err: ${e.stackTraceToString()}")
        }
    }
//
//    @Deprecated("弃用，感觉不需要做太多判断，应该调用者自己判断是否需要入栈")
//    private fun pushDeprecated(stack: MutableList<TextEditorState>, state: TextEditorState) {
//        try {
//            //第2个判断是个粗略判断，避免在只有一个条目且内容相等时重复添加内容到栈
//            if(stack.isEmpty() || !(state.fields.size==1 && stack.last().fields.size == 1 && state.fields.first()==stack.last().fields.first()) || stack.last().fieldsId != state.fieldsId) {
////            if(stack.isEmpty() || state.fields != peek(stack)?.fields) {
//                stack.add(state)
//
//                if(stack.size > sizeLimit) {
//                    stack.removeAt(0)
//                }
//            }
//        }catch (e:Exception) {
//            MyLog.e(TAG, "#push, err: ${e.stackTraceToString()}")
//        }
//    }

    private fun pop(stack: MutableList<TextEditorState>): TextEditorState? {
        return try {
            stack.removeAt(stack.size - 1)
        }catch (e:Exception) {
            null
        }
    }

    private fun peek(stack: MutableList<TextEditorState>): TextEditorState? {
        return try {
            stack.get(stack.size - 1)
        }catch (e:Exception) {
            null
        }
    }

}
