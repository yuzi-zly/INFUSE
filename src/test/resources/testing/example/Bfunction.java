import java.util.Map;

public class Bfunction {

    public boolean bfunc(String bfunctionName, Map<String, Map<String, String>> params) {    //bfunc直接调这个接口就行
        boolean result=false;
        switch(bfunctionName){
            case "bfunc_1":{
                result=bfunc_1(params);
                break;
            }
            case "bfunc_2":{
                result=bfunc_2(params);
                break;
            }
            default:{
                assert false;
            }
        }
        return result;
    }
    private boolean bfunc_1(Map<String, Map<String,String>> params){
        assert params.size()==2;
        Map<String, String> param1 = params.get("v1");
        Map<String, String> param2 = params.get("v2");
        if(param1.get("key2").equals("3.14") && !param2.get("key4").equals("222")){
            return false;
        }
        else{
            return true;
        }
    }
    private boolean bfunc_2(Map<String, Map<String,String>> params){
        assert params.size()==1;
        Map<String,String> param1=params.get("v3");
        if(param1.get("key6").equals("A")){
            return true;
        }
        else{
            return false;
        }
    }

    public void end(){
        System.out.println("run end method.");
    }
}
