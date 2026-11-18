package mr.x.zjx;

import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ProgressBar;
import android.widget.SeekBar;

/**
 * 进度条控件美化工具类（支持水平 ProgressBar / SeekBar / 不确定态圆形进度条）
 *
 * 用法：
 *   ProgressBar pb = findViewById(R.id.jdt1);
 *   ProgressBarUtil.beautify(pb);                               // 一键默认美化
 *   ProgressBarUtil.beautify(pb, 0xFF3F7EF7);                   // 指定主色
 *
 *   ProgressBarUtil.with(pb)                                    // 链式自定义
 *       .color(0xFF3F7EF7)
 *       .trackColor(0xFFE8ECF2)
 *       .gradient(0xFF5FA8FF)
 *       .height(10)
 *       .radius(5)
 *       .animate(75, 600)                                       // 带进度动画
 *       .apply();
 *
 *   // SeekBar 直接用（继承自 ProgressBar）
 *   SeekBar seek = findViewById(R.id.seek1);
 *   ProgressBarUtil.with(seek).color(0xFF3F7EF7).height(6)
 *       .thumbColor(0xFF3F7EF7).apply();
 */
public final class ProgressBarUtil {

    /** 默认进度色 */
    public static final int DEFAULT_COLOR = 0xFF3F7EF7;
    /** 默认轨道色 */
    public static final int DEFAULT_TRACK_COLOR = 0xFFE8ECF2;
    /** 默认二级进度色 */
    public static final int DEFAULT_SECONDARY_COLOR = 0x333F7EF7;
    /** 默认高度(dp) */
    public static final float DEFAULT_HEIGHT_DP = 8f;

    private ProgressBarUtil() {
        // 禁止实例化
    }

    /* ========================= 对外快捷入口 ========================= */

    /** 一键美化（默认主题） */
    public static void beautify(ProgressBar pb) {
        beautify(pb, DEFAULT_COLOR);
    }

    /** 一键美化（指定主色） */
    public static void beautify(ProgressBar pb, int color) {
        if (pb == null) {
            return;
        }
        with(pb).color(color).apply();
    }

    /** 返回 Builder，可链式自定义 */
    public static Builder with(ProgressBar pb) {
        return new Builder(pb);
    }

    /* ========================= Builder ========================= */

    public static final class Builder {

        private final ProgressBar pb;

        // ---- 颜色 ----
        private int color = DEFAULT_COLOR;
        private int trackColor = DEFAULT_TRACK_COLOR;
        private int secondaryColor = DEFAULT_SECONDARY_COLOR;
        private int thumbColor = 0;              // 仅 SeekBar 使用，0 表示不修改

        // ---- 尺寸 / 圆角 ----
        private Float heightDp = DEFAULT_HEIGHT_DP;
        private Float radiusDp = null;           // null = height / 2（胶囊）

        // ---- 渐变 ----
        private boolean gradient = false;
        private int gradientEndColor = 0;
        private GradientDrawable.Orientation orientation = GradientDrawable.Orientation.LEFT_RIGHT;

        // ---- 不确定态 ----
        private Integer indeterminateColor = null;

        // ---- 动画 ----
        private boolean animate = false;
        private int animateFrom = -1;
        private int animateTo = -1;
        private int animateDuration = 400;
        private Interpolator interpolator = new DecelerateInterpolator();

        private Builder(ProgressBar pb) {
            this.pb = pb;
        }

        /* ---------------------- 颜色 ---------------------- */

        /** 进度色 */
        public Builder color(int color) {
            this.color = color;
            return this;
        }

        /** 轨道（背景）色 */
        public Builder trackColor(int color) {
            this.trackColor = color;
            return this;
        }

        /** 二级进度色（setSecondaryProgress 时使用） */
        public Builder secondaryColor(int color) {
            this.secondaryColor = color;
            return this;
        }

        /** SeekBar 的滑块颜色（仅 SeekBar 有效，API 21+） */
        public Builder thumbColor(int color) {
            this.thumbColor = color;
            return this;
        }

        /* ---------------------- 尺寸 / 圆角 ---------------------- */

        /** 进度条高度(dp) */
        public Builder height(float dp) {
            this.heightDp = dp;
            return this;
        }

        /** 圆角(dp)，不设置则自动等于高度一半（胶囊） */
        public Builder radius(float dp) {
            this.radiusDp = dp;
            return this;
        }

        /* ---------------------- 渐变 ---------------------- */

        /** 线性渐变（从进度色渐变到 endColor） */
        public Builder gradient(int endColor) {
            return gradient(endColor, GradientDrawable.Orientation.LEFT_RIGHT);
        }

        /** 线性渐变 + 方向 */
        public Builder gradient(int endColor, GradientDrawable.Orientation o) {
            this.gradient = true;
            this.gradientEndColor = endColor;
            this.orientation = o;
            return this;
        }

        /* ---------------------- 不确定态 ---------------------- */

        /** 不确定态（圆形旋转）颜色 */
        public Builder indeterminateColor(int color) {
            this.indeterminateColor = color;
            return this;
        }

        /* ---------------------- 动画 ---------------------- */

        /** 从当前进度动画到 toProgress */
        public Builder animate(int toProgress) {
            return animate(toProgress, animateDuration);
        }

        /** 从当前进度动画到 toProgress，指定时长(ms) */
        public Builder animate(int toProgress, int durationMs) {
            this.animate = true;
            this.animateTo = toProgress;
            this.animateDuration = durationMs;
            return this;
        }

        /** 从 from 动画到 to，指定时长(ms) */
        public Builder animate(int from, int to, int durationMs) {
            this.animate = true;
            this.animateFrom = from;
            this.animateTo = to;
            this.animateDuration = durationMs;
            return this;
        }

        /** 动画插值器，默认 DecelerateInterpolator */
        public Builder interpolator(Interpolator i) {
            this.interpolator = i;
            return this;
        }

        /* ---------------------- 核心应用 ---------------------- */

        public void apply() {
            if (pb == null) {
                return;
            }

            final boolean indeterminate = pb.isIndeterminate();
            final float hDp = heightDp != null ? heightDp : DEFAULT_HEIGHT_DP;
            final int hPx = Math.max(1, Math.round(dp(hDp)));
            final float rPx = radiusDp != null ? dp(radiusDp) : hPx / 2f;

            // 1. 高度（不确定态圆环不修改，避免破坏系统尺寸）
            if (!indeterminate) {
                ViewGroup.LayoutParams lp = pb.getLayoutParams();
                if (lp != null) {
                    lp.height = hPx;
                    pb.setLayoutParams(lp);
                }
            }

            // 2. 水平进度条：自定义 drawable（轨道 / 二级进度 / 进度 三层）
            if (!indeterminate) {
                GradientDrawable bg  = buildShape(trackColor, rPx, 0, orientation);
                GradientDrawable sec = buildShape(secondaryColor, rPx, 0, orientation);
                GradientDrawable prg = buildShape(color, rPx,
                        gradient ? gradientEndColor : 0, orientation);

                ClipDrawable clipSec = new ClipDrawable(sec, Gravity.LEFT, ClipDrawable.HORIZONTAL);
                ClipDrawable clipPrg = new ClipDrawable(prg, Gravity.LEFT, ClipDrawable.HORIZONTAL);

                LayerDrawable ld = new LayerDrawable(new Drawable[]{bg, clipSec, clipPrg});
                ld.setId(0, android.R.id.background);
                ld.setId(1, android.R.id.secondaryProgress);
                ld.setId(2, android.R.id.progress);

                pb.setProgressDrawable(ld);

                // 清除系统 tint，避免覆盖自定义颜色
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    pb.setProgressTintList(null);
                    pb.setProgressBackgroundTintList(null);
                    pb.setSecondaryProgressTintList(null);
                }
            }

            // 3. 不确定态颜色（圆形旋转）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int ind = indeterminateColor != null ? indeterminateColor : color;
                pb.setIndeterminateTintList(ColorStateList.valueOf(ind));
            }

            // 4. SeekBar 滑块颜色
            if (thumbColor != 0
                    && pb instanceof SeekBar
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ((SeekBar) pb).setThumbTintList(ColorStateList.valueOf(thumbColor));
            }

            // 5. 进度动画
            if (animate) {
                int from = animateFrom >= 0 ? animateFrom : pb.getProgress();
                int to = animateTo >= 0 ? animateTo : pb.getMax();
                ObjectAnimator anim = ObjectAnimator.ofInt(pb, "progress", from, to);
                anim.setDuration(animateDuration);
                if (interpolator != null) {
                    anim.setInterpolator(interpolator);
                }
                anim.start();
            }
        }

        /* ---------------------- 内部工具 ---------------------- */

        /** 构建圆角矩形 drawable，endColor 非 0 时使用渐变 */
        private GradientDrawable buildShape(int fillColor, float radiusPx,
                                            int endColor, GradientDrawable.Orientation o) {
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.RECTANGLE);
            gd.setCornerRadius(radiusPx);

            if (endColor != 0) {
                gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                gd.setOrientation(o);
                gd.setColors(new int[]{fillColor, endColor});
            } else {
                gd.setColor(fillColor);
            }
            return gd;
        }

        private float dp(float v) {
            return v * pb.getResources().getDisplayMetrics().density + 0.5f;
        }
    }
}