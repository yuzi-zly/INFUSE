package com.CC;

import com.CC.Middleware.Schedulers.IMD;
import org.apache.commons.cli.*;


public class CLIParser {

    public static void main(String[] args) throws Exception {
        Option opt_h = new Option("h", "help", false, "help information");
        opt_h.setRequired(false);
        Option opt_r = new Option("r","run",false,"run mode");
        opt_r.setRequired(false);
        Option opt_rf = new Option("rf",  "ruleFile", true, "rule file");
        opt_rf.setRequired(false);
        Option opt_pf = new Option("pf", "patternFile", true, "pattern file");
        opt_pf.setRequired(false);
        Option opt_df = new Option("df", "dataFile", true, "data file");
        opt_df.setRequired(false);
        Option opt_ct = new Option("ct", "technique", true, "checking technique");
        opt_ct.setRequired(false);
        Option opt_ss = new Option("ss", "schedule", true, "schedule strategy");
        opt_ss.setRequired(false);
        Option opt_md = new Option("md", "mode", true, "checking mode");
        opt_md.setRequired(false);
        Option opt_bf = new Option("bf",  "bfunc", true, "bfunc file");
        opt_bf.setRequired(false);


        Option opt_t = new Option("t","test",false,"test mode");
        opt_t.setRequired(false);
        Option opt_cp = new Option("cp",  "contextPool", true, "context pool file");
        opt_cp.setRequired(false);


        Options options = new Options();
        options.addOption(opt_h);
        options.addOption(opt_r);
        options.addOption(opt_rf);
        options.addOption(opt_pf);
        options.addOption(opt_df);
        options.addOption(opt_ct);
        options.addOption(opt_ss);
        options.addOption(opt_md);
        options.addOption(opt_bf);

        options.addOption(opt_t);
        options.addOption(opt_cp);

        CommandLine cli = null;
        CommandLineParser cliParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();

        try {
            cli = cliParser.parse(options, args);
        } catch (ParseException e) {
            helpFormatter.printHelp(">>>>>>help information<<<<<<", options);
            e.printStackTrace();
        }

        assert cli != null;
        if(cli.hasOption("h")){
            System.out.println("help information TODO");
        }
        else if(cli.hasOption("t")){
            System.out.println("test TODO");
        }
        else if(cli.hasOption("r")){
            String ruleFile = cli.getOptionValue("rf", "");
            String patternFile = cli.getOptionValue("pf", "");
            String dataFile = cli.getOptionValue("df", "");
            String technique = cli.getOptionValue("ct", "ECC");
            String schedule = cli.getOptionValue("ss", "IMD");
            String checkingMode = cli.getOptionValue("md", "offline");
            String bfuncFile = cli.getOptionValue("bf","");

            if(checkingMode.equalsIgnoreCase("offline")){
                long startTime = System.nanoTime();
                OfflineStarter offlineStarter = new OfflineStarter(ruleFile, patternFile, dataFile, technique, schedule, bfuncFile, "taxi");
                offlineStarter.runWithOriginData();
                long totalTime = System.nanoTime() - startTime;
                System.out.println("Tech: " + technique +"\tSchd: " + schedule + "\tData: " + dataFile +  "\t" + totalTime / 1000000L + " ms");
            }
            else{
                //TODO()
            }
        }
        else{
            assert false;
        }
    }
}

