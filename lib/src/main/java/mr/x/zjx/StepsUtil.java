package mr.x.zjx;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 步骤条工具：横向/纵向流程节点（序号 / 对勾）+ 连接线 + 当前步高亮 + 节点点击回调
 * + 动态推进（next / prev / setCurrent）。
 * 仅使用 Android 原生 API，兼容 API 25+。
 */
public final class StepsUtil {

    public static final int   DEFAULT_COLOR         = 0xFF3F7EF7;
    public static final int   DEFAULT_INACTIVE      = 0xFFCBD5E1;
    public static final int   DEFAULT_TEXT_COLOR    = 0xFF111827;
    public static final int   DEFAULT_TEXT_INACTIVE = 0xFF9CA3AF;

    public static final int   NODE_SIZE_DP          = 28;
    public static final int   LINE_WIDTH_DP         = 2;
    public static final int   H_PADDING_DP          = 16;

    /* 容器 → 配置快照，用于动态推进后重绘 */
    private static final Map<ViewGroup, Config> STATES = new WeakHashMap<>();

    private StepsUtil() { }

    /* ========================= 回调接口 ========================= */
    public interface OnStepClickListener {
        void onClick(int position, String title);
    }

    /* ========================= 对外快捷入口 ========================= */
    public static Builder beautify(ViewGroup stepsBox) {
        return beautify(stepsBox, DEFAULT_COLOR);
    }

    public static Builder beautify(ViewGroup stepsBox, int activeColor) {
        return build(stepsBox).activeColor(activeColor);
    }

    public static Builder build(ViewGroup stepsBox) {
        return new Builder(stepsBox);
    }

    /* ========================= 动态推进 API ========================= */
    /** 进入下一个步骤；已是最后一步返回 false */
    public static boolean next(ViewGroup box) {
        Config c = box == null ? null : STATES.get(box);
        if (c == null || c.titles.length == 0) return false;
        if (c.allDone) return false;
        if (c.current >= c.titles.length - 1) return false;
        c.current++;
        render(box, c);
        return true;
    }

    /** 返回上一个步骤；已是第一步返回 false */
    public static boolean prev(ViewGroup box) {
        Config c = box == null ? null : STATES.get(box);
        if (c == null || c.titles.length == 0) return false;
        if (c.allDone) {
            c.allDone = false;
            c.current = c.titles.length - 1;
            render(box, c);
            return true;
        }
        if (c.current <= 0) return false;
        c.current--;
        render(box, c);
        return true;
    }

    /** 跳转到指定步骤（0 起，越界自动收敛） */
    public static void setCurrent(ViewGroup box, int index) {
        Config c = box == null ? null : STATES.get(box);
        if (c == null || c.titles.length == 0) return;
        c.allDone = false;
        c.current = Math.min(Math.max(index, 0), c.titles.length - 1);
        render(box, c);
    }

    /** 当前步骤下标（0 起） */
    public static int getCurrent(ViewGroup box) {
        Config c = box == null ? null : STATES.get(box);
        return c == null ? -1 : c.current;
    }

    /** 总步骤数 */
    public static int getTotal(ViewGroup box) {
        Config c = box == null ? null : STATES.get(box);
        return c == null ? 0 : c.titles.length;
    }

    /** 是否已全部完成 */
    public static boolean isAllDone(ViewGroup box) {
        Config c = box == null ? null : STATES.get(box);
        return c != null && c.allDone;
    }

    /* ========================= 配置快照 ========================= */
    private static final class Config {
        String[] titles = new String[0];
        int      current;
        boolean  allDone;
        boolean  vertical;
        boolean  clickable;

        OnStepClickListener clickListener;

        int activeColor       = DEFAULT_COLOR;
        int inactiveColor     = DEFAULT_INACTIVE;
        int textColor         = DEFAULT_TEXT_COLOR;
        int inactiveTextColor = DEFAULT_TEXT_INACTIVE;
        int lineActiveColor   = 0;
        int lineInactiveColor = 0;
        int nodeTextColor     = 0xFFFFFFFF;
    }

    /* ========================= Builder ========================= */
    public static final class Builder {

        private final ViewGroup stepsBox;

        private String[] titles     = new String[0];
        private int      current    = 0;
        private boolean  allDone    = false;
        private boolean  vertical   = false;
        private boolean  clickable  = false;
        private OnStepClickListener clickListener = null;

        private int activeColor       = DEFAULT_COLOR;
        private int inactiveColor     = DEFAULT_INACTIVE;
        private int textColor         = DEFAULT_TEXT_COLOR;
        private int inactiveTextColor = DEFAULT_TEXT_INACTIVE;
        private int lineActiveColor   = 0;
        private int lineInactiveColor = 0;
        private int nodeTextColor     = 0xFFFFFFFF;

        private Builder(ViewGroup stepsBox) {
            this.stepsBox = stepsBox;
        }

        /* ---- 数据配置 ---- */
        public Builder steps(String... titles) {
            this.titles = titles == null ? new String[0] : titles;
            return this;
        }

        public Builder current(int index) {
            this.current = index;
            return this;
        }

        public Builder allDone(boolean allDone) {
            this.allDone = allDone;
            return this;
        }

        public Builder vertical(boolean vertical) {
            this.vertical = vertical;
            return this;
        }

        public Builder clickable(boolean clickable) {
            this.clickable = clickable;
            return this;
        }

        public Builder onStepClick(OnStepClickListener listener) {
            this.clickListener = listener;
            return this;
        }

        /* ---- 颜色配置 ---- */
        public Builder activeColor(int color) {
            this.activeColor = color;
            return this;
        }

        public Builder inactiveColor(int color) {
            this.inactiveColor = color;
            return this;
        }

        public Builder textColor(int color) {
            this.textColor = color;
            return this;
        }

        public Builder inactiveTextColor(int color) {
            this.inactiveTextColor = color;
            return this;
        }

        public Builder lineActiveColor(int color) {
            this.lineActiveColor = color;
            return this;
        }

        public Builder lineInactiveColor(int color) {
            this.lineInactiveColor = color;
            return this;
        }

        public Builder nodeTextColor(int color) {
            this.nodeTextColor = color;
            return this;
        }

        /* ---- 核心应用 ---- */
        public void apply() {
            if (stepsBox == null || titles.length == 0) return;

            /* 保存配置快照，供 next/prev/setCurrent 重绘 */
            Config c = new Config();
            c.titles            = titles.clone();
            c.current           = current;
            c.allDone           = allDone;
            c.vertical          = vertical;
            c.clickable         = clickable;
            c.clickListener     = clickListener;
            c.activeColor       = activeColor;
            c.inactiveColor     = inactiveColor;
            c.textColor         = textColor;
            c.inactiveTextColor = inactiveTextColor;
            c.lineActiveColor   = lineActiveColor;
            c.lineInactiveColor = lineInactiveColor;
            c.nodeTextColor     = nodeTextColor;

            STATES.put(stepsBox, c);
            render(stepsBox, c);
        }
    }

    /* ========================= 渲染 ========================= */
    private static void render(ViewGroup stepsBox, Config c) {
        Context ctx = stepsBox.getContext();

        stepsBox.removeAllViews();
        stepsBox.setClipChildren(false);
        stepsBox.setClipToPadding(false);

        int lineActive   = c.lineActiveColor   != 0 ? c.lineActiveColor   : c.activeColor;
        int lineInactive = c.lineInactiveColor != 0 ? c.lineInactiveColor : c.inactiveColor;

        int total = c.titles.length;
        int cur   = c.allDone ? total : Math.min(Math.max(c.current, 0), total - 1);

        /* 拿到可设置方向的 LinearLayout 作为真实容器 */
        LinearLayout host;
        if (stepsBox instanceof LinearLayout) {
            host = (LinearLayout) stepsBox;
        } else {
            LinearLayout inner = new LinearLayout(ctx);
            inner.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            stepsBox.addView(inner);
            host = inner;
        }

        if (c.vertical) {
            buildVertical(ctx, host, c, total, cur, lineActive, lineInactive);
        } else {
            buildHorizontal(ctx, host, c, total, cur, lineActive, lineInactive);
        }
    }

    /* ========================= 横向布局 ========================= */
    private static void buildHorizontal(final Context ctx, final LinearLayout host,
                                        final Config c, int total, int cur,
                                        int lineActive, int lineInactive) {
        host.setOrientation(LinearLayout.HORIZONTAL);
        host.setPadding(dp(H_PADDING_DP, ctx), dp(4, ctx),
                dp(H_PADDING_DP, ctx), dp(4, ctx));

        for (int i = 0; i < total; i++) {
            final int pos = i;
            final String title = c.titles[i];

            LinearLayout cell = new LinearLayout(ctx);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            cell.setLayoutParams(clp);

            FrameLayout nodeRow = new FrameLayout(ctx);
            LinearLayout.LayoutParams nrlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(NODE_SIZE_DP, ctx));
            nodeRow.setLayoutParams(nrlp);

            if (i > 0) {
                View leftLine = new View(ctx);
                leftLine.setBackgroundColor(i <= cur ? lineActive : lineInactive);
                FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        dp(LINE_WIDTH_DP, ctx));
                llp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
                llp.rightMargin = dp(NODE_SIZE_DP / 2 + 4, ctx);
                nodeRow.addView(leftLine, llp);
            }
            if (i < total - 1) {
                View rightLine = new View(ctx);
                rightLine.setBackgroundColor(i < cur ? lineActive : lineInactive);
                FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        dp(LINE_WIDTH_DP, ctx));
                rlp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
                rlp.leftMargin = dp(NODE_SIZE_DP / 2 + 4, ctx);
                nodeRow.addView(rightLine, rlp);
            }

            boolean done    = i < cur;
            boolean current = i == cur;
            TextView node = buildNode(ctx, c, i + 1, done, current);
            FrameLayout.LayoutParams nlpp = new FrameLayout.LayoutParams(
                    dp(NODE_SIZE_DP, ctx), dp(NODE_SIZE_DP, ctx));
            nlpp.gravity = Gravity.CENTER;
            nodeRow.addView(node, nlpp);

            cell.addView(nodeRow);

            TextView tv = buildTitle(ctx, c, title, current, done);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = dp(8, ctx);
            cell.addView(tv, tlp);

            if (c.clickable) {
                cell.setClickable(true);
                cell.setBackground(buildCellPressBg(ctx));
                cell.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (c.clickListener != null) c.clickListener.onClick(pos, title);
                    }
                });
            }

            host.addView(cell);
        }
    }

    /* ========================= 纵向布局 ========================= */
    private static void buildVertical(final Context ctx, final LinearLayout host,
                                      final Config c, int total, int cur,
                                      int lineActive, int lineInactive) {
        host.setOrientation(LinearLayout.VERTICAL);
        host.setPadding(dp(H_PADDING_DP, ctx), dp(8, ctx),
                dp(H_PADDING_DP, ctx), dp(8, ctx));

        for (int i = 0; i < total; i++) {
            final int pos = i;
            final String title = c.titles[i];

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout leftCol = new LinearLayout(ctx);
            leftCol.setOrientation(LinearLayout.VERTICAL);
            leftCol.setGravity(Gravity.CENTER_HORIZONTAL);
            leftCol.setLayoutParams(new LinearLayout.LayoutParams(
                    dp(NODE_SIZE_DP, ctx), ViewGroup.LayoutParams.MATCH_PARENT));

            boolean done    = i < cur;
            boolean current = i == cur;
            TextView node = buildNode(ctx, c, i + 1, done, current);
            LinearLayout.LayoutParams nlpp = new LinearLayout.LayoutParams(
                    dp(NODE_SIZE_DP, ctx), dp(NODE_SIZE_DP, ctx));
            leftCol.addView(node, nlpp);

            if (i < total - 1) {
                View line = new View(ctx);
                line.setBackgroundColor(i < cur ? lineActive : lineInactive);
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                        dp(LINE_WIDTH_DP, ctx), 0, 1f);
                llp.topMargin = dp(4, ctx);
                llp.bottomMargin = dp(4, ctx);
                llp.gravity = Gravity.CENTER_HORIZONTAL;
                leftCol.addView(line, llp);
            }
            row.addView(leftCol);

            TextView tv = buildTitle(ctx, c, title, current, done);
            tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tlp.leftMargin = dp(12, ctx);
            tv.setPadding(0, dp(5, ctx), 0, dp(5, ctx));
            row.addView(tv, tlp);

            if (i < total - 1) {
                row.setMinimumHeight(dp(NODE_SIZE_DP + 20, ctx));
            }

            if (c.clickable) {
                row.setClickable(true);
                row.setBackground(buildCellPressBg(ctx));
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (c.clickListener != null) c.clickListener.onClick(pos, title);
                    }
                });
            }

            host.addView(row);
        }
    }

    /* ---- 圆形节点 ---- */
    private static TextView buildNode(Context ctx, Config c, int number,
                                      boolean done, boolean current) {
        TextView node = new TextView(ctx);
        node.setGravity(Gravity.CENTER);
        node.setIncludeFontPadding(false);
        node.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
        node.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);

        if (done) {
            bg.setColor(c.activeColor);
            node.setText("✓");
            node.setTextColor(c.nodeTextColor);
            node.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
        } else if (current) {
            bg.setColor(c.activeColor);
            node.setText(String.valueOf(number));
            node.setTextColor(c.nodeTextColor);
        } else {
            bg.setColor(Color.TRANSPARENT);
            bg.setStroke(dp(1.5f, ctx), c.inactiveColor);
            node.setText(String.valueOf(number));
            node.setTextColor(c.inactiveColor);
        }
        node.setBackground(bg);
        return node;
    }

    /* ---- 标题 ---- */
    private static TextView buildTitle(Context ctx, Config c, String title,
                                       boolean current, boolean done) {
        TextView tv = new TextView(ctx);
        tv.setText(title);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tv.setIncludeFontPadding(false);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);

        if (current || done) {
            tv.setTextColor(c.textColor);
            tv.setTypeface(Typeface.DEFAULT, current ? Typeface.BOLD : Typeface.NORMAL);
        } else {
            tv.setTextColor(c.inactiveTextColor);
            tv.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        }
        return tv;
    }

    /* ---- 节点按压背景 ---- */
    private static GradientDrawable buildCellPressBg(Context ctx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(0x0A000000);
        d.setCornerRadius(dp(8, ctx));
        return d;
    }

    /* ---- dp ---- */
    private static int dp(float value, Context ctx) {
        return Math.round(value * ctx.getResources().getDisplayMetrics().density);
    }

    /* ========================= 颜色工具 ========================= */
    public static int withAlpha(int color, float alpha) {
        int a = Math.min(255, Math.max(0, (int) (255 * alpha)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static int darken(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min(255, Math.max(0, (int) (Color.red(color) * factor)));
        int g = Math.min(255, Math.max(0, (int) (Color.green(color) * factor)));
        int b = Math.min(255, Math.max(0, (int) (Color.blue(color) * factor)));
        return Color.argb(a, r, g, b);
    }
}