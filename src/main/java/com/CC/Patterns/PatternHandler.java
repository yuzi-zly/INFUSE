package com.CC.Patterns;

import com.CC.Contexts.Context;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

public abstract class PatternHandler {
    private final LinkedHashMap<String, Pattern> patternMap;

    public PatternHandler(){
        patternMap = new LinkedHashMap<>();
    }

    public LinkedHashMap<String, Pattern> getPatternMap() {
        return patternMap;
    }


    public abstract void buildPatterns(String patternFile) throws Exception;
    public abstract void OutputPatterns();
    public abstract boolean ctxPatternMatched(Context context, Pattern pattern);
}
