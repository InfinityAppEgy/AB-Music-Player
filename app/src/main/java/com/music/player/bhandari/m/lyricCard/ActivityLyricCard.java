package com.music.player.bhandari.m.lyricCard;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.FontsContract;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.provider.FontRequest;
import android.support.v4.provider.FontsContractCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.music.player.bhandari.m.MyApp;
import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElementHelper.ColorHelper;
import com.music.player.bhandari.m.activity.ActivityPermissionSeek;
import com.music.player.bhandari.m.customViews.ZoomTextView;
import com.music.player.bhandari.m.model.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Amit AB AB on 17-Apr-18.
 *
 * I know, I know, there is not even bit of "Good Architecture" is followed in this class
 * Everything is mixed up, UI, data, card creation. Everything.
 * But its too late for this app to follow architectural guidelines.
 * You agree with me? Cheers.
 * If not. May god help you.
 *
 */

public class ActivityLyricCard extends AppCompatActivity implements View.OnTouchListener  {

    private final int PICK_IMAGE = 0;

    @BindView(R.id.rv_colors) RecyclerView recyclerViewColors;
    @BindView(R.id.rv_images) RecyclerView recyclerViewImages;
    @BindView(R.id.mainImageLyricCard) ImageView mainImage;
    @BindView(R.id.text_lyric) ZoomTextView lyricText;
    @BindView(R.id.text_artist) ZoomTextView artistText;
    @BindView(R.id.dragView) View dragView;
    @BindView(R.id.progressBar) ProgressBar progressBar;
    @BindView(R.id.brightnessSeekBar) SeekBar brightnessSeekBar;
    @BindView(R.id.overImageLayer) View overImageLayer;

    float dx;
    float dy;

    private ImagesAdapter imagesAdapter = new ImagesAdapter();
    private Handler mHandler;

    private int currentTextAlignment=0;
    private List<Typeface> typefaces = new ArrayList<>();
    int currentFontPosition = 0;

    private final static int DAYS_UNTIL_CACHE = 5;//Min number of days

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        //if player service not running, kill the app
        if(MyApp.getService()==null){
            Intent intent = new Intent(this, ActivityPermissionSeek.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }

        int themeSelector = MyApp.getPref().getInt(getString(R.string.pref_theme), Constants.PRIMARY_COLOR.LIGHT);
        switch (themeSelector){
            case Constants.PRIMARY_COLOR.DARK:
                setTheme(R.style.AppThemeDark);
                break;

            case Constants.PRIMARY_COLOR.GLOSSY:
                setTheme(R.style.AppThemeDark);
                break;

            case Constants.PRIMARY_COLOR.LIGHT:
                setTheme(R.style.AppThemeLight);
                break;
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lyric_card);
        ButterKnife.bind(this);


        if(getIntent().getExtras()==null){
            Toast.makeText(this, "Missing lyric text", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String text;
        String author;
        if(getIntent().getExtras().getString("lyric")!=null){
            text = getIntent().getExtras().getString("lyric");
        }else {
            text = "";
        }

        if(getIntent().getExtras().getString("artist")!=null){
            author = getIntent().getExtras().getString("artist");
        }else {
            author = "";
        }

        lyricText.setText(text);
        artistText.setText(author);

        Log.d("ActivityLyricCard", "onCreate: lyric " + lyricText.getText());
        Log.d("ActivityLyricCard", "onCreate: artist " + artistText.getText());

        findViewById(R.id.root_view_lyric_card).setBackgroundDrawable(ColorHelper.getColoredThemeGradientDrawable());

        Toolbar toolbar = findViewById(R.id.toolbar_);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ColorHelper.getPrimaryColor()));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ColorHelper.getDarkPrimaryColor());
        }

        setTitle("Lyric Card");

        fillFonts();
        initiateUI();
    }

    private void initiateUI(){
        recyclerViewColors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewColors.setAdapter(new ColorAdapter());

        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewImages.setAdapter(imagesAdapter);

        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        ArrayList<String> urls;
        urls = new Gson().fromJson(MyApp.getPref().getString(getString(R.string.pref_card_image_links), ""), type);
        if(System.currentTimeMillis() >= MyApp.getPref().getLong(getString(R.string.pref_card_image_saved_at), 0) + DAYS_UNTIL_CACHE
                && urls!=null){
            imagesAdapter.setUrls(urls);
        }else {
            FirebaseDatabase.getInstance().getReference().child("cardLinks").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d("ActivityLyricCard", "onDataChange: ");
                    ArrayList<String> urls = new ArrayList<>();
                    for (DataSnapshot snap : dataSnapshot.getChildren()) {
                        urls.add(snap.getValue(String.class));
                    }
                    imagesAdapter.setUrls(urls);

                    //cache links in shared pref
                    MyApp.getPref().edit().putString(getString(R.string.pref_card_image_links), new Gson().toJson(urls)).apply();
                    MyApp.getPref().edit().putLong(getString(R.string.pref_card_image_saved_at), System.currentTimeMillis()).apply();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d("ActivityLyricCard", "onCancelled: " + databaseError.getMessage());
                }
            });
        }

        initiateDragView();

        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int in, boolean b) {
                double i = seekBar.getProgress()/100.0d;
                int alpha = (int) Math.round(i * 255);
                String hex = Integer.toHexString(alpha).toUpperCase();
                if (hex.length() == 1) hex = "0" + hex;
                try {
                    overImageLayer.setBackgroundColor(Color.parseColor("#" + hex + "000000"));
                }catch (NumberFormatException e){
                    Log.d("ActivityLyricCard", "onProgressChanged: Color parse exception");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void initiateDragView(){
        artistText.setOnTouchListener(this);
        lyricText.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN :
                dx = v.getX() - event.getRawX();
                dy = v.getY() - event.getRawY();
                break;

            case MotionEvent.ACTION_MOVE :
                switch (v.getId()){
                    case R.id.text_artist:
                        artistText.animate()
                                .x(event.getRawX() + dx)
                                .y(event.getRawY() + dy)
                                .setDuration(0)
                                .start();
                        break;

                    case R.id.text_lyric:
                        lyricText.animate()
                            .x(event.getRawX() + dx)
                            .y(event.getRawY() + dy)
                            .setDuration(0)
                            .start();
                        break;
                }
                break;

            default: return false;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lyric_card, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;

            case R.id.action_share:
                shareCard();
                break;

            case R.id.action_font:
                changeFont();
                break;

            case R.id.action_alignment:
                changeAlignment();
                break;

            case R.id.action_save:
                File f = createImageFile(false);
                if(f!=null) {
                    Toast.makeText(this, "Lyric card is saved at " +f.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private File createImageFile(Boolean temp){
        dragView.destroyDrawingCache();  //if not done, image is going to be overridden every time
        dragView.setDrawingCacheEnabled(true);
        Bitmap bitmap = dragView.getDrawingCache();

        File dir =new File(Environment.getExternalStorageDirectory().toString() + "/abmusic");
        dir.mkdirs();

        String fileName = "temp.jpeg";
        if(!temp){
            fileName = UUID.randomUUID().toString()+".jpeg";
        }

        File lyricCardFile = new File(dir, fileName);
        if(lyricCardFile.exists()) lyricCardFile.delete();

        try{
            FileOutputStream stream = new FileOutputStream(lyricCardFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.flush();
            stream.close();
        }catch (Exception e){
            Log.d("ActivityLyricCard", "shareCard: " + e.getLocalizedMessage());
            Toast.makeText(this, "Error saving card on storage ", Toast.LENGTH_SHORT).show();
        }
        return  lyricCardFile;
    }

    private void shareCard(){
        File lyricCardFile = createImageFile(true);
        if(lyricCardFile==null) return;
        try {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/*");
            share.putExtra(Intent.EXTRA_STREAM
                    , FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + "com.bhandari.music.provider", lyricCardFile));
            startActivity(Intent.createChooser(share, "Share Lyric Card"));
        }catch (Exception e){
            Toast.makeText(this, "Error while sharing, lyric card is saved at " + lyricCardFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    private void fillFonts(){
        requestDownload("Aref Ruqaa");
        requestDownload("Bubblegum Sans");
        requestDownload("Lancelot");
    }

    private void requestDownload(String familyName) {
        QueryBuilder queryBuilder = new QueryBuilder(familyName);
        String query = queryBuilder.build();

        FontRequest request = new FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                query,
                R.array.com_google_android_gms_fonts_certs);

        FontsContractCompat.FontRequestCallback callback = new FontsContractCompat
                .FontRequestCallback() {
            @Override
            public void onTypefaceRetrieved(Typeface typeface) {
                Log.d("ActivityLyricCard", "onTypefaceRetrieved: " + typeface.toString());
                typefaces.add(typeface);
            }

            @Override
            public void onTypefaceRequestFailed(int reason) {

            }
        };
        FontsContractCompat
                .requestFont(this, request, callback,
                        getHandlerThreadHandler());
    }

    private Handler getHandlerThreadHandler() {
        if (mHandler == null) {
            HandlerThread handlerThread = new HandlerThread("fonts");
            handlerThread.start();
            mHandler = new Handler(handlerThread.getLooper());
        }
        return mHandler;
    }

    private void changeFont(){
        if(currentFontPosition>=typefaces.size()-1){
            currentFontPosition=0;
            lyricText.setTypeface(typefaces.get(currentFontPosition));
            artistText.setTypeface(typefaces.get(currentFontPosition));
        }else {
            int index=++currentFontPosition;
            lyricText.setTypeface(typefaces.get(index));
            artistText.setTypeface(typefaces.get(index));
        }
    }

    private void changeAlignment(){
        switch (currentTextAlignment){
            case 0:
                lyricText.setGravity(Gravity.CENTER);
                artistText.setGravity(Gravity.CENTER);
                currentTextAlignment = 1;
                break;

            case 1:
                lyricText.setGravity(Gravity.RIGHT);
                artistText.setGravity(Gravity.RIGHT);
                currentTextAlignment = 2;
                break;

            case 2:
                lyricText.setGravity(Gravity.LEFT);
                artistText.setGravity(Gravity.LEFT);
                currentTextAlignment = 0;
                break;

            default:
                lyricText.setGravity(Gravity.CENTER);
                artistText.setGravity(Gravity.CENTER);
                currentTextAlignment = 1;
                break;
        }
    }

    private void setMainImage(String url) {
        progressBar.setVisibility(View.VISIBLE);
        Glide.with(ActivityLyricCard.this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(mainImage);
    }

    private void setMainImage(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        Glide.with(ActivityLyricCard.this)
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<Uri, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, Uri model, Target<GlideDrawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, Uri model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(mainImage);
    }

    @OnClick(R.id.addCustomImage)
    void addCustomImage(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE) {
            Log.d("ActivityLyricCard", "onActivityResult: " + data);
            if(data.getData()!=null) setMainImage(data.getData());
            else Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.MyViewHolder>{

        String[] colors = getResources().getStringArray(R.array.colors);

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(ActivityLyricCard.this).inflate(R.layout.item_color, parent, false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            holder.color.setBackgroundColor(Color.parseColor(colors[position]));
        }

        @Override
        public int getItemCount() {
            return colors.length;
        }

        class MyViewHolder extends RecyclerView.ViewHolder{

            ImageView color;

            public MyViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        lyricText.setTextColor(Color.parseColor(colors[getLayoutPosition()]));
                        artistText.setTextColor(Color.parseColor(colors[getLayoutPosition()]));
                    }
                });
                color = itemView.findViewById(R.id.colorView);
            }
        }
    }

    class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.MyViewHolder>{

        private List<String> urls = new ArrayList<>();

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(ActivityLyricCard.this).inflate(R.layout.item_image_lyric_card, parent, false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {
            holder.progressBar.setVisibility(View.VISIBLE);
            Glide.with(ActivityLyricCard.this)
                    .load(urls.get(position))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            holder.progressBar.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            holder.progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }

        void setUrls(ArrayList<String> urls){
            this.urls = urls;
            setMainImage(urls.get(0));
            notifyDataSetChanged();
        }

        class MyViewHolder extends RecyclerView.ViewHolder{
            ImageView imageView;
            ProgressBar progressBar;

            public MyViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setMainImage(urls.get(getLayoutPosition()));
                    }
                });
                imageView = itemView.findViewById(R.id.image_lyric_card);
                progressBar = itemView.findViewById(R.id.progressBar);
            }
        }
    }
}
