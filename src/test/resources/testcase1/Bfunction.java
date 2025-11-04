import java.util.Map;

public class Bfunction {

    public void test(String funcName){
        System.out.println(funcName);
    }

    public boolean bfunc(String funcName, Map<String, Map<String, String>> vcMap) throws Exception {
        switch (funcName){
            case "same": return funcSame(vcMap);
            case "sz_loc_range": return funcSZLocRange(vcMap);
            case "sz_loc_close": return funcSZLocClose(vcMap);
            case "sz_spd_close": return funcSZSpdClose(vcMap);
            case "sz_loc_dist": return funcSZLocDist(vcMap);
            default: throw new Exception();
        }
    }

    // Compares two field values and a constant string if any (should be identical)
    private boolean funcSame(Map<String, Map<String, String>> vcMap)  {
        String subject1 = vcMap.get("v1").get("taxiId");
        String subject2 = vcMap.get("v2").get("taxiId");
        return subject1.equals(subject2);
    }

    // The longitude and latitude should be in [112, 116] and [20, 24], respectively
    private boolean funcSZLocRange(Map<String, Map<String, String>> vcMap)  {
        double lon = Double.parseDouble(vcMap.get("v1").get("longitude"));
        double lat = Double.parseDouble(vcMap.get("v1").get("latitude"));
        return !(lon < 112.0) && !(lon > 116.0) && !(lat < 20.0) && !(lat > 24.0);
    }

    // The distance should be no more than 0.001 as 'close'
    private boolean funcSZLocClose(Map<String, Map<String, String>> vcMap) {
        double lon1 = Double.parseDouble(vcMap.get("v1").get("longitude"));
        double lat1 = Double.parseDouble(vcMap.get("v1").get("latitude"));
        double lon2 = Double.parseDouble(vcMap.get("v2").get("longitude"));
        double lat2 = Double.parseDouble(vcMap.get("v2").get("latitude"));

        double distSq = (lon2 - lon1) * (lon2 - lon1) + (lat2 - lat1) * (lat2 - lat1);
        return !(distSq > 0.001 * 0.001);
    }

    // The difference should be no more than 50 (as 'close')
    private boolean funcSZSpdClose(Map<String, Map<String, String>> vcMap)  {
        int speed1 = Integer.parseInt(vcMap.get("v1").get("speed"));
        int speed2 = Integer.parseInt(vcMap.get("v2").get("speed"));
        return Math.abs(speed2 - speed1) <= 50;
    }

    // The distance should be no more than 0.025 (assuming that the speed is no more than 200 km/h)
    private boolean funcSZLocDist(Map<String, Map<String, String>> vcMap) {
        double lon1 = Double.parseDouble(vcMap.get("v1").get("longitude"));
        double lat1 = Double.parseDouble(vcMap.get("v1").get("latitude"));
        double lon2 = Double.parseDouble(vcMap.get("v2").get("longitude"));
        double lat2 = Double.parseDouble(vcMap.get("v2").get("latitude"));

        double distSq = (lon2 - lon1) * (lon2 - lon1) + (lat2 - lat1) * (lat2 - lat1);
        return !(distSq > 0.025 * 0.025);
    }
}