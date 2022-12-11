/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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

import jakarta.persistence.EnumType;
import net.binis.codegen.factory.CodeFactory;
import net.binis.codegen.objects.base.enumeration.CodeEnum;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;

public class CodeEnumJavaType<T extends CodeEnum> extends AbstractClassJavaType<T> {
    public CodeEnumJavaType(Class<T> type) {
        super(type, ImmutableMutabilityPlan.instance());
    }

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
        JdbcTypeRegistry registry = context.getTypeConfiguration().getJdbcTypeRegistry();
        if (context.getEnumeratedType() != null && context.getEnumeratedType() == EnumType.STRING) {
            if (context.getColumnLength() == 1) {
                return context.isNationalized()
                        ? registry.getDescriptor(Types.NCHAR)
                        : registry.getDescriptor(Types.CHAR);
            }

            return context.isNationalized()
                    ? registry.getDescriptor(Types.NVARCHAR)
                    : registry.getDescriptor(Types.VARCHAR);
        } else {
            return registry.getDescriptor(SqlTypes.SMALLINT);
        }
    }

    @Override
    public String toString(T value) {
        return value == null ? "<null>" : value.name();
    }

    @Override
    public T fromString(CharSequence string) {
        return string == null ? null : CodeFactory.enumValueOf(getJavaTypeClass(), string.toString());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
        if (String.class.equals(type)) {
            return (X) toName(value);
        } else if (Long.class.equals(type)) {
            return (X) toLong(value);
        } else if (Integer.class.equals(type)) {
            return (X) toInteger(value);
        } else if (Short.class.equals(type)) {
            return (X) toShort(value);
        } else if (Byte.class.equals(type)) {
            return (X) toByte(value);
        }
        return (X) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> T wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null;
        } else if (value instanceof String v) {
            return fromName(v);
        } else if (value instanceof Long v) {
            return fromLong(v);
        } else if (value instanceof Integer v) {
            return fromInteger(v);
        } else if (value instanceof Short v) {
            return fromShort(v);
        } else if (value instanceof Byte v) {
            return fromByte(v);
        }

        return (T) value;
    }

    /**
     * Convert a value of the enum type to its ordinal value
     */
    public Byte toByte(T domainForm) {
        if (domainForm == null) {
            return null;
        }
        return (byte) domainForm.ordinal();
    }

    /**
     * Convert a value of the enum type to its ordinal value
     */
    public Short toShort(T domainForm) {
        if (domainForm == null) {
            return null;
        }
        return (short) domainForm.ordinal();
    }

    /**
     * Convert a value of the enum type to its ordinal value
     */
    public Integer toInteger(T domainForm) {
        if (domainForm == null) {
            return null;
        }
        return domainForm.ordinal();
    }

    /**
     * Convert a value of the enum type to its ordinal value
     */
    public Long toLong(T domainForm) {
        if (domainForm == null) {
            return null;
        }
        return (long) domainForm.ordinal();
    }

    /**
     * Convert a value of the enum type to its ordinal value
     */
    public Integer toOrdinal(T domainForm) {
        return toInteger(domainForm);
    }

    /**
     * Interpret a numeric value as the ordinal of the enum type
     */
    public T fromByte(Byte relationalForm) {
        if (relationalForm == null) {
            return null;
        }
        return getJavaTypeClass().getEnumConstants()[relationalForm];
    }

    /**
     * Interpret a numeric value as the ordinal of the enum type
     */
    public T fromShort(Short relationalForm) {
        if (relationalForm == null) {
            return null;
        }
        return getJavaTypeClass().getEnumConstants()[relationalForm];
    }

    /**
     * Interpret a numeric value as the ordinal of the enum type
     */
    public T fromInteger(Integer relationalForm) {
        if (relationalForm == null) {
            return null;
        }
        return getJavaTypeClass().getEnumConstants()[relationalForm];
    }

    /**
     * Interpret a numeric value as the ordinal of the enum type
     */
    public T fromLong(Long relationalForm) {
        if (relationalForm == null) {
            return null;
        }
        return getJavaTypeClass().getEnumConstants()[relationalForm.intValue()];
    }

    /**
     * Interpret a numeric value as the ordinal of the enum type
     */
    public T fromOrdinal(Integer relationalForm) {
        return fromInteger(relationalForm);
    }

    /**
     * Convert a value of the enum type to its name value
     */
    public String toName(T domainForm) {
        if (domainForm == null) {
            return null;
        }
        return domainForm.name();
    }

    /**
     * Interpret a String value as the named value of the enum type
     */
    public T fromName(String relationalForm) {
        if (relationalForm == null) {
            return null;
        }
        return CodeFactory.enumValueOf(getJavaTypeClass(), relationalForm.trim());
    }

    @Override
    public String getCheckCondition(String columnName, JdbcType jdbcType, Dialect dialect) {
        return null;
    }

}
