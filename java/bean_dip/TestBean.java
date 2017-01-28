package bean_dip;

public class TestBean {
    private Long fooField;

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

        TestBean innerBean = (TestBean) o;

        return fooField != null ? fooField.equals(innerBean.fooField) : innerBean.fooField == null;

    }

    @Override
    public int hashCode() {
        return fooField != null ? fooField.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TestBean{" +
            "fooField=" + fooField +
            '}';
    }
}

