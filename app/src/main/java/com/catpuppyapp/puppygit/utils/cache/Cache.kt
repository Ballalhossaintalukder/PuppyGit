package com.catpuppyapp.puppygit.utils.cache

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import com.catpuppyapp.puppygit.utils.getShortUUID

/**
 * this is default cache instance
 */
object Cache:CacheStoreImpl(){
    const val keySeparator = ":"

    object Key {
//        val changeListInnerPage_SavePatchPath = "cl_patchsavepath"
//        val changeListInnerPage_RequireDoSyncAfterCommit = "cliprdsac"  //这个其实改用状态变量能达到同样的效果
        //            val changeListInnerPage_RequirePull = "cliprpul";
//            val changeListInnerPage_RequirePush = "cliprpus";
//            val changeListInnerPage_RequireSync = "cliprsyn";
        const val changeListInnerPage_requireDoActFromParent = "cliprdafp";
        const val repoTmpStatusPrefix = "repo_tmp_status"  // prefix+keyseparator+repoId，例如 "repo_tmp_status:a29388d9988"

        const val editorPageSaveLockPrefix = "editor_save_lock"
        const val subPagesStateKeyPrefix = "sub_pages_key_prefix$keySeparator"

//        const val diffScreen_underRepoPathKey = "diffScreen_underRepoPathKey"
//        const val diffScreen_diffableItemListKey = "diffScreen_diffableItemListKey"

//        const val fileHistory_fileRelativePathKey = "fileHistory_fileRelativePathKey"
//        const val subPageEditor_filePathKey = "subPageEditor_filePathKey"

        val diffableList_of_fromDiffScreenBackToWorkTreeChangeList = "diffableList_of_fromDiffScreenBackToWorkTreeChangeList"
//        val diffableList_of_fromDiffScreenBackToIndexChangeList = "diffableList_of_fromDiffScreenBackToIndexChangeList"
    }

    fun clearAllSubPagesStates() {
        clearByKeyPrefix(Key.subPagesStateKeyPrefix)
    }


    // 自定义状态存储器key相关函数：开始
    /**
     * 给子页面生成stateKeyTag的函数，这个函数最关键的操作是给【加子页面key前缀 和 给子页面生成随机id】，
     * 前缀的作用是用来清理内存时标识哪些是子页面的状态，具体实现就是在返回顶级页面后子页面已经全部弹出导航栈，这时就清理所有包含子页面前缀的key，释放内存；
     * 随机数则是为了避免 a->b-c->b 这样的路径上有重复的子页面实例出现导致状态冲突。
     *
     * 顶级页面和子页面和inner page的区别：
     * 顶级页面和子页面都是在导航组件导航进入的，其返回也是导航返回的（naviUp()），inner page本质上是组件
     * 例如：HomeScreen是顶级页面；BranchListScreen是子页面；ChangeListInnerPage是组件；Dialog弹窗之类的也是组件。
     */
    @Composable
    fun getSubPageKey(stateKeyTag:String):String {
        // e.g. "sub_pages_key_prefix:DiffScreen:ak1idkjgkk"
        return rememberSaveable { Key.subPagesStateKeyPrefix+stateKeyTag+ keySeparator+ getShortUUID() }
    }

    /**
     * 给组件用的，一个页面可能有多个组件，每个都需要单独生成，会"继承"父组件的stateKeyTag，
     *  是否会在返回顶级页面清理取决于其父组件是否是顶级页面，由parentKey(即父组件的stateKeyTag)判断，
     *  每个组件都应该有各自的stateKeyTag，子组件若需要，往下传递即可，inner page本质上也是组件而不是页面（不可导航进入），
     *  所以应使用此函数为其生成stateKeyTag。
     */
    @Composable
    fun getComponentKey(parentKey:String, stateKeyTag:String): String {
        // e.g. 子页面："sub_pages_key_prefix:DiffScreen:ak1idkjgkk:DiffRow:13idgiwkfd"
        //      顶级页面："HomeScreen:abcdef12345:DiffRow:abcdef12345"
        return rememberSaveable { parentKey+stateKeyTag+ keySeparator+ getShortUUID() }
    }
    // 自定义状态存储器key相关函数：结束


    fun combineKeys(vararg keys: String):String {
        val ret = StringBuilder()

        for (key in keys) {
            ret.append(key).append(keySeparator)
        }

        return ret.removeSuffix(keySeparator).toString()
    }
}
