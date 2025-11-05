package cn.edu.nju.ics.spar.cc.Patterns.matcher;


import cn.edu.nju.ics.spar.cc.Contexts.Context;
import cn.edu.nju.ics.spar.cc.Patterns.types.MatcherType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class FunctionMatcher extends AbstractMatcher{
    private final String funcName;
    private final List<String> extraArgList;

    public FunctionMatcher(String funcName, Object mfuncInstance){
        this.matcherType = MatcherType.function;
        this.mfuncInstance = mfuncInstance;
        this.funcName = funcName;
        this.extraArgList = new ArrayList<>();
    }

    @Override
    public boolean match(Context context) {
        boolean result = false;
        try {
            Method m = this.mfuncInstance.getClass().getMethod("mfunc", String.class, Class.forName("java.util.Map"), Class.forName("java.util.List"));
            result = (boolean) m.invoke(this.mfuncInstance, funcName, context.getCtx_fields(), extraArgList);
        } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public void addExtraArg(String extraArg){
        this.extraArgList.add(extraArg);
    }

    public String getFuncName() {
        return funcName;
    }

    public List<String> getExtraArgList() {
        return extraArgList;
    }

    @Override
    public String toString() {
        return "FunctionMatcher{" +
                "funcName='" + funcName + '\'' +
                ", extraArgList=" + extraArgList +
                '}';
    }
}
