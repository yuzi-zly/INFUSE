package com.CC;

import com.CC.Util.Loggable;
import org.apache.commons.cli.*;

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
        //add("INFUSE_base");
        add("INFUSE");
    }};


    public static String testIncOut = "inconsistencies.json";
    public static String testCCTOut = "cct.txt";
    public static String incOut = "inconsistencies.txt";


    public static void main(String[] args) throws Exception {
        Option opt_ap = Option.builder("approach")
                .argName("approach")
                .hasArg()
                .required(false)
                .desc("Use the specified approach for checking [ECC+IMD/ECC+GEAS_ori/PCC+IMD/PCC+GEAS_ori/ConC+IMD/ConC+GEAS_ori/INFUSE_base/INFUSE]")
                .build();

        Option opt_rf = Option.builder("rules")
                .argName("file")
                .hasArg()
                .required(false)
                .desc("Load rules from given file (XML file)")
                .build();

        Option opt_bf = Option.builder("bfuncs")
                .argName("file")
                .hasArg()
                .required(false)
                .desc("Load bfunctions from given file (Class file)")
                .build();


        Option opt_mg = new Option("mg", false, "Enable link generation minimization");
        opt_mg.setRequired(false);

        Option opt_h = new Option("help", false, "Print the usage");
        opt_h.setRequired(false);

        Option opt_oi = Option.builder("incs")
                .argName("file")
                .hasArg()
                .required(false)
                .desc("Write detected inconsistencies to given file")
                .build();

        Option opt_md = Option.builder("mode")
                .argName("mode")
                .hasArg()
                .required(false)
                .desc("Run under the given mode [offline/online]")
                .build();

        Option opt_pf = Option.builder("patterns")
                .argName("file")
                .hasArg()
                .required(false)
                .desc("Load patterns from given file (XML file)")
                .build();

        Option opt_df = Option.builder("data")
                .argName("file")
                .hasArg()
                .required(false)
                .desc("Read data from given file")
                .build();

        Option opt_rt = Option.builder("runtype")
                .argName("type")
                .hasArg()
                .required(false)
                .desc("Specify the type of running scenario")
                .build();

        Options options = new Options();
        options.addOption(opt_h);
        options.addOption(opt_rf);
        options.addOption(opt_pf);
        options.addOption(opt_df);
        options.addOption(opt_ap);
        options.addOption(opt_md);
        options.addOption(opt_bf);
        options.addOption(opt_rt);
        options.addOption(opt_mg);
        options.addOption(opt_oi);

        CommandLine cli = null;
        CommandLineParser cliParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();

        try {
            cli = cliParser.parse(options, args);
        } catch (ParseException e) {
            helpFormatter.printHelp("java -jar INFUSE-version.jar [Options]", options, true);
            e.printStackTrace();
        }

        assert cli != null;
        if(cli.hasOption("help")){
            helpFormatter.printHelp("java -jar INFUSE-version.jar [Options]", options);
        }
        else {
/*
java -jar INFUSE.jar
-mode offline
-approach INFUSE
-rules rules.xml
-bfuncs bfuncs.class
-patterns patterns.xml
-data data.txt
-runtype highwayQ3
-mg
-incs incs.json
 */
            // checking mode
            String checkingMode = null;
            if(!cli.hasOption("mode")){
                logger.error("\033[91m" + "No specified mode, please use option \"-mode\", available modes: [offline/online]" + "\033[0m");
                logger.info("\033[92m" + "Use option \"-help\" for more information"  + "\033[0m");
                System.exit(1);
            }
            else{
                checkingMode = cli.getOptionValue("mode");
                if(!checkingMode.equalsIgnoreCase("offline") && !checkingMode.equalsIgnoreCase("online")){
                    logger.error("\033[91m" + "The mode is illegal, available modes: [offline/online]" + "\033[0m");
                    logger.info("\033[92m" + "Use option \"-help\" for more information"  + "\033[0m");
                    System.exit(1);
                }
            }
            // checking approach
            String approach = null;
            if(!cli.hasOption("approach")){
                logger.error("\033[91m" + "No specified approach, please use option \"-approach \", available approaches: [ECC+IMD/ECC+GEAS_ori/PCC+IMD/PCC+GEAS_ori/ConC+IMD/ConC+GEAS_ori/INFUSE]" + "\033[0m");
                logger.info("\033[92m" + "Use option \"-help\" for more information"  + "\033[0m");
                System.exit(1);
            }
            else{
                approach = cli.getOptionValue("approach");
                if(!legalApproaches.contains(approach)){
                    logger.error("\033[91m" + "The approach is illegal, available approaches: [ECC+IMD/ECC+GEAS_ori/PCC+IMD/PCC+GEAS_ori/ConC+IMD/ConC+GEAS_ori/INFUSE]" + "\033[0m");
                    logger.info("\033[92m" + "Use option \"-help\" for more information"  + "\033[0m");
                    System.exit(1);
                }
            }
            // rule file
            String ruleFile = null;
            if(!cli.hasOption("rules")){
                logger.error("\033[91m" + "No specified rule file, please use option \"-rules\"" + "\033[0m");
                logger.info("\033[92m" + "Use option \"-help\" for more information"  + "\033[0m");
                System.exit(1);
            }
            else{
                ruleFile = cli.getOptionValue("rules");
                logger.info(String.format("The rule file is \"%s\"", ruleFile));
            }
            // bfunc file
            String bfuncFile = null;
            if(!cli.hasOption("bfuncs")){
                logger.error("\033[91m" + "No specified bfunction file, please use option \"-bfuncs\"" + "\033[0m");
                logger.info("\033[92m" + "Use option \"-help\" for more information"  + "\033[0m");
                System.exit(1);
            }
            else{
                bfuncFile = cli.getOptionValue("bfuncs");
                logger.info(String.format("The bfunction file is \"%s\"", bfuncFile));
            }
            // pattern file
            String patternFile = null;
            if(!cli.hasOption("patterns")){
                logger.error("\033[91m" + "No specified pattern file, please use option \"-patterns\"" + "\033[0m");
                logger.info("\033[92m" + "Use option \"-help\" for more information"  + "\033[0m");
                System.exit(1);
            }
            else{
                patternFile = cli.getOptionValue("patterns");
                logger.info(String.format("The pattern file is \"%s\"", patternFile));
            }
            // data file [offline]
            String dataFile = null;
            if(checkingMode.equalsIgnoreCase("offline")){
                //data file
                if(!cli.hasOption("data")){
                    logger.error("\033[91m" + "No specified data file in offline mode, please use option \"-data\"" + "\033[0m");
                    logger.info("\033[92m" + "Use option \"-help\" for more information"  + "\033[0m");
                    System.exit(1);
                }
                else{
                    dataFile = cli.getOptionValue("data");
                    logger.info(String.format("The data file is \"%s\"", dataFile));
                }
            }
            else{
                if(cli.hasOption("data")){
                    logger.error("\033[91m" + "Cannot specify data file in online mode" + "\033[0m");
                    logger.info("\033[92m" + "Use option \"-help\" for more information"  + "\033[0m");
                    System.exit(1);
                }
            }
            // runtype
            String runtype = null;
            if(!cli.hasOption("runtype")){
                logger.error("\033[91m" + "No specified runtype, please use option \"-runtype\"" + "\033[0m");
                logger.info("\033[92m" + "Use option \"-help\" for more information"  + "\033[0m");
                System.exit(1);
            }
            else{
                runtype = cli.getOptionValue("runtype");
                logger.info(String.format("The runtype is \"%s\"", runtype));
            }

            // mg
            boolean isMG = cli.hasOption("mg");
            logger.info(String.format("Minimizing link generation is %s", isMG ? "on" : "off"));
            // incs
            String incs = null;
            if(!cli.hasOption("incs")){
                incs = incOut;
                logger.info("The default inconsistency file is \"" + incOut + "\"");
            }
            else{
                incs = cli.getOptionValue("incs");
                logger.info(String.format("The inconsistency file is \"%s\"", incs));
            }

            // start
            if(checkingMode.equalsIgnoreCase("offline")){
                long startTime = System.nanoTime();
                OfflineStarter offlineStarter = new OfflineStarter();
                offlineStarter.start(approach, ruleFile, bfuncFile, patternFile, dataFile, isMG, incs, runtype);
                long totalTime = System.nanoTime() - startTime;
                logger.info("\033[92m" + "Time cost: " + totalTime / 1000000L + " ms\033[0m");
            }
            else if(checkingMode.equalsIgnoreCase("online")){
                OnlineStarter onlineStarter = new OnlineStarter();
                onlineStarter.start(approach, ruleFile, bfuncFile, patternFile, isMG, incs, runtype);
            }
        }
    }

}

