import java.util.Map;

public class Bfunction {

    public void test(String funcName){
        System.out.println(funcName);
    }

    public boolean bfunc(String funcName, Map<String, Map<String, String>> vcMap) throws Exception {
        switch (funcName){
            case "sz_loc_range": return funcSZLocRange(vcMap);
            case "sz_spd_range": return funcSZSpdRange(vcMap);
            case "sz_drc_range": return funcSZDrcRange(vcMap);
            case "sz_lon_range": return funcSZLonRange(vcMap);
            case "sz_lat_range": return funcSZLatRange(vcMap);
            case "sz_id_format": return funcSZIdFormat(vcMap);
            default: throw new Exception();
        }
    }

    // The longitude and latitude should be in [112, 116] and [20, 24], respectively
    private boolean funcSZLocRange(Map<String, Map<String, String>> vcMap)  {
        double lon = Double.parseDouble(vcMap.get("v1").get("longitude"));
        double lat = Double.parseDouble(vcMap.get("v1").get("latitude"));
        return !(lon < 112.0) && !(lon > 116.0) && !(lat < 20.0) && !(lat > 24.0);
    }

    // The speed should be in range [0, 100] km/h
    private boolean funcSZSpdRange(Map<String, Map<String, String>> vcMap) {
        int speed = Integer.parseInt(vcMap.get("v1").get("speed"));
        return speed >= 0 && speed <= 100;
    }

    // The direction should be in range [0, 360] degrees
    private boolean funcSZDrcRange(Map<String, Map<String, String>> vcMap) {
        int direction = Integer.parseInt(vcMap.get("v1").get("direction"));
        return direction >= 0 && direction <= 360;
    }

    // The longitude should be in range [112, 116]
    private boolean funcSZLonRange(Map<String, Map<String, String>> vcMap) {
        double lon = Double.parseDouble(vcMap.get("v1").get("longitude"));
        return lon >= 112.0 && lon <= 116.0;
    }

    // The latitude should be in range [20, 24]
    private boolean funcSZLatRange(Map<String, Map<String, String>> vcMap) {
        double lat = Double.parseDouble(vcMap.get("v2").get("latitude"));
        return lat >= 20.0 && lat <= 24.0;
    }

    // The taxi ID should follow the format: B followed by alphanumeric characters
    private boolean funcSZIdFormat(Map<String, Map<String, String>> vcMap) {
        String taxiId = vcMap.get("v1").get("taxiId");
        if (taxiId == null || taxiId.isEmpty()) {
            return false;
        }
        // Check if ID starts with 'B' and has at least one more character
        return taxiId.startsWith("B") && taxiId.length() == 6 && (taxiId.endsWith("0") || taxiId.endsWith("1"));
    }

}