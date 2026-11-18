package mr.x.zjx;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Snackbar 底部提示工具：简约现代风卡片 + 操作按钮回调 + 按钮三态美化。
 * 仅使用 Android 原生 API，兼容 API 25+（不使用 Material 库）。
 */
public final class SnackbarUtil {

    public static final int   DEFAULT_COLOR          = 0xFF3F7EF7;
    public static final int   DEFAULT_DISABLED_COLOR = 0xFFBFC6D1;
    public static final float DEFAULT_RADIUS_DP      = 12f;
    public static final int   DEFAULT_DURATION_MS    = 3000;

    public static final int   STYLE_NORMAL  = 0;
    public static final int   STYLE_SUCCESS = 1;
    public static final int   STYLE_ERROR   = 2;
    public static final int   STYLE_WARNING = 3;

    /* 全局调度：同一时间只显示一个 Snackbar */
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static View        currentView  = null;
    private static Runnable    currentHide  = null;

    private SnackbarUtil() { }

    /* ========================= 回调接口 ========================= */
    public interface OnActionListener {
        void onAction();
    }

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

    public static Builder show(View anchor, String text) {
        return new Builder(anchor, text, STYLE_NORMAL);
    }

    public static Builder success(View anchor, String text) {
        return new Builder(anchor, text, STYLE_SUCCESS);
    }

    public static Builder error(View anchor, String text) {
        return new Builder(anchor, text, STYLE_ERROR);
    }

    public static Builder warning(View anchor, String text) {
        return new Builder(anchor, text, STYLE_WARNING);
    }

    /* ========================= Builder ========================= */
    public static final class Builder {

        /* ---- 按钮美化模式 ---- */
        private final Button btnView;

        /* ---- Snackbar 模式 ---- */
        private final View   anchor;
        private final String text;
        private final int    style;

        /* ---- 通用颜色 ---- */
        private int color         = DEFAULT_COLOR;
        private int disabledColor = DEFAULT_DISABLED_COLOR;
        private int pressedColor  = 0;

        /* ---- 按钮文字 ---- */
        private int     btnTextColor = 0;
        private boolean bold         = false;
        private float   textSizeDp   = 0f;
        private float   btnRadiusDp  = 10f;

        /* ---- Snackbar 外观 ---- */
        private int     backgroundColor = 0;
        private int     textColor       = 0xFFFFFFFF;
        private int     actionColor     = 0;
        private float   radiusDp        = DEFAULT_RADIUS_DP;
        private boolean elevation       = false;
        private float   elevationDp     = 6f;
        private int     durationMs      = DEFAULT_DURATION_MS;
        private int     marginSideDp    = 16;
        private int     marginBottomDp  = 24;
        private int     paddingHDp      = 18;
        private int     paddingVDp      = 14;

        /* ---- 操作按钮 ---- */
        private String             actionText = null;
        private OnActionListener   actionListener = null;

        /* ---- 按钮内边距 ---- */
        private Integer btnPadHDp = null;
        private Integer btnPadVDp = null;

        /* ---- 按钮模式构造 ---- */
        private Builder(Button v) {
            this.btnView = v;
            this.anchor = null;
            this.text = null;
            this.style = STYLE_NORMAL;
        }

        /* ---- Snackbar 模式构造 ---- */
        private Builder(View anchor, String text, int style) {
            this.btnView = null;
            this.anchor = anchor;
            this.text = text == null ? "" : text;
            this.style = style;
        }

        /* ---- 按钮美化通用 ---- */
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

        public Builder btnTextColor(int color) {
            this.btnTextColor = color;
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

        public Builder btnRadius(float radiusDp) {
            this.btnRadiusDp = radiusDp;
            return this;
        }

        public Builder btnPadding(Integer hDp, Integer vDp) {
            this.btnPadHDp = hDp;
            this.btnPadVDp = vDp;
            return this;
        }

        /* ---- Snackbar 外观 ---- */
        public Builder backgroundColor(int color) {
            this.backgroundColor = color;
            return this;
        }

        public Builder textColor(int color) {
            this.textColor = color;
            return this;
        }

        public Builder actionColor(int color) {
            this.actionColor = color;
            return this;
        }

        public Builder radius(float radiusDp) {
            this.radiusDp = radiusDp;
            return this;
        }

        public Builder elevation(boolean elevation) {
            this.elevation = elevation;
            return this;
        }

        public Builder duration(int ms) {
            this.durationMs = ms > 0 ? ms : DEFAULT_DURATION_MS;
            return this;
        }

        public Builder margin(int sideDp, int bottomDp) {
            this.marginSideDp = sideDp;
            this.marginBottomDp = bottomDp;
            return this;
        }

        public Builder padding(int hDp, int vDp) {
            this.paddingHDp = hDp;
            this.paddingVDp = vDp;
            return this;
        }

        /* ---- 操作按钮 ---- */
        public Builder action(String text, OnActionListener listener) {
            this.actionText = text;
            this.actionListener = listener;
            return this;
        }

        /* ---- 核心应用 ---- */
        public void apply() {
            if (btnView != null) {
                applyButton();
            } else if (anchor != null) {
                showSnackbar();
            }
        }

        /* ========================= 按钮三态美化 ========================= */
        private void applyButton() {
            int pressed = pressedColor != 0 ? pressedColor : darken(color, 0.82f);

            StateListDrawable sld = new StateListDrawable();
            sld.addState(new int[]{ -android.R.attr.state_enabled },
                    buildShape(disabledColor, btnRadiusDp, btnView.getContext()));
            sld.addState(new int[]{ android.R.attr.state_pressed, android.R.attr.state_enabled },
                    buildShape(pressed, btnRadiusDp, btnView.getContext()));
            sld.addState(new int[]{}, buildShape(color, btnRadiusDp, btnView.getContext()));

            int pl = btnView.getPaddingLeft();
            int pt = btnView.getPaddingTop();
            int pr = btnView.getPaddingRight();
            int pb = btnView.getPaddingBottom();

            btnView.setBackground(sld);

            int newPl = btnPadHDp != null ? dp(btnPadHDp, btnView.getContext()) : pl;
            int newPt = btnPadVDp != null ? dp(btnPadVDp, btnView.getContext()) : pt;
            int newPr = btnPadHDp != null ? dp(btnPadHDp, btnView.getContext()) : pr;
            int newPb = btnPadVDp != null ? dp(btnPadVDp, btnView.getContext()) : pb;
            btnView.setPadding(newPl, newPt, newPr, newPb);

            int base = btnTextColor != 0 ? btnTextColor : Color.WHITE;
            int dis = withAlpha(base, 0.45f);
            btnView.setTextColor(new ColorStateList(
                    new int[][]{
                            new int[]{ -android.R.attr.state_enabled },
                            new int[]{}
                    },
                    new int[]{ dis, base }));

            if (textSizeDp > 0f) {
                btnView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                        dp(textSizeDp, btnView.getContext()));
            }
            btnView.setTypeface(btnView.getTypeface(),
                    bold ? Typeface.BOLD : Typeface.NORMAL);
        }

        /* ========================= Snackbar 显示 ========================= */
        private void showSnackbar() {
            final Context ctx = anchor.getContext();

            /* 根视图：用于承载 Snackbar 卡片 */
            ViewGroup root = findRoot(anchor);
            if (root == null) return;

            /* 先移除上一个 */
            hideCurrent();

            /* 卡片容器 */
            FrameLayout wrapper = new FrameLayout(ctx);
            FrameLayout.LayoutParams wlp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            wlp.gravity = Gravity.BOTTOM;
            wlp.leftMargin = dp(marginSideDp, ctx);
            wlp.rightMargin = dp(marginSideDp, ctx);
            wlp.bottomMargin = dp(marginBottomDp, ctx);
            wrapper.setLayoutParams(wlp);

            /* 卡片内容 */
            LinearLayout card = new LinearLayout(ctx);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(paddingHDp, ctx), dp(paddingVDp, ctx),
                    dp(paddingHDp, ctx), dp(paddingVDp, ctx));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(resolveBackground());
            bg.setCornerRadius(dp(radiusDp, ctx));
            card.setBackground(bg);

            /* 文字 */
            TextView tv = new TextView(ctx);
            tv.setText(text);
            tv.setTextColor(textColor);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(15, ctx));
            tv.setIncludeFontPadding(false);
            tv.setMaxLines(2);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            card.addView(tv, tlp);

            /* 操作按钮 */
            if (actionText != null) {
                TextView action = new TextView(ctx);
                action.setText(actionText);
                action.setTextColor(resolveActionColor());
                action.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(15, ctx));
                action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                action.setIncludeFontPadding(false);
                action.setPadding(dp(12, ctx), dp(6, ctx), 0, dp(6, ctx));
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (actionListener != null) actionListener.onAction();
                        hideCurrent();
                    }
                });
                card.addView(action, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            wrapper.addView(card, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT));

            /* 阴影 */
            if (elevation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                card.setElevation(dp(elevationDp, ctx));
                card.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View v, Outline outline) {
                        outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(),
                                dp(radiusDp, ctx));
                    }
                });
            }

            /* 入场动画：淡入 + 上移 */
            wrapper.setAlpha(0f);
            wrapper.setTranslationY(dp(24, ctx));
            root.addView(wrapper);
            wrapper.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(220)
                    .start();

            currentView = wrapper;

            /* 自动隐藏 */
            currentHide = new Runnable() {
                @Override
                public void run() {
                    hideCurrent();
                }
            };
            MAIN.postDelayed(currentHide, durationMs);
        }

        /* ---- 隐藏当前 Snackbar ---- */
        private static void hideCurrent() {
            if (currentHide != null) {
                MAIN.removeCallbacks(currentHide);
                currentHide = null;
            }
            if (currentView != null) {
                final View v = currentView;
                currentView = null;
                v.animate()
                        .alpha(0f)
                        .translationY(dpStatic(24, v.getContext()))
                        .setDuration(180)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                ViewGroup parent = (ViewGroup) v.getParent();
                                if (parent != null) parent.removeView(v);
                            }
                        })
                        .start();
            }
        }

        /* ---- 查找根视图 ---- */
        private static ViewGroup findRoot(View anchor) {
            View v = anchor;
            while (v != null) {
                if (v.getId() == android.R.id.content && v instanceof ViewGroup) {
                    return (ViewGroup) v;
                }
                if (v.getParent() instanceof View) {
                    v = (View) v.getParent();
                } else {
                    break;
                }
            }
            /* 兜底：使用 decorView */
            if (anchor.getRootView() instanceof ViewGroup) {
                ViewGroup root = (ViewGroup) anchor.getRootView();
                View content = root.findViewById(android.R.id.content);
                if (content instanceof ViewGroup) return (ViewGroup) content;
                return root;
            }
            return null;
        }

        /* ---- 主题色解析 ---- */
        private int resolveBackground() {
            if (backgroundColor != 0) return backgroundColor;
            switch (style) {
                case STYLE_SUCCESS: return 0xFF2E7D32;
                case STYLE_ERROR:   return 0xFFC62828;
                case STYLE_WARNING: return 0xFFEF6C00;
                case STYLE_NORMAL:
                default:            return 0xFF262A33;
            }
        }

        private int resolveActionColor() {
            if (actionColor != 0) return actionColor;
            switch (style) {
                case STYLE_SUCCESS: return 0xFFA5D6A7;
                case STYLE_ERROR:   return 0xFFFFCDD2;
                case STYLE_WARNING: return 0xFFFFE0B2;
                case STYLE_NORMAL:
                default:            return 0xFF7CB8FF;
            }
        }

        /* ---- 内部工具 ---- */
        private GradientDrawable buildShape(int fillColor, float radius, Context ctx) {
            GradientDrawable d = new GradientDrawable();
            d.setColor(fillColor);
            d.setCornerRadius(dp(radius, ctx));
            return d;
        }

        private int dp(float value, Context c) {
            return Math.round(value * c.getResources().getDisplayMetrics().density);
        }

        private static int dpStatic(float value, Context c) {
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