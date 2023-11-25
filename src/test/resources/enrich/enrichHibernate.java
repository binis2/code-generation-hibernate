package net.binis.codegen;

import net.binis.codegen.annotation.CodePrototype;
import net.binis.codegen.enrich.HibernateEnricher;
import java.util.List;
import java.util.Set;

@CodePrototype(enrichers = {HibernateEnricher.class})
public interface TestPrototype {
    TestEnumPrototype enumWithPrototype();
    TestEnum enumWithoutPrototype();

    List<TestEnumPrototype> enumWithPrototypeList();
    List<TestEnum> enumWithoutPrototypeList();

    Set<TestEnumPrototype> enumWithPrototypeSet();
    Set<TestEnum> enumWithoutPrototypeSet();

}
