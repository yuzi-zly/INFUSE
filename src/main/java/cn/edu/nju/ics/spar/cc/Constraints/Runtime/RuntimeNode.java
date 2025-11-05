package cn.edu.nju.ics.spar.cc.Constraints.Runtime;

import cn.edu.nju.ics.spar.cc.Constraints.Formulas.FBfunc;
import cn.edu.nju.ics.spar.cc.Constraints.Formulas.FExists;
import cn.edu.nju.ics.spar.cc.Constraints.Formulas.FForall;
import cn.edu.nju.ics.spar.cc.Constraints.Formulas.Formula;
import cn.edu.nju.ics.spar.cc.Contexts.Context;

import java.util.*;

public class RuntimeNode {

    private Formula formula;
    private int depth;
    private List<RuntimeNode> children;
    //from var to Context
    private HashMap<String, Context> varEnv;

    //checking results
    private boolean truth;
    private Set<Link> links;

    //for GEAS-opt
    private boolean optTruth;

    //for DIS
    public enum Virtual_Truth_Type {TRUE, FALSE, UNKNOWN};
    private Virtual_Truth_Type virtualTruth;
    private RuntimeNode parent;
    private final HashMap<Context, Virtual_Truth_Type> kidsVT; //only for forall and exists

    //constructor
    public RuntimeNode(Formula formula){
        //need to create a new formula
        this.formula = formula.formulaClone();
        this.depth = -1;
        this.children = new ArrayList<>();
        this.varEnv = new HashMap<>();
        this.parent = null;
        this.kidsVT = new HashMap<>();
        this.links = new HashSet<>();
        if(formula.getFormula_type() == Formula.Formula_Type.FORALL){
            this.setTruth(true);
            this.setOptTruth(true);
            this.setVirtualTruth(Virtual_Truth_Type.TRUE);
        }
        else{
            this.setTruth(false);
            this.setOptTruth(false);
            this.setVirtualTruth(Virtual_Truth_Type.FALSE);
        }
    }

    //getter
    public HashMap<Context, Virtual_Truth_Type> getKidsVT() {
        return kidsVT;
    }

    public RuntimeNode getParent() {
        return parent;
    }

    public Virtual_Truth_Type getVirtualTruth() {
        return virtualTruth;
    }

    public boolean isOptTruth() {
        return optTruth;
    }

    public Formula getFormula() {
        return formula;
    }

    public HashMap<String, Context> getVarEnv() {
        return varEnv;
    }

    public Set<Link> getLinks() {
        assert links != null;
        Set<Link> result = new HashSet<>();
        for(Link link : links){
            try {
                result.add(link.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public boolean isTruth() {
        return truth;
    }

    public int getDepth() {
        return depth;
    }

    public List<RuntimeNode> getChildren() {
        return children;
    }

    //setter
    public void setParent(RuntimeNode parent) {
        this.parent = parent;
    }

    public void setVirtualTruth(Virtual_Truth_Type virtualTruth) {
        this.virtualTruth = virtualTruth;
    }

    public void setOptTruth(boolean optTruth) {
        this.optTruth = optTruth;
    }

    public void setFormula(Formula formula) {
        this.formula = formula;
    }

    public void setLinks(Set<Link> links) {
        this.links = links;
    }

    public void setTruth(boolean truth) {
        this.truth = truth;
    }

    public void setVarEnv(HashMap<String, Context> varEnv) {
        this.varEnv = varEnv;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setChildren(List<RuntimeNode> children) {
        this.children = children;
    }

    //functional methods

    //DIS
    //子结点添加了一个变量赋值为context的结点
    public void vtPropagationAdd(Context context){
        Virtual_Truth_Type originVT = this.getVirtualTruth();
        //本结点必然成为UNKNOWN
        this.setVirtualTruth(Virtual_Truth_Type.UNKNOWN);
        //更新本结点的kidsVT
        this.getKidsVT().put(context, Virtual_Truth_Type.UNKNOWN);
        //如果原来的vt不是UNKNOWN，则需要向上传递影响
        if(originVT != Virtual_Truth_Type.UNKNOWN && this.parent != null){
            this.parent.vtPropagationUpdate(originVT, this);
        }
    }

    //子结点删除了一个变量赋值为context的结点，且删除时的vt为vt
    public void vtPropagationDelete(Virtual_Truth_Type vt, Context context){
        Virtual_Truth_Type originVT = this.getVirtualTruth();
        //更新本结点的kidVT
        this.getKidsVT().remove(context);
        //分情况讨论
        if(this.formula.getFormula_type() == Formula.Formula_Type.FORALL){
            if(originVT == Virtual_Truth_Type.TRUE){
                assert vt == Virtual_Truth_Type.TRUE;
                //结果还是TRUE，所以不用设置自身，也不用传播
            }
            else if(originVT == Virtual_Truth_Type.FALSE){
                assert vt != Virtual_Truth_Type.UNKNOWN;
                if(vt == Virtual_Truth_Type.TRUE){
                    //结果还是FALSE，所以不用设置自身，也不用传播
                    assert true;
                }
                else{
                    this.setVirtualTruth(Virtual_Truth_Type.UNKNOWN);
                    if(parent != null){
                        this.parent.vtPropagationUpdate(originVT, this);
                    }
                }
            }
            else{
                //不管是什么，最终都是UNKNOWN，所以不需要设置，也不需要传播
                assert true;
            }
        }
        else if(this.formula.getFormula_type() == Formula.Formula_Type.EXISTS){
            if(originVT == Virtual_Truth_Type.TRUE){
                assert vt != Virtual_Truth_Type.UNKNOWN;
                if(vt == Virtual_Truth_Type.TRUE){
                    this.setVirtualTruth(Virtual_Truth_Type.UNKNOWN);
                    if(parent != null){
                        this.parent.vtPropagationUpdate(originVT, this);
                    }
                }
                else{
                    //结果还是TRUE，所以不需要设置也不需要传播
                    assert true;
                }
            }
            else if(originVT == Virtual_Truth_Type.FALSE){
                assert vt == Virtual_Truth_Type.FALSE;
                //结果还是FALSE，所以不需要设置，也不需要传播
            }
            else{
                //不管是什么，最终都是UNKNOWN，所以不需要设置，也不需要传播
                assert true;
            }
        }
        else{
            assert false;
        }
    }

    //子结点child的originChildVT发生了更新
    public void vtPropagationUpdate(Virtual_Truth_Type originChildVT, RuntimeNode child){
        assert originChildVT != Virtual_Truth_Type.UNKNOWN && child.getVirtualTruth() == Virtual_Truth_Type.UNKNOWN;
        Virtual_Truth_Type originVT = this.getVirtualTruth();
        //本结点必然为UNKNOWN, can be more precise
        this.setVirtualTruth(Virtual_Truth_Type.UNKNOWN);
        //判断当前结点对应的公式是否是forall/exists，如果是则更新kidsVT
        if(this.formula.getFormula_type() == Formula.Formula_Type.FORALL){
            String var = ((FForall)this.formula).getVar();
            this.getKidsVT().put(child.getVarEnv().get(var), Virtual_Truth_Type.UNKNOWN);
        }
        else if(this.formula.getFormula_type() == Formula.Formula_Type.EXISTS){
            String var = ((FExists)this.formula).getVar();
            this.getKidsVT().put(child.getVarEnv().get(var), Virtual_Truth_Type.UNKNOWN);
        }
        //如果原来的vt不是UNKNOWN,则需要向上传递影响
        if(originVT != Virtual_Truth_Type.UNKNOWN && this.parent != null){
            this.parent.vtPropagationUpdate(originVT, this);
        }
    }


    @Override
    public String toString() {
        return "RuntimeNode{" +
                "formula=" + formula.toString() +
                ", depth=" + depth +
                ", newChildren=" + children.size() +
                ", varEnv=" + varEnv +
                ", truth=" + truth +
                ", virtualTruth=" + virtualTruth +
                ", links=" + links +
                '}';
    }

    public String show(int offset, final Set<RuntimeNode> scctNodes, String pVar){
        StringBuilder stringBuilder = new StringBuilder();
        //offset
        stringBuilder.append(" ".repeat(Math.max(0, offset)));
        //prefix
        if(pVar != null){
            stringBuilder.append("\"").append(pVar).append("=").append(this.getVarEnv().get(pVar).getCtx_id()).append("\"@");
        }
        //start
        stringBuilder.append("(\"");
        //SCCT
        if(scctNodes.contains(this)){
            stringBuilder.append("SCCT ");
        }
        //truth value
        if(this.isTruth()){
            stringBuilder.append("T ");
        }
        else{
            stringBuilder.append("F ");
        }
        //formula
        switch (this.formula.getFormula_type()){
            case FORALL:{
                String var = ((FForall) this.formula).getVar();
                stringBuilder.append("forall ").append(var).append(" ").append(((FForall) this.formula).getPattern_id()).append("\"").append("\n");
                for(RuntimeNode child : this.getChildren()){
                    stringBuilder.append(child.show(offset+4, scctNodes, var));
                }
                stringBuilder.append(" ".repeat(Math.max(0, offset)));
                stringBuilder.append(")\n");
                break;
            }
            case EXISTS:{
                String var = ((FExists) this.formula).getVar();
                stringBuilder.append("exists ").append(var).append(" ").append(((FExists) this.formula).getPattern_id()).append("\"").append("\n");
                for(RuntimeNode child : this.getChildren()){
                    stringBuilder.append(child.show(offset+4, scctNodes, var));
                }
                stringBuilder.append(" ".repeat(Math.max(0, offset)));
                stringBuilder.append(")\n");
                break;
            }
            case AND:{
                stringBuilder.append("and\"").append("\n");
                stringBuilder.append(this.getChildren().get(0).show(offset+4, scctNodes, null));
                stringBuilder.append(this.getChildren().get(1).show(offset+4, scctNodes, null));
                stringBuilder.append(" ".repeat(Math.max(0, offset)));
                stringBuilder.append(")\n");
                break;
            }
            case OR:{
                stringBuilder.append("or\"").append("\n");
                stringBuilder.append(this.getChildren().get(0).show(offset+4, scctNodes, null));
                stringBuilder.append(this.getChildren().get(1).show(offset+4, scctNodes, null));
                stringBuilder.append(" ".repeat(Math.max(0, offset)));
                stringBuilder.append(")\n");
                break;
            }
            case IMPLIES:{
                stringBuilder.append("implies\"").append("\n");
                stringBuilder.append(this.getChildren().get(0).show(offset+4, scctNodes, null));
                stringBuilder.append(this.getChildren().get(1).show(offset+4, scctNodes, null));
                stringBuilder.append(" ".repeat(Math.max(0, offset)));
                stringBuilder.append(")\n");
                break;
            }
            case NOT:{
                stringBuilder.append("not\"").append("\n");
                stringBuilder.append(this.getChildren().get(0).show(offset+4, scctNodes, null));
                stringBuilder.append(" ".repeat(Math.max(0, offset)));
                stringBuilder.append(")\n");
                break;
            }
            case BFUNC:{
                stringBuilder.append("bfunc ").append(((FBfunc) this.formula).getFunc()).append("\")\n");
                break;
            }
            default:
                assert false;
                break;
        }
        return stringBuilder.toString();
    }
}

