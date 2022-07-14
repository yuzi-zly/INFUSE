package com.CC;

import org.junit.jupiter.api.Test;

public class TaxiTest {

    @Test
    void lightTest(){
        CLIParserTest.testTaxi("offline", "INFUSE", "data_5_0-1");
    }

    void medianTest(){
        CLIParserTest.testTaxi("offline", "INFUSE", "data_10_0-1");
    }
}
