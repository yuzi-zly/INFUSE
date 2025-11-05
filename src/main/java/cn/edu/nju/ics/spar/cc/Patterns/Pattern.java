package cn.edu.nju.ics.spar.cc.Patterns;

import cn.edu.nju.ics.spar.cc.Patterns.matcher.AbstractMatcher;
import cn.edu.nju.ics.spar.cc.Patterns.types.FreshnessType;

import java.util.HashSet;
import java.util.Set;

public class Pattern {
    private String patternId;
    private FreshnessType freshnessType;
    private String freshnessValue;
    private final Set<String> dataSourceSet;
    private AbstractMatcher matcher;

    public Pattern() {
        this.dataSourceSet = new HashSet<>();
    }

    public void addDataSource(String dataSource) {this.dataSourceSet.add(dataSource);}

    public void setPatternId(String patternId) {
        this.patternId = patternId;
    }

    public void setFreshnessType(FreshnessType freshnessType) {
        this.freshnessType = freshnessType;
    }

    public void setFreshnessValue(String freshnessValue) {
        this.freshnessValue = freshnessValue;
    }

    public void setMatcher(AbstractMatcher matcher) {
        this.matcher = matcher;
    }

    public String getPatternId() {
        return patternId;
    }

    public FreshnessType getFreshnessType() {
        return freshnessType;
    }

    public String getFreshnessValue() {
        return freshnessValue;
    }

    public Set<String> getDataSourceSet() {
        return dataSourceSet;
    }

    public AbstractMatcher getMatcher() {
        return matcher;
    }

    @Override
    public String toString() {
        return "Pattern{" +
                "patternId='" + patternId + '\'' +
                ", freshnessType='" + freshnessType + '\'' +
                ", freshnessValue='" + freshnessValue + '\'' +
                ", dataSourceList=" + dataSourceSet +
                ", matcher=" + matcher +
                '}';
    }
}

