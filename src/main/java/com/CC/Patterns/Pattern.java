package com.CC.Patterns;

import java.util.HashMap;
import java.util.Map;

public class Pattern {
    private String patternId;

    private final Map<String, String> patternFields;

    public Pattern(){
        this.patternFields = new HashMap<>();
    }

    public Map<String, String> getPatternFields() {
        return patternFields;
    }

    public String getPatternId() {
        return patternId;
    }

    public void setPatternId(String patternId) {
        this.patternId = patternId;
    }
}

