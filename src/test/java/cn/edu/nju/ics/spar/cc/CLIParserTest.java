package cn.edu.nju.ics.spar.cc;

class CLIParserTest {

    public static void testTaxi(String ap, boolean isMG){
        String[] args = null;
        if(isMG){
            args = new String[]{
                    "-mode", "offline", "-approach", ap,
                    "-rules", "src/test/resources/taxi/rules.xml",
                    "-bfuncs", "src/test/resources/taxi/Bfunction.class",
                    "-patterns", "src/test/resources/taxi/patterns.xml",
                    "-mfuncs", "src/test/resources/taxi/Mfunction.class",
                    "-data", "src/test/resources/taxi/data_5_0-1_new.txt",
                    "-datatype", "rawData",
                    "-mg"
            };
        }
        else{
            args = new String[]{
                    "-mode", "offline", "-approach", ap,
                    "-rules", "src/test/resources/taxi/rules.xml",
                    "-bfuncs", "src/test/resources/taxi/Bfunction.class",
                    "-patterns", "src/test/resources/taxi/patterns.xml",
                    "-mfuncs", "src/test/resources/taxi/Mfunction.class",
                    "-data", "src/test/resources/taxi/data_5_0-1_new.txt",
                    "-datatype", "rawData"
            };
        }
        try {
            CLIParser.main(args);
            //TODO: compare answer
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testTesting(String ap, String path){
        String [] args = new String[]{
                "-test", "-approach", ap,
                "-rules", "src/test/resources/testing/" + path +  "/rules.xml",
                "-contextpool", "src/test/resources/testing/" + path + "/cp.json",
                "-bfuncs", "src/test/resources/testing/" + path + "/Bfunction.class",
                "-incs", "src/test/resources/testing/" + path + "/cceResult.json",
                "-cct", "src/test/resources/testing/" + path + "/CCT.txt",
                "-mg"
        };
        try {
            CLIParser.main(args);
            //TODO: compare answer
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}