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
 * JPA {@link jakarta.persistence.EnumType#ORDINAL} strategy (storing the ordinal)
 *
 * @author Steve Ebersole
 */
public class OrdinalCodeEnumValueConverter<E extends CodeEnum> implements CodeEnumValueConverter<E, Number>, Serializable {

	private final CodeEnumJavaType<E> enumJavaType;
	private final JdbcType jdbcType;
	private final JavaType<Number> relationalJavaType;

	private transient ValueExtractor<Number> valueExtractor;
	private transient ValueBinder<Number> valueBinder;

	public OrdinalCodeEnumValueConverter(
			CodeEnumJavaType<E> enumJavaType,
			JdbcType jdbcType,
			JavaType<Number> relationalJavaType) {
		this.enumJavaType = enumJavaType;
		this.jdbcType = jdbcType;
		this.relationalJavaType = relationalJavaType;

		this.valueExtractor = jdbcType.getExtractor( relationalJavaType );
		this.valueBinder = jdbcType.getBinder( relationalJavaType );
	}

	@Override
	public E toDomainValue(Number relationalForm) {
		return enumJavaType.fromOrdinal( relationalForm == null ? null : relationalForm.intValue() );
	}

	@Override
	public Number toRelationalValue(E domainForm) {
		return enumJavaType.toOrdinal( domainForm );
	}

	@Override
	public int getJdbcTypeCode() {
		return jdbcType.getJdbcTypeCode();
	}

	@Override
	public CodeEnumJavaType<E> getDomainJavaType() {
		return enumJavaType;
	}

	@Override
	public JavaType<Number> getRelationalJavaType() {
		return relationalJavaType;
	}

	@Override
	public String toSqlLiteral(Object value) {
		//noinspection rawtypes
		return Integer.toString( ( (Enum) value ).ordinal() );
	}

	private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
		stream.defaultReadObject();

		this.valueExtractor = jdbcType.getExtractor( relationalJavaType );
		this.valueBinder = jdbcType.getBinder( relationalJavaType );
	}

	@Override
	public void writeValue(PreparedStatement statement, E value, int position, SharedSessionContractImplementor session)
			throws SQLException {
		valueBinder.bind( statement, toRelationalValue( value ), position, session );
	}
}
