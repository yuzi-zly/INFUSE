package com.CC.Contexts;

import com.CC.Patterns.Pattern;
import com.CC.Patterns.PatternHandler;

import java.util.List;
import java.util.StringTokenizer;

public class Q3ContextHandler extends ContextHandler{


    public Q3ContextHandler(PatternHandler patternHandler) {
        super(patternHandler);
    }

    @Override
    protected Context buildContextFromOrigin(String line, long idIndex) {
        Context retctx = null;
        if (line != null && !line.equals("")) {
            while (line.contains(",,")) {
                line = line.substring(0, line.indexOf(",,") + 1) + " " + line.substring(line.indexOf(",,") + 1);
            }
            if(line.endsWith(",")){
                line = line + " ";
            }

            //FLOWTYPE,TIME,STATIONID,VLP,VLPC,VEHICLETYPE,PASSID,
            //TIMESTRING,ORIGINALFLAG,PROVINCEBOUND,MEDIATYPE,SPECIALTYPE,TRANSCODE
            StringTokenizer st = new StringTokenizer(line, ",");
            String flowtype = st.nextToken();
            String time = st.nextToken();
            String stationid = st.nextToken();
            String vlp = st.nextToken();
            String vlpc = st.nextToken();
            String vehicletype = st.nextToken();
            String passid = st.nextToken();
            String timestring = st.nextToken();
            String originalflag = st.nextToken();
            String provincebound = st.nextToken();
            String mediatype = st.nextToken();
            String specialtype = st.nextToken();
            String transcode = st.nextToken();


            // Generate one context
            retctx = new Context();
            retctx.setCtx_id("ctx_" + idIndex);
            retctx.getCtx_fields().put("flowType", flowtype);
            retctx.getCtx_fields().put("time", time);
            retctx.getCtx_fields().put("stationId", stationid);
            retctx.getCtx_fields().put("vlp", vlp);
            retctx.getCtx_fields().put("vlpc", vlpc);
            retctx.getCtx_fields().put("vehicleType", vehicletype);
            retctx.getCtx_fields().put("passId", passid);
            retctx.getCtx_fields().put("timeString", timestring);
            retctx.getCtx_fields().put("originalFlag", originalflag);
            retctx.getCtx_fields().put("provinceBound", provincebound);
            retctx.getCtx_fields().put("mediaType", mediatype);
            retctx.getCtx_fields().put("specialType", specialtype);
            retctx.getCtx_fields().put("transCode", transcode);
        }
        return retctx;
    }



    private boolean validRecord(Context context){
        if(context.getCtx_fields().get("passId").startsWith("000000")){
            return false;
        }
        if(context.getCtx_fields().get("vlp").startsWith("默") || context.getCtx_fields().get("vlp").startsWith("0")){
            return false;
        }
        return true;
    }

    enum recordType{ENTRY,EXIT, NORMAL};

    //入口-> ENTRY, 出口/虚拟门架-> EXIT, 正常门架-> NORMAL
    private recordType normalRecord(Context context){
        //入口
        if(context.getCtx_fields().get("flowType").equals("1")
                || context.getCtx_fields().get("flowType").equals("2") && context.getCtx_fields().get("provinceBound").equals("1")){
            return recordType.ENTRY;
        }
        //出口
        else if(context.getCtx_fields().get("flowType").equals("3")
                || (context.getCtx_fields().get("flowType").equals("2") && context.getCtx_fields().get("provinceBound").equals("2"))
                || (context.getCtx_fields().get("flowType").equals("2") && context.getCtx_fields().get("provinceBound").equals("0") && context.getCtx_fields().get("originalFlag").equals("2"))){
            return recordType.EXIT;
        }
        else{
            return recordType.NORMAL;
        }
    }


    @Override
    protected int createChanges(Context context, List<ContextChange> changeList, int atomicGroup) {
        if(!validRecord(context))
            return atomicGroup;
        for(Pattern pattern : this.getPatternHandler().getPatternMap().values()){
            //not match the pattern group
            if(!this.getPatternHandler().groupMatched(context, pattern))
                continue;
            //判断pattern是entry 还是exit
            if(pattern.getPatternFields().get("entryFlag").equals("true")){
                //entry
                if(normalRecord(context) == recordType.ENTRY){
                    if(pattern.getEntryBP().containsKey(context.getCtx_fields().get("vlp") + "-" + context.getCtx_fields().get("vlpc"))){
                        ContextChange overdueChange = pattern.getEntryBP().get(context.getCtx_fields().get("vlp") + "-" + context.getCtx_fields().get("vlpc"));
                        //deletion
                        ContextChange delChange = new ContextChange();
                        delChange.setChange_type(ContextChange.Change_Type.DELETION);
                        delChange.setContext(overdueChange.getContext());
                        delChange.setPattern_id(pattern.getPatternId());
                        delChange.setAtomicGroup(atomicGroup);
                        changeList.add(delChange);

                        pattern.getEntryBP().remove(context.getCtx_fields().get("vlp") + "-" + context.getCtx_fields().get("vlpc"));
                    }

                    //addition
                    ContextChange addChange = new ContextChange();
                    addChange.setChange_type(ContextChange.Change_Type.ADDITION);
                    addChange.setAtomicGroup(atomicGroup);
                    addChange.setPattern_id(pattern.getPatternId());
                    addChange.setContext(context);
                    changeList.add(addChange);

                    pattern.getEntryBP().put(context.getCtx_fields().get("vlp") + "-" + context.getCtx_fields().get("vlpc"), addChange);

                    atomicGroup++;
                }
            }
            else{
                //exit
                if(normalRecord(context) == recordType.EXIT){
                    //addition
                    ContextChange addChange = new ContextChange();
                    addChange.setChange_type(ContextChange.Change_Type.ADDITION);
                    addChange.setContext(context);
                    addChange.setPattern_id(pattern.getPatternId());
                    addChange.setAtomicGroup(atomicGroup);
                    changeList.add(addChange);

                    atomicGroup++;

                    //deletion
                    ContextChange delChange = new ContextChange();
                    delChange.setChange_type(ContextChange.Change_Type.DELETION);
                    delChange.setContext(context);
                    delChange.setPattern_id(pattern.getPatternId());
                    delChange.setAtomicGroup(atomicGroup);
                    changeList.add(delChange);

                    //atomicGroup++;
                    Pattern entryPattern = getPatternHandler().getPatternMap().get(pattern.getPatternId().substring(0, pattern.getPatternId().length() - 4) + "entry");

                    //deletion
                    if(entryPattern.getEntryBP().containsKey(context.getCtx_fields().get("vlp") + "-" + context.getCtx_fields().get("vlpc"))){
                        ContextChange overdueChange = entryPattern.getEntryBP().get(context.getCtx_fields().get("vlp") + "-" + context.getCtx_fields().get("vlpc"));
                        //deletion
                        ContextChange delChange1 = new ContextChange();
                        delChange1.setChange_type(ContextChange.Change_Type.DELETION);
                        delChange1.setContext(overdueChange.getContext());
                        delChange1.setPattern_id(entryPattern.getPatternId());
                        delChange1.setAtomicGroup(atomicGroup);
                        changeList.add(delChange1);

                        entryPattern.getEntryBP().remove(context.getCtx_fields().get("vlp") + "-" + context.getCtx_fields().get("vlpc"));
                    }

                    atomicGroup++;

                }
            }
        }
        return atomicGroup;
    }

    @Override
    protected int cleanChanges(List<ContextChange> changeList, int atomicGroup) {
        return atomicGroup;
    }

}
