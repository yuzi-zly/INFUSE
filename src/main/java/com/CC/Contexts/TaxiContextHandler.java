package com.CC.Contexts;

import com.CC.Patterns.Pattern;
import com.CC.Patterns.PatternHandler;

import java.text.SimpleDateFormat;
import java.util.*;

public class TaxiContextHandler extends ContextHandler{

    public static final String categoryStr = "category";
    public static final String subjectStr = "subject";
    public static final String predicateStr = "predicate";
    public static final String longitudeStr = "longitude";
    public static final String latitudeStr = "latitude";
    public static final String speedStr = "speed";
    public static final String siteStr = "site";
    public static final String timestampStr = "timestamp";

    /*  存储现有的待过期的context, 按到期时间升序排列
      <OverDueTime: Date, <Pattern_id: String, context: Context>>
      Regular
  */
    private final Queue<Map.Entry<Date, ContextChange>> activateCtxQue;

    private int counter;
    private final Date latestDate;

    public TaxiContextHandler(PatternHandler patternHandler) {
        super(patternHandler);
        Comparator<Map.Entry<Date, ContextChange>> activateCtxCmp = (o1, o2) -> {
            Date date1 = o1.getKey();
            Date date2 = o2.getKey();
            if (date1.getTime() == date2.getTime()) {
                String pat1 = o1.getValue().getPattern_id().substring(4);
                String pat2 = o2.getValue().getPattern_id().substring(4);
                return pat1.compareTo(pat2);
            } else {
                return (int) (date1.getTime() - date2.getTime());
            }
        };
        activateCtxQue = new PriorityQueue<>(activateCtxCmp);
        counter = 0;
        latestDate = new Date();
    }

    @Override
    public void generateChanges(String line, List<ContextChange> changeList) throws Exception {
        if(line == null){
            latestDate.setTime(latestDate.getTime() + (long)(24*3600));
            this.cleanOverdueContext(latestDate, changeList);
        }
        else{
            Context taxiContext = buildContext(line, counter++);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            Date ctxDate = simpleDateFormat.parse(taxiContext.getCtx_fields().get(timestampStr));
            this.cleanOverdueContext(ctxDate, changeList);
            this.createAdditionChangesRegular(taxiContext, changeList);
            this.createAdditionChangesHotArea(taxiContext, changeList);
            latestDate.setTime(ctxDate.getTime());
        }
    }

    public Context buildContext(String line, int counter){
        Context retctx = null;
        if (line != null && !line.equals("")) {
            // Get taxi information
            StringTokenizer st = new StringTokenizer(line, ",");
            String time = st.nextToken();  // Time
            String taxiId = st.nextToken();  // Taxi id
            String longitude = st.nextToken();  // Longitude
            String latitude = st.nextToken();  // Latitude
            String speed = st.nextToken();  // Speed
            st.nextToken();  // Skip direction
            int status = Integer.parseInt(st.nextToken());  // Status
            // Skip tag

            // Generate one context
            retctx = new Context();
            retctx.setCtx_id("ctx_" + counter);
            retctx.getCtx_fields().put(categoryStr, "location");
            retctx.getCtx_fields().put(subjectStr, taxiId);
            if (status == 0) {
                retctx.getCtx_fields().put(predicateStr, "run_without_service");
            } else {
                retctx.getCtx_fields().put(predicateStr, "run_with_service");
            }
            // retctx.setCtx_object(longitude + "_" + latitude + "_" + speed);
            retctx.getCtx_fields().put(longitudeStr, longitude);
            retctx.getCtx_fields().put(latitudeStr, latitude);
            retctx.getCtx_fields().put(speedStr, speed);
            //倒数第二位
            retctx.getCtx_fields().put(siteStr, "sutpc_" + taxiId.substring(taxiId.length() - 2, taxiId.length() - 1).toUpperCase());
            retctx.getCtx_fields().put(timestampStr, time);
        }
        return retctx;
    }

    /*
        For Regular Pattern
     */

    //根据Date参数检查PQ是否有过期
    public void cleanOverdueContext(Date date, List<ContextChange> changeList){
        while(!activateCtxQue.isEmpty()){
            ContextChange contextChange = activateCtxQue.peek().getValue();
            Date date0 = activateCtxQue.peek().getKey();
            if(date0.compareTo(date) <= 0){
                ContextChange delete_change = new ContextChange();
                delete_change.setChange_type(ContextChange.Change_Type.DELETION);
                delete_change.setPattern_id(contextChange.getPattern_id());
                delete_change.setContext(contextChange.getContext());
                delete_change.setTimeStamp(date0.getTime());
                changeList.add(delete_change);

                activateCtxQue.poll();
            }
            else{
                break;
            }
        }

    }

    //RegularPattern: 根据读入数据创建若干个新的addition change(每对应一个pattern就有一个新change)
    public void createAdditionChangesRegular(Context context, List<ContextChange> changeList) throws Exception{
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        for(Pattern pattern : this.getPatternHandler().getPatternMap().values()){
            if(this.getPatternHandler().ctxPatternMatched(context, pattern)){
                ContextChange contextChange = new ContextChange();
                contextChange.setChange_type(ContextChange.Change_Type.ADDITION);
                contextChange.setPattern_id(pattern.getPattern_id());
                contextChange.setContext(context);
                contextChange.setTimeStamp(simpleDateFormat.parse(context.getCtx_fields().get(timestampStr)).getTime());
                changeList.add(contextChange);
                Date overdue = simpleDateFormat.parse(context.getCtx_fields().get(timestampStr));
                long period = Long.parseLong(pattern.getPattern_fields().get(TaxiPatternHandler.freshnessStr));
                overdue.setTime(overdue.getTime() + period);
                activateCtxQue.add(new AbstractMap.SimpleEntry<>(overdue, contextChange));
            }
        }
    }

    /*
        For hot Area
        pair<latitude, longitude>
    */

    private boolean InPolygon(Map.Entry<Double, Double> site, List<Map.Entry<Double, Double>> points){
        int cnt = 0;
        Map.Entry<Double, Double> p1, p2;
        for(int i = 0, j = points.size() - 1; i < points.size(); j=i++){
            p1 = points.get(i);
            p2 = points.get(j);

            if( ((site.getKey() >= p1.getKey()) && (site.getKey() < p2.getKey())) || ((site.getKey() >= p2.getKey()) && (site.getKey() < p1.getKey())) ){
                if(Math.abs(p1.getKey() - p2.getKey()) > 0){
                    double y = p1.getValue() - ((p1.getValue() - p2.getValue()) * (p1.getKey() - site.getKey())) / (p1.getKey() - p2.getKey());
                    if(y == site.getValue())
                        return true;
                    if(y < site.getValue()){
                        cnt++;
                    }
                }
            }
        }
        return (cnt % 2) != 0;
    }

    private boolean InHotArea_A(String longitude, String latitude){
        /*
            [AA]:4
            [AA0]:22.571615,113.923059
            [AA1]:22.573121,113.864853
            [AA2]:22.590556,113.882534
            [AA3]:22.590873,113.901760
        */
        Map.Entry<Double, Double> site = new AbstractMap.SimpleEntry<>(Double.valueOf(latitude), Double.valueOf(longitude));
        List<Map.Entry<Double, Double>> points = new ArrayList<>();
        points.add(new AbstractMap.SimpleEntry<>(22.571615, 113.923059));
        points.add(new AbstractMap.SimpleEntry<>(22.573121, 113.864853));
        points.add(new AbstractMap.SimpleEntry<>(22.590556, 113.882534));
        points.add(new AbstractMap.SimpleEntry<>(22.590873, 113.901760));
        return InPolygon(site, points);
    }

    private boolean InHotArea_B(String longitude, String latitude){
        /*
            [BB]:4
            [BB0]:22.548391,113.89455
            [BB1]:22.573121,113.864853
            [BB2]:22.590556,113.882534
            [BB3]:22.590873,113.90176
        */
        Map.Entry<Double, Double> site = new AbstractMap.SimpleEntry<>(Double.valueOf(latitude), Double.valueOf(longitude));
        List<Map.Entry<Double, Double>> points = new ArrayList<>();
        points.add(new AbstractMap.SimpleEntry<>(22.548391, 113.89455));
        points.add(new AbstractMap.SimpleEntry<>(22.573121, 113.864853));
        points.add(new AbstractMap.SimpleEntry<>(22.590556, 113.882534));
        points.add(new AbstractMap.SimpleEntry<>(22.590873, 113.90176));
        return InPolygon(site, points);
    }

    private boolean InHotArea_C(String longitude, String latitude){
        /*
            [CC]:5
            [CC0]:22.571615,113.923059
            [CC1]:22.548391,113.89455
            [CC2]:22.573121,113.864853
            [CC3]:22.590556,113.882534
            [CC4]:22.590873,113.901761
         */
        Map.Entry<Double, Double> site = new AbstractMap.SimpleEntry<>(Double.valueOf(latitude), Double.valueOf(longitude));
        List<Map.Entry<Double, Double>> points = new ArrayList<>();
        points.add(new AbstractMap.SimpleEntry<>(22.571615, 113.923059));
        points.add(new AbstractMap.SimpleEntry<>(22.548391, 113.89455));
        points.add(new AbstractMap.SimpleEntry<>(22.573121, 113.864853));
        points.add(new AbstractMap.SimpleEntry<>(22.590556, 113.882534));
        points.add(new AbstractMap.SimpleEntry<>(22.590873, 113.901761));
        return InPolygon(site, points);
    }

    private boolean InHotArea_D(String longitude, String latitude){
        /*
            [DD]:3
            [DD0]:22.559489,114.02018
            [DD1]:22.570902,114.085411
            [DD2]:22.503359,114.060348
         */
        Map.Entry<Double, Double> site = new AbstractMap.SimpleEntry<>(Double.valueOf(latitude), Double.valueOf(longitude));
        List<Map.Entry<Double, Double>> points = new ArrayList<>();
        points.add(new AbstractMap.SimpleEntry<>(22.559489, 114.02018));
        points.add(new AbstractMap.SimpleEntry<>(22.570902, 114.085411));
        points.add(new AbstractMap.SimpleEntry<>(22.503359, 114.060348));
        return InPolygon(site, points);
    }

    private boolean InHotArea_E(String longitude, String latitude){
        /*
            [EE]:4
            [EE0]:22.559489,114.092304
            [EE1]:22.571853,114.142402
            [EE2]:22.541416,114.135879
            [EE3]:22.532457,114.08485
         */
        Map.Entry<Double, Double> site = new AbstractMap.SimpleEntry<>(Double.valueOf(latitude), Double.valueOf(longitude));
        List<Map.Entry<Double, Double>> points = new ArrayList<>();
        points.add(new AbstractMap.SimpleEntry<>(22.559489, 114.092304));
        points.add(new AbstractMap.SimpleEntry<>(22.571853, 114.142402));
        points.add(new AbstractMap.SimpleEntry<>(22.541416, 114.135879));
        points.add(new AbstractMap.SimpleEntry<>(22.532457, 114.08485));
        return InPolygon(site, points);
    }

    private boolean InHotArea_F(String longitude, String latitude){
        /*
            [FF]:5
            [FF0]:22.559489,114.02018
            [FF1]:22.570902,114.085411
            [FF2]:22.571853,114.142402
            [FF3]:22.541416,114.135879
            [FF4]:22.503359,114.060348
         */
        Map.Entry<Double, Double> site = new AbstractMap.SimpleEntry<>(Double.valueOf(latitude), Double.valueOf(longitude));
        List<Map.Entry<Double, Double>> points = new ArrayList<>();
        points.add(new AbstractMap.SimpleEntry<>(22.559489, 114.02018));
        points.add(new AbstractMap.SimpleEntry<>(22.570902, 114.085411));
        points.add(new AbstractMap.SimpleEntry<>(22.571853, 114.142402));
        points.add(new AbstractMap.SimpleEntry<>(22.541416, 114.135879));
        points.add(new AbstractMap.SimpleEntry<>(22.503359, 114.060348));
        return InPolygon(site, points);
    }

    private boolean InHotArea_G(String longitude, String latitude){
        /*
            [GG]:5
            [GG0]:22.565195,113.927826
            [GG1]:22.55616,114.015056
            [GG2]:22.528414,114.019691
            [GG3]:22.514936,113.937809
            [GG4]:22.531744,113.902618
         */
        Map.Entry<Double, Double> site = new AbstractMap.SimpleEntry<>(Double.valueOf(latitude), Double.valueOf(longitude));
        List<Map.Entry<Double, Double>> points = new ArrayList<>();
        points.add(new AbstractMap.SimpleEntry<>(22.565195, 113.927826));
        points.add(new AbstractMap.SimpleEntry<>(22.55616, 114.015056));
        points.add(new AbstractMap.SimpleEntry<>(22.528414, 114.019691));
        points.add(new AbstractMap.SimpleEntry<>(22.514936, 113.937809));
        points.add(new AbstractMap.SimpleEntry<>(22.531744, 113.902618));
        return InPolygon(site, points);
    }

    private boolean InHotArea_H(String longitude, String latitude){
        /*
            [HH]:3
            [HH0]:22.565195,113.927826
            [HH1]:22.55616,114.015056
            [HH2]:22.611317,113.988792
         */
        Map.Entry<Double, Double> site = new AbstractMap.SimpleEntry<>(Double.valueOf(latitude), Double.valueOf(longitude));
        List<Map.Entry<Double, Double>> points = new ArrayList<>();
        points.add(new AbstractMap.SimpleEntry<>(22.565195, 113.927826));
        points.add(new AbstractMap.SimpleEntry<>(22.55616, 114.015056));
        points.add(new AbstractMap.SimpleEntry<>(22.611317, 113.988792));
        return InPolygon(site, points);
    }

    private boolean InHotArea_I(String longitude, String latitude){
        // I = G
        return InHotArea_G(longitude, latitude);
    }

    // hotAreaPattern with freshness 48000
    public void createAdditionChangesHotArea(Context context, List<ContextChange> changeList) throws Exception{
        if(context.getCtx_fields().get(predicateStr).equals("run_without_service"))
            return;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        //StringTokenizer st = new StringTokenizer(context.getCtx_object(), "_");
        String longitude = context.getCtx_fields().get(longitudeStr);
        String latitude = context.getCtx_fields().get(latitudeStr);
        if(InHotArea_A(longitude, latitude)){
            ContextChange contextChange = new ContextChange();
            contextChange.setChange_type(ContextChange.Change_Type.ADDITION);
            contextChange.setPattern_id("pat_A");
            contextChange.setContext(context);
            contextChange.setTimeStamp(simpleDateFormat.parse(context.getCtx_fields().get(timestampStr)).getTime());
            changeList.add(contextChange);
            Date overdue = simpleDateFormat.parse(context.getCtx_fields().get(timestampStr));
            overdue.setTime(overdue.getTime() + 48000);
            activateCtxQue.add(new AbstractMap.SimpleEntry<>(overdue, contextChange));
        }
        if(InHotArea_B(longitude, latitude)){
            ContextChange contextChange = new ContextChange();
            contextChange.setChange_type(ContextChange.Change_Type.ADDITION);
            contextChange.setPattern_id("pat_B");
            contextChange.setContext(context);
            contextChange.setTimeStamp(simpleDateFormat.parse(context.getCtx_fields().get(timestampStr)).getTime());
            changeList.add(contextChange);
            Date overdue = simpleDateFormat.parse(context.getCtx_fields().get(timestampStr));
            overdue.setTime(overdue.getTime() + 48000);
            activateCtxQue.add(new AbstractMap.SimpleEntry<>(overdue, contextChange));
        }
        if(InHotArea_C(longitude, latitude)){
            ContextChange contextChange = new ContextChange();
            contextChange.setChange_type(ContextChange.Change_Type.ADDITION);
            contextChange.setPattern_id("pat_C");
            contextChange.setContext(context);
            contextChange.setTimeStamp(simpleDateFormat.parse(context.getCtx_fields().get(timestampStr)).getTime());
            changeList.add(contextChange);
            Date overdue = simpleDateFormat.parse(context.getCtx_fields().get(timestampStr));
            overdue.setTime(overdue.getTime() + 48000);
            activateCtxQue.add(new AbstractMap.SimpleEntry<>(overdue, contextChange));
        }
        if(InHotArea_D(longitude, latitude)){
            ContextChange contextChange = new ContextChange();
            contextChange.setChange_type(ContextChange.Change_Type.ADDITION);
            contextChange.setPattern_id("pat_D");
            contextChange.setContext(context);
            contextChange.setTimeStamp(simpleDateFormat.parse(context.getCtx_fields().get(timestampStr)).getTime());
            changeList.add(contextChange);
            Date overdue = simpleDateFormat.parse(context.getCtx_fields().get(timestampStr));
            overdue.setTime(overdue.getTime() + 48000);
            activateCtxQue.add(new AbstractMap.SimpleEntry<>(overdue, contextChange));
        }
        if(InHotArea_E(longitude, latitude)){
            ContextChange contextChange = new ContextChange();
            contextChange.setChange_type(ContextChange.Change_Type.ADDITION);
            contextChange.setPattern_id("pat_E");
            contextChange.setContext(context);
            contextChange.setTimeStamp(simpleDateFormat.parse(context.getCtx_fields().get(timestampStr)).getTime());
            changeList.add(contextChange);
            Date overdue = simpleDateFormat.parse(context.getCtx_fields().get(timestampStr));
            overdue.setTime(overdue.getTime() + 48000);
            activateCtxQue.add(new AbstractMap.SimpleEntry<>(overdue, contextChange));
        }
        if(InHotArea_F(longitude, latitude)){
            ContextChange contextChange = new ContextChange();
            contextChange.setChange_type(ContextChange.Change_Type.ADDITION);
            contextChange.setPattern_id("pat_F");
            contextChange.setContext(context);
            contextChange.setTimeStamp(simpleDateFormat.parse(context.getCtx_fields().get(timestampStr)).getTime());
            changeList.add(contextChange);
            Date overdue = simpleDateFormat.parse(context.getCtx_fields().get(timestampStr));
            overdue.setTime(overdue.getTime() + 48000);
            activateCtxQue.add(new AbstractMap.SimpleEntry<>(overdue, contextChange));
        }
        if(InHotArea_G(longitude, latitude)){
            ContextChange contextChange = new ContextChange();
            contextChange.setChange_type(ContextChange.Change_Type.ADDITION);
            contextChange.setPattern_id("pat_G");
            contextChange.setContext(context);
            contextChange.setTimeStamp(simpleDateFormat.parse(context.getCtx_fields().get(timestampStr)).getTime());
            changeList.add(contextChange);
            Date overdue = simpleDateFormat.parse(context.getCtx_fields().get(timestampStr));
            overdue.setTime(overdue.getTime() + 48000);
            activateCtxQue.add(new AbstractMap.SimpleEntry<>(overdue, contextChange));
        }
        if(InHotArea_H(longitude, latitude)){
            ContextChange contextChange = new ContextChange();
            contextChange.setChange_type(ContextChange.Change_Type.ADDITION);
            contextChange.setPattern_id("pat_H");
            contextChange.setContext(context);
            contextChange.setTimeStamp(simpleDateFormat.parse(context.getCtx_fields().get(timestampStr)).getTime());
            changeList.add(contextChange);
            Date overdue = simpleDateFormat.parse(context.getCtx_fields().get(timestampStr));
            overdue.setTime(overdue.getTime() + 48000);
            activateCtxQue.add(new AbstractMap.SimpleEntry<>(overdue, contextChange));
        }
        if(InHotArea_I(longitude, latitude)){
            ContextChange contextChange = new ContextChange();
            contextChange.setChange_type(ContextChange.Change_Type.ADDITION);
            contextChange.setPattern_id("pat_I");
            contextChange.setContext(context);
            contextChange.setTimeStamp(simpleDateFormat.parse(context.getCtx_fields().get(timestampStr)).getTime());
            changeList.add(contextChange);
            Date overdue = simpleDateFormat.parse(context.getCtx_fields().get(timestampStr));
            overdue.setTime(overdue.getTime() + 48000);
            activateCtxQue.add(new AbstractMap.SimpleEntry<>(overdue, contextChange));
        }
    }

}
