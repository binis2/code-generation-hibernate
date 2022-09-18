/*Generated code by Binis' code generator.*/
package net.binis.codegen;

import javax.annotation.processing.Generated;

@Generated(value = "TestPrototype", comments = "TestImpl")
public interface Test {
    TestEnum getEnumWithPrototype();
    TestEnum getEnumWithoutPrototype();

    void setEnumWithPrototype(TestEnum enumWithPrototype);
    void setEnumWithoutPrototype(TestEnum enumWithoutPrototype);
}
