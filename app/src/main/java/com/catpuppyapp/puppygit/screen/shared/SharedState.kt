package com.catpuppyapp.puppygit.screen.shared

import androidx.compose.runtime.mutableStateOf
import com.catpuppyapp.puppygit.constants.Cons
import com.catpuppyapp.puppygit.dto.UndoStack
import com.catpuppyapp.puppygit.git.StatusTypeEntrySaver


private const val TAG = "SharedState"

object SharedState {
    val homeScreenNeedRefresh = mutableStateOf("")


    // indexToWorktree or headToIndex页面的列表，无论从上述哪个页面跳转到diff页面，都会把那个页面的列表地址存到这个变量中。(所以，导航链不能是 indexToWorktree ChangeList->Diff->headToIndex ChangeList->Diff，不过正常来说，并没从Diff前进到Index的路径，所以无此bug)
    var homeChangeList_itemList = mutableListOf<StatusTypeEntrySaver>()

    //用于在indexToWorkTree那个页面指示index是否有条目
    var homeChangeList_indexHasItem = mutableStateOf(false)

    // index to worktree
    val homeChangeList_LastClickedItemKey = mutableStateOf(Cons.init_last_clicked_item_key)
    val index_LastClickedItemKey = mutableStateOf(Cons.init_last_clicked_item_key)
    val treeToTree_LastClickedItemKey = mutableStateOf(Cons.init_last_clicked_item_key)
    val fileHistory_LastClickedItemKey = mutableStateOf(Cons.init_last_clicked_item_key)

    val homeChangeList_Refresh = mutableStateOf("IndexToWorkTree_ChangeList_refresh_init_value_5hpn")
    val indexChangeList_Refresh = mutableStateOf("HeadToIndex_ChangeList_refresh_init_value_ts7n")

    //用来在请求选择和执行选择的FileChooser页面之间共享选中的路径
    val fileChooser_DirPath = mutableStateOf("")  //如果用app内置 File Picker 选目录，用这个做state
    val fileChooser_FilePath = mutableStateOf("")  //如果用app内置 File Picker 选文件，用这个做state


}
