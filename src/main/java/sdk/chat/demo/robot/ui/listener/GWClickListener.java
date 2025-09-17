package sdk.chat.demo.robot.ui.listener;

import static sdk.chat.demo.MainApp.getContext;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;

import com.stfalcon.chatkit.commons.models.IMessage;

import java.lang.ref.WeakReference;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import sdk.chat.core.dao.Keys;
import sdk.chat.core.dao.Message;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.rigs.MessageSendRig;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.utils.PermissionRequestHandler;
import sdk.chat.demo.MainApp;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.activities.ArticleListActivity;
import sdk.chat.demo.robot.activities.ImageViewerActivity;
import sdk.chat.demo.robot.adpter.ChatAdapter;
import sdk.chat.demo.robot.adpter.data.AIExplore;
import sdk.chat.demo.robot.api.model.AIFeedback;
import sdk.chat.demo.robot.api.model.ImageDaily;
import sdk.chat.demo.robot.api.model.MessageDetail;
import sdk.chat.demo.robot.audio.TTSHelper;
import sdk.chat.demo.robot.extensions.ActivityExtensionsKt;
import sdk.chat.demo.robot.extensions.ImageSaveUtils;
import sdk.chat.demo.robot.handlers.CardGenerator;
import sdk.chat.demo.robot.handlers.GWThreadHandler;
import sdk.chat.demo.robot.holder.DailyGWHolder;
import sdk.chat.demo.robot.holder.ImageHolder;
import sdk.chat.demo.robot.holder.TextHolder;
import sdk.chat.ui.activities.BaseActivity;
import sdk.chat.ui.utils.ToastHelper;
import sdk.guru.common.RX;

public class GWClickListener<MESSAGE extends IMessage> implements ChatAdapter.OnMessageViewClickListener {
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

    public static void registerListener(BaseActivity activity, ChatAdapter adapter) {
        GWClickListener listener = new GWClickListener(activity);
        adapter.registerViewClickListener(R.id.image_container, listener);
        adapter.registerViewClickListener(R.id.btn_download, listener);
        adapter.registerViewClickListener(R.id.btn_share_image, listener);
        adapter.registerViewClickListener(R.id.btn_share_text, listener);
        adapter.registerViewClickListener(R.id.btn_play, listener);
        adapter.registerViewClickListener(R.id.btn_pray, listener);
        adapter.registerViewClickListener(R.id.btn_pic, listener);
        adapter.registerViewClickListener(R.id.btn_del, listener);
        adapter.registerViewClickListener(R.id.btn_redo, listener);
        adapter.registerViewClickListener(R.id.btn_like_ai, listener);
        adapter.registerViewClickListener(R.id.btn_copy_user_text, listener);
        adapter.registerViewClickListener(R.id.btn_like_user_text, listener);
        adapter.registerViewClickListener(R.id.btn_share_user_text, listener);
        adapter.registerViewClickListener(R.id.btn_del_user_text, listener);
        adapter.registerViewClickListener(R.id.session_name, listener);
        adapter.registerViewClickListener(R.id.process_container, listener);
        adapter.registerViewClickListener(R.id.reply_error_hint, listener);
        adapter.registerViewClickListener(R.id.send_error_hint, listener);
    }

    @Override
    public void onMessageViewClick(View view, IMessage imessage) {
        if (this.weakContext.get() == null) {
            return;
        }
        int id = view.getId();

        int resId;
        ImageDaily imageDaily;
        MessageDetail aiFeedback = null;
        Message message;
        GWThreadHandler threadHandler = (GWThreadHandler) ChatSDK.thread();
        if (imessage.getClass() == TextHolder.class) {
            TextHolder t = (TextHolder) imessage;
            message = t.message;
            aiFeedback = t.getAiFeedback();
            resId = R.layout.view_popup_image_bible;
            if (aiFeedback != null && aiFeedback.getFeedback() != null) {
                imageDaily = new ImageDaily(aiFeedback.getFeedback().getBible(), message.stringForKey(Keys.ImageUrl));
            } else {
                imageDaily = null;
            }
//                Intent shareIntent = new Intent(Intent.ACTION_SEND);
//                shareIntent.setType("text/plain"); // 或具体类型如 "image/jpeg"
//                shareIntent.putExtra(Intent.EXTRA_TEXT, t.getAiFeedback().getFeedbackText());
//                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "恩语之声"); // 临时权限
//
//                weakContext.get().startActivity(Intent.createChooser(shareIntent, "分享到"));
//                return;
        } else if (imessage.getClass() == DailyGWHolder.class) {
            message = null;
            resId = R.layout.item_image_gw;
            imageDaily = ((DailyGWHolder) imessage).getImageDaily();
        } else {
            ImageHolder holder = (ImageHolder) imessage;
            message = holder.message;
            imageDaily = holder.getImageDaily();
            if (holder.getAction() == AIExplore.ExploreItem.action_bible_pic) {
                resId = R.layout.view_popup_image_bible;
            } else {
                resId = R.layout.item_image_gw;
            }
        }

        if (message != null && "welcome".equals(message.getEntityID())
                && (id == R.id.btn_del || id == R.id.btn_play || id == R.id.btn_like_ai || id == R.id.btn_redo)) {
            ToastHelper.show(getContext(), getContext().getString(R.string.error_msg_type));
            return;
        }

        if (id == R.id.btn_share_text || id == R.id.btn_share_user_text) {
            if (imessage.getClass() == TextHolder.class) {
                String copyText = id == R.id.btn_share_text && aiFeedback != null ? aiFeedback.getFeedbackText() : message.getText();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain"); // 或具体类型如 "image/jpeg"
                shareIntent.putExtra(Intent.EXTRA_TEXT, copyText);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "恩语之声"); // 临时权限

                weakContext.get().startActivity(Intent.createChooser(shareIntent, "分享到"));
            }
        } else if (id == R.id.btn_download || id == R.id.btn_share_image) {
            if (pending) {
                ToastHelper.show(getContext(), getContext().getString(R.string.pending_image));
                return;
            }
            if (imageDaily == null) {
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
            } else if (imageDaily != null) {
                ImageMessageOnClickHandler.onClick(this.weakContext.get(), view, imageDaily.getBackgroundUrl(), imageDaily.getScripture());
            }
        } else if (id == R.id.btn_play) {
            if (imessage.getClass() == TextHolder.class) {
                if (message.equals(TTSHelper.INSTANCE.getPlayingMsg())) {
                    TTSHelper.INSTANCE.stop();
                } else {
                    boolean change = TTSHelper.INSTANCE.setPlayingMsg(message);
                    if (change && aiFeedback != null) {
                        TTSHelper.INSTANCE.speek(aiFeedback.getFeedbackText(), message.getId().toString());
                    }
                }
            }
        } else if (id == R.id.btn_copy_user_text) {
            if (this.weakContext.get() != null && imessage.getClass() == TextHolder.class) {
                ClipboardManager clipboard = (ClipboardManager) this.weakContext.get().getSystemService(Context.CLIPBOARD_SERVICE);
//                TextHolder holder = (TextHolder) imessage;
//                String copyText = holder.getText();
                ClipData clip = ClipData.newPlainText("恩语", message.getText());
                clipboard.setPrimaryClip(clip);
                ToastHelper.show(getContext(), getContext().getString(R.string.copied));
            }
        } else if (id == R.id.btn_pic) {
            if (this.weakContext.get() != null && imessage.getClass() == TextHolder.class) {
                TextHolder t = (TextHolder) imessage;
                AIFeedback feedback = t.getAiFeedback().getFeedback();
                threadHandler.sendExploreMessage(
                        "",
                        t.message,
                        AIExplore.ExploreItem.action_bible_pic,
                        feedback.getTag() + "|" + feedback.getBible()
                ).subscribe();
            }
        } else if (id == R.id.btn_del || id == R.id.btn_del_user_text) {
            if (imessage.getClass() == TextHolder.class && message != null) {
                Context context = this.weakContext.get();
                ActivityExtensionsKt.showMaterialConfirmationDialog(context,  context.getString(R.string.delete_confirm),null,null, () -> {
                    Single<Boolean> r = id == R.id.btn_del ? threadHandler.clearFeedbackText(message) : threadHandler.clearUserText(message);
                    weakContext.get().onSubscribe(r.observeOn(AndroidSchedulers.mainThread())
                            .subscribe(result -> {
                                        if (!result) {
                                            ToastHelper.show(
                                                    weakContext.get(),
                                                    weakContext.get().getString(R.string.failed_and_retry)
                                            );
                                        }
                                    },
                                    error -> {
                                        weakContext.get().onError(error);
                                    }));
                    return Unit.INSTANCE;
                });

            }
        } else if (id == R.id.btn_like_ai || id == R.id.btn_like_user_text) {
            if (imessage.getClass() == TextHolder.class && message != null) {
                Single<Integer> r = id == R.id.btn_like_ai ? threadHandler.toggleAiLikeState(message) : threadHandler.toggleContentLikeState(message);
                Disposable disposable = r.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(newState -> {
                                    if (newState == 0) {
                                        ToastHelper.show(
                                                weakContext.get(),
                                                weakContext.get().getString(R.string.unsaved)
                                        );
                                    } else if (newState == 1) {
                                        ToastHelper.show(
                                                weakContext.get(),
                                                weakContext.get().getString(R.string.saved)
                                        );
                                    }
                                },
                                error -> {
                                    weakContext.get().onError(error);
                                });
                weakContext.get().onSubscribe(disposable);
            }
        } else if (id == R.id.btn_redo) {
            if (imessage.getClass() == TextHolder.class) {
                if (aiFeedback != null) {
                    aiFeedback.setStatus(1);
                    ChatSDK.events().source().accept(NetworkEvent.messageUpdated(message));
                }
                weakContext.get().onSubscribe(threadHandler.renew(message)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(ret -> {
                                },
                                error -> {
                                    weakContext.get().onError(error);
                                }));
            }
        } else if (id == R.id.session_name) {
            if (message != null) {
                ArticleListActivity.Companion.start(weakContext.get(), message.getThreadId().toString());
            }
        } else if (id == R.id.btn_pray) {
            String params = null;
            if (imageDaily != null) {
                params = imageDaily.getScripture();
            } else if (aiFeedback != null && aiFeedback.getFeedback() != null) {
                params = aiFeedback.getFeedback().getBible();
            }
            threadHandler.sendExploreMessage(
                    "",
                    message,
                    AIExplore.ExploreItem.action_daily_pray,
                    params
            ).subscribe();
        } else if (id == R.id.reply_error_hint || id == R.id.send_error_hint) {
            assert message != null;
            view.setVisibility(View.INVISIBLE);
            Completable completable = new MessageSendRig(message).run();
            completable.observeOn(RX.main()).doOnError(throwable -> {
                ToastHelper.show(
                        weakContext.get(),
                        throwable.getLocalizedMessage()
                );
                view.setVisibility(View.VISIBLE);
            }).subscribe(weakContext.get());
        }
    }
}
