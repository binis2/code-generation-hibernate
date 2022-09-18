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

import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.objects.base.enumeration.CodeEnum;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.LoggableUserType;

import javax.persistence.Enumerated;
import javax.persistence.MapKeyEnumerated;
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
public class CodeEnumType<T extends CodeEnum>
        implements EnhancedUserType, DynamicParameterizedType, LoggableUserType, TypeConfigurationAware, Serializable {
    public static final String ENUM = "enumClass";
    public static final String NAMED = "useNamed";
    public static final String TYPE = "type";

    private Class enumClass;

    private CodeEnumValueConverter enumValueConverter;

    private TypeConfiguration typeConfiguration;

    @Override
    public void setParameterValues(Properties parameters) {
        final ParameterType reader = (ParameterType) parameters.get(DynamicParameterizedType.PARAMETER_TYPE);

        if (reader != null) {
            enumClass = reader.getReturnedClass().asSubclass(CodeEnum.class);

            final boolean isOrdinal;
            final javax.persistence.EnumType enumType = getEnumType(reader);
            if (enumType == null) {
                isOrdinal = true;
            } else if (javax.persistence.EnumType.ORDINAL.equals(enumType)) {
                isOrdinal = true;
            } else if (javax.persistence.EnumType.STRING.equals(enumType)) {
                isOrdinal = false;
            } else {
                throw new AssertionFailure("Unknown EnumType: " + enumType);
            }

            var descriptor = typeConfiguration
                    .getJavaTypeDescriptorRegistry()
                    .getDescriptor(enumClass);

            if (!(descriptor instanceof CodeEnumJavaTypeDescriptor)) {
                descriptor = new CodeEnumJavaTypeDescriptor(enumClass);
            }

            final CodeEnumJavaTypeDescriptor enumJavaDescriptor = (CodeEnumJavaTypeDescriptor) descriptor;

            if (isOrdinal) {
                this.enumValueConverter = new OrdinalCodeEnumValueConverter(enumJavaDescriptor);
            } else {
                this.enumValueConverter = new NamedCodeEnumValueConverter(enumJavaDescriptor);
            }
        } else {
            final String enumClassName = (String) parameters.get(ENUM);
            try {
                enumClass = ReflectHelper.classForName(enumClassName, this.getClass()).asSubclass(Enum.class);
            } catch (ClassNotFoundException exception) {
                throw new HibernateException("Enum class not found: " + enumClassName, exception);
            }

            this.enumValueConverter = interpretParameters(parameters);
        }

        log.debug(
                "Using {}-based conversion for CodeEnum {}",
                isOrdinal() ? "ORDINAL" : "NAMED",
                enumClass.getName()
        );
    }

    private javax.persistence.EnumType getEnumType(ParameterType reader) {
        javax.persistence.EnumType enumType = null;
        if (reader.isPrimaryKey()) {
            MapKeyEnumerated enumAnn = getAnnotation(reader.getAnnotationsMethod(), MapKeyEnumerated.class);
            if (enumAnn != null) {
                enumType = enumAnn.value();
            }
        } else {
            Enumerated enumAnn = getAnnotation(reader.getAnnotationsMethod(), Enumerated.class);
            if (enumAnn != null) {
                enumType = enumAnn.value();
            }
        }
        return enumType;
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
        final CodeEnumJavaTypeDescriptor javaTypeDescriptor = (CodeEnumJavaTypeDescriptor) typeConfiguration
                .getJavaTypeDescriptorRegistry()
                .getDescriptor(enumClass);
        if (parameters.containsKey(NAMED)) {
            final boolean useNamed = ConfigurationHelper.getBoolean(NAMED, parameters);
            if (useNamed) {
                return new NamedCodeEnumValueConverter(javaTypeDescriptor);
            } else {
                return new OrdinalCodeEnumValueConverter(javaTypeDescriptor);
            }
        }

        if (parameters.containsKey(TYPE)) {
            final int type = Integer.decode((String) parameters.get(TYPE));
            if (isNumericType(type)) {
                return new OrdinalCodeEnumValueConverter(javaTypeDescriptor);
            } else if (isCharacterType(type)) {
                return new NamedCodeEnumValueConverter(javaTypeDescriptor);
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
        return new OrdinalCodeEnumValueConverter(javaTypeDescriptor);
    }

    private boolean isCharacterType(int jdbcTypeCode) {
        switch (jdbcTypeCode) {
            case Types.CHAR:
            case Types.LONGVARCHAR:
            case Types.VARCHAR: {
                return true;
            }
            default: {
                return false;
            }
        }
    }

    private boolean isNumericType(int jdbcTypeCode) {
        switch (jdbcTypeCode) {
            case Types.INTEGER:
            case Types.NUMERIC:
            case Types.SMALLINT:
            case Types.TINYINT:
            case Types.BIGINT:
            case Types.DECIMAL:
            case Types.DOUBLE:
            case Types.FLOAT: {
                return true;
            }
            default:
                return false;
        }
    }

    @Override
    public int[] sqlTypes() {
        verifyConfigured();
        return new int[]{enumValueConverter.getJdbcTypeCode()};
    }

    @Override
    public Class<? extends CodeEnum> returnedClass() {
        return enumClass;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return x == y;
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws SQLException {
        verifyConfigured();
        return enumValueConverter.readValue(rs, names[0], session);
    }

    private void verifyConfigured() {
        if (enumValueConverter == null) {
            throw new AssertionFailure("EnumType (" + enumClass.getName() + ") not properly, fully configured");
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        verifyConfigured();
        enumValueConverter.writeValue(st, (CodeEnum) value, index, session);
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
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
    public String objectToSQLString(Object value) {
        verifyConfigured();
        return enumValueConverter.toSqlLiteral(value);
    }

    @Override
    public String toXMLString(Object value) {
        verifyConfigured();
        return (String) enumValueConverter.getJavaDescriptor().unwrap((CodeEnum) value, String.class, null);
    }

    @Override
    @SuppressWarnings("RedundantCast")
    public Object fromXMLString(String xmlValue) {
        verifyConfigured();
        return (T) enumValueConverter.getJavaDescriptor().wrap(xmlValue, null);
    }

    @Override
    public String toLoggableString(Object value, SessionFactoryImplementor factory) {
        verifyConfigured();
        return enumValueConverter.getJavaDescriptor().toString((CodeEnum) value);
    }

    public boolean isOrdinal() {
        verifyConfigured();
        return enumValueConverter instanceof OrdinalEnumValueConverter;
    }
}
