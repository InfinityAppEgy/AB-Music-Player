package com.music.player.bhandari.m.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElementHelper.TypeFaceHelper;
import com.music.player.bhandari.m.activity.ActivityTagEditor;
import com.music.player.bhandari.m.activity.ActivityNowPlaying;
import com.music.player.bhandari.m.activity.ActivityPermissionSeek;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.model.dataItem;
import com.music.player.bhandari.m.model.MusicLibrary;
import com.music.player.bhandari.m.UIElementHelper.recyclerviewHelper.ItemTouchHelperAdapter;
import com.music.player.bhandari.m.UIElementHelper.recyclerviewHelper.OnStartDragListener;
import com.music.player.bhandari.m.service.PlayerService;
import com.music.player.bhandari.m.MyApp;
import com.music.player.bhandari.m.utils.UtilityFun;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;

/**
 * Created by Amit AB on 21/12/16.
 */

public class CurrentTracklistAdapter extends RecyclerView.Adapter<CurrentTracklistAdapter.MyViewHolder>
        implements ItemTouchHelperAdapter, PopupMenu.OnMenuItemClickListener{

    private static ArrayList<dataItem> dataItems = new ArrayList<>();
    private PlayerService playerService;
    private long mLastClickTime;
    private OnStartDragListener mDragStartListener;
    private Context context;
    private LayoutInflater inflater;
    //current playing position
    private int position=0;
    private Handler handler;

    public CurrentTracklistAdapter(Context context, OnStartDragListener dragStartListener){
        mDragStartListener = dragStartListener;

        if(MyApp.getService()==null){
            Intent intent = new Intent(context, ActivityPermissionSeek.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
            return;
        }

        playerService = MyApp.getService();

        handler = new Handler(Looper.getMainLooper());

        dataItems.clear();
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                ArrayList<Integer> temp = playerService.getTrackList();
                ArrayList<dataItem> data = MusicLibrary.getInstance().getDataItemsForTracks();
                try {
                    for (int id:temp){
                        for (dataItem d:data){
                            if(d.id==id){
                                dataItems.add(d);
                                break;
                            }
                        }
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                }catch (Exception ignored){
                    //ignore for now
                    Log.e("Notify","notify");
                }
            }
        });


        position = playerService.getCurrentTrackPosition();
        this.context=context;
        inflater=LayoutInflater.from(context);
                //setHasStableIds(true);
    }

    public void fillData(){
        if(playerService==null) return;
        dataItems.clear();
        ArrayList<Integer> temp = playerService.getTrackList();
        ArrayList<dataItem> data = MusicLibrary.getInstance().getDataItemsForTracks();
        try {
            for (int id:temp){
                for (dataItem d:data){
                    if(d.id ==  id){
                        dataItems.add(d);
                        break;
                    }
                }
            }

            position = playerService.getCurrentTrackPosition();

            notifyDataSetChanged();
        }catch (Exception ignored){
            //ignore for now
            Log.e("Notify","notify");
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = inflater.inflate(R.layout.track_item_for_dragging, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final CurrentTracklistAdapter.MyViewHolder holder, int position) {

        if(dataItems.get(position) == null) return;
        holder.title.setText(dataItems.get(position).title);
        holder.secondary.setText(dataItems.get(position).artist_name);

        holder.handle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (MotionEventCompat.getActionMasked(motionEvent) ==
                        MotionEvent.ACTION_DOWN) {
                    Log.d("CurrentTracklistAdapter", "onTouch: ");
                    mDragStartListener.onStartDrag(holder);
                }
                return false;
            }
        });
        if(playerService!=null && position==playerService.getCurrentTrackPosition()) {
            holder.cv.setCardBackgroundColor(context.getResources().getColor(R.color.pw_circle_color_translucent));
            if (playerService.getStatus()==PlayerService.PLAYING){
                holder.iv.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_pause_black_24dp));
            }else {
                holder.iv.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
            }
            holder.iv.setVisibility(View.VISIBLE);
        }else {
            holder.cv.setCardBackgroundColor(context.getResources().getColor(R.color.colorTransparent));
            holder.iv.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return dataItems.size();
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        //no need to update list of in player service.
        //listOfHeader is reference for that list itself
        //it will automatically reflect in current tracklist in player service class
        Log.d("CurrentTracklistAdapter", "onItemMove: from to " + fromPosition + " : " + toPosition);
        playerService.swapPosition(fromPosition,toPosition);
        Collections.swap(dataItems,fromPosition,toPosition);
        notifyItemMoved(fromPosition,toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        if(playerService.getCurrentTrackPosition()!=position) {
            //listOfHeader.remove(position);
            playerService.removeTrack(position);
            dataItems.remove(position);
            notifyItemRemoved(position);
        }else {
            notifyItemChanged(position);
            //notifyDataSetChanged();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_play:
                playerService.playAtPositionFromNowPlaying(position);
                notifyItemChanged(position);
                break;

            case R.id.action_add_to_playlist:
                int[] ids = new int[]{dataItems.get(position).id};
                UtilityFun.AddToPlaylist(context,ids);
                break;

            case R.id.action_share:
                ArrayList<Uri> uris = new ArrayList<>();  //for sending multiple files
                File file = new File(dataItems.get(position).file_path);
                Uri fileUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + "com.bhandari.music.provider", file);
                uris.add(fileUri);
                UtilityFun.Share(context, uris, dataItems.get(position).title);
                break;

            case R.id.action_delete:
                Delete();
                break;

            case R.id.action_track_info:
                setTrackInfoDialog();
                break;

            case R.id.action_edit_track_info:
                context.startActivity(new Intent(context, ActivityTagEditor.class)
                        .putExtra("from",Constants.TAG_EDITOR_LAUNCHED_FROM.NOW_PLAYING)
                        .putExtra("file_path",dataItems.get(position).file_path)
                        .putExtra("track_title",dataItems.get(position).title)
                        .putExtra("position",position)
                        .putExtra("id",dataItems.get(position).id));
                ((ActivityNowPlaying)context).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                break;

            case R.id.action_search_youtube:
                UtilityFun.LaunchYoutube(context,dataItems.get(position).artist_name + " - "
                        +dataItems.get(position).title);
        }
        return true;
    }

    public ArrayList<Integer> getSongList(){
        ArrayList<Integer>temp = new ArrayList<>();
        for(dataItem d:dataItems){
            if(d.id != 0) {
                temp.add(d.id);
            }
        }
        return temp;
    }

    public void updateItem(int position, String... param){
        try {
            dataItems.get(position).title = param[0];
            dataItems.get(position).artist_name = param[1];
            dataItems.get(position).albumName = param[2];
            notifyItemChanged(position);
        }catch (Exception e){
            Log.v(Constants.TAG, e.toString());
        }
    }

    private void setTrackInfoDialog(){
        final AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(context.getString(R.string.track_info_title));
        LinearLayout linear = new LinearLayout(context);
        linear.setOrientation(LinearLayout.VERTICAL);
        final TextView text = new TextView(context);
        text.setTypeface(TypeFaceHelper.getTypeFace(context));
        text.setText(UtilityFun.trackInfoBuild(dataItems.get(position).id).toString());

        text.setPadding(20, 20,20,10);
        text.setTextSize(15);
        //text.setGravity(Gravity.CENTER);
        text.setTypeface(TypeFaceHelper.getTypeFace(context));
        linear.addView(text);
        alert.setView(linear);
        alert.setPositiveButton(context.getString(R.string.okay), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        alert.show();
    }

    private void Delete(){

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        ArrayList<Integer> ids = new ArrayList<>();
                        ArrayList<File> files = new ArrayList<>();

                        files.add(new File(dataItems.get(position).file_path));
                        ids.add(dataItems.get(position).id);
                        if(UtilityFun.Delete(context, files, ids)){  //last parameter not needed
                            Toast.makeText(context, context.getString(R.string.deleted) + dataItems.get(position).title, Toast.LENGTH_SHORT).show();
                            if(playerService.getCurrentTrack().getTitle().equals(dataItems.get(position).title)){
                                playerService.nextTrack();
                                playerService.notifyUI();
                                notifyItemChanged(position+1);
                            }
                            playerService.removeTrack(position);
                            dataItems.remove(position);
                            notifyItemRemoved(position);
                            // notifyDataSetChanged();
                        } else {
                            Toast.makeText(context, context.getString(R.string.unable_to_del), Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getString(R.string.are_u_sure))
                .setPositiveButton(context.getString(R.string.yes), dialogClickListener)
                .setNegativeButton(context.getString(R.string.no), dialogClickListener).show();
    }

    public void onClick(View view, int position) {
        this.position=position;
        switch (view.getId()){
            case R.id.more:
                PopupMenu popup = new PopupMenu(context, view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_tracks_by_title, popup.getMenu());
                popup.getMenu().removeItem(R.id.action_set_as_ringtone);
                popup.getMenu().removeItem(R.id.action_add_to_q);
                popup.getMenu().removeItem(R.id.action_play_next);
                popup.getMenu().removeItem(R.id.action_exclude_folder);
                popup.show();
                popup.setOnMenuItemClickListener(this);
                break;


            case R.id.trackItemDraggable:
                if (SystemClock.elapsedRealtime() - mLastClickTime < 300){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                notifyItemChanged(position);
                if(position==playerService.getCurrentTrackPosition()){
                    playerService.play();
                    playerService.notifyUI();
                }else {
                    playerService.playAtPositionFromNowPlaying(position);
                    Log.v(Constants.TAG,position+"  position");
                }
                break;
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title,secondary;
        ImageView handle;
        CardView cv;
        ImageView iv;

        MyViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.header);

            secondary = itemView.findViewById(R.id.secondaryHeader);

            handle = itemView.findViewById(R.id.handleForDrag);
            cv = itemView.findViewById(R.id.card_view_track_item_drag);
            iv = itemView.findViewById(R.id.play_button_item_drag);
            itemView.findViewById(R.id.more).setOnClickListener(this);
            itemView.findViewById(R.id.trackItemDraggable).setOnClickListener(this);
        }

        @Override
        public void onClick(View v){
            CurrentTracklistAdapter.this.onClick(v,this.getLayoutPosition());
        }
    }
}
