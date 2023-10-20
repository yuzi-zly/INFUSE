package com.CC;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleTest {

    static boolean validation(){
        try {
            List<String> oracleList = FileUtils.readLines(new File("src/test/resources/simpleTest/oracle.txt"), StandardCharsets.UTF_8);
            List<String> resultList = FileUtils.readLines(new File("src/test/resources/simpleTest/result.txt"), StandardCharsets.UTF_8);
            if (oracleList.size() != resultList.size()){
                return false;
            }
            Collections.sort(oracleList);
            Collections.sort(resultList);
            for(int i = 0; i < oracleList.size(); ++i){
                if(!oracleList.get(i).equals(resultList.get(i))){
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void testDriver(String ap, boolean isMG){
        String[] args = null;
        if(isMG){
            args = new String[]{
                    "-mode", "offline", "-approach", ap,
                    "-rules", "src/test/resources/simpleTest/rules.xml",
                    "-bfuncs", "src/test/resources/simpleTest/Bfunction.class",
                    "-patterns", "src/test/resources/simpleTest/patterns.xml",
                    "-mfuncs", "src/test/resources/simpleTest/Mfunction.class",
                    "-data", "src/test/resources/simpleTest/data.txt",
                    "-datatype", "rawData",
                    "-mg",
                    "-incs", "src/test/resources/simpleTest/result.txt"
            };
        }
        else{
            args = new String[]{
                    "-mode", "offline", "-approach", ap,
                    "-rules", "src/test/resources/simpleTest/rules.xml",
                    "-bfuncs", "src/test/resources/simpleTest/Bfunction.class",
                    "-patterns", "src/test/resources/simpleTest/patterns.xml",
                    "-mfuncs", "src/test/resources/simpleTest/Mfunction.class",
                    "-data", "src/test/resources/simpleTest/data.txt",
                    "-datatype", "rawData",
                    "-incs", "src/test/resources/simpleTest/result.txt"
            };
        }
        try {
            CLIParser.main(args);
            assertTrue(validation());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void PCC_IMD_CG_Test(){SimpleTest.testDriver("PCC+IMD", false);}
    @Test
    void PCC_IMD_MG_Test(){SimpleTest.testDriver("PCC+IMD", true);}
    @Test
    void ConC_IMD_CG_Test(){SimpleTest.testDriver("ConC+IMD", false);}
    @Test
    void ConC_IMD_MG_Test(){SimpleTest.testDriver("ConC+IMD", true);}
    @Test
    void ECC_IMD_CG_Test(){SimpleTest.testDriver("ECC+IMD", false);}
    @Test
    void ECC_IMD_MG_Test(){SimpleTest.testDriver("ECC+IMD", true);}

    @Test
    void PCC_GEAS_CG_Test(){SimpleTest.testDriver("PCC+GEAS_ori", false);}
    @Test
    void PCC_GEAS_MG_Test(){SimpleTest.testDriver("PCC+GEAS_ori", true);}
    @Test
    void ConC_GEAS_CG_Test(){SimpleTest.testDriver("ConC+GEAS_ori", false);}
    @Test
    void ConC_GEAS_MG_Test(){SimpleTest.testDriver("ConC+GEAS_ori", true);}
    @Test
    void ECC_GEAS_CG_Test(){SimpleTest.testDriver("ECC+GEAS_ori", false);}
    @Test
    void ECC_GEAS_MG_Test(){SimpleTest.testDriver("ECC+GEAS_ori", true);}

    @Test
    void INFUSE_CG_Test(){SimpleTest.testDriver("INFUSE", false);}

    @Test
    void INFUSE_MG_Test(){SimpleTest.testDriver("INFUSE", true);}
}
