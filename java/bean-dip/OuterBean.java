package beandip;

import java.util.List;

public class OuterBean {
    private String fooField;
    private List<InnerBean> inners;

    public List<InnerBean> getInners() {
        return inners;
    }

    public void setInners(List<InnerBean> inners) {
        this.inners = inners;
    }

    public String getFooField() {
        return fooField;
    }

    public void setFooField(String fooField) {
        this.fooField = fooField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OuterBean outerBean = (OuterBean) o;

        if (fooField != null ? !fooField.equals(outerBean.fooField) : outerBean.fooField != null)
            return false;
        return inners != null ? inners.equals(outerBean.inners) : outerBean.inners == null;

    }

    @Override
    public int hashCode() {
        int result = fooField != null ? fooField.hashCode() : 0;
        result = 31 * result + (inners != null ? inners.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "OuterBean{" +
            "fooField='" + fooField + '\'' +
            ", inners=" + inners +
            '}';
    }
}

