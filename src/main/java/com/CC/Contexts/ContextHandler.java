package com.CC.Contexts;

import com.CC.Patterns.PatternHandler;
import com.CC.Util.Loggable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ContextHandler implements Loggable{


    private final PatternHandler patternHandler;
    private final AtomicLong idCounter;

    private final AtomicInteger atomicCounter;

    public ContextHandler(PatternHandler patternHandler) {
        this.patternHandler = patternHandler;
        this.idCounter =  new AtomicLong(0);
        this.atomicCounter = new AtomicInteger(0);
    }

    public PatternHandler getPatternHandler() {
        return patternHandler;
    }

    public List<ContextChange> generateChanges(String dataLine){
        List<ContextChange> changeList = new ArrayList<>();
        if(dataLine == null){
            // clean changes
            atomicCounter.set(cleanChanges(changeList, atomicCounter.get()));
        }
        else{
            // build context
            Context context = buildContextFromOrigin(dataLine, idCounter.getAndIncrement());
            // create changes
            atomicCounter.set(createChanges(context, changeList, atomicCounter.get()));
        }
        return changeList;
    }

    protected abstract Context buildContextFromOrigin(String line, long idIndex);

    protected abstract int createChanges(Context context, List<ContextChange> changeList, int atomicIndex);
    protected abstract int cleanChanges(List<ContextChange> changeList, int atomicIndex);
}
