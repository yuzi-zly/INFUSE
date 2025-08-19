package com.CC.Util;

import com.CC.LLMCallBack;


public class LLM implements LLMCallBack {

    @Override
    public Boolean askLLM(String question) {
        System.out.println("Function %s call LLM".formatted(question));
        return true;
    }
}
