package sdk.chat.demo.robot.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import sdk.chat.ui.chat.model.MessageHolder;
import sdk.chat.ui.view_holders.v2.MessageDirection;
import sdk.chat.ui.views.ChatView;

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

}
