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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

import net.binis.codegen.objects.base.enumeration.CodeEnum;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link jakarta.persistence.EnumType#STRING} strategy (storing the name)
 *
 * @author Steve Ebersole
 */
public class NamedCodeEnumValueConverter<E extends CodeEnum> implements CodeEnumValueConverter<E, String>, Serializable {
    private final CodeEnumJavaType<E> domainTypeDescriptor;
    private final JdbcType jdbcType;
    private final JavaType<String> relationalTypeDescriptor;

    private transient ValueExtractor<String> valueExtractor;
    private transient ValueBinder<String> valueBinder;

    public NamedCodeEnumValueConverter(
            CodeEnumJavaType<E> domainTypeDescriptor,
            JdbcType jdbcType,
            JavaType<String> relationalTypeDescriptor) {
        this.domainTypeDescriptor = domainTypeDescriptor;
        this.jdbcType = jdbcType;
        this.relationalTypeDescriptor = relationalTypeDescriptor;

        this.valueExtractor = jdbcType.getExtractor(relationalTypeDescriptor);
        this.valueBinder = jdbcType.getBinder(relationalTypeDescriptor);
    }

    @Override
    public CodeEnumJavaType<E> getDomainJavaType() {
        return domainTypeDescriptor;
    }

    @Override
    public JavaType<String> getRelationalJavaType() {
        return relationalTypeDescriptor;
    }

    @Override
    public E toDomainValue(String relationalForm) {
        return domainTypeDescriptor.fromName(relationalForm);
    }

    @Override
    public String toRelationalValue(E domainForm) {
        return domainTypeDescriptor.toName(domainForm);
    }

    @Override
    public int getJdbcTypeCode() {
        return jdbcType.getJdbcTypeCode();
    }

    public int getDefaultSqlTypeCode() {
        return jdbcType.getDefaultSqlTypeCode();
    }

    @Override
    public String toSqlLiteral(Object value) {
        //noinspection rawtypes
        return String.format(Locale.ROOT, "'%s'", ((CodeEnum) value).name());
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();

        this.valueExtractor = jdbcType.getExtractor(relationalTypeDescriptor);
        this.valueBinder = jdbcType.getBinder(relationalTypeDescriptor);
    }

    @Override
    public void writeValue(
            PreparedStatement statement,
            E value,
            int position,
            SharedSessionContractImplementor session) throws SQLException {
        final String jdbcValue = value == null ? null : value.name();
        valueBinder.bind(statement, jdbcValue, position, session);
    }
}
