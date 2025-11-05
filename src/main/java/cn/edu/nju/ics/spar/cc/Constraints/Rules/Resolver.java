package cn.edu.nju.ics.spar.cc.Constraints.Rules;

import java.util.HashMap;
import java.util.Map;

public class Resolver {
    private ResolverType resolverType;
    private String variable;

    private final Map<String, String> fixingPairs;

    public Resolver() {
        this.fixingPairs = new HashMap<>();
    }

    public void addFixingPair(String field, String value){
        this.fixingPairs.put(field, value);
    }

    public void setResolverType(ResolverType resolverType) {
        this.resolverType = resolverType;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public ResolverType getResolverType() {
        return resolverType;
    }

    public String getVariable() {
        return variable;
    }

    public Map<String, String> getFixingPairs() {
        return fixingPairs;
    }
}
