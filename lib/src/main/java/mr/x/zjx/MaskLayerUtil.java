package mr.x.zjx;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 遮罩浮层工具：全屏半透明遮罩 + 可选居中浮层卡片 + 点击回调 + 按钮三态美化。
 * 仅使用 Android 原生 API，兼容 API 25+。
 */
public final class MaskLayerUtil {

    public static final int   DEFAULT_COLOR          = 0xFF3F7EF7;
    public static final int   DEFAULT_DISABLED_COLOR = 0xFFBFC6D1;
    public static final float DEFAULT_RADIUS_DP      = 16f;

    /* 当前遮罩（全局唯一，便于直接关闭） */
    private static Handle currentHandle = null;

    private MaskLayerUtil() { }

    /* ========================= 回调接口 ========================= */
    public interface OnMaskClickListener {
        void onClick();
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

    public static Builder create(Activity activity) {
        return new Builder(activity);
    }

    /* ========================= 直接关闭 ========================= */
    /** 直接关闭当前遮罩浮层 */
    public static void dismiss() {
        if (currentHandle != null) {
            currentHandle.dismiss();
            currentHandle = null;
        }
    }

    /** 当前是否有遮罩显示 */
    public static boolean isShowing() {
        return currentHandle != null && currentHandle.isShowing();
    }

    /* ========================= 遮罩句柄 ========================= */
    public static final class Handle {
        private final Dialog dialog;
        Handle(Dialog dialog) {
            this.dialog = dialog;
        }
        public void dismiss() {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
        }
        public boolean isShowing() {
            return dialog != null && dialog.isShowing();
        }
    }

    /* ========================= Builder ========================= */
    public static final class Builder {

        /* ---- 按钮美化模式 ---- */
        private final Button btnView;

        /* ---- 遮罩模式 ---- */
        private final Activity activity;

        /* ---- 按钮美化通用 ---- */
        private int     color         = DEFAULT_COLOR;
        private int     disabledColor = DEFAULT_DISABLED_COLOR;
        private int     pressedColor  = 0;
        private int     btnTextColor  = 0;
        private boolean bold          = false;
        private float   textSizeDp    = 0f;
        private float   btnRadiusDp   = 10f;

        /* ---- 遮罩外观 ---- */
        private int     maskColor    = 0xFF000000;
        private float   dimAmount    = 0.5f;
        private boolean canceledOnTouchOutside = true;
        private OnMaskClickListener maskClickListener = null;

        /* ---- 浮层卡片 ---- */
        private boolean contentCard = false;
        private String  title       = null;
        private String  message     = null;
        private int     cardColor   = 0xFFFFFFFF;
        private int     titleColor  = 0xFF111827;
        private int     messageColor = 0xFF4B5563;
        private float   radiusDp    = DEFAULT_RADIUS_DP;
        private int     horizontalMarginDp = 40;

        /* ---- 按钮内边距 ---- */
        private Integer btnPadHDp = null;
        private Integer btnPadVDp = null;

        private Builder(Button v) {
            this.btnView = v;
            this.activity = null;
        }

        private Builder(Activity activity) {
            this.btnView = null;
            this.activity = activity;
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

        /* ---- 遮罩外观 ---- */
        public Builder maskColor(int color) {
            this.maskColor = color;
            return this;
        }

        public Builder dimAmount(float dimAmount) {
            this.dimAmount = dimAmount;
            return this;
        }

        public Builder canceledOnTouchOutside(boolean cancel) {
            this.canceledOnTouchOutside = cancel;
            return this;
        }

        public Builder onMaskClick(OnMaskClickListener listener) {
            this.maskClickListener = listener;
            return this;
        }

        /* ---- 浮层卡片 ---- */
        public Builder contentCard(boolean contentCard) {
            this.contentCard = contentCard;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder cardColor(int color) {
            this.cardColor = color;
            return this;
        }

        public Builder radius(float radiusDp) {
            this.radiusDp = radiusDp;
            return this;
        }

        /* ---- 核心应用 ---- */
        public Handle apply() {
            if (btnView != null) {
                applyButton();
                return null;
            }
            if (activity != null) {
                return showMask();
            }
            return null;
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

        /* ========================= 显示遮罩 ========================= */
        private Handle showMask() {
            dismiss();

            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            /* ---- 根容器 FrameLayout：遮罩 + 可选卡片 ---- */
            FrameLayout root = new FrameLayout(activity);

            /* 遮罩层 */
            View mask = new View(activity);
            mask.setBackgroundColor(withAlpha(maskColor, dimAmount));
            mask.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mask.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (maskClickListener != null) maskClickListener.onClick();
                    if (canceledOnTouchOutside) dialog.dismiss();
                }
            });
            root.addView(mask);

            /* 居中浮层卡片 */
            if (contentCard) {
                LinearLayout card = new LinearLayout(activity);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(24, activity), dp(24, activity),
                        dp(24, activity), dp(20, activity));

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(cardColor);
                bg.setCornerRadius(dp(radiusDp, activity));
                card.setBackground(bg);

                if (title != null) {
                    TextView tvTitle = new TextView(activity);
                    tvTitle.setText(title);
                    tvTitle.setTextColor(titleColor);
                    tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(18, activity));
                    tvTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    tvTitle.setIncludeFontPadding(false);
                    LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    tlp.bottomMargin = dp(message != null ? 10 : 0, activity);
                    card.addView(tvTitle, tlp);
                }

                if (message != null) {
                    TextView tvMsg = new TextView(activity);
                    tvMsg.setText(message);
                    tvMsg.setTextColor(messageColor);
                    tvMsg.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(15, activity));
                    tvMsg.setLineSpacing(dp(4, activity), 1f);
                    card.addView(tvMsg);
                }

                FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                clp.gravity = Gravity.CENTER;
                clp.leftMargin = dp(horizontalMarginDp, activity);
                clp.rightMargin = dp(horizontalMarginDp, activity);
                card.setLayoutParams(clp);
                card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        /* 拦截点击，避免穿透到遮罩关闭 */
                    }
                });
                root.addView(card);

                /* 入场动画：卡片缩放淡入 */
                card.setAlpha(0f);
                card.setScaleX(0.9f);
                card.setScaleY(0.9f);
                card.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(220)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
            }

            dialog.setContentView(root);
            dialog.setCanceledOnTouchOutside(canceledOnTouchOutside);

            Window win = dialog.getWindow();
            if (win != null) {
                win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams wp = win.getAttributes();
                wp.width = WindowManager.LayoutParams.MATCH_PARENT;
                wp.height = WindowManager.LayoutParams.MATCH_PARENT;
                wp.gravity = Gravity.CENTER;
                wp.dimAmount = 0f;   /* 遮罩色由 mask 自绘，不用系统 dim */
                win.setAttributes(wp);
            }

            dialog.show();

            /* 遮罩淡入 */
            mask.setAlpha(0f);
            mask.animate()
                    .alpha(1f)
                    .setDuration(220)
                    .start();

            Handle handle = new Handle(dialog);
            currentHandle = handle;
            return handle;
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