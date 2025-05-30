package com.example.connard;

public class GameAllowLimitInfo {
    private String name;
    private String status;
    private String limit;
    private String played;

    public GameAllowLimitInfo(String name, String status, String limit, String played)  {
        this.name = name;
        this.status = status;
        this.limit = limit;
        this.played = played;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }
    public String getLimit() {
        return limit;
    }
    public String getPlayed() {
        return played;
    }
}
