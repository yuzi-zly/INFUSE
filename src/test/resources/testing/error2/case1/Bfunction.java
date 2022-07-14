import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Bfunction {
    private HashMap<String,List<String>> functionParamMap=new HashMap<>();      //functionName -> [param1_variable_name,param2_variable_name...]
    private HashMap<String,HashMap<String, Boolean>> functionValueMap=new HashMap<>();  //functionName -> (key:(1,2) -> value:{True,False})
    public Bfunction(){
        String nowPath=this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        String filePath=nowPath+"/bfunc_in.txt";
        //System.out.println("bfunc path: "+filePath);
        File file = new File(filePath);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;

            String nowFunction=null;
            while ((tempString = reader.readLine()) != null) {
                if(tempString.length()==0){
                    continue;
                }
                if(tempString.charAt(0)=='#'){
                    nowFunction=tempString.substring(1, tempString.length());
                    functionValueMap.put(nowFunction, new HashMap<String,Boolean>());
                }
                else if(tempString.charAt(0)=='@'){
                    String paramString=tempString.substring(1, tempString.length());
                    String[] paramArray=paramString.split(",");
                    List<String> paramList=new ArrayList<String>();
                    Collections.addAll(paramList, paramArray);
                    functionParamMap.put(nowFunction, paramList);

                    //!!!
                    //System.out.println(nowFunction+" "+paramList);
                }
                else{
                    String[] temp_list=tempString.split(" ");
                    String key=temp_list[0];
                    Boolean value=Boolean.parseBoolean(temp_list[1]);
                    functionValueMap.get(nowFunction).put(key, value);
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
    }
    public synchronized boolean bfunc(String bfunctionName, Map<String, Map<String, String>> params) {
        if(functionValueMap.containsKey(bfunctionName)==false){
            assert 0==1;
        }

        List<String> paramList=functionParamMap.get(bfunctionName);
        String key="(";
        for (String s: paramList){
            key=key+params.get(s).get("data")+",";
        }
        key=key.substring(0, key.length()-1)+")";

        HashMap<String,Boolean> functionMap=functionValueMap.get(bfunctionName);
        boolean value;
        if(functionMap.containsKey(key)){
            value=functionMap.get(key);
        }
        else{
            Random generator=new Random();
            int rand=generator.nextInt(2);
            if(rand==1){
                value=true;
            }
            else{
                value=false;
            }
            functionMap.put(key, value);
        }
        //System.out.println(key+value);
        return value;
    }
    
    public void end(){
        String nowPath=this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        File file = new File(nowPath+"/bfunc_out.txt");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for(Entry<String,HashMap<String,Boolean>> entry : functionValueMap.entrySet()){
                String bfunctionName=entry.getKey();
                writer.write("#"+bfunctionName+"\n");

                String paramString="@";
                for(String s:functionParamMap.get(bfunctionName)){
                    paramString=paramString+s+",";
                }
                paramString=paramString.substring(0, paramString.length()-1)+"\n";
                writer.write(paramString);


                HashMap<String,Boolean> functionMap=entry.getValue();
                for(Entry<String,Boolean> functionEntry:functionMap.entrySet()){
                    writer.write(functionEntry.getKey()+" "+functionEntry.getValue()+"\n");
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                }
            }
        }
    }
}
