package mr.x.zjx;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.CheckBox;

/**
 * 多选框（CheckBox）美化工具类
 *
 * 修复点：
 *  - 不再使用 LayerDrawable（会被拉伸导致对勾不居中）
 *  - 改用自定义 CheckBoxDrawable，按 sizePx 居中绘制背景+描边+对勾
 *  - 对勾在任何 CheckBox 高度下都严格居中、不变形
 *
 * 特性：
 *  - 自定义方框（圆角矩形 + 描边 + 白色对勾）
 *  - 四态：未选 / 选中 / 禁用未选 / 禁用已选
 *  - 文字三态颜色：正常 / 选中（主色）/ 禁用（灰色）
 *  - 点击水波纹（API 21+）
 *  - 点击缩放动画
 *
 * 用法：
 *   CheckBox cb = findViewById(R.id.cb1);
 *   CheckBoxUtil.beautify(cb);
 *   CheckBoxUtil.beautify(cb, 0xFF4CAF50);
 *
 *   CheckBoxUtil.with(cb)
 *       .color(0xFF3F7EF7)
 *       .size(24)
 *       .radius(6)
 *       .ripple(true)
 *       .clickScale(true)
 *       .apply();
 */
public final class CheckBoxUtil {

    public static final int DEFAULT_COLOR         = 0xFF3F7EF7;
    public static final int DEFAULT_BORDER_COLOR  = 0xFFCCCCCC;
    public static final int DEFAULT_TEXT_COLOR    = 0xFF333333;
    public static final int DEFAULT_TEXT_DISABLED = 0xFFBDBDBD;
    public static final float DEFAULT_SIZE_DP     = 22f;
    public static final float DEFAULT_RADIUS_DP   = 4f;
    public static final float DEFAULT_BORDER_DP   = 1.5f;

    private CheckBoxUtil() {
        // 禁止实例化
    }

    /* ========================= 对外快捷入口 ========================= */

    /** 一键美化（默认主题） */
    public static void beautify(CheckBox cb) {
        beautify(cb, DEFAULT_COLOR);
    }

    /** 一键美化（指定主色） */
    public static void beautify(CheckBox cb, int color) {
        if (cb == null) return;
        with(cb).color(color).apply();
    }

    /** 返回 Builder，可链式自定义 */
    public static Builder with(CheckBox cb) {
        return new Builder(cb);
    }

    /* ========================= Builder ========================= */

    public static final class Builder {

        private final CheckBox cb;

        // ---- 颜色 ----
        private int color = DEFAULT_COLOR;
        private int borderColor = DEFAULT_BORDER_COLOR;
        private float borderWidthDp = DEFAULT_BORDER_DP;
        private int checkedFillColor = 0;             // 0 = color
        private int checkmarkColor = Color.WHITE;

        // ---- 尺寸 / 形状 ----
        private float sizeDp = DEFAULT_SIZE_DP;
        private float radiusDp = DEFAULT_RADIUS_DP;

        // ---- 文字 ----
        private int textColor = DEFAULT_TEXT_COLOR;
        private int textColorChecked = 0;             // 0 = color
        private int textColorDisabled = DEFAULT_TEXT_DISABLED;
        private boolean keepTextColors = false;
        private boolean bold = false;
        private float textSizeSp = 0;

        // ---- 水波纹 ----
        private boolean ripple = true;
        private int rippleColor = 0;                  // 0 = 主色 20%

        // ---- 点击缩放动画 ----
        private boolean clickScale = true;
        private float clickScaleFactor = 0.92f;

        // ---- drawable 与文字间距 ----
        private float compoundPaddingDp = 8f;

        private Builder(CheckBox cb) {
            this.cb = cb;
        }

        /* ---------------------- 颜色 ---------------------- */

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder borderColor(int color) {
            this.borderColor = color;
            return this;
        }

        public Builder borderWidth(float dp) {
            this.borderWidthDp = dp;
            return this;
        }

        public Builder checkedFillColor(int color) {
            this.checkedFillColor = color;
            return this;
        }

        public Builder checkmarkColor(int color) {
            this.checkmarkColor = color;
            return this;
        }

        /* ---------------------- 尺寸 / 形状 ---------------------- */

        /** 方框边长(dp) */
        public Builder size(float dp) {
            this.sizeDp = dp;
            return this;
        }

        /** 方框圆角(dp)，设成 size/2 即为圆形 */
        public Builder radius(float dp) {
            this.radiusDp = dp;
            return this;
        }

        /* ---------------------- 文字 ---------------------- */

        public Builder textColor(int color) {
            this.textColor = color;
            return this;
        }

        public Builder textColorChecked(int color) {
            this.textColorChecked = color;
            return this;
        }

        public Builder textColorDisabled(int color) {
            this.textColorDisabled = color;
            return this;
        }

        public Builder keepTextColors(boolean keep) {
            this.keepTextColors = keep;
            return this;
        }

        public Builder bold(boolean bold) {
            this.bold = bold;
            return this;
        }

        public Builder textSize(float sp) {
            this.textSizeSp = sp;
            return this;
        }

        /* ---------------------- 水波纹 ---------------------- */

        public Builder ripple(boolean enable) {
            this.ripple = enable;
            return this;
        }

        public Builder ripple(boolean enable, int color) {
            this.ripple = enable;
            this.rippleColor = color;
            return this;
        }

        /* ---------------------- 点击缩放 ---------------------- */

        public Builder clickScale(boolean enable) {
            this.clickScale = enable;
            return this;
        }

        public Builder clickScaleFactor(float factor) {
            this.clickScaleFactor = factor;
            return this;
        }

        /* ---------------------- 间距 ---------------------- */

        public Builder compoundPadding(float dp) {
            this.compoundPaddingDp = dp;
            return this;
        }

        /* ---------------------- 核心应用 ---------------------- */

        public void apply() {
            if (cb == null) return;

            final int fillChecked = checkedFillColor != 0 ? checkedFillColor : color;
            final int textChecked = textColorChecked != 0 ? textColorChecked : color;

            final float sizePx = dp(sizeDp);
            final float rPx    = dp(radiusDp);
            final float borderPx = Math.max(1f, dp(borderWidthDp));

            // 1. 四态 Drawable（每种状态都是"完整绘制"的 CheckBoxDrawable）
            CheckBoxDrawable unchecked = new CheckBoxDrawable(
                    sizePx, rPx, borderPx,
                    0, borderColor,
                    false, 0);

            CheckBoxDrawable checked = new CheckBoxDrawable(
                    sizePx, rPx, 0,
                    fillChecked, 0,
                    true, checkmarkColor);

            CheckBoxDrawable disabledUnchecked = new CheckBoxDrawable(
                    sizePx, rPx, borderPx,
                    0, withAlpha(borderColor, 0.5f),
                    false, 0);

            CheckBoxDrawable disabledChecked = new CheckBoxDrawable(
                    sizePx, rPx, 0,
                    withAlpha(fillChecked, 0.4f), 0,
                    true, withAlpha(checkmarkColor, 0.85f));

            // 2. StateListDrawable：四态
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[]{-android.R.attr.state_enabled, android.R.attr.state_checked},
                    disabledChecked);
            states.addState(new int[]{-android.R.attr.state_enabled}, disabledUnchecked);
            states.addState(new int[]{android.R.attr.state_checked}, checked);
            states.addState(new int[]{}, unchecked);

            // 3. 水波纹
            Drawable finalDrawable;
            if (ripple && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int rc = rippleColor != 0 ? rippleColor : withAlpha(color, 0.20f);
                GradientDrawable mask = new GradientDrawable();
                mask.setShape(GradientDrawable.RECTANGLE);
                mask.setCornerRadius(rPx);
                mask.setColor(Color.WHITE);
                mask.setSize((int) sizePx, (int) sizePx);
                finalDrawable = new RippleDrawable(
                        ColorStateList.valueOf(rc), states, mask);
            } else {
                finalDrawable = states;
            }

            // 4. 应用到 CheckBox
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cb.setButtonTintList(null);   // 清除系统 tint
            }
            cb.setButtonDrawable(null);       // 部分 ROM 会复用旧 drawable，先清一下
            cb.setButtonDrawable(finalDrawable);

            // 5. 文字三态
            if (!keepTextColors) {
                cb.setTextColor(new ColorStateList(
                        new int[][]{
                                new int[]{-android.R.attr.state_enabled},
                                new int[]{android.R.attr.state_checked},
                                new int[]{}
                        },
                        new int[]{textColorDisabled, textChecked, textColor}
                ));
            }

            // 6. 字体
            if (bold) {
                cb.setTypeface(cb.getTypeface(), Typeface.BOLD);
            }
            if (textSizeSp > 0) {
                cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
            }

            // 7. 图标与文字间距
            cb.setCompoundDrawablePadding((int) dp(compoundPaddingDp));

            // 8. 点击缩放
            if (clickScale) {
                final float f = clickScaleFactor;
                cb.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            v.animate().scaleX(f).scaleY(f).setDuration(100).start();
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                            break;
                    }
                    return false;  // 不消费，onClick 正常触发
                });
            }
        }

        private float dp(float v) {
            return v * cb.getResources().getDisplayMetrics().density + 0.5f;
        }
    }

    /* ========================= 自定义 Drawable ========================= */

    /**
     * 一次性绘制「圆角矩形背景 + 描边 + 对勾」，
     * 所有元素**基于 sizePx、以 bounds 中心为锚点**，因此在任何 bounds 下都严格居中。
     */
    private static class CheckBoxDrawable extends Drawable {

        private final float sizePx;
        private final float radiusPx;
        private final float strokePx;
        private final int fillColor;
        private final int strokeColor;
        private final boolean showCheck;
        private final int checkColor;
        private final float checkStrokePx;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Path checkPath = new Path();

        CheckBoxDrawable(float sizePx, float radiusPx, float strokePx,
                         int fillColor, int strokeColor,
                         boolean showCheck, int checkColor) {
            this.sizePx = sizePx;
            this.radiusPx = radiusPx;
            this.strokePx = strokePx;
            this.fillColor = fillColor;
            this.strokeColor = strokeColor;
            this.showCheck = showCheck;
            this.checkColor = checkColor;
            this.checkStrokePx = sizePx * 0.14f;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            // 以 bounds 中心为锚点，强制按 sizePx 绘制 → 永远不会偏移
            final float cx = b.exactCenterX();
            final float cy = b.exactCenterY();
            final float half = sizePx / 2f;

            // 1. 圆角矩形背景
            if (fillColor != 0) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(fillColor);
                rect.set(cx - half, cy - half, cx + half, cy + half);
                canvas.drawRoundRect(rect, radiusPx, radiusPx, paint);
            }

            // 2. 描边（向内收缩半个描边宽，避免裁切）
            if (strokePx > 0 && strokeColor != 0) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(strokeColor);
                paint.setStrokeWidth(strokePx);
                final float inset = strokePx / 2f;
                rect.set(cx - half + inset, cy - half + inset,
                        cx + half - inset, cy + half - inset);
                canvas.drawRoundRect(rect, radiusPx, radiusPx, paint);
            }

            // 3. 对勾（固定比例 + bounds 中心对齐）
            if (showCheck) {
                final float left = cx - half;
                final float top  = cy - half;

                // 对勾三点坐标（视觉居中：包围盒中心 ≈ 0.50）
                checkPath.reset();
                checkPath.moveTo(left + sizePx * 0.24f, top + sizePx * 0.52f);
                checkPath.lineTo(left + sizePx * 0.42f, top + sizePx * 0.72f);
                checkPath.lineTo(left + sizePx * 0.78f, top + sizePx * 0.28f);

                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(checkColor);
                paint.setStrokeWidth(checkStrokePx);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                canvas.drawPath(checkPath, paint);
            }
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        @Override public int getIntrinsicWidth() { return (int) sizePx; }
        @Override public int getIntrinsicHeight() { return (int) sizePx; }
    }

    /* ========================= 颜色工具 ========================= */

    private static int withAlpha(int color, float alphaFactor) {
        int a = (int) (Color.alpha(color) * alphaFactor);
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }
}