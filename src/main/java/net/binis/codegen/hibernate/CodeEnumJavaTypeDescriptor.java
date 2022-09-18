package net.binis.codegen.hibernate;

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

import net.binis.codegen.factory.CodeFactory;
import net.binis.codegen.objects.base.enumeration.CodeEnum;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry;

import java.util.UUID;

import static java.util.Objects.isNull;

public class CodeEnumJavaTypeDescriptor<T extends CodeEnum> extends AbstractTypeDescriptor<T> {
    @SuppressWarnings("unchecked")
    public CodeEnumJavaTypeDescriptor(Class<T> type) {
        super(type, ImmutableMutabilityPlan.INSTANCE);
        JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( this );
    }

    @Override
    public String toString(T value) {
        return value == null ? "<null>" : value.name();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T fromString(String string) {
        return string == null ? null : (T) CodeFactory.enumValueOf(getJavaType(), string);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
        if (String.class.equals(type)) {
            return (X) toName(value);
        } else if (Integer.class.isInstance(type)) {
            return (X) toOrdinal(value);
        }

        return (X) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> T wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null;
        } else if (String.class.isInstance(value)) {
            return fromName((String) value);
        } else if (Integer.class.isInstance(value)) {
            return fromOrdinal((Integer) value);
        }

        return (T) value;
    }


    public <E extends CodeEnum> Integer toOrdinal(E domainForm) {
        if (domainForm == null) {
            return null;
        }
        return domainForm.ordinal();
    }

    @SuppressWarnings("unchecked")
    public <E extends CodeEnum> E fromOrdinal(Integer relationalForm) {
        if (relationalForm == null) {
            return null;
        }

        var result = CodeFactory.enumValueOf(getJavaType(), relationalForm);

        if (isNull(result)) {
            result = CodeFactory.initializeUnknownEnumValue(getJavaType(), UUID.randomUUID().toString(), relationalForm);
        }

        return (E) result;
    }

    @SuppressWarnings("unchecked")
    public T fromName(String relationalForm) {
        if (relationalForm == null) {
            return null;
        }
        var result = CodeFactory.enumValueOf(getJavaType(), relationalForm);

        if (isNull(result)) {
            result = CodeFactory.initializeUnknownEnumValue(getJavaType(), relationalForm, Integer.MIN_VALUE);
        }

        return (T) result;
    }

    public String toName(T domainForm) {
        if (domainForm == null) {
            return null;
        }
        return domainForm.name();
    }
}
