package com.CC.Patterns;

import com.CC.Contexts.ContextChange;

import java.util.*;

public class Pattern {
    private String patternId;

    private final Map<String, String> patternFields;

    private final Queue<ContextChange> exitBP;
    //vid to context change
    private final Map<String, ContextChange> entryBP;

    public Pattern(){
        this.patternFields = new HashMap<>();
        this.exitBP = new LinkedList<>();
        this.entryBP = new HashMap<>();
    }

    public Map<String, String> getPatternFields() {
        return patternFields;
    }

    public String getPatternId() {
        return patternId;
    }

    public void setPatternId(String patternId) {
        this.patternId = patternId;
    }


    public Map<String, ContextChange> getEntryBP() {
        return entryBP;
    }

    public Queue<ContextChange> getExitBP() {
        return exitBP;
    }

    @Override
    public String toString() {
        return "Pattern{" +
                "patternId='" + patternId + '\'' +
                ", patternFields=" + patternFields +
                '}';
    }
}

