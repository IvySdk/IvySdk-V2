package com.ivy.base.util;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.util.function.DoubleUnaryOperator;

public class VibrationUtil {

    private static VibrationUtil _instance = null;
    private Vibrator vibrator;

    public static VibrationUtil getInstance() {
        if (_instance == null) {
            synchronized (VibrationUtil.class) {
                if (_instance == null) {
                    _instance = new VibrationUtil();
                }
            }
        }
        return _instance;
    }

    private VibrationUtil() {
    }

    private void initVibrateService(Context context) {
        if (vibrator == null && context != null) {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }


    /**
     * 单次震动
     *
     * @param duration  震动时间，毫秒
     * @param amplitude 强度 0~255（API 26+可用），低版本无效果
     */
    public void vibrateOnce(Context context, long duration, int amplitude) {
        initVibrateService(context);
        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            amplitude = Math.max(1, Math.min(amplitude, 255)); // 保证在 1~255
            VibrationEffect effect = VibrationEffect.createOneShot(duration, amplitude);
            vibrator.vibrate(effect);
        } else {
            vibrator.vibrate(duration);
        }
    }

    /**
     * 波形震动
     *
     * @param timings    震动/暂停时间数组，例如 {0, 200, 100, 300}
     * @param amplitudes 强度数组，对应每段震动 0~255（低版本忽略）
     * @param repeat     -1 不循环，0循环
     *                   <p>
     *                   示例参数：  long[] timings = {0, 200, 100, 300};
     *                   int[] amplitudes = {0, 255, 0, 128};
     */
    public void vibrateWaveform(Context context, long[] timings, int[] amplitudes, int repeat) {
        initVibrateService(context);
        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (int i = 0; i < amplitudes.length; i++) {
                amplitudes[i] = Math.max(0, Math.min(amplitudes[i], 255));
            }
            VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, repeat);
            vibrator.vibrate(effect);
        } else {
            vibrator.vibrate(timings, repeat);
        }
    }

    /**
     * 自定义曲线震动
     *
     * @param totalDuration  总时长，毫秒
     * @param stepDuration   每段时间，毫秒，越小曲线越平滑
     * @param intensityCurve 强度曲线数组，每个值 0~255，对应 stepDuration
     *                       <p>
     *                       int[] curve = {0, 64, 128, 192, 255, 192, 128, 64, 0};
     *                       long stepDuration = 50; // 每段50ms，总时长 = 50 * 9 = 450ms
     *                       vibration.vibrateWithCurve(stepDuration * curve.length, stepDuration, curve);
     */
    public void vibrateWithCurve(Context context, long totalDuration, long stepDuration, int[] intensityCurve) {
        initVibrateService(context);
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // 低版本不震动
//            vibrator.vibrate(totalDuration);
            return;
        }

        int steps = intensityCurve.length;
        long[] timings = new long[steps];
        int[] amplitudes = new int[steps];

        for (int i = 0; i < steps; i++) {
            timings[i] = stepDuration;
            amplitudes[i] = Math.max(0, Math.min(intensityCurve[i], 255));
        }

        VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, -1);
        vibrator.vibrate(effect);
    }


    public void vibrateWithCurve(Context context, long[] stepDuration, int[] intensityCurve) {
        initVibrateService(context);
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // 低版本不震动
            // vibrator.vibrate(totalDuration);
            return;
        }

        int steps = intensityCurve.length;
        long[] timings = new long[steps];
        int[] amplitudes = new int[steps];

        for (int i = 0; i < steps; i++) {
            timings[i] = stepDuration[i];
            amplitudes[i] = Math.max(0, Math.min(intensityCurve[i], 255));
        }

        VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, -1);
        vibrator.vibrate(effect);
    }

    /**
     * 按函数曲线震动
     *
     * @param totalDuration 总时长，毫秒
     * @param steps         曲线分段数，越大曲线越平滑
     * @param intensityFunc 强度函数：输入范围 0~1，输出范围 0~255
     *                      <p>
     *                      正玄震动：
     *                      vibration.vibrateWithFunction(
     *                      1000, // 1秒总时长
     *                      50,   // 50步，越多越平滑
     *                      t -> 128 + 127 * Math.sin(2 * Math.PI * t) // 正弦波，强度 0~255
     *                      );
     *                      <p>
     *                      渐入渐出：
     *                      vibration.vibrateWithFunction(
     *                      800, // 总时长
     *                      40,  // 分段
     *                      t -> 255 * (Math.sin(Math.PI * t - Math.PI / 2) / 2 + 0.5) // 0~255平滑渐入渐出
     *                      );
     *                      <p>
     *                      贝塞尔曲线：
     *                      vibration.vibrateWithFunction(
     *                      600,
     *                      30,
     *                      t -> 255 * (3*t*t - 2*t*t*t) // 简单二次贝塞尔曲线，平滑变化
     *                      );
     */
    public void vibrateWithFunction(Context context, long totalDuration, int steps, DoubleUnaryOperator intensityFunc) {
        initVibrateService(context);
        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // 低版本不震动
//            vibrator.vibrate(totalDuration);
            return;
        }

        long stepDuration = totalDuration / steps;
        long[] timings = new long[steps];
        int[] amplitudes = new int[steps];

        for (int i = 0; i < steps; i++) {
            double t = (double) i / (steps - 1); // 0~1
            int amplitude = (int) Math.round(intensityFunc.applyAsDouble(t));
            amplitude = Math.max(0, Math.min(amplitude, 255));
            timings[i] = stepDuration;
            amplitudes[i] = amplitude;
        }

        VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, -1);
        vibrator.vibrate(effect);
    }

    /**
     * 取消震动
     */
    public void cancel() {
        if (vibrator != null) vibrator.cancel();
    }

}
