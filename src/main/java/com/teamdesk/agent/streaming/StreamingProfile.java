package com.teamdesk.agent.streaming;

public enum StreamingProfile {
    HIGH(20, 1920, 1080),
    MEDIUM(12, 1280, 720),
    LOW(8, 854, 480);

    private final int fps;
    private final int width;
    private final int height;

    StreamingProfile(int fps, int width, int height) {
        this.fps = fps;
        this.width = width;
        this.height = height;
    }

    public int getFps() {
        return fps;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}