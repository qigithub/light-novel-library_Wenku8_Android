package org.mewx.wenku8.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

public class ScreenUtil {
    /**
     * 设置刘海区域可供自己的activity使用
     * 28(9.0_P) 才有的新属性
     *
     * @param mAc
     */
    public static void setDisplayCutoutCanUse(Activity mAc) {
        if (mAc == null) return;
        // 延伸显示区域到刘海
        // 9.0 (P)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = mAc.getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            mAc.getWindow().setAttributes(lp);
        }
    }

    /**
     * 设置刘海区域可供自己的activity使用
     * 28(9.0_P) 才有的新属性
     *
     * @param lp
     */
    public static void setDisplayCutoutCanUse(WindowManager.LayoutParams lp) {
        if (lp == null) return;
        // 延伸显示区域到刘海
        // 9.0 (P)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }
    /**
     * 隐藏虚拟按键，并且全屏
     * android:fitsSystemWindows=“true”，这个属性表示系统UI（状态栏、导航栏）可见的时候，
     * 会给我们的布局加上padding（paddingTop、paddingBottom）属性
     */
    public static void hideNavigationBarAndStatusBar(Activity activity) {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            View v = activity.getWindow().getDecorView();
            WindowInsetsController controller = v.getWindowInsetsController();
            controller.hide(WindowInsets.Type.systemBars());
        }  else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.R ) {
            //for new api versions.
            View decorView = activity.getWindow().getDecorView();
            int uiOptions =
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

                            // 它被称作“粘性”的沉浸模式，这个模式会在状态栏和导航栏显示一段时间后，
                            // 自动隐藏（你可以点击一下屏幕，立即隐藏）。同时需要重点说明的是，这种模式下，
                            // 状态栏和导航栏出现的时候是“半透明”状态，
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    ;
            decorView.setSystemUiVisibility(uiOptions);
        }else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) { // lower api
            View v = activity.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        }
    }

    /**
     * 显示虚拟按键，取消全屏
     *
     * @param activity
     */
    public static void showNavigationBarAndStatusBar(Activity activity) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            View v = activity.getWindow().getDecorView();
            WindowInsetsController controller = v.getWindowInsetsController();
            controller.show(WindowInsets.Type.systemBars());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.R ) {
            //for new api versions.
            View decorView = activity.getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            decorView.setSystemUiVisibility(uiOptions);
        } else if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) { // lower api
            View v = activity.getWindow().getDecorView();
            v.setSystemUiVisibility(View.VISIBLE);
        }
    }
    /**
     * 获取状态栏高度
     *
     * @param context context
     * @return px
     */
    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height"
                , "dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }
}

