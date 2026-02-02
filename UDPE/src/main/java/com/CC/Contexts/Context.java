package com.CC.Contexts;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private String ctx_id;

    private final Map<String, String> ctx_fields;

    public Context() {
        this.ctx_fields = new HashMap<>();
    }

    public String getCtx_id() {
        return ctx_id;
    }

    public Map<String, String> getCtx_fields() {
        return ctx_fields;
    }

    public void setCtx_id(String ctx_id) {
        this.ctx_id = ctx_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Context context = (Context) o;

        return ctx_id.equals(context.ctx_id);
    }

    @Override
    public int hashCode() {
        return ctx_id.hashCode();
    }

    @Override
    public String toString() {
        return "ctx_id=" + ctx_id ;
    }
}
