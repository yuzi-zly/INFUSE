package com.CC.Patterns;

import com.CC.Patterns.matcher.FunctionMatcher;
import com.CC.Patterns.matcher.PrimaryKeyMatcher;
import com.CC.Patterns.types.FreshnessType;
import com.CC.Util.Loggable;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;

public class PatternHandler implements Loggable {
    private final LinkedHashMap<String, Pattern> patternMap;

    public PatternHandler(){
        patternMap = new LinkedHashMap<>();
    }

    public LinkedHashMap<String, Pattern> getPatternMap() {
        return patternMap;
    }

    public void buildPatterns(String patternFile, String mfuncFile){
        Object mfuncInstance = loadMfuncFile(mfuncFile);
        if(mfuncInstance != null){
            logger.info("Load mfunc file successfully");
        }
        try {
            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(new File(patternFile));
            List<Element> patternElements = document.getRootElement().elements();
            for(Element patternElement :  patternElements){
                List<Element> labelElements = patternElement.elements();
                assert labelElements.size() == 2 || labelElements.size() == 3;
                Pattern pattern = new Pattern();
                //patternId
                assert labelElements.get(0).getName().equals("id");
                pattern.setPatternId(labelElements.get(0).getText());
                //freshness
                assert labelElements.get(1).getName().equals("freshness");
                List<Element> freshnessElements = labelElements.get(1).elements();
                assert freshnessElements.size() == 2;
                assert freshnessElements.get(0).getName().equals("type");
                assert freshnessElements.get(1).getName().equals("value");
                pattern.setFreshnessType(FreshnessType.valueOf(freshnessElements.get(0).getText()));
                pattern.setFreshnessValue(freshnessElements.get(1).getText());
                //matcher (optional)
                if(labelElements.size() == 3){
                    assert labelElements.get(2).getName().equals("matcher");
                    List<Element> matcherElements = labelElements.get(2).elements();
                    assert matcherElements.get(0).getName().equals("type");
                    String matcherType = matcherElements.get(0).getText();
                    if(matcherType.equals("primaryKey")){
                        assert matcherElements.get(1).getName().equals("primaryKey");
                        PrimaryKeyMatcher primaryKeyMatcher = new PrimaryKeyMatcher(matcherElements.get(1).getText());
                        assert matcherElements.get(2).getName().equals("optionalValueList");
                        List<Element> optionalValueElements = matcherElements.get(2).elements();
                        for(Element optionalValueElement : optionalValueElements){
                            assert optionalValueElement.getName().equals("value");
                            primaryKeyMatcher.addOptionalValue(optionalValueElement.getText());
                        }
                        pattern.setMatcher(primaryKeyMatcher);
                    }
                    else if(matcherType.equals("function")){
                        assert matcherElements.get(1).getName().equals("functionName");
                        FunctionMatcher functionMatcher = new FunctionMatcher(matcherElements.get(1).getText(), mfuncInstance);
                        //extraArgumentList (optional)
                        if(matcherElements.size() == 3){
                            assert matcherElements.get(2).getName().equals("extraArgumentList");
                            List<Element> extraArgElements = matcherElements.get(2).elements();
                            for(Element extraArgElement : extraArgElements){
                                assert extraArgElement.getName().equals("argument");
                                functionMatcher.addExtraArg(extraArgElement.getText());
                            }
                        }
                        pattern.setMatcher(functionMatcher);
                    }
                    else{
                        assert false;
                    }
                }
                patternMap.put(pattern.getPatternId(), pattern);
            }
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    private Object loadMfuncFile(String mfuncFile) {
        if(mfuncFile == null || mfuncFile.equals(""))
            return null;
        Object mfuncInstance;
        Path mfuncPath = Paths.get(mfuncFile).toAbsolutePath();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{mfuncPath.getParent().toFile().toURI().toURL()})) {
            Class<?> clazz = classLoader.loadClass(mfuncPath.getFileName().toString().split("\\.")[0]);
            Constructor<?> constructor = clazz.getConstructor();
            mfuncInstance = constructor.newInstance();
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return mfuncInstance;
    }
}
