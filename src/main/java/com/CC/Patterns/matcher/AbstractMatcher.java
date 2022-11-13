package com.CC.Patterns.matcher;


import com.CC.Contexts.Context;
import com.CC.Patterns.types.MatcherType;

public abstract class AbstractMatcher {
    protected Object mfuncInstance;
    protected MatcherType matcherType;
    public abstract boolean match(Context context);

    public MatcherType getMatcherType() {
        return matcherType;
    }

    public Object getMfuncInstance() {
        return mfuncInstance;
    }
}
