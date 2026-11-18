package mr.x.zjx;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
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
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

/**
 * 文本控件外观工具：渐变文字（支持无限流动画）、圆角背景、描边、水波纹、阴影。
 * 仅使用 Android 原生 API，兼容 API 25+。
 */
public final class TextUtil {

    public static final int   DEFAULT_COLOR          = 0xFF3F7EF7;
    public static final int   DEFAULT_DISABLED_COLOR = 0xFFBFC6D1;
    public static final float DEFAULT_RADIUS_DP      = 10f;
    public static final int   DEFAULT_ANIM_DURATION  = 2000;

    /* 渐变方向常量 */
    public static final int DIRECTION_HORIZONTAL = 0;
    public static final int DIRECTION_VERTICAL   = 1;
    public static final int DIRECTION_DIAGONAL   = 2;

    private TextUtil() { }

    /* ========================= 对外快捷入口 ========================= */
    public static Builder beautify(TextView v) {
        return beautify(v, DEFAULT_COLOR);
    }

    public static Builder beautify(TextView v, int color) {
        return with(v)
                .color(color)
                .gradient(color, darken(color, 0.72f));
    }

    public static Builder with(TextView v) {
        return new Builder(v);
    }

    /* ========================= Builder ========================= */
    public static final class Builder {

        private final TextView view;

        /* ---- 颜色 ---- */
        private int   color          = DEFAULT_COLOR;
        private int   disabledColor  = DEFAULT_DISABLED_COLOR;
        private int   pressedColor   = 0;
        private int[] gradientColors = null;

        /* ---- 形状圆角 ---- */
        private float   radiusDp = DEFAULT_RADIUS_DP;
        private boolean circle   = false;

        /* ---- 尺寸 ---- */
        private Integer widthDp     = null;
        private Integer heightDp    = null;
        private Integer minWidthDp  = null;
        private Integer minHeightDp = null;

        /* ---- 描边 ---- */
        private int strokeWidthDp = 0;
        private int strokeColor   = 0;

        /* ---- 文字 ---- */
        private int     textColor         = 0;
        private int     textDisabledColor = 0;
        private boolean bold              = false;
        private boolean allCaps           = false;
        private float   textSizeDp        = 0f;
        private int     gradientDirection = DIRECTION_HORIZONTAL;

        /* ---- 渐变动画 ---- */
        private boolean animate      = false;
        private int     animDuration = DEFAULT_ANIM_DURATION;

        /* ---- 水波纹阴影 ---- */
        private boolean ripple      = false;
        private boolean elevation   = false;
        private float   elevationDp = 4f;

        /* ---- 内边距 ---- */
        private Integer paddingHDp = null;
        private Integer paddingVDp = null;

        /* ---- 动画状态 ---- */
        private ValueAnimator gradientAnimator = null;
        private View.OnAttachStateChangeListener animAttachListener = null;

        private int gradientRetry = 0;

        private Builder(TextView v) {
            this.view = v;
        }

        /* ---- 颜色 ---- */
        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder colors(int... colors) {
            this.gradientColors = colors;
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

        /* ---- 描边渐变 ---- */
        public Builder stroke(int widthDp, int color) {
            this.strokeWidthDp = widthDp;
            this.strokeColor = color;
            return this;
        }

        public Builder gradient(int startColor, int endColor) {
            this.gradientColors = new int[]{ startColor, endColor };
            return this;
        }

        public Builder gradient(int[] colors) {
            this.gradientColors = colors;
            return this;
        }

        public Builder direction(int direction) {
            this.gradientDirection = direction;
            return this;
        }

        /* ---- 文字 ---- */
        public Builder textColor(int color) {
            this.textColor = color;
            return this;
        }

        public Builder textColors(int normalColor, int disabledColor) {
            this.textColor = normalColor;
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

        /* ---- 渐变动画 ---- */
        public Builder animate(boolean animate) {
            this.animate = animate;
            return this;
        }

        public Builder animate(int durationMs) {
            this.animate = true;
            this.animDuration = durationMs;
            return this;
        }

        public Builder animDuration(int ms) {
            this.animDuration = ms;
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

            /* ---- 文字颜色三态 ---- */
            int baseText = textColor != 0
                    ? textColor
                    : (gradientColors != null && gradientColors.length > 0
                    ? gradientColors[0] : color);
            int disText = textDisabledColor != 0
                    ? textDisabledColor
                    : withAlpha(baseText, 0.45f);

            ColorStateList textCsl = new ColorStateList(
                    new int[][]{
                            new int[]{ -android.R.attr.state_enabled },
                            new int[]{}
                    },
                    new int[]{ disText, baseText });
            view.setTextColor(textCsl);

            /* ---- 渐变文字 ---- */
            if (gradientColors != null && gradientColors.length >= 2 && view.isEnabled()) {
                if (animate) {
                    startGradientAnimation();
                } else {
                    stopGradientAnimation();
                    applyStaticGradient();
                }
            } else {
                stopGradientAnimation();
                view.getPaint().setShader(null);
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

        /* ---- 静态渐变 Shader ---- */
        private void applyStaticGradient() {
            if (gradientColors == null || gradientColors.length < 2) return;

            final int w = view.getWidth();
            final int h = view.getHeight();
            if (w <= 0 || h <= 0) {
                if (gradientRetry++ < 3) {
                    view.post(new Runnable() {
                        @Override
                        public void run() {
                            applyStaticGradient();
                        }
                    });
                }
                return;
            }

            LinearGradient lg;
            switch (gradientDirection) {
                case DIRECTION_VERTICAL:
                    lg = new LinearGradient(0, 0, 0, h, gradientColors, null, Shader.TileMode.CLAMP);
                    break;
                case DIRECTION_DIAGONAL:
                    lg = new LinearGradient(0, 0, w, h, gradientColors, null, Shader.TileMode.CLAMP);
                    break;
                case DIRECTION_HORIZONTAL:
                default:
                    lg = new LinearGradient(0, 0, w, 0, gradientColors, null, Shader.TileMode.CLAMP);
                    break;
            }

            Paint paint = view.getPaint();
            paint.setShader(lg);
            view.invalidate();
        }

        /* ---- 无限流动渐变动画 ---- */
        private void startGradientAnimation() {
            if (gradientColors == null || gradientColors.length < 2) return;
            stopGradientAnimation();

            final int w = view.getWidth();
            final int h = view.getHeight();
            if (w <= 0 || h <= 0) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        startGradientAnimation();
                    }
                });
                return;
            }

            /* 首尾闭合，保证循环时颜色衔接无缝 */
            final int[] loop = loopColors(gradientColors);
            final LinearGradient lg;
            switch (gradientDirection) {
                case DIRECTION_VERTICAL:
                    lg = new LinearGradient(0, -h, 0, h, loop, null, Shader.TileMode.CLAMP);
                    break;
                case DIRECTION_DIAGONAL:
                    lg = new LinearGradient(-w, -h, w, h, loop, null, Shader.TileMode.CLAMP);
                    break;
                case DIRECTION_HORIZONTAL:
                default:
                    lg = new LinearGradient(-w, 0, w, 0, loop, null, Shader.TileMode.CLAMP);
                    break;
            }

            final Matrix matrix = new Matrix();
            view.getPaint().setShader(lg);
            view.invalidate();

            final ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
            va.setDuration(animDuration);
            va.setRepeatCount(ValueAnimator.INFINITE);
            va.setInterpolator(new LinearInterpolator());
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator a) {
                    float f = (float) a.getAnimatedValue();
                    matrix.reset();
                    switch (gradientDirection) {
                        case DIRECTION_VERTICAL:
                            matrix.setTranslate(0, f * h * 2f);
                            break;
                        case DIRECTION_DIAGONAL:
                            matrix.setTranslate(f * w * 2f, f * h * 2f);
                            break;
                        case DIRECTION_HORIZONTAL:
                        default:
                            matrix.setTranslate(f * w * 2f, 0);
                            break;
                    }
                    lg.setLocalMatrix(matrix);
                    view.invalidate();
                }
            });

            animAttachListener = new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) { }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    va.cancel();
                }
            };
            view.addOnAttachStateChangeListener(animAttachListener);
            va.start();
            gradientAnimator = va;
        }

        /* ---- 停止渐变动画 ---- */
        private void stopGradientAnimation() {
            if (gradientAnimator != null) {
                gradientAnimator.cancel();
                gradientAnimator = null;
            }
            if (animAttachListener != null) {
                view.removeOnAttachStateChangeListener(animAttachListener);
                animAttachListener = null;
            }
        }

        /* ---- 首尾闭合色环，实现无缝循环 ---- */
        private static int[] loopColors(int[] colors) {
            if (colors == null || colors.length < 2) return colors;
            int[] result = new int[colors.length + 1];
            System.arraycopy(colors, 0, result, 0, colors.length);
            result[colors.length] = colors[0];
            return result;
        }

        /* ---- 内部工具 ---- */
        private GradientDrawable buildShape(int fillColor) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(circle ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
            d.setColor(fillColor);
            if (!circle) {
                d.setCornerRadius(dp(radiusDp));
            }
            if (strokeWidthDp > 0 && strokeColor != 0) {
                d.setStroke(dp(strokeWidthDp), strokeColor);
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