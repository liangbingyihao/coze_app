package sdk.chat.demo.examples.helper;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.LayoutRes;

import sdk.chat.core.session.ChatSDK;
import sdk.chat.demo.pre.R;
import sdk.chat.ui.fragments.PrivateThreadsFragment;
import sdk.chat.ui.provider.MenuItemProvider;
import sdk.guru.common.RX;

public class CustomPrivateThreadsFragment extends PrivateThreadsFragment {
    protected View newSession;

    // You can customize the layout file here
    @Override
    protected @LayoutRes
    int getLayout() {
        return R.layout.fragment_robot_threads;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        newSession = view.findViewById(R.id.newSession);
        newSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dm.add(ChatSDK.thread()
                        .createThread("", ChatSDK.contact().contacts())
                        .observeOn(RX.main())
                        .subscribe(thread -> {
                            ChatSDK.ui().startChatActivityForID(v.getContext(), thread.getEntityID());
                        }, CustomPrivateThreadsFragment.this));
            }
        });
        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        /* Cant use switch in the library*/
        int id = item.getItemId();

        if (id == MenuItemProvider.addItemId) {
//            ChatSDK.ui().startCreateThreadActivity(getContext());
            dm.add(ChatSDK.thread()
                    .createThread("", ChatSDK.contact().contacts())
                    .observeOn(RX.main())
                    .subscribe(thread -> {
                        ChatSDK.ui().startChatActivityForID(this.getContext(), thread.getEntityID());
                    }, this));

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void synchronize(boolean sort) {
        super.synchronize(sort);
        if(newSession!=null){
            if(!threadHolders.isEmpty()){
                newSession.setVisibility(View.GONE);
            }else{
                newSession.setVisibility(View.VISIBLE);
            }
        }
    }

}
