package sdk.chat.demo.robot.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import sdk.chat.core.dao.Message;
import sdk.chat.core.events.EventType;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.Progress;
import sdk.chat.demo.robot.handlers.GWThreadHandler;
import sdk.chat.ui.ChatSDKUI;
import sdk.chat.ui.chat.model.MessageHolder;
import sdk.chat.ui.utils.ToastHelper;
import sdk.chat.ui.view_holders.v2.MessageDirection;
import sdk.chat.ui.views.ChatView;
import sdk.guru.common.RX;

public class CustomChatView extends ChatView {
    private boolean isAllContent = true;

    public CustomChatView(Context context) {
        super(context);
    }

    public CustomChatView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomChatView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void switchContent() {
        final ArrayList<MessageHolder> filtered = new ArrayList<>();
        if (isAllContent) {
            isAllContent = false;
            for (MessageHolder holder : messageHolders) {
                if (holder.direction() == MessageDirection.Outcoming) {
                    filtered.add(holder);
                }
            }
        } else {
            isAllContent = true;
            filtered.addAll(messageHolders);
        }
        synchronize(() -> {
            messagesListAdapter.getItems().clear();
            messagesListAdapter.addToEnd(filtered, false, false);
        });
    }

    @Override
    public void onLoadMore(int page, int totalItemsCount) {
        // There is an issue with Firebase whereby the message date is
        // initially just estimated. When the message is added, the scrollview
        // scrolls and that triggers the on-load-more which then does a database query
        // while that is running the message date has been updated to a later date
        // so a duplicate message comes back...
        if (!loadMoreEnabled) {
            return;
        }

        Date loadMessagesAfter = delegate.getThread().getLoadMessagesFrom();
        Long startId = 0L;

        // If there are already items in the list, load messages before oldest
        if (!messageHolders.isEmpty()) {
            startId = messageHolders.get(messageHolders.size() - 1).getMessage().getId();
        }
        GWThreadHandler handler = (GWThreadHandler) ChatSDK.thread();
        dm.add(
                // Changed this from before to after because it makes more sense...
                handler.loadMessagesEarlier(startId, true)
                        .flatMap((Function<List<Message>, SingleSource<List<MessageHolder>>>) messages -> {
                            return getMessageHoldersAsync(messages, false);
                        })
                        .observeOn(RX.main())
                        .subscribe(messages -> {
                            synchronize(() -> {
                                addMessageHoldersToEnd(messages, false);
                            });
                        }, error -> {
                            Context context = getContext(); // 获取Context
                            if (context != null) {
                                Toast.makeText(
                                        context,
                                        "加载失败: " + error.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }));

    }


    public void addListeners() {
        if (listenersAdded) {
            return;
        }
        listenersAdded = true;

        dm.add(ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(
                        EventType.ThreadMessagesUpdated
//                        EventType.MessageSendStatusUpdated,
//                        EventType.MessageReadReceiptUpdated
                ))
//                .filter(NetworkEvent.filterThreadEntityID(delegate.getThread().getEntityID()))
                .subscribe(networkEvent -> {

                    Message message = networkEvent.getMessage();
                    if (message != null) {
                        MessageHolder holder = ChatSDKUI.provider().holderProvider().getMessageHolder(message);
                        if (holder != null && !messageHolders.contains(holder)) {
                            Logger.debug("Missing");
                        }
                    }

                    Logger.debug("ChatView: " + networkEvent.debugText());

                    messagesList.post(() -> {
                        synchronize(null, true);
                    });
                }));


//        dm.add(ChatSDK.events().sourceOnMain()
//                .filter(NetworkEvent.filterType(
//                        EventType.UserPresenceUpdated,
//                        EventType.UserMetaUpdated))
//                .filter(NetworkEvent.filterThreadContainsUser(delegate.getThread()))
//                .subscribe(networkEvent -> {
//                    messagesList.post(() -> {
//                        synchronize(null, true);
//                    });
//                }));

        dm.add(ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageAdded))
//                .filter(NetworkEvent.filterThreadEntityID(delegate.getThread().getEntityID()))
                .subscribe(networkEvent -> {

                    if (!ChatSDK.appBackgroundMonitor().inBackground()) {
                        networkEvent.getMessage().markReadIfNecessary();
                    }

                    dm.add(delegate.getThread().markReadAsync().subscribe());

                    messagesList.post(() -> {
                        addMessageToStart(networkEvent.getMessage());
                    });
                }));

        dm.add(ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageProgressUpdated))
                .filter(NetworkEvent.filterThreadEntityID(delegate.getThread().getEntityID()))
                .subscribe(networkEvent -> {
                    Progress progress = networkEvent.getProgress();
                    if (progress != null && progress.error != null) {
                        messagesList.post(() -> {
                            ToastHelper.show(getContext(), progress.error.getLocalizedMessage());
                        });
                    }
                }));

        dm.add(ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageRemoved))
                .filter(NetworkEvent.filterThreadEntityID(delegate.getThread().getEntityID()))
                .subscribe(networkEvent -> {
                    messagesList.post(() -> {
                        removeMessage(networkEvent.getMessage());
                    });
                }));
    }

}
