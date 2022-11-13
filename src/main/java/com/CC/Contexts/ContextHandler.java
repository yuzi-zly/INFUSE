package com.CC.Contexts;

import com.CC.Patterns.Pattern;
import com.CC.Patterns.PatternHandler;
import com.CC.Patterns.types.FreshnessType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class ContextHandler {
    private final PatternHandler patternHandler;
    private String dataType;

    private final AtomicLong ctxCounter;

    //date : (patternId, context)
    private final PriorityBlockingQueue<Map.Entry<Long, Map.Entry<String, Context>>> activateContextsTimeQue;
    //patternId : [context1, context2,...]
    private final ConcurrentHashMap<String, LinkedBlockingQueue<Context>> activateContextsNumberMap;

    public ContextHandler(PatternHandler patternHandler) {
        this.patternHandler = patternHandler;
        this.ctxCounter = new AtomicLong();
        this.activateContextsTimeQue = new PriorityBlockingQueue<>(50, (o1, o2) -> (int) (o1.getKey() - o2.getKey()));
        this.activateContextsNumberMap = new ConcurrentHashMap<>();
        initActivateContextsNumberMap(patternHandler.getPatternMap());
    }

    public PatternHandler getPatternHandler() {
        return patternHandler;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getDataType() {
        return dataType;
    }

    public void generateChanges(String line, List<ContextChange> changeList) throws Exception{
        if(line == null){

        }
        else{

        }
    }

    private Context buildContext(String line){

    }

    private void initActivateContextsNumberMap(HashMap<String, Pattern> patternHashMap){
        for(Pattern pattern : patternHashMap.values()){
            if(pattern.getFreshnessType() == FreshnessType.number){
                activateContextsNumberMap.put(pattern.getPatternId(), new LinkedBlockingQueue<>());
            }
        }
    }
}
