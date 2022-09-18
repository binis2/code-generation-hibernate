package net.binis.codegen;

import net.binis.codegen.annotation.CodePrototype;
import net.binis.codegen.enrich.HibernateEnricher;

@CodePrototype(enrichers = {HibernateEnricher.class})
public interface TestPrototype {
    TestEnumPrototype enumWithPrototype();
    TestEnum enumWithoutPrototype();
}
