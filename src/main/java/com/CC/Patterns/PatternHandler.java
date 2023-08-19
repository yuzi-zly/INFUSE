package com.CC.Patterns;

import com.CC.Contexts.Context;
import com.CC.Util.Loggable;

import java.util.LinkedHashMap;

public abstract class PatternHandler implements Loggable {
    protected final LinkedHashMap<String, Pattern> patternMap;

    public PatternHandler(){
        patternMap = new LinkedHashMap<>();
    }

    public LinkedHashMap<String, Pattern> getPatternMap() {
        return patternMap;
    }

    public abstract void buildPatterns(String patternFile);

    public abstract boolean groupMatched(Context context, Pattern pattern);
}
