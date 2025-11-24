import java.util.Map;

import cn.edu.nju.ics.spar.cc.IoC.InfuseResource;
import cn.edu.nju.ics.spar.cc.Services.LLMService;

public class Bfunction {
    
    @InfuseResource
    private LLMService llm;

    public boolean bfunc(String funcName, Map<String, Map<String, String>> vcMap) throws Exception {
        switch (funcName){
            case "taxiId_check": return funcIdCheck(vcMap);
            case "expr_check": return funcExprCheck(vcMap);
            default: throw new Exception();
        }
    }

    private boolean funcIdCheck(Map<String, Map<String, String>> vcMap) throws Exception {
        String id = vcMap.get("v1").get("taxiId");
        return id.endsWith("0");
    }

    private boolean funcExprCheck(Map<String, Map<String, String>> vcMap) throws Exception {
        // Ask LLM asynchronously - returns true as placeholder, actual result determined later
        if (llm != null) {
            // return llm.askAsync("Is 1 + 1 = 1?");
            return llm.ask("Is 1 + 1 = 1?");
        }
        return false;
    }
}
