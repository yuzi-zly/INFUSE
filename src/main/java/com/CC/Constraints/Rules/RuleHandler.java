package com.CC.Constraints.Rules;

import com.CC.Constraints.Formulas.*;
import com.CC.Constraints.Runtime.RuntimeNode;
import com.CC.Util.Loggable;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RuleHandler implements Loggable {

    private final Map<String, Rule> ruleMap;
    private final Map<String, Resolver> resolverMap;

    public RuleHandler() {
        this.ruleMap = new HashMap<>();
        this.resolverMap = new HashMap<>();
    }

    public void buildRules(String filename) throws Exception {
        try(InputStream inputStream = Files.newInputStream(Paths.get(filename))) {
            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(inputStream);
            List<Element> eRuleList = document.getRootElement().elements();
            for(Element eRule: eRuleList){
                List<Element> eLabelList = eRule.elements();
                assert eLabelList.size() == 2 || eLabelList.size() == 3;
                //id
                assert eLabelList.get(0).getName().equals("id");
                Rule newRule = new Rule(eLabelList.get(0).getText());
                // formula
                assert eLabelList.get(1).getName().equals("formula");
                Element eFormula =  eLabelList.get(1).elements().get(0);
                newRule.setFormula(resolveFormula(eFormula, newRule.getVarPatternMap(), newRule.getPatToFormula(), newRule.getPatToRuntimeNode(), 0));
                setPatWithDepth(newRule.getFormula(), newRule.getPatToDepth(), newRule.getDepthToPat());
                ruleMap.put(newRule.getRule_id(), newRule);
                // resolver
                if(eLabelList.size() == 3){
                    resolverMap.put(newRule.getRule_id(), buildResolver(eLabelList.get(2).elements()));
                }
            }
        }
    }

    private Formula resolveFormula(Element eFormula, Map<String, String> varPatternMap, Map<String, Formula> patToFormula,
                                   Map<String, Set<RuntimeNode>> patToRunTimeNode, int depth){
        Formula retFormula = null;
        switch (eFormula.getName()){
            case "forall":{
                FForall tmpForall = new FForall(eFormula.attributeValue("var"), eFormula.attributeValue("in"));
                // forall has only one kid
                tmpForall.setSubformula(resolveFormula(eFormula.elements().get(0), varPatternMap, patToFormula, patToRunTimeNode, depth + 1));
                varPatternMap.put(eFormula.attributeValue("var"), eFormula.attributeValue("in"));
                patToFormula.put(eFormula.attributeValue("in"), tmpForall);
                patToRunTimeNode.put(eFormula.attributeValue("in"), new HashSet<>());
                retFormula = tmpForall;
                break;
            }
            case "exists":{
                FExists tmpExists = new FExists(eFormula.attributeValue("var"), eFormula.attributeValue("in"));
                // exists has only one kid
                tmpExists.setSubformula(resolveFormula(eFormula.elements().get(0), varPatternMap, patToFormula, patToRunTimeNode, depth + 1));
                varPatternMap.put(eFormula.attributeValue("var"), eFormula.attributeValue("in"));
                patToFormula.put(eFormula.attributeValue("in"), tmpExists);
                patToRunTimeNode.put(eFormula.attributeValue("in"), new HashSet<>());
                retFormula = tmpExists;
                break;
            }
            case "and":{
                FAnd tmpAnd = new FAnd();
                // and has two kids
                tmpAnd.replaceSubformula(0, resolveFormula(eFormula.elements().get(0), varPatternMap, patToFormula, patToRunTimeNode, depth + 1));
                tmpAnd.replaceSubformula(1, resolveFormula(eFormula.elements().get(1), varPatternMap, patToFormula, patToRunTimeNode, depth + 1));
                retFormula = tmpAnd;
                break;
            }
            case "or" :{
                FOr tmpOr = new FOr();
                // or has two kids
                tmpOr.replaceSubformula(0, resolveFormula(eFormula.elements().get(0), varPatternMap, patToFormula, patToRunTimeNode, depth + 1));
                tmpOr.replaceSubformula(1, resolveFormula(eFormula.elements().get(1), varPatternMap, patToFormula, patToRunTimeNode, depth + 1));
                retFormula = tmpOr;
                break;
            }
            case "implies" :{
                FImplies tmpImplies = new FImplies();
                // implies has two kids
                tmpImplies.replaceSubformula(0, resolveFormula(eFormula.elements().get(0), varPatternMap, patToFormula, patToRunTimeNode, depth + 1));
                tmpImplies.replaceSubformula(1, resolveFormula(eFormula.elements().get(1), varPatternMap, patToFormula, patToRunTimeNode, depth + 1));
                retFormula = tmpImplies;
                break;
            }
            case "not" :{
                FNot tmpNot = new FNot();
                // not has only one kid
                tmpNot.setSubformula(resolveFormula(eFormula.elements().get(0), varPatternMap, patToFormula, patToRunTimeNode, depth + 1));
                retFormula = tmpNot;
                break;
            }
            case "bfunc" :{
                FBfunc tmpBfunc = new FBfunc(eFormula.attributeValue("name"));
                // bfunc has several params
                List<Element> paramElementList = eFormula.elements();
                for(Element paramElement : paramElementList){
                    tmpBfunc.addParam(paramElement.attributeValue("pos"), paramElement.attributeValue("var"));
                }
                retFormula = tmpBfunc;
                break;
            }
            default:
                assert false;
        }

        return retFormula;
    }

    private int setPatWithDepth(Formula formula, Map<String,Integer> patToDepth, Map<Integer, String> depthToPat){
        int maxDepth;
        switch (formula.getFormula_type()){
            case FORALL:
                maxDepth = setPatWithDepth(((FForall)formula).getSubformula(), patToDepth, depthToPat);
                patToDepth.put(((FForall)formula).getPattern_id(), maxDepth);
                depthToPat.put(maxDepth, ((FForall)formula).getPattern_id());
                return maxDepth + 1;
            case EXISTS:
                maxDepth = setPatWithDepth(((FExists)formula).getSubformula(), patToDepth, depthToPat);
                patToDepth.put(((FExists)formula).getPattern_id(), maxDepth);
                depthToPat.put(maxDepth, ((FExists)formula).getPattern_id());
                return maxDepth + 1;
            case AND:
                maxDepth = setPatWithDepth(((FAnd)formula).getSubformulas()[0], patToDepth, depthToPat);
                maxDepth = Math.max(maxDepth, setPatWithDepth(((FAnd)formula).getSubformulas()[1], patToDepth, depthToPat));
                return maxDepth + 1;
            case OR:
                maxDepth = setPatWithDepth(((FOr)formula).getSubformulas()[0], patToDepth, depthToPat);
                maxDepth = Math.max(maxDepth, setPatWithDepth(((FOr)formula).getSubformulas()[1], patToDepth, depthToPat));
                return maxDepth + 1;
            case IMPLIES:
                maxDepth = setPatWithDepth(((FImplies)formula).getSubformulas()[0], patToDepth, depthToPat);
                maxDepth = Math.max(maxDepth, setPatWithDepth(((FImplies)formula).getSubformulas()[1], patToDepth, depthToPat));
                return maxDepth + 1;
            case NOT:
                maxDepth = setPatWithDepth(((FNot)formula).getSubformula(), patToDepth, depthToPat);
                return maxDepth + 1;
            case BFUNC:
                return 1;
            default:
                return -1;
        }
    }

    private Resolver buildResolver(List<Element> resolverElements){
        Resolver resolver = new Resolver();
        //type
        assert resolverElements.get(0).getName().equals("type");
        resolver.setResolverType(ResolverType.valueOf(resolverElements.get(0).getText()));
        //variable
        assert resolverElements.get(1).getName().equals("variable");
        resolver.setVariable(resolverElements.get(1).getText());
        //fixingPairList
        if(resolverElements.size() == 3){
            assert resolverElements.get(2).getName().equals("fixingPairList");
            List<Element> fixingPairElements = resolverElements.get(2).elements();
            for(Element fixingPairElement : fixingPairElements){
                assert fixingPairElement.getName().equals("fixingPair");
                assert fixingPairElement.elements().get(0).getName().equals("field");
                assert fixingPairElement.elements().get(1).getName().equals("value");
                resolver.addFixingPair(fixingPairElement.elements().get(0).getText(), fixingPairElement.elements().get(1).getText());
            }
        }
        return resolver;
    }


    public Map<String, Rule> getRuleMap() {
        return ruleMap;
    }

    public Map<String, Resolver> getResolverMap() {
        return resolverMap;
    }
}
