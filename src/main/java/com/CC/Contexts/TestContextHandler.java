package com.CC.Contexts;

import com.CC.Patterns.PatternHandler;

import java.util.List;

public class TestContextHandler extends ContextHandler{

    public TestContextHandler(PatternHandler patternHandler) {
        super(patternHandler);
    }

    @Override
    public void generateChanges(String line, List<ContextChange> changeList) throws Exception {

    }
}
