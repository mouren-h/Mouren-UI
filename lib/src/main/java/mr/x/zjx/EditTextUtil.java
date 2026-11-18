package mr.x.zjx;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.widget.EditText;

/**
 * 编辑框（EditText）美化工具类
 *
 * 特性：
 *  - 圆角 / 描边 / 渐变
 *  - 三态背景：普通 / 聚焦（点击后高亮）/ 按下 / 禁用
 *  - 可选水波纹（点击效果）
 *  - 文字 / 提示 / 光标颜色
 *
 * 用法：
 *   EditText et = findViewById(R.id.et1);
 *   EditTextUtil.beautify(et);                                    // 一键默认
 *   EditTextUtil.beautify(et, 0xFF3F7EF7);                        // 指定主色
 *
 *   EditTextUtil.with(et)                                         // 链式自定义
 *       .accentColor(0xFF3F7EF7)
 *       .backgroundColor(0xFFF5F7FA)
 *       .stroke(1, 0xFFDDE1E7)
 *       .strokeFocused(2, 0xFF3F7EF7)     // 聚焦时边框加粗并高亮
 *       .radius(10)
 *       .textColor(0xFF333333)
 *       .hintColor(0xFFAAAAAA)
 *       .ripple(true)
 *       .apply();
 */
public final class EditTextUtil {

    /** 默认主色（聚焦 / 光标） */
    public static final int DEFAULT_ACCENT_COLOR   = 0xFF3F7EF7;
    /** 默认背景色 */
    public static final int DEFAULT_BG_COLOR       = 0xFFFFFFFF;
    /** 默认描边色 */
    public static final int DEFAULT_STROKE_COLOR   = 0xFFDDE1E7;
    /** 默认文字色 */
    public static final int DEFAULT_TEXT_COLOR     = 0xFF333333;
    /** 默认提示文字色 */
    public static final int DEFAULT_HINT_COLOR     = 0xFFAAAAAA;
    /** 默认禁用背景 */
    public static final int DEFAULT_DISABLED_BG    = 0xFFF5F5F5;
    /** 默认禁用描边 */
    public static final int DEFAULT_DISABLED_STROKE = 0xFFE5E5E5;
    /** 默认圆角(dp) */
    public static final float DEFAULT_RADIUS_DP    = 8f;
    /** 默认描边宽度(dp) */
    public static final float DEFAULT_STROKE_WIDTH_DP = 1f;

    private EditTextUtil() {
        // 禁止实例化
    }

    /* ========================= 对外快捷入口 ========================= */

    /** 一键美化（默认主题） */
    public static void beautify(EditText et) {
        beautify(et, DEFAULT_ACCENT_COLOR);
    }

    /** 一键美化（指定主色） */
    public static void beautify(EditText et, int accentColor) {
        if (et == null) {
            return;
        }
        with(et).accentColor(accentColor).apply();
    }

    /** 返回 Builder，可链式自定义 */
    public static Builder with(EditText et) {
        return new Builder(et);
    }

    /* ========================= Builder ========================= */

    public static final class Builder {

        private final EditText et;

        // ---- 主色 ----
        private int accentColor = DEFAULT_ACCENT_COLOR;

        // ---- 背景 ----
        private int bgColor = DEFAULT_BG_COLOR;
        private int bgFocusedColor = 0;              // 0 = 自动（accent 的 4% 透明）
        private int bgPressedColor = 0;              // 0 = 自动（accent 的 8% 透明）
        private int bgDisabledColor = DEFAULT_DISABLED_BG;

        // ---- 描边 ----
        private int strokeColor = DEFAULT_STROKE_COLOR;
        private int strokeFocusedColor = 0;          // 0 = accentColor
        private int strokeDisabledColor = DEFAULT_DISABLED_STROKE;
        private float strokeWidthDp = DEFAULT_STROKE_WIDTH_DP;
        private float strokeFocusedWidthDp = 0;      // 0 = 同 strokeWidthDp

        // ---- 形状 ----
        private float radiusDp = DEFAULT_RADIUS_DP;
        private float[] cornerRadii = null;

        // ---- 渐变（只作用于普通态） ----
        private boolean gradient = false;
        private int gradientEndColor = 0;
        private GradientDrawable.Orientation orientation = GradientDrawable.Orientation.LEFT_RIGHT;

        // ---- 文字 ----
        private int textColor = DEFAULT_TEXT_COLOR;
        private int hintColor = DEFAULT_HINT_COLOR;
        private int cursorColor = 0;                 // 0 = accentColor
        private boolean bold = false;
        private Float textSizeSp = null;

        // ---- 内边距 ----
        private Integer paddingH = null;
        private Integer paddingV = null;
        private Integer padL = null, padT = null, padR = null, padB = null;

        // ---- 水波纹（点击效果） ----
        private boolean ripple = false;
        private int rippleColor = 0;                 // 0 = accent 的 15%

        private Builder(EditText et) {
            this.et = et;
        }

        /* ---------------------- 主色 / 背景 ---------------------- */

        /** 主色（聚焦边框、光标、水波纹默认色） */
        public Builder accentColor(int color) {
            this.accentColor = color;
            return this;
        }

        /** 普通态背景色 */
        public Builder backgroundColor(int color) {
            this.bgColor = color;
            return this;
        }

        /** 聚焦态背景色（不设置则使用 accent 的 4% alpha） */
        public Builder focusedBackgroundColor(int color) {
            this.bgFocusedColor = color;
            return this;
        }

        /** 按下态背景色（不设置则使用 accent 的 8% alpha） */
        public Builder pressedBackgroundColor(int color) {
            this.bgPressedColor = color;
            return this;
        }

        /** 禁用态背景色 */
        public Builder disabledBackgroundColor(int color) {
            this.bgDisabledColor = color;
            return this;
        }

        /* ---------------------- 描边 ---------------------- */

        /** 普通态描边：宽度(dp) + 颜色 */
        public Builder stroke(float widthDp, int color) {
            this.strokeWidthDp = widthDp;
            this.strokeColor = color;
            return this;
        }

        /** 普通态描边色（仅改色） */
        public Builder strokeColor(int color) {
            this.strokeColor = color;
            return this;
        }

        /** 聚焦态描边：宽度(dp) + 颜色（点击后明显的视觉效果） */
        public Builder strokeFocused(float widthDp, int color) {
            this.strokeFocusedWidthDp = widthDp;
            this.strokeFocusedColor = color;
            return this;
        }

        /** 禁用态描边色 */
        public Builder strokeDisabledColor(int color) {
            this.strokeDisabledColor = color;
            return this;
        }

        /* ---------------------- 形状 ---------------------- */

        /** 圆角(dp) */
        public Builder radius(float radiusDp) {
            this.radiusDp = radiusDp;
            this.cornerRadii = null;
            return this;
        }

        /** 每个角单独设置圆角(dp)：左上、右上、右下、左下 */
        public Builder corners(float tl, float tr, float br, float bl) {
            this.cornerRadii = new float[]{
                    dp(tl), dp(tl),
                    dp(tr), dp(tr),
                    dp(br), dp(br),
                    dp(bl), dp(bl)
            };
            return this;
        }

        /* ---------------------- 渐变 ---------------------- */

        /** 线性渐变（只作用于普通态，其他状态仍为纯色） */
        public Builder gradient(int endColor) {
            return gradient(endColor, GradientDrawable.Orientation.LEFT_RIGHT);
        }

        /** 线性渐变 + 方向 */
        public Builder gradient(int endColor, GradientDrawable.Orientation o) {
            this.gradient = true;
            this.gradientEndColor = endColor;
            this.orientation = o;
            return this;
        }

        /* ---------------------- 文字 ---------------------- */

        /** 输入文字颜色 */
        public Builder textColor(int color) {
            this.textColor = color;
            return this;
        }

        /** hint 提示文字颜色 */
        public Builder hintColor(int color) {
            this.hintColor = color;
            return this;
        }

        /** 光标颜色（API 29+ 生效） */
        public Builder cursorColor(int color) {
            this.cursorColor = color;
            return this;
        }

        /** 是否加粗 */
        public Builder bold(boolean bold) {
            this.bold = bold;
            return this;
        }

        /** 文字大小(sp) */
        public Builder textSize(float sp) {
            this.textSizeSp = sp;
            return this;
        }

        /* ---------------------- 内边距 ---------------------- */

        /** 水平 / 垂直内边距(dp)，传 null 表示保留原值 */
        public Builder padding(Integer hDp, Integer vDp) {
            this.paddingH = hDp;
            this.paddingV = vDp;
            return this;
        }

        /** 四边独立内边距(dp)，传 null 表示保留原值 */
        public Builder padding(Integer lDp, Integer tDp, Integer rDp, Integer bDp) {
            this.padL = lDp;
            this.padT = tDp;
            this.padR = rDp;
            this.padB = bDp;
            return this;
        }

        /* ---------------------- 水波纹 ---------------------- */

        /** 是否开启点击水波纹 */
        public Builder ripple(boolean enable) {
            this.ripple = enable;
            return this;
        }

        /** 水波纹 + 自定义颜色 */
        public Builder ripple(boolean enable, int color) {
            this.ripple = enable;
            this.rippleColor = color;
            return this;
        }

        /* ---------------------- 核心应用 ---------------------- */

        public void apply() {
            if (et == null) {
                return;
            }

            final int focusAccent = accentColor;
            final int strokeFocus = strokeFocusedColor != 0 ? strokeFocusedColor : focusAccent;
            final int bgFocus = bgFocusedColor != 0 ? bgFocusedColor : withAlpha(focusAccent, 0.04f);
            final int bgPress = bgPressedColor != 0 ? bgPressedColor : withAlpha(focusAccent, 0.08f);
            final float strokeFocusW = strokeFocusedWidthDp > 0 ? strokeFocusedWidthDp : strokeWidthDp;

            // 1. 三态背景
            GradientDrawable normal   = buildShape(bgColor,         strokeColor,         strokeWidthDp, true);
            GradientDrawable focused  = buildShape(bgFocus,         strokeFocus,         strokeFocusW,  false);
            GradientDrawable pressed  = buildShape(bgPress,         strokeFocus,         strokeFocusW,  false);
            GradientDrawable disabled = buildShape(bgDisabledColor, strokeDisabledColor, strokeWidthDp, false);

            StateListDrawable states = new StateListDrawable();
            states.addState(new int[]{-android.R.attr.state_enabled}, disabled);
            states.addState(new int[]{android.R.attr.state_focused}, focused);
            states.addState(new int[]{android.R.attr.state_pressed}, pressed);
            states.addState(new int[]{}, normal);

            // 2. 水波纹（可选）
            Drawable background;
            if (ripple && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int rc = rippleColor != 0 ? rippleColor : withAlpha(focusAccent, 0.15f);
                background = new RippleDrawable(
                        ColorStateList.valueOf(rc),
                        states,
                        buildShape(Color.WHITE, 0, 0, false));
            } else {
                background = states;
            }

            // 3. 保留原 padding，避免 setBackground 造成位移
            int pl = et.getPaddingLeft();
            int pt = et.getPaddingTop();
            int pr = et.getPaddingRight();
            int pb = et.getPaddingBottom();

            et.setBackground(background);

            if (paddingH != null || paddingV != null
                    || padL != null || padT != null || padR != null || padB != null) {
                int l = padL != null ? (int) dp(padL)
                        : (paddingH != null ? (int) dp(paddingH) : pl);
                int t = padT != null ? (int) dp(padT)
                        : (paddingV != null ? (int) dp(paddingV) : pt);
                int r = padR != null ? (int) dp(padR)
                        : (paddingH != null ? (int) dp(paddingH) : pr);
                int b = padB != null ? (int) dp(padB)
                        : (paddingV != null ? (int) dp(paddingV) : pb);
                et.setPadding(l, t, r, b);
            }

            // 4. 文字 / 提示颜色
            et.setTextColor(textColor);
            et.setHintTextColor(hintColor);

            // 5. 光标颜色
            applyCursorColor(et, cursorColor != 0 ? cursorColor : focusAccent);

            // 6. 字体
            if (bold) {
                et.setTypeface(et.getTypeface(), Typeface.BOLD);
            }
            if (textSizeSp != null) {
                et.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
            }
        }

        /* ---------------------- 内部工具 ---------------------- */

        /** 构建圆角矩形 drawable，allowGradient=true 时普通态使用渐变 */
        private GradientDrawable buildShape(int fillColor, int strokeCol,
                                            float strokeW, boolean allowGradient) {
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.RECTANGLE);

            if (cornerRadii != null) {
                gd.setCornerRadii(cornerRadii);
            } else {
                gd.setCornerRadius(dp(radiusDp));
            }

            if (allowGradient && gradient && gradientEndColor != 0) {
                gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                gd.setOrientation(orientation);
                gd.setColors(new int[]{fillColor, gradientEndColor});
            } else {
                gd.setColor(fillColor);
            }

            if (strokeW > 0 && strokeCol != 0) {
                gd.setStroke(Math.max(1, (int) dp(strokeW)), strokeCol);
            }
            return gd;
        }

        private float dp(float v) {
            return v * et.getResources().getDisplayMetrics().density + 0.5f;
        }
    }

    /* ========================= 光标颜色 ========================= */

    /** 设置 EditText 光标颜色（API 29+ 生效，低版本忽略） */
    private static void applyCursorColor(EditText et, int color) {
        if (color == 0) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        try {
            Drawable d = et.getTextCursorDrawable();
            if (d != null) {
                d = d.mutate();
                d.setTint(color);
                et.setTextCursorDrawable(d);
            } else {
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(color);
                int h = et.getLineHeight() > 0 ? et.getLineHeight()
                        : (int) (20 * et.getResources().getDisplayMetrics().density);
                gd.setSize(Math.max(2, (int) (2 * et.getResources().getDisplayMetrics().density)), h);
                et.setTextCursorDrawable(gd);
            }
        } catch (Exception ignored) {
            // 部分定制 ROM 可能抛异常，忽略
        }
    }

    /* ========================= 颜色工具 ========================= */

    /** 按比例调整 alpha */
    private static int withAlpha(int color, float alphaFactor) {
        int a = (int) (Color.alpha(color) * alphaFactor);
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }
}