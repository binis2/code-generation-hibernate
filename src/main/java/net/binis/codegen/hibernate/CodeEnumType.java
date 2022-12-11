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

import jakarta.persistence.Enumerated;
import jakarta.persistence.MapKeyEnumerated;
import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.objects.base.enumeration.CodeEnum;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.annotations.Nationalized;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.BasicJavaType;
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
import java.sql.Types;
import java.util.Locale;
import java.util.Properties;

@Slf4j
@SuppressWarnings("unchecked")
public class CodeEnumType
        implements EnhancedUserType<CodeEnum>, DynamicParameterizedType, LoggableUserType, TypeConfigurationAware, Serializable {

    public static final String ENUM = "enumClass";
    public static final String NAMED = "useNamed";
    public static final String TYPE = "type";

    private Class enumClass;

    private CodeEnumValueConverter enumValueConverter;
    private JdbcType jdbcType;
    private ValueExtractor<Object> jdbcValueExtractor;
    private ValueBinder<Object> jdbcValueBinder;

    private TypeConfiguration typeConfiguration;

    public CodeEnumType() {
    }

    public CodeEnumType(
            Class<CodeEnum> enumClass,
            CodeEnumValueConverter enumValueConverter,
            TypeConfiguration typeConfiguration) {
        this.enumClass = enumClass;
        this.typeConfiguration = typeConfiguration;

        this.enumValueConverter = enumValueConverter;
        this.jdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor(enumValueConverter.getJdbcTypeCode());
        this.jdbcValueExtractor = jdbcType.getExtractor(enumValueConverter.getRelationalJavaType());
        this.jdbcValueBinder = jdbcType.getBinder(enumValueConverter.getRelationalJavaType());
    }

    public CodeEnumValueConverter getEnumValueConverter() {
        return enumValueConverter;
    }

    @Override
    public JdbcType getJdbcType(TypeConfiguration typeConfiguration) {
        return jdbcType;
    }

    @Override
    public BasicValueConverter<CodeEnum, Object> getValueConverter() {
        return enumValueConverter;
    }

    @Override
    public void setParameterValues(Properties parameters) {
        // IMPL NOTE: we handle 2 distinct cases here:
        // 		1) we are passed a ParameterType instance in the incoming Properties - generally
        //			speaking this indicates the annotation-binding case, and the passed ParameterType
        //			represents information about the attribute and annotation
        //		2) we are not passed a ParameterType - generally this indicates a hbm.xml binding case.
        final ParameterType reader = (ParameterType) parameters.get(PARAMETER_TYPE);

        // the `reader != null` block handles annotations, while the `else` block
        // handles hbm.xml
        if (reader != null) {
            enumClass = reader.getReturnedClass().asSubclass(CodeEnum.class);

            final Long columnLength = reader.getColumnLengths()[0];

            final boolean isOrdinal;
            final jakarta.persistence.EnumType enumType = getEnumType(reader);
            if (enumType == null) {
                isOrdinal = true;
            } else if (jakarta.persistence.EnumType.ORDINAL.equals(enumType)) {
                isOrdinal = true;
            } else if (jakarta.persistence.EnumType.STRING.equals(enumType)) {
                isOrdinal = false;
            } else {
                throw new AssertionFailure("Unknown EnumType: " + enumType);
            }

            var type = typeConfiguration
                    .getJavaTypeRegistry()
                    .getDescriptor(enumClass);

            if (!(type instanceof CodeEnumJavaType)) {
                typeConfiguration.getJavaTypeRegistry().addDescriptor(new CodeEnumJavaType(enumClass));
            }

            final CodeEnumJavaType enumJavaType = (CodeEnumJavaType) typeConfiguration
                    .getJavaTypeRegistry()
                    .getDescriptor(enumClass);

            final LocalJdbcTypeIndicators indicators = new LocalJdbcTypeIndicators(
                    enumType,
                    columnLength,
                    reader
            );

            final BasicJavaType<?> relationalJavaType = resolveRelationalJavaType(
                    indicators,
                    enumJavaType
            );

            this.jdbcType = relationalJavaType.getRecommendedJdbcType(indicators);

            if (isOrdinal) {
                this.enumValueConverter = new OrdinalCodeEnumValueConverter(
                        enumJavaType,
                        jdbcType,
                        relationalJavaType
                );
            } else {
                this.enumValueConverter = new NamedCodeEnumValueConverter(
                        enumJavaType,
                        jdbcType,
                        relationalJavaType
                );
            }
        } else {
            final String enumClassName = (String) parameters.get(ENUM);
            try {
                enumClass = ReflectHelper.classForName(enumClassName, this.getClass()).asSubclass(Enum.class);
            } catch (ClassNotFoundException exception) {
                throw new HibernateException("Enum class not found: " + enumClassName, exception);
            }

            this.enumValueConverter = interpretParameters(parameters);
            this.jdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor(enumValueConverter.getJdbcTypeCode());
        }
        this.jdbcValueExtractor = jdbcType.getExtractor(enumValueConverter.getRelationalJavaType());
        this.jdbcValueBinder = jdbcType.getBinder(enumValueConverter.getRelationalJavaType());

        log.debug(
                "Using {}-based conversion for Enum {}",
                isOrdinal() ? "ORDINAL" : "NAMED",
                enumClass.getName()
        );
    }

    private BasicJavaType<?> resolveRelationalJavaType(
            LocalJdbcTypeIndicators indicators,
            CodeEnumJavaType<?> enumJavaType) {
        return enumJavaType.getRecommendedJdbcType(indicators).getJdbcRecommendedJavaTypeMapping(
                null,
                null,
                typeConfiguration
        );
    }

    private jakarta.persistence.EnumType getEnumType(ParameterType reader) {
        if (reader == null) {
            return null;
        }

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

        return null;
    }

    private <A extends Annotation> A getAnnotation(Annotation[] annotations, Class<A> anClass) {
        for (Annotation annotation : annotations) {
            if (anClass.isInstance(annotation)) {
                return (A) annotation;
            }
        }
        return null;
    }

    private CodeEnumValueConverter interpretParameters(Properties parameters) {
        //noinspection rawtypes
        var enumJavaType = (CodeEnumJavaType) typeConfiguration
                .getJavaTypeRegistry()
                .getDescriptor(enumClass);

        // this method should only be called for hbm.xml handling
        assert parameters.get(PARAMETER_TYPE) == null;

        final LocalJdbcTypeIndicators localIndicators = new LocalJdbcTypeIndicators(
                // use ORDINAL as default for hbm.xml mappings
                jakarta.persistence.EnumType.ORDINAL,
                // Is there a reasonable value here?  Limits the
                // number of enums that can be stored:
                // 	1 = 10
                //	2 = 100
                //  etc
                -1L,
                null
        );
        final BasicJavaType<?> stringJavaType = (BasicJavaType<?>) typeConfiguration.getJavaTypeRegistry().getDescriptor(String.class);
        final BasicJavaType<?> integerJavaType = (BasicJavaType<?>) typeConfiguration.getJavaTypeRegistry().getDescriptor(Integer.class);

        if (parameters.containsKey(NAMED)) {
            final boolean useNamed = ConfigurationHelper.getBoolean(NAMED, parameters);
            if (useNamed) {
                //noinspection rawtypes
                return new NamedCodeEnumValueConverter(
                        enumJavaType,
                        stringJavaType.getRecommendedJdbcType(localIndicators),
                        stringJavaType
                );
            } else {
                //noinspection rawtypes
                return new OrdinalCodeEnumValueConverter(
                        enumJavaType,
                        integerJavaType.getRecommendedJdbcType(localIndicators),
                        typeConfiguration.getJavaTypeRegistry().getDescriptor(Integer.class)
                );
            }
        }

        if (parameters.containsKey(TYPE)) {
            final int type = Integer.decode((String) parameters.get(TYPE));
            if (isNumericType(type)) {
                //noinspection rawtypes
                return new OrdinalCodeEnumValueConverter(
                        enumJavaType,
                        integerJavaType.getRecommendedJdbcType(localIndicators),
                        typeConfiguration.getJavaTypeRegistry().getDescriptor(Integer.class)
                );
            } else if (isCharacterType(type)) {
                //noinspection rawtypes
                return new NamedCodeEnumValueConverter(
                        enumJavaType,
                        stringJavaType.getRecommendedJdbcType(localIndicators),
                        stringJavaType
                );
            } else {
                throw new HibernateException(
                        String.format(
                                Locale.ENGLISH,
                                "Passed JDBC type code [%s] not recognized as numeric nor character",
                                type
                        )
                );
            }
        }

        // the fallback
        return new OrdinalCodeEnumValueConverter(
                enumJavaType,
                integerJavaType.getRecommendedJdbcType(localIndicators),
                typeConfiguration.getJavaTypeRegistry().getDescriptor(Integer.class)
        );
    }

    private boolean isCharacterType(int jdbcTypeCode) {
        switch (jdbcTypeCode) {
            case Types.CHAR,
                    Types.LONGVARCHAR,
                    Types.VARCHAR -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean isNumericType(int jdbcTypeCode) {
        switch (jdbcTypeCode) {
            case Types.INTEGER,
                    Types.NUMERIC,
                    Types.SMALLINT,
                    Types.TINYINT,
                    Types.BIGINT,
                    Types.DECIMAL,
                    Types.DOUBLE,
                    Types.FLOAT -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public int getSqlType() {
        verifyConfigured();
        return enumValueConverter.getJdbcTypeCode();
    }

    @Override
    public Class<CodeEnum> returnedClass() {
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
        final Object relational = jdbcValueExtractor.extract(rs, position, session);
        return (CodeEnum) enumValueConverter.toDomainValue(relational);
    }

    private void verifyConfigured() {
        if (enumValueConverter == null || jdbcValueBinder == null || jdbcValueExtractor == null) {
            throw new AssertionFailure("EnumType (" + enumClass.getName() + ") not properly, fully configured");
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, CodeEnum value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        verifyConfigured();
        jdbcValueBinder.bind(st, enumValueConverter.toRelationalValue(value), index, session);
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
        return (Serializable) enumValueConverter.toRelationalValue(value);
    }

    @Override
    public CodeEnum assemble(Serializable cached, Object owner) throws HibernateException {
        return (CodeEnum) enumValueConverter.toDomainValue(cached);
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
        return enumValueConverter.toSqlLiteral(value);
    }

    @Override
    public String toString(CodeEnum value) {
        verifyConfigured();
        return enumValueConverter.getRelationalJavaType().toString(enumValueConverter.toRelationalValue(value));
    }

    @Override
    public CodeEnum fromStringValue(CharSequence sequence) {
        verifyConfigured();
        return (CodeEnum) enumValueConverter.toDomainValue(enumValueConverter.getRelationalJavaType().fromString(sequence));
    }

    @Override
    public String toLoggableString(Object value, SessionFactoryImplementor factory) {
        verifyConfigured();
        return enumValueConverter.getDomainJavaType().toString((CodeEnum) value);
    }

    public boolean isOrdinal() {
        verifyConfigured();
        return enumValueConverter instanceof OrdinalEnumValueConverter;
    }

    private class LocalJdbcTypeIndicators implements JdbcTypeIndicators {
        private final jakarta.persistence.EnumType enumType;
        private final Long columnLength;
        private final ParameterType reader;

        public LocalJdbcTypeIndicators(jakarta.persistence.EnumType enumType, Long columnLength, ParameterType reader) {
            this.enumType = enumType;
            this.columnLength = columnLength;
            this.reader = reader;
        }

        @Override
        public TypeConfiguration getTypeConfiguration() {
            return typeConfiguration;
        }

        @Override
        public jakarta.persistence.EnumType getEnumeratedType() {
            if (enumType != null) {
                return enumType;
            }
            return typeConfiguration.getCurrentBaseSqlTypeIndicators().getEnumeratedType();
        }

        @Override
        public boolean isNationalized() {
            return isNationalized(reader);
        }

        private boolean isNationalized(ParameterType reader) {
            if (typeConfiguration.getCurrentBaseSqlTypeIndicators().isNationalized()) {
                return true;
            }

            if (reader != null) {
                for (Annotation annotation : reader.getAnnotationsMethod()) {
                    if (annotation instanceof Nationalized) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public long getColumnLength() {
            return columnLength == null ? NO_COLUMN_LENGTH : columnLength;
        }
    }
}
