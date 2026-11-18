package mr.x.zjx;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.*;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 加载中提示工具：简约现代风加载弹窗（自绘转圈）+ 按钮三态美化。
 * 仅使用 Android 原生 API，兼容 API 25+。
 */
public final class LoadingUtil {

    public static final int   DEFAULT_COLOR          = 0xFF3F7EF7;
    public static final int   DEFAULT_DISABLED_COLOR = 0xFFBFC6D1;
    public static final float DEFAULT_RADIUS_DP      = 16f;
    public static final int   DEFAULT_PERIOD_MS      = 900;

    /* 当前显示的加载弹窗（全局唯一，便于直接关闭） */
    private static DialogHandle currentHandle = null;

    private LoadingUtil() { }

    /* ========================= 回调接口 ========================= */
    public interface OnCancelListener {
        void onCancel();
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

    public static Builder show(Activity activity) {
        return new Builder(activity);
    }

    /* ========================= 直接关闭 ========================= */
    /** 直接关闭当前显示的加载弹窗，无需持有句柄 */
    public static void dismiss() {
        if (currentHandle != null) {
            currentHandle.dismiss();
            currentHandle = null;
        }
    }

    /** 当前是否有加载弹窗在显示 */
    public static boolean isShowing() {
        return currentHandle != null && currentHandle.isShowing();
    }

    /* ========================= 弹窗句柄 ========================= */
    /** show().apply() 返回的句柄，用于手动关闭 */
    public static final class DialogHandle {
        private final Dialog dialog;
        private final SpinnerView spinner;
        DialogHandle(Dialog dialog, SpinnerView spinner) {
            this.dialog = dialog;
            this.spinner = spinner;
        }
        public void dismiss() {
            if (spinner != null) spinner.stop();
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
        }
        public boolean isShowing() {
            return dialog != null && dialog.isShowing();
        }
    }

    /* ========================= 自绘圆环转圈 ========================= */
    static final class SpinnerView extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arcRect = new RectF();
        private final Matrix matrix = new Matrix();
        private final int ringColor;
        private final float strokeWidthPx;
        private final long periodMs;

        private SweepGradient gradient;
        private long startTime = 0L;
        private float angle = 0f;
        private boolean running = false;

        SpinnerView(Context ctx, int ringColor, float strokeDp, int periodMs) {
            super(ctx);
            this.ringColor = ringColor;
            this.strokeWidthPx = strokeDp * getResources().getDisplayMetrics().density;
            this.periodMs = periodMs;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidthPx);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setDither(true);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            float inset = strokeWidthPx / 2f + 1f;
            arcRect.set(inset, inset, w - inset, h - inset);
            /* 渐变圈：主色不透明 → 同色全透明，形成拖尾 */
            gradient = new SweepGradient(w / 2f, h / 2f,
                    new int[]{ ringColor, withAlpha(ringColor, 0f) },
                    new float[]{ 0f, 1f });
            matrix.setRotate(-90f, w / 2f, h / 2f);
            gradient.setLocalMatrix(matrix);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (arcRect.isEmpty()) return;
            paint.setShader(gradient);
            canvas.rotate(angle, getWidth() / 2f, getHeight() / 2f);
            canvas.drawArc(arcRect, 0f, 300f, false, paint);
        }

        void start() {
            if (running) return;
            running = true;
            startTime = System.currentTimeMillis();
            postInvalidateOnAnimation();
            post(new Runnable() {
                @Override
                public void run() {
                    if (!running) return;
                    long elapsed = System.currentTimeMillis() - startTime;
                    angle = (elapsed % periodMs) * 360f / periodMs;
                    invalidate();
                    postOnAnimation(this);
                }
            });
        }

        void stop() {
            running = false;
        }
    }

    /* ========================= Builder ========================= */
    public static final class Builder {

        /* ---- 按钮美化模式 ---- */
        private final Button btnView;

        /* ---- 加载弹窗模式 ---- */
        private final Activity activity;

        /* ---- 按钮美化通用 ---- */
        private int     color         = DEFAULT_COLOR;
        private int     disabledColor = DEFAULT_DISABLED_COLOR;
        private int     pressedColor  = 0;
        private int     btnTextColor  = 0;
        private boolean bold          = false;
        private float   textSizeDp    = 0f;
        private float   btnRadiusDp   = 10f;

        /* ---- 加载弹窗外观 ---- */
        private String  message        = "加载中…";
        private int     cardColor      = 0xFFFFFFFF;
        private int     messageColor   = 0xFF4B5563;
        private int     ringColorValue = DEFAULT_COLOR;
        private float   radiusDp       = DEFAULT_RADIUS_DP;
        private float   spinnerDp      = 34f;
        private float   spinnerStrokeDp = 3.5f;
        private int     periodMs       = DEFAULT_PERIOD_MS;
        private boolean cancelable     = false;
        private String  cancelText     = "取消";
        private OnCancelListener cancelListener = null;
        private boolean canceledOnTouchOutside = false;

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
            this.ringColorValue = color;
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

        /* ---- 加载弹窗外观 ---- */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder cardColor(int color) {
            this.cardColor = color;
            return this;
        }

        public Builder messageColor(int color) {
            this.messageColor = color;
            return this;
        }

        public Builder ringColor(int color) {
            this.ringColorValue = color;
            return this;
        }

        public Builder radius(float radiusDp) {
            this.radiusDp = radiusDp;
            return this;
        }

        public Builder spinnerSize(float sizeDp) {
            this.spinnerDp = sizeDp;
            return this;
        }

        public Builder spinnerStroke(float strokeDp) {
            this.spinnerStrokeDp = strokeDp;
            return this;
        }

        public Builder period(int ms) {
            this.periodMs = ms > 0 ? ms : DEFAULT_PERIOD_MS;
            return this;
        }

        public Builder cancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public Builder cancelText(String text) {
            this.cancelText = text;
            return this;
        }

        public Builder onCancel(OnCancelListener listener) {
            this.cancelListener = listener;
            return this;
        }

        public Builder canceledOnTouchOutside(boolean cancel) {
            this.canceledOnTouchOutside = cancel;
            return this;
        }

        /* ---- 核心应用 ---- */
        public DialogHandle apply() {
            if (btnView != null) {
                applyButton();
                return null;
            }
            if (activity != null) {
                return showLoading();
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

        /* ========================= 加载弹窗 ========================= */
        private DialogHandle showLoading() {
            /* 先关闭已存在的加载弹窗，避免叠加 */
            dismiss();

            Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            /* ---- 卡片容器 ---- */
            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER_HORIZONTAL);
            card.setPadding(dp(28, activity), dp(26, activity),
                    dp(28, activity), dp(22, activity));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(cardColor);
            bg.setCornerRadius(dp(radiusDp, activity));
            card.setBackground(bg);

            /* ---- 自绘转圈 ---- */
            SpinnerView spinner = new SpinnerView(activity, ringColorValue,
                    spinnerStrokeDp, periodMs);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    dp(spinnerDp, activity), dp(spinnerDp, activity));
            card.addView(spinner, slp);

            /* ---- 文案 ---- */
            if (message != null && message.length() > 0) {
                TextView tv = new TextView(activity);
                tv.setText(message);
                tv.setTextColor(messageColor);
                tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(14, activity));
                tv.setIncludeFontPadding(false);
                tv.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                tlp.topMargin = dp(16, activity);
                card.addView(tv, tlp);
            }

            /* ---- 取消按钮 ---- */
            if (cancelable) {
                TextView cancel = new TextView(activity);
                cancel.setText(cancelText);
                cancel.setTextColor(ringColorValue);
                cancel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(14, activity));
                cancel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                cancel.setIncludeFontPadding(false);
                cancel.setPadding(dp(12, activity), dp(8, activity),
                        dp(12, activity), dp(8, activity));
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                clp.topMargin = dp(10, activity);
                final OnCancelListener cl = cancelListener;
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (cl != null) cl.onCancel();
                    }
                });
                card.addView(cancel, clp);
            }

            /* ---- 窗口配置 ---- */
            dialog.setContentView(card);
            dialog.setCancelable(cancelable);
            dialog.setCanceledOnTouchOutside(canceledOnTouchOutside);

            Window win = dialog.getWindow();
            if (win != null) {
                win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams wp = win.getAttributes();
                wp.width = WindowManager.LayoutParams.WRAP_CONTENT;
                wp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                wp.gravity = Gravity.CENTER;
                wp.dimAmount = 0.35f;
                win.setAttributes(wp);
                win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }

            dialog.show();
            spinner.start();

            DialogHandle handle = new DialogHandle(dialog, spinner);
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