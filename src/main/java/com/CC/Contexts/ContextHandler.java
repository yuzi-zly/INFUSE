package com.CC.Contexts;

import com.CC.Patterns.Pattern;
import com.CC.Patterns.PatternHandler;
import com.CC.Patterns.types.FreshnessType;
import com.CC.Util.Loggable;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ContextHandler implements Loggable{
    private final PatternHandler patternHandler;
    private final String dataType;

    private final AtomicLong ctxCounter;

    private final Date latestDate;

    private final SimpleDateFormat simpleDateFormat;

    //time: (patternId, context)
    private final PriorityQueue<Map.Entry<Long, Map.Entry<String, Context>>> activateContextsTimeQue;
    //patternId : [context1, context2,...]
    private final HashMap<String, Queue<Context>> activateContextsNumberMap;

    public ContextHandler(PatternHandler patternHandler, String dataType) {
        this.patternHandler = patternHandler;
        this.dataType = dataType;
        this.ctxCounter = new AtomicLong();
        //this.activateContextsTimeQue = new PriorityQueue<>(50, (o1, o2) -> (int) (o1.getKey() - o2.getKey()));
        // for taxi
        this.activateContextsTimeQue = new PriorityQueue<>(50, ((o1, o2) -> {
            if(o1.getKey() - o2.getKey() == 0){
                return o1.getValue().getKey().substring(4).compareTo(o2.getValue().getKey().substring(4));
            }
            else{
                return (int) (o1.getKey() - o2.getKey());
            }
        }));
        this.activateContextsNumberMap = new HashMap<>();
        initActivateContextsNumberMap(patternHandler.getPatternMap());
        this.latestDate = new Date();
        this.simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
    }

    private void initActivateContextsNumberMap(HashMap<String, Pattern> patternHashMap){
        for(Pattern pattern : patternHashMap.values()){
            if(pattern.getFreshnessType() == FreshnessType.number){
                activateContextsNumberMap.put(pattern.getPatternId(), new LinkedList<>());
            }
        }
    }

    public List<ContextChange> generateChanges(String line) throws Exception{
        if(dataType.equals("change")){
            return generateFromChangeLine(line);
        }
        else if(dataType.equals("rawData")){
            return generateFromRawDataLine(line);
        }
        else{
            logger.error("Illegal dataType.");
            System.exit(1);
        }
        return null;
    }

    private List<ContextChange> generateFromChangeLine(String line){
        if(line == null){
            return new ArrayList<>();
        }
        JSONObject chgJsonObj = JSON.parseObject(line);
        String chgType = chgJsonObj.getString("changeType");
        String patternId = chgJsonObj.getString("patternId");
        JSONObject ctxJsonObj = chgJsonObj.getJSONObject("context");
        Context context = buildContext(ctxJsonObj.getString("contextId"), ctxJsonObj.getJSONObject("fields"));
        ContextChange contextChange = new ContextChange();
        switch (chgType) {
            case "+":
                contextChange.setChange_type(ContextChange.Change_Type.ADDITION);
                break;
            case "-":
                contextChange.setChange_type(ContextChange.Change_Type.DELETION);
                break;
            case "u":
                contextChange.setChange_type(ContextChange.Change_Type.UPDATE);
                break;
        }
        contextChange.setPattern_id(patternId);
        contextChange.setContext(context);
        return new ArrayList<>(){{add(contextChange);}};
    }

    private List<ContextChange> generateFromRawDataLine(String line) throws ParseException {
        List<ContextChange> changeList = new ArrayList<>();
        if(line == null){
            latestDate.setTime(latestDate.getTime() + 24*3600*1000L);
            this.cleanOverdueContext(latestDate, changeList);
        }
        else{
            //date
            JSONObject dataJsonObj = JSON.parseObject(line);
            String timestampStr = dataJsonObj.getString("timestamp");
            latestDate.setTime(simpleDateFormat.parse(timestampStr).getTime());
            //context
            JSONObject fieldsJsonObj = dataJsonObj.getJSONObject("fields");
            Context context = buildContext("ctx_" + ctxCounter.getAndIncrement(), fieldsJsonObj);
            //clean overdue
            this.cleanOverdueContext(latestDate, changeList);
            //context pattern match
            boolean matched = false;
            for(Pattern pattern : patternHandler.getPatternMap().values()){
                //TODO: inducing from-pattern changes
                if(pattern.getMatcher() == null || match(pattern, context)){
                    matched = true;
                    changeList.addAll(generate(pattern, context));
                }
            }
        }
        return changeList;
    }

    private Context buildContext(String ctxId, JSONObject fieldsJsonObj){
        Context context = new Context();
        context.setCtx_id(ctxId);
        for(String fieldName : fieldsJsonObj.keySet()){
            context.getCtx_fields().put(fieldName, fieldsJsonObj.getString(fieldName));
        }
        return context;
    }

    private void cleanOverdueContext(Date dateLimit, List<ContextChange> changeList){
        while(!activateContextsTimeQue.isEmpty()){
            long overdueTime = activateContextsTimeQue.peek().getKey();
            String patternId = activateContextsTimeQue.peek().getValue().getKey();
            Context context = activateContextsTimeQue.peek().getValue().getValue();
            if(overdueTime <= dateLimit.getTime()){
                ContextChange delChange = new ContextChange();
                delChange.setChange_type(ContextChange.Change_Type.DELETION);
                delChange.setPattern_id(patternId);
                delChange.setContext(context);
                //TODO(): inducing from-pattern changes.
                changeList.add(delChange);

                activateContextsTimeQue.poll();
            }
            else{
                 break;
            }
        }
    }

    private boolean match(Pattern pattern, Context context){
        return pattern.getMatcher().match(context);
    }

    private List<ContextChange> generate(Pattern pattern, Context context){
        List<ContextChange> changeList = new ArrayList<>();
        //判断是否是number，如果是，判断是否满容量，如果是，先生成delChange，如果有delChange，则要考虑 inducing from-pattern changes.
        if(pattern.getFreshnessType() == FreshnessType.number){
            Queue<Context> queue = activateContextsNumberMap.get(pattern.getPatternId());
            if(queue.size() == Integer.parseInt(pattern.getFreshnessValue())){
                Context oldContext = queue.poll();
                assert oldContext != null;
                ContextChange delChange = new ContextChange();
                delChange.setChange_type(ContextChange.Change_Type.DELETION);
                delChange.setPattern_id(pattern.getPatternId());
                delChange.setContext(oldContext);
                changeList.add(delChange);
                //TODO(): inducing from-pattern changes.
            }
        }
        //生成addChange
        ContextChange addChange = new ContextChange();
        addChange.setChange_type(ContextChange.Change_Type.ADDITION);
        addChange.setPattern_id(pattern.getPatternId());
        addChange.setContext(context);
        changeList.add(addChange);

        //更新activateContexts容器
        if(pattern.getFreshnessType() == FreshnessType.number){
            Queue<Context> queue = activateContextsNumberMap.get(pattern.getPatternId());
            queue.add(context);
        }
        else if(pattern.getFreshnessType() == FreshnessType.time){
            long overdueTime = latestDate.getTime() + Long.parseLong(pattern.getFreshnessValue());
            activateContextsTimeQue.add(new AbstractMap.SimpleEntry<>(overdueTime, new AbstractMap.SimpleEntry<>(pattern.getPatternId(), context)));
        }

        return changeList;
    }


    public PatternHandler getPatternHandler() {
        return patternHandler;
    }

    public String getDataType() {
        return dataType;
    }
}
