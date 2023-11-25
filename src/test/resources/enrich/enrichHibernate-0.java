/*Generated code by Binis' code generator.*/
package net.binis.codegen;

import org.hibernate.annotations.Type;
import javax.annotation.processing.Generated;
import java.util.Set;
import java.util.List;
import jakarta.persistence.ElementCollection;

@Generated(value = "net.binis.codegen.TestPrototype", comments = "Test")
public class TestImpl implements Test {

    @Type(net.binis.codegen.hibernate.CodeEnumType.class)
    protected TestEnum enumWithPrototype;

    @ElementCollection
    @Type(net.binis.codegen.hibernate.CodeEnumType.class)
    protected List<TestEnum> enumWithPrototypeList;

    @ElementCollection
    @Type(net.binis.codegen.hibernate.CodeEnumType.class)
    protected Set<TestEnum> enumWithPrototypeSet;

    protected TestEnum enumWithoutPrototype;

    @ElementCollection
    @Type(net.binis.codegen.hibernate.CodeEnumType.class)
    protected List<TestEnum> enumWithoutPrototypeList;

    @ElementCollection
    @Type(net.binis.codegen.hibernate.CodeEnumType.class)
    protected Set<TestEnum> enumWithoutPrototypeSet;

    public TestImpl() {
    }

    public TestEnum getEnumWithPrototype() {
        return enumWithPrototype;
    }

    public List<TestEnum> getEnumWithPrototypeList() {
        return enumWithPrototypeList;
    }

    public Set<TestEnum> getEnumWithPrototypeSet() {
        return enumWithPrototypeSet;
    }

    public TestEnum getEnumWithoutPrototype() {
        return enumWithoutPrototype;
    }

    public List<TestEnum> getEnumWithoutPrototypeList() {
        return enumWithoutPrototypeList;
    }

    public Set<TestEnum> getEnumWithoutPrototypeSet() {
        return enumWithoutPrototypeSet;
    }

    public void setEnumWithPrototype(TestEnum enumWithPrototype) {
        this.enumWithPrototype = enumWithPrototype;
    }

    public void setEnumWithPrototypeList(List<TestEnum> enumWithPrototypeList) {
        this.enumWithPrototypeList = enumWithPrototypeList;
    }

    public void setEnumWithPrototypeSet(Set<TestEnum> enumWithPrototypeSet) {
        this.enumWithPrototypeSet = enumWithPrototypeSet;
    }

    public void setEnumWithoutPrototype(TestEnum enumWithoutPrototype) {
        this.enumWithoutPrototype = enumWithoutPrototype;
    }

    public void setEnumWithoutPrototypeList(List<TestEnum> enumWithoutPrototypeList) {
        this.enumWithoutPrototypeList = enumWithoutPrototypeList;
    }

    public void setEnumWithoutPrototypeSet(Set<TestEnum> enumWithoutPrototypeSet) {
        this.enumWithoutPrototypeSet = enumWithoutPrototypeSet;
    }
}
