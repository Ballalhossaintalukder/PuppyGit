x 已解决）无法克隆ssh子模块 20250424：

已解决：原因：把`凭据.name`当作private key传过去了，晕！

报错：failed to authenticate SSH session: Unable to extract public key from private key file: Unsupported private key file format

但如果不是子模块的ssh仓库则可正常克隆，感觉应该不是很严重的bug，否则普通ssh url的仓库应该也无法克隆，改天看下libgit2源代码，看下是否好解决

复现：添加ssh url的submodule，然后选择对应的sshkey凭据并克隆

---
偶发bug，难以复现，备忘 20250402：
x 无法复现，检查逻辑，未发现问题，以后遇到再说吧) 换行bug：在行开头，回车，下次光标会在下一行第一个字后面，应在前面

---
（已解决）文本编辑器bug（不严重） 20250325：
解决方案：仅让TextField的onFocus负责聚焦，其会调用selectField定位到某行，update时更新状态使用上次定位的位置即可。


复现：选中有文本的行，点击其他行的行号，启动多选模式，指示当前行的三道横线图标依然在之前编辑的行而不是点击行号启动选择模式的那行。

原因：旧状态调用updateField()的时机有问题，比如旧状态先调用selectField()，触发onChanged()，创建了新状态，接着，旧状态的updateField()被调用，于是，state中的已选中行又变回了旧状态中的数据。


实际影响：问题不大，只有当按复现方法触发bug后，初次光标定位不准，其他没影响。

---
文本编辑器切换预览和编辑时自动定位不够准确 20250319：
功能：在特定切换预览和编辑的时候换算行号和像素，然后定位到滚动位置。


触发场景：
从预览到编辑，需要定位行号的场景： 
1. 在预览页面点击顶部编辑图标

从编辑到预览，需要定位行号的场景：
1. 预览页面导航栈首次打开root条目时
2. 非初次预览，但当前预览页面和编辑页面不匹配：正在编辑文件a，然后预览文件a，然后从文件a通过点击相对路径链接跳转到了文件b，在预览文件b的时候点顶部左上角关闭按钮退出预览返回编辑，这时预览的b和编辑的a，正在预览和正在编辑不匹配，若再进入预览页面，就会在当前导航栈“加塞”，这时相当于预览页面重新打开一次当前编辑的文件，所以需要重新初始化下预览的滚动位置，而初始化操作就是尽量定位到当前正在编辑的位置


缺陷：不准，因为需要换算像素和行号，很难做到精准，如果基本都是纯文字，准确度尚可，若有图片等文字和预览大小差异较大的情况，偏差会比较大。
