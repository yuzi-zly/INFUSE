package com.CC.Patterns;

import java.util.HashMap;
import java.util.Map;

public class Pattern {
    private String pattern_id;

    private final Map<String, String> pattern_fields;

    public Pattern() {
        this.pattern_fields = new HashMap<>();
    }


    public Map<String, String> getPattern_fields() {
        return pattern_fields;
    }

    public String getPattern_id() {
        return pattern_id;
    }

    public void setPattern_id(String pattern_id) {
        this.pattern_id = pattern_id;
    }

    @Override
    public String toString() {
        return pattern_id;
    }
}

