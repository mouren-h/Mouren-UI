package mr.x.zjx;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * 状态栏工具类
 * <p>
 * 支持：#3EB489 / #FF3EB489 / 3EB489 以及 int 颜色值、资源 id
 * 自动根据颜色亮度切换状态栏图标深/浅色（Android 6.0+）
 */
public final class StatusBarUtil {

    private StatusBarUtil() {
        throw new UnsupportedOperationException("no instance");
    }

    /** 记录 View 原始 paddingTop 的 tag key，防止重复调用时 padding 叠加 */
    private static final int KEY_BASE_PADDING_TOP = 0x7F0F0001;

    /** 亮度阈值，大于该值认为是浅色背景，需要深色图标 */
    private static final float LIGHT_THRESHOLD = 0.5f;

    // ==================== 对外主方法 ====================

    /**
     * 设置状态栏颜色（传入 #3EB489 这类字符串）
     *
     * @param colorStr 如 "#3EB489"、"#FF3EB489"、"3EB489"
     */
    public static void setColor(@NonNull Activity activity, String colorStr) {
        setColor(activity, parseColor(colorStr));
    }

    /**
     * 设置状态栏颜色
     */
    public static void setColor(@NonNull Activity activity, @ColorInt int color) {
        setColor(activity, color, false);
    }

    /**
     * 设置状态栏颜色（使用颜色资源）
     */
    public static void setColorRes(@NonNull Activity activity, @ColorRes int colorRes) {
        setColor(activity, ContextCompat.getColor(activity, colorRes));
    }

    /**
     * 设置状态栏颜色
     *
     * @param fitSystemWindow 是否同时给内容区域加上 paddingTop，
     *                        避免内容被状态栏遮挡（根布局已设 fitsSystemWindows 时无需传 true）
     */
    public static void setColor(@NonNull Activity activity,
                                @ColorInt int color,
                                boolean fitSystemWindow) {
        if (activity == null) return;

        boolean useDarkIcon = isLightColor(color);
        int realColor = color;

        // 6.0 以下无法把状态栏图标改成深色，降级处理：把背景压暗，保证白色图标可见
        if (useDarkIcon && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            realColor = darken(color, 0.4f);
            useDarkIcon = false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            // 允许我们自己绘制状态栏背景
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // 清除半透明 & 全屏标志，否则 setStatusBarColor 不生效
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.setStatusBarColor(realColor);
        }
        // 5.0 以下系统不支持直接设置状态栏颜色，直接跳过

        setLightStatusBar(activity, useDarkIcon);

        if (fitSystemWindow) {
            fitStatusBar(activity);
        }
    }

    // ==================== 图标颜色 ====================

    /**
     * 设置状态栏图标颜色
     *
     * @param lightStatusBar true = 深色图标（浅色背景时用）；false = 白色图标
     */
    public static void setLightStatusBar(@NonNull Activity activity, boolean lightStatusBar) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        View decorView = activity.getWindow().getDecorView();
        int flags = decorView.getSystemUiVisibility();
        if (lightStatusBar) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        decorView.setSystemUiVisibility(flags);
    }

    // ==================== 沉浸式 / 透明 ====================

    /**
     * 状态栏透明（内容延伸到状态栏下方，需自行给内容加 paddingTop）
     */
    public static void setTranslucent(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 给内容区域加上状态栏高度的 paddingTop，防止内容被遮挡
     */
    public static void fitStatusBar(@NonNull Activity activity) {
        ViewGroup contentView = activity.findViewById(android.R.id.content);
        if (contentView == null || contentView.getChildCount() == 0) return;

        View child = contentView.getChildAt(0);
        if (child == null || child.getFitsSystemWindows()) return;

        int statusBarHeight = getStatusBarHeight(activity);
        int baseTop = getBasePaddingTop(child);

        child.setPadding(
                child.getPaddingLeft(),
                baseTop + statusBarHeight,
                child.getPaddingRight(),
                child.getPaddingBottom());
    }

    /** 获取 View 原始的 paddingTop（防止多次调用叠加） */
    private static int getBasePaddingTop(View view) {
        Object tag = view.getTag(KEY_BASE_PADDING_TOP);
        if (tag instanceof Integer) {
            return (Integer) tag;
        }
        int top = view.getPaddingTop();
        view.setTag(KEY_BASE_PADDING_TOP, top);
        return top;
    }

    /** 获取状态栏高度 */
    public static int getStatusBarHeight(Context context) {
        int resId = context.getResources()
                .getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            return context.getResources().getDimensionPixelSize(resId);
        }
        return Math.round(24 * context.getResources().getDisplayMetrics().density);
    }

    /**
     * 判断是否为浅色（根据人眼亮度公式），决定用深色还是白色图标
     */
    public static boolean isLightColor(@ColorInt int color) {
        double luminance = (0.299 * Color.red(color)
                + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255.0;
        return luminance > LIGHT_THRESHOLD;
    }

    /** 按比例压暗颜色 */
    private static int darken(@ColorInt int color, float factor) {
        float ratio = 1f - factor;
        int r = Math.round(Color.red(color) * ratio);
        int g = Math.round(Color.green(color) * ratio);
        int b = Math.round(Color.blue(color) * ratio);
        return Color.argb(Color.alpha(color), r, g, b);
    }

    /** 解析颜色字符串，容错处理 */
    public static int parseColor(String colorStr) {
        if (TextUtils.isEmpty(colorStr)) return Color.TRANSPARENT;
        String s = colorStr.trim();
        if (!s.startsWith("#")) {
            s = "#" + s;
        }
        try {
            return Color.parseColor(s);
        } catch (IllegalArgumentException e) {
            return Color.TRANSPARENT;
        }
    }
}