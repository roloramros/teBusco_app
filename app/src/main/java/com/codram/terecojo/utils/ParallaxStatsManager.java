package com.codram.terecojo.utils;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;
import com.codram.terecojo.data.model.StatsResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParallaxStatsManager {
    private final ViewGroup container;
    private final List<TextView> textViews = new ArrayList<>();
    private final Random random = new Random();
    private StatsResponse lastStats;

    public ParallaxStatsManager(ViewGroup container) {
        this.container = container;
    }

    public void init(int count) {
        container.post(() -> {
            int width = container.getWidth();
            int height = container.getHeight();
            for (int i = 0; i < count; i++) {
                createTextItem(width, height);
            }
        });
    }

    private void createTextItem(int width, int height) {
        TextView tv = new TextView(container.getContext());
        tv.setAlpha(0.09f + random.nextFloat() * 0.09f);
        tv.setTextSize(18 + random.nextInt(12));
        tv.setText("Cargando...");
        
        // Assign a fixed type (0-3) to this TextView
        tv.setTag(random.nextInt(4));
        
        container.addView(tv);
        textViews.add(tv);

        animateView(tv, width, height);
    }

    private void animateView(TextView tv, int width, int height) {
        // Margin to ensure they start/end off-screen
        float margin = 400f;
        
        // Random start and end points
        float startX, startY, endX, endY;
        
        // Pick a random side to start from (0:Top, 1:Bottom, 2:Left, 3:Right)
        int startSide = random.nextInt(4);
        switch (startSide) {
            case 0: // Top
                startX = random.nextInt(width);
                startY = -margin;
                endX = random.nextInt(width);
                endY = height + margin;
                break;
            case 1: // Bottom
                startX = random.nextInt(width);
                startY = height + margin;
                endX = random.nextInt(width);
                endY = -margin;
                break;
            case 2: // Left
                startX = -margin;
                startY = random.nextInt(height);
                endX = width + margin;
                endY = random.nextInt(height);
                break;
            default: // Right
                startX = width + margin;
                startY = random.nextInt(height);
                endX = -margin;
                endY = random.nextInt(height);
                break;
        }

        long duration = 25000 + random.nextInt(20000);
        
        ObjectAnimator animX = ObjectAnimator.ofFloat(tv, "translationX", startX, endX);
        ObjectAnimator animY = ObjectAnimator.ofFloat(tv, "translationY", startY, endY);
        
        animX.setDuration(duration);
        animY.setDuration(duration);
        animX.setRepeatCount(ValueAnimator.INFINITE);
        animY.setRepeatCount(ValueAnimator.INFINITE);
        animX.setRepeatMode(ValueAnimator.RESTART);
        animY.setRepeatMode(ValueAnimator.RESTART);

        // Initial fraction to start them already on screen
        float initialFraction = random.nextFloat();
        animX.setCurrentPlayTime((long) (duration * initialFraction));
        animY.setCurrentPlayTime((long) (duration * initialFraction));

        animX.start();
        animY.start();
    }

    private void updateText(TextView tv) {
        if (lastStats == null) return;
        int type = (int) tv.getTag();
        String text;
        switch (type) {
            case 0: text = "Pasajeros: " + lastStats.totalUsuarios; break;
            case 1: text = "Choferes Activos: " + lastStats.totalChoferes; break;
            case 2: text = "Viajes Completados: " + lastStats.viajesCompletados; break;
            default: text = "Solicitudes Activas: " + lastStats.viajesActivos; break;
        }
        tv.setText(text);
    }

    public void updateData(StatsResponse stats) {
        this.lastStats = stats;
        container.post(() -> {
            for (TextView tv : textViews) {
                updateText(tv);
            }
        });
    }
}
