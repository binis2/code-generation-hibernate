/*Generated code by Binis' code generator.*/
package net.binis.codegen;

import javax.annotation.processing.Generated;
import java.util.Set;
import java.util.List;

@Generated(value = "net.binis.codegen.TestPrototype", comments = "TestImpl")
public interface Test {
    TestEnum getEnumWithPrototype();
    List<TestEnum> getEnumWithPrototypeList();
    Set<TestEnum> getEnumWithPrototypeSet();
    TestEnum getEnumWithoutPrototype();
    List<TestEnum> getEnumWithoutPrototypeList();
    Set<TestEnum> getEnumWithoutPrototypeSet();

    void setEnumWithPrototype(TestEnum enumWithPrototype);
    void setEnumWithPrototypeList(List<TestEnum> enumWithPrototypeList);
    void setEnumWithPrototypeSet(Set<TestEnum> enumWithPrototypeSet);
    void setEnumWithoutPrototype(TestEnum enumWithoutPrototype);
    void setEnumWithoutPrototypeList(List<TestEnum> enumWithoutPrototypeList);
    void setEnumWithoutPrototypeSet(Set<TestEnum> enumWithoutPrototypeSet);
}
