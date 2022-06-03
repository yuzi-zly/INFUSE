package com.CC.Constraints;

import com.CC.Constraints.Formulas.*;
import com.CC.Constraints.Runtime.RuntimeNode;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class RuleHandler {
    private final List<Rule> ruleList;

    public RuleHandler() {
        this.ruleList = new ArrayList<>();
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

    private Formula resolveFormula(Element Eformula, Set<String> relatedPatterns, Map<String, Formula> patToFormula,
                                   Map<String, Set<RuntimeNode>> patToRunTimeNode, int depth) throws Exception {
        Formula retformula = null;
        switch (Eformula.getName()){
            case "forall":{
                FForall tempforall = new FForall(Eformula.attributeValue("var"), Eformula.attributeValue("in"));
                // forall has only one kid
                tempforall.setSubformula(resolveFormula(Eformula.elements().get(0), relatedPatterns, patToFormula, patToRunTimeNode, depth + 1));
                relatedPatterns.add(Eformula.attributeValue("in"));
                patToFormula.put(Eformula.attributeValue("in"), tempforall);
                patToRunTimeNode.put(Eformula.attributeValue("in"), new HashSet<>());
                retformula = tempforall;
                break;
            }
            case "exists":{
                FExists tempexists = new FExists(Eformula.attributeValue("var"), Eformula.attributeValue("in"));
                // exists has only one kid
                tempexists.setSubformula(resolveFormula(Eformula.elements().get(0), relatedPatterns, patToFormula, patToRunTimeNode, depth + 1));
                relatedPatterns.add(Eformula.attributeValue("in"));
                patToFormula.put(Eformula.attributeValue("in"), tempexists);
                patToRunTimeNode.put(Eformula.attributeValue("in"), new HashSet<>());
                retformula = tempexists;
                break;
            }
            case "and":{
                FAnd tempand = new FAnd();
                // and has two kids
                tempand.replaceSubformula(0, resolveFormula(Eformula.elements().get(0), relatedPatterns, patToFormula, patToRunTimeNode, depth + 1));
                tempand.replaceSubformula(1, resolveFormula(Eformula.elements().get(1), relatedPatterns, patToFormula, patToRunTimeNode, depth + 1));
                retformula = tempand;
                break;
            }
            case "or" :{
                FOr tempor = new FOr();
                // or has two kids
                tempor.replaceSubformula(0, resolveFormula(Eformula.elements().get(0), relatedPatterns, patToFormula, patToRunTimeNode, depth + 1));
                tempor.replaceSubformula(1, resolveFormula(Eformula.elements().get(1), relatedPatterns, patToFormula, patToRunTimeNode, depth + 1));
                retformula = tempor;
                break;
            }
            case "implies" :{
                FImplies tempimplies = new FImplies();
                // implies has two kids
                tempimplies.replaceSubformula(0, resolveFormula(Eformula.elements().get(0), relatedPatterns, patToFormula, patToRunTimeNode, depth + 1));
                tempimplies.replaceSubformula(1, resolveFormula(Eformula.elements().get(1), relatedPatterns, patToFormula, patToRunTimeNode, depth + 1));
                retformula = tempimplies;
                break;
            }
            case "not" :{
                FNot tempnot = new FNot();
                // not has only one kid
                tempnot.setSubformula(resolveFormula(Eformula.elements().get(0), relatedPatterns, patToFormula, patToRunTimeNode, depth + 1));
                retformula = tempnot;
                break;
            }
            case "bfunc" :{
                FBfunc tempbfunc = new FBfunc(Eformula.attributeValue("name"));
                // bfunc has several params
                List<Element> Eparamlist = Eformula.elements();
                for(Element Eparam : Eparamlist){
                    tempbfunc.addParam(Eparam.attributeValue("pos"), Eparam.attributeValue("var"), Eparam.attributeValue("field"));
                }
                retformula = tempbfunc;
                break;
            }
            default:
                throw new Exception("[CCE] impossible formula name" + Eformula.getName());
        }

        return retformula;
    }

    public void buildRules(String filename) throws Exception {
        Logger logger = Logger.getGlobal();

        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(new File(filename));
        // 获取根元素 Rules
        Element Erules = document.getRootElement();
        // 获取所有子元素 rule
        List<Element> Erulelist = Erules.elements();

        for(Element Erule: Erulelist){
            List<Element> Enodelist = Erule.elements();
            if(Enodelist.size() != 2){
                // id + formula
                logger.warning("[CCE] rule does not have 2 root node but " + Enodelist.size());
                continue;
            }

            Rule newrule = new Rule(Enodelist.get(0).getText());
            // formula
            Element Eformula =  Enodelist.get(1).elements().get(0);
            newrule.setFormula(resolveFormula(Eformula, newrule.getRelatedPatterns(), newrule.getPatToFormula(), newrule.getPatToRuntimeNode(), 0));
            setPatWithDepth(newrule.getFormula(), newrule.getPatToDepth(), newrule.getDepthToPat());

            ruleList.add(newrule);
        }
    }

    public  void outputRules(){
        for(Rule rule: ruleList){
            rule.Output();
            System.out.println();
        }
    }

    public  List<Rule> getRuleList() {
        return ruleList;
    }
}
