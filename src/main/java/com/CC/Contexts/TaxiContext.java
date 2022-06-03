package com.CC.Contexts;

public class TaxiContext extends Context{
    private String ctx_category;
    private String ctx_subject;
    private String ctx_predicate;
    //private String ctx_object;
    private String ctx_longitude;
    private String ctx_latitude;
    private String ctx_speed;
    private String ctx_site;
    private String ctx_timestamp;

    public String getCtx_category() {
        return ctx_category;
    }

    public String getCtx_subject() {
        return ctx_subject;
    }

    public String getCtx_predicate() {
        return ctx_predicate;
    }

    public String getCtx_longitude() {
        return ctx_longitude;
    }

    public String getCtx_latitude() {
        return ctx_latitude;
    }

    public String getCtx_speed() {
        return ctx_speed;
    }

    public String getCtx_site() {
        return ctx_site;
    }

    public String getCtx_timestamp() {
        return ctx_timestamp;
    }

    public void setCtx_category(String ctx_category) {
        this.ctx_category = ctx_category;
    }

    public void setCtx_subject(String ctx_subject) {
        this.ctx_subject = ctx_subject;
    }

    public void setCtx_predicate(String ctx_predicate) {
        this.ctx_predicate = ctx_predicate;
    }

    public void setCtx_longitude(String ctx_longitude) {
        this.ctx_longitude = ctx_longitude;
    }

    public void setCtx_latitude(String ctx_latitude) {
        this.ctx_latitude = ctx_latitude;
    }

    public void setCtx_speed(String ctx_speed) {
        this.ctx_speed = ctx_speed;
    }

    public void setCtx_site(String ctx_site) {
        this.ctx_site = ctx_site;
    }

    public void setCtx_timestamp(String ctx_timestamp) {
        this.ctx_timestamp = ctx_timestamp;
    }
}
