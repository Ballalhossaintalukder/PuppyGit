TODO sha256 support 20250607:
1. waiting for libgit2 sha256 feature become stable
2. UI: add menu item to RepoCard menu to allow users switch hash method, then save settings into .gitconfig (need test if were switched to sha256, then, still can switch back to sha1?)
3. UI: can choose sha256/sha1 when init a folder to git repo, and save to .gitconfig file
4. GUIDE: honor sha256 settings in the .gitconfig file, rather than save settings to db repo table
5. CODE: change Oid class of git24j to libgit2 oid series method binding, else need implement sha256 in java side, may cause bug


中文：
1. 等 libgit2 的sha256功能稳定
2. UI: 仓库卡片添加可切换hash算法的菜单项，并保存到仓库的.gitconfig（需要测试如果切换到 sha256 还能否切换回sha1？）
3. UI: 初始化文件夹为仓库时可选hash算法，并保存到.gitconfig
4. GUIDE: 遵循 .gitconfig 的 sha256 相关选项，把相关设置都存到 .gitcofnig 里，尽量别往 db repo表 存
5. CODE: 修改 git24j 的 Oid 类为libgit2 oid系列函数的binding，不然还得自己写sha256相关的算法，可能导致新的bug

