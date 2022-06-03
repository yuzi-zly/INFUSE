package com.CC.Patterns;

public class TaxiPattern extends Pattern{
    private  String freshness;
    private  String category;
    private  String subject;
    private  String predicate;
    private  String object;
    private  String site;

    public String getFreshness() {
        return freshness;
    }

    public String getCategory() {
        return category;
    }

    public String getSubject() {
        return subject;
    }

    public String getPredicate() {
        return predicate;
    }

    public String getObject() {
        return object;
    }

    public String getSite() {
        return site;
    }

    public void setFreshness(String freshness) {
        this.freshness = freshness;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public void setSite(String site) {
        this.site = site;
    }
}
