package com.CC.Patterns;

import com.CC.Contexts.Context;

public class TestPatternHandler extends PatternHandler{
    @Override
    public void buildPatterns(String patternFile) throws Exception {

    }

    @Override
    public void OutputPatterns() {

    }

    @Override
    public boolean ctxPatternMatched(Context context, Pattern pattern) {
        return false;
    }
}
