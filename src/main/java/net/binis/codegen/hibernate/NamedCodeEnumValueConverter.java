/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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

import net.binis.codegen.hibernate.CodeEnumJavaTypeDescriptor;
import net.binis.codegen.hibernate.CodeEnumValueConverter;
import net.binis.codegen.objects.base.enumeration.CodeEnum;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

public class NamedCodeEnumValueConverter<E extends CodeEnum> implements CodeEnumValueConverter<E,String>, Serializable {

	private final CodeEnumJavaTypeDescriptor<E> enumJavaDescriptor;

	private transient ValueExtractor<E> valueExtractor;

	private transient ValueBinder<String> valueBinder;

	public NamedCodeEnumValueConverter(CodeEnumJavaTypeDescriptor<E> enumJavaDescriptor) {
		this.enumJavaDescriptor = enumJavaDescriptor;
		this.valueExtractor = createValueExtractor( enumJavaDescriptor );
		this.valueBinder = createValueBinder();
	}

	@Override
	public E toDomainValue(String relationalForm) {
		return enumJavaDescriptor.fromName( relationalForm );
	}

	@Override
	public String toRelationalValue(E domainForm) {
		return enumJavaDescriptor.toName( domainForm );
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.VARCHAR;
	}

	@Override
	public CodeEnumJavaTypeDescriptor<E> getJavaDescriptor() {
		return enumJavaDescriptor;
	}

	@Override
	public E readValue(ResultSet resultSet, String name, SharedSessionContractImplementor session) throws SQLException {
		return valueExtractor.extract( resultSet, name, session );
	}

	@Override
	public void writeValue(PreparedStatement statement, E value, int position, SharedSessionContractImplementor session) throws SQLException {
		final String jdbcValue = value == null ? null : toRelationalValue( value );

		valueBinder.bind( statement, jdbcValue, position, session );
	}

	@Override
	@SuppressWarnings("unchecked")
	public String toSqlLiteral(Object value) {
		return String.format( Locale.ROOT, "'%s'", ( (E) value ).name() );
	}

	private static <T extends CodeEnum> ValueExtractor<T> createValueExtractor(CodeEnumJavaTypeDescriptor<T> enumJavaDescriptor) {
		return VarcharTypeDescriptor.INSTANCE.getExtractor( enumJavaDescriptor );
	}

	private static ValueBinder<String> createValueBinder() {
		return VarcharTypeDescriptor.INSTANCE.getBinder( StringTypeDescriptor.INSTANCE );
	}

	private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
		stream.defaultReadObject();

		this.valueExtractor = createValueExtractor( enumJavaDescriptor );
		this.valueBinder = createValueBinder();
	}
}
