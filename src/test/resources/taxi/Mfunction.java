import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Mfunction {

    public boolean mfunc(final String funcName, final Map<String, String> ctxFields, final List<String> extraArgumentList) throws Exception {
        if ("run_with_service".equals(funcName)) {
            return run_with_service(ctxFields, extraArgumentList);
        }
        else if("run_with_service_ending".equals(funcName)){
            return run_with_service_ending(ctxFields, extraArgumentList);
        }
        else if("run_with_service_hotArea".equals(funcName)){
            return run_with_service_hotArea(ctxFields, extraArgumentList);
        }
        else{
            throw new Exception("Illegal bfuncName");
        }
    }

    private boolean run_with_service(final Map<String, String> ctxFields, final List<String> extraArgumentList){
        return !ctxFields.get("status").equals("0");
    }

    private boolean run_with_service_ending(final Map<String, String> ctxFields, final List<String> extraArgumentList){
        return extraArgumentList.get(0).equals(ctxFields.get("taxiId").substring(ctxFields.get("taxiId").length() - 1));
    }

    private boolean run_with_service_hotArea(final Map<String, String> ctxFields, final List<String> extraArgumentList){
        switch (extraArgumentList.get(0)){
            case "A":
                return InHotArea_A(ctxFields.get("longitude"), ctxFields.get("latitude"));
            case "B":
                return InHotArea_B(ctxFields.get("longitude"), ctxFields.get("latitude"));
            case "C":
                return InHotArea_C(ctxFields.get("longitude"), ctxFields.get("latitude"));
            case "D":
                return InHotArea_D(ctxFields.get("longitude"), ctxFields.get("latitude"));
            case "E":
                return InHotArea_E(ctxFields.get("longitude"), ctxFields.get("latitude"));
            case "F":
                return InHotArea_F(ctxFields.get("longitude"), ctxFields.get("latitude"));
            case "G":
                return InHotArea_G(ctxFields.get("longitude"), ctxFields.get("latitude"));
            case "H":
                return InHotArea_H(ctxFields.get("longitude"), ctxFields.get("latitude"));
            case "I":
                return InHotArea_I(ctxFields.get("longitude"), ctxFields.get("latitude"));
        }
        return false;
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

}