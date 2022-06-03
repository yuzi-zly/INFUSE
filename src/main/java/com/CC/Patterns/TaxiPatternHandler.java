package com.CC.Patterns;

import com.CC.Contexts.Context;
import com.CC.Contexts.TaxiContext;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.List;

public class TaxiPatternHandler extends PatternHandler{
    @Override
    public void buildPatterns(String patternFile) throws Exception {
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(new File(patternFile));
        // 获取根元素 patterns
        Element Epatterns = document.getRootElement();
        // 获取所有子元素 pattern
        List<Element> Epatternlist = Epatterns.elements();

        for(Element Epattern: Epatternlist){
            TaxiPattern temppattern = new TaxiPattern();
            List<Element> Elabels = Epattern.elements();
            if(Elabels.size() != 7){
                throw new Exception("[BuildRegularPatterns] impossible pattern format.");
            }
            //id
            temppattern.setPattern_id(Elabels.get(0).getText());
            //freshness
            temppattern.setFreshness(Elabels.get(1).getText());
            //category
            temppattern.setCategory(Elabels.get(2).getText());
            //subject
            temppattern.setSubject(Elabels.get(3).getText());
            //predicate
            temppattern.setPredicate(Elabels.get(4).getText());
            //object
            temppattern.setObject(Elabels.get(5).getText());
            //site
            temppattern.setSite(Elabels.get(6).getText());
            //add to hashmap
            if(this.getPatternMap().containsKey(Elabels.get(0).getText())){
                throw new Exception("[CCE] not unique pattern_id.");
            }
            this.getPatternMap().put(Elabels.get(0).getText(), temppattern);
        }
    }

    @Override
    public void OutputPatterns() {
        //TODO()
    }

    @Override
    public boolean ctxPatternMatched(Context context, Pattern pattern) {
        assert context instanceof TaxiContext;
        assert pattern instanceof TaxiPattern;
        if(!((TaxiContext) context).getCtx_category().equals(((TaxiPattern) pattern).getCategory()))
            return false;
        else if(!((TaxiPattern) pattern).getSubject().equals("any") && !((TaxiContext) context).getCtx_subject().equals(((TaxiPattern) pattern).getSubject()))
            return false;
        else if(!((TaxiPattern) pattern).getPredicate().equals("any") && !((TaxiContext) context).getCtx_predicate().equals(((TaxiPattern) pattern).getPredicate()))
            return false;
        else if(!((TaxiPattern) pattern).getSite().equals("any") && !((TaxiContext) context).getCtx_site().equals(((TaxiPattern) pattern).getSite()))
            return false;
        return true;
    }
}
