package mr.x.zjx;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 直链下载工具：点击任意控件（含 ViewGroup 父控件）触发下载，表面实时渲染进度。
 * 9 种渲染样式：环形 / 底部条 / 纯文字 / 横向填充 / 纵向水位 / 中心扩展 / 波浪水位 / 斜纹滚动 / 气泡上升。
 * 渲染期间隐藏控件原内容：
 *   - TextView（非 EditText）：清空文本
 *   - ImageView：清空 drawable
 *   - ViewGroup：把全部直接子 View 设为 INVISIBLE（自动递归隐藏整棵子树）
 * 结束后恢复。支持真实暂停/恢复（Range 续传）。
 */
public final class DownloadUtil {

    public static final int   DEFAULT_COLOR       = 0xFF3F7EF7;
    public static final int   DEFAULT_TRACK_COLOR = 0x33FFFFFF;
    public static final int   DEFAULT_MASK_COLOR  = 0x88000000;
    public static final float DEFAULT_STROKE_DP   = 4f;
    public static final float DEFAULT_RADIUS_DP   = 10f;
    public static final float DEFAULT_FILL_ALPHA  = 0.72f;

    /* 进度渲染样式 */
    public static final int STYLE_RING           = 0;
    public static final int STYLE_BAR            = 1;
    public static final int STYLE_TEXT           = 2;
    public static final int STYLE_FILL           = 3;
    public static final int STYLE_FILL_VERTICAL  = 4;
    public static final int STYLE_FILL_CENTER    = 5;
    public static final int STYLE_WAVE           = 6;
    public static final int STYLE_STRIPES        = 7;
    public static final int STYLE_BUBBLES        = 8;

    private static final int STATE_IDLE     = 0;
    private static final int STATE_RUNNING  = 1;
    private static final int STATE_PAUSED   = 2;
    private static final int STATE_FINISHED = 3;

    static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<View, DownloadTask> TASKS = new WeakHashMap<>();

    private DownloadUtil() { }

    /* ========================= 对外入口 ========================= */
    public static Builder beautify(View v) { return beautify(v, DEFAULT_COLOR); }

    public static Builder beautify(View v, int color) {
        Builder b = new Builder(v);
        b.applyBeautify = true;
        b.color = color;
        return b;
    }

    public static Builder with(View v) { return new Builder(v); }

    public static void cancel(View v) {
        if (v == null) return;
        DownloadTask t = TASKS.get(v);
        if (t != null) t.cancel();
    }

    public static void pause(View v) {
        if (v == null) return;
        DownloadTask t = TASKS.get(v);
        if (t != null) t.pause();
    }

    public static void resume(View v) {
        if (v == null) return;
        DownloadTask t = TASKS.get(v);
        if (t != null) t.resume();
    }

    public static boolean isDownloading(View v) {
        return v != null && TASKS.containsKey(v);
    }

    public static boolean isPaused(View v) {
        if (v == null) return false;
        DownloadTask t = TASKS.get(v);
        return t != null && t.isPaused();
    }

    public interface OnDownloadListener { void onResult(File file, Exception e); }
    public interface OnProgressListener { void onProgress(int percent); }

    /* ========================= Builder ========================= */
    public static final class Builder {

        private final View view;
        private boolean applyBeautify = false;

        private String url      = null;
        private File   file     = null;
        private String fileName = null;
        private File   dir      = null;

        private int     color             = DEFAULT_COLOR;
        private int     pressedColor      = 0;
        private int     textColor         = 0;
        private int     progressTextColor = Color.WHITE;
        private float   radiusDp          = DEFAULT_RADIUS_DP;
        private boolean bold              = false;

        private int   style      = STYLE_RING;
        private int   trackColor = DEFAULT_TRACK_COLOR;
        private int   maskColor  = DEFAULT_MASK_COLOR;
        private float strokeDp   = DEFAULT_STROKE_DP;
        private float fillAlpha  = DEFAULT_FILL_ALPHA;

        private OnDownloadListener successListener  = null;
        private OnDownloadListener errorListener    = null;
        private OnProgressListener progressListener = null;

        private Builder(View v) { this.view = v; }

        public Builder url(String url) { this.url = url; return this; }
        public Builder file(File file) { this.file = file; return this; }
        public Builder fileName(String name) { this.fileName = name; return this; }
        public Builder dir(File d) { this.dir = d; return this; }

        public Builder color(int color) { this.color = color; return this; }
        public Builder pressedColor(int color) { this.pressedColor = color; return this; }
        public Builder textColor(int color) { this.textColor = color; return this; }
        public Builder progressTextColor(int color) { this.progressTextColor = color; return this; }
        public Builder radius(float radiusDp) { this.radiusDp = radiusDp; return this; }
        public Builder bold(boolean bold) { this.bold = bold; return this; }

        public Builder style(int style) { this.style = style; return this; }
        public Builder trackColor(int color) { this.trackColor = color; return this; }
        public Builder maskColor(int color) { this.maskColor = color; return this; }
        public Builder strokeWidth(float dp) { this.strokeDp = dp; return this; }
        public Builder fillAlpha(float alpha) { this.fillAlpha = alpha; return this; }

        public Builder onSuccess(OnDownloadListener listener) { this.successListener = listener; return this; }
        public Builder onError(OnDownloadListener listener) { this.errorListener = listener; return this; }
        public Builder onProgress(OnProgressListener listener) { this.progressListener = listener; return this; }

        public void apply() {
            if (view == null) return;
            if (applyBeautify) applyBeautifyStyle();
            view.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    DownloadTask existing = TASKS.get(view);
                    if (existing == null) startDownload();
                    else existing.toggle();
                }
            });
            view.setClickable(true);
        }

        private void applyBeautifyStyle() {
            int pressed = pressedColor != 0 ? pressedColor : darken(color, 0.82f);
            StateListDrawable sld = new StateListDrawable();
            sld.addState(new int[]{ -android.R.attr.state_enabled }, buildShape(0xFFBFC6D1));
            sld.addState(new int[]{ android.R.attr.state_pressed,
                    android.R.attr.state_enabled }, buildShape(pressed));
            sld.addState(new int[]{}, buildShape(color));

            int pl = view.getPaddingLeft();
            int pt = view.getPaddingTop();
            int pr = view.getPaddingRight();
            int pb = view.getPaddingBottom();
            view.setBackground(sld);
            view.setPadding(pl, pt, pr, pb);

            if (view instanceof TextView) {
                int base = textColor != 0 ? textColor : Color.WHITE;
                TextView tv = (TextView) view;
                tv.setTextColor(new ColorStateList(
                        new int[][]{
                                new int[]{ -android.R.attr.state_enabled },
                                new int[]{}
                        },
                        new int[]{ withAlpha(base, 0.45f), base }));
                if (bold) tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
            }
        }

        private GradientDrawable buildShape(int fillColor) {
            GradientDrawable d = new GradientDrawable();
            d.setColor(fillColor);
            d.setCornerRadius(dp(radiusDp, view.getContext()));
            return d;
        }

        private void startDownload() {
            if (url == null || url.length() == 0) return;
            if (TASKS.containsKey(view)) return;
            if (!view.isEnabled()) return;
            DownloadTask task = new DownloadTask(this);
            TASKS.put(view, task);
            task.start();
        }
    }

    /* ========================= 下载任务 ========================= */
    private static final class DownloadTask implements Runnable {

        private final WeakReference<View> viewRef;
        private final String url;
        private final File   file;
        private final String fileName;
        private final File   dir;
        private final int    style;
        private final int    color;
        private final int    trackColor;
        private final int    maskColor;
        private final int    progressTextColor;
        private final float  strokeDp;
        private final float  fillAlpha;

        private final OnDownloadListener successListener;
        private final OnDownloadListener errorListener;
        private final OnProgressListener progressListener;

        private volatile int    state      = STATE_IDLE;
        private volatile boolean canceled  = false;
        private volatile long   downloaded = 0L;
        private volatile long   total      = -1L;
        private volatile HttpURLConnection activeConn = null;
        private volatile Thread currentThread = null;

        private File target = null;
        private ProgressOverlayDrawable overlay;
        private boolean useForeground = false;

        /* 隐藏内容保存 */
        private CharSequence savedText  = null;
        private Drawable     savedImage = null;
        private List<HiddenChild> hiddenChildren = null;

        private static final class HiddenChild {
            final View view;
            final int  visibility;
            HiddenChild(View v, int vis) { this.view = v; this.visibility = vis; }
        }

        private final View.OnLayoutChangeListener layoutListener =
                new View.OnLayoutChangeListener() {
                    @Override public void onLayoutChange(View v, int l, int t, int r, int b,
                                                         int ol, int ot, int or, int ob) {
                        if (overlay != null && r - l > 0 && b - t > 0) {
                            overlay.setBounds(0, 0, r - l, b - t);
                            v.postInvalidateOnAnimation();
                        }
                    }
                };

        DownloadTask(Builder b) {
            this.viewRef           = new WeakReference<>(b.view);
            this.url               = b.url;
            this.file              = b.file;
            this.fileName          = b.fileName;
            this.dir               = b.dir;
            this.style             = b.style;
            this.color             = b.color;
            this.trackColor        = b.trackColor;
            this.maskColor         = b.maskColor;
            this.progressTextColor = b.progressTextColor;
            this.strokeDp          = b.strokeDp;
            this.fillAlpha         = b.fillAlpha;
            this.successListener   = b.successListener;
            this.errorListener     = b.errorListener;
            this.progressListener  = b.progressListener;
        }

        void start() {
            View v = viewRef.get();
            if (v == null || state != STATE_IDLE) return;
            state = STATE_RUNNING;

            overlay = new ProgressOverlayDrawable(v.getContext(), style, color,
                    trackColor, maskColor, progressTextColor, strokeDp, fillAlpha);

            if (v.getWidth() > 0 && v.getHeight() > 0) {
                overlay.setBounds(0, 0, v.getWidth(), v.getHeight());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                useForeground = true;
                v.setForeground(overlay);
            } else {
                v.getOverlay().add(overlay);
            }
            v.addOnLayoutChangeListener(layoutListener);
            hideContent();
            v.postInvalidateOnAnimation();
            launch();
        }

        void toggle() {
            if (state == STATE_PAUSED) resume();
            else if (state == STATE_RUNNING) pause();
        }

        void pause() {
            if (state != STATE_RUNNING) return;
            state = STATE_PAUSED;
            HttpURLConnection c = activeConn;
            if (c != null) { try { c.disconnect(); } catch (Exception ignored) { } }
        }

        void resume() {
            if (state != STATE_PAUSED) return;
            state = STATE_RUNNING;
            launch();
        }

        void cancel() {
            canceled = true;
            state = STATE_FINISHED;
            HttpURLConnection c = activeConn;
            if (c != null) { try { c.disconnect(); } catch (Exception ignored) { } }
            Thread t = currentThread;
            if (t == null || !t.isAlive()) {
                MAIN.post(new Runnable() {
                    @Override public void run() {
                        File f = target;
                        if (f != null && f.exists()) f.delete();
                        postFailed(new IOException("已取消"));
                    }
                });
            }
        }

        boolean isPaused() { return state == STATE_PAUSED; }

        private void launch() {
            Thread t = new Thread(this, "download-" + System.currentTimeMillis());
            currentThread = t;
            t.start();
        }

        /* ---------- 隐藏控件内容（含父控件的全部子 View） ---------- */
        private void hideContent() {
            View v = viewRef.get();
            if (v == null) return;

            if (v instanceof TextView && !(v instanceof EditText)) {
                TextView tv = (TextView) v;
                savedText = tv.getText();
                tv.setText(null);
            }

            if (v instanceof ImageView) {
                ImageView iv = (ImageView) v;
                savedImage = iv.getDrawable();
                iv.setImageDrawable(null);
            }

            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                int count = vg.getChildCount();
                if (count > 0) {
                    hiddenChildren = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        View child = vg.getChildAt(i);
                        hiddenChildren.add(new HiddenChild(child, child.getVisibility()));
                        child.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }

        private void restoreContent() {
            View v = viewRef.get();
            if (v != null) {
                if (v instanceof TextView && !(v instanceof EditText) && savedText != null) {
                    ((TextView) v).setText(savedText);
                }
                if (v instanceof ImageView && savedImage != null) {
                    ImageView iv = (ImageView) v;
                    if (iv.getDrawable() == null) iv.setImageDrawable(savedImage);
                }
            }
            savedText  = null;
            savedImage = null;

            if (hiddenChildren != null) {
                for (int i = 0; i < hiddenChildren.size(); i++) {
                    HiddenChild hc = hiddenChildren.get(i);
                    if (hc.view != null) hc.view.setVisibility(hc.visibility);
                }
                hiddenChildren.clear();
                hiddenChildren = null;
            }
        }

        @Override public void run() {
            HttpURLConnection conn = null;
            InputStream is = null;
            FileOutputStream fos = null;

            try {
                View v = viewRef.get();
                if (v == null) return;
                Context ctx = v.getContext();

                if (target == null) {
                    if (file != null) {
                        target = file;
                    } else {
                        String name = fileName;
                        if (name == null || name.length() == 0) {
                            name = resolveFileName(url, null, null);
                        }
                        File baseDir = dir;
                        if (baseDir == null) {
                            baseDir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                        }
                        if (baseDir == null) baseDir = ctx.getFilesDir();
                        target = uniqueFile(baseDir, name);
                    }
                    File parent = target.getParentFile();
                    if (parent != null) parent.mkdirs();
                }

                conn = openWithRedirects(url, downloaded);
                activeConn = conn;
                int code = conn.getResponseCode();

                if (canceled || state == STATE_PAUSED) {
                    /* skip */
                } else if (downloaded > 0 && code == 416) {
                    conn.disconnect();
                    activeConn = null;
                    downloaded = 0L;
                    if (target.exists()) target.delete();
                    conn = openWithRedirects(url, 0L);
                    activeConn = conn;
                    code = conn.getResponseCode();
                }

                if (!canceled && state != STATE_PAUSED) {
                    if (code == 206 && downloaded > 0) {
                        total = parseContentRangeTotal(
                                conn.getHeaderField("Content-Range"),
                                downloaded + safeContentLength(conn));
                        fos = new FileOutputStream(target, true);
                    } else if (code == 200) {
                        downloaded = 0L;
                        total = safeContentLength(conn);
                        fos = new FileOutputStream(target, false);
                    } else if (code >= 200 && code < 300) {
                        total = safeContentLength(conn);
                        fos = new FileOutputStream(target, false);
                    } else {
                        throw new IOException("HTTP " + code);
                    }

                    /* 连接后才拿到真实文件名，若首次是 URL 兜底名则换名并清理空壳 */
                    if (file == null && (fileName == null || fileName.length() == 0)
                            && downloaded == 0L) {
                        String resolved = resolveFileName(
                                conn.getURL().toString(),
                                conn.getHeaderField("Content-Disposition"),
                                conn.getContentType());
                        File better = uniqueFile(target.getParentFile(), resolved);
                        if (!better.getName().equals(target.getName())) {
                            File oldTarget = target;
                            target = better;
                            File p = target.getParentFile();
                            if (p != null) p.mkdirs();
                            try { fos.close(); } catch (Exception ignored) { }

                            // 删除按 URL 兜底名创建出来的空文件，避免留 0KB 残骸
                            if (oldTarget.exists() && oldTarget.length() == 0) {
                                //noinspection ResultOfMethodCallIgnored
                                oldTarget.delete();
                            }

                            fos = new FileOutputStream(target, false);
                        }
                    }

                    is = conn.getInputStream();
                    byte[] buf = new byte[8192];
                    int len;
                    long lastUpdate = 0L;
                    while ((len = is.read(buf)) > 0) {
                        if (canceled || state == STATE_PAUSED) break;
                        fos.write(buf, 0, len);
                        downloaded += len;
                        long now = System.currentTimeMillis();
                        long interval = total > 0 ? 80L : 250L;
                        if (now - lastUpdate > interval) {
                            postProgress(downloaded, total);
                            lastUpdate = now;
                        }
                    }
                    fos.flush();
                }
            } catch (Exception e) {
                if (!(canceled || state == STATE_PAUSED)) {
                    postFailed(e);
                    return;
                }
            } finally {
                closeQuietly(is);
                closeQuietly(fos);
                if (conn != null) { try { conn.disconnect(); } catch (Exception ignored) { } }
                if (Thread.currentThread() == currentThread) activeConn = null;
            }

            if (Thread.currentThread() != currentThread) return;

            if (canceled) {
                if (target != null && target.exists()) target.delete();
                postFailed(new IOException("已取消"));
            } else if (state == STATE_PAUSED) {
                postPaused();
            } else {
                postProgress(downloaded, total > 0 ? total : downloaded);
                postSuccess(target);
            }
        }

        void postProgress(final long read, final long ttl) {
            MAIN.post(new Runnable() {
                @Override public void run() {
                    if (overlay != null) overlay.setProgress(read, ttl);
                    View v = viewRef.get();
                    if (v != null) v.postInvalidateOnAnimation();
                    if (progressListener != null && ttl > 0) {
                        int p = (int) Math.min(100, read * 100 / ttl);
                        if (p < 0) p = 0;
                        progressListener.onProgress(p);
                    }
                }
            });
        }

        void postPaused() {
            MAIN.post(new Runnable() {
                @Override public void run() {
                    if (overlay != null) overlay.setPaused();
                    View v = viewRef.get();
                    if (v != null) v.postInvalidateOnAnimation();
                }
            });
        }

        void postSuccess(final File f) {
            MAIN.post(new Runnable() {
                @Override public void run() {
                    if (overlay != null) overlay.setSuccess();
                    View v = viewRef.get();
                    if (v != null) v.postInvalidateOnAnimation();
                    if (successListener != null) successListener.onResult(f, null);
                    if (v != null) TASKS.remove(v);
                    MAIN.postDelayed(new Runnable() {
                        @Override public void run() { removeOverlay(); }
                    }, 1200);
                }
            });
        }

        void postFailed(final Exception e) {
            MAIN.post(new Runnable() {
                @Override public void run() {
                    if (overlay != null) overlay.setFail();
                    View v = viewRef.get();
                    if (v != null) v.postInvalidateOnAnimation();
                    if (errorListener != null) errorListener.onResult(null, e);
                    if (v != null) TASKS.remove(v);
                    MAIN.postDelayed(new Runnable() {
                        @Override public void run() { removeOverlay(); }
                    }, 1200);
                }
            });
        }

        void removeOverlay() {
            View v = viewRef.get();
            if (v != null) {
                v.removeOnLayoutChangeListener(layoutListener);
                if (useForeground) {
                    if (v.getForeground() == overlay) v.setForeground(null);
                } else if (overlay != null) {
                    v.getOverlay().remove(overlay);
                }
                v.postInvalidateOnAnimation();
            }
            if (overlay != null) overlay.stopAnimation();
            overlay = null;
            restoreContent();
        }

        static void closeQuietly(Closeable c) {
            if (c != null) { try { c.close(); } catch (Exception ignored) { } }
        }

        static long safeContentLength(HttpURLConnection c) {
            long len = -1;
            String h = c.getHeaderField("Content-Length");
            if (h != null) {
                try { len = Long.parseLong(h.trim()); } catch (Exception ignored) { }
            }
            return len;
        }
    }

    /* ========================= 进度覆盖层 ========================= */
    private static final class ProgressOverlayDrawable extends Drawable {

        private static final int DS_PROGRESS = 0;
        private static final int DS_SUCCESS  = 1;
        private static final int DS_FAIL     = 2;
        private static final int DS_PAUSED   = 3;

        private static final int BUBBLE_COUNT = 9;

        private final int   style;
        private final int   color;
        private final int   maskColor;
        private final float density;
        private final float strokeWidthPx;
        private final float cornerRadiusPx;
        private final float textSizePx;
        private final float sidePaddingPx;
        private final float fillAlpha;

        private final Paint maskPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringProgPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint barTrackPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint barProgPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stripePaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bubblePaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bubbleHiPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

        private int  progress   = 0;
        private long downloaded = 0L;
        private long total      = -1L;
        private int  drawState  = DS_PROGRESS;

        private boolean animationRunning = false;
        private float   waveOffset       = 0f;
        private float   stripeOffset     = 0f;
        private final List<Bubble> bubbleList = new ArrayList<>();

        private final Runnable animTick = new Runnable() {
            @Override public void run() {
                boolean changed = false;
                if (style == STYLE_WAVE) {
                    waveOffset = (waveOffset + 0.04f) % 1f;
                    changed = true;
                } else if (style == STYLE_STRIPES) {
                    stripeOffset = (stripeOffset + 0.05f) % 1f;
                    changed = true;
                } else if (style == STYLE_BUBBLES) {
                    updateBubbles();
                    changed = true;
                }
                if (changed) invalidateSelf();
                if (animationRunning && drawState == DS_PROGRESS) {
                    MAIN.postDelayed(this, 33);
                } else {
                    animationRunning = false;
                }
            }
        };

        ProgressOverlayDrawable(Context ctx, int style, int color,
                                int trackColor, int maskColor,
                                int textColor, float strokeDp, float fillAlpha) {
            this.style     = style;
            this.color     = color;
            this.maskColor = maskColor;
            this.fillAlpha = fillAlpha;

            float d = ctx.getResources().getDisplayMetrics().density;
            this.density        = d;
            this.strokeWidthPx  = strokeDp * d;
            this.cornerRadiusPx = 10f * d;
            this.textSizePx     = 16f * d;
            this.sidePaddingPx  = 8f * d;

            maskPaint.setColor(maskColor);
            maskPaint.setStyle(Paint.Style.FILL);

            ringTrackPaint.setColor(trackColor);
            ringTrackPaint.setStyle(Paint.Style.STROKE);
            ringTrackPaint.setStrokeWidth(strokeWidthPx);

            ringProgPaint.setColor(color);
            ringProgPaint.setStyle(Paint.Style.STROKE);
            ringProgPaint.setStrokeWidth(strokeWidthPx);
            ringProgPaint.setStrokeCap(Paint.Cap.ROUND);

            barTrackPaint.setColor(trackColor);
            barTrackPaint.setStyle(Paint.Style.FILL);

            barProgPaint.setColor(color);
            barProgPaint.setStyle(Paint.Style.FILL);

            fillTrackPaint.setColor(trackColor);
            fillTrackPaint.setStyle(Paint.Style.FILL);

            fillPaint.setStyle(Paint.Style.FILL);
            stripePaint.setStyle(Paint.Style.STROKE);
            stripePaint.setStrokeCap(Paint.Cap.BUTT);
            bubblePaint.setStyle(Paint.Style.FILL);
            bubbleHiPaint.setStyle(Paint.Style.FILL);

            textPaint.setColor(textColor);
            textPaint.setTextSize(textSizePx);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setTextAlign(Paint.Align.CENTER);
        }

        void setProgress(long read, long ttl) {
            this.drawState  = DS_PROGRESS;
            this.downloaded = read;
            this.total      = ttl;
            if (ttl > 0) {
                int p = (int) (read * 100 / ttl);
                if (p > 100) p = 100;
                if (p < 0)   p = 0;
                this.progress = p;
            } else {
                this.progress = -1;
            }
            ringProgPaint.setColor(color);
            barProgPaint.setColor(color);
            if (isAnimatedStyle()) startAnimation();
            invalidateSelf();
        }

        void setPaused() {
            this.drawState = DS_PAUSED;
            stopAnimation();
            invalidateSelf();
        }

        void setSuccess() {
            this.progress  = 100;
            this.drawState = DS_SUCCESS;
            ringProgPaint.setColor(0xFF10B981);
            barProgPaint.setColor(0xFF10B981);
            stopAnimation();
            invalidateSelf();
        }

        void setFail() {
            this.drawState = DS_FAIL;
            ringProgPaint.setColor(0xFFEF4444);
            barProgPaint.setColor(0xFFEF4444);
            stopAnimation();
            invalidateSelf();
        }

        void stopAnimation() {
            animationRunning = false;
            MAIN.removeCallbacks(animTick);
        }

        private void startAnimation() {
            if (animationRunning) return;
            animationRunning = true;
            MAIN.postDelayed(animTick, 33);
        }

        private boolean isAnimatedStyle() {
            return style == STYLE_WAVE || style == STYLE_STRIPES || style == STYLE_BUBBLES;
        }

        private int computeFillColor() {
            if (drawState == DS_SUCCESS) return withAlpha(0xFF10B981, Math.min(fillAlpha + 0.1f, 1f));
            if (drawState == DS_FAIL)    return withAlpha(0xFFEF4444, Math.min(fillAlpha + 0.1f, 1f));
            return withAlpha(color, fillAlpha);
        }

        @Override public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.width() <= 0 || bounds.height() <= 0) return;

            if (Color.alpha(maskColor) > 0) {
                RectF r = new RectF(bounds);
                canvas.drawRoundRect(r, cornerRadiusPx, cornerRadiusPx, maskPaint);
            }

            fillPaint.setColor(computeFillColor());

            switch (style) {
                case STYLE_BAR:           drawBar(canvas, bounds);          break;
                case STYLE_TEXT:          drawText(canvas, bounds);         break;
                case STYLE_FILL:          drawFill(canvas, bounds);         break;
                case STYLE_FILL_VERTICAL: drawFillVertical(canvas, bounds); break;
                case STYLE_FILL_CENTER:   drawFillCenter(canvas, bounds);   break;
                case STYLE_WAVE:          drawWave(canvas, bounds);         break;
                case STYLE_STRIPES:       drawStripes(canvas, bounds);      break;
                case STYLE_BUBBLES:       drawBubbles(canvas, bounds);      break;
                case STYLE_RING:
                default:                  drawRing(canvas, bounds);         break;
            }
        }

        private String buildLabel(boolean compact) {
            if (drawState == DS_SUCCESS) return compact ? "✓" : "✓ 完成";
            if (drawState == DS_FAIL)    return compact ? "✗" : "✗ 失败";
            if (drawState == DS_PAUSED) {
                String base = progress >= 0 ? (progress + "%") : formatBytes(downloaded);
                return compact ? ("‖ " + base) : ("已暂停 " + base);
            }
            if (progress >= 0) return progress + "%";
            return formatBytes(downloaded);
        }

        private void drawRing(Canvas canvas, Rect bounds) {
            float cx = bounds.exactCenterX();
            float cy = bounds.exactCenterY();
            float radius = Math.min(bounds.width(), bounds.height()) / 2f
                    - strokeWidthPx - sidePaddingPx;
            if (radius < strokeWidthPx * 2) radius = strokeWidthPx * 2;

            canvas.drawCircle(cx, cy, radius, ringTrackPaint);
            RectF arcRect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
            if (drawState == DS_PROGRESS || drawState == DS_PAUSED) {
                if (progress >= 0) {
                    float sweep = 360f * progress / 100f;
                    canvas.drawArc(arcRect, -90f, sweep, false, ringProgPaint);
                } else {
                    canvas.drawArc(arcRect, -90f, 60f, false, ringProgPaint);
                }
            } else {
                canvas.drawCircle(cx, cy, radius, ringProgPaint);
            }

            String label;
            float size;
            if (drawState == DS_SUCCESS || drawState == DS_FAIL) {
                label = buildLabel(true); size = textSizePx * 1.3f;
            } else if (progress >= 0) {
                label = buildLabel(true); size = textSizePx;
            } else {
                label = buildLabel(true); size = textSizePx * 0.75f;
            }
            textPaint.setTextSize(size);
            float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
            canvas.drawText(label, cx, textY, textPaint);
        }

        private void drawBar(Canvas canvas, Rect bounds) {
            float barHeight = Math.max(strokeWidthPx, 4f);
            float left  = cornerRadiusPx;
            float right = bounds.right - cornerRadiusPx;
            float top   = bounds.bottom - barHeight - sidePaddingPx;

            RectF trackRect = new RectF(left, top, right, top + barHeight);
            canvas.drawRoundRect(trackRect, barHeight / 2f, barHeight / 2f, barTrackPaint);

            if (drawState == DS_PROGRESS || drawState == DS_PAUSED) {
                if (progress >= 0) {
                    float progRight = left + (right - left) * progress / 100f;
                    if (progRight > left) {
                        RectF progRect = new RectF(left, top, progRight, top + barHeight);
                        canvas.drawRoundRect(progRect, barHeight / 2f, barHeight / 2f, barProgPaint);
                    }
                }
            } else {
                barProgPaint.setColor(drawState == DS_SUCCESS ? 0xFF10B981 : 0xFFEF4444);
                RectF progRect = new RectF(left, top, right, top + barHeight);
                canvas.drawRoundRect(progRect, barHeight / 2f, barHeight / 2f, barProgPaint);
            }
            drawCenteredLabel(canvas, bounds);
        }

        private void drawText(Canvas canvas, Rect bounds) {
            String label = buildLabel(false);
            float size = (progress < 0 && drawState == DS_PROGRESS) ? textSizePx * 0.8f : textSizePx;
            textPaint.setTextSize(size);
            float cx = bounds.exactCenterX();
            float cy = bounds.exactCenterY();
            float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
            canvas.drawText(label, cx, textY, textPaint);
        }

        private void drawFill(Canvas canvas, Rect bounds) {
            RectF trackRect = new RectF(bounds);
            canvas.drawRoundRect(trackRect, cornerRadiusPx, cornerRadiusPx, fillTrackPaint);
            if (drawState == DS_PROGRESS || drawState == DS_PAUSED) {
                if (progress > 0) {
                    float w = bounds.width() * progress / 100f;
                    RectF fillRect = new RectF(bounds.left, bounds.top, bounds.left + w, bounds.bottom);
                    float r = Math.min(cornerRadiusPx, w / 2f);
                    canvas.drawRoundRect(fillRect, r, r, fillPaint);
                }
            } else {
                canvas.drawRoundRect(trackRect, cornerRadiusPx, cornerRadiusPx, fillPaint);
            }
            drawCenteredLabel(canvas, bounds);
        }

        private void drawFillVertical(Canvas canvas, Rect bounds) {
            RectF trackRect = new RectF(bounds);
            canvas.drawRoundRect(trackRect, cornerRadiusPx, cornerRadiusPx, fillTrackPaint);
            if (drawState == DS_PROGRESS || drawState == DS_PAUSED) {
                if (progress > 0) {
                    float h = bounds.height() * progress / 100f;
                    RectF fillRect = new RectF(bounds.left, bounds.bottom - h, bounds.right, bounds.bottom);
                    float r = Math.min(cornerRadiusPx, h / 2f);
                    canvas.drawRoundRect(fillRect, r, r, fillPaint);
                }
            } else {
                canvas.drawRoundRect(trackRect, cornerRadiusPx, cornerRadiusPx, fillPaint);
            }
            drawCenteredLabel(canvas, bounds);
        }

        private void drawFillCenter(Canvas canvas, Rect bounds) {
            RectF trackRect = new RectF(bounds);
            canvas.drawRoundRect(trackRect, cornerRadiusPx, cornerRadiusPx, fillTrackPaint);
            if (drawState == DS_PROGRESS || drawState == DS_PAUSED) {
                if (progress > 0) {
                    float cx = bounds.exactCenterX();
                    float halfW = bounds.width() / 2f * progress / 100f;
                    RectF fillRect = new RectF(cx - halfW, bounds.top, cx + halfW, bounds.bottom);
                    float r = Math.min(cornerRadiusPx, halfW);
                    canvas.drawRoundRect(fillRect, r, r, fillPaint);
                }
            } else {
                canvas.drawRoundRect(trackRect, cornerRadiusPx, cornerRadiusPx, fillPaint);
            }
            drawCenteredLabel(canvas, bounds);
        }

        private void drawWave(Canvas canvas, Rect bounds) {
            RectF trackRect = new RectF(bounds);
            canvas.drawRoundRect(trackRect, cornerRadiusPx, cornerRadiusPx, fillTrackPaint);

            if (drawState == DS_PROGRESS || drawState == DS_PAUSED) {
                if (progress > 0) {
                    float h = bounds.height() * progress / 100f;
                    float waterTop = bounds.bottom - h;
                    float amp = Math.min(h * 0.22f, 6f * density);
                    if (amp < 1.5f) amp = 0f;

                    Path path = new Path();
                    path.moveTo(bounds.left, bounds.bottom);

                    if (amp > 0 && drawState == DS_PROGRESS) {
                        float waveLen = Math.max(bounds.width() / 2.5f, 24f * density);
                        float segW = waveLen / 2f;
                        float phase = waveOffset * waveLen * 2f;

                        path.lineTo(bounds.left, waterTop);
                        float x = bounds.left - phase;
                        boolean up = true;
                        while (x < bounds.right) {
                            float startX = Math.max(x, bounds.left);
                            float endX = Math.min(x + segW, bounds.right);
                            if (endX > startX) {
                                float midX = (startX + endX) / 2f;
                                float peakY = up ? (waterTop - amp) : (waterTop + amp);
                                path.quadTo(midX, peakY, endX, waterTop);
                            }
                            x += segW;
                            up = !up;
                        }
                    } else {
                        path.lineTo(bounds.left, waterTop);
                        path.lineTo(bounds.right, waterTop);
                    }

                    path.lineTo(bounds.right, bounds.bottom);
                    path.close();

                    canvas.save();
                    Path clip = new Path();
                    clip.addRoundRect(trackRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW);
                    canvas.clipPath(clip);
                    canvas.drawPath(path, fillPaint);
                    canvas.restore();
                }
            } else {
                canvas.drawRoundRect(trackRect, cornerRadiusPx, cornerRadiusPx, fillPaint);
            }
            drawCenteredLabel(canvas, bounds);
        }

        private void drawStripes(Canvas canvas, Rect bounds) {
            RectF trackRect = new RectF(bounds);
            canvas.drawRoundRect(trackRect, cornerRadiusPx, cornerRadiusPx, fillTrackPaint);

            if (drawState == DS_PROGRESS || drawState == DS_PAUSED) {
                if (progress > 0) {
                    float w = bounds.width() * progress / 100f;
                    RectF fillRect = new RectF(bounds.left, bounds.top, bounds.left + w, bounds.bottom);
                    float r = Math.min(cornerRadiusPx, w / 2f);

                    canvas.save();
                    Path clip = new Path();
                    clip.addRoundRect(fillRect, r, r, Path.Direction.CW);
                    canvas.clipPath(clip);
                    canvas.drawRect(fillRect, fillPaint);

                    float diagonal = bounds.height();
                    float period   = 20f * density;
                    float sw       = 8f  * density;
                    stripePaint.setColor(0x44FFFFFF);
                    stripePaint.setStrokeWidth(sw);

                    float offset = stripeOffset * period;
                    float start  = bounds.left - diagonal - period + offset;
                    float end    = bounds.right + period;
                    for (float x = start; x < end; x += period) {
                        canvas.drawLine(x, bounds.bottom, x + diagonal, bounds.top, stripePaint);
                    }
                    canvas.restore();
                }
            } else {
                canvas.drawRoundRect(trackRect, cornerRadiusPx, cornerRadiusPx, fillPaint);
            }
            drawCenteredLabel(canvas, bounds);
        }

        private void drawBubbles(Canvas canvas, Rect bounds) {
            RectF trackRect = new RectF(bounds);
            canvas.drawRoundRect(trackRect, cornerRadiusPx, cornerRadiusPx, fillTrackPaint);

            if (drawState == DS_PROGRESS || drawState == DS_PAUSED) {
                if (progress > 0) {
                    float h = bounds.height() * progress / 100f;
                    float waterTop = bounds.bottom - h;
                    RectF fillRect = new RectF(bounds.left, waterTop, bounds.right, bounds.bottom);
                    float r = Math.min(cornerRadiusPx, h / 2f);

                    canvas.save();
                    Path clip = new Path();
                    clip.addRoundRect(fillRect, r, r, Path.Direction.CW);
                    canvas.clipPath(clip);
                    canvas.drawRect(fillRect, fillPaint);

                    if (bubbleList.isEmpty()) initBubbles();
                    bubblePaint.setColor(0x55FFFFFF);
                    bubbleHiPaint.setColor(0x99FFFFFF);

                    for (int i = 0; i < bubbleList.size(); i++) {
                        Bubble b = bubbleList.get(i);
                        if (b.y + b.r < waterTop || b.y - b.r > bounds.bottom) continue;
                        canvas.drawCircle(b.x, b.y, b.r, bubblePaint);
                        canvas.drawCircle(b.x - b.r * 0.32f, b.y - b.r * 0.32f,
                                b.r * 0.34f, bubbleHiPaint);
                    }
                    canvas.restore();
                }
            } else {
                canvas.drawRoundRect(trackRect, cornerRadiusPx, cornerRadiusPx, fillPaint);
            }
            drawCenteredLabel(canvas, bounds);
        }

        private static final class Bubble { float x, y, r, speed; }

        private void initBubbles() {
            Rect b = getBounds();
            if (b.width() <= 0 || b.height() <= 0) return;
            bubbleList.clear();
            for (int i = 0; i < BUBBLE_COUNT; i++) {
                Bubble bub = new Bubble();
                bub.x     = b.left + (float) Math.random() * b.width();
                bub.y     = b.top + (float) Math.random() * b.height();
                bub.r     = (2.5f + (float) Math.random() * 3.5f) * density;
                bub.speed = (1.5f + (float) Math.random() * 2.0f) * density;
                bubbleList.add(bub);
            }
        }

        private void updateBubbles() {
            Rect b = getBounds();
            if (b.width() <= 0 || b.height() <= 0) return;
            if (bubbleList.isEmpty()) { initBubbles(); return; }
            for (int i = 0; i < bubbleList.size(); i++) {
                Bubble bub = bubbleList.get(i);
                bub.y -= bub.speed;
                if (bub.y + bub.r < b.top) {
                    bub.y     = b.bottom + (float) Math.random() * b.height() * 0.3f;
                    bub.x     = b.left + (float) Math.random() * b.width();
                    bub.r     = (2.5f + (float) Math.random() * 3.5f) * density;
                    bub.speed = (1.5f + (float) Math.random() * 2.0f) * density;
                }
            }
        }

        private void drawCenteredLabel(Canvas canvas, Rect bounds) {
            String label = buildLabel(true);
            float size = textSizePx;
            if (progress < 0 && drawState == DS_PROGRESS) size = textSizePx * 0.75f;
            textPaint.setTextSize(size);

            float cx = bounds.exactCenterX();
            float cy = bounds.exactCenterY();
            float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
            canvas.drawText(label, cx, textY, textPaint);
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter colorFilter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /* ========================= 工具 ========================= */
    private static int dp(float value, Context ctx) {
        return Math.round(value * ctx.getResources().getDisplayMetrics().density);
    }

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

    private static String formatBytes(long bytes) {
        if (bytes < 0) bytes = 0;
        if (bytes < 1024L) return bytes + "B";
        if (bytes < 1024L * 1024L)
            return String.format(Locale.US, "%.1fKB", bytes / 1024.0);
        if (bytes < 1024L * 1024L * 1024L)
            return String.format(Locale.US, "%.1fMB", bytes / 1024.0 / 1024.0);
        return String.format(Locale.US, "%.2fGB", bytes / 1024.0 / 1024.0 / 1024.0);
    }

    private static long parseContentRangeTotal(String cr, long fallback) {
        if (cr != null) {
            int slash = cr.lastIndexOf('/');
            if (slash > 0) {
                String t = cr.substring(slash + 1).trim();
                if (!"*".equals(t)) {
                    try { return Long.parseLong(t); } catch (Exception ignored) { }
                }
            }
        }
        return fallback;
    }

    private static HttpURLConnection openWithRedirects(String urlStr, long rangeStart) throws IOException {
        final int MAX = 6;
        String current = urlStr;
        for (int i = 0; i <= MAX; i++) {
            URL u = new URL(current);
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setRequestMethod("GET");
            c.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36");
            c.setRequestProperty("Accept", "*/*");
            if (rangeStart > 0) {
                c.setRequestProperty("Range", "bytes=" + rangeStart + "-");
            }
            c.connect();

            int code = c.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String location = c.getHeaderField("Location");
                c.disconnect();
                if (location == null || location.length() == 0) {
                    throw new IOException("Redirect without Location");
                }
                if (!location.startsWith("http://") && !location.startsWith("https://")) {
                    URL base = new URL(current);
                    String prefix = base.getProtocol() + "://" + base.getHost();
                    if (base.getPort() != -1) prefix += ":" + base.getPort();
                    if (location.startsWith("/")) {
                        location = prefix + location;
                    } else {
                        String path = base.getPath() == null ? "/" : base.getPath();
                        int slash = path.lastIndexOf('/');
                        location = prefix
                                + (slash >= 0 ? path.substring(0, slash + 1) : "/")
                                + location;
                    }
                }
                current = location;
                continue;
            }
            return c;
        }
        throw new IOException("Too many redirects");
    }

    private static String resolveFileName(String finalUrl, String cd, String ct) {
        String name = parseContentDisposition(cd);
        if (name != null && name.length() > 0) {
            name = sanitize(name);
            if (name.length() > 0) return name;
        }
        if (finalUrl != null) {
            try {
                URL u = new URL(finalUrl);
                String path = u.getPath();
                if (path != null && path.length() > 0) {
                    int idx = path.lastIndexOf('/');
                    String last = idx >= 0 ? path.substring(idx + 1) : path;
                    if (last.length() > 0) {
                        String decoded = urlDecode(last);
                        int q = decoded.indexOf('?');
                        if (q >= 0) decoded = decoded.substring(0, q);
                        decoded = sanitize(decoded);
                        if (decoded.length() > 0) {
                            if (hasExtension(decoded)) return decoded;
                            String ext = extFromMime(ct);
                            return decoded + (ext != null ? ext : "");
                        }
                    }
                }
            } catch (Exception ignored) { }
        }
        String ext = extFromMime(ct);
        return "download_" + System.currentTimeMillis() + (ext != null ? ext : "");
    }

    private static String parseContentDisposition(String cd) {
        if (cd == null) return null;
        try {
            int idx = cd.toLowerCase(Locale.US).indexOf("filename*=");
            if (idx >= 0) {
                String rest = cd.substring(idx + "filename*=".length()).trim();
                int semi = rest.indexOf(';');
                if (semi >= 0) rest = rest.substring(0, semi);
                rest = rest.trim();
                int dq = rest.indexOf("''");
                if (dq >= 0) {
                    String enc = rest.substring(dq + 2);
                    if (enc.startsWith("\"")) enc = enc.substring(1);
                    if (enc.endsWith("\""))   enc = enc.substring(0, enc.length() - 1);
                    return urlDecode(enc);
                }
            }
            idx = cd.toLowerCase(Locale.US).indexOf("filename=");
            if (idx >= 0) {
                String rest = cd.substring(idx + "filename=".length()).trim();
                int semi = rest.indexOf(';');
                if (semi >= 0) rest = rest.substring(0, semi);
                rest = rest.trim();
                if (rest.length() >= 2 && rest.startsWith("\"") && rest.endsWith("\"")) {
                    rest = rest.substring(1, rest.length() - 1);
                }
                return rest;
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s.replace("+", "%2B"), "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private static boolean hasExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 && dot < name.length() - 1 && name.length() - dot <= 8;
    }

    private static String extFromMime(String mime) {
        if (mime == null) return null;
        String m = mime.toLowerCase(Locale.US);
        int semi = m.indexOf(';');
        if (semi >= 0) m = m.substring(0, semi).trim();
        switch (m) {
            case "text/plain": return ".txt";
            case "text/html": return ".html";
            case "text/css": return ".css";
            case "text/javascript":
            case "application/javascript": return ".js";
            case "application/json": return ".json";
            case "application/xml":
            case "text/xml": return ".xml";
            case "application/pdf": return ".pdf";
            case "application/zip": return ".zip";
            case "application/x-rar-compressed":
            case "application/vnd.rar": return ".rar";
            case "application/x-7z-compressed": return ".7z";
            case "application/x-tar": return ".tar";
            case "application/gzip":
            case "application/x-gzip": return ".gz";
            case "image/jpeg": return ".jpg";
            case "image/png": return ".png";
            case "image/gif": return ".gif";
            case "image/webp": return ".webp";
            case "image/svg+xml": return ".svg";
            case "audio/mpeg": return ".mp3";
            case "audio/mp4":
            case "audio/x-m4a": return ".m4a";
            case "audio/wav":
            case "audio/x-wav": return ".wav";
            case "audio/ogg": return ".ogg";
            case "video/mp4": return ".mp4";
            case "video/x-matroska": return ".mkv";
            case "video/webm": return ".webm";
            case "video/quicktime": return ".mov";
            case "application/vnd.android.package-archive": return ".apk";
            case "application/vnd.ms-excel": return ".xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                return ".xlsx";
            case "application/msword": return ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return ".docx";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
                return ".pptx";
            case "application/octet-stream": return null;
        }
        int slash = m.indexOf('/');
        if (slash > 0) {
            String sub = m.substring(slash + 1).replaceAll("[^a-z0-9]", "");
            if (sub.length() > 0 && sub.length() <= 6) return "." + sub;
        }
        return null;
    }

    private static String sanitize(String name) {
        if (name == null) return "";
        name = name.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
        if (name.length() == 0) return name;
        if (name.length() > 120) {
            int dot = name.lastIndexOf('.');
            if (dot > 0 && name.length() - dot <= 10) {
                String ext = name.substring(dot);
                name = name.substring(0, 120 - ext.length()) + ext;
            } else {
                name = name.substring(0, 120);
            }
        }
        return name;
    }

    private static File uniqueFile(File dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) return f;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext  = dot > 0 ? name.substring(dot) : "";
        int i = 1;
        while (f.exists() && i < 10000) {
            f = new File(dir, base + "(" + i + ")" + ext);
            i++;
        }
        return f;
    }
}