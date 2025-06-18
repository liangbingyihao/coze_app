package sdk.chat.demo.robot.handlers;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.graphics.drawable.ColorDrawable;

import sdk.chat.ui.R;
import sdk.chat.ui.activities.BaseActivity;
import sdk.chat.demo.robot.ui.PopupImageView;


public class ImageMessageOnClickHandler {

    public static void onClick(Activity activity, View view, String imageUrl, String bible) {
        BaseActivity.hideKeyboard(activity);

        if (!imageUrl.replace(" ", "").isEmpty()) {

//            PopupImageView popupView = ChatSDKUI.provider().popupImageView(activity);

            PopupImageView popupView = new PopupImageView(activity);

            final PopupWindow imagePopup = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
            imagePopup.setOnDismissListener(popupView::dispose);

            imagePopup.setContentView(popupView);
//            imagePopup.setBackgroundDrawable(new BitmapDrawable());
            imagePopup.setOutsideTouchable(true);
            imagePopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            imagePopup.setAnimationStyle(R.style.ImagePopupAnimation);

            popupView.setUrl(activity, imageUrl, bible, imagePopup::dismiss);

            popupView.setOnDismiss(imagePopup::dismiss);
            popupView.findViewById(sdk.chat.demo.pre.R.id.popupView).setOnClickListener(v -> imagePopup.dismiss());

            imagePopup.showAtLocation(view, Gravity.CENTER, 0, 0);
        }
    }

    public static void onClick(Activity activity, View view, Bitmap bitmap, String cacheKey) {
        BaseActivity.hideKeyboard(activity);

        PopupImageView popupView = new PopupImageView(activity);
//        PopupImageView popupView = ChatSDKUI.provider().popupImageView(activity);

        final PopupWindow imagePopup = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        imagePopup.setOnDismissListener(popupView::dispose);

        imagePopup.setContentView(popupView);
        imagePopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        imagePopup.setOutsideTouchable(true);
        imagePopup.setFocusable(true);
        imagePopup.setAnimationStyle(R.style.ImagePopupAnimation);

        popupView.setBitmap(activity, bitmap);

        popupView.setOnDismiss(imagePopup::dismiss);
        popupView.findViewById(sdk.chat.demo.pre.R.id.content).setOnClickListener(v -> imagePopup.dismiss());

        imagePopup.showAtLocation(view, Gravity.CENTER, 0, 0);
    }

}
