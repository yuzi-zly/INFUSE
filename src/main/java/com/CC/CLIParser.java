package com.CC;

import com.CC.Util.Loggable;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;



public class CLIParser implements Loggable {

    public static List<String> legalApproaches = new ArrayList<>(){{
        add("ECC+IMD");
        add("ECC+GEAS_ori");
        add("PCC+IMD");
        add("PCC+GEAS_ori");
        add("ConC+IMD");
        add("ConC+GEAS_ori");
        add("INFUSE_base");
        add("INFUSE");
    }};
    public static String defaultCheckingMode = "offline";
    public static String defaultApproach = "INFUSE";

    public static String incOut = "inconsistencies.txt";

    public static String dataOut = "fixedData.txt";

    public static String testIncOut = "cceResult.json";

    public static void main(String[] args) throws Exception {
        Option opt_h = new Option("h", "help", false, "To print the usage");
        opt_h.setRequired(false);
        Option opt_t = new Option("t","test",false,"Run in the testing mode");
        opt_t.setRequired(false);

        Option opt_md = new Option("md", "mode", true, "To specify the checkingMode [offline/online]");
        opt_md.setRequired(false);
        Option opt_ap = new Option("ap", "approach", true,
                "To specify the checkingApproach [ECC+IMD/ECC+GEAS_ori/PCC+IMD/PCC+GEAS_ori/ConC+IMD/ConC+GEAS_ori/INFUSE_base/INFUSE]");
        opt_ap.setRequired(false);
        Option opt_rf = new Option("rf",  "ruleFile", true, "To specify the ruleFile [e.g. src/main/resources/example/rules.xml]");
        opt_rf.setRequired(false);
        Option opt_bf = new Option("bf",  "bfuncFile", true, "To specify the bfuncFile [e.g. src/main/resources/example/Bfunctions.class]");
        opt_bf.setRequired(false);
        Option opt_pf = new Option("pf", "patternFile", true, "To specify the patternFile [e.g. src/main/resources/example/patterns.xml]");
        opt_pf.setRequired(false);
        Option opt_mf = new Option("mf", "mfuncFile", true, "To specify the mfuncFile [e.g. src/main/resources/example/Mfunctions.class]");
        opt_mf.setRequired(false);

        Option opt_df = new Option("df", "dataFile", true, "To specify the dataFile [e.g. src/main/resources/example/data.txt]");
        opt_df.setRequired(false);
        Option opt_cp = new Option("cp", "contextPool", true, "To specify the contextPool file [e.g. src/main/resources/example/cp.json]");
        opt_cp.setRequired(false);


        Options options = new Options();
        options.addOption(opt_h);
        options.addOption(opt_t);
        options.addOption(opt_rf);
        options.addOption(opt_pf);
        options.addOption(opt_df);
        options.addOption(opt_ap);
        options.addOption(opt_md);
        options.addOption(opt_bf);
        options.addOption(opt_cp);

        CommandLine cli = null;
        CommandLineParser cliParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();

        try {
            cli = cliParser.parse(options, args);
        } catch (ParseException e) {
            helpFormatter.printHelp("cmdLine Syntax", options);
            e.printStackTrace();
        }

        assert cli != null;
        if(cli.hasOption("h")){
            helpFormatter.printHelp("cmdLine Syntax", options);
        }
        else if(cli.hasOption("t")){
            // checking approach
            String approach = null;
            if(!cli.hasOption("ap")){
                approach = defaultApproach;
                logger.info("\033[92m" + "The default approach is \"" + defaultApproach + "\"\033[0m");
            }
            else{
                approach = cli.getOptionValue("ap");
                if(!legalApproaches.contains(approach)){
                    logger.error("\033[91m" + "The approach is illegal" + "\033[0m");
                    helpFormatter.printHelp("cmdLine Syntax", options);
                    System.exit(1);
                }
            }
            // rule file
            String ruleFile = null;
            if(!cli.hasOption("rf")){
                logger.error("\033[91m" + "The ruleFile cannot be empty" + "\033[0m");
                helpFormatter.printHelp("cmdLine Syntax", options);
                System.exit(1);
            }
            else{
                ruleFile = cli.getOptionValue("rf");
            }
            // bfunc file
            String bfuncFile = null;
            if(!cli.hasOption("bf")){
                logger.error("\033[91m" + "The bfuncFile cannot be empty" + "\033[0m");
                helpFormatter.printHelp("cmdLine Syntax", options);
                System.exit(1);
            }
            else{
                bfuncFile = cli.getOptionValue("bf");
            }
            // context pool file
            String contextPool = null;
            if(!cli.hasOption("cp")){
                logger.error("\033[91m" + "The contextPool cannot be empty" + "\033[0m");
                helpFormatter.printHelp("cmdLine Syntax", options);
                System.exit(1);
            }
            else{
                contextPool = cli.getOptionValue("cp");
            }

            String parentPathStr = testModeDataConvertor(contextPool);
            String patternFile = parentPathStr + "/tmpPatterns.xml";
            String dataFile = parentPathStr + "/tmpData.txt";

            //default offline checking
            long startTime = System.nanoTime();
            OfflineStarter offlineStarter = new OfflineStarter();
            offlineStarter.start(approach, ruleFile, patternFile, dataFile, bfuncFile, testIncOut, null, "test");
            long totalTime = System.nanoTime() - startTime;

            Files.delete(Paths.get(patternFile));
            Files.delete(Paths.get(dataFile));
            logger.info("The output is at \"" + testIncOut + "\"");
            logger.info("Checking Approach: " + approach +  "\tData: " + dataFile +  "\t" + totalTime / 1000000L + " ms");
        }
        else {
            // checking mode
            String checkingMode = null;
            if(!cli.hasOption("md")){
                checkingMode = defaultCheckingMode;
                logger.info("\033[92m" + "The default checkingMode is \"" + defaultCheckingMode + "\"\033[0m");
            }
            else{
                checkingMode = cli.getOptionValue("md");
                if(!checkingMode.equalsIgnoreCase("offline") && !checkingMode.equalsIgnoreCase("online")){
                    logger.error("\033[91m" + "The checkingMode is illegal" + "\033[0m");
                    helpFormatter.printHelp("cmdLine Syntax", options);
                    System.exit(1);
                }
            }
            // checking approach
            String approach = null;
            if(!cli.hasOption("ap")){
                approach = defaultApproach;
                logger.info("\033[92m" + "The default approach is \"" + defaultApproach + "\"\033[0m");
            }
            else{
                approach = cli.getOptionValue("ap");
                if(!legalApproaches.contains(approach)){
                    logger.error("\033[91m" + "The approach is illegal" + "\033[0m");
                    helpFormatter.printHelp("cmdLine Syntax", options);
                    System.exit(1);
                }
            }
            // rule file
            String ruleFile = null;
            if(!cli.hasOption("rf")){
                logger.error("\033[91m" + "The ruleFile cannot be empty" + "\033[0m");
                helpFormatter.printHelp("cmdLine Syntax", options);
                System.exit(1);
            }
            else{
                ruleFile = cli.getOptionValue("rf");
            }
            // bfunc file
            String bfuncFile = null;
            if(!cli.hasOption("bf")){
                logger.error("\033[91m" + "The bfuncFile cannot be empty" + "\033[0m");
                helpFormatter.printHelp("cmdLine Syntax", options);
                System.exit(1);
            }
            else{
                bfuncFile = cli.getOptionValue("bf");
            }
            // pattern file
            String patternFile = null;
            if(!cli.hasOption("pf")){
                logger.error("\033[91m" + "The patternFile cannot be empty" + "\033[0m");
                helpFormatter.printHelp("cmdLine Syntax", options);
                System.exit(1);
            }
            else{
                patternFile = cli.getOptionValue("pf");
            }
            // mfunc file
            String mfuncFile = null;
            if(!cli.hasOption("mf")){
                logger.warn("No specified mfuncFile.");
            }
            else{
                mfuncFile = cli.getOptionValue("mf");
            }
            // data file [offline]
            String dataFile = null;
            if(checkingMode.equalsIgnoreCase("offline")){
                if(!cli.hasOption("df")){
                    logger.error("\033[91m" + "The dataFile cannot be empty in offline mode" + "\033[0m");
                    helpFormatter.printHelp("cmdLine Syntax", options);
                    System.exit(1);
                }
                else{
                    dataFile = cli.getOptionValue("df");
                }
            }

            // start
            if(checkingMode.equalsIgnoreCase("offline")){
                long startTime = System.nanoTime();
                OfflineStarter offlineStarter = new OfflineStarter();
                offlineStarter.start(approach, ruleFile, patternFile, dataFile, bfuncFile, incOut, dataOut, "run");
                long totalTime = System.nanoTime() - startTime;
                System.out.println("Checking Approach: " + approach +  "\tData: " + dataFile +  "\t" + totalTime / 1000000L + " ms");
            }
            else if(checkingMode.equalsIgnoreCase("online")){
                OnlineStarter onlineStarter = new OnlineStarter();
                onlineStarter.start(approach, ruleFile, patternFile, bfuncFile, incOut, dataOut,"run");
            }
        }
    }


    private static String testModeDataConvertor(String contextPool) throws Exception {
        Path cpPath = Paths.get(contextPool).toAbsolutePath();
        String parent = cpPath.getParent().toString();

        OutputStreamWriter patternWriter = new OutputStreamWriter(new FileOutputStream(parent + "/tmpPatterns.xml"), StandardCharsets.UTF_8);
        BufferedWriter patternBufferWriter = new BufferedWriter(patternWriter);
        patternBufferWriter.write("<?xml version=\"1.0\"?>\n\n<patterns>\n\n");

        OutputStreamWriter dataWriter = new OutputStreamWriter(new FileOutputStream(parent + "/tmpData.txt"), StandardCharsets.UTF_8);
        BufferedWriter dataBufferWriter = new BufferedWriter(dataWriter);

        String cpStr = FileUtils.readFileToString(new File(contextPool), StandardCharsets.UTF_8);
        JSONArray cpArray = (JSONArray) JSON.parse(cpStr);
        for(Object patObj : cpArray){
            JSONObject patJsonObj = (JSONObject) patObj;
            String patternId = patJsonObj.getString("pat_id");
            patternBufferWriter.write("<pattern>\n<id>" + patternId + "</id>\n</pattern>\n\n");
            JSONArray ctxJsonArray = patJsonObj.getJSONArray("contexts");
            for(Object ctxObj : ctxJsonArray){
                JSONObject ctxJsonObj = (JSONObject) ctxObj;
                String ctxId = ctxJsonObj.getString("ctx_id");
                StringBuilder lineData = new StringBuilder("+," + patternId + "," + ctxId + "{");
                JSONObject fieldsJsonObj = ctxJsonObj.getJSONObject("fields");
                for(String fieldName : fieldsJsonObj.keySet()){
                    lineData.append(fieldName).append(":").append(fieldsJsonObj.getString(fieldName)).append(";");
                }
                lineData = new StringBuilder(lineData.substring(0, lineData.length() - 1) + "}\n");
                dataBufferWriter.write(lineData.toString());
            }
        }

        dataBufferWriter.flush();
        patternBufferWriter.write("</patterns>\n");
        patternBufferWriter.flush();
        return parent;
    }

}

