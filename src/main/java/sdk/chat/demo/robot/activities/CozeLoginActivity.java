package sdk.chat.demo.robot.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;

import com.google.android.material.button.MaterialButton;

import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.AccountDetails;
import sdk.chat.core.utils.StringChecker;
import sdk.chat.demo.pre.R;
import sdk.chat.ui.activities.BaseActivity;
import sdk.chat.ui.activities.LoginActivity;
import sdk.chat.ui.module.UIModule;

public class CozeLoginActivity extends LoginActivity {

    MaterialButton advancedConfigurationButton;
    TextView usernameSubtitleTextView;

    protected @LayoutRes
    int getLayout() {
        return R.layout.activity_coze_login;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void initViews() {
        appIconImageView = findViewById(sdk.chat.ui.R.id.appIconImageView);
        usernameTextInput = findViewById(sdk.chat.ui.R.id.usernameTextInput);
        usernameTextInputLayout = findViewById(sdk.chat.ui.R.id.usernameTextInputLayout);
        passwordTextInput = findViewById(sdk.chat.ui.R.id.passwordTextInput);
        passwordTextInputLayout = findViewById(sdk.chat.ui.R.id.passwordTextInputLayout);
        loginButton = findViewById(sdk.chat.ui.R.id.loginButton);
        registerButton = findViewById(sdk.chat.ui.R.id.registerButton);
        anonymousButton = findViewById(sdk.chat.ui.R.id.anonymousButton);
        resetPasswordButton = findViewById(sdk.chat.ui.R.id.resetPasswordButton);
        root = findViewById(sdk.chat.ui.R.id.root);

        resetPasswordButton.setVisibility(UIModule.config().resetPasswordEnabled ? View.VISIBLE : View.INVISIBLE);

        if (!ChatSDK.auth().accountTypeEnabled(AccountDetails.Type.Anonymous)) {
            anonymousButton.setVisibility(View.GONE);
        }

        // Set the debug username and password details for testing
        if (!StringChecker.isNullOrEmpty(ChatSDK.config().debugUsername)) {
            usernameTextInput.setText(ChatSDK.config().debugUsername);
        }
        if (!StringChecker.isNullOrEmpty(ChatSDK.config().debugPassword)) {
            passwordTextInput.setText(ChatSDK.config().debugPassword);
        }

        if (UIModule.config().usernameHint != null) {
            usernameTextInputLayout.setHint(UIModule.config().usernameHint);
        }

        advancedConfigurationButton = findViewById(R.id.advancedConfigurationButton);
        advancedConfigurationButton.setVisibility(View.INVISIBLE);

//
//        usernameSubtitleTextView = findViewById(R.id.usernameSubtitleTextView);
//        advancedConfigurationButton.setOnClickListener(v -> {
////            Intent intent = new Intent(this, XMPPConfigureActivity.class);
////            startActivity(intent);
//        });
//
////        if (XMPPModule.config().allowServerConfiguration) {
////            advancedConfigurationButton.setVisibility(View.VISIBLE);
////        } else {
////        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSubtitle();
    }

    public void updateSubtitle() {
//        XMPPServer server = XMPPManager.getCurrentServer(this);
//        if (server != null && server.isValid()) {
//            usernameSubtitleTextView.setText(String.format(getString(R.string.connecting_to__as__), server.address, server.resource));
//        } else {
//            usernameSubtitleTextView.setText(getString(R.string.no_server_specificed));
//        }
    }


    protected boolean checkFields() {
        boolean valid = super.checkFields();
        if (valid) {
            // Check that there is a valid XMPP server

            // These values will be overridden if the user enters a fully qualified
            // username like user@domain.com:port/resource
            if (!StringChecker.isNullOrEmpty(usernameTextInput.getText())) {
                String username = usernameTextInput.getText().toString();

//                try {
//                    // Get the current server
//                    XMPPServer server = XMPPManager.getCurrentServer(this);
//                    XMPPServerDetails details = new XMPPServerDetails(username);
//
//                    if (server == null) {
//                        server = details.getServer();
//                    }
//
//                    usernameTextInput.setText(details.getUser());
//
//                    // If the server is not hard coded or configured in defaults, then update it from the user
//                    if (server.isValid()) {
//                        XMPPManager.setCurrentServer(this, server);
//                    } else {
//                        showToast(R.string.xmpp_server_must_be_specified);
//                        return false;
//                    }
//
//                    updateSubtitle();
//
//                } catch (Exception e) {
//                    showToast(e.getLocalizedMessage());
//                    return false;
//                }
            }
        }
        return valid;
    }

}
