package com.CC.Patterns;

import com.CC.Contexts.Context;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.List;

public class TestPatternHandler extends PatternHandler{
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
            if(Elabels.size() != 1){
                throw new Exception("[CCE] impossible pattern format.");
            }
            //id
            temppattern.setPattern_id(Elabels.get(0).getText());
            //add to hashmap
            if(this.getPatternMap().containsKey(Elabels.get(0).getText())){
                throw new Exception("[CCE] not unique pattern_id.");
            }
            this.getPatternMap().put(Elabels.get(0).getText(), temppattern);
        }
    }

    @Override
    public void outputPatterns() {
        for(Pattern pattern : this.getPatternMap().values()){
            System.out.println(pattern);
        }
    }

    @Override
    public boolean ctxPatternMatched(Context context, Pattern pattern) {
        return false;
    }
}
