package com.music.player.bhandari.m.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.adapter.CursorLibraryAdapter;
import com.music.player.bhandari.m.adapter.FolderLibraryAdapter;
import com.music.player.bhandari.m.customViews.fast_scroller.RecyclerFastScroller;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.utils.MyApp;

import java.util.concurrent.Executors;

/**
 * Created by amit on 7/12/16.
 */

public class FragmentFolderLibrary extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private  RecyclerView mRecyclerView;
    private  FolderLibraryAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    /*overwrite back button for this fragment as we will be using same recycler view for
        walking into directory
     */
    private static BroadcastReceiver mReceiverForBackPressedAction;

    public FragmentFolderLibrary(){
        mReceiverForBackPressedAction=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (adapter!= null){
                    adapter.onStepBack();
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        if(adapter!=null)
        adapter.clear();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume(){
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiverForBackPressedAction,new IntentFilter(MainActivity.NOTIFY_BACK_PRESSED));
    }

    @Override
    public void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiverForBackPressedAction);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_library, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) layout.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);
        layout.findViewById(R.id.relativeLayoutForRecyclerView).setBackgroundDrawable(ColorHelper.getBaseThemeDrawable());
        mRecyclerView = (RecyclerView) layout.findViewById(R.id.recyclerviewList);
        RecyclerFastScroller fastScroller = (RecyclerFastScroller) layout.findViewById(R.id.fastScroller);

        adapter=new FolderLibraryAdapter(getContext());
        //adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setLayoutManager(new FragmentFolderLibrary.WrapContentLinearLayoutManager(getContext()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(dividerItemDecoration);
        fastScroller.attachRecyclerView(mRecyclerView);
        fastScroller.setHandlePressedColor(ColorHelper.getDarkPrimaryColor());
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
            {
                if (dy > 0  )
                {
                    ((MainActivity)getActivity()).hideFab(true);
                }else ((MainActivity)getActivity()).hideFab(false);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState)
            {
                if (newState == RecyclerView.SCROLL_STATE_IDLE)
                {
                    ((MainActivity)getActivity()).hideFab(false);
                }

                super.onScrollStateChanged(recyclerView, newState);
            }
        });
        return layout;
    }

    @Override
    public void onRefresh() {
        //((MainActivity) getActivity()).refreshLibrary();
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                MusicLibrary.getInstance().RefreshLibrary();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                adapter=new FolderLibraryAdapter(getContext());
                Handler mHandler = new Handler(getContext().getMainLooper());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRecyclerView.setAdapter(adapter);
                        Toast.makeText(getContext(),"Folders Refreshed",Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }

    //for catching exception generated by recycler view which was causing abend, no other way to handle this
    class WrapContentLinearLayoutManager extends LinearLayoutManager {
        WrapContentLinearLayoutManager(Context context) {
            super(context);
        }

        //... constructor
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
    }

}