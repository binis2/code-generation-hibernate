package net.binis.codegen.hibernate;

/*-
 * #%L
 * code-generator-hibernate
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

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import net.binis.codegen.factory.CodeFactory;
import net.binis.codegen.objects.base.enumeration.CodeEnum;
import org.hibernate.boot.model.process.internal.EnumeratedValueConverter;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;


import static jakarta.persistence.EnumType.ORDINAL;
import static java.util.Objects.isNull;
import static org.hibernate.internal.util.collections.CollectionHelper.setOfSize;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.ENUM;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;
import static org.hibernate.type.SqlTypes.ORDINAL_ENUM;
import static org.hibernate.type.SqlTypes.NAMED_ORDINAL_ENUM;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARCHAR;

public class CodeEnumJavaType<T extends CodeEnum> extends AbstractClassJavaType<T> {

    public CodeEnumJavaType(Class<T> type) {
        super(type, ImmutableMutabilityPlan.instance());
    }

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
        return context.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor(sqlType(context));
    }

    private int sqlType(JdbcTypeIndicators context) {
        final var enumType = context.getEnumeratedType();
        final boolean preferNativeEnumTypes = context.isPreferNativeEnumTypesEnabled();
        final var jdbcTypeRegistry = context.getTypeConfiguration().getJdbcTypeRegistry();
        return switch (enumType == null ? ORDINAL : enumType) {
            case ORDINAL:
                if (preferNativeEnumTypes && jdbcTypeRegistry.hasRegisteredDescriptor(ORDINAL_ENUM)) {
                    yield ORDINAL_ENUM;
                } else if (preferNativeEnumTypes && jdbcTypeRegistry.hasRegisteredDescriptor(NAMED_ORDINAL_ENUM)) {
                    yield NAMED_ORDINAL_ENUM;
                } else {
                    yield hasManyValues() ? SMALLINT : TINYINT;
                }
            case STRING:
                if (jdbcTypeRegistry.hasRegisteredDescriptor(ENUM)) {
                    yield ENUM;
                } else if (preferNativeEnumTypes && jdbcTypeRegistry.hasRegisteredDescriptor(NAMED_ENUM)) {
                    yield NAMED_ENUM;
                } else if (context.getColumnLength() == 1) {
                    yield context.isNationalized() ? NCHAR : CHAR;
                } else {
                    yield context.isNationalized() ? NVARCHAR : VARCHAR;
                }
        };
    }

    public boolean hasManyValues() {
        // a bit arbitrary, but gives us some headroom
        return CodeFactory.enumValues(getJavaType()).length > 128;
    }

    @Override
    public boolean useObjectEqualsHashCode() {
        return true;
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
    public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
        if (String.class.equals(type)) {
            return type.cast(toName(value));
        } else if (Long.class.equals(type)) {
            return type.cast(toLong(value));
        } else if (Integer.class.equals(type)) {
            return type.cast(toInteger(value));
        } else if (Short.class.equals(type)) {
            return type.cast(toShort(value));
        } else if (Byte.class.equals(type)) {
            return type.cast(toByte(value));
        } else if (type.isInstance(value)) {
            return type.cast(value);
        } else {
            throw unknownUnwrap(type);
        }
    }

    @Override
    public <X> T wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null;
        } else if (value instanceof String string) {
            return fromName(string);
        } else if (value instanceof Long longValue) {
            return fromLong(longValue);
        } else if (value instanceof Integer integerValue) {
            return fromInteger(integerValue);
        } else if (value instanceof Short shortValue) {
            return fromShort(shortValue);
        } else if (value instanceof Byte byteValue) {
            return fromByte(byteValue);
        } else if (value instanceof Number number) {
            return fromLong(number.longValue());
        } else if (getJavaType().isInstance(value)) {
            return (T) value;
        } else if (isInstance(value)) {
            return cast(value);
        } else {
            throw unknownWrap(value.getClass());
        }
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
        return CodeFactory.enumValuesMap(getJavaTypeClass()).get(relationalForm.intValue());
    }

    /**
     * Interpret a numeric value as the ordinal of the enum type
     */
    public T fromShort(Short relationalForm) {
        if (relationalForm == null) {
            return null;
        }
        return CodeFactory.enumValuesMap(getJavaTypeClass()).get(relationalForm.intValue());
    }

    /**
     * Interpret a numeric value as the ordinal of the enum type
     */
    public T fromInteger(Integer relationalForm) {
        if (relationalForm == null) {
            return null;
        }

        var result = CodeFactory.enumValueOf(getJavaType(), relationalForm);

        if (isNull(result)) {
            result = CodeFactory.initializeUnknownEnumValue(getJavaType(), UUID.randomUUID().toString(), relationalForm);
        }

        return result;
    }

    /**
     * Interpret a numeric value as the ordinal of the enum type
     */
    public T fromLong(Long relationalForm) {
        if (relationalForm == null) {
            return null;
        }
        return CodeFactory.enumValuesMap(getJavaTypeClass()).get(relationalForm.intValue());
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
        return domainForm == null ? null : domainForm.name();
    }

    /**
     * Interpret a string value as the named value of the enum type
     */
    public T fromName(String relationalForm) {
        if (relationalForm == null) {
            return null;
        }
        var result = CodeFactory.enumValueOf(getJavaType(), relationalForm);

        if (isNull(result)) {
            result = CodeFactory.initializeUnknownEnumValue(getJavaType(), relationalForm, Integer.MIN_VALUE);
        }

        return result;
    }

    @Override
    public String getCheckCondition(String columnName, JdbcType jdbcType, BasicValueConverter<T, ?> converter, Dialect dialect) {
        if (converter != null
                && jdbcType.getDefaultSqlTypeCode() != NAMED_ENUM) {
            return renderConvertedEnumCheckConstraint(columnName, jdbcType, converter, dialect);
        } else if (jdbcType.isInteger()) {
            int max = getJavaTypeClass().getEnumConstants().length - 1;
            return dialect.getCheckCondition(columnName, 0, max);
        } else if (jdbcType.isString()) {
            return dialect.getCheckCondition(columnName, Arrays.stream(CodeFactory.enumValues(getJavaType())).map(CodeEnum::name).toList().toArray(new String[0]));
        } else {
            return null;
        }
    }

    private String renderConvertedEnumCheckConstraint(
            String columnName,
            JdbcType jdbcType,
            BasicValueConverter<T, ?> converter,
            Dialect dialect) {
        final Set<?> valueSet = valueSet(jdbcType, converter);
        return valueSet == null ? null : dialect.getCheckCondition(columnName, valueSet, jdbcType);
    }

    @SuppressWarnings("unchecked")
    private Set valueSet(JdbcType jdbcType, BasicValueConverter converter) {
        // for `@EnumeratedValue` we already have the possible values...
        if (converter instanceof EnumeratedValueConverter enumeratedValueConverter) {
            return enumeratedValueConverter.getRelationalValueSet();
        } else {
            if (!SqlTypes.isIntegral(jdbcType.getJdbcTypeCode())
                    && !SqlTypes.isCharacterType(jdbcType.getJdbcTypeCode())) {
                // we only support adding check constraints for generalized conversions to
                // INTEGER, SMALLINT, TINYINT, (N)CHAR, (N)VARCHAR, LONG(N)VARCHAR
                return null;
            } else {
                final T[] enumConstants = getJavaTypeClass().getEnumConstants();
                final Set valueSet = setOfSize(enumConstants.length);
                for (T enumConstant : enumConstants) {
                    valueSet.add(converter.toRelationalValue(enumConstant));
                }
                return valueSet;
            }
        }
    }
}
