package bean_dip;

public class BuiltBean {
    private Long fooField;
    private String barField;
    private Boolean someCondition;
    private final String readOnlyField = "READ ONLY";

    public BuiltBean(Long fooField, String barField, Boolean someCondition) {
        this.fooField = fooField;
        this.barField = barField;
        this.someCondition = someCondition;
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

    public Boolean isSomeCondition() {
        return someCondition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuiltBean builtBean = (BuiltBean) o;

        if (fooField != null ? !fooField.equals(builtBean.fooField) : builtBean.fooField != null)
            return false;
        if (barField != null ? !barField.equals(builtBean.barField) : builtBean.barField != null)
            return false;
        if (someCondition != null ? !someCondition.equals(builtBean.someCondition) : builtBean.someCondition != null)
            return false;
        return readOnlyField != null ? readOnlyField.equals(builtBean.readOnlyField) : builtBean.readOnlyField == null;

    }

    @Override
    public int hashCode() {
        int result = fooField != null ? fooField.hashCode() : 0;
        result = 31 * result + (barField != null ? barField.hashCode() : 0);
        result = 31 * result + (someCondition != null ? someCondition.hashCode() : 0);
        result = 31 * result + (readOnlyField != null ? readOnlyField.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BuiltBean{" +
            "fooField=" + fooField +
            ", barField='" + barField + '\'' +
            ", someCondition=" + someCondition +
            ", readOnlyField='" + readOnlyField + '\'' +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long fooField;
        private String barField;
        private Boolean someCondition;

        public Builder fooField(Long foo) {
            fooField = foo;
            return this;
        }

        public Builder someCondition(Boolean condition) {
            someCondition = condition;
            return this;
        }

        public Builder barFieldUnconventional(String bar) {
            barField = bar;
            return this;
        }

        public BuiltBean build() {
            return new BuiltBean(fooField, barField, someCondition);
        }
    }
}

