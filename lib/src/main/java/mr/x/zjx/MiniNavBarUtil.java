package mr.x.zjx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 顶部小程序导航栏工具：状态栏适配 + 圆形返回 + 居中标题 + 微信胶囊按钮。
 * 三个按钮（返回 / 更多 / 关闭）各自独立回调。
 * 仅使用 Android 原生 API，兼容 API 25+。
 */
public final class MiniNavBarUtil {

    public static final int DEFAULT_BG             = 0xFFFFFFFF;
    public static final int DEFAULT_TITLE_COLOR    = 0xFF1A1A1A;
    public static final int DEFAULT_ICON_COLOR     = 0xFF1A1A1A;
    public static final int DEFAULT_CAPSULE_BORDER = 0x33000000;
    public static final int DEFAULT_CAPSULE_BG     = 0x0D000000;
    public static final int DEFAULT_DIVIDER_COLOR  = 0xFFEDEDED;

    public static final int DEFAULT_HEIGHT_DP = 44;
    public static final int TOUCH_SIZE_DP     = 40;
    public static final int CAPSULE_WIDTH_DP  = 87;
    public static final int CAPSULE_HEIGHT_DP = 32;

    private MiniNavBarUtil() { }

    /* ========================= 回调接口 ========================= */
    public interface OnNavClickListener {
        void onClick();
    }

    /* ========================= 对外快捷入口 ========================= */
    public static Builder beautify(ViewGroup navBox) {
        return beautify(navBox, DEFAULT_BG);
    }

    public static Builder beautify(ViewGroup navBox, int bgColor) {
        return build(navBox).backgroundColor(bgColor);
    }

    public static Builder build(ViewGroup navBox) {
        return new Builder(navBox);
    }

    /* ========================= 状态栏高度 ========================= */
    public static int getStatusBarHeight(Context ctx) {
        int resId = ctx.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            return ctx.getResources().getDimensionPixelSize(resId);
        }
        return Math.round(24 * ctx.getResources().getDisplayMetrics().density);
    }

    /* ========================= Builder ========================= */
    public static final class Builder {

        private final ViewGroup navBox;

        /* ---- 外观 ---- */
        private int     bgColor       = DEFAULT_BG;
        private int     titleColor    = DEFAULT_TITLE_COLOR;
        private int     iconColor     = DEFAULT_ICON_COLOR;
        private int     capsuleBorder = DEFAULT_CAPSULE_BORDER;
        private int     capsuleBg     = DEFAULT_CAPSULE_BG;
        private int     heightDp      = DEFAULT_HEIGHT_DP;
        private boolean includeStatusBar = true;
        private boolean divider       = true;
        private int     dividerColor  = DEFAULT_DIVIDER_COLOR;

        /* ---- 内容 ---- */
        private String  title       = "";
        private boolean showBack    = true;
        private boolean showCapsule = true;

        /* ---- 三个按钮回调 ---- */
        private OnNavClickListener backListener    = null;  // 返回
        private OnNavClickListener moreListener    = null;  // 胶囊 •••
        private OnNavClickListener closeListener   = null;  // 胶囊 ⊙
        private OnNavClickListener capsuleListener = null;  // 胶囊兜底

        private Builder(ViewGroup navBox) {
            this.navBox = navBox;
        }

        /* ---- 外观配置 ---- */
        public Builder backgroundColor(int color) {
            this.bgColor = color;
            return this;
        }

        public Builder titleColor(int color) {
            this.titleColor = color;
            return this;
        }

        public Builder iconColor(int color) {
            this.iconColor = color;
            return this;
        }

        public Builder capsuleBorderColor(int color) {
            this.capsuleBorder = color;
            return this;
        }

        public Builder capsuleBackgroundColor(int color) {
            this.capsuleBg = color;
            return this;
        }

        public Builder height(int heightDp) {
            this.heightDp = heightDp;
            return this;
        }

        public Builder includeStatusBar(boolean include) {
            this.includeStatusBar = include;
            return this;
        }

        public Builder divider(boolean divider) {
            this.divider = divider;
            return this;
        }

        public Builder dividerColor(int color) {
            this.dividerColor = color;
            return this;
        }

        /* ---- 内容配置 ---- */
        public Builder title(String title) {
            this.title = title == null ? "" : title;
            return this;
        }

        public Builder showBack(boolean show) {
            this.showBack = show;
            return this;
        }

        public Builder showCapsule(boolean show) {
            this.showCapsule = show;
            return this;
        }

        /* ---- 三个按钮回调 ---- */
        /** 返回按钮回调 */
        public Builder onBack(OnNavClickListener listener) {
            this.backListener = listener;
            return this;
        }

        /** 胶囊左侧「•••」回调 */
        public Builder onMore(OnNavClickListener listener) {
            this.moreListener = listener;
            return this;
        }

        /** 胶囊右侧「⊙」回调 */
        public Builder onClose(OnNavClickListener listener) {
            this.closeListener = listener;
            return this;
        }

        /** 胶囊整体兜底回调（onMore / onClose 未设置时触发） */
        public Builder onCapsule(OnNavClickListener listener) {
            this.capsuleListener = listener;
            return this;
        }

        /* ---- 核心应用 ---- */
        public void apply() {
            if (navBox == null) return;
            Context ctx = navBox.getContext();

            navBox.removeAllViews();
            if (navBox instanceof LinearLayout) {
                ((LinearLayout) navBox).setOrientation(LinearLayout.VERTICAL);
            }
            navBox.setBackground(buildSolid(bgColor));

            /* ---- 状态栏占位 ---- */
            if (includeStatusBar) {
                View statusBar = new View(ctx);
                statusBar.setBackground(buildSolid(bgColor));
                LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(ctx));
                navBox.addView(statusBar, slp);
            }

            /* ---- 导航行 ---- */
            FrameLayout row = new FrameLayout(ctx);
            row.setBackground(buildSolid(bgColor));
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp, ctx));
            navBox.addView(row, rlp);

            /* ---- 左：返回按钮（圆形按压） ---- */
            if (showBack) {
                FrameLayout backBox = new FrameLayout(ctx);
                backBox.setBackground(buildCirclePressBg(ctx));

                TextView arrow = new TextView(ctx);
                arrow.setText("‹");
                arrow.setTextColor(iconColor);
                arrow.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 26);
                arrow.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
                arrow.setGravity(Gravity.CENTER);
                arrow.setIncludeFontPadding(false);
                arrow.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                backBox.addView(arrow);

                backBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (backListener != null) backListener.onClick();
                    }
                });

                FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                        dp(TOUCH_SIZE_DP, ctx), dp(TOUCH_SIZE_DP, ctx));
                blp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
                blp.leftMargin = dp(4, ctx);
                row.addView(backBox, blp);
            }

            /* ---- 中：标题 ---- */
            TextView tvTitle = new TextView(ctx);
            tvTitle.setText(title);
            tvTitle.setTextColor(titleColor);
            tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17);
            tvTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            tvTitle.setGravity(Gravity.CENTER);
            tvTitle.setSingleLine(true);
            tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tvTitle.setIncludeFontPadding(false);

            int sidePad = dp(CAPSULE_WIDTH_DP + 24, ctx);
            FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            tlp.leftMargin = sidePad;
            tlp.rightMargin = sidePad;
            row.addView(tvTitle, tlp);

            /* ---- 右：微信胶囊「••• | ⊙」 ---- */
            if (showCapsule) {
                FrameLayout capsule = new FrameLayout(ctx);
                GradientDrawable capBg = new GradientDrawable();
                capBg.setCornerRadius(dp(CAPSULE_HEIGHT_DP / 2f, ctx));
                capBg.setColor(capsuleBg);
                capBg.setStroke(hairline(ctx), capsuleBorder);
                capsule.setBackground(capBg);

                float halfW = CAPSULE_WIDTH_DP / 2f * ctx.getResources().getDisplayMetrics().density;

                /* 左半区：三个点（可点击） */
                DotsView dots = new DotsView(ctx, iconColor);
                FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(
                        Math.round(halfW), FrameLayout.LayoutParams.MATCH_PARENT);
                dlp.gravity = Gravity.START;
                capsule.addView(dots, dlp);

                dots.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (moreListener != null) {
                            moreListener.onClick();
                        } else if (capsuleListener != null) {
                            capsuleListener.onClick();
                        }
                    }
                });

                /* 竖直分隔线 */
                View vLine = new View(ctx);
                vLine.setBackgroundColor(capsuleBorder);
                FrameLayout.LayoutParams vlp = new FrameLayout.LayoutParams(
                        hairline(ctx), dp(16, ctx));
                vlp.gravity = Gravity.CENTER;
                capsule.addView(vLine, vlp);

                /* 右半区：圆圈点（可点击） */
                CircleDotView circleDot = new CircleDotView(ctx, iconColor);
                FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                        Math.round(halfW), FrameLayout.LayoutParams.MATCH_PARENT);
                clp.gravity = Gravity.END;
                capsule.addView(circleDot, clp);

                circleDot.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (closeListener != null) {
                            closeListener.onClick();
                        } else if (capsuleListener != null) {
                            capsuleListener.onClick();
                        }
                    }
                });

                FrameLayout.LayoutParams caplp = new FrameLayout.LayoutParams(
                        dp(CAPSULE_WIDTH_DP, ctx), dp(CAPSULE_HEIGHT_DP, ctx));
                caplp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
                caplp.rightMargin = dp(8, ctx);
                row.addView(capsule, caplp);
            }

            /* ---- 底部分割线 ---- */
            if (divider) {
                View line = new View(ctx);
                line.setBackgroundColor(dividerColor);
                FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, hairline(ctx));
                llp.gravity = Gravity.BOTTOM;
                row.addView(line, llp);
            }
        }

        /* ---- 背景 / 按压 ---- */
        private GradientDrawable buildSolid(int color) {
            GradientDrawable d = new GradientDrawable();
            d.setColor(color);
            return d;
        }

        private StateListDrawable buildCirclePressBg(Context ctx) {
            float r = dp(TOUCH_SIZE_DP / 2f, ctx);

            GradientDrawable pressed = new GradientDrawable();
            pressed.setShape(GradientDrawable.OVAL);
            pressed.setColor(0x14000000);
            pressed.setCornerRadius(r);

            GradientDrawable normal = new GradientDrawable();
            normal.setShape(GradientDrawable.OVAL);
            normal.setColor(Color.TRANSPARENT);
            normal.setCornerRadius(r);

            StateListDrawable sld = new StateListDrawable();
            sld.addState(new int[]{ android.R.attr.state_pressed }, pressed);
            sld.addState(new int[]{}, normal);
            return sld;
        }

        /* ---- dp / hairline ---- */
        private int dp(float value, Context ctx) {
            return Math.round(value * ctx.getResources().getDisplayMetrics().density);
        }

        private int hairline(Context ctx) {
            return Math.max(1, (int) (0.5f * ctx.getResources().getDisplayMetrics().density));
        }
    }

    /* ========================= 三个点（•••） ========================= */
    static final class DotsView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float radius;
        private final float gap;

        DotsView(Context ctx, int color) {
            super(ctx);
            float d = ctx.getResources().getDisplayMetrics().density;
            radius = 1.8f * d;
            gap = 3.2f * d;
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            setClickable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float step = gap + radius * 2;
            canvas.drawCircle(cx - step, cy, radius, paint);
            canvas.drawCircle(cx, cy, radius, paint);
            canvas.drawCircle(cx + step, cy, radius, paint);
        }
    }

    /* ========================= 圆圈点（⊙） ========================= */
    static final class CircleDotView extends View {
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float outerR;
        private final float innerR;

        CircleDotView(Context ctx, int color) {
            super(ctx);
            float d = ctx.getResources().getDisplayMetrics().density;
            outerR = 6.5f * d;
            innerR = 2.6f * d;

            ringPaint.setColor(color);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(1.5f * d);

            dotPaint.setColor(color);
            dotPaint.setStyle(Paint.Style.FILL);
            setClickable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            canvas.drawCircle(cx, cy, outerR, ringPaint);
            canvas.drawCircle(cx, cy, innerR, dotPaint);
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