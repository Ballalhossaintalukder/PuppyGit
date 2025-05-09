package com.catpuppyapp.puppygit.compose

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.catpuppyapp.puppygit.utils.apkIconOrNull
import com.catpuppyapp.puppygit.utils.doJobThenOffLoading
import com.catpuppyapp.puppygit.utils.getVideoThumbnail
import com.catpuppyapp.puppygit.utils.mime.MimeType
import com.catpuppyapp.puppygit.utils.mime.guessFromFile
import com.catpuppyapp.puppygit.utils.mime.iconRes
import java.io.File

private const val iconSizeInPx = 100
private val iconModifierSize = 50.dp
private val thumbnailModifierSize = 50.dp
private val defaultIconModifier = Modifier.size(iconModifierSize)
private val defaultThumbnailModifier = Modifier.size(thumbnailModifierSize)
private val contentScale = ContentScale.Crop


@Composable
fun IconOfItem(
    fileName:String,
    filePath:String,
    context: Context,
    contentDescription:String?,

    //非图片类型，才会使用图标颜色
    iconColor: Color = LocalContentColor.current,

    //若为null，将根据mime类型获取对应图标
    defaultIconWhenLoadFailed: ImageVector? = null,
    iconModifier: Modifier = defaultIconModifier,
    thumbnailModifier: Modifier = defaultThumbnailModifier,

) {
    val file = remember(filePath) { File(filePath) }

    val filePath = remember(file.canonicalPath) { file.canonicalPath }

    //如果是图片，显示缩略图，否则显示图标
    // guessFromFile()会判断是否是文件夹，guessFromFileName()则不会，为了能为文件夹正常显示图标，这里应用用guessFromFile()
    val mime = MimeType.guessFromFile(file)

    //图片类型
    if(mime.type == "image" && file.let{ it.exists() && it.isFile }) {
        ShowThumbnail(context, filePath, contentDescription, mime.iconRes, iconColor, thumbnailModifier)

        return
    }

    //视频类型
    if(mime.type == "video" && file.let{ it.exists() && it.isFile }) {
        ShowVideoThumbnail(context, filePath, contentDescription, mime.iconRes, iconColor, thumbnailModifier)

        return
    }

    if(mime == MimeType.APK) {
        val apkIcon = apkIconOrNull(context, filePath, iconSizeInPx)
        if(apkIcon != null) {
            Image(
                apkIcon,
                contentDescription = contentDescription,
                modifier = thumbnailModifier,
            )

            return
        }
    }



    ShowIcon(defaultIconWhenLoadFailed ?: mime.iconRes, contentDescription, iconColor, iconModifier)

}

@Composable
private fun ShowThumbnail(
    context:Context,
    filePath:String,
    contentDescription: String?,
    fallbackIcon: ImageVector,
    fallbackIconColor: Color,
    modifier: Modifier
) {
    val loadErr = remember { mutableStateOf(false) }

    if(loadErr.value) {
        ShowIcon(fallbackIcon, contentDescription, fallbackIconColor, modifier)
    }else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(filePath)
                .size(iconSizeInPx)
                .decoderFactory(SvgDecoder.Factory())
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,  //超过容器宽高则居中裁剪
            placeholder = rememberVectorPainter(fallbackIcon),  // show while loading
            modifier = modifier,  //.clip(RectangleShape)，想弄成正方形，但没卵用，算了
            onError = {
                loadErr.value = true
            },
            onLoading = {
                loadErr.value = false
            },
            onSuccess = {
                loadErr.value = false
            }
        )
    }
}


@Composable
private fun ShowVideoThumbnail(
    context:Context,
    filePath:String,
    contentDescription: String?,
    fallbackIcon: ImageVector,
    fallbackIconColor: Color,
    modifier: Modifier
) {
    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }

    thumbnail.value.let {
        if(it == null) {
            ShowIcon(fallbackIcon, contentDescription, fallbackIconColor, modifier)
        }else {
            Image(
                it,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
            )
        }
    }

    LaunchedEffect(Unit) {
        doJobThenOffLoading {
            thumbnail.value = getVideoThumbnail(filePath)
        }
    }
}

