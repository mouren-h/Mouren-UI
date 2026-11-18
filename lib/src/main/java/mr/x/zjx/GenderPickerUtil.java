package mr.x.zjx;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 性别选择器工具：分段式单选（男 / 女 / 自定义），选中项实心主色 + 白字，未选中透明 + 灰字。
 * 仅使用 Android 原生 API，兼容 API 25+。
 */
public final class GenderPickerUtil {

    public static final int   DEFAULT_COLOR         = 0xFF3F7EF7;
    public static final int   DEFAULT_BG            = 0xFFF3F4F6;
    public static final int   DEFAULT_TEXT_COLOR    = 0xFF374151;
    public static final int   DEFAULT_SELECTED_TEXT = 0xFFFFFFFF;
    public static final float DEFAULT_RADIUS_DP     = 12f;
    public static final float DEFAULT_HEIGHT_DP     = 44f;
    public static final float DEFAULT_TEXT_SIZE_SP  = 15f;
    public static final float DEFAULT_PADDING_DP    = 4f;

    private GenderPickerUtil() { }

    /* ========================= 回调接口 ========================= */
    public interface OnGenderChangedListener {
        /**
         * @param index 选中项下标
         * @param text  选中项文字
         */
        void onGenderChanged(int index, String text);
    }

    /* ========================= 对外快捷入口 ========================= */
    public static Builder beautify(ViewGroup container) {
        return build(container);
    }

    public static Builder beautify(ViewGroup container, int color) {
        return build(container).selectedColor(color);
    }

    public static Builder build(ViewGroup container) {
        return new Builder(container);
    }

    public static Builder with(ViewGroup container) {
        return build(container);
    }

    /* ========================= Builder ========================= */
    public static final class Builder {

        private final ViewGroup container;

        /* ---- 数据 ---- */
        private List<String> options = new ArrayList<>();
        private int          defaultIndex = 0;

        /* ---- 外观 ---- */
        private int   selectedColor     = DEFAULT_COLOR;
        private int   backgroundColor   = DEFAULT_BG;
        private int   textColor         = DEFAULT_TEXT_COLOR;
        private int   selectedTextColor = DEFAULT_SELECTED_TEXT;
        private float radiusDp          = DEFAULT_RADIUS_DP;
        private float heightDp          = DEFAULT_HEIGHT_DP;
        private float textSizeSp        = DEFAULT_TEXT_SIZE_SP;
        private float paddingDp         = DEFAULT_PADDING_DP;
        private boolean bold            = true;

        /* ---- 回调 ---- */
        private OnGenderChangedListener listener = null;

        /* ---- 状态 ---- */
        private LinearLayout row;
        private final List<TextView> items = new ArrayList<>();
        private int currentIndex = 0;

        private Builder(ViewGroup container) {
            this.container = container;
            /* 默认男 / 女 */
            options.add("男");
            options.add("女");
        }

        /* ---- 数据配置 ---- */
        public Builder options(String... opts) {
            if (opts != null && opts.length > 0) {
                options = new ArrayList<>();
                for (String o : opts) {
                    options.add(o == null ? "" : o);
                }
            }
            return this;
        }

        public Builder defaultIndex(int index) {
            this.defaultIndex = index;
            return this;
        }

        /* ---- 外观配置 ---- */
        public Builder selectedColor(int color) { this.selectedColor = color; return this; }
        public Builder backgroundColor(int color) { this.backgroundColor = color; return this; }
        public Builder textColor(int color) { this.textColor = color; return this; }
        public Builder selectedTextColor(int color) { this.selectedTextColor = color; return this; }
        public Builder radius(float radiusDp) { this.radiusDp = radiusDp; return this; }
        public Builder height(float heightDp) { this.heightDp = heightDp; return this; }
        public Builder textSize(float sizeSp) { this.textSizeSp = sizeSp; return this; }
        public Builder bold(boolean bold) { this.bold = bold; return this; }

        /* ---- 回调 ---- */
        public Builder onChanged(OnGenderChangedListener listener) {
            this.listener = listener;
            return this;
        }

        /* ---- 核心应用 ---- */
        public void apply() {
            if (container == null || options.isEmpty()) return;
            Context ctx = container.getContext();

            /* 规范化默认下标 */
            if (defaultIndex < 0) defaultIndex = 0;
            if (defaultIndex >= options.size()) defaultIndex = options.size() - 1;
            currentIndex = defaultIndex;

            container.removeAllViews();

            /* ---- 外层胶囊背景 ---- */
            GradientDrawable outer = new GradientDrawable();
            outer.setColor(backgroundColor);
            outer.setCornerRadius(dp(radiusDp, ctx));
            container.setBackground(outer);

            int pad = dp(paddingDp, ctx);
            container.setPadding(pad, pad, pad, pad);

            /* ---- 获得可用的水平 LinearLayout 作为行容器 ---- */
            if (container instanceof LinearLayout) {
                row = (LinearLayout) container;
                row.setOrientation(LinearLayout.HORIZONTAL);
            } else {
                row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                container.addView(row);
            }

            /* ---- 生成选项 ---- */
            items.clear();
            for (int i = 0; i < options.size(); i++) {
                final int idx = i;
                final String text = options.get(i);

                TextView tv = new TextView(ctx);
                tv.setText(text);
                tv.setGravity(Gravity.CENTER);
                tv.setIncludeFontPadding(false);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, dp(heightDp, ctx), 1f);
                tv.setLayoutParams(lp);

                tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currentIndex == idx) return;
                        currentIndex = idx;
                        updateStyles();
                        if (listener != null) listener.onGenderChanged(idx, text);
                    }
                });

                items.add(tv);
                row.addView(tv);
            }

            updateStyles();
        }

        /* ---- 刷新选中 / 未选中样式 ---- */
        private void updateStyles() {
            if (items.isEmpty()) return;
            Context ctx = items.get(0).getContext();

            /* 选中块圆角 = 外层圆角 − 内边距，最小 0 */
            float innerRadius = Math.max(0f, radiusDp - paddingDp);

            for (int i = 0; i < items.size(); i++) {
                TextView tv = items.get(i);
                if (i == currentIndex) {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(selectedColor);
                    bg.setCornerRadius(dp(innerRadius, ctx));
                    tv.setBackground(bg);
                    tv.setTextColor(selectedTextColor);
                    tv.setTypeface(Typeface.DEFAULT,
                            bold ? Typeface.BOLD : Typeface.NORMAL);
                } else {
                    tv.setBackground(null);
                    tv.setTextColor(textColor);
                    tv.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
                }
            }
        }

        /* ---- dp ---- */
        private int dp(float value, Context ctx) {
            return Math.round(value * ctx.getResources().getDisplayMetrics().density);
        }
    }
}