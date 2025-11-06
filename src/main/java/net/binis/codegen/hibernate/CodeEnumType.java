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

import jakarta.persistence.Enumerated;
import jakarta.persistence.MapKeyEnumerated;
import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.objects.base.enumeration.CodeEnum;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.annotations.Nationalized;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.LoggableUserType;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import static jakarta.persistence.EnumType.ORDINAL;
import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

@Slf4j
@SuppressWarnings("unchecked")
public class CodeEnumType implements EnhancedUserType<CodeEnum>, DynamicParameterizedType, LoggableUserType, TypeConfigurationAware, Serializable {

    public static final String ENUM = "enumClass";
    public static final String NAMED = "useNamed";
    public static final String TYPE = "type";

    private Class enumClass;
    private boolean useString;

    private JdbcType jdbcType;
    private CodeEnumJavaType<CodeEnum> enumJavaType;

    private TypeConfiguration typeConfiguration;

    public CodeEnumType() {
    }

    public Class getEnumClass() {
        return enumClass;
    }

    @Override
    public JdbcType getJdbcType(TypeConfiguration typeConfiguration) {
        return jdbcType;
    }

    @Override
    public void setParameterValues(Properties parameters) {
        final ParameterType reader = (ParameterType) parameters.get(PARAMETER_TYPE);

        if (parameters.containsKey(ENUM)) {
            final String enumClassName = (String) parameters.get(ENUM);
            try {
                enumClass = ReflectHelper.classForName(enumClassName, this.getClass()).asSubclass(Enum.class);
            } catch (ClassNotFoundException exception) {
                throw new HibernateException("Enum class not found: " + enumClassName, exception);
            }
        } else if (reader != null) {
            enumClass = reader.getReturnedClass().asSubclass(CodeEnum.class);
        }

        enumJavaType = new CodeEnumJavaType<>(enumClass);

        if (parameters.containsKey(TYPE)) {
            int jdbcTypeCode = Integer.parseInt((String) parameters.get(TYPE));
            jdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor(jdbcTypeCode);
        } else {
            final LocalJdbcTypeIndicators indicators;
            final Long columnLength = reader == null ? null : reader.getColumnLengths()[0];
            if (parameters.containsKey(NAMED)) {
                useString = true;
                indicators = new LocalJdbcTypeIndicators(
                        getBoolean(NAMED, parameters) ? STRING : ORDINAL,
                        false,
                        columnLength
                );
            } else {
                indicators = new LocalJdbcTypeIndicators(
                        getEnumType(reader),
                        isNationalized(reader),
                        columnLength
                );
            }
            jdbcType = enumJavaType.getRecommendedJdbcType(indicators);
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "Using {}-based conversion for Enum {}",
                    isOrdinal() ? "ORDINAL" : "NAMED",
                    enumClass.getName()
            );
        }
    }

    private jakarta.persistence.EnumType getEnumType(ParameterType reader) {
        if (reader != null) {
            if (reader.isPrimaryKey()) {
                final MapKeyEnumerated enumAnn = getAnnotation(reader.getAnnotationsMethod(), MapKeyEnumerated.class);
                if (enumAnn != null) {
                    return enumAnn.value();
                }
            }
            final Enumerated enumAnn = getAnnotation(reader.getAnnotationsMethod(), Enumerated.class);
            if (enumAnn != null) {
                return enumAnn.value();
            }
        }
        return useString ? STRING : ORDINAL;
    }

    private boolean isNationalized(ParameterType reader) {
        return typeConfiguration.getCurrentBaseSqlTypeIndicators().isNationalized()
                || reader != null && getAnnotation(reader.getAnnotationsMethod(), Nationalized.class) != null;
    }

    @SuppressWarnings("unchecked")
    private <A extends Annotation> A getAnnotation(Annotation[] annotations, Class<A> annotationType) {
        for (Annotation annotation : annotations) {
            if (annotationType.isInstance(annotation)) {
                return (A) annotation;
            }
        }
        return null;
    }

    @Override
    public int getSqlType() {
        verifyConfigured();
        return jdbcType.getJdbcTypeCode();
    }

    @Override
    public Class returnedClass() {
        return enumClass;
    }

    @Override
    public boolean equals(CodeEnum x, CodeEnum y) throws HibernateException {
        return x == y;
    }

    @Override
    public int hashCode(CodeEnum x) throws HibernateException {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public CodeEnum nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        verifyConfigured();
        return jdbcType.getExtractor(enumJavaType).extract(rs, position, session);
    }

    private void verifyConfigured() {
        if (enumJavaType == null) {
            throw new AssertionFailure("EnumType (" + enumClass.getName() + ") not properly, fully configured");
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, CodeEnum value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        verifyConfigured();
        jdbcType.getBinder(enumJavaType).bind(st, value, index, session);
    }

    @Override
    public CodeEnum deepCopy(CodeEnum value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(CodeEnum value) throws HibernateException {
        return null;
    }

    @Override
    public CodeEnum assemble(Serializable cached, Object owner) throws HibernateException {
        return (CodeEnum) cached;
    }

    @Override
    public CodeEnum replace(CodeEnum original, CodeEnum target, Object owner) throws HibernateException {
        return original;
    }

    @Override
    public TypeConfiguration getTypeConfiguration() {
        return typeConfiguration;
    }

    @Override
    public void setTypeConfiguration(TypeConfiguration typeConfiguration) {
        this.typeConfiguration = typeConfiguration;
    }

    @Override
    public String toSqlLiteral(CodeEnum value) {
        verifyConfigured();
        return isOrdinal()
                ? Integer.toString(value.ordinal())
                : "'" + value.name() + "'";
    }

    @Override
    public String toString(CodeEnum value) {
        verifyConfigured();
        return enumJavaType.toName(value);
    }

    @Override
    public CodeEnum fromStringValue(CharSequence sequence) {
        verifyConfigured();
        return enumJavaType.fromName(sequence.toString());
    }

    @Override
    public String toLoggableString(Object value, SessionFactoryImplementor factory) {
        verifyConfigured();
        return enumJavaType.toString((CodeEnum) value);
    }

    public boolean isOrdinal() {
        verifyConfigured();
        return jdbcType.isInteger();
    }

    private class LocalJdbcTypeIndicators implements JdbcTypeIndicators {
        private final jakarta.persistence.EnumType enumType;
        private final boolean nationalized;
        private final Long columnLength;

        private LocalJdbcTypeIndicators(jakarta.persistence.EnumType enumType, boolean nationalized, Long columnLength) {
            this.enumType = enumType;
            this.nationalized = nationalized;
            this.columnLength = columnLength;
        }

        @Override
        public TypeConfiguration getTypeConfiguration() {
            return typeConfiguration;
        }

        @Override
        public jakarta.persistence.EnumType getEnumeratedType() {
            return enumType != null ? enumType : typeConfiguration.getCurrentBaseSqlTypeIndicators().getEnumeratedType();
        }

        @Override
        public boolean isNationalized() {
            return nationalized;
        }


        @Override
        public long getColumnLength() {
            return columnLength == null ? NO_COLUMN_LENGTH : columnLength;
        }

        @Override
        public Dialect getDialect() {
            return typeConfiguration.getCurrentBaseSqlTypeIndicators().getDialect();
        }
    }
}
