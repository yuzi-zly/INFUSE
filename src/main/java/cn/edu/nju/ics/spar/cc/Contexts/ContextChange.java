package cn.edu.nju.ics.spar.cc.Contexts;

public class ContextChange {

    public enum Change_Type {ADDITION, DELETION, UPDATE};

    private Change_Type change_type;
    private String pattern_id;
    private Context context;


    //setter and getter
    public void setChange_type(Change_Type change_type) {
        this.change_type = change_type;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setPattern_id(String pattern_id) {
        this.pattern_id = pattern_id;
    }

    public Context getContext() {
        return context;
    }

    public Change_Type getChange_type() {
        return change_type;
    }

    public String getPattern_id() {
        return pattern_id;
    }

    @Override
    public String toString() {
        return "<" + (change_type == Change_Type.ADDITION ? "+" : "-") + ", " + pattern_id + ", " + context.getCtx_id() + ">";
    }
}
