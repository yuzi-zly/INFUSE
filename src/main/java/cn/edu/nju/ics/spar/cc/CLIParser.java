package cn.edu.nju.ics.spar.cc;

import cn.edu.nju.ics.spar.cc.Util.InfuseException;
import cn.edu.nju.ics.spar.cc.Util.Loggable;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CLIParser implements Loggable {

    // Valid approach combinations
    private static final List<String> VALID_APPROACHES = Arrays.asList(
            "ECC+IMD",
            "ECC+GEAS_ori",
            "PCC+IMD",
            "PCC+GEAS_ori",
            "ConC+IMD",
            "ConC+GEAS_ori",
            "INFUSE"
    );

    // Valid modes
    private static final List<String> VALID_MODES = Arrays.asList("offline", "online");
    
    // Valid data types
    private static final List<String> VALID_DATA_TYPES = Arrays.asList("rawData", "change");

    // Default values
    private static final String DEFAULT_INC_OUT = "inconsistencies.txt";
    
    // ANSI color codes for terminal output
    private static final String COLOR_RED = "\033[91m";
    private static final String COLOR_GREEN = "\033[92m";
    private static final String COLOR_RESET = "\033[0m";
    
    // Error message templates
    private static final String HELP_HINT = "Use option \"-help\" for more information";

    public static void main(String[] args) throws Exception {
        Options options = buildOptions();
        CommandLine cli = parseCommandLine(args, options);
        
        // Show help and exit if requested
        if (cli.hasOption("help")) {
            printHelp(options);
            return;
        }
        
        // Parse and validate all options
        InfuseConfig config = parseAndValidateOptions(cli);
        
        // Execute the command
        executeCommand(config);
    }

    /**
     * Build all command line options
     */
    private static Options buildOptions() {
        Options options = new Options();
        
        // Help option
        options.addOption(new Option("help", false, "Print the usage"));
        
        // Mode option
        options.addOption(Option.builder("mode")
                .argName("mode")
                .hasArg()
                .desc("Run under the given mode [offline/online]")
                .build());
        
        // Approach option
        options.addOption(Option.builder("approach")
                .argName("approach")
                .hasArg()
                .desc("Use the specified approach for checking " + VALID_APPROACHES)
                .build());
        
        // Rule file option
        options.addOption(Option.builder("rules")
                .argName("file")
                .hasArg()
                .desc("Load rules from given file (XML file)")
                .build());
        
        // Bfunction file option
        options.addOption(Option.builder("bfuncs")
                .argName("file")
                .hasArg()
                .desc("Load bfunctions from given file (Class file)")
                .build());
        
        // Pattern file option
        options.addOption(Option.builder("patterns")
                .argName("file")
                .hasArg()
                .desc("Load patterns from given file (XML file)")
                .build());
        
        // Mfunction file option
        options.addOption(Option.builder("mfuncs")
                .argName("file")
                .hasArg()
                .desc("Load mfunctions from given file (Class file)")
                .build());
        
        // Data file option
        options.addOption(Option.builder("data")
                .argName("file")
                .hasArg()
                .desc("Read data from given file (offline mode only)")
                .build());
        
        // Data type option
        options.addOption(Option.builder("datatype")
                .argName("type")
                .hasArg()
                .desc("Specify the type of data in dataFile [rawData/change]")
                .build());
        
        // Minimization option
        options.addOption(new Option("mg", false, "Enable link generation minimization"));
        
        // Inconsistency output option
        options.addOption(Option.builder("incs")
                .argName("file")
                .hasArg()
                .desc("Write detected inconsistencies to given file")
                .build());
        
        return options;
    }

    /**
     * Parse command line arguments
     */
    private static CommandLine parseCommandLine(String[] args, Options options) throws InfuseException {
        CommandLineParser parser = new DefaultParser();
        
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            printHelp(options);
            throw new InfuseException("Failed to parse command line arguments", e);
        }
    }

    /**
     * Print help information
     */
    private static void printHelp(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("java -jar INFUSE-version.jar [Options]", options);
    }

    /**
     * Parse and validate all command line options
     */
    private static InfuseConfig parseAndValidateOptions(CommandLine cli) throws InfuseException {
        InfuseConfig config = new InfuseConfig();
        
        // Validate and set mode and approach
        config.mode = requireChoiceOption(cli, "mode", "mode", VALID_MODES);
        config.approach = requireChoiceOption(cli, "approach", "approach", VALID_APPROACHES);
        
        // Validate and set required files
        config.ruleFile = requireFileOption(cli, "rules", "rule file");
        config.bfuncFile = requireFileOption(cli, "bfuncs", "bfunction file");
        config.patternFile = requireFileOption(cli, "patterns", "pattern file");
        
        // Optional mfunction file
        if (cli.hasOption("mfuncs")) {
            config.mfuncFile = requireFileOption(cli, "mfuncs", "mfunction file");
        } else {
            logger.info("No specified mfunction file");
        }
        
        // Data file validation (offline mode only)
        if (config.mode.equalsIgnoreCase("offline")) {
            config.dataFile = requireFileOption(cli, "data", "data file");
        } else if (cli.hasOption("data")) {
            throwError("Cannot specify data file in online mode");
        }
        
        // Validate and set data type
        config.dataType = requireChoiceOption(cli, "datatype", "data type", VALID_DATA_TYPES);
        
        // Set boolean options
        config.isMG = cli.hasOption("mg");
        logger.info(String.format("Minimizing link generation is %s", config.isMG ? "on" : "off"));
        
        // Set inconsistency output file
        config.incFile = cli.hasOption("incs") ? cli.getOptionValue("incs") : DEFAULT_INC_OUT;
        logger.info(String.format("The inconsistency file is \"%s\"", config.incFile));
        
        return config;
    }

    /**
     * Execute the command based on configuration
     */
    private static void executeCommand(InfuseConfig config) throws Exception {
        if (config.mode.equalsIgnoreCase("offline")) {
            long startTime = System.nanoTime();
            OfflineStarter offlineStarter = new OfflineStarter();
            offlineStarter.start(config);
            long totalTime = System.nanoTime() - startTime;
            logger.info(colorize("Time cost: " + totalTime / 1000000L + " ms", COLOR_GREEN));
        } else if (config.mode.equalsIgnoreCase("online")) {
            OnlineStarter onlineStarter = new OnlineStarter();
            onlineStarter.start(config);
        }
    }

    /**
     * Require a choice option with validation
     */
    private static String requireChoiceOption(CommandLine cli, String optionName, String displayName,List<String> validChoices) throws InfuseException {
        if (!cli.hasOption(optionName)) {
            throwError("No specified " + displayName + ", please use option \"-" + optionName + "\"");
        }
        String value = cli.getOptionValue(optionName);
        if (!validChoices.contains(value)) {
            throwError("The " + displayName + " is illegal: " + value + ", available options: " + validChoices);
        }
        logger.info(String.format("The %s is \"%s\"", displayName, value));
        return value;
    }

    /**
     * Require a file option with existence validation
     */
    private static String requireFileOption(CommandLine cli, String optionName, String displayName) throws InfuseException {
        if (!cli.hasOption(optionName)) {
            throwError("No specified " + displayName + ", please use option \"-" + optionName + "\"");
        }
        String filePath = cli.getOptionValue(optionName);
        File file = new File(filePath);
        if (!file.exists()) {
            throwError("The " + displayName + " does not exist: " + filePath);
        }
        if (!file.canRead()) {
            throwError("The " + displayName + " is not readable: " + filePath);
        }
        logger.info(String.format("The %s is \"%s\"", displayName, filePath));
        return filePath;
    }

    /**
     * Throw a formatted error with color
     */
    private static void throwError(String message) throws InfuseException {
        logger.error(colorize(message, COLOR_RED));
        logger.info(colorize(HELP_HINT, COLOR_GREEN));
        throw new InfuseException(message);
    }

    /**
     * Colorize a message with ANSI color codes
     */
    private static String colorize(String message, String colorCode) {
        return colorCode + message + COLOR_RESET;
    }
}
