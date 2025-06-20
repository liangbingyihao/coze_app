package sdk.chat.demo.robot.ui.listener;

import static sdk.chat.demo.MainApp.getContext;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;

import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import java.lang.ref.WeakReference;
import java.util.Date;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import sdk.chat.core.utils.PermissionRequestHandler;
import sdk.chat.demo.MainApp;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.activities.ImageViewerActivity;
import sdk.chat.demo.robot.api.model.ImageDaily;
import sdk.chat.demo.robot.extensions.ImageSaveUtils;
import sdk.chat.demo.robot.handlers.CardGenerator;
import sdk.chat.demo.robot.handlers.GWThreadHandler;
import sdk.chat.demo.robot.holder.DailyGWHolder;
import sdk.chat.demo.robot.holder.ImageHolder;
import sdk.chat.ui.activities.BaseActivity;
import sdk.chat.ui.chat.model.MessageHolder;
import sdk.chat.ui.utils.ToastHelper;

public class GWClickListener<MESSAGE extends IMessage> implements MessagesListAdapter.OnMessageViewClickListener {
    private WeakReference<BaseActivity> weakContext;
    private boolean pending = false;

    public GWClickListener(BaseActivity activity) {
        this.weakContext = new WeakReference<>(activity);
    }

    public static void registerListener(BaseActivity activity, MessagesListAdapter<MessageHolder> adapter) {
        GWClickListener listener = new GWClickListener(activity);
        adapter.registerViewClickListener(R.id.image_container, listener);
        adapter.registerViewClickListener(R.id.btn_download, listener);
        adapter.registerViewClickListener(R.id.btn_share, listener);
    }

    @Override
    public void onMessageViewClick(View view, IMessage imessage) {
        if (this.weakContext == null || this.weakContext.get() == null) {
            return;
        }
        int id = view.getId();
        if (id == R.id.btn_download||id==R.id.btn_share) {
            if (pending) {
                ToastHelper.show(getContext(), getContext().getString(R.string.pending_image));
                return;
            }
            pending = true;
//            ImageHolder imageHolder = (ImageHolder) imessage;
            int resId;
            ImageDaily imageDaily;
            if (imessage.getClass() == DailyGWHolder.class) {
                resId = R.layout.item_image_gw;
                imageDaily = ((DailyGWHolder) imessage).getImageDaily();
            } else {
                ImageHolder holder = (ImageHolder) imessage;
                imageDaily = holder.getImageDaily();
                if (holder.getAction() == GWThreadHandler.action_bible_pic) {
                    resId = R.layout.view_popup_image_bible;
                } else {
                    resId = R.layout.item_image_gw;
                }
            }
            Disposable disposable = PermissionRequestHandler
                    .requestWriteExternalStorage(weakContext.get())
                    .andThen( // After permission is granted, execute the following operations
                            Observable.<Bitmap>create(emitter -> {
                                        CardGenerator.Companion.getInstance()
                                                .generateBibleCard(MainApp.getContext(),
                                                        resId,
                                                        imageDaily,
                                                        result -> {
                                                            emitter.onNext(result); // 发送成功结果
                                                            emitter.onComplete(); // 完成
                                                            return Unit.INSTANCE;

                                                        }, err -> {
                                                            emitter.onError(err);
                                                            return Unit.INSTANCE;
                                                        });

                                    })
                                    .subscribeOn(Schedulers.io())
                    )
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bitmap -> {
                                Uri bitmapURL = ImageSaveUtils.INSTANCE.saveBitmapToGallery(
                                        weakContext.get(), // context
                                        bitmap,
                                        "img_" + System.currentTimeMillis(),
                                        Bitmap.CompressFormat.JPEG
                                );
                                if (bitmapURL != null) {
                                    if (id==R.id.btn_share) {
                                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                        shareIntent.setType("image/*"); // 或具体类型如 "image/jpeg"
                                        shareIntent.putExtra(Intent.EXTRA_STREAM, bitmapURL);
                                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 临时权限

                                        weakContext.get().startActivity(Intent.createChooser(shareIntent, "分享图片"));
                                    }else{
                                        ToastHelper.show(weakContext.get(), weakContext.get().getString(sdk.chat.ui.R.string.image_saved));
                                    }
                                } else {
                                    ToastHelper.show(weakContext.get(), weakContext.get().getString(sdk.chat.ui.R.string.image_save_failed));
                                }
                                bitmap.recycle();
                                pending = false;
                            },
                            e -> {
                                pending = false;
                                weakContext.get().onError(e);
                            }
                    );
            weakContext.get().onSubscribe(disposable);

        } else if (id == R.id.image_container) {
            ImageHolder imageHolder = (ImageHolder) imessage;
            ImageDaily imageDaily = imageHolder.getImageDaily();
            if (imageHolder.getAction() == GWThreadHandler.action_daily_gw) {
                ImageViewerActivity.Companion.start(this.weakContext.get(), imageDaily.getDate());
            } else {
                ImageMessageOnClickHandler.onClick(this.weakContext.get(), view, imageDaily.getBackgroundUrl(), imageDaily.getScripture());
            }
        }
    }
}
