package mr.x.zjx;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.TextView;

/**
 * 文本控件描边文字工具：文字描边、内部填充 / 渐变填充、圆角背景、描边、水波纹、阴影。
 * 仅使用 Android 原生 API，兼容 API 25+。
 */
public final class StrokeTextUtil {

    public static final int   DEFAULT_COLOR          = 0xFF3F7EF7;
    public static final int   DEFAULT_DISABLED_COLOR = 0xFFBFC6D1;
    public static final float DEFAULT_RADIUS_DP      = 10f;
    public static final float DEFAULT_STROKE_WIDTH_DP = 2f;

    private StrokeTextUtil() { }

    /* ========================= 对外快捷入口 ========================= */
    public static Builder beautify(TextView v) {
        return beautify(v, DEFAULT_COLOR);
    }

    public static Builder beautify(TextView v, int color) {
        return with(v)
                .strokeColor(color)
                .fillColor(Color.WHITE)
                .strokeWidth((int) DEFAULT_STROKE_WIDTH_DP);
    }

    public static Builder with(TextView v) {
        return new Builder(v);
    }

    /* ========================= Builder ========================= */
    public static final class Builder {

        private final TextView view;

        /* ---- 颜色 ---- */
        private int   color         = DEFAULT_COLOR;
        private int   disabledColor = DEFAULT_DISABLED_COLOR;
        private int   pressedColor  = 0;

        /* ---- 文字描边 ---- */
        private int   strokeColor     = DEFAULT_COLOR;
        private int   fillColor       = Color.WHITE;
        private float strokeWidthDp   = DEFAULT_STROKE_WIDTH_DP;
        private int   fillGradient[]  = null;

        /* ---- 文字 ---- */
        private int     textDisabledColor = 0;
        private boolean bold              = false;
        private boolean allCaps           = false;
        private float   textSizeDp        = 0f;

        /* ---- 形状圆角 ---- */
        private float   radiusDp = DEFAULT_RADIUS_DP;
        private boolean circle   = false;

        /* ---- 尺寸 ---- */
        private Integer widthDp     = null;
        private Integer heightDp    = null;
        private Integer minWidthDp  = null;
        private Integer minHeightDp = null;

        /* ---- 背景描边 ---- */
        private int bgStrokeWidthDp = 0;
        private int bgStrokeColor   = 0;

        /* ---- 水波纹阴影 ---- */
        private boolean ripple      = false;
        private boolean elevation   = false;
        private float   elevationDp = 4f;

        /* ---- 内边距 ---- */
        private Integer paddingHDp = null;
        private Integer paddingVDp = null;

        private Builder(TextView v) {
            this.view = v;
        }

        /* ---- 颜色 ---- */
        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder disabledColor(int color) {
            this.disabledColor = color;
            return this;
        }

        public Builder pressedColor(int color) {
            this.pressedColor = color;
            return this;
        }

        /* ---- 文字描边 ---- */
        public Builder strokeColor(int color) {
            this.strokeColor = color;
            return this;
        }

        public Builder fillColor(int color) {
            this.fillColor = color;
            this.fillGradient = null;
            return this;
        }

        public Builder strokeWidth(int widthDp) {
            this.strokeWidthDp = widthDp;
            return this;
        }

        public Builder gradient(int startColor, int endColor) {
            this.fillGradient = new int[]{ startColor, endColor };
            return this;
        }

        public Builder gradient(int[] colors) {
            this.fillGradient = colors;
            return this;
        }

        /* ---- 文字 ---- */
        public Builder textColors(int normalColor, int disabledColor) {
            this.textDisabledColor = disabledColor;
            return this;
        }

        public Builder bold(boolean bold) {
            this.bold = bold;
            return this;
        }

        public Builder allCaps(boolean allCaps) {
            this.allCaps = allCaps;
            return this;
        }

        public Builder textSize(float sizeDp) {
            this.textSizeDp = sizeDp;
            return this;
        }

        /* ---- 形状圆角 ---- */
        public Builder radius(float radiusDp) {
            this.radiusDp = radiusDp;
            this.circle = false;
            return this;
        }

        public Builder corners(float radiusDp) {
            return radius(radiusDp);
        }

        public Builder circle() {
            this.circle = true;
            return this;
        }

        /* ---- 尺寸 ---- */
        public Builder size(int widthDp, int heightDp) {
            this.widthDp = widthDp;
            this.heightDp = heightDp;
            return this;
        }

        public Builder minSize(int widthDp, int heightDp) {
            this.minWidthDp = widthDp;
            this.minHeightDp = heightDp;
            return this;
        }

        /* ---- 背景描边 ---- */
        public Builder bgStroke(int widthDp, int color) {
            this.bgStrokeWidthDp = widthDp;
            this.bgStrokeColor = color;
            return this;
        }

        /* ---- 水波纹阴影 ---- */
        public Builder ripple(boolean ripple) {
            this.ripple = ripple;
            return this;
        }

        public Builder elevation(boolean elevation) {
            this.elevation = elevation;
            return this;
        }

        public Builder elevation(float elevationDp) {
            this.elevation = true;
            this.elevationDp = elevationDp;
            return this;
        }

        /* ---- 内边距 ---- */
        public Builder padding(Integer hDp, Integer vDp) {
            this.paddingHDp = hDp;
            this.paddingVDp = vDp;
            return this;
        }

        /* ---- 核心应用 ---- */
        public void apply() {
            if (view == null) return;

            /* ---- 背景三态 ---- */
            int pressed = pressedColor != 0 ? pressedColor : darken(color, 0.82f);

            GradientDrawable normal   = buildShape(color);
            GradientDrawable pressedD = buildShape(pressed);
            GradientDrawable disabled = buildShape(disabledColor);

            StateListDrawable sld = new StateListDrawable();
            sld.addState(new int[]{ -android.R.attr.state_enabled }, disabled);
            sld.addState(new int[]{ android.R.attr.state_pressed, android.R.attr.state_enabled }, pressedD);
            sld.addState(new int[]{}, normal);

            /* ---- 保存原始 padding ---- */
            int pl = view.getPaddingLeft();
            int pt = view.getPaddingTop();
            int pr = view.getPaddingRight();
            int pb = view.getPaddingBottom();

            /* ---- 水波纹 ---- */
            if (ripple && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                GradientDrawable mask;
                if (circle) {
                    mask = new GradientDrawable();
                    mask.setShape(GradientDrawable.OVAL);
                    mask.setColor(Color.WHITE);
                } else {
                    mask = buildShape(Color.WHITE);
                    mask.setStroke(0, Color.TRANSPARENT);
                }
                RippleDrawable rd = new RippleDrawable(
                        ColorStateList.valueOf(withAlpha(pressed, 0.36f)), sld, mask);
                view.setBackground(rd);
            } else {
                view.setBackground(sld);
            }

            /* ---- 恢复 padding ---- */
            int newPl = paddingHDp != null ? dp(paddingHDp) : pl;
            int newPt = paddingVDp != null ? dp(paddingVDp) : pt;
            int newPr = paddingHDp != null ? dp(paddingHDp) : pr;
            int newPb = paddingVDp != null ? dp(paddingVDp) : pb;
            view.setPadding(newPl, newPt, newPr, newPb);

            /* ---- 尺寸 ---- */
            if (widthDp != null || heightDp != null) {
                ViewGroup.LayoutParams lp = view.getLayoutParams();
                if (lp != null) {
                    if (widthDp != null)  lp.width  = dp(widthDp);
                    if (heightDp != null) lp.height = dp(heightDp);
                }
            }
            if (minWidthDp != null)  view.setMinimumWidth(dp(minWidthDp));
            if (minHeightDp != null) view.setMinimumHeight(dp(minHeightDp));

            /* ---- 文字大小 ---- */
            if (textSizeDp > 0f) {
                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(textSizeDp));
            }

            /* ---- 文字颜色三态（描边开启时内部色由 Paint 控制，禁用走禁用色） ---- */
            int disText = textDisabledColor != 0
                    ? textDisabledColor
                    : withAlpha(strokeColor, 0.45f);

            ColorStateList textCsl = new ColorStateList(
                    new int[][]{
                            new int[]{ -android.R.attr.state_enabled },
                            new int[]{}
                    },
                    new int[]{ disText, fillColor });
            view.setTextColor(textCsl);

            /* ---- 描边文字 ---- */
            if (view.isEnabled()) {
                applyStrokeText();
            } else {
                view.getPaint().setShader(null);
                view.getPaint().clearShadowLayer();
                view.invalidate();
            }

            /* ---- 文字加粗 / 大写 ---- */
            view.setAllCaps(allCaps);
            view.setTypeface(view.getTypeface(), bold ? Typeface.BOLD : Typeface.NORMAL);

            /* ---- 阴影 ---- */
            if (elevation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.setElevation(dp(elevationDp));
                view.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View v, Outline outline) {
                        if (circle) {
                            outline.setOval(0, 0, v.getWidth(), v.getHeight());
                        } else {
                            outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(radiusDp));
                        }
                    }
                });
                view.setClipToOutline(true);
            }
        }

        /* ---- 描边文字（Paint 描边 + 填充双层） ---- */
        private void applyStrokeText() {
            if (view == null) return;

            Paint paint = view.getPaint();

            /* 先清除旧的阴影/描边状态，避免叠加 */
            paint.clearShadowLayer();

            if (strokeWidthDp > 0f && strokeColor != 0) {
                /* 用阴影层模拟描边：偏移 0，扩散半径 = 描边宽度 */
                paint.setShadowLayer(dp(strokeWidthDp), 0f, 0f, strokeColor);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }
            } else {
                paint.clearShadowLayer();
            }

            /* 填充：纯色或渐变 */
            if (fillGradient != null && fillGradient.length >= 2) {
                int w = view.getWidth();
                if (w > 0) {
                    paint.setShader(new LinearGradient(
                            0, 0, w, 0, fillGradient, null, Shader.TileMode.CLAMP));
                } else {
                    paint.setShader(null);
                }
            } else {
                paint.setShader(null);
            }

            view.invalidate();
        }

        /* ---- 内部工具 ---- */
        private GradientDrawable buildShape(int fillColor) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(circle ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
            d.setColor(fillColor);
            if (!circle) {
                d.setCornerRadius(dp(radiusDp));
            }
            if (bgStrokeWidthDp > 0 && bgStrokeColor != 0) {
                d.setStroke(dp(bgStrokeWidthDp), bgStrokeColor);
            }
            return d;
        }

        private int dp(float value) {
            return Math.round(value * view.getResources().getDisplayMetrics().density);
        }
    }

    /* ========================= 颜色工具 ========================= */
    public static int darken(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min(255, Math.max(0, (int) (Color.red(color) * factor)));
        int g = Math.min(255, Math.max(0, (int) (Color.green(color) * factor)));
        int b = Math.min(255, Math.max(0, (int) (Color.blue(color) * factor)));
        return Color.argb(a, r, g, b);
    }

    public static int withAlpha(int color, float alpha) {
        int a = Math.min(255, Math.max(0, (int) (255 * alpha)));
        return (color & 0x00FFFFFF) | (a << 24);
    }
}