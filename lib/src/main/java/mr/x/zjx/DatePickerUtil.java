package mr.x.zjx;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 日期选择器工具：年 / 月 / 日三段水平滚轮，滑动停止自动吸附，中间高亮选中。
 * 单位文字（年 / 月 / 日）与滚轮分块布局，间距舒适不拥挤。
 * 仅使用 Android 原生 API，兼容 API 25+。
 */
public final class DatePickerUtil {

    public static final int   DEFAULT_COLOR       = 0xFF3F7EF7;
    public static final int   DEFAULT_TEXT_COLOR  = 0xFF111827;
    public static final int   DEFAULT_INACTIVE    = 0xFF9CA3AF;
    public static final int   DEFAULT_BG          = 0xFFFFFFFF;
    public static final float DEFAULT_RADIUS_DP   = 16f;
    public static final float DEFAULT_HEIGHT_DP   = 180f;
    public static final float DEFAULT_ITEM_W_DP   = 52f;
    public static final float DEFAULT_TEXT_SIZE_DP       = 20f;
    public static final float DEFAULT_TEXT_SIZE_SMALL_DP = 16f;
    public static final float DEFAULT_UNIT_TEXT_SIZE_SP  = 15f;

    /* 布局间距常量（本次加大，解决拥挤） */
    private static final float CARD_PADDING_H_DP   = 20f;
    private static final float CARD_PADDING_V_DP   = 26f;
    private static final float COLUMN_GAP_DP       = 10f;   // 三列之间的间距
    private static final float UNIT_GAP_DP         = 4f;    // 滚轮与单位文字之间的间距
    private static final float UNIT_END_PAD_DP     = 2f;    // 单位文字右侧额外留白

    private DatePickerUtil() { }

    /* ========================= 对外快捷入口 ========================= */
    public static Builder beautify(ViewGroup container) {
        return build(container);
    }

    public static Builder beautify(ViewGroup container, int color) {
        return build(container).selectedColor(color);
    }

    public static Builder build(ViewGroup container) {
        Builder b = new Builder(container);
        Calendar c = Calendar.getInstance();
        b.year  = c.get(Calendar.YEAR);
        b.month = c.get(Calendar.MONTH) + 1;
        b.day   = c.get(Calendar.DAY_OF_MONTH);
        return b;
    }

    public static Builder with(ViewGroup container) {
        return build(container);
    }

    /* ========================= 回调接口 ========================= */
    public interface OnDateChangedListener {
        void onDateChanged(int year, int month, int day);
    }

    /* ========================= Builder ========================= */
    public static final class Builder {

        private final ViewGroup container;

        /* ---- 数据 ---- */
        private int year     = 2024;
        private int month    = 1;
        private int day      = 1;
        private int minYear  = 1900;
        private int maxYear  = 2100;

        /* ---- 外观 ---- */
        private int     selectedColor   = DEFAULT_COLOR;
        private int     textColor       = DEFAULT_TEXT_COLOR;
        private int     inactiveColor   = DEFAULT_INACTIVE;
        private int     backgroundColor = DEFAULT_BG;
        private int     dividerColor    = 0xFFE5E7EB;
        private float   radiusDp        = DEFAULT_RADIUS_DP;
        private float   heightDp        = DEFAULT_HEIGHT_DP;
        private float   itemWidthDp     = DEFAULT_ITEM_W_DP;
        private float   textSizeDp      = DEFAULT_TEXT_SIZE_DP;
        private float   textSizeSmallDp = DEFAULT_TEXT_SIZE_SMALL_DP;
        private boolean showUnit        = true;

        /* ---- 单位文字 ---- */
        private int   unitTextColor      = 0;
        private float unitTextSizeSp     = DEFAULT_UNIT_TEXT_SIZE_SP;

        /* ---- 回调 ---- */
        private OnDateChangedListener dateChangedListener = null;

        /* ---- 子组件 ---- */
        private Wheel yearWheel, monthWheel, dayWheel;

        private Builder(ViewGroup container) {
            this.container = container;
        }

        /* ---- 数据配置 ---- */
        public Builder date(int year, int month, int day) {
            this.year  = year;
            this.month = month;
            this.day   = day;
            return this;
        }

        public Builder range(int minYear, int maxYear) {
            this.minYear = minYear;
            this.maxYear = Math.max(minYear, maxYear);
            return this;
        }

        /* ---- 外观配置 ---- */
        public Builder selectedColor(int color) { this.selectedColor = color; return this; }
        public Builder textColor(int color) { this.textColor = color; return this; }
        public Builder inactiveColor(int color) { this.inactiveColor = color; return this; }
        public Builder backgroundColor(int color) { this.backgroundColor = color; return this; }
        public Builder dividerColor(int color) { this.dividerColor = color; return this; }
        public Builder radius(float radiusDp) { this.radiusDp = radiusDp; return this; }
        public Builder height(float heightDp) { this.heightDp = heightDp; return this; }
        public Builder itemWidth(float itemWidthDp) { this.itemWidthDp = itemWidthDp; return this; }
        public Builder textSize(float sizeDp) { this.textSizeDp = sizeDp; return this; }
        public Builder showUnit(boolean show) { this.showUnit = show; return this; }

        /* ---- 单位文字 ---- */
        public Builder unitTextColor(int color) { this.unitTextColor = color; return this; }
        public Builder unitTextSize(float sizeSp) { this.unitTextSizeSp = sizeSp; return this; }

        /* ---- 回调 ---- */
        public Builder onDateChanged(OnDateChangedListener listener) {
            this.dateChangedListener = listener;
            return this;
        }

        /* ---- 核心应用 ---- */
        public void apply() {
            if (container == null) return;
            Context ctx = container.getContext();

            minYear = Math.max(1900, minYear);
            maxYear = Math.min(2100, maxYear);
            if (year < minYear) year = minYear;
            if (year > maxYear) year = maxYear;
            if (month < 1) month = 1;
            if (month > 12) month = 12;
            int maxDay = getMaxDay(year, month);
            if (day < 1) day = 1;
            if (day > maxDay) day = maxDay;

            container.removeAllViews();

            /* ---- 卡片背景 ---- */
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(backgroundColor);
            cardBg.setCornerRadius(dp(radiusDp, ctx));
            container.setBackground(cardBg);

            /* 加大卡片内边距，缓解拥挤 */
            int paddingH = dp(CARD_PADDING_H_DP, ctx);
            int paddingV = dp(CARD_PADDING_V_DP, ctx);
            container.setPadding(paddingH, paddingV, paddingH, paddingV);

            /* ---- 根水平布局 ---- */
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            /* ===== 年列 ===== */
            yearWheel = new Wheel(ctx, dp(itemWidthDp, ctx), dp(heightDp, ctx),
                    selectedColor, textColor, inactiveColor,
                    dp(textSizeDp, ctx), dp(textSizeSmallDp, ctx));
            List<String> years = new ArrayList<>();
            for (int y = minYear; y <= maxYear; y++) years.add(String.valueOf(y));
            yearWheel.setItems(years, year - minYear);

            LinearLayout yearCol = buildColumn(ctx, yearWheel, "年");
            LinearLayout.LayoutParams yLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f);
            yLp.rightMargin = dp(COLUMN_GAP_DP, ctx);
            row.addView(yearCol, yLp);

            /* ===== 月列 ===== */
            monthWheel = new Wheel(ctx, dp(itemWidthDp, ctx), dp(heightDp, ctx),
                    selectedColor, textColor, inactiveColor,
                    dp(textSizeDp, ctx), dp(textSizeSmallDp, ctx));
            List<String> months = new ArrayList<>();
            for (int m = 1; m <= 12; m++) months.add(String.valueOf(m));
            monthWheel.setItems(months, month - 1);

            LinearLayout monthCol = buildColumn(ctx, monthWheel, "月");
            LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            mLp.rightMargin = dp(COLUMN_GAP_DP, ctx);
            row.addView(monthCol, mLp);

            /* ===== 日列 ===== */
            dayWheel = new Wheel(ctx, dp(itemWidthDp, ctx), dp(heightDp, ctx),
                    selectedColor, textColor, inactiveColor,
                    dp(textSizeDp, ctx), dp(textSizeSmallDp, ctx));
            dayWheel.setItems(buildDayList(maxDay), day - 1);

            LinearLayout dayCol = buildColumn(ctx, dayWheel, "日");
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            row.addView(dayCol, dLp);

            container.addView(row);

            /* ---- 联动 ---- */
            yearWheel.listener = new Wheel.OnIndexChangedListener() {
                @Override
                public void onSelected(int index) {
                    updateDayWheel();
                    notifyDateChanged();
                }
            };
            monthWheel.listener = new Wheel.OnIndexChangedListener() {
                @Override
                public void onSelected(int index) {
                    updateDayWheel();
                    notifyDateChanged();
                }
            };
            dayWheel.listener = new Wheel.OnIndexChangedListener() {
                @Override
                public void onSelected(int index) {
                    notifyDateChanged();
                }
            };
        }

        /* ---- 一列 = 滚轮 + 单位文字 ---- */
        private LinearLayout buildColumn(Context ctx, Wheel wheel, String unit) {
            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.HORIZONTAL);
            col.setGravity(Gravity.CENTER_VERTICAL);

            /* 滚轮占满本列剩余宽度 */
            LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            col.addView(wheel.root, wLp);

            /* 单位文字 */
            if (showUnit) {
                LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                uLp.leftMargin = dp(UNIT_GAP_DP, ctx);
                uLp.rightMargin = dp(UNIT_END_PAD_DP, ctx);
                col.addView(buildUnitText(ctx, unit), uLp);
            }

            return col;
        }

        /* ---- 日段重建 ---- */
        private void updateDayWheel() {
            if (yearWheel == null || monthWheel == null || dayWheel == null) return;
            int y = minYear + yearWheel.selectedIndex;
            int m = monthWheel.selectedIndex + 1;
            int maxDay = getMaxDay(y, m);
            if (dayWheel.items.size() == maxDay) return;

            int currentDay = dayWheel.selectedIndex + 1;
            int newDay = Math.min(currentDay, maxDay);
            dayWheel.setItems(buildDayList(maxDay), newDay - 1);
        }

        /* ---- 通知回调 ---- */
        private void notifyDateChanged() {
            if (dateChangedListener == null) return;
            if (yearWheel == null || monthWheel == null || dayWheel == null) return;
            int y = minYear + yearWheel.selectedIndex;
            int m = monthWheel.selectedIndex + 1;
            int d = dayWheel.selectedIndex + 1;
            dateChangedListener.onDateChanged(y, m, d);
        }

        /* ---- 单位文字 ---- */
        private TextView buildUnitText(Context ctx, String text) {
            TextView tv = new TextView(ctx);
            tv.setText(text);
            int color = unitTextColor != 0 ? unitTextColor : textColor;
            tv.setTextColor(color);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, unitTextSizeSp);
            tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            tv.setIncludeFontPadding(false);
            tv.setGravity(Gravity.CENTER);
            tv.setSingleLine(true);
            return tv;
        }

        /* ---- 布局辅助 ---- */
        private static List<String> buildDayList(int maxDay) {
            List<String> days = new ArrayList<>();
            for (int i = 1; i <= maxDay; i++) days.add(String.valueOf(i));
            return days;
        }

        private static int getMaxDay(int year, int month) {
            switch (month) {
                case 2:
                    boolean leap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
                    return leap ? 29 : 28;
                case 4: case 6: case 9: case 11:
                    return 30;
                default:
                    return 31;
            }
        }

        private static int dp(float value, Context ctx) {
            return Math.round(value * ctx.getResources().getDisplayMetrics().density);
        }
    }

    /* ========================= 单个滚轮 ========================= */
    static final class Wheel {

        interface OnIndexChangedListener {
            void onSelected(int index);
        }

        final FrameLayout root;
        final HorizontalScrollView scrollView;
        final LinearLayout content;
        final List<TextView> items = new ArrayList<>();
        final Context ctx;
        final int   itemWidthPx;
        final int   heightPx;
        final int   activeColor;
        final int   textColor;
        final int   inactiveColor;
        final float textSizeActivePx;
        final float textSizeInactivePx;

        int selectedIndex = 0;
        OnIndexChangedListener listener;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable snapRunnable;
        private final View highlightBar;

        Wheel(Context ctx, int itemWidthPx, int heightPx,
              int activeColor, int textColor, int inactiveColor,
              float textSizeActivePx, float textSizeInactivePx) {
            this.ctx                = ctx;
            this.itemWidthPx        = itemWidthPx;
            this.heightPx           = heightPx;
            this.activeColor        = activeColor;
            this.textColor          = textColor;
            this.inactiveColor      = inactiveColor;
            this.textSizeActivePx   = textSizeActivePx;
            this.textSizeInactivePx = textSizeInactivePx;

            root = new FrameLayout(ctx);
            root.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            highlightBar = new View(ctx);
            GradientDrawable hl = new GradientDrawable();
            hl.setColor(withAlpha(activeColor, 0.10f));
            hl.setCornerRadius(dp(10, ctx));
            highlightBar.setBackground(hl);
            FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(
                    itemWidthPx, dp(34, ctx));
            hlp.gravity = Gravity.CENTER;
            root.addView(highlightBar, hlp);

            scrollView = new HorizontalScrollView(ctx);
            scrollView.setHorizontalScrollBarEnabled(false);
            scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            scrollView.setFillViewport(false);
            FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            slp.gravity = Gravity.CENTER;
            root.addView(scrollView, slp);

            content = new LinearLayout(ctx);
            content.setOrientation(LinearLayout.HORIZONTAL);
            scrollView.addView(content, new HorizontalScrollView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            setupTouch();
        }

        void setItems(List<String> labels, int defaultIndex) {
            content.removeAllViews();
            items.clear();

            for (int i = 0; i < labels.size(); i++) {
                TextView tv = new TextView(ctx);
                tv.setText(labels.get(i));
                tv.setGravity(Gravity.CENTER);
                tv.setWidth(itemWidthPx);
                tv.setHeight(heightPx);
                tv.setIncludeFontPadding(false);
                tv.setSingleLine(true);
                content.addView(tv);
                items.add(tv);
            }

            selectedIndex = Math.max(0, Math.min(defaultIndex, labels.size() - 1));

            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    layoutPadding();
                    scrollView.scrollTo(selectedIndex * itemWidthPx, 0);
                    updateStyles();
                }
            });
        }

        private void layoutPadding() {
            int w = scrollView.getWidth();
            if (w <= 0) return;
            int pad = (w - itemWidthPx) / 2;
            if (pad < 0) pad = 0;
            scrollView.setPadding(pad, 0, pad, 0);
            scrollView.setClipToPadding(false);
        }

        private void setupTouch() {
            scrollView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getAction();
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        scheduleSnap();
                    }
                    return false;
                }
            });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                    @Override
                    public void onScrollChange(View v, int scrollX, int scrollY,
                                               int oldScrollX, int oldScrollY) {
                        int idx = Math.round((float) scrollX / itemWidthPx);
                        if (idx < 0) idx = 0;
                        if (idx > items.size() - 1) idx = items.size() - 1;
                        if (idx != selectedIndex) {
                            selectedIndex = idx;
                            updateStyles();
                        }
                    }
                });
            }
        }

        private void scheduleSnap() {
            if (snapRunnable != null) handler.removeCallbacks(snapRunnable);
            snapRunnable = new Runnable() {
                @Override
                public void run() {
                    if (items.isEmpty()) return;
                    int sx = scrollView.getScrollX();
                    int targetIdx = Math.round((float) sx / itemWidthPx);
                    if (targetIdx < 0) targetIdx = 0;
                    if (targetIdx > items.size() - 1) targetIdx = items.size() - 1;
                    int targetX = targetIdx * itemWidthPx;
                    if (sx != targetX) {
                        scrollView.smoothScrollTo(targetX, 0);
                    }
                    selectedIndex = targetIdx;
                    updateStyles();
                    if (listener != null) listener.onSelected(selectedIndex);
                }
            };
            handler.postDelayed(snapRunnable, 120);
        }

        void updateStyles() {
            for (int i = 0; i < items.size(); i++) {
                TextView tv = items.get(i);
                if (i == selectedIndex) {
                    tv.setTextColor(activeColor);
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeActivePx);
                    tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    tv.setAlpha(1f);
                } else {
                    tv.setTextColor(inactiveColor);
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInactivePx);
                    tv.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
                    int dist = Math.abs(i - selectedIndex);
                    float alpha = Math.max(0.3f, 1f - dist * 0.25f);
                    tv.setAlpha(alpha);
                }
            }
        }

        private static int dp(float value, Context ctx) {
            return Math.round(value * ctx.getResources().getDisplayMetrics().density);
        }

        private static int withAlpha(int color, float alpha) {
            int a = Math.min(255, Math.max(0, (int) (255 * alpha)));
            return (color & 0x00FFFFFF) | (a << 24);
        }
    }
}