package org.mewx.wenku8.fragment;

import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jpardogo.android.googleprogressbar.library.GoogleProgressBar;
import com.umeng.analytics.MobclickAgent;

import org.mewx.wenku8.R;
import org.mewx.wenku8.activity.MainActivity;
import org.mewx.wenku8.activity.NovelInfoActivity;
import org.mewx.wenku8.adapter.NovelItemAdapter;
import org.mewx.wenku8.global.GlobalConfig;
import org.mewx.wenku8.global.api.NovelItemInfoUpdate;
import org.mewx.wenku8.global.api.NovelListWithInfoParser;
import org.mewx.wenku8.global.api.Wenku8API;
import org.mewx.wenku8.listener.MyItemClickListener;
import org.mewx.wenku8.listener.MyItemLongClickListener;
import org.mewx.wenku8.util.LightNetwork;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * 最近更新
 */
public class LatestFragment extends Fragment implements MyItemClickListener, MyItemLongClickListener {

    static private final String TAG = "LatestFragment";

    // components
    private MainActivity mainActivity = null;
    private LinearLayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    private TextView mTextView;

    // Novel Item info
    private List<NovelItemInfoUpdate> listNovelItemInfo;
    private NovelItemAdapter mAdapter;
    private int currentPage, totalPage; // currentP stores next reading page num, TODO: fix wrong number

    // switcher
    private boolean isLoading;
    int pastVisibleItems, visibleItemCount, totalItemCount;
    private TextView relay_warning;
    private LinearLayout list_loading;

    public LatestFragment() {
        // Required empty public constructor
    }

    public static LatestFragment newInstance() {
        return new LatestFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get main activity
        while (mainActivity == null)
            mainActivity = (MainActivity) getActivity();
    }

    public NovelItemAdapter getNovelItemAdapter() {
        return mAdapter;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_latest, container, false);

        // Set warning message.
        relay_warning = rootView.findViewById(R.id.relay_warning);
        relay_warning.setOnClickListener(view -> new MaterialDialog.Builder(getContext())
                .theme(Theme.LIGHT)
                .backgroundColorRes(R.color.dlgBackgroundColor)
                .contentColorRes(R.color.dlgContentColor)
                .positiveColorRes(R.color.dlgPositiveButtonColor)
                .negativeColorRes(R.color.dlgNegativeButtonColor)
                .title(getResources().getString(R.string.system_warning))
                .content(getResources().getString(R.string.relay_warning_full))
                .positiveText(R.string.dialog_positive_ok)
                .show());

        list_loading = rootView.findViewById(R.id.list_loading);
        google_progress = rootView.findViewById(R.id.google_progress);
        btn_loading = rootView.findViewById(R.id.btn_loading);
        // get views
        mRecyclerView = rootView.findViewById(R.id.novel_item_list);
        mTextView = rootView.findViewById(R.id.list_loading_status);

        mRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Listener
        mRecyclerView.addOnScrollListener(new MyOnScrollListener());
        if (listNovelItemInfo==null)
            listNovelItemInfo = new ArrayList<>(16);
        if (mAdapter == null) {
            mAdapter = new NovelItemAdapter(listNovelItemInfo);
        }
        mAdapter.setOnItemClickListener(LatestFragment.this);
        mAdapter.setOnItemLongClickListener(LatestFragment.this);
        mRecyclerView.setAdapter(mAdapter);

        // set click event
        btn_loading.setOnClickListener(v -> {
            if (isLoading) {
                isLoading = false; // set this false as a terminator signal
            } else {
                // need to reload novel list all
                currentPage = 1;
                totalPage = 1;
                isLoading = false;
                loadNovelList(currentPage);
            }
        });


        // fetch novel list
        currentPage = 1;
        totalPage = 1;
        isLoading = false;
        loadNovelList(currentPage);

        return rootView;
    }

    private void loadNovelList(int page) {
        // In fact, I don't need to know what it really is.
        // I just need to get the NOVELSORTBY
        isLoading = true; // set loading states
        hideRetryButton();

        // fetch list
        AsyncLoadLatestList ast = new AsyncLoadLatestList();
        ast.execute(Wenku8API.getNovelListWithInfo(Wenku8API.NOVELSORTBY.lastUpdate, page,
                GlobalConfig.getCurrentLang()));
    }

    @Override
    public void onItemClick(View view, final int position) {
        //Toast.makeText(getActivity(),"item click detected", Toast.LENGTH_SHORT).show();
        if(position < 0 || position >= listNovelItemInfo.size()) {
            // ArrayIndexOutOfBoundsException
            Toast.makeText(getActivity(), "ArrayIndexOutOfBoundsException: " + position + " in size " + listNovelItemInfo.size(), Toast.LENGTH_SHORT).show();
            return;
        }

        // go to detail activity
        Intent intent = new Intent(getActivity(), NovelInfoActivity.class);
        intent.putExtra("aid", listNovelItemInfo.get(position).aid);
        intent.putExtra("from", "latest");
        intent.putExtra("title", listNovelItemInfo.get(position).title);
        if(Build.VERSION.SDK_INT < 21) {
            startActivity(intent);
        }
        else {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                    Pair.create(view.findViewById(R.id.novel_cover), "novel_cover"),
                    Pair.create(view.findViewById(R.id.novel_title), "novel_title"));
            ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
        }
    }

    @Override
    public void onItemLongClick(View view, int position) {
        // empty
        onItemClick(view, position);
    }


    @Override
    public void onDetach() {
        isLoading = false;
        super.onDetach();
    }

    private class MyOnScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            visibleItemCount = mLayoutManager.getChildCount();
            totalItemCount = mLayoutManager.getItemCount();
            pastVisibleItems = mLayoutManager.findFirstVisibleItemPosition();

            if (!isLoading) {
                // 滚动到一半的时候加载，即：剩余3个元素的时候就加载
                if (visibleItemCount + pastVisibleItems + 3 >= totalItemCount) {
                    isLoading = true;

                    // load more toast
                    Snackbar.make(mRecyclerView, getResources().getString(R.string.list_loading)
                                    + "(" + Integer.toString(currentPage) + "/" + totalPage + ")",
                            Snackbar.LENGTH_SHORT).show();

                    // load more thread
                    if (currentPage <= totalPage) {
                        loadNovelList(currentPage);
                    } else {
                        Snackbar.make(mRecyclerView, getResources().getText(R.string.loading_done), Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    class AsyncLoadLatestList extends AsyncTask<ContentValues, Integer, Integer> {
        private boolean usingWenku8Relay = false;

        // fail return -1
        @Override
        protected Integer doInBackground(ContentValues... params) {
            try {
                // Try requesting from the original website.
                byte[] tempXml = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, params[0]);
                if (tempXml == null) {
                    // Try requesting from the relay.
                    tempXml = LightNetwork.LightHttpPostConnection(Wenku8API.RELAY_URL, params[0], false);
                    if (tempXml == null) {
                        // Still failed, return the error code.
                        return -100;
                    }
                    usingWenku8Relay = true;
                }
                String xml = new String(tempXml, "UTF-8");
                totalPage = NovelListWithInfoParser.getNovelListWithInfoPageNum(xml);
                List<NovelListWithInfoParser.NovelListWithInfo> l = NovelListWithInfoParser.getNovelListWithInfo(xml);
                if (l.isEmpty()) {
                    // Try requesting from the relay.
                    tempXml = LightNetwork.LightHttpPostConnection(Wenku8API.RELAY_URL, params[0], false);
                    if (tempXml == null) {
                        // Relay network error.
                        return -100;
                    }
                    xml = new String(tempXml, "UTF-8");
                    totalPage = NovelListWithInfoParser.getNovelListWithInfoPageNum(xml);
                    l = NovelListWithInfoParser.getNovelListWithInfo(xml);
                    if (l.isEmpty()) {
                        // Blocked error.
                        return -100;
                    }
                    usingWenku8Relay = true;
                }

                if (listNovelItemInfo == null)
                    listNovelItemInfo = new ArrayList<>();
                for (int i = 0; i < l.size(); i++) {
                    NovelListWithInfoParser.NovelListWithInfo nlwi = l.get(i);
                    NovelItemInfoUpdate ni = new NovelItemInfoUpdate(nlwi.aid);
                    ni.title = nlwi.name;
                    ni.author = nlwi.hit + ""; // hit
                    ni.update = nlwi.push + ""; // push
                    ni.intro_short = nlwi.fav + ""; // fav
                    listNovelItemInfo.add(ni);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == -100) {
                if(!isAdded())
                    return; // detached

                mTextView.setText(getResources().getString(R.string.system_parse_failed));
                showRetryButton();
                isLoading = false;
                return;
            }

            // result:
            // add imageView, only here can fetch the layout2 id!!!
            // hide loading layout
            if (mAdapter == null) {
                mAdapter = new NovelItemAdapter(listNovelItemInfo);
//                mAdapter.setOnItemClickListener(LatestFragment.this);
//                mAdapter.setOnItemLongClickListener(LatestFragment.this);
//                mRecyclerView.setAdapter(mAdapter);
            }
            mAdapter.RefreshDataset(listNovelItemInfo);
            if (list_loading!= null)
                list_loading.setVisibility(View.GONE);
            mAdapter.notifyDataSetChanged();

            currentPage ++; // add when loaded
            isLoading = false;

            if (usingWenku8Relay) {
                relay_warning.setVisibility(View.VISIBLE);
            } else {
                relay_warning.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd(TAG);
        GlobalConfig.LeaveLatest();
    }

    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onPageStart(TAG);
        GlobalConfig.EnterLatest();
    }

    private GoogleProgressBar google_progress;
    private TextView btn_loading;

    private void showRetryButton() {
        if (btn_loading == null || !isAdded()) return;
        btn_loading.setText(getResources().getString(R.string.task_retry));
        google_progress.setVisibility(View.GONE);
        btn_loading.setVisibility(View.VISIBLE);
    }

    /**
     * After button pressed, should hide the "retry" button
     */
    private void hideRetryButton() {
        if (btn_loading == null) return;
        mTextView.setText(getResources().getString(R.string.list_loading));
        google_progress.setVisibility(View.VISIBLE);
        btn_loading.setVisibility(View.GONE);
    }


}
