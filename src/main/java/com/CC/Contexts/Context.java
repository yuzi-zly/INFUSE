package com.CC.Contexts;

import java.util.Objects;

public abstract class Context {
    public String ctx_id;

    public String getCtx_id() {
        return ctx_id;
    }

    public void setCtx_id(String ctx_id) {
        this.ctx_id = ctx_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Context context = (Context) o;
        return Objects.equals(ctx_id, context.ctx_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctx_id);
    }

    @Override
    public String toString() {
        return "ctx_id=" + ctx_id ;
    }
}
