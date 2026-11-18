package mr.x.zjx;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.*;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * 悬浮窗工具：可拖动悬浮球 / 悬浮卡片 + 贴边吸附 + 点击回调 + 按钮三态美化。
 * 仅使用 Android 原生 API，兼容 API 25+（使用 WindowManager 添加应用内悬浮层）。
 */
public final class FloatingWindowUtil {

    public static final int   DEFAULT_COLOR          = 0xFF3F7EF7;
    public static final int   DEFAULT_DISABLED_COLOR = 0xFFBFC6D1;
    public static final float DEFAULT_RADIUS_DP      = 12f;

    /* 当前悬浮窗（全局唯一，便于直接关闭） */
    private static Handle currentHandle = null;

    private FloatingWindowUtil() { }

    /* ========================= 回调接口 ========================= */
    public interface OnFloatingClickListener {
        void onClick(View v);
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
    /** 直接关闭当前悬浮窗 */
    public static void dismiss() {
        if (currentHandle != null) {
            currentHandle.dismiss();
            currentHandle = null;
        }
    }

    /** 当前是否有悬浮窗显示 */
    public static boolean isShowing() {
        return currentHandle != null;
    }

    /* ========================= 悬浮窗句柄 ========================= */
    public static final class Handle {
        private final WindowManager windowManager;
        private final View view;
        Handle(WindowManager wm, View view) {
            this.windowManager = wm;
            this.view = view;
        }
        public void dismiss() {
            try {
                if (view.getParent() != null) {
                    windowManager.removeView(view);
                }
            } catch (Exception ignored) { }
        }
        public View getView() {
            return view;
        }
    }

    /* ========================= Builder ========================= */
    public static final class Builder {

        /* ---- 按钮美化模式 ---- */
        private final Button btnView;

        /* ---- 悬浮窗模式 ---- */
        private final Activity activity;

        /* ---- 按钮美化通用 ---- */
        private int     color         = DEFAULT_COLOR;
        private int     disabledColor = DEFAULT_DISABLED_COLOR;
        private int     pressedColor  = 0;
        private int     btnTextColor  = 0;
        private boolean bold          = false;
        private float   textSizeDp    = 0f;
        private float   btnRadiusDp   = 10f;

        /* ---- 悬浮窗外观 ---- */
        private String  text          = "悬浮窗";
        private int     cardColor     = DEFAULT_COLOR;
        private int     textColorVal  = 0xFFFFFFFF;
        private int     textSizeSp    = 14;
        private float   radiusDp      = DEFAULT_RADIUS_DP;
        private boolean circle        = false;
        private boolean draggable     = true;
        private boolean snapToEdge    = true;
        private boolean closable      = false;
        private boolean elevation     = true;
        private boolean ripple        = true;
        private int     gravity       = Gravity.TOP | Gravity.START;
        private int     marginXDp     = 16;
        private int     marginYDp     = 120;
        private int     paddingHDp    = 16;
        private int     paddingVDp    = 12;
        private OnFloatingClickListener clickListener = null;

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
            this.cardColor = color;
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

        /* ---- 悬浮窗外观 ---- */
        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder textColor(int color) {
            this.textColorVal = color;
            return this;
        }

        public Builder textSize(int sizeSp) {
            this.textSizeSp = sizeSp;
            return this;
        }

        public Builder radius(float radiusDp) {
            this.radiusDp = radiusDp;
            return this;
        }

        public Builder circle() {
            this.circle = true;
            return this;
        }

        public Builder draggable(boolean draggable) {
            this.draggable = draggable;
            return this;
        }

        public Builder snapToEdge(boolean snap) {
            this.snapToEdge = snap;
            return this;
        }

        public Builder closable(boolean closable) {
            this.closable = closable;
            return this;
        }

        public Builder elevation(boolean elevation) {
            this.elevation = elevation;
            return this;
        }

        public Builder ripple(boolean ripple) {
            this.ripple = ripple;
            return this;
        }

        public Builder gravity(int gravity) {
            this.gravity = gravity;
            return this;
        }

        public Builder margin(int xDp, int yDp) {
            this.marginXDp = xDp;
            this.marginYDp = yDp;
            return this;
        }

        public Builder padding(int hDp, int vDp) {
            this.paddingHDp = hDp;
            this.paddingVDp = vDp;
            return this;
        }

        public Builder onClick(OnFloatingClickListener listener) {
            this.clickListener = listener;
            return this;
        }

        /* ---- 核心应用 ---- */
        public Handle apply() {
            if (btnView != null) {
                applyButton();
                return null;
            }
            if (activity != null) {
                return showFloating();
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

        /* ========================= 悬浮窗显示 ========================= */
        private Handle showFloating() {
            dismiss();

            final Context ctx = activity;
            final WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);

            /* ---- 内容：TextView 作为悬浮球/卡片 ---- */
            final TextView content = new TextView(ctx);
            content.setText(text);
            content.setTextColor(textColorVal);
            content.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp);
            content.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            content.setIncludeFontPadding(false);
            content.setGravity(Gravity.CENTER);
            content.setPadding(dp(paddingHDp, ctx), dp(paddingVDp, ctx),
                    dp(paddingHDp, ctx), dp(paddingVDp, ctx));

            /* 背景 */
            content.setBackground(buildFloatingBg(ctx));

            /* 阴影 */
            if (elevation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                content.setElevation(dp(8, ctx));
                content.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View v, Outline outline) {
                        if (circle) {
                            outline.setOval(0, 0, v.getWidth(), v.getHeight());
                        } else {
                            outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(),
                                    dp(radiusDp, ctx));
                        }
                    }
                });
                content.setClipToOutline(true);
            }

            /* ---- 关闭小按钮（可选） ---- */
            final FrameLayout wrapper = new FrameLayout(ctx);
            wrapper.addView(content);
            if (closable) {
                TextView close = new TextView(ctx);
                close.setText("×");
                close.setTextColor(0xFFFFFFFF);
                close.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
                close.setGravity(Gravity.CENTER);
                GradientDrawable cbg = new GradientDrawable();
                cbg.setColor(0xFFEF4444);
                cbg.setShape(GradientDrawable.OVAL);
                close.setBackground(cbg);
                FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                        dp(20, ctx), dp(20, ctx));
                clp.gravity = Gravity.TOP | Gravity.END;
                close.setLayoutParams(clp);
                close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });
                wrapper.addView(close);
            }

            /* ---- WindowManager 参数 ---- */
            final WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;
            lp.format = PixelFormat.TRANSLUCENT;
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = gravity;
            lp.x = dp(marginXDp, ctx);
            lp.y = dp(marginYDp, ctx);

            /* ---- 拖动 + 点击 ---- */
            final int[] downX = new int[1];
            final int[] downY = new int[1];
            final int[] lastX = new int[1];
            final int[] lastY = new int[1];
            final boolean[] moved = new boolean[1];

            content.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            downX[0] = lastX[0] = (int) event.getRawX();
                            downY[0] = lastY[0] = (int) event.getRawY();
                            moved[0] = false;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            if (!draggable) return true;
                            int dx = (int) event.getRawX() - lastX[0];
                            int dy = (int) event.getRawY() - lastY[0];
                            lastX[0] = (int) event.getRawX();
                            lastY[0] = (int) event.getRawY();
                            lp.x += dx;
                            lp.y += dy;
                            if (Math.abs(lp.x) > dp(4, ctx) || Math.abs(lp.y) > dp(4, ctx)) {
                                moved[0] = true;
                            }
                            try {
                                wm.updateViewLayout(wrapper, lp);
                            } catch (Exception ignored) { }
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (!moved[0] && clickListener != null) {
                                clickListener.onClick(v);
                            }
                            if (snapToEdge && draggable) {
                                snap(wrapper, lp, wm, ctx);
                            }
                            return true;
                    }
                    return false;
                }
            });

            /* ---- 添加并入场 ---- */
            try {
                wm.addView(wrapper, lp);
            } catch (Exception e) {
                return null;
            }

            wrapper.setAlpha(0f);
            wrapper.setScaleX(0.85f);
            wrapper.setScaleY(0.85f);
            wrapper.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(240)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();

            Handle handle = new Handle(wm, wrapper);
            currentHandle = handle;
            return handle;
        }

        /* ---- 贴边吸附 ---- */
        private void snap(View view, WindowManager.LayoutParams lp,
                          WindowManager wm, Context ctx) {
            int screenW = ctx.getResources().getDisplayMetrics().widthPixels;
            int viewW = view.getWidth();
            /* 判断靠左还是靠右 */
            if (lp.x + viewW / 2 > screenW / 2) {
                lp.x = screenW - viewW - dp(marginXDp, ctx);
            } else {
                lp.x = dp(marginXDp, ctx);
            }
            try {
                wm.updateViewLayout(view, lp);
            } catch (Exception ignored) { }
        }

        /* ---- 悬浮窗背景 ---- */
        private GradientDrawable buildFloatingBg(Context ctx) {
            GradientDrawable normal = new GradientDrawable();
            if (circle) {
                normal.setShape(GradientDrawable.OVAL);
            } else {
                normal.setCornerRadius(dp(radiusDp, ctx));
            }
            normal.setColor(cardColor);

            if (ripple && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                GradientDrawable mask = new GradientDrawable();
                if (circle) {
                    mask.setShape(GradientDrawable.OVAL);
                } else {
                    mask.setCornerRadius(dp(radiusDp, ctx));
                }
                mask.setColor(Color.WHITE);
                return null; /* 由下方 setBackground 处理 */
            }
            return normal;
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