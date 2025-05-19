package sdk.chat.demo.robot.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import sdk.chat.core.dao.Message;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.ui.chat.model.MessageHolder;
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
                if(holder.direction()== MessageDirection.Outcoming){
                    filtered.add(holder);
                }
            }
        }else{
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
        Date loadMessageBefore = null;

        // If there are already items in the list, load messages before oldest
        if (!messageHolders.isEmpty()) {
            loadMessageBefore = messageHolders.get(messageHolders.size() - 1).getCreatedAt();
        }

        if (loadMessageBefore != null) {
            Logger.debug("Load messages from: " + loadMessageBefore.getTime());
        }

        if (loadMessageBefore != null) {
            dm.add(ChatSDK.thread()
                    // Changed this from before to after because it makes more sense...
                    .loadMoreMessagesBefore(delegate.getThread(), loadMessageBefore, true)
                    .flatMap((Function<List<Message>, SingleSource<List<MessageHolder>>>) messages -> {
                        return getMessageHoldersAsync(messages, false);
                    })
                    .observeOn(RX.main())
                    .subscribe(messages -> {
                        synchronize(() -> {
                            addMessageHoldersToEnd(messages, false);
                        });
                    }));
        } else {
            dm.add(ChatSDK.thread()
                    // Changed this from before to after because it makes more sense...
                    .loadMoreMessagesAfter(delegate.getThread(), loadMessagesAfter, true)
                    .flatMap((Function<List<Message>, SingleSource<List<MessageHolder>>>) messages -> {
                        return getMessageHoldersAsync(messages, false);
                    })
                    .observeOn(RX.main())
                    .subscribe(messages -> {
                        synchronize(() -> {
                            addMessageHoldersToEnd(messages, false);
                        });
                    }));
        }

    }

}
