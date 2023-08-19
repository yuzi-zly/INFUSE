package com.CC.Patterns;

import com.CC.Util.Loggable;

import java.util.LinkedHashMap;

public abstract class PatternHandler implements Loggable {
    private final LinkedHashMap<String, Pattern> patternMap;

    public PatternHandler(){
        patternMap = new LinkedHashMap<>();
    }

    public LinkedHashMap<String, Pattern> getPatternMap() {
        return patternMap;
    }

    public abstract void buildPatterns(String patternFile);

//    public void buildPatterns(String patternFile) {
//        try(InputStream inputStream = Files.newInputStream(Paths.get(patternFile))){
//            SAXReader saxReader = new SAXReader();
//            Document document = saxReader.read(inputStream);
//            List<Element> patternElements = document.getRootElement().elements();
//            for(Element patternElement :  patternElements){
//                List<Element> labelElements = patternElement.elements();
//                assert labelElements.size() == 2 || labelElements.size() == 3;
//                Pattern pattern = new Pattern();
//                //patternId
//                assert labelElements.get(0).getName().equals("id");
//                pattern.setPatternId(labelElements.get(0).getText());
//                //freshness
//                assert labelElements.get(1).getName().equals("freshness");
//                List<Element> freshnessElements = labelElements.get(1).elements();
//                assert freshnessElements.size() == 2;
//                assert freshnessElements.get(0).getName().equals("type");
//                assert freshnessElements.get(1).getName().equals("value");
//                pattern.setFreshnessType(FreshnessType.valueOf(freshnessElements.get(0).getText()));
//                pattern.setFreshnessValue(freshnessElements.get(1).getText());
//                //matcher (optional)
//                if(labelElements.size() == 3){
//                    assert labelElements.get(2).getName().equals("matcher");
//                    List<Element> matcherElements = labelElements.get(2).elements();
//                    assert matcherElements.get(0).getName().equals("type");
//                    String matcherType = matcherElements.get(0).getText();
//                    if(matcherType.equals("primaryKey")){
//                        assert matcherElements.get(1).getName().equals("primaryKey");
//                        PrimaryKeyMatcher primaryKeyMatcher = new PrimaryKeyMatcher(matcherElements.get(1).getText());
//                        assert matcherElements.get(2).getName().equals("optionalValueList");
//                        List<Element> optionalValueElements = matcherElements.get(2).elements();
//                        for(Element optionalValueElement : optionalValueElements){
//                            assert optionalValueElement.getName().equals("value");
//                            primaryKeyMatcher.addOptionalValue(optionalValueElement.getText());
//                        }
//                        pattern.setMatcher(primaryKeyMatcher);
//                    }
//                    else if(matcherType.equals("function")){
//                        assert matcherElements.get(1).getName().equals("functionName");
//                        FunctionMatcher functionMatcher = new FunctionMatcher(matcherElements.get(1).getText(), mfuncInstance);
//                        //extraArgumentList (optional)
//                        if(matcherElements.size() == 3){
//                            assert matcherElements.get(2).getName().equals("extraArgumentList");
//                            List<Element> extraArgElements = matcherElements.get(2).elements();
//                            for(Element extraArgElement : extraArgElements){
//                                assert extraArgElement.getName().equals("argument");
//                                functionMatcher.addExtraArg(extraArgElement.getText());
//                            }
//                        }
//                        pattern.setMatcher(functionMatcher);
//                    }
//                    else{
//                        assert false;
//                    }
//                }
//                patternMap.put(pattern.getPatternId(), pattern);
//            }
//        }
//        catch (DocumentException | IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

}
