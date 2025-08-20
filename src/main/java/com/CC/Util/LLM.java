package com.CC.Util;

import com.CC.LLMCallBack;


public class LLM implements LLMCallBack {

    @Override
    public Boolean askLLM(String question) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }
}
