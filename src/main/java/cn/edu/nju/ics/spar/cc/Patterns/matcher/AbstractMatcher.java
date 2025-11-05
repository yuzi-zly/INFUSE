package cn.edu.nju.ics.spar.cc.Patterns.matcher;


import cn.edu.nju.ics.spar.cc.Contexts.Context;
import cn.edu.nju.ics.spar.cc.Patterns.types.MatcherType;

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
