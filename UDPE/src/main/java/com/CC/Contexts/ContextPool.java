package com.CC.Contexts;

import com.CC.Constraints.Rules.Rule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContextPool {

    //存储现在有效的context, rule_id to Map<pattern_id to set>
    private final HashMap<String, HashMap<String, Set<Context>>> Pool;

    // pattern_id to set
    private final HashMap<String, Set<Context>> DelSets;
    private final HashMap<String, Set<Context>> AddSets;
    private final HashMap<String, Set<Context>> UpdSets;

    public ContextPool() {
        Pool = new HashMap<>();
        DelSets = new HashMap<>();
        AddSets = new HashMap<>();
        UpdSets = new HashMap<>();
    }

    public void poolInit(Rule rule){
            HashMap<String, Set<Context>> map = new HashMap<>();
            for(String pattern_id : rule.getVarPatternMap().values()){
                map.put(pattern_id, new HashSet<>());
            }
            Pool.put(rule.getRule_id(), map);
    }

    public void threeSetsInit(String pattern_id){
        DelSets.put(pattern_id, new HashSet<>());
        AddSets.put(pattern_id, new HashSet<>());
        UpdSets.put(pattern_id, new HashSet<>());
    }

    public Set<Context> getAddSet(String pattern_id){
        return AddSets.get(pattern_id);
    }

    public Set<Context> getDelSet(String pattern_id){
        return DelSets.get(pattern_id);
    }

    public Set<Context> getUpdSet(String pattern_id){
        return UpdSets.get(pattern_id);
    }

    public Set<Context> getPoolSet(String rule_id, String pattern_id){
        return Pool.get(rule_id).get(pattern_id);
    }

    public int getAddSetSize(String pattern_id){
        return AddSets.get(pattern_id).size();
    }

    public int getDelSetSize(String pattern_id){
        return DelSets.get(pattern_id).size();
    }

    public int getUpdSetSize(String pattern_id){
        return UpdSets.get(pattern_id).size();
    }

    public int getPoolSetSize(String rule_id, String pattern_id){
        return Pool.get(rule_id).get(pattern_id).size();
    }

    //ECC PCC CON-C
    public void applyChange(String rule_id, ContextChange contextChange){
        if(contextChange.getChange_type() == ContextChange.Change_Type.ADDITION){
            Pool.get(rule_id).get(contextChange.getPattern_id()).add(contextChange.getContext());
        }
        else{
            Pool.get(rule_id).get(contextChange.getPattern_id()).remove(contextChange.getContext());
        }
    }

    //CPCC method 2
    public void applyChanges(Rule rule, List<ContextChange> batch) {
        //init DelSet, AddSet, and ModSet
        for(String pattern_id : rule.getVarPatternMap().values()){
            DelSets.get(pattern_id).clear();
            AddSets.get(pattern_id).clear();
            UpdSets.get(pattern_id).clear();
        }

        //update DelSets, AddSets, and ModSets
        for(ContextChange contextChange : batch){
            String pattern_id = contextChange.getPattern_id();
            if(!rule.getVarPatternMap().containsValue(pattern_id))
                continue;
            Set<Context> DelSet = DelSets.get(pattern_id);
            Set<Context> AddSet = AddSets.get(pattern_id);
            Set<Context> ModSet = UpdSets.get(pattern_id);
            if(contextChange.getChange_type() == ContextChange.Change_Type.ADDITION){
                Pool.get(rule.getRule_id()).get(pattern_id).add(contextChange.getContext());
                if(DelSet.contains(contextChange.getContext())){
                    DelSet.remove(contextChange.getContext());
                    ModSet.add(contextChange.getContext());
                }
                else{
                    AddSet.add(contextChange.getContext());
                }
            }
            else if(contextChange.getChange_type() == ContextChange.Change_Type.DELETION){
                Pool.get(rule.getRule_id()).get(pattern_id).remove(contextChange.getContext());
                if(AddSet.contains(contextChange.getContext())){
                    AddSet.remove(contextChange.getContext());
                }
                else if(ModSet.contains(contextChange.getContext())){
                    ModSet.remove(contextChange.getContext());
                    DelSet.add(contextChange.getContext());
                }
                else{
                    DelSet.add(contextChange.getContext());
                }
            }
            else {
                System.out.println("Error");
                System.exit(1);
            }
        }

    }

    //PCCM CPCC method 1
    public void applyChangeWithSets(String rule_id, ContextChange contextChange){
        Set<Context> DelS = DelSets.get(contextChange.getPattern_id());
        Set<Context> AddS = AddSets.get(contextChange.getPattern_id());
        Set<Context> ModS = UpdSets.get(contextChange.getPattern_id());
        if(contextChange.getChange_type() == ContextChange.Change_Type.ADDITION){
            Pool.get(rule_id).get(contextChange.getPattern_id()).add(contextChange.getContext());
            if(DelS.contains(contextChange.getContext())){
                DelS.remove(contextChange.getContext());
                ModS.add(contextChange.getContext());
            }else{
                AddS.add(contextChange.getContext());
            }
        }
        else{
            Pool.get(rule_id).get(contextChange.getPattern_id()).remove(contextChange.getContext());
            if(AddS.contains(contextChange.getContext())){
                AddS.remove(contextChange.getContext());
            }
            else if(ModS.contains(contextChange.getContext())){
                ModS.remove(contextChange.getContext());
                DelS.add(contextChange.getContext());
            }
            else{
                DelS.add(contextChange.getContext());
            }
        }
    }
}
