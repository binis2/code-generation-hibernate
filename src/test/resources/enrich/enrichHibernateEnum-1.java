/*Generated code by Binis' code generator.*/
package net.binis.codegen;

/*-
 * #%L
 * code-generator-hibernate
 * %%
 * Copyright (C) 2021 - 2022 Binis Belev
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

import net.binis.codegen.objects.base.enumeration.CodeEnum;
import net.binis.codegen.factory.CodeFactory;
import net.binis.codegen.annotation.Default;
import javax.annotation.processing.Generated;

@Generated(value = "TestEnumPrototype", comments = "TestEnumImpl")
@Default("net.binis.codegen.TestEnumImpl")
public interface TestEnum extends CodeEnum {

    static final TestEnum KNOWN = CodeFactory.initializeEnumValue(TestEnum.class, "KNOWN", 1);

    static final TestEnum UNKNOWN = CodeFactory.initializeEnumValue(TestEnum.class, "UNKNOWN", 0);

    static TestEnum valueOf(String name) {
        return CodeFactory.enumValueOf(TestEnum.class, name);
    }

    static TestEnum valueOf(int ordinal) {
        return CodeFactory.enumValueOf(TestEnum.class, ordinal);
    }

    static TestEnum[] values() {
        return CodeFactory.enumValues(TestEnum.class);
    }
}
