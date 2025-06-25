package sdk.chat.demo.robot.ui.listener;

import static sdk.chat.demo.MainApp.getContext;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;

import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import java.lang.ref.WeakReference;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import sdk.chat.core.dao.Keys;
import sdk.chat.core.dao.Message;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.utils.PermissionRequestHandler;
import sdk.chat.demo.MainApp;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.activities.ImageViewerActivity;
import sdk.chat.demo.robot.api.model.AIFeedback;
import sdk.chat.demo.robot.api.model.ImageDaily;
import sdk.chat.demo.robot.extensions.ImageSaveUtils;
import sdk.chat.demo.robot.handlers.CardGenerator;
import sdk.chat.demo.robot.handlers.GWThreadHandler;
import sdk.chat.demo.robot.holder.DailyGWHolder;
import sdk.chat.demo.robot.holder.ImageHolder;
import sdk.chat.demo.robot.holder.TextHolder;
import sdk.chat.ui.activities.BaseActivity;
import sdk.chat.ui.chat.model.MessageHolder;
import sdk.chat.ui.utils.ToastHelper;

public class GWClickListener<MESSAGE extends IMessage> implements MessagesListAdapter.OnMessageViewClickListener {
    private final WeakReference<BaseActivity> weakContext;
    private WeakReference<TTSSpeaker> ttsSpeaker;
    private boolean pending = false;

    public interface TTSSpeaker {
        void speek(String text, String msgId);

        String getCurrentUtteranceId();

        void stop();
    }

    public GWClickListener(BaseActivity activity) {
        this.weakContext = new WeakReference<>(activity);
        if (activity instanceof TTSSpeaker) {
            this.ttsSpeaker = new WeakReference<>((TTSSpeaker) activity);
        }
    }

    public static void registerListener(BaseActivity activity, MessagesListAdapter<MessageHolder> adapter) {
        GWClickListener listener = new GWClickListener(activity);
        adapter.registerViewClickListener(R.id.image_container, listener);
        adapter.registerViewClickListener(R.id.btn_download, listener);
        adapter.registerViewClickListener(R.id.btn_share_image, listener);
        adapter.registerViewClickListener(R.id.btn_share_text, listener);
        adapter.registerViewClickListener(R.id.btn_play, listener);
        adapter.registerViewClickListener(R.id.btn_copy, listener);
        adapter.registerViewClickListener(R.id.btn_pic, listener);
        adapter.registerViewClickListener(R.id.btn_del, listener);
    }

    @Override
    public void onMessageViewClick(View view, IMessage imessage) {
        if (this.weakContext.get() == null) {
            return;
        }
        int id = view.getId();

        int resId;
        ImageDaily imageDaily;
        if (imessage.getClass() == TextHolder.class) {
            TextHolder t = (TextHolder) imessage;
            resId = R.layout.view_popup_image_bible;
            imageDaily = new ImageDaily(t.getAiFeedback().getFeedback().getBible(), t.message.stringForKey(Keys.ImageUrl));
//                Intent shareIntent = new Intent(Intent.ACTION_SEND);
//                shareIntent.setType("text/plain"); // 或具体类型如 "image/jpeg"
//                shareIntent.putExtra(Intent.EXTRA_TEXT, t.getAiFeedback().getFeedbackText());
//                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "恩语之声"); // 临时权限
//
//                weakContext.get().startActivity(Intent.createChooser(shareIntent, "分享到"));
//                return;
        } else if (imessage.getClass() == DailyGWHolder.class) {
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

        if (id == R.id.btn_share_text) {
            if (imessage.getClass() == TextHolder.class) {
                TextHolder t = (TextHolder) imessage;
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain"); // 或具体类型如 "image/jpeg"
                shareIntent.putExtra(Intent.EXTRA_TEXT, t.getAiFeedback().getFeedbackText());
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "恩语之声"); // 临时权限

                weakContext.get().startActivity(Intent.createChooser(shareIntent, "分享到"));
            }
        } else if (id == R.id.btn_download || id == R.id.btn_share_image) {
            if (pending) {
                ToastHelper.show(getContext(), getContext().getString(R.string.pending_image));
                return;
            }
//            ImageHolder imageHolder = (ImageHolder) imessage;

            pending = true;
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
                                    if (id == R.id.btn_share_image) {
                                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                        shareIntent.setType("image/*"); // 或具体类型如 "image/jpeg"
                                        shareIntent.putExtra(Intent.EXTRA_STREAM, bitmapURL);
                                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 临时权限

                                        weakContext.get().startActivity(Intent.createChooser(shareIntent, "分享图片"));
                                    } else {
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
//            ImageHolder imageHolder = (ImageHolder) imessage;
//            ImageDaily imageDaily = imageHolder.getImageDaily();
            if (resId == R.layout.item_image_gw) {
                ImageViewerActivity.Companion.start(this.weakContext.get(), imageDaily.getDate());
            } else {
                ImageMessageOnClickHandler.onClick(this.weakContext.get(), view, imageDaily.getBackgroundUrl(), imageDaily.getScripture());
            }
        } else if (id == R.id.btn_play) {
            if (imessage.getClass() == TextHolder.class) {
                GWThreadHandler threadHandler = (GWThreadHandler) ChatSDK.thread();
                if (ttsSpeaker != null && ttsSpeaker.get() != null) {
                    TextHolder holder = (TextHolder) imessage;
                    if (holder.message.equals(threadHandler.getPlayingMsg())) {
                        ttsSpeaker.get().stop();
                        threadHandler.setPlayingMsg(null);
                    } else {
                        boolean change = threadHandler.setPlayingMsg(holder.message);
                        if (change) {
                            ttsSpeaker.get().speek(holder.getAiFeedback().getFeedbackText(), holder.message.getId().toString());
                        }
                    }
                }
            }
        } else if (id == R.id.btn_copy) {
            if (this.weakContext.get() != null && imessage.getClass() == TextHolder.class) {
                ClipboardManager clipboard = (ClipboardManager) this.weakContext.get().getSystemService(Context.CLIPBOARD_SERVICE);
                TextHolder holder = (TextHolder) imessage;
                ClipData clip = ClipData.newPlainText("恩语", holder.getAiFeedback().getFeedbackText());
                clipboard.setPrimaryClip(clip);
                ToastHelper.show(getContext(), getContext().getString(R.string.copied));
            }
        } else if (id == R.id.btn_pic) {
            if (this.weakContext.get() != null && imessage.getClass() == TextHolder.class) {
                TextHolder t = (TextHolder) imessage;
                GWThreadHandler threadHandler = (GWThreadHandler) ChatSDK.thread();
                AIFeedback feedback = t.getAiFeedback().getFeedback();
                threadHandler.sendExploreMessage(
                        "",
                        t.message,
                        GWThreadHandler.action_bible_pic,
                        feedback.getTag() + "|" + feedback.getBible()
                ).subscribe();
            }
        } else if (id == R.id.btn_del) {
            if (imessage.getClass() == TextHolder.class) {
                TextHolder t = (TextHolder) imessage;
                GWThreadHandler threadHandler = (GWThreadHandler) ChatSDK.thread();
                threadHandler.clearFeedbackText(t.message);
            }
        }
    }
}
