package bean_dip;

import java.util.List;

public class ParentBean {
    private List<BuiltBean> children;

    public List<BuiltBean> getChildren() {
        return children;
    }

    public void setChildren(List<BuiltBean> children) {
        this.children = children;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParentBean outerBean = (ParentBean) o;

        return children != null ? children.equals(outerBean.children) : outerBean.children == null;

    }

    @Override
    public int hashCode() {
        return children != null ? children.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ParentBean{" +
            "children=" + children +
            '}';
    }
}

