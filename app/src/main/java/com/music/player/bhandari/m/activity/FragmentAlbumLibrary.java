package com.music.player.bhandari.m.activity;

/**
 * Created by amit on 16/1/17.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.adapter.AlbumLibraryAdapter;
import com.music.player.bhandari.m.adapter.CursorLibraryAdapter;
import com.music.player.bhandari.m.customViews.fast_scroller.RecyclerFastScroller;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.utils.MyApp;

import java.util.concurrent.Executors;


/**
 * Created by amit on 29/11/16.
 */

public class FragmentAlbumLibrary extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    private RecyclerView mRecyclerView;
    private AlbumLibraryAdapter albumLibraryAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private BroadcastReceiver mRefreshLibraryReceiver;

    public FragmentAlbumLibrary() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRefreshLibraryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                albumLibraryAdapter = new AlbumLibraryAdapter(getContext(), MusicLibrary.getInstance().getDataItemsForAlbums());
                mRecyclerView.setAdapter(albumLibraryAdapter);
                Toast.makeText(getContext(),"Library Refreshed",Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
                ((MainActivity)getActivity()).updateNavigationMenuItems();
            }
        };
    }

    @Override
    public void onDestroy() {
        mRecyclerView=null;
        if(albumLibraryAdapter!=null) {
            albumLibraryAdapter.clear();
        }
        super.onDestroy();
    }

    public void filter(String s){
        if(albumLibraryAdapter !=null) {
            albumLibraryAdapter.filter(s);
        }
    }

    public void sort(int sort_id){
        if(albumLibraryAdapter !=null) {
            albumLibraryAdapter.sort(sort_id);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if(albumLibraryAdapter!=null) {
            albumLibraryAdapter.registerReceiver();
        }
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mRefreshLibraryReceiver
                ,new IntentFilter(Constants.ACTION.REFRESH_LIB));
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(albumLibraryAdapter!=null) {
            albumLibraryAdapter.unregisterReceiver();
        }
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mRefreshLibraryReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_library, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) layout.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);
        layout.findViewById(R.id.relativeLayoutForRecyclerView).setBackgroundDrawable(ColorHelper.getBaseThemeDrawable());
        RecyclerFastScroller fastScroller = (RecyclerFastScroller) layout.findViewById(R.id.fastScroller);
        mRecyclerView = (RecyclerView) layout.findViewById(R.id.recyclerviewList);

        albumLibraryAdapter = new AlbumLibraryAdapter(getContext(), MusicLibrary.getInstance().getDataItemsForAlbums());
        albumLibraryAdapter.sort(MyApp.getPref().getInt(getString(R.string.pref_album_sort_by),Constants.SORT_BY.NAME));
        //albumLibraryAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(albumLibraryAdapter);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getContext(), 3);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setHasFixedSize(true);
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

        fastScroller.setHandlePressedColor(ColorHelper.getDarkPrimaryColor());
        fastScroller.attachRecyclerView(mRecyclerView);
        return layout;
    }


    @Override
    public void onRefresh() {
     //   ((MainActivity) getActivity()).refreshLibrary();
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                MusicLibrary.getInstance().RefreshLibrary();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecyclerView.setAdapter(null);
    }

}

