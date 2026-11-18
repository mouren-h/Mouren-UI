package mr.x.zjx;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;

/**
 * 按钮 UI 美化工具类
 *
 * 用法：
 *   Button btn = findViewById(R.id.an1);
 *   ButtonUtil.beautify(btn);                 // 一键默认美化
 *   ButtonUtil.beautify(btn, 0xFF3F7EF7);     // 指定主色美化
 *
 *   ButtonUtil.with(btn)                      // 链式自定义
 *       .color(0xFF3F7EF7)
 *       .radius(24)
 *       .gradient(0xFF5FA8FF)
 *       .textColor(Color.WHITE)
 *       .elevation(true, 4)
 *       .apply();
 *
 *   ButtonUtil.with(btn)                      // 圆形按钮
 *       .circle()
 *       .size(56, 56)
 *       .color(0xFFFF5722)
 *       .textColor(Color.WHITE)
 *       .apply();
 *
 *   ButtonUtil.with(btn)                      // 每个角单独控制
 *       .corners(20, 0, 20, 0)
 *       .color(0xFF7C4DFF)
 *       .apply();
 */
public final class ButtonUtil {

    /** 默认主色 */
    public static final int DEFAULT_COLOR = 0xFF3F7EF7;
    /** 默认禁用色 */
    public static final int DEFAULT_DISABLED_COLOR = 0xFFBFC6D1;
    /** 默认圆角(dp) */
    public static final float DEFAULT_RADIUS_DP = 10f;

    private ButtonUtil() {
        // 禁止实例化
    }

    /* ========================= 对外快捷入口 ========================= */

    /** 一键美化（默认主题） */
    public static void beautify(Button btn) {
        beautify(btn, DEFAULT_COLOR);
    }

    /** 一键美化（指定主色） */
    public static void beautify(Button btn, int color) {
        if (btn == null) {
            return;
        }
        with(btn).color(color).apply();
    }

    /** 返回 Builder，可链式自定义 */
    public static Builder with(Button btn) {
        return new Builder(btn);
    }

    /* ========================= Builder ========================= */

    public static final class Builder {

        private final Button btn;

        // ---- 颜色 ----
        private int color = DEFAULT_COLOR;
        private int colorPressed = 0;              // 0 = 自动计算（加深）
        private int colorDisabled = DEFAULT_DISABLED_COLOR;

        // ---- 形状 / 圆角 ----
        private int shapeType = GradientDrawable.RECTANGLE; // RECTANGLE / OVAL / LINE / RING
        private float radiusDp = DEFAULT_RADIUS_DP;
        private float[] cornerRadii = null;                 // 8 个值：每角 x/y 半径

        // ---- 描边 ----
        private float strokeWidthDp = 0f;
        private int strokeColor = 0;

        // ---- 渐变 ----
        private boolean gradient = false;
        private int gradientEndColor = 0;
        private GradientDrawable.Orientation orientation = GradientDrawable.Orientation.LEFT_RIGHT;

        // ---- 尺寸 ----
        private Integer fixedWidthDp = null;
        private Integer fixedHeightDp = null;
        private Integer minWidthDp = null;
        private Integer minHeightDp = null;

        // ---- 文字 ----
        private int textColor = Color.WHITE;
        private int textColorPressed = 0;
        private int textColorDisabled = 0;
        private boolean bold = true;
        private boolean allCaps = false;

        // ---- 水波纹 ----
        private boolean ripple = true;
        private int rippleColor = 0x33FFFFFF;      // 半透明白

        // ---- 阴影 ----
        private boolean elevation = true;
        private float elevationDp = 2f;

        // ---- 内边距 ----
        private Integer paddingH = null;
        private Integer paddingV = null;

        private Builder(Button btn) {
            this.btn = btn;
        }

        /* ---------------------- 颜色 ---------------------- */

        /** 主色（按下色自动加深） */
        public Builder color(int color) {
            this.color = color;
            return this;
        }

        /** 分别指定 正常色 / 按下色 */
        public Builder colors(int normal, int pressed) {
            this.color = normal;
            this.colorPressed = pressed;
            return this;
        }

        /** 禁用态颜色 */
        public Builder disabledColor(int color) {
            this.colorDisabled = color;
            return this;
        }

        /* ---------------------- 形状 / 圆角 ---------------------- */

        /** 形状：GradientDrawable.RECTANGLE / OVAL / LINE / RING */
        public Builder shape(int shapeType) {
            this.shapeType = shapeType;
            return this;
        }

        /** 圆形 / 椭圆形（配合 size() 使用即为正圆） */
        public Builder circle() {
            this.shapeType = GradientDrawable.OVAL;
            return this;
        }

        /** 圆角(dp)，传入大于高度一半的值即为胶囊按钮 */
        public Builder radius(float radiusDp) {
            this.radiusDp = radiusDp;
            this.cornerRadii = null;    // 覆盖 corners
            return this;
        }

        /**
         * 每个角单独设置圆角(dp)：左上、右上、右下、左下
         * 传 0 表示该角为直角
         */
        public Builder corners(float tl, float tr, float br, float bl) {
            this.cornerRadii = new float[]{
                    dp(tl), dp(tl),
                    dp(tr), dp(tr),
                    dp(br), dp(br),
                    dp(bl), dp(bl)
            };
            return this;
        }

        /* ---------------------- 尺寸 ---------------------- */

        /** 固定尺寸(dp) */
        public Builder size(int wDp, int hDp) {
            this.fixedWidthDp = wDp;
            this.fixedHeightDp = hDp;
            return this;
        }

        /** 最小尺寸(dp) */
        public Builder minSize(int wDp, int hDp) {
            this.minWidthDp = wDp;
            this.minHeightDp = hDp;
            return this;
        }

        /* ---------------------- 描边 / 渐变 ---------------------- */

        /** 描边 */
        public Builder stroke(float widthDp, int color) {
            this.strokeWidthDp = widthDp;
            this.strokeColor = color;
            return this;
        }

        /** 线性渐变（从主色渐变到 endColor） */
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

        /** 文字颜色 */
        public Builder textColor(int color) {
            this.textColor = color;
            return this;
        }

        /** 文字颜色（三态：正常 / 按下 / 禁用） */
        public Builder textColors(int normal, int pressed, int disabled) {
            this.textColor = normal;
            this.textColorPressed = pressed;
            this.textColorDisabled = disabled;
            return this;
        }

        /** 是否加粗 */
        public Builder bold(boolean bold) {
            this.bold = bold;
            return this;
        }

        /** 是否全部大写（Button 默认会大写，这里默认关闭） */
        public Builder allCaps(boolean allCaps) {
            this.allCaps = allCaps;
            return this;
        }

        /* ---------------------- 水波纹 / 阴影 ---------------------- */

        /** 水波纹效果（API 21+ 生效，低版本自动降级为按下变色） */
        public Builder ripple(boolean enable, int rippleColor) {
            this.ripple = enable;
            this.rippleColor = rippleColor;
            return this;
        }

        /** 阴影（API 21+ 生效） */
        public Builder elevation(boolean enable, float dp) {
            this.elevation = enable;
            this.elevationDp = dp;
            return this;
        }

        /* ---------------------- 内边距 ---------------------- */

        /** 内边距(dp)，传 null 表示不修改 */
        public Builder padding(Integer hDp, Integer vDp) {
            this.paddingH = hDp;
            this.paddingV = vDp;
            return this;
        }

        /* ---------------------- 核心应用 ---------------------- */

        public void apply() {
            if (btn == null) {
                return;
            }

            final int pressedColor = colorPressed != 0 ? colorPressed : darken(color, 0.82f);

            // 1. 背景：正常 / 按下 / 禁用 三态
            GradientDrawable normal   = buildShape(color);
            GradientDrawable pressed  = buildShape(pressedColor);
            GradientDrawable disabled = buildShape(colorDisabled);

            StateListDrawable states = new StateListDrawable();
            states.addState(new int[]{-android.R.attr.state_enabled}, disabled);
            states.addState(new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled}, pressed);
            states.addState(new int[]{}, normal);

            // 2. 水波纹包裹（带 mask，保证波纹不超出圆角；OVAL 使用椭圆 mask）
            Drawable background;
            if (ripple && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Drawable mask;
                if (shapeType == GradientDrawable.OVAL) {
                    GradientDrawable oval = new GradientDrawable();
                    oval.setShape(GradientDrawable.OVAL);
                    oval.setColor(Color.WHITE);
                    mask = oval;
                } else {
                    mask = buildShape(Color.WHITE);
                }
                background = new RippleDrawable(
                        ColorStateList.valueOf(rippleColor),
                        states,
                        mask);
            } else {
                background = states;
            }

            // 3. 先保留原 padding，避免 setBackground 造成位移
            int pl = btn.getPaddingLeft();
            int pt = btn.getPaddingTop();
            int pr = btn.getPaddingRight();
            int pb = btn.getPaddingBottom();

            btn.setBackground(background);

            // 4. 内边距
            if (paddingH != null || paddingV != null) {
                int h = paddingH != null ? (int) dp(paddingH) : pl;
                int v = paddingV != null ? (int) dp(paddingV) : pt;
                btn.setPadding(h, v, h, v);
            } else {
                btn.setPadding(pl, pt, pr, pb);
            }

            // 5. 固定尺寸 / 最小尺寸
            if (fixedWidthDp != null || fixedHeightDp != null) {
                ViewGroup.LayoutParams lp = btn.getLayoutParams();
                if (lp != null) {
                    if (fixedWidthDp  != null) lp.width  = (int) dp(fixedWidthDp);
                    if (fixedHeightDp != null) lp.height = (int) dp(fixedHeightDp);
                    btn.setLayoutParams(lp);
                }
            }
            if (minWidthDp  != null) btn.setMinimumWidth((int) dp(minWidthDp));
            if (minHeightDp != null) btn.setMinimumHeight((int) dp(minHeightDp));

            // 6. 文字颜色三态
            int tc  = textColor;
            int tcp = textColorPressed  != 0 ? textColorPressed  : tc;
            int tcd = textColorDisabled != 0 ? textColorDisabled : withAlpha(tc, 0.45f);
            btn.setTextColor(new ColorStateList(
                    new int[][]{
                            new int[]{-android.R.attr.state_enabled},
                            new int[]{android.R.attr.state_pressed},
                            new int[]{}
                    },
                    new int[]{tcd, tcp, tc}
            ));

            // 7. 字体
            btn.setAllCaps(allCaps);
            btn.setTypeface(btn.getTypeface(), bold ? Typeface.BOLD : Typeface.NORMAL);

            // 8. 阴影（圆角轮廓，OVAL 使用椭圆轮廓）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (elevation) {
                    btn.setElevation(dp(elevationDp));
                    final boolean isOval = (shapeType == GradientDrawable.OVAL);
                    final float r = dp(radiusDp);
                    btn.setOutlineProvider(new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, Outline outline) {
                            if (isOval) {
                                outline.setOval(0, 0, view.getWidth(), view.getHeight());
                            } else {
                                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
                            }
                        }
                    });
                } else {
                    btn.setElevation(0f);
                }
            }
        }

        /* ---------------------- 内部工具 ---------------------- */

        /** 私有构建方法（与公开的 shape(int shapeType) 区分开） */
        private GradientDrawable buildShape(int fillColor) {
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(shapeType);

            // 只有矩形才处理圆角；OVAL / LINE / RING 忽略圆角
            if (shapeType == GradientDrawable.RECTANGLE) {
                if (cornerRadii != null) {
                    gd.setCornerRadii(cornerRadii);
                } else {
                    gd.setCornerRadius(dp(radiusDp));
                }
            }

            if (gradient && gradientEndColor != 0) {
                gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                gd.setOrientation(orientation);
                gd.setColors(new int[]{fillColor, gradientEndColor});
            } else {
                gd.setColor(fillColor);
            }

            if (strokeWidthDp > 0 && strokeColor != 0) {
                gd.setStroke(Math.max(1, (int) dp(strokeWidthDp)), strokeColor);
            }
            return gd;
        }

        private float dp(float dp) {
            return dp * btn.getResources().getDisplayMetrics().density + 0.5f;
        }
    }

    /* ========================= 颜色工具 ========================= */

    /** 按比例加深颜色（保留 alpha） */
    private static int darken(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min(255, (int) (Color.red(color)   * factor));
        int g = Math.min(255, (int) (Color.green(color) * factor));
        int b = Math.min(255, (int) (Color.blue(color)  * factor));
        return Color.argb(a, r, g, b);
    }

    /** 按比例调整 alpha */
    private static int withAlpha(int color, float alphaFactor) {
        int a = (int) (Color.alpha(color) * alphaFactor);
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }
}