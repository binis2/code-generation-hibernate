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

import net.binis.codegen.objects.base.enumeration.CodeEnum;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * BasicValueConverter extension for enum-specific support
 *
 * @author Steve Ebersole
 */
public interface CodeEnumValueConverter<O extends CodeEnum, R> extends BasicValueConverter<O,R> {
	CodeEnumJavaTypeDescriptor<O> getJavaDescriptor();
	int getJdbcTypeCode();

	O readValue(ResultSet resultSet, String name, SharedSessionContractImplementor session) throws SQLException;
	void writeValue(PreparedStatement statement, O value, int position, SharedSessionContractImplementor session) throws SQLException;

	String toSqlLiteral(Object value);
}
