package mr.x.zjx;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 弹窗 Dialog 工具：标题/内容/输入框 + 取消确认按钮 + 按钮三态美化 + 点击回调。
 * 仅使用 Android 原生 API，兼容 API 25+。
 */
public final class DialogUtil {

    public static final int   DEFAULT_COLOR          = 0xFF3F7EF7;
    public static final int   DEFAULT_DISABLED_COLOR = 0xFFBFC6D1;
    public static final float DEFAULT_RADIUS_DP      = 14f;

    public static final int   STYLE_NORMAL  = 0;
    public static final int   STYLE_SUCCESS = 1;
    public static final int   STYLE_ERROR   = 2;
    public static final int   STYLE_WARNING = 3;

    private DialogUtil() { }

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
        return new Builder(activity, STYLE_NORMAL);
    }

    public static Builder success(Activity activity) {
        return new Builder(activity, STYLE_SUCCESS);
    }

    public static Builder error(Activity activity) {
        return new Builder(activity, STYLE_ERROR);
    }

    public static Builder warning(Activity activity) {
        return new Builder(activity, STYLE_WARNING);
    }

    /* ========================= 回调接口 ========================= */
    /** 弹窗按钮点击回调 */
    public interface OnDialogClickListener {
        /**
         * @param dialog  当前弹窗
         * @param input   输入框内容（无输入框时为 null）
         */
        void onClick(Dialog dialog, String input);
    }

    /* ========================= Builder ========================= */
    public static final class Builder {

        /* ---- 按钮美化模式 ---- */
        private final Button btnView;

        /* ---- 弹窗模式 ---- */
        private final Activity activity;
        private final int      style;
        private Dialog         dialog;

        /* ---- 弹窗内容 ---- */
        private String title    = null;
        private String message  = null;
        private String posText  = "确定";
        private String negText  = null;
        private OnDialogClickListener posListener = null;
        private OnDialogClickListener negListener = null;
        private String   inputHint    = null;
        private String   inputDefault = null;
        private EditText inputView    = null;

        /* ---- 按钮美化通用 ---- */
        private int     color         = DEFAULT_COLOR;
        private int     disabledColor = DEFAULT_DISABLED_COLOR;
        private int     pressedColor  = 0;
        private int     textColor     = 0;
        private boolean bold          = false;
        private float   textSizeDp    = 0f;
        private float   btnRadiusDp   = 10f;

        /* ---- 弹窗外观 ---- */
        private int     accentColor     = 0;
        private int     cardColor       = 0xFFFFFFFF;
        private int     titleColor      = 0xFF1F2937;
        private int     messageColor    = 0xFF4B5563;
        private float   radiusDp        = DEFAULT_RADIUS_DP;
        private boolean elevation       = false;
        private float   elevationDp     = 8f;
        private boolean canceledOnTouchOutside = true;
        private boolean canceledOnBack  = true;
        private int     horizontalMarginDp = 28;   // 左右留白，避免贴屏

        /* ---- 按钮内边距 ---- */
        private Integer btnPadHDp = null;
        private Integer btnPadVDp = null;

        /* ---- 按钮模式构造 ---- */
        private Builder(Button v) {
            this.btnView = v;
            this.activity = null;
            this.style = STYLE_NORMAL;
        }

        /* ---- 弹窗模式构造 ---- */
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

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder positive(String text, OnDialogClickListener listener) {
            this.posText = text;
            this.posListener = listener;
            return this;
        }

        public Builder negative(String text, OnDialogClickListener listener) {
            this.negText = text;
            this.negListener = listener;
            return this;
        }

        public Builder input(String hint, String defaultValue) {
            this.inputHint = hint;
            this.inputDefault = defaultValue;
            return this;
        }

        /* ---- 弹窗外观 ---- */
        public Builder accentColor(int color) {
            this.accentColor = color;
            return this;
        }

        public Builder cardColor(int color) {
            this.cardColor = color;
            return this;
        }

        public Builder titleColor(int color) {
            this.titleColor = color;
            return this;
        }

        public Builder messageColor(int color) {
            this.messageColor = color;
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

        public Builder canceledOnTouchOutside(boolean cancel) {
            this.canceledOnTouchOutside = cancel;
            return this;
        }

        public Builder canceledOnBack(boolean cancel) {
            this.canceledOnBack = cancel;
            return this;
        }

        /** 自定义左右留白（dp），默认 28dp */
        public Builder horizontalMargin(int dp) {
            this.horizontalMarginDp = dp;
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

        /* ---- 显示弹窗 ---- */
        public Dialog show() {
            if (activity == null) return null;
            dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            /* ---- 内容容器 ---- */
            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(24), dp(24), dp(24), dp(16));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(cardColor);
            bg.setCornerRadius(dp(radiusDp));
            root.setBackground(bg);

            /* ---- 标题 ---- */
            if (title != null) {
                TextView tvTitle = new TextView(activity);
                tvTitle.setText(title);
                tvTitle.setTextColor(resolveAccent());
                tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(18));
                tvTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                tvTitle.setIncludeFontPadding(false);
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                tlp.bottomMargin = dp(message != null || inputHint != null ? 12 : 20);
                root.addView(tvTitle, tlp);
            }

            /* ---- 内容 ---- */
            if (message != null) {
                TextView tvMsg = new TextView(activity);
                tvMsg.setText(message);
                tvMsg.setTextColor(messageColor);
                tvMsg.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(15));
                tvMsg.setLineSpacing(dp(4), 1f);
                LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                mlp.bottomMargin = dp(20);
                root.addView(tvMsg, mlp);
            }

            /* ---- 输入框 ---- */
            if (inputHint != null) {
                inputView = new EditText(activity);
                inputView.setHint(inputHint);
                inputView.setText(inputDefault == null ? "" : inputDefault);
                inputView.setInputType(InputType.TYPE_CLASS_TEXT);
                inputView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(15));
                inputView.setHintTextColor(0xFF9CA3AF);
                inputView.setTextColor(titleColor);
                inputView.setPadding(dp(12), dp(10), dp(12), dp(10));
                inputView.setBackground(buildInputBg());
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                ilp.bottomMargin = dp(20);
                root.addView(inputView, ilp);
            }

            /* ---- 按钮区 ---- */
            LinearLayout btnRow = new LinearLayout(activity);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setGravity(Gravity.END);

            if (negText != null) {
                Button negBtn = createDialogButton(negText, 0xFF6B7280, 0xFFF3F4F6);
                final OnDialogClickListener negL = negListener;
                negBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String input = inputView != null ? inputView.getText().toString() : null;
                        if (negL != null) negL.onClick(dialog, input);
                        if (dialog != null) dialog.dismiss();
                    }
                });
                LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                        dp(88), dp(42));
                nlp.rightMargin = dp(10);
                btnRow.addView(negBtn, nlp);
            }

            Button posBtn = createDialogButton(posText, 0xFFFFFFFF, resolveAccent());
            final OnDialogClickListener posL = posListener;
            posBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String input = inputView != null ? inputView.getText().toString() : null;
                    if (posL != null) posL.onClick(dialog, input);
                    if (dialog != null) dialog.dismiss();
                }
            });
            btnRow.addView(posBtn, new LinearLayout.LayoutParams(dp(96), dp(42)));

            root.addView(btnRow, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            /* ---- 阴影 ---- */
            if (elevation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                root.setElevation(dp(elevationDp));
                root.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View v, Outline outline) {
                        outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(radiusDp));
                    }
                });
            }

            /* ---- 窗口配置 ---- */
            dialog.setContentView(root);
            dialog.setCanceledOnTouchOutside(canceledOnTouchOutside);
            dialog.setCancelable(canceledOnBack);

            Window win = dialog.getWindow();
            if (win != null) {
                win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams wp = win.getAttributes();
                /* 屏幕宽度减去左右留白，弹窗不贴边 */
                int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
                int sideMargin  = dp(horizontalMarginDp) * 2;
                wp.width = Math.max(dp(200), screenWidth - sideMargin);
                wp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                wp.gravity = Gravity.CENTER;
                wp.dimAmount = 0.5f;
                win.setAttributes(wp);
                win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }

            dialog.show();
            return dialog;
        }

        /* ========================= 按钮三态美化 ========================= */
        private void applyButton() {
            int pressed = pressedColor != 0 ? pressedColor : darken(color, 0.82f);

            StateListDrawable sld = new StateListDrawable();
            sld.addState(new int[]{ -android.R.attr.state_enabled },
                    buildShape(disabledColor, btnRadiusDp));
            sld.addState(new int[]{ android.R.attr.state_pressed, android.R.attr.state_enabled },
                    buildShape(pressed, btnRadiusDp));
            sld.addState(new int[]{}, buildShape(color, btnRadiusDp));

            int pl = btnView.getPaddingLeft();
            int pt = btnView.getPaddingTop();
            int pr = btnView.getPaddingRight();
            int pb = btnView.getPaddingBottom();

            btnView.setBackground(sld);

            int newPl = btnPadHDp != null ? dp(btnPadHDp) : pl;
            int newPt = btnPadVDp != null ? dp(btnPadVDp) : pt;
            int newPr = btnPadHDp != null ? dp(btnPadHDp) : pr;
            int newPb = btnPadVDp != null ? dp(btnPadVDp) : pb;
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
                btnView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(textSizeDp));
            }
            btnView.setTypeface(btnView.getTypeface(),
                    bold ? Typeface.BOLD : Typeface.NORMAL);
        }

        /* ---- 弹窗内按钮：纯色圆角，无边框 ---- */
        private Button createDialogButton(String text, int textColorValue, int bgColor) {
            Button b = new Button(activity);
            b.setText(text);
            b.setAllCaps(false);
            b.setTextColor(textColorValue);
            b.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(15));
            b.setIncludeFontPadding(false);
            b.setPadding(0, 0, 0, 0);
            b.setBackground(buildShape(bgColor, 10f));
            b.setStateListAnimator(null);
            return b;
        }

        /* ---- 输入框背景 ---- */
        private GradientDrawable buildInputBg() {
            GradientDrawable d = new GradientDrawable();
            d.setColor(0xFFF3F4F6);
            d.setCornerRadius(dp(10));
            d.setStroke(dp(1), 0xFFE5E7EB);
            return d;
        }

        /* ---- 主题色解析 ---- */
        private int resolveAccent() {
            if (accentColor != 0) return accentColor;
            switch (style) {
                case STYLE_SUCCESS: return 0xFF2E7D32;
                case STYLE_ERROR:   return 0xFFC62828;
                case STYLE_WARNING: return 0xFFEF6C00;
                case STYLE_NORMAL:
                default:            return DEFAULT_COLOR;
            }
        }

        /* ---- 内部工具 ---- */
        private GradientDrawable buildShape(int fillColor, float radius) {
            GradientDrawable d = new GradientDrawable();
            d.setColor(fillColor);
            d.setCornerRadius(dp(radius));
            return d;
        }

        private int dp(float value) {
            Context c = activity != null ? activity : (btnView != null ? btnView.getContext() : null);
            return c == null ? Math.round(value) : Math.round(value * c.getResources().getDisplayMetrics().density);
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