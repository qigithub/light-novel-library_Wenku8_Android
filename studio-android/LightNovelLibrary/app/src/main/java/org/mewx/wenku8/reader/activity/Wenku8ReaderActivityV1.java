package org.mewx.wenku8.reader.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.umeng.analytics.MobclickAgent;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import org.mewx.wenku8.R;
import org.mewx.wenku8.activity.BaseMaterialActivity;
import org.mewx.wenku8.global.GlobalConfig;
import org.mewx.wenku8.global.api.ChapterInfo;
import org.mewx.wenku8.global.api.OldNovelContentParser;
import org.mewx.wenku8.global.api.VolumeList;
import org.mewx.wenku8.global.api.Wenku8API;
import org.mewx.wenku8.global.api.Wenku8Error;
import org.mewx.wenku8.reader.loader.WenkuReaderLoader;
import org.mewx.wenku8.reader.loader.WenkuReaderLoaderXML;
import org.mewx.wenku8.reader.setting.WenkuReaderSettingV1;
import org.mewx.wenku8.reader.slider.SlidingAdapter;
import org.mewx.wenku8.reader.slider.SlidingLayout;
import org.mewx.wenku8.reader.slider.base.OverlappedSlider;
import org.mewx.wenku8.reader.view.WenkuReaderPageView;
import org.mewx.wenku8.util.LightNetwork;
import org.mewx.wenku8.util.LightTool;
import org.mewx.wenku8.util.ScreenUtil;
import org.mewx.wenku8.util.ViewHolderLV;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by MewX on 2015/7/10.
 * Novel Reader Engine V1.
 */
public class Wenku8ReaderActivityV1 extends BaseMaterialActivity {
    // constant
    static private final String FromLocal = "fav";

    // vars
    private String from = "";
    private int aid, cid;
    private String forcejump;
    private VolumeList volumeList= null;
    private List<OldNovelContentParser.NovelContent> nc = new ArrayList<>();
    private RelativeLayout mSliderHolder;
    private SlidingLayout sl;
//    private int tempNavBarHeight;

    // components
    private SlidingPageAdapter mSlidingPageAdapter;
    private WenkuReaderLoader loader;
    private WenkuReaderSettingV1 setting;

    private Toolbar toolbar_actionbar;

    private RelativeLayout reader_top,reader_bot_seeker,reader_bot_settings,layout_font_size;
    private LinearLayout reader_bot;
    private LinearLayout btn_daylight,btn_jump,btn_find,btn_config;
    private TextView text_previous,text_next;

    private DiscreteSeekBar seekerFontSize,
            seekerLineDistance ,
            seekerParagraphDistance ,
            seekerParagraphEdgeDistance ,reader_seekbar;

    private TextView btn_custom_background,btn_custom_font;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ScreenUtil.setDisplayCutoutCanUse(this);
        setContentView(R.layout.layout_reader_swipe_temp);

//        initMaterialStyle(R.layout.layout_reader_swipe_temp, BaseMaterialActivity.StatusBarColor.DARK);


        // fetch values
        aid = getIntent().getIntExtra("aid", 1);
        volumeList = (VolumeList) getIntent().getSerializableExtra("volume");
        cid = getIntent().getIntExtra("cid", 1);
        from = getIntent().getStringExtra("from");
        forcejump = getIntent().getStringExtra("forcejump");
        if(forcejump == null || forcejump.length() == 0) forcejump = "no";
//        tempNavBarHeight = LightTool.getNavigationBarSize(this).y;

        getTintManager().setTintAlpha(0.0f);
//        if(getSupportActionBar() != null) {
//            getSupportActionBar().setTitle(volumeList.volumeName);
//        }

        seekerFontSize = findViewById(R.id.reader_font_size_seeker);
        seekerLineDistance = findViewById(R.id.reader_line_distance_seeker);
        seekerParagraphDistance = findViewById(R.id.reader_paragraph_distance_seeker);
        seekerParagraphEdgeDistance = findViewById(R.id.reader_paragraph_edge_distance_seeker);
        reader_top = findViewById(R.id.reader_top);

        reader_bot_seeker = findViewById(R.id.reader_bot_seeker);
        reader_bot_settings = findViewById(R.id.reader_bot_settings);
        layout_font_size = findViewById(R.id.layout_font_size);
        btn_daylight = findViewById(R.id.btn_daylight);
        btn_jump = findViewById(R.id.btn_jump);
        btn_find = findViewById(R.id.btn_find);
        btn_config = findViewById(R.id.btn_config);
        text_previous = findViewById(R.id.text_previous);
        text_next = findViewById(R.id.text_next);
        reader_seekbar = findViewById(R.id.reader_seekbar);
        reader_bot = findViewById(R.id.reader_bot);
        btn_custom_background = findViewById(R.id.btn_custom_background);
        btn_custom_font = findViewById(R.id.btn_custom_font);
        //        reader_top = findViewById(R.id.reader_top);
        //        reader_top = findViewById(R.id.reader_top);
        
        
        toolbar_actionbar = findViewById(R.id.toolbar_actionbar);
        toolbar_actionbar.setTitle(volumeList.volumeName);
        toolbar_actionbar.setTitleTextColor(Color.WHITE);
        toolbar_actionbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        toolbar_actionbar.inflateMenu(R.menu.menu_reader_v1);

        toolbar_actionbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.action_watch_image:
                        if(sl != null && sl.getAdapter().getCurrentView() != null && ((RelativeLayout) sl.getAdapter().getCurrentView()).getChildAt(0) instanceof WenkuReaderPageView)
                            ((WenkuReaderPageView) ((RelativeLayout) sl.getAdapter().getCurrentView()).getChildAt(0)).watchImageDetailed(Wenku8ReaderActivityV1.this);
                        break;
                }
                return true;
            }
        });

        // find views
        mSliderHolder = findViewById(R.id.slider_holder);

        // UIL setting
        if(ImageLoader.getInstance() == null || !ImageLoader.getInstance().isInited()) {
            GlobalConfig.initImageLoader(this);
        }

        // async tasks
        ContentValues cv = Wenku8API.getNovelContent(aid, cid, GlobalConfig.getCurrentLang());
        AsyncNovelContentTask ast = new AsyncNovelContentTask();
        ast.execute(cv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);

        if(reader_bot.getVisibility() != View.VISIBLE)
            hideNavigationBar();
        else
            showNavigationBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_reader_v1, menu);

        Drawable drawable = menu.getItem(0).getIcon();
        if (drawable != null) {
            drawable.mutate();
            drawable.setColorFilter(getResources().getColor(R.color.default_white), PorterDuff.Mode.SRC_ATOP);
        }

        return true;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Update display cutout area.
        if (Build.VERSION.SDK_INT >= 28) {
            DisplayCutout cutout = getWindow().getDecorView().getRootWindowInsets().getDisplayCutout();
            if (cutout != null) {
                LightTool.setDisplayCutout(
                        new Rect(cutout.getSafeInsetLeft(),
                                cutout.getSafeInsetTop(),
                                cutout.getSafeInsetRight(),
                                cutout.getSafeInsetBottom()));
            }
        }
    }

    private void hideNavigationBar() {
        // This work only for android 4.4+
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            // set navigation bar status, remember to disable "setNavigationBarTintEnabled"
//            final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
//            getWindow().getDecorView().setSystemUiVisibility(flags);
//
//            // Code below is to handle presses of Volume up or Volume down.
//            // Without this, after pressing volume buttons, the navigation bar will
//            // show up and won't hide
//            final View decorView = getWindow().getDecorView();
//            decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
//                if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
//                    decorView.setSystemUiVisibility(flags);
//                }
//            });
//        }
        ScreenUtil.hideNavigationBarAndStatusBar(this);
    }

    private void showNavigationBar() {
        // set navigation bar status, remember to disable "setNavigationBarTintEnabled"
//        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
//        // This work only for android 4.4+
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            getWindow().getDecorView().setSystemUiVisibility(flags);
//
//            // Code below is to handle presses of Volume up or Volume down.
//            // Without this, after pressing volume buttons, the navigation bar will
//            // show up and won't hide
//            final View decorView = getWindow().getDecorView();
//            decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
//                if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
//                    decorView.setSystemUiVisibility(flags);
//                }
//            });
//        }
        ScreenUtil.showNavigationBarAndStatusBar(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);

        // save record
        if(mSlidingPageAdapter != null && loader != null) {
            loader.setCurrentIndex(mSlidingPageAdapter.getCurrentLastLineIndex());
            if (volumeList.chapterList.size() > 1 && volumeList.chapterList.get(volumeList.chapterList.size() - 1).cid == cid && mSlidingPageAdapter.getCurrentLastWordIndex() == loader.getCurrentStringLength() - 1)
                GlobalConfig.removeReadSavesRecordV1(aid);
            else
                GlobalConfig.addReadSavesRecordV1(aid, volumeList.vid, cid, mSlidingPageAdapter.getCurrentFirstLineIndex(), mSlidingPageAdapter.getCurrentFirstWordIndex());
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        switch(event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                gotoNextPage();
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                gotoPreviousPage();
                return true;
        }
        return super.dispatchKeyEvent(event);
    }

    class SlidingPageAdapter extends SlidingAdapter<WenkuReaderPageView> {
        int firstLineIndex = 0; // line index of first index of this page
        int firstWordIndex = 0; // first index of this page
        int lastLineIndex = 0; // line index of last index of this page
        int lastWordIndex = 0; // last index of this page

        WenkuReaderPageView nextPage;
        WenkuReaderPageView previousPage;
        boolean isLoadingNext = false;
        boolean isLoadingPrevious = false;

        public SlidingPageAdapter(int begLineIndex, int begWordIndex) {
            super();

            // init values
            firstLineIndex = begLineIndex;
            firstWordIndex = begWordIndex;

            // check valid first
            if(firstLineIndex + 1 >= loader.getElementCount()) firstLineIndex = loader.getElementCount() - 1; // to last one
            loader.setCurrentIndex(firstLineIndex);
            if(firstWordIndex + 1 >= loader.getCurrentStringLength()) {
                firstLineIndex --;
                firstWordIndex = 0;
                if(firstLineIndex < 0) firstLineIndex = 0;
            }
        }

        @Override
        public View getView(View contentView, WenkuReaderPageView pageView) {
            Log.d("MewX", "-- slider getView");
            if (contentView == null)
                contentView = getLayoutInflater().inflate(R.layout.layout_reader_swipe_page, null);

            
            // prevent memory leak
            final RelativeLayout rl = ViewHolderLV.get(contentView,R.id.page_holder);
//                    contentView.findViewById(R.id.page_holder);
            rl.removeAllViews();
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            rl.addView(pageView, lp);

            return contentView;
        }

        public int getCurrentFirstLineIndex() {
            return firstLineIndex;
        }

        public int getCurrentFirstWordIndex() {
            return firstWordIndex;
        }

        public int getCurrentLastLineIndex() {
            return lastLineIndex;
        }

        public int getCurrentLastWordIndex() {
            return lastWordIndex;
        }

        public void setCurrentIndex(int lineIndex, int wordIndex) {
            firstLineIndex = lineIndex + 1 >= loader.getElementCount() ? loader.getElementCount() - 1 : lineIndex;
            loader.setCurrentIndex(firstLineIndex);
            firstWordIndex = wordIndex + 1 >= loader.getCurrentStringLength() ? loader.getCurrentStringLength() - 1 : wordIndex;

            WenkuReaderPageView temp = new WenkuReaderPageView(Wenku8ReaderActivityV1.this, firstLineIndex, firstWordIndex, WenkuReaderPageView.LOADING_DIRECTION.CURRENT);
            firstLineIndex = temp.getFirstLineIndex();
            firstWordIndex = temp.getFirstWordIndex();
            lastLineIndex = temp.getLastLineIndex();
            lastWordIndex = temp.getLastWordIndex();
        }

        @Override
        public boolean hasNext() {
            Log.d("MewX", "-- slider hasNext");
            loader.setCurrentIndex(lastLineIndex);
            return !isLoadingNext && loader.hasNext(lastWordIndex);
        }

        @Override
        protected void computeNext() {
            Log.d("MewX", "-- slider computeNext");
            // vars change to next
            //if(nextPage == null) return;

            nextPage = new WenkuReaderPageView(Wenku8ReaderActivityV1.this, lastLineIndex, lastWordIndex, WenkuReaderPageView.LOADING_DIRECTION.FORWARDS);
            firstLineIndex = nextPage.getFirstLineIndex();
            firstWordIndex = nextPage.getFirstWordIndex();
            lastLineIndex = nextPage.getLastLineIndex();
            lastWordIndex = nextPage.getLastWordIndex();
            printLog();
        }

        @Override
        protected void computePrevious() {
            Log.d("MewX", "-- slider computePrevious");
            // vars change to previous
//            if(previousPage == null) return;
//            loader.setCurrentIndex(firstLineIndex);

            WenkuReaderPageView previousPage = new WenkuReaderPageView(Wenku8ReaderActivityV1.this, firstLineIndex, firstWordIndex, WenkuReaderPageView.LOADING_DIRECTION.BACKWARDS);
            firstLineIndex = previousPage.getFirstLineIndex();
            firstWordIndex = previousPage.getFirstWordIndex();
            lastLineIndex = previousPage.getLastLineIndex();
            lastWordIndex = previousPage.getLastWordIndex();

            // reset first page
//            if(firstLineIndex == 0 && firstWordIndex == 0)
//                notifyDataSetChanged();
            printLog();
        }

        @Override
        public WenkuReaderPageView getNext() {
            Log.d("MewX", "-- slider getNext");
//            isLoadingNext = true;
            nextPage = new WenkuReaderPageView(Wenku8ReaderActivityV1.this, lastLineIndex, lastWordIndex, WenkuReaderPageView.LOADING_DIRECTION.FORWARDS);
//            isLoadingNext = false;
            return nextPage;
        }

        @Override
        public boolean hasPrevious() {
            Log.d("MewX", "-- slider hasPrevious");
            loader.setCurrentIndex(firstLineIndex);
            return !isLoadingPrevious && loader.hasPrevious(firstWordIndex);
        }

        @Override
        public WenkuReaderPageView getPrevious() {
            Log.d("MewX", "-- slider getPrevious");
//            isLoadingPrevious = true;
            previousPage = new WenkuReaderPageView(Wenku8ReaderActivityV1.this, firstLineIndex, firstWordIndex, WenkuReaderPageView.LOADING_DIRECTION.BACKWARDS);
//            isLoadingPrevious = false;
            return previousPage;
        }

        @Override
        public WenkuReaderPageView getCurrent() {
            Log.d("MewX", "-- slider getCurrent");
            WenkuReaderPageView temp = new WenkuReaderPageView(Wenku8ReaderActivityV1.this, firstLineIndex, firstWordIndex, WenkuReaderPageView.LOADING_DIRECTION.CURRENT);
            firstLineIndex = temp.getFirstLineIndex();
            firstWordIndex = temp.getFirstWordIndex();
            lastLineIndex = temp.getLastLineIndex();
            lastWordIndex = temp.getLastWordIndex();
            printLog();
            return temp;
        }

        private void printLog() {
            Log.d("MewX", "saved index: " + firstLineIndex + "(" + firstWordIndex + ") -> " + lastLineIndex + "(" + lastWordIndex + ") | Total: " + loader.getCurrentIndex() + " of " + (loader.getElementCount()-1) );
        }
    }

    

    class AsyncNovelContentTask extends AsyncTask<ContentValues, Integer, Wenku8Error.ErrorCode> {
        private MaterialDialog md;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            md = new MaterialDialog.Builder(Wenku8ReaderActivityV1.this)
                    .theme(WenkuReaderPageView.getInDayMode() ? Theme.LIGHT : Theme.DARK)
                    .title(R.string.reader_please_wait)
                    .content(R.string.reader_engine_v1_parsing)
                    .progress(true, 0)
                    .cancelable(false)
                    .show();
        }

        @Override
        protected Wenku8Error.ErrorCode doInBackground(ContentValues... params) {
            try {
                String xml;
                if (from.equals(FromLocal)) // or exist
                    xml = GlobalConfig.loadFullFileFromSaveFolder("novel", cid + ".xml");
                else {
                    byte[] tempXml = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, params[0]);
                    if (tempXml == null) return Wenku8Error.ErrorCode.NETWORK_ERROR;
                    xml = new String(tempXml, "UTF-8");
                }

                nc = OldNovelContentParser.parseNovelContent(xml, null);
                if (nc.size() == 0)
                    return xml.length() == 0 ? Wenku8Error.ErrorCode.SERVER_RETURN_NOTHING : Wenku8Error.ErrorCode.XML_PARSE_FAILED;

                return Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return Wenku8Error.ErrorCode.STRING_CONVERSION_ERROR;
            }
        }

        @Override
        protected void onPostExecute(Wenku8Error.ErrorCode result) {
            if (result != Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED) {
                Toast.makeText(Wenku8ReaderActivityV1.this, result.toString(), Toast.LENGTH_LONG).show();
                if (md != null) md.dismiss();
                Wenku8ReaderActivityV1.this.finish(); // return friendly
                return;
            }
            Log.d("MewX", "-- 小说获取完成");

            // init components
            loader = new WenkuReaderLoaderXML(nc);
            setting = new WenkuReaderSettingV1();
            loader.setCurrentIndex(0);
            for(ChapterInfo ci : volumeList.chapterList) {
                // get chapter name
                if(ci.cid == cid) {
                    loader.setChapterName(ci.chapterName);
                    break;
                }
            }

            // config sliding layout
            mSlidingPageAdapter = new SlidingPageAdapter(0, 0);
            WenkuReaderPageView.setViewComponents(loader, setting, false);
            Log.d("MewX", "-- loader, setting 初始化完成");
            sl = new SlidingLayout(Wenku8ReaderActivityV1.this);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            sl.setAdapter(mSlidingPageAdapter);
            sl.setSlider(new OverlappedSlider());
            sl.setOnTapListener(new SlidingLayout.OnTapListener() {
                boolean barStatus = false;
                boolean isSet = false;

                @Override
                public void onSingleTap(MotionEvent event) {
                    int screenWidth = getResources().getDisplayMetrics().widthPixels;
                    int screenHeight = getResources().getDisplayMetrics().heightPixels;
                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    if(x > screenWidth / 3 && x < screenWidth * 2 / 3 && y > screenHeight / 3 && y < screenHeight * 2 / 3) {
                        // first init
                        //点击位置是内部 小矩形
                        if(!barStatus) {
                            showNavigationBar();
                            reader_top.setVisibility(View.VISIBLE);
                            reader_bot.setVisibility(View.VISIBLE);

                            if (Build.VERSION.SDK_INT >= 16 ) {
                                getTintManager().setStatusBarAlpha(0.90f);
                                getTintManager().setNavigationBarAlpha(0.80f); // TODO: fix bug
                            }
                            barStatus = true;

                            if(!isSet) {
                                // add action to each
                                btn_daylight.setOnClickListener(v -> {
                                    // switch day/night mode
                                    WenkuReaderPageView.switchDayMode();
                                    WenkuReaderPageView.resetTextColor();
                                    mSlidingPageAdapter.restoreState(null, null);
                                    mSlidingPageAdapter.notifyDataSetChanged();
                                });
                                btn_daylight.setOnLongClickListener(v -> {
                                    Toast.makeText(Wenku8ReaderActivityV1.this, getResources().getString(R.string.reader_daynight), Toast.LENGTH_SHORT).show();
                                    return true;
                                });

                                btn_jump.setOnClickListener(new View.OnClickListener() {
                                    boolean isOpen = false;
                                    @Override
                                    public void onClick(View v) {
                                        // show jump dialog
                                        if(reader_bot_settings.getVisibility() == View.VISIBLE
                                                || reader_bot_seeker.getVisibility() == View.INVISIBLE) {
                                            isOpen = false;
                                            reader_bot_settings.setVisibility(View.INVISIBLE);
                                        }
                                        if(!isOpen)
                                            reader_bot_seeker.setVisibility(View.VISIBLE);
                                        else
                                            reader_bot_seeker.setVisibility(View.INVISIBLE);
                                        isOpen = !isOpen;

                                        reader_seekbar.setMin(1);
                                        reader_seekbar.setProgress(mSlidingPageAdapter.getCurrentFirstLineIndex() + 1); // bug here
                                        reader_seekbar.setMax(loader.getElementCount());
                                        reader_seekbar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
                                            @Override
                                            public void onProgressChanged(DiscreteSeekBar discreteSeekBar, int i, boolean b) { }

                                            @Override
                                            public void onStartTrackingTouch(DiscreteSeekBar discreteSeekBar) { }

                                            @Override
                                            public void onStopTrackingTouch(DiscreteSeekBar discreteSeekBar) {
                                                mSlidingPageAdapter.setCurrentIndex(discreteSeekBar.getProgress() - 1, 0);
                                                mSlidingPageAdapter.restoreState(null, null);
                                                mSlidingPageAdapter.notifyDataSetChanged();
                                            }
                                        });
                                    }
                                });
                                btn_jump.setOnLongClickListener(v -> {
                                    Toast.makeText(Wenku8ReaderActivityV1.this, getResources().getString(R.string.reader_jump), Toast.LENGTH_SHORT).show();
                                    return true;
                                });

                                btn_find.setOnClickListener(v -> {
                                    // show label page
                                    Toast.makeText(Wenku8ReaderActivityV1.this, "查找功能尚未就绪", Toast.LENGTH_SHORT).show();
                                });
                                btn_find.setOnLongClickListener(v -> {
                                    Toast.makeText(Wenku8ReaderActivityV1.this, getResources().getString(R.string.reader_find), Toast.LENGTH_SHORT).show();
                                    return true;
                                });

                                btn_config.setOnClickListener(new View.OnClickListener() {
                                    private boolean isOpen = false;
                                    @Override
                                    public void onClick(View v) {
                                        // show jump dialog
                                        if(reader_bot_seeker.getVisibility() == View.VISIBLE
                                                || reader_bot_settings.getVisibility() == View.INVISIBLE) {
                                            isOpen = false;
                                            reader_bot_seeker.setVisibility(View.INVISIBLE);
                                        }
                                        if(!isOpen)
                                            reader_bot_settings.setVisibility(View.VISIBLE);
                                        else
                                            reader_bot_settings.setVisibility(View.INVISIBLE);
                                        isOpen = !isOpen;

                                        // set all listeners

                                        seekerFontSize.setProgress(setting.getFontSize());
                                        seekerFontSize.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
                                            @Override
                                            public void onProgressChanged(DiscreteSeekBar discreteSeekBar, int i, boolean b) { }

                                            @Override
                                            public void onStartTrackingTouch(DiscreteSeekBar discreteSeekBar) { }

                                            @Override
                                            public void onStopTrackingTouch(DiscreteSeekBar discreteSeekBar) {
                                                setting.setFontSize(discreteSeekBar.getProgress());
                                                WenkuReaderPageView.setViewComponents(loader, setting, false);
                                                mSlidingPageAdapter.restoreState(null, null);
                                                mSlidingPageAdapter.notifyDataSetChanged();
                                            }
                                        });

                                        seekerLineDistance.setProgress(setting.getLineDistance());
                                        seekerLineDistance.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
                                            @Override
                                            public void onProgressChanged(DiscreteSeekBar discreteSeekBar, int i, boolean b) { }

                                            @Override
                                            public void onStartTrackingTouch(DiscreteSeekBar discreteSeekBar) { }

                                            @Override
                                            public void onStopTrackingTouch(DiscreteSeekBar discreteSeekBar) {
                                                setting.setLineDistance(discreteSeekBar.getProgress());
                                                WenkuReaderPageView.setViewComponents(loader, setting, false);
                                                mSlidingPageAdapter.restoreState(null, null);
                                                mSlidingPageAdapter.notifyDataSetChanged();
                                            }
                                        });

                                        seekerParagraphDistance.setProgress(setting.getParagraphDistance());
                                        seekerParagraphDistance.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
                                            @Override
                                            public void onProgressChanged(DiscreteSeekBar discreteSeekBar, int i, boolean b) { }

                                            @Override
                                            public void onStartTrackingTouch(DiscreteSeekBar discreteSeekBar) { }

                                            @Override
                                            public void onStopTrackingTouch(DiscreteSeekBar discreteSeekBar) {
                                                setting.setParagraphDistance(discreteSeekBar.getProgress());
                                                WenkuReaderPageView.setViewComponents(loader, setting, false);
                                                mSlidingPageAdapter.restoreState(null, null);
                                                mSlidingPageAdapter.notifyDataSetChanged();
                                            }
                                        });

                                        seekerParagraphEdgeDistance.setProgress(setting.getPageEdgeDistance());
                                        seekerParagraphEdgeDistance.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
                                            @Override
                                            public void onProgressChanged(DiscreteSeekBar discreteSeekBar, int i, boolean b) { }

                                            @Override
                                            public void onStartTrackingTouch(DiscreteSeekBar discreteSeekBar) { }

                                            @Override
                                            public void onStopTrackingTouch(DiscreteSeekBar discreteSeekBar) {
                                                setting.setPageEdgeDistance(discreteSeekBar.getProgress());
                                                WenkuReaderPageView.setViewComponents(loader, setting, false);
                                                mSlidingPageAdapter.restoreState(null, null);
                                                mSlidingPageAdapter.notifyDataSetChanged();
                                            }
                                        });

                                        btn_custom_font.setOnClickListener(v1 -> new MaterialDialog.Builder(Wenku8ReaderActivityV1.this)
                                                .theme(WenkuReaderPageView.getInDayMode() ? Theme.LIGHT : Theme.DARK)
                                                .title(R.string.reader_custom_font)
                                                .items(R.array.reader_font_option)
                                                .itemsCallback((dialog, view, which, text) -> {
                                                    switch (which) {
                                                        case 0:
                                                            // system default
                                                            setting.setUseCustomFont(false);
                                                            WenkuReaderPageView.setViewComponents(loader, setting, false);
                                                            mSlidingPageAdapter.restoreState(null, null);
                                                            mSlidingPageAdapter.notifyDataSetChanged();
                                                            break;
                                                        case 1:
                                                            // TODO: use system UI FilePicker.
                                                            // choose a ttf file
                                                            Intent i = new Intent(Wenku8ReaderActivityV1.this, FilePickerActivity.class);
                                                            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                                                            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                                                            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
                                                            i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                                                                    GlobalConfig.pathPickedSave == null || GlobalConfig.pathPickedSave.length() == 0 ?
                                                                            Environment.getExternalStorageDirectory().getPath() : GlobalConfig.pathPickedSave);
                                                            startActivityForResult(i, 0); // choose font is 0
                                                            break;
                                                    }
                                                })
                                                .show());

                                        btn_custom_background.setOnClickListener(v12 -> new MaterialDialog.Builder(Wenku8ReaderActivityV1.this)
                                                .theme(WenkuReaderPageView.getInDayMode() ? Theme.LIGHT : Theme.DARK)
                                                .title(R.string.reader_custom_background)
                                                .items(R.array.reader_background_option)
                                                .itemsCallback((dialog, view, which, text) -> {
                                                    switch (which) {
                                                        case 0:
                                                            // system default
                                                            setting.setPageBackgroundType(WenkuReaderSettingV1.PAGE_BACKGROUND_TYPE.SYSTEM_DEFAULT);
                                                            WenkuReaderPageView.setViewComponents(loader, setting, true);
                                                            mSlidingPageAdapter.restoreState(null, null);
                                                            mSlidingPageAdapter.notifyDataSetChanged();
                                                            break;
                                                        case 1:
                                                            // TODO: use system UI file picker.
                                                            // choose a image file
                                                            Intent i = new Intent(Wenku8ReaderActivityV1.this, FilePickerActivity.class);
                                                            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                                                            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                                                            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
                                                            i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                                                                    GlobalConfig.pathPickedSave == null || GlobalConfig.pathPickedSave.length() == 0 ?
                                                                            Environment.getExternalStorageDirectory().getPath() : GlobalConfig.pathPickedSave);
                                                            startActivityForResult(i, 1); // choose image is 1
                                                            break;
                                                    }
                                                })
                                                .show());
                                    }
                                });
                                btn_config.setOnLongClickListener(v -> {
                                    Toast.makeText(Wenku8ReaderActivityV1.this, getResources().getString(R.string.reader_config), Toast.LENGTH_SHORT).show();
                                    return true;
                                });

                                text_previous.setOnClickListener(v -> {
                                    // goto previous chapter
                                    for (int i = 0; i < volumeList.chapterList.size(); i++) {
                                        if (cid == volumeList.chapterList.get(i).cid) {
                                            // found self
                                            if (i == 0) {
                                                // no more previous
                                                Toast.makeText(Wenku8ReaderActivityV1.this, getResources().getString(R.string.reader_already_first_chapter), Toast.LENGTH_SHORT).show();
                                            } else {
                                                // jump to previous
                                                final int i_bak = i;
                                                new MaterialDialog.Builder(Wenku8ReaderActivityV1.this)
                                                        .onPositive((dialog, which) -> {
                                                            Intent intent = new Intent(Wenku8ReaderActivityV1.this, Wenku8ReaderActivityV1.class); //VerticalReaderActivity.class);
                                                            intent.putExtra("aid", aid);
                                                            intent.putExtra("volume", volumeList);
                                                            intent.putExtra("cid", volumeList.chapterList.get(i_bak - 1).cid);
                                                            intent.putExtra("from", from); // from cloud
                                                            startActivity(intent);
                                                            overridePendingTransition(R.anim.fade_in, R.anim.hold); // fade in animation
                                                            Wenku8ReaderActivityV1.this.finish();
                                                        })
                                                        .theme(WenkuReaderPageView.getInDayMode() ? Theme.LIGHT : Theme.DARK)
                                                        .title(R.string.dialog_sure_to_jump_chapter)
                                                        .content(volumeList.chapterList.get(i_bak - 1).chapterName)
                                                        .contentGravity(GravityEnum.CENTER)
                                                        .positiveText(R.string.dialog_positive_yes)
                                                        .negativeText(R.string.dialog_negative_no)
                                                        .show();
                                            }
                                            break;
                                        }
                                    }
                                });

                                text_next.setOnClickListener(v -> {
                                    // goto next chapter
                                    for (int i = 0; i < volumeList.chapterList.size(); i++) {
                                        if (cid == volumeList.chapterList.get(i).cid) {
                                            // found self
                                            if (i + 1 >= volumeList.chapterList.size()) {
                                                // no more previous
                                                Toast.makeText(Wenku8ReaderActivityV1.this, getResources().getString(R.string.reader_already_last_chapter), Toast.LENGTH_SHORT).show();
                                            } else {
                                                // jump to previous
                                                final int i_bak = i;
                                                new MaterialDialog.Builder(Wenku8ReaderActivityV1.this)
                                                        .onPositive((dialog, which) -> {
                                                            Intent intent = new Intent(Wenku8ReaderActivityV1.this, Wenku8ReaderActivityV1.class); //VerticalReaderActivity.class);
                                                            intent.putExtra("aid", aid);
                                                            intent.putExtra("volume", volumeList);
                                                            intent.putExtra("cid", volumeList.chapterList.get(i_bak + 1).cid);
                                                            intent.putExtra("from", from); // from cloud
                                                            startActivity(intent);
                                                            overridePendingTransition(R.anim.fade_in, R.anim.hold); // fade in animation
                                                            Wenku8ReaderActivityV1.this.finish();
                                                        })
                                                        .theme(WenkuReaderPageView.getInDayMode() ? Theme.LIGHT : Theme.DARK)
                                                        .title(R.string.dialog_sure_to_jump_chapter)
                                                        .content(volumeList.chapterList.get(i_bak + 1).chapterName)
                                                        .contentGravity(GravityEnum.CENTER)
                                                        .positiveText(R.string.dialog_positive_yes)
                                                        .negativeText(R.string.dialog_negative_no)
                                                        .show();
                                            }
                                            break;
                                        }
                                    }
                                });
                            }
                        }
                        else {
                            // show menu
                            hideNavigationBar();
                            reader_top.setVisibility(View.INVISIBLE);
                            reader_bot.setVisibility(View.INVISIBLE);
                            reader_bot_seeker.setVisibility(View.INVISIBLE);
                            reader_bot_settings.setVisibility(View.INVISIBLE);
                            if (Build.VERSION.SDK_INT >= 16 ) {
                                getTintManager().setStatusBarAlpha(0.0f);
                                getTintManager().setNavigationBarAlpha(0.0f);
                            }
                            barStatus = false;
                        }
                        return;
                    }

                    //
                    if (x > (screenWidth / 3 ) * 2 ) {
                        gotoNextPage();
                    } else if (x <= (screenWidth / 3) ) {
                        gotoPreviousPage();
                    }else {
                        if (y > (screenHeight / 3 ) *2 ) {
                            gotoNextPage();
                        } else if (y <= screenHeight / 3 ) {
                            gotoPreviousPage();
                        }
                    }

                }
            });
            mSliderHolder.addView(sl, 0, lp);
            Log.d("MewX", "-- slider创建完毕");

            // end loading dialog
            if (md != null)
                md.dismiss();

            // show dialog, jump to last read position
            if (GlobalConfig.getReadSavesRecordV1(aid) != null) {
                final GlobalConfig.ReadSavesV1 rs = GlobalConfig.getReadSavesRecordV1(aid);
                if(rs != null && rs.vid == volumeList.vid && rs.cid == cid) {
                    if(forcejump.equals("yes")) {
                        mSlidingPageAdapter.setCurrentIndex(rs.lineId, rs.wordId);
                        mSlidingPageAdapter.restoreState(null, null);
                        mSlidingPageAdapter.notifyDataSetChanged();
                    } else if (mSlidingPageAdapter.getCurrentFirstLineIndex() != rs.lineId ||
                            mSlidingPageAdapter.getCurrentFirstWordIndex() != rs.wordId) {
                        // Popping up jump dialog only when the user didn't exist at the first page.
                        new MaterialDialog.Builder(Wenku8ReaderActivityV1.this)
                                .onPositive((dialog, which) -> {
                                    mSlidingPageAdapter.setCurrentIndex(rs.lineId, rs.wordId);
                                    mSlidingPageAdapter.restoreState(null, null);
                                    mSlidingPageAdapter.notifyDataSetChanged();
                                })
                                .theme(WenkuReaderPageView.getInDayMode() ? Theme.LIGHT : Theme.DARK)
                                .title(R.string.reader_v1_notice)
                                .content(R.string.reader_jump_last)
                                .contentGravity(GravityEnum.CENTER)
                                .positiveText(R.string.dialog_positive_sure)
                                .negativeText(R.string.dialog_negative_biao)
                                .show();
                    }
                }
            }
        }
    }

    private void gotoNextPage() {
        if(mSlidingPageAdapter != null && !mSlidingPageAdapter.hasNext()) {
            // goto next chapter
            for (int i = 0; i < volumeList.chapterList.size(); i++) {
                if (cid == volumeList.chapterList.get(i).cid) {
                    // found self
                    if (i + 1 >= volumeList.chapterList.size()) {
                        // no more previous
                        Toast.makeText(Wenku8ReaderActivityV1.this, getResources().getString(R.string.reader_already_last_chapter), Toast.LENGTH_SHORT).show();
                    } else {
                        // jump to previous
                        final int i_bak = i;
                        new MaterialDialog.Builder(Wenku8ReaderActivityV1.this)
                                .onPositive((dialog, which) -> {
                                    Intent intent = new Intent(Wenku8ReaderActivityV1.this, Wenku8ReaderActivityV1.class); //VerticalReaderActivity.class);
                                    intent.putExtra("aid", aid);
                                    intent.putExtra("volume", volumeList);
                                    intent.putExtra("cid", volumeList.chapterList.get(i_bak + 1).cid);
                                    intent.putExtra("from", from); // from cloud
                                    startActivity(intent);
                                    overridePendingTransition(R.anim.fade_in, R.anim.hold); // fade in animation
                                    Wenku8ReaderActivityV1.this.finish();
                                })
                                .theme(WenkuReaderPageView.getInDayMode() ? Theme.LIGHT : Theme.DARK)
                                .title(R.string.dialog_sure_to_jump_chapter)
                                .content(volumeList.chapterList.get(i_bak + 1).chapterName)
                                .contentGravity(GravityEnum.CENTER)
                                .positiveText(R.string.dialog_positive_yes)
                                .negativeText(R.string.dialog_negative_no)
                                .show();
                    }
                    break;
                }
            }
        }
        else {
            if(sl != null)
                sl.slideNext();
        }
    }

    private void gotoPreviousPage() {
        if(mSlidingPageAdapter != null && !mSlidingPageAdapter.hasPrevious()) {
            // goto previous chapter
            for (int i = 0; i < volumeList.chapterList.size(); i++) {
                if (cid == volumeList.chapterList.get(i).cid) {
                    // found self
                    if (i == 0) {
                        // no more previous
                        Toast.makeText(Wenku8ReaderActivityV1.this, getResources().getString(R.string.reader_already_first_chapter), Toast.LENGTH_SHORT).show();
                    } else {
                        // jump to previous
                        final int i_bak = i;
                        new MaterialDialog.Builder(Wenku8ReaderActivityV1.this)
                                .onPositive((dialog, which) -> {
                                    Intent intent = new Intent(Wenku8ReaderActivityV1.this, Wenku8ReaderActivityV1.class); //VerticalReaderActivity.class);
                                    intent.putExtra("aid", aid);
                                    intent.putExtra("volume", volumeList);
                                    intent.putExtra("cid", volumeList.chapterList.get(i_bak - 1).cid);
                                    intent.putExtra("from", from); // from cloud
                                    startActivity(intent);
                                    overridePendingTransition(R.anim.fade_in, R.anim.hold); // fade in animation
                                    Wenku8ReaderActivityV1.this.finish();
                                })
                                .theme(WenkuReaderPageView.getInDayMode() ? Theme.LIGHT : Theme.DARK)
                                .title(R.string.dialog_sure_to_jump_chapter)
                                .content(volumeList.chapterList.get(i_bak - 1).chapterName)
                                .contentGravity(GravityEnum.CENTER)
                                .positiveText(R.string.dialog_positive_yes)
                                .negativeText(R.string.dialog_negative_no)
                                .show();
                    }
                    break;
                }
            }
        }
        else {
            if(sl != null)
                sl.slidePrevious();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            // get ttf path
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                // For JellyBean and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip = data.getClipData();
                    if (clip != null) {
                        for (int i = 0; i < clip.getItemCount(); i++) {
                            Uri uri = clip.getItemAt(i).getUri();
                            // Do something with the URI
                            runSaveCustomFontPath(uri.toString().replaceAll("file://", ""));
                        }
                    }
                    // For Ice Cream Sandwich
                } else {
                    ArrayList<String> paths = data.getStringArrayListExtra(FilePickerActivity.EXTRA_PATHS);
                    if (paths != null) {
                        for (String path : paths) {
                            Uri uri = Uri.parse(path);
                            // Do something with the URI
                            runSaveCustomFontPath(uri.toString().replaceAll("file://", ""));
                        }
                    }
                }
            } else {
                Uri uri = data.getData();
                // Do something with the URI
                runSaveCustomFontPath(uri.toString().replaceAll("file://", ""));
            }
        } else if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            // get image path
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                // For JellyBean and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip = data.getClipData();
                    if (clip != null) {
                        for (int i = 0; i < clip.getItemCount(); i++) {
                            Uri uri = clip.getItemAt(i).getUri();
                            // Do something with the URI
                            runSaveCustomBackgroundPath(uri.toString().replaceAll("file://", ""));
                        }
                    }
                    // For Ice Cream Sandwich
                } else {
                    ArrayList<String> paths = data.getStringArrayListExtra(FilePickerActivity.EXTRA_PATHS);
                    if (paths != null) {
                        for (String path : paths) {
                            Uri uri = Uri.parse(path);
                            // Do something with the URI
                            runSaveCustomBackgroundPath(uri.toString().replaceAll("file://", ""));
                        }
                    }
                }
            } else {
                Uri uri = data.getData();
                // Do something with the URI
                runSaveCustomBackgroundPath(uri.toString().replaceAll("file://", ""));
            }

        }
    }

    private void runSaveCustomFontPath(String path) {
        setting.setCustomFontPath(path);
        WenkuReaderPageView.setViewComponents(loader, setting, false);
        mSlidingPageAdapter.restoreState(null, null);
        mSlidingPageAdapter.notifyDataSetChanged();
    }

    private void runSaveCustomBackgroundPath(String path) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
        } catch (OutOfMemoryError oome) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeFile(path, options);
                if(bitmap == null) throw new Exception("PictureDecodeFailedException");
            } catch(Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Exception: " + e.toString() + "\n可能的原因有：图片不在内置SD卡；图片格式不正确；图片像素尺寸太大，请使用小一点的图，谢谢，此功能为试验性功能；", Toast.LENGTH_LONG).show();
                return;
            }
        }
        setting.setPageBackgroundCustomPath(path);
        WenkuReaderPageView.setViewComponents(loader, setting, true);
        mSlidingPageAdapter.restoreState(null, null);
        mSlidingPageAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_watch_image:
                if(sl != null && sl.getAdapter().getCurrentView() != null && ((RelativeLayout) sl.getAdapter().getCurrentView()).getChildAt(0) instanceof WenkuReaderPageView)
                    ((WenkuReaderPageView) ((RelativeLayout) sl.getAdapter().getCurrentView()).getChildAt(0)).watchImageDetailed(this);
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.fade_out);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (toolbar_actionbar == null)
            return;
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)toolbar_actionbar.getLayoutParams();
        lp.topMargin = ScreenUtil.getStatusBarHeight(this);
        toolbar_actionbar.setLayoutParams(lp);

    }
}
