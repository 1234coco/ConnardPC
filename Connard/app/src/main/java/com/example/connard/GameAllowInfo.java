package com.example.connard;

public class GameAllowInfo {
    private String name;
    private String status;

    public GameAllowInfo(String name, String status) {
        this.name = name;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }
}