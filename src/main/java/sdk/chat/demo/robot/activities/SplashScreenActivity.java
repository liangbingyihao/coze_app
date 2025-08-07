package sdk.chat.demo.robot.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.ui.LifecycleService;
import sdk.chat.core.utils.StringChecker;
import sdk.chat.demo.MainApp;
import sdk.chat.demo.pre.R;
import sdk.chat.demo.robot.api.GWApiManager;
// 基础导入
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

// 版本检测
import android.os.Build;

// Android 4.4+ (KitKat) 沉浸式模式
import android.view.WindowInsets;

// Android 11+ (R) 新API
import android.view.WindowInsetsController;

// 如果使用兼容库（可选）
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.gyf.immersionbar.ImmersionBar;

public class SplashScreenActivity extends AppCompatActivity {

    public static int AUTH = 1;

    protected ProgressBar progressBar;
    protected ConstraintLayout root;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImmersionBar.with(this).init();

        setContentView(R.layout.activity_splash_gw);
        startService(new Intent(getBaseContext(), LifecycleService.class));

//        // TODO: This better me imageView and not image_view.
//        imageView = findViewById(R.id.imageView);
//        progressBar = findViewById(R.id.progressBar);
//        root = findViewById(R.id.root);
//
//
//        imageView.setImageResource(sdk.chat.demo.pre.R.mipmap.ic_splash);

//        stopProgressBar();

//        if (getActionBar() != null) {
//            getActionBar().hide();
//        }

        checkInitializationStatus();
    }




    private void checkInitializationStatus() {
        MainApp app = (MainApp) getApplication();

        if (app.isInitialized()) {

            boolean hasShownGuide = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getBoolean("has_shown_guide", false);
            if (hasShownGuide) {
                new Handler().postDelayed(this::proceedToMain, 500);
            } else {
                startActivity(new Intent(this, GuideActivity.class));
                finish();
            }
        } else {
            // 每隔500ms检查一次
            new Handler().postDelayed(this::checkInitializationStatus, 200);
        }
    }

    private void proceedToMain() {
        // 确保主界面启动前所有服务就绪
        if (ChatSDK.db().getDaoCore()!=null) {
            startActivity(new Intent(this, MainDrawerActivity.class));
            finish();
        } else {
            showErrorAndRetry();
        }
    }

    private void showErrorAndRetry() {
        new AlertDialog.Builder(this)
                .setTitle("初始化失败")
                .setMessage("无法启动应用，请重试")
                .setPositiveButton("重试", (d, w) -> checkInitializationStatus())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onResume() {
        Log.i("FATAL", "SplashScreenActivity.onResume");
        super.onResume();
//        if (ChatSDK.shared().isActive()) {
//            startNextActivity();
//        } else {
//            ChatSDK.shared().addOnActivateListener(() -> {
//                startNextActivity();
//            });
//        }
    }

//    protected void startNextActivity() {
//
//        stopProgressBar();
//        if (ChatSDK.auth() != null) {
//            if (ChatSDK.auth().isAuthenticatedThisSession()) {
//                Log.i("FATAL","SplashScreenActivity.startMainActivity");
//                startMainActivity();
//                return;
//            } else if (ChatSDK.auth().cachedCredentialsAvailable()) {
//                startProgressBar();
//                Log.i("FATAL","SplashScreenActivity.authenticate");
//                dm.add(ChatSDK.auth().authenticate()
//                        .observeOn(RX.main())
//                        .doFinally(this::stopProgressBar)
//                        .subscribe(this::startMainActivity, throwable -> startLoginActivity()));
//                return;
//            }
//        }
//        startLoginActivity();
//    }

    protected void startMainActivity() {
        if (StringChecker.isNullOrEmpty(ChatSDK.currentUser().getName())) {
            ChatSDK.ui().startPostRegistrationActivity(this, null);
        } else {
            ChatSDK.ui().startMainActivity(this, null);
        }
    }

    protected void startLoginActivity() {
        startActivityForResult(ChatSDK.ui().getLoginIntent(this, null), AUTH);
    }

    protected void startProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        progressBar.animate();
    }

    protected void stopProgressBar() {
        progressBar.setVisibility(View.GONE);
        progressBar.setProgress(0);
        progressBar.setIndeterminate(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("FATAL", "SplashScreenActivity.onDestroy");
    }
}
