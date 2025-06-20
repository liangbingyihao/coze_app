package sdk.chat.demo.robot.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.lang.ref.WeakReference;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import sdk.chat.core.utils.PermissionRequestHandler;
import sdk.chat.demo.robot.extensions.ImageSaveUtils;
import sdk.chat.demo.robot.handlers.CardGenerator;
import sdk.chat.ui.R;
import sdk.chat.ui.utils.ToastHelper;
import sdk.guru.common.DisposableMap;

public class PopupImageView extends RelativeLayout {


    protected ImageView photoView;
    protected TextView bible;
//    protected ProgressBar progressBar;
    protected ImageView fab;
    protected ImageView share;
    protected View popupView;

    private String cacheKey;
    private View content;
    WeakReference<Activity> weakActivity;
    protected Runnable onDismiss;
    DisposableMap dm = new DisposableMap();

    public PopupImageView(Context context) {
        super(context);
        initViews();
    }

    public PopupImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    public PopupImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews();
    }

    @LayoutRes
    public int getLayout() {
        return sdk.chat.demo.pre.R.layout.view_popup_image_bible;
    }

    public void initViews() {

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(getLayout(), this);

        photoView = findViewById(R.id.photoView);
        bible = findViewById(sdk.chat.demo.pre.R.id.bible);
        fab = findViewById(R.id.fab);
        share = findViewById(sdk.chat.demo.pre.R.id.share);
        content = findViewById(sdk.chat.demo.pre.R.id.content);
        popupView = findViewById(R.id.popupView);

//        fab.setImageDrawable(ChatSDKUI.icons().get(getContext(), ChatSDKUI.icons().save, R.color.fab_icon_color));
        share.setVisibility(View.INVISIBLE);
        fab.setVisibility(View.INVISIBLE);
//        progressBar.setVisibility(View.VISIBLE);
    }

    public void setBitmap(Activity activity, Bitmap bitmap) {
        photoView.setImageBitmap(bitmap);
//        progressBar.setVisibility(View.INVISIBLE);
        fab.setVisibility(View.VISIBLE);
        share.setVisibility(View.VISIBLE);
        fab.setOnClickListener(v1 -> PermissionRequestHandler.requestWriteExternalStorage(activity).subscribe(() -> {
            if (bitmap != null) {
                String bitmapURL = MediaStore.Images.Media.insertImage(activity.getContentResolver(), bitmap, "", "");
                if (bitmapURL != null) {
                    ToastHelper.show(activity, activity.getString(R.string.image_saved));
                } else {
                    ToastHelper.show(activity, activity.getString(R.string.image_save_failed));
                }
            }
        }, throwable -> ToastHelper.show(activity, throwable.getLocalizedMessage())));
        share.setOnClickListener(v -> {
            if (this.cacheKey != null && !cacheKey.isEmpty()) {
                CardGenerator generator = CardGenerator.Companion.getInstance();
                Uri imageUri = generator.getCachedCardUri(cacheKey);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/*"); // 或具体类型如 "image/jpeg"
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 临时权限

                photoView.getContext().startActivity(Intent.createChooser(shareIntent, "分享图片"));
            }
        });
    }

    public void setUrl(Activity activity, String imageUrl,String bible, Runnable dismiss) {
        this.cacheKey = null;

        Glide.with(photoView)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(sdk.chat.demo.pre.R.drawable.icn_200_image_message_placeholder) // 占位图
                .error(sdk.chat.demo.pre.R.drawable.icn_200_image_message_error)
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(
                            @Nullable GlideException e,
                            Object model,
                            Target<Drawable> target,
                            boolean isFirstResource) {
                        ToastHelper.show(activity, "load failed");
                        dismiss.run();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(
                            Drawable resource,
                            Object model,
                            Target<Drawable> target,
                            DataSource dataSource,
                            boolean isFirstResource) {
                        photoView.setImageDrawable(resource);
                        PopupImageView.this.cacheKey = imageUrl;
                        PopupImageView.this.bible.setText(bible);
                        fab.setVisibility(View.VISIBLE);
                        share.setVisibility(View.VISIBLE);
                        weakActivity = new WeakReference<>(activity);
                        return false;
                    }
                })
                .into(photoView);


        fab.setOnClickListener(v1 -> save(false));
        share.setOnClickListener(v1 -> save(true));

    }

    public void dispose() {
        dm.dispose();
    }

    public void setOnDismiss(Runnable onDismiss) {
        this.onDismiss = onDismiss;
    }

    private void save(boolean share){

        Activity activity = weakActivity.get();
        if (activity == null || activity.isFinishing()) {
            return;
        }

        Disposable disposable = PermissionRequestHandler
                .requestWriteExternalStorage(activity)
                .andThen( // After permission is granted, execute the following operations
                        Observable.fromCallable(() -> {
                                    Bitmap bitmap = Bitmap.createBitmap(
                                            content.getWidth(),
                                            content.getHeight(),
                                            Bitmap.Config.ARGB_8888
                                    );

                                    // 2. Draw the View to Bitmap
                                    Canvas canvas = new Canvas(bitmap);
                                    content.draw(canvas);
                                    return bitmap;
                                })
                                .subscribeOn(Schedulers.io())
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        bitmap -> {
                            Uri bitmapURL = ImageSaveUtils.INSTANCE.saveBitmapToGallery(
                                    activity.getApplicationContext(), // context
                                    bitmap,
                                    "img_" + System.currentTimeMillis(),
                                    Bitmap.CompressFormat.JPEG
                            );
                            if (bitmapURL != null) {
                                ToastHelper.show(this.getContext(), activity.getString(R.string.image_saved));
                            } else {
                                ToastHelper.show(this.getContext(), activity.getString(R.string.image_save_failed));
                            }
                            if(share){
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.setType("image/*"); // 或具体类型如 "image/jpeg"
                                shareIntent.putExtra(Intent.EXTRA_STREAM, bitmapURL);
                                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 临时权限

                                photoView.getContext().startActivity(Intent.createChooser(shareIntent, "分享图片"));
                            }
                            bitmap.recycle();
                        },
                        throwable -> {
                            // Handle error
                        }
                );
        dm.add(disposable);
    }
}
