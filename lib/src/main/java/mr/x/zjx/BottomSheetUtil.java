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
import android.view.*;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * 底部弹窗工具：底部滑入选项列表 + 顶部指示条 + 取消按钮 + 按钮三态美化。
 * 仅使用 Android 原生 API，兼容 API 25+。
 */
public final class BottomSheetUtil {

    public static final int   DEFAULT_COLOR          = 0xFF3F7EF7;
    public static final int   DEFAULT_DISABLED_COLOR = 0xFFBFC6D1;
    public static final float DEFAULT_RADIUS_DP      = 20f;

    public static final int   STYLE_NORMAL = 0;
    public static final int   STYLE_DANGER = 1;

    private BottomSheetUtil() { }

    /* ========================= 回调接口 ========================= */
    public interface OnItemClickListener {
        void onClick(int position, String text);
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

    public static Builder sheet(Activity activity) {
        return new Builder(activity, STYLE_NORMAL);
    }

    public static Builder danger(Activity activity) {
        return new Builder(activity, STYLE_DANGER);
    }

    /* ========================= Builder ========================= */
    public static final class Builder {

        /* ---- 按钮美化模式 ---- */
        private final Button btnView;

        /* ---- 底部弹窗模式 ---- */
        private final Activity activity;
        private final int      style;
        private Dialog         dialog;

        /* ---- 弹窗内容 ---- */
        private String title    = null;
        private String cancelText = null;
        private final List<String> itemTexts = new ArrayList<>();
        private final List<OnItemClickListener> itemListeners = new ArrayList<>();

        /* ---- 按钮美化通用 ---- */
        private int     color         = DEFAULT_COLOR;
        private int     disabledColor = DEFAULT_DISABLED_COLOR;
        private int     pressedColor  = 0;
        private int     btnTextColor  = 0;
        private boolean bold          = false;
        private float   textSizeDp    = 0f;
        private float   btnRadiusDp   = 10f;

        /* ---- 弹窗外观 ---- */
        private int     cardColor    = 0xFFFFFFFF;
        private int     titleColor   = 0xFF111827;
        private int     itemColor    = 0xFF374151;
        private int     dangerColor  = 0xFFEF4444;
        private float   radiusDp     = DEFAULT_RADIUS_DP;
        private boolean handle       = true;
        private float   dimAmount    = 0.35f;
        private boolean canceledOnTouchOutside = true;
        private int     itemHeightDp = 54;

        /* ---- 按钮内边距 ---- */
        private Integer btnPadHDp = null;
        private Integer btnPadVDp = null;

        /* ---- 按钮模式构造 ---- */
        private Builder(Button v) {
            this.btnView = v;
            this.activity = null;
            this.style = STYLE_NORMAL;
        }

        /* ---- 底部弹窗模式构造 ---- */
        private Builder(Activity activity, int style) {
            this.btnView = null;
            this.activity = activity;
            this.style = style;
        }

        /* ---- 弹窗内容 ---- */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder item(String text, OnItemClickListener listener) {
            itemTexts.add(text == null ? "" : text);
            itemListeners.add(listener);
            return this;
        }

        public Builder cancelText(String text) {
            this.cancelText = text;
            return this;
        }

        /* ---- 弹窗外观 ---- */
        public Builder cardColor(int color) {
            this.cardColor = color;
            return this;
        }

        public Builder titleColor(int color) {
            this.titleColor = color;
            return this;
        }

        public Builder itemColor(int color) {
            this.itemColor = color;
            return this;
        }

        public Builder radius(float radiusDp) {
            this.radiusDp = radiusDp;
            return this;
        }

        public Builder handle(boolean handle) {
            this.handle = handle;
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

        public Builder itemHeight(int heightDp) {
            this.itemHeightDp = heightDp;
            return this;
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

        /* ---- 核心应用（按钮模式） ---- */
        public void apply() {
            if (btnView != null) {
                applyButton();
            }
        }

        /* ========================= 显示底部弹窗 ========================= */
        public Dialog show() {
            if (activity == null) return null;
            dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            /* ---- 外层：整体垂直布局 ---- */
            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);

            /* ---- 顶部留白，点击空白关闭 ---- */
            View space = new View(activity);
            space.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            space.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (canceledOnTouchOutside && dialog != null) dialog.dismiss();
                }
            });
            root.addView(space);

            /* ---- 内容卡片 ---- */
            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(cardColor);
            bg.setCornerRadii(new float[]{
                    dp(radiusDp), dp(radiusDp),
                    dp(radiusDp), dp(radiusDp),
                    0, 0, 0, 0});
            card.setBackground(bg);

            card.setPadding(0, dp(handle ? 8 : 16), 0, dp(8));

            /* ---- 顶部指示条 ---- */
            if (handle) {
                View bar = new View(activity);
                GradientDrawable barBg = new GradientDrawable();
                barBg.setColor(0xFFD1D5DB);
                barBg.setCornerRadius(dp(2));
                bar.setBackground(barBg);
                LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                        dp(36), dp(4));
                blp.gravity = Gravity.CENTER_HORIZONTAL;
                blp.bottomMargin = dp(8);
                card.addView(bar, blp);
            }

            /* ---- 标题 ---- */
            if (title != null) {
                TextView tvTitle = new TextView(activity);
                tvTitle.setText(title);
                tvTitle.setTextColor(titleColor);
                tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(16));
                tvTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                tvTitle.setIncludeFontPadding(false);
                tvTitle.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                tlp.topMargin = dp(6);
                tlp.bottomMargin = dp(10);
                card.addView(tvTitle, tlp);
            }

            /* ---- 选项列表 ---- */
            for (int i = 0; i < itemTexts.size(); i++) {
                final int pos = i;
                final String text = itemTexts.get(i);
                final OnItemClickListener listener = itemListeners.get(i);

                TextView item = new TextView(activity);
                item.setText(text);
                item.setTextColor(style == STYLE_DANGER && i == 0 ? dangerColor : itemColor);
                item.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(16));
                item.setGravity(Gravity.CENTER);
                item.setIncludeFontPadding(false);
                item.setBackground(buildItemBg());

                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) listener.onClick(pos, text);
                        if (dialog != null) dialog.dismiss();
                    }
                });

                card.addView(item, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(itemHeightDp)));
            }

            /* ---- 取消按钮（可选） ---- */
            if (cancelText != null) {
                View divider = new View(activity);
                divider.setBackgroundColor(0xFFF3F4F6);
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
                dlp.topMargin = dp(6);
                card.addView(divider, dlp);

                TextView cancel = new TextView(activity);
                cancel.setText(cancelText);
                cancel.setTextColor(0xFF6B7280);
                cancel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(16));
                cancel.setGravity(Gravity.CENTER);
                cancel.setIncludeFontPadding(false);
                cancel.setBackground(buildItemBg());
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (dialog != null) dialog.dismiss();
                    }
                });
                card.addView(cancel, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(itemHeightDp)));
            }

            root.addView(card, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            /* ---- 窗口配置 ---- */
            dialog.setContentView(root);
            dialog.setCanceledOnTouchOutside(canceledOnTouchOutside);

            Window win = dialog.getWindow();
            if (win != null) {
                win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams wp = win.getAttributes();
                wp.width = WindowManager.LayoutParams.MATCH_PARENT;
                wp.height = WindowManager.LayoutParams.MATCH_PARENT;
                wp.gravity = Gravity.BOTTOM;
                wp.dimAmount = dimAmount;
                win.setAttributes(wp);
                win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }

            dialog.show();

            /* ---- 入场动画：从底部滑入 ---- */
            final View cardView = card;
            cardView.post(new Runnable() {
                @Override
                public void run() {
                    cardView.setTranslationY(cardView.getHeight());
                    cardView.animate()
                            .translationY(0f)
                            .setDuration(260)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                }
            });

            return dialog;
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

        /* ---- 选项点击背景：浅灰按压态 ---- */
        private StateListDrawable buildItemBg() {
            StateListDrawable sld = new StateListDrawable();
            GradientDrawable pressed = new GradientDrawable();
            pressed.setColor(0xFFF3F4F6);
            sld.addState(new int[]{ android.R.attr.state_pressed }, pressed);
            GradientDrawable normal = new GradientDrawable();
            normal.setColor(Color.TRANSPARENT);
            sld.addState(new int[]{}, normal);
            return sld;
        }

        /* ---- 内部工具 ---- */
        private GradientDrawable buildShape(int fillColor, float radius, Context ctx) {
            GradientDrawable d = new GradientDrawable();
            d.setColor(fillColor);
            d.setCornerRadius(dp(radius, ctx));
            return d;
        }

        private int dp(float value) {
            Context c = activity != null ? activity
                    : (btnView != null ? btnView.getContext() : null);
            return c == null ? Math.round(value)
                    : Math.round(value * c.getResources().getDisplayMetrics().density);
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