package net.binis.codegen.hibernate;

/*-
 * #%L
 * code-generator
 * %%
 * Copyright (C) 2021 - 2024 Binis Belev
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

import net.binis.codegen.generation.core.Helpers;
import net.binis.codegen.objects.Pair;
import net.binis.codegen.test.BaseCodeGenTest;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class HibernateTest extends BaseCodeGenTest {

    @Test
    void enrichHibernate() {
        testMultiPass(List.of(
                Pair.of(List.of(Triple.of("enrich/enrichHibernateEnum.java", "enrich/enrichHibernateEnum-0.java", "enrich/enrichHibernateEnum-1.java")), 1),
                Pair.of(List.of(Triple.of("enrich/enrichHibernate.java", "enrich/enrichHibernate-0.java", "enrich/enrichHibernate-1.java")), 2)));
    }

}
