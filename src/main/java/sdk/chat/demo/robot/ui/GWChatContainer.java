package sdk.chat.demo.robot.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.messages.MessageHolders;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import kotlin.Unit;
import sdk.chat.core.dao.Message;
import sdk.chat.core.dao.Thread;
import sdk.chat.core.events.EventType;
import sdk.chat.core.events.NetworkEvent;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.utils.TimeLog;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.adpter.ChatAdapter;
import sdk.chat.demo.robot.handlers.GWThreadHandler;
import sdk.chat.ui.ChatSDKUI;
import sdk.chat.ui.chat.model.MessageHolder;
import sdk.chat.ui.module.UIModule;
import sdk.guru.common.DisposableMap;
import sdk.guru.common.RX;

public class GWChatContainer extends LinearLayout implements MessagesListAdapter.OnLoadMoreListener {

    protected RecyclerView messagesList;
    private LoadMoreSwipeRefreshLayout swipeRefreshLayout;
    protected LinearLayout root;
    protected boolean listenersAdded = false;

    public interface Delegate {
        Thread getThread();

        void onClick(Message message);

        void onLongClick(Message message);

        String getMessageId();
    }

    protected ChatAdapter messagesListAdapter;

    protected List<MessageHolder> messageHolders = new ArrayList<>();

    protected DisposableMap dm = new DisposableMap();


    protected Delegate delegate;
//    protected boolean loadMoreEnabled = true;

    public GWChatContainer(Context context) {
        super(context);
    }

    public GWChatContainer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GWChatContainer(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }


    public void initViews() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_gwchat, this);
//

        messagesList = findViewById(R.id.recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, true);
        messagesList.setLayoutManager(layoutManager);


        root = findViewById(R.id.root);

        final MessageHolders holders = new MessageHolders();
        ChatSDKUI.shared().getMessageRegistrationManager().onBindMessageHolders(getContext(), holders);


        messagesListAdapter = new ChatAdapter();

//        messagesListAdapter.setLoadMoreListener(this);
////        messagesListAdapter.setOnMessageViewClickListener(new GWClickListener());
//
//        messagesListAdapter.setDateHeadersFormatter(date -> {
//            ChatDateProvider provider = ChatSDK.feather().instance(ChatDateProvider.class);
//            if (provider != null) {
//                return provider.from(date);
//            } else {
//                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", CurrentLocale.get());
//                return formatter.format(date);
//            }
//        });

        messagesList.setAdapter(messagesListAdapter);
        setupRefreshLayout();

        onLoadMore(0, 0);

    }

    private void setupRefreshLayout() {
        swipeRefreshLayout = findViewById(R.id.swiperefreshlayout);

        // 下拉刷新监听
        swipeRefreshLayout.setOnRefreshListener(() -> {
            messagesListAdapter.setHeader(false);
            swipeRefreshLayout.setLoadingMore(false);
            loadElder();
        });

        // 绑定RecyclerView
        swipeRefreshLayout.setupWithRecyclerView(messagesList);

        // 上拉加载监听
        swipeRefreshLayout.setOnLoadMoreListener(new LoadMoreSwipeRefreshLayout.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                if (!messagesListAdapter.getHeader()) {
                    messagesListAdapter.setHeader(true);
                    swipeRefreshLayout.setLoadingMore(true);
                    loadLater();
                }
            }
        });
    }

    public void addListeners() {
        if (listenersAdded) {
            return;
        }
        listenersAdded = true;

        dm.add(ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageAdded))
                .subscribe(networkEvent -> {
                    messagesList.post(() -> {
                        addMessageToStart(networkEvent.getMessage());
                    });
                }));

//        dm.add(ChatSDK.events().sourceOnSingle()
//                .filter(NetworkEvent.filterType(EventType.MessageProgressUpdated))
//                .filter(NetworkEvent.filterThreadEntityID(delegate.getThread().getEntityID()))
//                .subscribe(networkEvent -> {
//                    Progress progress = networkEvent.getProgress();
//                    if (progress != null && progress.error != null) {
//                        messagesList.post(() -> {
//                            ToastHelper.show(getContext(), progress.error.getLocalizedMessage());
//                        });
//                    }
//                }));

        dm.add(ChatSDK.events().sourceOnSingle()
                .filter(NetworkEvent.filterType(EventType.MessageRemoved))
                .subscribe(networkEvent -> {
                    messagesList.post(() -> {
                        removeMessage(networkEvent.getMessage());
                    });
                }));
    }

    protected int maxImageWidth() {
        // Prevent overly big messages in landscape mode
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            return Math.min(Math.round(getResources().getDisplayMetrics().widthPixels), UIModule.config().maxImageSize);
        } else {
            return Math.min(Math.round(getResources().getDisplayMetrics().heightPixels), UIModule.config().maxImageSize);
        }
    }


    public void loadLater() {
        Long startId = 0L;

        // If there are already items in the list, load messages before oldest
        if (!messageHolders.isEmpty()) {
            startId = messageHolders.get(0).getMessage().getId();
//        } else {
//            startId = (delegate.getMessageId() != null && delegate.getMessageId() > 0)
//                    ? delegate.getMessageId() -1
//                    : 0L;
        }
        Logger.warn("onLoadLater:" + startId);
        GWThreadHandler handler = (GWThreadHandler) ChatSDK.thread();
        dm.add(
                handler.loadMessagesLater(startId, true)
                        .flatMap((Function<List<Message>, SingleSource<List<MessageHolder>>>) messages -> {
                            return getMessageHoldersAsync(messages, true);
                        })
                        .observeOn(RX.main())
                        .subscribe(messages -> {
                            messagesListAdapter.setHeader(false);
                            swipeRefreshLayout.setRefreshing(false);
                            swipeRefreshLayout.setLoadingMore(false);
                            synchronize(() -> {
                                addMessageToStart(messages);
                            });
                        }, error -> {
                            messagesListAdapter.setHeader(false);
                            swipeRefreshLayout.setRefreshing(false);
                            swipeRefreshLayout.setLoadingMore(false);
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

    public void loadElder() {
        Long startId;

        // If there are already items in the list, load messages before oldest
        if (!messageHolders.isEmpty()) {
            startId = messageHolders.get(messageHolders.size() - 1).getMessage().getId();
        } else {
            startId = 0L;
            String messageId = delegate.getMessageId();
            if (messageId != null && !messageId.isEmpty()) {
                Message msg = ChatSDK.db().fetchMessageWithEntityID(messageId);
                if (msg != null) {
                    startId = msg.getId() + 1;
//                    Toast.makeText(
//                            getContext(),
//                            "准加载id: " + msg.getId(),
//                            Toast.LENGTH_SHORT
//                    ).show();
                }
            }
        }
        Logger.warn("onLoadElder:" + startId);
        GWThreadHandler handler = (GWThreadHandler) ChatSDK.thread();
        dm.add(
                handler.loadMessagesEarlier(startId, true)
                        .flatMap((Function<List<Message>, SingleSource<List<MessageHolder>>>) messages -> {
                            return getMessageHoldersAsync(messages, false);
                        })
                        .observeOn(RX.main())
                        .subscribe(messages -> {
                            synchronize(() -> {
//                                messagesListAdapter.setHeader(false);
                                swipeRefreshLayout.setRefreshing(false);
                                swipeRefreshLayout.setLoadingMore(false);
                                addMessageHoldersToEnd(messages, false);
                            });
                        }, error -> {
//                            messagesListAdapter.setHeader(false);
                            swipeRefreshLayout.setRefreshing(false);
                            swipeRefreshLayout.setLoadingMore(false);
                            Context context = getContext(); // 获取Context
                            if (context != null) {
                                Toast.makeText(
                                        context, error.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }));


    }

    @Override
    public void onLoadMore(int page, int totalItemsCount) {
        loadElder();
    }

    public void loadPendingMsg(){
        String messageId = delegate.getMessageId();

        if (messageId != null && !messageId.isEmpty()) {

        }
    }

    /**
     * Start means new messages to bottom of screen
     */
    protected void addMessageToStart(Message message) {
        Logger.warn("onLoadNew:");

//        boolean scroll = message.getSender().isMe();
//
//        int offset = messagesList.computeVerticalScrollOffset();
//        int extent = messagesList.computeVerticalScrollExtent();
//        int range = messagesList.computeVerticalScrollRange();
//        int distanceFromBottom = range - extent - offset;
//
//        if (distanceFromBottom < 400) {
//            scroll = true;
//        }

        MessageHolder holder = ChatSDKUI.provider().holderProvider().getMessageHolder(message);
        if (!messageHolders.contains(holder)) {

            messageHolders.add(0, holder);

//            updatePreviousMessage(holder);
            holder.updateReadStatus();
            messagesListAdapter.addNewMessage(holder, () -> {
                messagesList.scrollToPosition(0);
                return Unit.INSTANCE;
            });
//            messagesListAdapter.addToStart(holder, scroll, true);
        } else {
            Logger.debug("Exists already");
        }

    }

    protected void addMessageToStart(List<MessageHolder> holders) {
        List<MessageHolder> toAdd = new ArrayList<>();
        for (MessageHolder holder : holders) {
            if (!messageHolders.contains(holder)) {
                messageHolders.add(0, holder);
                toAdd.add(holder);
            } else {
                Logger.error("We have a duplicate");
            }
        }
        messagesListAdapter.addNewMessage(toAdd, () -> {
//            messagesList.scrollToPosition(0);
            LinearLayoutManager layoutManager = (LinearLayoutManager) messagesList.getLayoutManager();
//            int position = layoutManager.findLastVisibleItemPosition();
            assert layoutManager != null;
            layoutManager.scrollToPositionWithOffset(toAdd.size(), 600);
            return Unit.INSTANCE;
        });
    }
//    protected void updatePreviousMessage(MessageHolder holder) {
//        if (holder != null) {
//            MessageHolder previous = ChatSDKUI.provider().holderProvider().getMessageHolder(holder.previousMessage());
//            if (previous != null) {
//                previous.updateNextAndPreviousMessages();
//                if (previous.isDirty()) {
//                    previous.makeClean();
//                    messagesListAdapter.update(previous);
//                }
//            }
//        }
//    }

//    public void update(MessageHolder holder) {
//        messagesListAdapter.update(holder);
//    }

//    protected void updateNextMessage(MessageHolder holder) {
//        MessageHolder next = ChatSDKUI.provider().holderProvider().getMessageHolder(holder.nextMessage());
//        if (next != null) {
//            next.updateNextAndPreviousMessages();
//            if (next.isDirty()) {
//                next.makeClean();
//                messagesListAdapter.update(next);
//            }
//        }
//    }

    protected void removeMessage(Message message) {
        MessageHolder holder = ChatSDKUI.provider().holderProvider().getMessageHolder(message);
        messageHolders.remove(holder);
        messagesListAdapter.delMessage(holder, null);
        ChatSDKUI.provider().holderProvider().removeMessageHolder(message);
    }


    /**
     * End means historic messages to top of screen
     */
    protected void addMessageHoldersToEnd(List<MessageHolder> holders, boolean notify) {
        // Add to current holders at zero index
        // Newest first
        if (holders == null || holders.isEmpty()) {
            return;
        }
        List<MessageHolder> toAdd = new ArrayList<>();
        for (MessageHolder holder : holders) {
            if (!messageHolders.contains(holder)) {
                messageHolders.add(holder);
                toAdd.add(holder);
            } else {
                Logger.error("We have a duplicate");
            }
        }

        // Reverse order because we are adding to end
        LinearLayoutManager layoutManager = (LinearLayoutManager) messagesList.getLayoutManager();
        assert layoutManager != null;
        int position = layoutManager.findLastVisibleItemPosition();

        messagesListAdapter.addHistoryMessages(toAdd, () -> {
            messagesList.setItemAnimator(null);
            layoutManager.scrollToPositionWithOffset(position + 1, 700);

            return Unit.INSTANCE;
        });
//        messagesListAdapter.addToEnd(toAdd, false, notify);
    }

    protected void synchronize(Runnable modifyList) {
        synchronize(modifyList, false);
    }

    protected void synchronize(Runnable modifyList, boolean sort) {

        long start = System.currentTimeMillis();

        if (messagesListAdapter != null) {
//            final List<MessageWrapper<?>> oldHolders = new ArrayList<>(messagesListAdapter.getItems());

            if (modifyList != null) {
                modifyList.run();
            }
//
//            final List<MessageWrapper<?>> newHolders = new ArrayList<>(messagesListAdapter.getItems());
//
////            RX.single().scheduleDirect(() -> {
//            if (sort) {
//                sortMessageHolders();
//            }
//
//            MessageHoldersDiffCallback callback = new MessageHoldersDiffCallback(newHolders, oldHolders);
//            DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback);
//
////                messagesList.post(() -> {
//            Parcelable recyclerViewState = messagesList.getLayoutManager().onSaveInstanceState();
//
//            messagesListAdapter.getItems().clear();
//            messagesListAdapter.getItems().addAll(newHolders);
//
//            result.dispatchUpdatesTo(messagesListAdapter);
//
//            messagesList.getLayoutManager().onRestoreInstanceState(recyclerViewState);
////                });
////            });

        }

        long end = System.currentTimeMillis();
        long diff = end - start;
//        System.out.println("Diff: " + diff);

    }

    public void sortMessageHolders() {
        Collections.sort(messageHolders, (o1, o2) -> {
            return o1.getCreatedAt().compareTo(o2.getCreatedAt());
        });
    }

    public List<MessageHolder> getMessageHolders(final List<Message> messages, boolean reverse) {

        // Get the holders - they will be in asc order i.e. oldest at 0
        TimeLog log = new TimeLog("Get Holders - " + messages.size());

        final List<MessageHolder> holders = new ArrayList<>();
        for (Message message : messages) {
            MessageHolder holder = ChatSDKUI.provider().holderProvider().getMessageHolder(message);
            holders.add(holder);
        }
        if (reverse) {
            Collections.reverse(holders);
        }

        log.end();

        return holders;
    }

    public Single<List<MessageHolder>> getMessageHoldersAsync(final List<Message> messages, boolean reverse) {
        return Single.create((SingleOnSubscribe<List<MessageHolder>>) emitter -> {
            emitter.onSuccess(getMessageHolders(messages, reverse));
        }).subscribeOn(RX.computation()).observeOn(RX.main());
    }

    public void notifyDataSetChanged() {
        messagesListAdapter.notifyDataSetChanged();
    }

    public void clear() {
        if (messagesListAdapter != null) {
            messageHolders.clear();
            messagesListAdapter.submitList(new ArrayList<IMessage>(), null);
        }
    }

    public void scrollToLatest() {
        messagesList.post(() -> {
            messagesList.getLayoutManager().scrollToPosition(0);
        });
    }
//    public void copySelectedMessagesText(Context context, MessagesListAdapter.Formatter<MessageHolder> formatter, boolean reverse) {
//        messagesListAdapter.copySelectedMessagesText(context, formatter, reverse);
//    }
//
//    public void enableSelectionMode(MessagesListAdapter.SelectionListener selectionListener) {
//        messagesListAdapter.enableSelectionMode(selectionListener);
//    }

//    public void filter(final String filter) {
//        if (filter == null || filter.isEmpty()) {
//            clearFilter();
//        } else {
//            final ArrayList<MessageHolder> filtered = new ArrayList<>();
//            for (MessageHolder holder : messageHolders) {
//                if (holder.getText().toLowerCase().contains(filter.trim().toLowerCase())) {
//                    filtered.add(holder);
//                }
//            }
//            synchronize(() -> {
//                messagesListAdapter.getItems().clear();
//                messagesListAdapter.addToEnd(filtered, false, false);
//            });
//        }
//    }

    //    public void clearFilter() {
//        synchronize(() -> {
//            messagesListAdapter.getItems().clear();
//            messagesListAdapter.addToEnd(messageHolders, false, false);
//        });
//    }
    public ChatAdapter getMessagesListAdapter() {
        return messagesListAdapter;
    }

    public void removeListeners() {
        dm.dispose();
        listenersAdded = false;
    }

}
