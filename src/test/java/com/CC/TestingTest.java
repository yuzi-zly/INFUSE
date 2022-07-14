package com.CC;

import org.junit.jupiter.api.Test;

public class TestingTest {

    @Test
    void exampleTest(){
        CLIParserTest.testTesting("INFUSE", "example");
    }

    @Test
    void error1Test(){
        CLIParserTest.testTesting("INFUSE", "error1");
    }

    @Test
    void error2Case1Test(){
        CLIParserTest.testTesting("INFUSE", "error2/case1");
    }

    @Test
    void error2Case2Test(){
        CLIParserTest.testTesting("INFUSE", "error2/case2");
    }

    @Test
    void error3Test(){
        CLIParserTest.testTesting("INFUSE", "error3");
    }

    @Test
    void error4Test(){
        CLIParserTest.testTesting("INFUSE", "error4");
    }

    @Test
    void error5Test(){
        CLIParserTest.testTesting("INFUSE", "error5");
    }

    @Test
    void error6Test(){
        CLIParserTest.testTesting("INFUSE", "error6");
    }
}
