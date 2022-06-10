package com.CC.Patterns;

import com.CC.Contexts.Context;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.List;

public class TaxiPatternHandler extends PatternHandler{

    public static final String freshnessStr = "freshness";
    public static final String categoryStr = "category";
    public static final String subjectStr = "subject";
    public static final String predicateStr = "predicate";
    public static final String objectStr = "object";
    public static final String siteStr = "site";

    @Override
    public void buildPatterns(String patternFile) throws Exception {
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(new File(patternFile));
        // 获取根元素 patterns
        Element Epatterns = document.getRootElement();
        // 获取所有子元素 pattern
        List<Element> Epatternlist = Epatterns.elements();

        for(Element Epattern: Epatternlist){
            Pattern temppattern = new Pattern();
            List<Element> Elabels = Epattern.elements();
            if(Elabels.size() != 7){
                throw new Exception("[CCE] impossible pattern format.");
            }
            //id
            temppattern.setPattern_id(Elabels.get(0).getText());
            //freshness
            assert Elabels.get(1).getName().equals(freshnessStr);
            temppattern.getPattern_fields().put(freshnessStr, Elabels.get(1).getText());
            //category
            assert Elabels.get(2).getName().equals(categoryStr);
            temppattern.getPattern_fields().put(categoryStr, Elabels.get(2).getText());
            //subject
            assert Elabels.get(3).getName().equals(subjectStr);
            temppattern.getPattern_fields().put(subjectStr, Elabels.get(3).getText());
            //predicate
            assert Elabels.get(4).getName().equals(predicateStr);
            temppattern.getPattern_fields().put(predicateStr, Elabels.get(4).getText());
            //object
            assert Elabels.get(5).getName().equals(objectStr);
            temppattern.getPattern_fields().put(objectStr, Elabels.get(5).getText());
            //site
            assert Elabels.get(6).getName().equals(siteStr);
            temppattern.getPattern_fields().put(siteStr, Elabels.get(6).getText());
            //add to hashmap
            if(this.getPatternMap().containsKey(Elabels.get(0).getText())){
                throw new Exception("[CCE] not unique pattern_id.");
            }
            this.getPatternMap().put(Elabels.get(0).getText(), temppattern);
        }
    }

    @Override
    public void outputPatterns() {
        //TODO()
    }

    @Override
    public boolean ctxPatternMatched(Context context, Pattern pattern) {
        if(! context.getCtx_fields().get(categoryStr).equals(pattern.getPattern_fields().get(categoryStr))){
            return false;
        }
        else if(! pattern.getPattern_fields().get(subjectStr).equals("any") && ! context.getCtx_fields().get(subjectStr).equals(pattern.getPattern_fields().get(subjectStr))){
            return false;
        }
        else if(! pattern.getPattern_fields().get(predicateStr).equals("any") && !context.getCtx_fields().get(predicateStr).equals(pattern.getPattern_fields().get(predicateStr))){
            return false;
        }
        else if(! pattern.getPattern_fields().get(siteStr).equals("any") && ! context.getCtx_fields().get(siteStr).equals(pattern.getPattern_fields().get(siteStr))){
            return false;
        }
        return true;
    }
}
