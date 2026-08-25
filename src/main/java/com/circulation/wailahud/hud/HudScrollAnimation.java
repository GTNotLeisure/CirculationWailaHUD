package com.circulation.wailahud.hud;

public class HudScrollAnimation {

    private static final float PAUSE_TICKS = 40.0F;

    public float offset(int actualWidth, int viewportWidth, long tick, float partialTick) {
        if (actualWidth <= viewportWidth) return 0.0F;
        float overflow = actualWidth - viewportWidth;
        float cycle = PAUSE_TICKS + overflow + PAUSE_TICKS + overflow;
        float phase = (tick % Math.max(1L, (long) Math.ceil(cycle))) + partialTick;
        phase %= cycle;
        if (phase < PAUSE_TICKS) return 0.0F;
        phase -= PAUSE_TICKS;
        if (phase < overflow) return phase;
        phase -= overflow;
        if (phase < PAUSE_TICKS) return overflow;
        return overflow - (phase - PAUSE_TICKS);
    }
}
