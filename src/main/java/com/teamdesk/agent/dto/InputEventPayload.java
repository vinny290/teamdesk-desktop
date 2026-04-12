package com.teamdesk.agent.dto;

public class InputEventPayload {

    private String eventType;
    private Double x;
    private Double y;
    private Integer button;
    private Integer wheelDelta;
    private String key;
    private String code;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Integer getButton() {
        return button;
    }

    public void setButton(Integer button) {
        this.button = button;
    }

    public Integer getWheelDelta() {
        return wheelDelta;
    }

    public void setWheelDelta(Integer wheelDelta) {
        this.wheelDelta = wheelDelta;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "InputEventPayload{" +
                "eventType='" + eventType + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", button=" + button +
                ", wheelDelta=" + wheelDelta +
                ", key='" + key + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}