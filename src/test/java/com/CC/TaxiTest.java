package com.CC;

import org.junit.jupiter.api.Test;

public class TaxiTest {

    @Test
    void PCC_IMD_CG_Test(){CLIParserTest.testTaxi("PCC+IMD", false);}
    @Test
    void PCC_IMD_MG_Test(){CLIParserTest.testTaxi("PCC+IMD", true);}
    @Test
    void ConC_IMD_CG_Test(){CLIParserTest.testTaxi("ConC+IMD", false);}
    @Test
    void ConC_IMD_MG_Test(){CLIParserTest.testTaxi("ConC+IMD", true);}
    @Test
    void ECC_IMD_CG_Test(){CLIParserTest.testTaxi("ECC+IMD", false);}
    @Test
    void ECC_IMD_MG_Test(){CLIParserTest.testTaxi("ECC+IMD", true);}

}
