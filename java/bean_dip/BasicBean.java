package bean_dip;

public class BasicBean {
    private Long fooField;
    private boolean truthy = false;

    public boolean isTruthy() {
        return truthy;
    }

    public void setTruthy(boolean truthy) {
        this.truthy = truthy;
    }

    public Long getFooField() {
        return fooField;
    }

    public void setFooField(Long fooField) {
        this.fooField = fooField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BasicBean basicBean = (BasicBean) o;

        return fooField != null ? fooField.equals(basicBean.fooField) : basicBean.fooField == null;

    }

    @Override
    public int hashCode() {
        return fooField != null ? fooField.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BasicBean{" +
            "fooField=" + fooField +
            '}';
    }
}

