package com.catpuppyapp.puppygit.settings.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.catpuppyapp.puppygit.data.entity.RepoEntity
import com.catpuppyapp.puppygit.dto.AppInfo
import com.catpuppyapp.puppygit.service.AutomationService
import com.catpuppyapp.puppygit.settings.AutomationSettings
import com.catpuppyapp.puppygit.settings.SettingsUtil
import com.catpuppyapp.puppygit.utils.AppModel
import com.catpuppyapp.puppygit.utils.Libgit2Helper
import com.catpuppyapp.puppygit.utils.MyLog
import com.catpuppyapp.puppygit.utils.getInstalledAppList


object AutomationUtil {
    private const val TAG = "AutomationUtil"


    fun getPackageNames(automationSettings: AutomationSettings):Set<String> {
        return automationSettings.packageNameAndRepoIdsMap.keys
    }

    fun getRepoIds(automationSettings: AutomationSettings, packageName:String):List<String> {
        return automationSettings.packageNameAndRepoIdsMap.get(packageName) ?: listOf()
    }


    suspend fun getRepos(automationSettings: AutomationSettings, packageName:String):List<RepoEntity>? {
        //去重下id，以免重复，虽然一般不会重复
        val bindRepoIds = getRepoIds(automationSettings, packageName).toSet()
        if(bindRepoIds.isEmpty()) {
            return null
        }

        return try {
            val repoList = AppModel.dbContainer.repoRepository.getAll(updateRepoInfo = false).filter { bindRepoIds.contains(it.id) }
            val settings = SettingsUtil.getSettingsSnapshot()

            //仅更新有可能用到的仓库的信息
            repoList.forEach {
                Libgit2Helper.updateRepoInfo(it, settings = settings)
            }

            repoList
        } catch (e:Exception) {
            MyLog.e(TAG, "#getRepos() err: packageName=$packageName, err=${e.stackTraceToString()}")

            null
        }
    }


    /**
     * 检查App无障碍服务是否启用
     * @return `null` unknown state, `true` enabled, `false` disabled
     *
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean? {
        return try {
            val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            val componentName = ComponentName(context, AutomationService::class.java).flattenToString()

            enabledServices.contains(componentName)
        }catch (e:Exception) {
            MyLog.e(TAG, "#isAccessibilityServiceEnabled() err: ${e.stackTraceToString()}")
            null
        }
    }

    /**
     * @return Pair(selectedList, unselectedList)
     */
    fun getSelectedAndUnSelectedAppList(context: Context, automationSettings: AutomationSettings):Pair<List<AppInfo>, List<AppInfo>> {
        val installedAppList = getInstalledAppList(context)
        val userAddedAppList = getPackageNames(automationSettings)
        val selectedList = mutableListOf<AppInfo>()
        val unselectedList = mutableListOf<AppInfo>()

        //记录存在的app包名，用来移除已卸载但仍在配置文件中的app
        val existedApps = mutableListOf<String>()

        installedAppList.forEach { installed ->
            if(userAddedAppList.contains(installed.packageName)) {
                installed.isSelected = true
                selectedList.add(installed)
                existedApps.add(installed.packageName)
            }else {
                installed.isSelected = false
                unselectedList.add(installed)
            }
        }


        // remove uninstalled apps
        SettingsUtil.update { s ->
            val newMap = mutableMapOf<String, List<String>>()
            val oldMap = s.automation.packageNameAndRepoIdsMap
            existedApps.forEach { packageName ->
                //这里oldMap.get()百分百有值（除非并发修改，但在这个函数运行期间并发修改这个map的概率很小，几乎不会发生），
                // 因为existedApps添加的包名必然是oldMap的key，不过为了逻辑完整以及避免出错，还是 ?: 一个空list保险
                newMap.put(packageName, oldMap.get(packageName) ?: listOf())
            }

            s.automation.packageNameAndRepoIdsMap = newMap
        }

        return Pair(selectedList, unselectedList)
    }


}
