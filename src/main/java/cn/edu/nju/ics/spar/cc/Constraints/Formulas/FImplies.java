package cn.edu.nju.ics.spar.cc.Constraints.Formulas;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import cn.edu.nju.ics.spar.cc.Constraints.Rules.Rule;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.LGUtils;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.Link;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.RuntimeNode;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.RuntimeNode.AsyncTruthValue;
import cn.edu.nju.ics.spar.cc.Constraints.Runtime.AsyncEvaluationResult;
import cn.edu.nju.ics.spar.cc.Contexts.ContextChange;
import cn.edu.nju.ics.spar.cc.Middleware.Checkers.Checker;
import cn.edu.nju.ics.spar.cc.Middleware.Schedulers.Scheduler;
import cn.edu.nju.ics.spar.cc.Util.InfuseException;

public class FImplies extends Formula{
    private final Formula[] subformulas;

    //constructors
    public FImplies(){
        this.setFormula_type(Formula_Type.IMPLIES);
        this.subformulas = new Formula[2];
        this.setAffected(false);
    }

    public void replaceSubformula(int pos, Formula formula){
        if( pos < 0 || pos >= 2){
            throw new InfuseException("[CCE] position is out of range: " + pos);
        }
        this.subformulas[pos] = formula;
    }

    @Override
    public void setFormula_type(Formula.Formula_Type formula_type) {
        super.setFormula_type(formula_type);
    }

    @Override
    public Formula.Formula_Type getFormula_type() {
        return super.getFormula_type();
    }

    public Formula[] getSubformulas() {
        return subformulas;
    }

    @Override
    public void output(int offset) {
        for(int i = 0; i < offset; ++i)
            System.out.print(" ");
        System.out.println("implies  affected: " + this.isAffected());
        subformulas[0].output(offset+2);
        subformulas[1].output(offset+2);
    }

    @Override
    public Formula formulaClone() {
        return new FImplies();
    }

    //S-condition
    @Override
    public void deriveIncPlusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incPlusSet) {
        this.subformulas[0].deriveIncMinusSet(incPlusSet);
        this.subformulas[1].deriveIncPlusSet(incPlusSet);
    }

    @Override
    public void deriveIncMinusSet(Set<Map.Entry<ContextChange.Change_Type, String>> incMinusSet) {
        this.subformulas[0].deriveIncPlusSet(incMinusSet);
        this.subformulas[1].deriveIncMinusSet(incMinusSet);
    }

    //C-condition
    @Override
    public boolean evaluationAndEqualSideEffect(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler) {
        if(var != null){
            curNode.getVarEnv().remove(var);
            curNode.getVarEnv().put(var, addChange.getContext());
        }

        boolean result;
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        boolean tempresult = runtimeNode1.getFormula().evaluationAndEqualSideEffect(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], var, delChange, addChange, canConcurrent, scheduler);
        result = tempresult;

        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        tempresult = runtimeNode2.getFormula().evaluationAndEqualSideEffect(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], var, delChange, addChange, canConcurrent, scheduler);
        result = result && tempresult;

        boolean newTruth = !runtimeNode1.isTruth() || runtimeNode2.isTruth();
        curNode.setOptTruth(curNode.isTruth());
        curNode.setTruth(newTruth);

        return result;
    }

    @Override
    public void sideEffectResolution(RuntimeNode curNode, Formula originFormula, String var, ContextChange delChange, ContextChange addChange, boolean canConcurrent, Scheduler scheduler) {
        if(var != null){
            curNode.setTruth(curNode.isOptTruth());
            curNode.setOptTruth(false);
            curNode.getVarEnv().remove(var);
            curNode.getVarEnv().put(var, delChange.getContext());
        }
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().sideEffectResolution(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], var, delChange, addChange, canConcurrent, scheduler);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().sideEffectResolution(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], var, delChange, addChange, canConcurrent, scheduler);
    }

    //DIS
    @Override
    public void deriveRCRESets(boolean from) {
        this.subformulas[0].deriveRCRESets(!from);
        this.subformulas[1].deriveRCRESets(from);
    }

    //PCC
    @Override
    public boolean updateAffectedWithOneChange(ContextChange contextChange, Checker checker) {
        boolean result0 = this.subformulas[0].updateAffectedWithOneChange(contextChange, checker);
        boolean result1 = this.subformulas[1].updateAffectedWithOneChange(contextChange, checker);
        result0 = result0 || result1;
        this.setAffected(result0);
        return result0;
    }

    //PCCM && CPCC
    @Override
    public boolean updateAffectedWithChanges(Checker checker) {
        boolean result0 = this.subformulas[0].updateAffectedWithChanges(checker);
        boolean result1 = this.subformulas[1].updateAffectedWithChanges(checker);
        result0 = result0 || result1;
        this.setAffected(result0);
        return result0;
    }
    @Override
    public void cleanAffected() {
        this.setAffected(false);
        this.subformulas[0].cleanAffected();
        this.subformulas[1].cleanAffected();
    }

    //CPCC_NB
    @Override
    public void updateCanConcurrent_INFUSE(boolean canConcurrent, Rule rule, Checker checker) {
        if(canConcurrent){
            this.subformulas[0].updateCanConcurrent_INFUSE(true, rule, checker);
            this.subformulas[1].updateCanConcurrent_INFUSE(true, rule, checker);
        }
    }

    @Override
    public void cleanAffectedAndCanConcurrent() {
        this.setAffected(false);
        this.subformulas[0].cleanAffectedAndCanConcurrent();
        this.subformulas[1].cleanAffectedAndCanConcurrent();
    }

    //MG
    @Override
    public void taintSCCT(RuntimeNode curNode, Formula originFormula, Set<RuntimeNode> substantialNodes) {
        substantialNodes.add(curNode);
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        if(curNode.isTruth()){
            if(!runtimeNode1.isTruth()){
                runtimeNode1.getFormula().taintSCCT(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], substantialNodes);
            }
            if(runtimeNode2.isTruth()){
                runtimeNode2.getFormula().taintSCCT(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], substantialNodes);
            }
        }
        else{
            runtimeNode1.getFormula().taintSCCT(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], substantialNodes);
            runtimeNode2.getFormula().taintSCCT(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], substantialNodes);
        }
    }

    /*
                                            ECC PCC
                                         */
    @Override
    public void createBranches_ECCPCC(String rule_id, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = new RuntimeNode(((FImplies)originFormula).getSubformulas()[0]);
        runtimeNode1.setDepth(curNode.getDepth() + 1);
        runtimeNode1.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode1);
        //递归调用
        runtimeNode1.getFormula().createBranches_ECCPCC(rule_id, runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], checker);
        //分支2
        RuntimeNode runtimeNode2 = new RuntimeNode(((FImplies) originFormula).getSubformulas()[1]);
        runtimeNode2.setDepth(curNode.getDepth() + 1);
        runtimeNode2.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode2);
        //递归调用
        runtimeNode2.getFormula().createBranches_ECCPCC(rule_id, runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], checker);
    }

    /*
        ECC
     */
    @Override
    public boolean truthEvaluation_ECC(RuntimeNode curNode, Formula originFormula, Checker checker)  {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        boolean result = !runtimeNode1.getFormula().truthEvaluation_ECC(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], checker);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        boolean tempresult = runtimeNode2.getFormula().truthEvaluation_ECC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], checker);
        result = result || tempresult;
        curNode.setTruth(result);
        return result;
    }

    @Override
    public Set<Link> linksGeneration_ECC(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker)  {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();

        if(!checker.isMG() || !curNode.isTruth()) {
            // case 1: !MG --> all
            // case 3: MG && false --> all
            if (runtimeNode1.isTruth()) {
                if (runtimeNode2.isTruth()) {
                    runtimeNode1.getFormula().linksGeneration_ECC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    result.addAll(runtimeNode2.getFormula().linksGeneration_ECC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker));
                } else {
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ECC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_ECC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
                }
            } else {
                if (runtimeNode2.isTruth()) {
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ECC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_ECC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.flipSet(ret1));
                    result.addAll(ret2);
                } else {
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ECC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    runtimeNode2.getFormula().linksGeneration_ECC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.flipSet(ret1));
                }
            }
        }
        else{
            // case 2: MG && true --> left false, right true
            if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ECC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_ECC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                result.addAll(lgUtils.flipSet(ret1));
                result.addAll(ret2);
            }
            else if(!runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ECC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                result.addAll(lgUtils.flipSet(ret1));
            }
            else{
                result.addAll(runtimeNode2.getFormula().linksGeneration_ECC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker));
            }
        }
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    // Async-aware ECC: truth evaluation with short-circuit optimization
    // A -> B is equivalent to !A || B
    @Override
    public AsyncEvaluationResult truthEvaluationAsync_ECC(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode antecedent = curNode.getChildren().get(0);  // A
        RuntimeNode consequent = curNode.getChildren().get(1);  // B

        // Evaluate antecedent (A)
        AsyncEvaluationResult resultA = antecedent.getFormula().truthEvaluationAsync_ECC(
            antecedent, ((FImplies)originFormula).getSubformulas()[0], checker);

        // Short-circuit: If A is FALSE, then !A is TRUE, so (A -> B) is TRUE
        // Don't evaluate consequent B at all to avoid generating unnecessary async calls
        if (resultA.getTruthValue() == AsyncTruthValue.DETERMINED_FALSE) {
            curNode.setAsyncTruthValue(AsyncTruthValue.DETERMINED_TRUE);
            // Only return antecedent's pending nodes (consequent is never evaluated)
            return AsyncEvaluationResult.determinedTrue();
        }

        // Only evaluate consequent B if antecedent A was not FALSE
        AsyncEvaluationResult resultB = consequent.getFormula().truthEvaluationAsync_ECC(
            consequent, ((FImplies)originFormula).getSubformulas()[1], checker);

        // Short-circuit: If B is TRUE, entire IMPLIES is TRUE (A -> TRUE = TRUE)
        if (resultB.getTruthValue() == AsyncTruthValue.DETERMINED_TRUE) {
            curNode.setAsyncTruthValue(AsyncTruthValue.DETERMINED_TRUE);
            // Entire IMPLIES is determined as TRUE, don't pass up any pending nodes
            return AsyncEvaluationResult.determinedTrue();
        }

        // A is not FALSE (already short-circuited), B is not TRUE: determine final result
        AsyncTruthValue finalTruth;
        if (resultA.getTruthValue() == AsyncTruthValue.DETERMINED_TRUE &&
            resultB.getTruthValue() == AsyncTruthValue.DETERMINED_FALSE) {
            // TRUE -> FALSE = FALSE
            finalTruth = AsyncTruthValue.DETERMINED_FALSE;
        } else {
            // At least one is PENDING
            finalTruth = AsyncTruthValue.PENDING_ASYNC;
        }

        curNode.setAsyncTruthValue(finalTruth);

        // Combine pending requests from both children (only needed if final result is PENDING)
        Map<String, RuntimeNode> allPendingNodes = new HashMap<>();
        allPendingNodes.putAll(resultA.getPendingNodes());
        allPendingNodes.putAll(resultB.getPendingNodes());

        return new AsyncEvaluationResult(finalTruth, allPendingNodes);
    }

    // Update truth value after executeAllAsync (propagate from children)
    @Override
    public void updateTruthValueAsync(RuntimeNode curNode, Formula originFormula) {
        // Only update if current node is PENDING (short-circuited nodes don't need update)
        if (curNode.getAsyncTruthValue() != AsyncTruthValue.PENDING_ASYNC) {
            return;
        }
        
        RuntimeNode antecedent = curNode.getChildren().get(0);
        RuntimeNode consequent = curNode.getChildren().get(1);
        
        // Recursively update children first
        antecedent.getFormula().updateTruthValueAsync(antecedent, ((FImplies)originFormula).getSubformulas()[0]);
        consequent.getFormula().updateTruthValueAsync(consequent, ((FImplies)originFormula).getSubformulas()[1]);
        
        // Recalculate current node's truth value (A -> B = !A || B)
        AsyncTruthValue resultA = antecedent.getAsyncTruthValue();
        AsyncTruthValue resultB = consequent.getAsyncTruthValue();
        
        // After executeAllAsync, children should be TRUE or FALSE (no PENDING)
        AsyncTruthValue finalResult;
        if (resultA == AsyncTruthValue.DETERMINED_FALSE || resultB == AsyncTruthValue.DETERMINED_TRUE) {
            finalResult = AsyncTruthValue.DETERMINED_TRUE;
        } else {
            // A is TRUE and B is FALSE
            assert resultA == AsyncTruthValue.DETERMINED_TRUE && resultB == AsyncTruthValue.DETERMINED_FALSE;
            finalResult = AsyncTruthValue.DETERMINED_FALSE;
        }
        
        curNode.setAsyncTruthValue(finalResult);
    }

    // Async-aware ECC: links generation (no MG support, simplified)
    // A -> B is equivalent to !A || B
    @Override
    public Set<Link> linksGenerationAsync_ECC(RuntimeNode curNode, Formula originFormula, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode antecedent = curNode.getChildren().get(0);  // A
        RuntimeNode consequent = curNode.getChildren().get(1);  // B
        LGUtils lgUtils = new LGUtils();
        
        AsyncTruthValue statusA = antecedent.getAsyncTruthValue();
        AsyncTruthValue statusB = consequent.getAsyncTruthValue();
        
        if (statusA == AsyncTruthValue.DETERMINED_FALSE) {
            // Due to short-circuit, consequent was never evaluated (statusB should be null)
            assert statusB == null : "consequent should not have been evaluated when antecedent is FALSE due to short-circuit";
            // Only generate links from the antecedent which caused the short-circuit (flip !A)
            Set<Link> retA = antecedent.getFormula().linksGenerationAsync_ECC(
                antecedent, ((FImplies)originFormula).getSubformulas()[0], checker);
            result.addAll(lgUtils.flipSet(retA));
        } else if (statusA == AsyncTruthValue.DETERMINED_TRUE) {
            // statusA is TRUE, so consequent must have been evaluated
            assert statusB != null : "consequent should have been evaluated when antecedent is TRUE";

            if (statusB == AsyncTruthValue.DETERMINED_TRUE) {
                // T -> T: Only consequent matters
                Set<Link> retB = consequent.getFormula().linksGenerationAsync_ECC(
                    consequent, ((FImplies)originFormula).getSubformulas()[1], checker);
                result.addAll(retB);
            } else {
                // T -> F: Cartesian product of flip(!A) and B
                assert statusB == AsyncTruthValue.DETERMINED_FALSE;
                Set<Link> retA = antecedent.getFormula().linksGenerationAsync_ECC(
                    antecedent, ((FImplies)originFormula).getSubformulas()[0], checker);
                Set<Link> retB = consequent.getFormula().linksGenerationAsync_ECC(
                    consequent, ((FImplies)originFormula).getSubformulas()[1], checker);
                result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(retA), retB));
            }
        } else {
            // statusA is PENDING_ASYNC, which means it wasn't updated in updateTruthValueAsync
            // This only happens when the IMPLIES node returned DETERMINED_TRUE (so no update needed)
            // IMPLIES returns TRUE when antecedent is FALSE
            // Since statusA is PENDING (not FALSE), consequent must be TRUE
            assert statusA == AsyncTruthValue.PENDING_ASYNC;
            assert statusB == AsyncTruthValue.DETERMINED_TRUE;
            // PENDING -> T: Only consequent matters
            Set<Link> retB = consequent.getFormula().linksGenerationAsync_ECC(
                consequent, ((FImplies)originFormula).getSubformulas()[1], checker);
            result.addAll(retB);
        }
        
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        PCC
     */

    @Override
    public void modifyBranch_PCC(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().modifyBranch_PCC(rule_id, runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], contextChange, checker);
        //分支2
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().modifyBranch_PCC(rule_id, runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, checker);
    }

    @Override
    public boolean truthEvaluation_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        //curNode.update == false
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else if(((FImplies)originFormula).getSubformulas()[0].isAffected()){
            boolean result = runtimeNode1.getFormula().truthEvaluation_PCC(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], contextChange, checker);
            result = !result || runtimeNode2.isTruth();
            curNode.setTruth(result);
            return result;
        }
        else{
            boolean result = runtimeNode2.getFormula().truthEvaluation_PCC(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], contextChange, checker);
            result = !runtimeNode1.isTruth() || result;
            curNode.setTruth(result);
            return result;
        }
    }

    @Override
    public Set<Link> linksGeneration_PCC(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();

        if(!checker.isMG()){
            // case 1: !MG --> all
            if(!originFormula.isAffected()){
                return curNode.getLinks();
            }
            else if(((FImplies)originFormula).getSubformulas()[0].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), runtimeNode2.getLinks()));
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(lgUtils.flipSet(ret1));
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(lgUtils.flipSet(ret1));
                    }
                }
            }
            else{
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret2,lgUtils.flipSet(runtimeNode1.getLinks())));
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(lgUtils.flipSet(runtimeNode1.getLinks()));
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.flipSet(runtimeNode1.getLinks()));
                    }
                }
            }
        }
        else if(curNode.isTruth()){
            // case 2: MG &&  true --> left false, right true
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                        result.addAll(ret2);
                    }
                    else if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                        result.addAll(lgUtils.flipSet(ret1));
                        result.addAll(ret2);
                    }
                    else{
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                        result.addAll(lgUtils.flipSet(ret1));
                    }
                }
            }
            else if(((FImplies)originFormula).getSubformulas()[0].isAffected()){
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else{
                        ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(ret2);
                }
                else if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else{
                        ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(lgUtils.flipSet(ret1));
                    result.addAll(ret2);
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    result.addAll(lgUtils.flipSet(ret1));
                }
            }
            else{
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    result.addAll(ret2);
                }
                else if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    Set<Link> ret1;
                    // check whether runtimeNode1.links reusable
                    if(prevSubstantialNodes.contains(runtimeNode1)){
                        ret1 = runtimeNode1.getLinks();
                    }
                    else{
                        ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(lgUtils.flipSet(ret1));
                    result.addAll(ret2);
                }
                else {
                    Set<Link> ret1;
                    // check whether runtimeNode1.links reusable
                    if(prevSubstantialNodes.contains(runtimeNode1)){
                        ret1 = runtimeNode1.getLinks();
                    }
                    else{
                        ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(lgUtils.flipSet(ret1));
                }
            }
        }
        else{
            // case 3: MG && false --> all
            assert runtimeNode1.isTruth() && !runtimeNode2.isTruth();
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
                }
            }
            else if(((FImplies)originFormula).getSubformulas()[0].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                // check whether runtimeNode2.links reusable
                Set<Link> ret2;
                if(prevSubstantialNodes.contains(runtimeNode2)){
                    ret2 = runtimeNode2.getLinks();
                }
                else{
                    ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                }
                result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
            }
            else{
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                // check whether runtimeNode1.links reusable
                Set<Link> ret1;
                if (prevSubstantialNodes.contains(runtimeNode1)) {
                    ret1 = runtimeNode1.getLinks();
                }
                else{
                    ret1 = runtimeNode1.getFormula().linksGeneration_PCC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                }
                result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
            }
        }


        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        ConC
     */
    @Override
    public void createBranches_ConC(String rule_id, RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = new RuntimeNode(((FImplies)originFormula).getSubformulas()[0]);
        runtimeNode1.setDepth(curNode.getDepth() + 1);
        runtimeNode1.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode1);
        //递归调用
        runtimeNode1.getFormula().createBranches_ConC(rule_id, runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], canConcurrent, checker);
        //分支2
        RuntimeNode runtimeNode2 = new RuntimeNode(((FImplies) originFormula).getSubformulas()[1]);
        runtimeNode2.setDepth(curNode.getDepth() + 1);
        runtimeNode2.getVarEnv().putAll(curNode.getVarEnv());
        curNode.getChildren().add(runtimeNode2);
        //递归调用
        runtimeNode2.getFormula().createBranches_ConC(rule_id, runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], canConcurrent, checker);
    }

    @Override
    public boolean truthEvaluation_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        boolean result = !runtimeNode1.getFormula().truthEvaluation_ConC(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], canConcurrent, checker);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        boolean tempresult = runtimeNode2.getFormula().truthEvaluation_ConC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], canConcurrent, checker);
        result = result || tempresult;
        curNode.setTruth(result);
        return result;
    }

    @Override
    public Set<Link> linksGeneration_ConC(RuntimeNode curNode, Formula originFormula, boolean canConcurrent, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();
        if(!checker.isMG() || !curNode.isTruth()) {
            // case 1: !MG --> all
            // case 3: MG && false --> all
            if(runtimeNode1.isTruth()){
                if(runtimeNode2.isTruth()){
                    runtimeNode1.getFormula().linksGeneration_ConC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], canConcurrent, prevSubstantialNodes, checker);
                    result = runtimeNode2.getFormula().linksGeneration_ConC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], canConcurrent, prevSubstantialNodes, checker);
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ConC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], canConcurrent, prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_ConC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], canConcurrent, prevSubstantialNodes, checker);
                    result = lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2);
                }
            }
            else{
                if(runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ConC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], canConcurrent, prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_ConC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], canConcurrent, prevSubstantialNodes, checker);
                    result = lgUtils.flipSet(ret1);
                    result.addAll(ret2);
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ConC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], canConcurrent, prevSubstantialNodes, checker);
                    runtimeNode2.getFormula().linksGeneration_ConC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], canConcurrent, prevSubstantialNodes, checker);
                    result = lgUtils.flipSet(ret1);
                }
            }
        }
        else{
            // case 2: MG && true --> left false, right true
            if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ConC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], canConcurrent, prevSubstantialNodes, checker);
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_ConC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], canConcurrent, prevSubstantialNodes, checker);
                result = lgUtils.flipSet(ret1);
                result.addAll(ret2);
            }
            else if(!runtimeNode1.isTruth() && !runtimeNode2.isTruth()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_ConC(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], canConcurrent, prevSubstantialNodes, checker);
                result = lgUtils.flipSet(ret1);
            }
            else{
                result = runtimeNode2.getFormula().linksGeneration_ConC(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], canConcurrent, prevSubstantialNodes, checker);
            }
        }
        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        PCCM
     */

    @Override
    public void modifyBranch_PCCM(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().modifyBranch_PCCM(rule_id, runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], contextChange, checker);
        //分支2
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().modifyBranch_PCCM(rule_id, runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, checker);
    }

    @Override
    public boolean truthEvaluation_PCCM(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else if(((FImplies)originFormula).getSubformulas()[0].isAffected() && !((FImplies)originFormula).getSubformulas()[1].isAffected()){
            boolean result = runtimeNode1.getFormula().truthEvaluation_PCCM(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], checker);
            result = !result || runtimeNode2.isTruth();
            curNode.setTruth(result);
            return result;
        }
        else if(!((FImplies)originFormula).getSubformulas()[0].isAffected() && ((FImplies)originFormula).getSubformulas()[1].isAffected()){
            boolean result = runtimeNode2.getFormula().truthEvaluation_PCCM(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], checker);
            result = !runtimeNode1.isTruth() || result;
            curNode.setTruth(result);
            return result;
        }
        else{
            boolean result = runtimeNode1.getFormula().truthEvaluation_PCCM(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], checker);
            boolean tempresult = runtimeNode2.getFormula().truthEvaluation_PCCM(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], checker);
            result = !result || tempresult;
            curNode.setTruth(result);
            return result;
        }
    }

    @Override
    public Set<Link> linksGeneration_PCCM(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();

        if(!checker.isMG()){
            // case 1: !MG --> all
            if(!originFormula.isAffected()){
                return curNode.getLinks();
            }
            else if(((FImplies)originFormula).getSubformulas()[0].isAffected() && !((FImplies)originFormula).getSubformulas()[1].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), runtimeNode2.getLinks()));
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(lgUtils.flipSet(ret1));
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(lgUtils.flipSet(ret1));
                    }
                }
            }
            else if(!((FImplies)originFormula).getSubformulas()[0].isAffected() && ((FImplies)originFormula).getSubformulas()[1].isAffected()){
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret2,lgUtils.flipSet(runtimeNode1.getLinks())));
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(lgUtils.flipSet(runtimeNode1.getLinks()));
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.flipSet(runtimeNode1.getLinks()));
                    }
                }
            }
            else{
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret2,lgUtils.flipSet(ret1)));
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(lgUtils.flipSet(ret1));
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.flipSet(ret1));
                    }
                }
            }
        }
        else if(curNode.isTruth()){
            // case 2: MG && true --> left false, right true
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)) {
                    return curNode.getLinks();
                }
                else{
                    if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                        result.addAll(ret2);
                    }
                    else if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                        result.addAll(lgUtils.flipSet(ret1));
                        result.addAll(ret2);
                    }
                    else{
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                        result.addAll(lgUtils.flipSet(ret1));
                    }
                }
            }
            else if(((FImplies)originFormula).getSubformulas()[0].isAffected() && !((FImplies)originFormula).getSubformulas()[1].isAffected()){
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else {
                        ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    }
                    result.addAll(ret2);
                }
                else if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else {
                        ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    }
                    result.addAll(lgUtils.flipSet(ret1));
                    result.addAll(ret2);
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.flipSet(ret1));
                }
            }
            else if(!((FImplies)originFormula).getSubformulas()[0].isAffected() && ((FImplies)originFormula).getSubformulas()[1].isAffected()){
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(ret2);
                }
                else if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    // check whether runtimeNode1.links reusable
                    Set<Link> ret1;
                    if(prevSubstantialNodes.contains(runtimeNode1)){
                        ret1 = runtimeNode1.getLinks();
                    }
                    else {
                     ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    }
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.flipSet(ret1));
                    result.addAll(ret2);
                }
                else{
                    // check whether runtimeNode1.links reusable
                    Set<Link> ret1;
                    if(prevSubstantialNodes.contains(runtimeNode1)){
                        ret1 = runtimeNode1.getLinks();
                    }
                    else {
                        ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    }
                    result.addAll(lgUtils.flipSet(ret1));
                }
            }
            else{
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(ret2);
                }
                else if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.flipSet(ret1));
                    result.addAll(ret2);
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.flipSet(ret1));
                }
            }
        }
        else{
            // case 3: MG && false --> all
            assert runtimeNode1.isTruth() && !runtimeNode2.isTruth();
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)) {
                    return curNode.getLinks();
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
                }
            }
            else if(((FImplies)originFormula).getSubformulas()[0].isAffected() && !((FImplies)originFormula).getSubformulas()[1].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                // check whether runtimeNode2.links reusable
                Set<Link> ret2;
                if(prevSubstantialNodes.contains(runtimeNode2)){
                    ret2 = runtimeNode2.getLinks();
                }
                else{
                    ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                }
                result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
            }
            else if(!((FImplies)originFormula).getSubformulas()[0].isAffected() && ((FImplies)originFormula).getSubformulas()[1].isAffected()){
                // check whether runtimeNode1.links reusable
                Set<Link> ret1;
                if(prevSubstantialNodes.contains(runtimeNode1)){
                    ret1 = runtimeNode1.getLinks();
                }
                else {
                    ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                }
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
            }
            else{
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_PCCM(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_PCCM(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
            }
        }

        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        CPCC_NB
     */
    @Override
    public void createBranches_INFUSE(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = new RuntimeNode(((FImplies)originFormula).getSubformulas()[0]);
        runtimeNode1.setDepth(curNode.getDepth() + 1);
        runtimeNode1.getVarEnv().putAll(curNode.getVarEnv());
        runtimeNode1.setParent(curNode);
        curNode.getChildren().add(runtimeNode1);
        //递归调用
        runtimeNode1.getFormula().createBranches_INFUSE(rule, runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], checker);
        //分支2
        RuntimeNode runtimeNode2 = new RuntimeNode(((FImplies) originFormula).getSubformulas()[1]);
        runtimeNode2.setDepth(curNode.getDepth() + 1);
        runtimeNode2.getVarEnv().putAll(curNode.getVarEnv());
        runtimeNode2.setParent(curNode);
        curNode.getChildren().add(runtimeNode2);
        //递归调用
        runtimeNode2.getFormula().createBranches_INFUSE(rule, runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], checker);

    }

    @Override
    public void modifyBranch_INFUSE(Rule rule, RuntimeNode curNode, Formula originFormula, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().modifyBranch_INFUSE(rule, runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], checker);
        //分支2
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().modifyBranch_INFUSE(rule, runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], checker);
    }

    @Override
    public boolean truthEvaluationCom_INFUSE(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        boolean result = !runtimeNode1.getFormula().truthEvaluationCom_INFUSE(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], checker);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        boolean tempresult = runtimeNode2.getFormula().truthEvaluationCom_INFUSE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], checker);
        result = result || tempresult;
        curNode.setTruth(result);
        curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
        return result;
    }

    @Override
    public boolean truthEvaluationPar_INFUSE(RuntimeNode curNode, Formula originFormula, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else if(((FImplies)originFormula).getSubformulas()[0].isAffected() && !((FImplies)originFormula).getSubformulas()[1].isAffected()){
            boolean result = runtimeNode1.getFormula().truthEvaluationPar_INFUSE(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], checker);
            result = !result || runtimeNode2.isTruth();
            curNode.setTruth(result);
            curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
            return result;
        }
        else if(!((FImplies)originFormula).getSubformulas()[0].isAffected() && ((FImplies)originFormula).getSubformulas()[1].isAffected()){
            boolean result = runtimeNode2.getFormula().truthEvaluationPar_INFUSE(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], checker);
            result = !runtimeNode1.isTruth() || result;
            curNode.setTruth(result);
            curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
            return result;
        }
        else {
            boolean result = runtimeNode1.getFormula().truthEvaluationPar_INFUSE(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], checker);
            boolean tempresult = runtimeNode2.getFormula().truthEvaluationPar_INFUSE(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], checker);
            result = !result || tempresult;
            curNode.setTruth(result);
            curNode.setVirtualTruth(result ? RuntimeNode.Virtual_Truth_Type.TRUE : RuntimeNode.Virtual_Truth_Type.FALSE);
            return result;
        }
    }

    @Override
    public Set<Link> linksGeneration_INFUSE(RuntimeNode curNode, Formula originFormula, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();

        if(!checker.isMG()){
            // case 1: !MG --> all
            if(!originFormula.isAffected()){
                return curNode.getLinks();
            }
            else if(((FImplies)originFormula).getSubformulas()[0].isAffected() && !((FImplies)originFormula).getSubformulas()[1].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), runtimeNode2.getLinks()));
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(lgUtils.flipSet(ret1));
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(lgUtils.flipSet(ret1));
                    }
                }
            }
            else if(!((FImplies)originFormula).getSubformulas()[0].isAffected() && ((FImplies)originFormula).getSubformulas()[1].isAffected()){
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret2,lgUtils.flipSet(runtimeNode1.getLinks())));
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(lgUtils.flipSet(runtimeNode1.getLinks()));
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.flipSet(runtimeNode1.getLinks()));
                    }
                }
            }
            else{
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret2,lgUtils.flipSet(ret1)));
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(lgUtils.flipSet(ret1));
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.flipSet(ret1));
                    }
                }
            }
        }
        else if(curNode.isTruth()){
            // case 2: MG && true --> left false, right true
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)) {
                    return curNode.getLinks();
                }
                else{
                    if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                        result.addAll(ret2);
                    }
                    else if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                        result.addAll(lgUtils.flipSet(ret1));
                        result.addAll(ret2);
                    }
                    else{
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                        result.addAll(lgUtils.flipSet(ret1));
                    }
                }
            }
            else if(((FImplies)originFormula).getSubformulas()[0].isAffected() && !((FImplies)originFormula).getSubformulas()[1].isAffected()){
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else {
                        ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    }
                    result.addAll(ret2);
                }
                else if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else {
                        ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    }
                    result.addAll(lgUtils.flipSet(ret1));
                    result.addAll(ret2);
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.flipSet(ret1));
                }
            }
            else if(!((FImplies)originFormula).getSubformulas()[0].isAffected() && ((FImplies)originFormula).getSubformulas()[1].isAffected()){
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(ret2);
                }
                else if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    // check whether runtimeNode1.links reusable
                    Set<Link> ret1;
                    if(prevSubstantialNodes.contains(runtimeNode1)){
                        ret1 = runtimeNode1.getLinks();
                    }
                    else {
                        ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    }
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.flipSet(ret1));
                    result.addAll(ret2);
                }
                else{
                    // check whether runtimeNode1.links reusable
                    Set<Link> ret1;
                    if(prevSubstantialNodes.contains(runtimeNode1)){
                        ret1 = runtimeNode1.getLinks();
                    }
                    else {
                        ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    }
                    result.addAll(lgUtils.flipSet(ret1));
                }
            }
            else{
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(ret2);
                }
                else if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.flipSet(ret1));
                    result.addAll(ret2);
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.flipSet(ret1));
                }
            }
        }
        else{
            // case 3: MG && false --> all
            assert runtimeNode1.isTruth() && !runtimeNode2.isTruth();
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)) {
                    return curNode.getLinks();
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                    result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
                }
            }
            else if(((FImplies)originFormula).getSubformulas()[0].isAffected() && !((FImplies)originFormula).getSubformulas()[1].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                // check whether runtimeNode2.links reusable
                Set<Link> ret2;
                if(prevSubstantialNodes.contains(runtimeNode2)){
                    ret2 = runtimeNode2.getLinks();
                }
                else{
                    ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                }
                result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
            }
            else if(!((FImplies)originFormula).getSubformulas()[0].isAffected() && ((FImplies)originFormula).getSubformulas()[1].isAffected()){
                // check whether runtimeNode1.links reusable
                Set<Link> ret1;
                if(prevSubstantialNodes.contains(runtimeNode1)){
                    ret1 = runtimeNode1.getLinks();
                }
                else {
                    ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                }
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
            }
            else{
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_INFUSE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], prevSubstantialNodes, checker);
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_INFUSE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], prevSubstantialNodes, checker);
                result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
            }
        }

        curNode.setLinks(result);
        return curNode.getLinks();
    }

    /*
        CPCC_BASE
     */

    @Override
    public void modifyBranch_BASE(String rule_id, RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        //分支1
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        runtimeNode1.getFormula().modifyBranch_BASE(rule_id, runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], contextChange, checker);
        //分支2
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        runtimeNode2.getFormula().modifyBranch_BASE(rule_id, runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, checker);
    }

    @Override
    public boolean truthEvaluation_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, Checker checker) {
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        //curNode.update == false
        if(!originFormula.isAffected()){
            return curNode.isTruth();
        }
        else if(((FImplies)originFormula).getSubformulas()[0].isAffected()){
            boolean result = runtimeNode1.getFormula().truthEvaluation_BASE(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], contextChange, checker);
            result = !result || runtimeNode2.isTruth();
            curNode.setTruth(result);
            return result;
        }
        else{
            boolean result = runtimeNode2.getFormula().truthEvaluation_BASE(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], contextChange, checker);
            result = !runtimeNode1.isTruth() || result;
            curNode.setTruth(result);
            return result;
        }
    }

    @Override
    public Set<Link> linksGeneration_BASE(RuntimeNode curNode, Formula originFormula, ContextChange contextChange, final Set<RuntimeNode> prevSubstantialNodes, Checker checker) {
        Set<Link> result = new HashSet<>();
        RuntimeNode runtimeNode1 = curNode.getChildren().get(0);
        RuntimeNode runtimeNode2 = curNode.getChildren().get(1);
        LGUtils lgUtils = new LGUtils();

        if(!checker.isMG()){
            // case 1: !MG --> all
            if(!originFormula.isAffected()){
                return curNode.getLinks();
            }
            else if(((FImplies)originFormula).getSubformulas()[0].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), runtimeNode2.getLinks()));
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(lgUtils.flipSet(ret1));
                        result.addAll(runtimeNode2.getLinks());
                    }
                    else{
                        result.addAll(lgUtils.flipSet(ret1));
                    }
                }
            }
            else{
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                if(runtimeNode1.isTruth()){
                    if(runtimeNode2.isTruth()){
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.cartesianSet(ret2,lgUtils.flipSet(runtimeNode1.getLinks())));
                    }
                }
                else{
                    if(runtimeNode2.isTruth()){
                        result.addAll(lgUtils.flipSet(runtimeNode1.getLinks()));
                        result.addAll(ret2);
                    }
                    else{
                        result.addAll(lgUtils.flipSet(runtimeNode1.getLinks()));
                    }
                }
            }
        }
        else if(curNode.isTruth()){
            // case 2: MG &&  true --> left false, right true
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                        result.addAll(ret2);
                    }
                    else if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                        Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                        result.addAll(lgUtils.flipSet(ret1));
                        result.addAll(ret2);
                    }
                    else{
                        Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                        result.addAll(lgUtils.flipSet(ret1));
                    }
                }
            }
            else if(((FImplies)originFormula).getSubformulas()[0].isAffected()){
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else{
                        ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(ret2);
                }
                else if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    // check whether runtimeNode2.links reusable
                    Set<Link> ret2;
                    if(prevSubstantialNodes.contains(runtimeNode2)){
                        ret2 = runtimeNode2.getLinks();
                    }
                    else{
                        ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(lgUtils.flipSet(ret1));
                    result.addAll(ret2);
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FImplies)originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    result.addAll(lgUtils.flipSet(ret1));
                }
            }
            else{
                if(runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    result.addAll(ret2);
                }
                else if(!runtimeNode1.isTruth() && runtimeNode2.isTruth()){
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FImplies)originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    Set<Link> ret1;
                    // check whether runtimeNode1.links reusable
                    if(prevSubstantialNodes.contains(runtimeNode1)){
                        ret1 = runtimeNode1.getLinks();
                    }
                    else{
                        ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(lgUtils.flipSet(ret1));
                    result.addAll(ret2);
                }
                else {
                    Set<Link> ret1;
                    // check whether runtimeNode1.links reusable
                    if(prevSubstantialNodes.contains(runtimeNode1)){
                        ret1 = runtimeNode1.getLinks();
                    }
                    else{
                        ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    }
                    result.addAll(lgUtils.flipSet(ret1));
                }
            }
        }
        else{
            // case 3: MG && false --> all
            assert runtimeNode1.isTruth() && !runtimeNode2.isTruth();
            if(!originFormula.isAffected()){
                // check whether curNode.links reusable
                if(prevSubstantialNodes.contains(curNode)){
                    return curNode.getLinks();
                }
                else{
                    Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                    Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                    result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
                }
            }
            else if(((FImplies)originFormula).getSubformulas()[0].isAffected()){
                Set<Link> ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                // check whether runtimeNode2.links reusable
                Set<Link> ret2;
                if(prevSubstantialNodes.contains(runtimeNode2)){
                    ret2 = runtimeNode2.getLinks();
                }
                else{
                    ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                }
                result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
            }
            else{
                Set<Link> ret2 = runtimeNode2.getFormula().linksGeneration_BASE(runtimeNode2, ((FImplies) originFormula).getSubformulas()[1], contextChange, prevSubstantialNodes, checker);
                // check whether runtimeNode1.links reusable
                Set<Link> ret1;
                if (prevSubstantialNodes.contains(runtimeNode1)) {
                    ret1 = runtimeNode1.getLinks();
                }
                else{
                    ret1 = runtimeNode1.getFormula().linksGeneration_BASE(runtimeNode1, ((FImplies) originFormula).getSubformulas()[0], contextChange, prevSubstantialNodes, checker);
                }
                result.addAll(lgUtils.cartesianSet(lgUtils.flipSet(ret1), ret2));
            }
        }


        curNode.setLinks(result);
        return curNode.getLinks();
    }
}

