package com.CC.Patterns;

public class PatternHandlerFactory {
    public PatternHandler getPatternHandler(String type){
        if(type == null){
            return null;
        }
        if(type.equalsIgnoreCase("taxi")){
            return new TaxiPatternHandler();
        }
        else if(type.equalsIgnoreCase("test")){
            return new TestPatternHandler();
        }
        return null;
    }
}
