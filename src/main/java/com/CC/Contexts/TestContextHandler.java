package com.CC.Contexts;

import com.CC.Patterns.PatternHandler;

import java.util.List;
import java.util.StringTokenizer;

public class TestContextHandler extends ContextHandler{

    public TestContextHandler(PatternHandler patternHandler) {
        super(patternHandler);
    }

    @Override
    public void generateChanges(String line, List<ContextChange> changeList) throws Exception {
        if(line == null || line.equals(""))
            return;
        StringTokenizer stringTokenizer = new StringTokenizer(line, ",");
        String changeType = stringTokenizer.nextToken();
        String patternId = stringTokenizer.nextToken();

        String ctxStr = stringTokenizer.nextToken();
        String ctx_id = ctxStr.substring(0, ctxStr.indexOf("{"));
        Context testContext = new Context();
        testContext.setCtx_id(ctx_id);
        String fields = ctxStr.substring(ctxStr.indexOf("{") + 1, ctxStr.indexOf("}"));
        stringTokenizer = new StringTokenizer(fields, ";");
        while(stringTokenizer.hasMoreTokens()){
            String field = stringTokenizer.nextToken();
            testContext.getCtx_fields().put(field.substring(0,field.indexOf(":")), field.substring(field.indexOf(":") + 1));
        }

        ContextChange addChange = new ContextChange();
        addChange.setChange_type(changeType.equals("+") ? ContextChange.Change_Type.ADDITION : ContextChange.Change_Type.DELETION);
        addChange.setPattern_id(patternId);
        addChange.setContext(testContext);
        addChange.setTimeStamp(0L);
        changeList.add(addChange);
    }
}
