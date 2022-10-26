package com.CC.Constraints.Runtime;

import com.CC.Contexts.Context;

import java.util.*;

public class Link implements Cloneable{

    public enum Link_Type {SATISFIED, VIOLATED};

    private Link_Type linkType;
    //a set of var to context
    private Set<Map.Entry<String, Context>> vaSet;

    //constructor
    public Link(Link_Type linkType){
        this.linkType = linkType;
        this.vaSet = new HashSet<>();
    }

    //getter && setter
    public Link_Type getLinkType() {
        return linkType;
    }

    public Set<Map.Entry<String, Context>> getVaSet() {
        return vaSet;
    }

    public void setLinkType(Link_Type linkType) {
        this.linkType = linkType;
    }

    public void setVaSet(Set<Map.Entry<String, Context>> vaSet) {
        this.vaSet = vaSet;
    }

    //functional methods
    public void AddVA(String var, Context context){
        this.vaSet.add(new AbstractMap.SimpleEntry<>(var, context));
    }

    @Override
    public String toString() {
        return "{" +
                "linkType=" + linkType +
                ", vaSet=" + vaSet +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Link link = (Link) o;

        if (linkType != link.linkType) return false;
        return vaSet.equals(link.vaSet);
    }

    @Override
    public int hashCode() {
        int result = linkType.hashCode();
        result = 31 * result + vaSet.hashCode();
        return result;
    }

    @Override
    protected Link clone() throws CloneNotSupportedException {
        Link clone = (Link) super.clone();
        clone.setLinkType(this.linkType == Link_Type.SATISFIED ? Link_Type.SATISFIED : Link_Type.VIOLATED);
        clone.setVaSet(new HashSet<>());
        for(Map.Entry<String, Context> entry : this.vaSet){
            String key = entry.getKey();
            Context value = entry.getValue();
            clone.getVaSet().add(new AbstractMap.SimpleEntry<>(key, value));
        }
        return clone;
    }
}
