package com.example.connard;

public class GamePauseInfo {
    private String name;
    private String status;
    private String timeEnd;

    public GamePauseInfo(String name, String status,String timeEnd) {
        this.name = name;
        this.status = status;
        this.timeEnd = timeEnd;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }
    public String getTimeEnd() {
        return timeEnd;
    }
}