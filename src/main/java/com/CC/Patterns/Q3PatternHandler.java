package com.CC.Patterns;

import com.CC.Contexts.Context;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Q3PatternHandler extends PatternHandler{
    @Override
    public void buildPatterns(String patternFile) {
        try(InputStream inputStream = Files.newInputStream(Paths.get(patternFile))){
            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(inputStream);
            List<Element> patternElements = document.getRootElement().elements();
            for(Element patternElement :  patternElements){
                List<Element> labelElements = patternElement.elements();
                assert labelElements.size() == 5;
                Pattern pattern = new Pattern();
                //patternId
                assert labelElements.get(0).getName().equals("id");
                pattern.setPatternId(labelElements.get(0).getText());
                //freshnessType
                assert labelElements.get(1).getName().equals("freshnessType");
                pattern.getPatternFields().put("freshnessType", labelElements.get(1).getText());
                //freshness
                assert labelElements.get(2).getName().equals("freshness");
                pattern.getPatternFields().put("freshness", labelElements.get(2).getText());
                //entryFlag
                assert labelElements.get(3).getName().equals("entryFlag");
                pattern.getPatternFields().put("entryFlag", labelElements.get(3).getText());
                //vlp
                assert labelElements.get(4).getName().equals("vlp");
                pattern.getPatternFields().put("vlp", labelElements.get(4).getText());
                patternMap.put(pattern.getPatternId(), pattern);
            }
        }
        catch (DocumentException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean normalVLP(String vlp){
        if(vlp.length() < 4){
            return false;
        }
        else{
            char c2 = vlp.charAt(2);
            char c3 = vlp.charAt(3);
            if((c2 >= '0' && c2 <= '9') || (c2 >= 'A' && c2 <= 'Z' && c2 != 'I' && c2 != 'O')){
                return (c3 >= '0' && c3 <= '9') || (c3 >= 'A' && c3 <= 'Z' && c3 != 'I' && c3 != 'O');
            }
            return false;
        }
    }

    public boolean groupMatched(Context context, Pattern pattern){
        //vlpshort是vlp的第3，4位
        String vlpshort = pattern.getPatternFields().get("vlp");

        String vlp = context.getCtx_fields().get("vlp");
        if(normalVLP(vlp)){
            return vlp.substring(2,4).equals(vlpshort);
        }
        else{
            return vlpshort.equals("others");
        }
    }
}
