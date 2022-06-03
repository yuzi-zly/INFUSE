package com.CC.Contexts;

import com.CC.Patterns.PatternHandler;

public class ContextHandlerFactory {

    public ContextHandler getContextHandler(String type, PatternHandler patternHandler){
        if(type == null){
            return null;
        }
        if(type.equalsIgnoreCase("taxi")){
            return new TaxiContextHandler(patternHandler);
        }
        else if(type.equalsIgnoreCase("test")){
            return new TestContextHandler(patternHandler);
        }
        return null;
    }
}
