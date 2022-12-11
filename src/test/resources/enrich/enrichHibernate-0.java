/*Generated code by Binis' code generator.*/
package net.binis.codegen;

import org.hibernate.annotations.Type;
import javax.annotation.processing.Generated;

@Generated(value = "TestPrototype", comments = "Test")
public class TestImpl implements Test {

    @Type(net.binis.codegen.hibernate.CodeEnumType.class)
    protected TestEnum enumWithPrototype;

    protected TestEnum enumWithoutPrototype;

    public TestImpl() {
    }

    public TestEnum getEnumWithPrototype() {
        return enumWithPrototype;
    }

    public TestEnum getEnumWithoutPrototype() {
        return enumWithoutPrototype;
    }

    public void setEnumWithPrototype(TestEnum enumWithPrototype) {
        this.enumWithPrototype = enumWithPrototype;
    }

    public void setEnumWithoutPrototype(TestEnum enumWithoutPrototype) {
        this.enumWithoutPrototype = enumWithoutPrototype;
    }
}
