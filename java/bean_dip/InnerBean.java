package bean_dip;

public class InnerBean {
    private Long bar;

    public Long getBar() {
        return bar;
    }

    public void setBar(Long bar) {
        this.bar = bar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InnerBean innerBean = (InnerBean) o;

        return bar != null ? bar.equals(innerBean.bar) : innerBean.bar == null;

    }

    @Override
    public int hashCode() {
        return bar != null ? bar.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "InnerBean{" +
            "bar=" + bar +
            '}';
    }
}

