package net.binis.codegen.hibernate.objects;

/*-
 * #%L
 * code-generator-hibernate
 * %%
 * Copyright (C) 2021 - 2023 Binis Belev
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import net.binis.codegen.enrich.HibernateEnricher;
import net.binis.codegen.spring.annotation.builder.CodeQueryBuilder;

import java.util.List;
import java.util.UUID;

@CodeQueryBuilder(implementationPackage = "net.binis.codegen.hibernate.db", enrichers = HibernateEnricher.class)
@Entity
public interface TestEnumsEntityPrototype {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", nullable = false, updatable = false)
    UUID id();

    TestEnumPrototype testEnum();

    TestMixEnumPrototype testMixEnum();

    List<TestEnumPrototype> testList();

    List<TestMixEnumPrototype> testMixList();

}
