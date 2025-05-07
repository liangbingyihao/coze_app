package sdk.chat.demo.robot

import android.content.Context
import sdk.chat.ui.R
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorRes
import com.mikepenz.iconics.utils.sizeDp
import sdk.chat.demo.MainApp

object IconUtils {
    private val appContext: Context = MainApp.getContext()

    // 懒加载图标
     val favoriteFilled by lazy {
        IconicsDrawable(appContext).apply {
            icon = GoogleMaterial.Icon.gmd_favorite
            colorRes = R.color.red
            sizeDp = 18
        }
    }

     val favoriteBorder by lazy {
        IconicsDrawable(appContext).apply {
            icon = GoogleMaterial.Icon.gmd_favorite_border
            colorRes = R.color.gray_1
            sizeDp = 18
        }
    }
}