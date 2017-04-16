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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElemetHelper.ColorHelper;
import com.music.player.bhandari.m.adapter.CursorLibraryAdapter;
import com.music.player.bhandari.m.customViews.fast_scroller.RecyclerFastScroller;
import com.music.player.bhandari.m.model.Constants;
import com.music.player.bhandari.m.utils.MusicLibrary;
import com.music.player.bhandari.m.utils.MyApp;

import java.util.concurrent.Executors;


/**
 * Created by amit on 29/11/16.
 */

public class FragmentLibrary extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private int status;
    private CursorLibraryAdapter cursoradapter;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private BroadcastReceiver mRefreshLibraryReceiver;

    public FragmentLibrary() {
    }

    public int getStatus(){return status;}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(this.getArguments()!=null)
            this.status=this.getArguments().getInt("status");
        mRefreshLibraryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(Constants.TAG,"Items found tracks = "+MusicLibrary.getInstance().getDataItemsForTracks().size());
                Log.v(Constants.TAG,"Items found art= "+MusicLibrary.getInstance().getDataItemsArtist().size());
                Log.v(Constants.TAG,"Items found alb= "+MusicLibrary.getInstance().getDataItemsForAlbums().size());
                Log.v(Constants.TAG,"Items found genr= "+MusicLibrary.getInstance().getDataItemsForGenres().size());
                switch (status){
                    case Constants.FRAGMENT_STATUS.TITLE_FRAGMENT:
                        cursoradapter=new CursorLibraryAdapter(FragmentLibrary.this, getContext()
                                ,MusicLibrary.getInstance().getDataItemsForTracks());
                        cursoradapter.sort(MyApp.getPref().getInt(getString(R.string.pref_tracks_sort_by),Constants.SORT_BY.NAME));
                        // adapter=new LibraryAdapter(this, getActivity(), MusicLibrary.getInstance(getActivity()).getLibraryByTitle());
                        break;

                    case Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT:
                        cursoradapter=new CursorLibraryAdapter(FragmentLibrary.this, getContext()
                                , MusicLibrary.getInstance().getDataItemsArtist());
                        cursoradapter.sort(MyApp.getPref().getInt(getString(R.string.pref_artist_sort_by),Constants.SORT_BY.NAME));
                        break;

                    case Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT:
                        cursoradapter=new CursorLibraryAdapter(FragmentLibrary.this, getContext()
                                , MusicLibrary.getInstance().getDataItemsForAlbums());
                        cursoradapter.sort(MyApp.getPref().getInt(getString(R.string.pref_album_sort_by),Constants.SORT_BY.NAME));
                        break;

                    case Constants.FRAGMENT_STATUS.GENRE_FRAGMENT:
                        cursoradapter=new CursorLibraryAdapter(FragmentLibrary.this, getContext()
                                , MusicLibrary.getInstance().getDataItemsForGenres());
                        cursoradapter.sort(MyApp.getPref().getInt(getString(R.string.pref_genre_sort_by),Constants.SORT_BY.NAME));
                        break;
                }

                notifyDataSetChanges();
                mRecyclerView.setAdapter(cursoradapter);
                Toast.makeText(getContext(),"Library Refreshed",Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
                ((MainActivity)getActivity()).updateNavigationMenuItems();
            }
        };
    }

    @Override
    public void onDestroy() {
        if(cursoradapter!=null)
            cursoradapter.clear();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecyclerView.setAdapter(null);
    }

    public void filter(String s){
        if(cursoradapter!=null) {
            cursoradapter.filter(s);
        }
    }

    public void notifyDataSetChanges(){
        if(cursoradapter!=null){
            cursoradapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if(cursoradapter!=null) {
            cursoradapter.registerReceiver();
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
        if(cursoradapter!=null) {
            cursoradapter.unregisterReceiver();
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

        mRecyclerView = (RecyclerView) layout.findViewById(R.id.recyclerviewList);
        initializeAdapter(status);

        final RecyclerFastScroller fastScroller = (RecyclerFastScroller) layout.findViewById(R.id.fastScroller);
        Log.v(Constants.TAG,"STARTED");

        mRecyclerView.setLayoutManager(new WrapContentLinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(true);
        fastScroller.attachRecyclerView(mRecyclerView);
        fastScroller.setHandlePressedColor(ColorHelper.getDarkPrimaryColor());
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(dividerItemDecoration);

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

    public void initializeAdapter(int status){
        switch (status){
            case Constants.FRAGMENT_STATUS.TITLE_FRAGMENT:
                cursoradapter=new CursorLibraryAdapter(FragmentLibrary.this, getContext()
                        ,MusicLibrary.getInstance().getDataItemsForTracks());
                // adapter=new LibraryAdapter(this, getActivity(), MusicLibrary.getInstance(getActivity()).getLibraryByTitle());
                cursoradapter.sort(MyApp.getPref().getInt(getString(R.string.pref_tracks_sort_by),Constants.SORT_BY.NAME));
                break;

            case Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT:
                cursoradapter=new CursorLibraryAdapter(FragmentLibrary.this, getContext()
                        , MusicLibrary.getInstance().getDataItemsArtist());
                cursoradapter.sort(MyApp.getPref().getInt(getString(R.string.pref_artist_sort_by),Constants.SORT_BY.NAME));
                break;

            case Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT:
                cursoradapter=new CursorLibraryAdapter(FragmentLibrary.this, getContext()
                        , MusicLibrary.getInstance().getDataItemsForAlbums());
                cursoradapter.sort(MyApp.getPref().getInt(getString(R.string.pref_album_sort_by),Constants.SORT_BY.NAME));
                break;

            case Constants.FRAGMENT_STATUS.GENRE_FRAGMENT:
                cursoradapter=new CursorLibraryAdapter(FragmentLibrary.this, getContext()
                        , MusicLibrary.getInstance().getDataItemsForGenres());
                cursoradapter.sort(MyApp.getPref().getInt(getString(R.string.pref_genre_sort_by),Constants.SORT_BY.NAME));
                break;
        }

        Log.v(Constants.TAG,"item count "+cursoradapter.getItemCount());
        //cursoradapter.setHasStableIds(true);
        mRecyclerView.setAdapter(cursoradapter);
    }

    public void sort(int sort_id){
        if(cursoradapter !=null) {
            cursoradapter.sort(sort_id);
        }
    }

    public void updateItem(int position, String ...param){
        if(cursoradapter !=null) {
            cursoradapter.updateItem(position, param);
        }
    }

    @Override
    public void onRefresh() {
        MusicLibrary.getInstance().RefreshLibrary();
    }

        //for catching exception generated by recycler view which was causing abend, no other way to handle this
        class WrapContentLinearLayoutManager extends LinearLayoutManager {
            public WrapContentLinearLayoutManager(Context context) {
                super(context);
            }

            //... constructor
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren(recycler, state);
                } catch (IndexOutOfBoundsException e) {
                }
            }
        }

}

