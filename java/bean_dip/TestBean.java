package bean_dip;

public class TestBean {
    private Long fooField;
    private String barField;
    private final String readOnlyField = "READ ONLY";

    public TestBean(Long fooField, String barField) {
        this.fooField = fooField;
        this.barField = barField;
    }

    public Long getFooField() {
        return fooField;
    }

    public String getBarField() {
        return barField;
    }

    public String getReadOnlyField() {
        return readOnlyField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestBean testBean = (TestBean) o;

        if (fooField != null ? !fooField.equals(testBean.fooField) : testBean.fooField != null)
            return false;
        if (barField != null ? !barField.equals(testBean.barField) : testBean.barField != null)
            return false;
        return readOnlyField != null ? readOnlyField.equals(testBean.readOnlyField) : testBean.readOnlyField == null;

    }

    @Override
    public int hashCode() {
        int result = fooField != null ? fooField.hashCode() : 0;
        result = 31 * result + (barField != null ? barField.hashCode() : 0);
        result = 31 * result + (readOnlyField != null ? readOnlyField.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TestBean{" +
            "fooField=" + fooField +
            ", barFieldUnconventional='" + barField + '\'' +
            ", readOnlyField='" + readOnlyField + '\'' +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long fooField;
        private String barField;

        public Builder fooField(Long foo) {
            fooField = foo;
            return this;
        }

        public Builder barFieldUnconventional(String bar) {
            barField = bar;
            return this;
        }

        public TestBean build() {
            return new TestBean(fooField, barField);
        }
    }
}

