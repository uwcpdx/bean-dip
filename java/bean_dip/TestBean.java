package bean_dip;

public class TestBean {
    private Long fooField;
    private String readOnlyField;

    public TestBean() {
    }

    public TestBean(Long fooField) {
        this.fooField = fooField;
    }

    public Long getFooField() {
        return fooField;
    }

    public void setFooField(Long fooField) {
        this.fooField = fooField;
    }

    public String getReadOnlyField() {
        return "READ ONLY";
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long fooField;

        public Builder fooField(Long foo) {
            fooField = foo;
            return this;
        }

        public TestBean build() {
            return new TestBean(fooField);
        }
    }
}

