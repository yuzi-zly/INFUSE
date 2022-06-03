package com.CC.Contexts;

import com.CC.Patterns.Pattern;
import com.CC.Patterns.PatternHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class ContextHandler {
    private final PatternHandler patternHandler;

    public ContextHandler(PatternHandler patternHandler) {
        this.patternHandler = patternHandler;
    }

    public PatternHandler getPatternHandler() {
        return patternHandler;
    }

    public abstract void generateChanges(String line, List<ContextChange> changeList) throws Exception;
}
