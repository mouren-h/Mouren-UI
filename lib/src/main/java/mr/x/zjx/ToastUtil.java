package mr.x.zjx;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.*;

/**
 * Toast 提示工具：自定义提示气泡 + 按钮三态美化。
 * 仅使用 Android 原生 API，兼容 API 25+。
 */
public final class ToastUtil {

    public static final int   DEFAULT_COLOR          = 0xFF3F7EF7;
    public static final int   DEFAULT_DISABLED_COLOR = 0xFFBFC6D1;
    public static final float DEFAULT_RADIUS_DP      = 10f;
    public static final int   DEFAULT_DURATION_MS    = 2000;

    public static final int   STYLE_NORMAL  = 0;
    public static final int   STYLE_SUCCESS = 1;
    public static final int   STYLE_ERROR   = 2;
    public static final int   STYLE_WARNING = 3;

    private ToastUtil() { }

    /* ========================= 对外快捷入口 ========================= */
    public static Builder beautify(Button v) {
        return beautify(v, DEFAULT_COLOR);
    }

    public static Builder beautify(Button v, int color) {
        return with(v).color(color);
    }

    public static Builder with(Button v) {
        return new Builder(v);
    }

    public static Builder show(Context ctx, String text) {
        return new Builder(ctx, text, STYLE_NORMAL);
    }

    public static Builder success(Context ctx, String text) {
        return new Builder(ctx, text, STYLE_SUCCESS);
    }

    public static Builder error(Context ctx, String text) {
        return new Builder(ctx, text, STYLE_ERROR);
    }

    public static Builder warning(Context ctx, String text) {
        return new Builder(ctx, text, STYLE_WARNING);
    }

    /* ========================= Builder ========================= */
    public static final class Builder {

        /* ---- 按钮美化模式 ---- */
        private final Button btnView;

        /* ---- Toast 模式 ---- */
        private final Context ctx;
        private final String  text;
        private final int     style;

        /* ---- 通用颜色 ---- */
        private int color         = DEFAULT_COLOR;
        private int disabledColor = DEFAULT_DISABLED_COLOR;
        private int pressedColor  = 0;

        /* ---- 文字 ---- */
        private int     textColor  = 0;
        private boolean bold       = false;
        private float   textSizeDp = 0f;

        /* ---- 形状 ---- */
        private float   radiusDp = DEFAULT_RADIUS_DP;
        private boolean circle   = false;

        /* ---- 描边 ---- */
        private int strokeWidthDp = 0;
        private int strokeColor   = 0;

        /* ---- 阴影 ---- */
        private boolean elevation   = false;
        private float   elevationDp = 6f;

        /* ---- Toast 专属 ---- */
        private int     backgroundColor = 0;
        private int     durationMs      = DEFAULT_DURATION_MS;
        private int     gravity         = Gravity.BOTTOM;
        private int     iconRes         = 0;
        private int     paddingHDp      = 22;
        private int     paddingVDp      = 12;

        /* ---- 按钮内边距 ---- */
        private Integer btnPadHDp = null;
        private Integer btnPadVDp = null;

        /* ---- 按钮模式构造 ---- */
        private Builder(Button v) {
            this.btnView = v;
            this.ctx = null;
            this.text = null;
            this.style = STYLE_NORMAL;
        }

        /* ---- Toast 模式构造 ---- */
        private Builder(Context ctx, String text, int style) {
            this.btnView = null;
            this.ctx = ctx;
            this.text = text == null ? "" : text;
            this.style = style;
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

        /* ---- 文字 ---- */
        public Builder textColor(int color) {
            this.textColor = color;
            return this;
        }

        public Builder bold(boolean bold) {
            this.bold = bold;
            return this;
        }

        public Builder textSize(float sizeDp) {
            this.textSizeDp = sizeDp;
            return this;
        }

        /* ---- 形状 ---- */
        public Builder radius(float radiusDp) {
            this.radiusDp = radiusDp;
            this.circle = false;
            return this;
        }

        public Builder circle() {
            this.circle = true;
            return this;
        }

        /* ---- 描边 ---- */
        public Builder stroke(int widthDp, int color) {
            this.strokeWidthDp = widthDp;
            this.strokeColor = color;
            return this;
        }

        /* ---- 阴影 ---- */
        public Builder elevation(boolean elevation) {
            this.elevation = elevation;
            return this;
        }

        public Builder elevation(float elevationDp) {
            this.elevation = true;
            this.elevationDp = elevationDp;
            return this;
        }

        /* ---- Toast 专属 ---- */
        public Builder backgroundColor(int color) {
            this.backgroundColor = color;
            return this;
        }

        public Builder duration(int ms) {
            this.durationMs = ms > 0 ? ms : DEFAULT_DURATION_MS;
            return this;
        }

        public Builder gravity(int gravity) {
            this.gravity = gravity;
            return this;
        }

        public Builder icon(int drawableRes) {
            this.iconRes = drawableRes;
            return this;
        }

        public Builder padding(int hDp, int vDp) {
            this.paddingHDp = hDp;
            this.paddingVDp = vDp;
            return this;
        }

        /* ---- 按钮内边距 ---- */
        public Builder btnPadding(Integer hDp, Integer vDp) {
            this.btnPadHDp = hDp;
            this.btnPadVDp = vDp;
            return this;
        }

        /* ---- 核心应用 ---- */
        public void apply() {
            if (btnView != null) {
                applyButton();
            } else if (ctx != null) {
                applyToast();
            }
        }

        /* ========================= 按钮三态美化 ========================= */
        private void applyButton() {
            int pressed = pressedColor != 0 ? pressedColor : darken(color, 0.82f);

            StateListDrawable sld = new StateListDrawable();
            sld.addState(new int[]{ -android.R.attr.state_enabled }, buildShape(disabledColor));
            sld.addState(new int[]{ android.R.attr.state_pressed, android.R.attr.state_enabled },
                    buildShape(pressed));
            sld.addState(new int[]{}, buildShape(color));

            int pl = btnView.getPaddingLeft();
            int pt = btnView.getPaddingTop();
            int pr = btnView.getPaddingRight();
            int pb = btnView.getPaddingBottom();

            btnView.setBackground(sld);

            int newPl = btnPadHDp != null ? dp(btnView, btnPadHDp) : pl;
            int newPt = btnPadVDp != null ? dp(btnView, btnPadVDp) : pt;
            int newPr = btnPadHDp != null ? dp(btnView, btnPadHDp) : pr;
            int newPb = btnPadVDp != null ? dp(btnView, btnPadVDp) : pb;
            btnView.setPadding(newPl, newPt, newPr, newPb);

            int base = textColor != 0 ? textColor : Color.WHITE;
            int dis = withAlpha(base, 0.45f);
            btnView.setTextColor(new ColorStateList(
                    new int[][]{
                            new int[]{ -android.R.attr.state_enabled },
                            new int[]{}
                    },
                    new int[]{ dis, base }));

            if (textSizeDp > 0f) {
                btnView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                        dp(btnView, textSizeDp));
            }
            btnView.setTypeface(btnView.getTypeface(),
                    bold ? Typeface.BOLD : Typeface.NORMAL);

            if (elevation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                btnView.setElevation(dp(btnView, elevationDp));
                btnView.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View v, Outline outline) {
                        if (circle) {
                            outline.setOval(0, 0, v.getWidth(), v.getHeight());
                        } else {
                            outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(),
                                    dp(v, radiusDp));
                        }
                    }
                });
                btnView.setClipToOutline(true);
            }
        }

        /* ========================= Toast 自定义气泡 ========================= */
        private void applyToast() {
            int bgColor = resolveBackground();
            int fgColor = textColor != 0 ? textColor : Color.WHITE;
            if (style != STYLE_NORMAL && textColor == 0) {
                /* 成功/错误/警告默认浅底深字 */
                fgColor = Color.WHITE;
            }

            /* 容器 */
            LinearLayout root = new LinearLayout(ctx);
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setPadding(dp(ctx, paddingHDp), dp(ctx, paddingVDp),
                    dp(ctx, paddingHDp), dp(ctx, paddingVDp));

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(circle ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
            bg.setColor(bgColor);
            if (!circle) {
                bg.setCornerRadius(dp(ctx, radiusDp));
            }
            if (strokeWidthDp > 0 && strokeColor != 0) {
                bg.setStroke(dp(ctx, strokeWidthDp), strokeColor);
            }
            root.setBackground(bg);

            /* 图标 */
            int icon = resolveIcon();
            if (icon != 0) {
                ImageView iv = new ImageView(ctx);
                iv.setImageResource(icon);
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                        dp(ctx, 18), dp(ctx, 18));
                ilp.rightMargin = dp(ctx, 8);
                iv.setLayoutParams(ilp);
                root.addView(iv);
            }

            /* 文字 */
            TextView tv = new TextView(ctx);
            tv.setText(text);
            tv.setTextColor(fgColor);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                    textSizeDp > 0f ? dp(ctx, textSizeDp) : dp(ctx, 15f));
            tv.setIncludeFontPadding(false);
            tv.setTypeface(tv.getTypeface(), bold ? Typeface.BOLD : Typeface.NORMAL);
            root.addView(tv);

            /* 显示 */
            Toast toast = new Toast(ctx);
            toast.setDuration(durationMs > 2500 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
            toast.setGravity(gravity, 0, dp(ctx, 80));
            toast.setView(root);

            if (elevation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                root.setElevation(dp(ctx, elevationDp));
            }
            toast.show();
        }

        private int resolveBackground() {
            if (backgroundColor != 0) return backgroundColor;
            switch (style) {
                case STYLE_SUCCESS: return 0xFF2E7D32;
                case STYLE_ERROR:   return 0xFFC62828;
                case STYLE_WARNING: return 0xFFEF6C00;
                case STYLE_NORMAL:
                default:            return 0xEE323232;
            }
        }

        private int resolveIcon() {
            if (iconRes != 0) return iconRes;
            switch (style) {
                case STYLE_SUCCESS: return android.R.drawable.checkbox_on_background;
                case STYLE_ERROR:   return android.R.drawable.ic_dialog_alert;
                case STYLE_WARNING: return android.R.drawable.stat_sys_warning;
                case STYLE_NORMAL:
                default:            return 0;
            }
        }

        /* ---- 内部工具 ---- */
        private GradientDrawable buildShape(int fillColor) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(circle ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
            d.setColor(fillColor);
            if (!circle) {
                d.setCornerRadius(dp(btnView, radiusDp));
            }
            if (strokeWidthDp > 0 && strokeColor != 0) {
                d.setStroke(dp(btnView, strokeWidthDp), strokeColor);
            }
            return d;
        }

        private int dp(View v, float value) {
            return Math.round(value * v.getResources().getDisplayMetrics().density);
        }

        private int dp(Context c, float value) {
            return Math.round(value * c.getResources().getDisplayMetrics().density);
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