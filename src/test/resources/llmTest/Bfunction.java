import java.util.Map;

import cn.edu.nju.ics.spar.cc.IoC.InfuseService;
import cn.edu.nju.ics.spar.cc.Services.LLMService;

public class Bfunction {
    
    @InfuseService
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
        return id.endsWith("1");
    }

    private boolean funcExprCheck(Map<String, Map<String, String>> vcMap) throws Exception {
        // Ask LLM asynchronously - returns true as placeholder, actual result determined later
        // return llm.askAsync("Is 1 + 1 = 1? return 'true' or 'false' only.");
        return llm.ask("Is 1 + 1 = 1? return 'true' or 'false' only.");
    }
}
