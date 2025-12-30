package net.binis.codegen.hibernate;

/*-
 * #%L
 * code-generator-jackson
 * %%
 * Copyright (C) 2021 - 2026 Binis Belev
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

import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import lombok.extern.java.Log;
import net.binis.codegen.annotation.CodeConfiguration;
import net.binis.codegen.map.Mapper;
import net.binis.codegen.map.MappingStrategy;
import net.binis.codegen.map.executor.MapperExecutor;
import net.binis.codegen.tools.Reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Log
@CodeConfiguration
public class CodeHibernate {

    @SuppressWarnings("unchecked")
    public static void initialize() {
        Mapper.registerMapper(Tuple.class, Object.class, (source, destination) -> {
            var executor = new TupleMapperExecutor(source, destination, false, false, MappingStrategy.GETTERS_SETTERS, null);
            return executor.map(source, destination);
        });

    }

    protected static class TupleMapperExecutor<T> extends MapperExecutor<T> {

        private final Tuple tuple;

        public TupleMapperExecutor(Object source, T destination, boolean convert, boolean producer, MappingStrategy strategy, Object key) {
            super(source, destination, convert, producer, strategy, key);
            tuple = (Tuple) source;
            build();
        }

        protected void build() {
            if (nonNull(tuple)) {
                var accessors = new LinkedHashMap<String, TriFunction>();

                buildTupleMatcher(accessors);
                buildMapper(accessors);
            }
        }

        private void buildTupleMatcher(Map<String, TriFunction> accessors) {
            if (net.binis.codegen.modifier.Modifier.class.isAssignableFrom(destination)) {
                matchTupleModifier(accessors, destination);
            } else {
                matchTupleWithers(accessors, destination);
                matchTupleSetters(accessors, destination);
            }

        }

        @SuppressWarnings("unchecked")
        private void matchTupleSetters(Map<String, TriFunction> accessors, Class<T> destination) {
            var getters = tuple.getElements().stream()
                    .collect(Collectors.toMap(TupleElement::getAlias, v -> (Supplier) () -> tuple.get(v.getAlias())));
            var setters = Arrays.stream(destination.getMethods())
                    .filter(Reflection::isSetter)
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .filter(this::shouldNotSkip)
                    .collect(Collectors.toMap(k -> getFieldName(k.getName()), v -> v, (n1, n2) -> n1));

            if (!setters.isEmpty()) {
                for (var entry : getters.entrySet()) {
                    if (!accessors.containsKey(entry.getKey())) {
                        var setter = setters.get(entry.getKey());
                        var getter = entry.getValue();
                        if (nonNull(setter)) {
                            var name = entry.getKey();
                            var destType = setter.getParameterTypes()[0];
                            accessors.put(name, (s, d, w) -> {
                                try {
                                    var value = getter.get();

                                    if (nonNull(value)) {
                                        if (destType.isAssignableFrom(value.getClass())) {
                                            setter.invoke(d, value);
                                        } else {
                                            setter.invoke(d, Mapper.convert(value, destType));
                                        }
                                    }
                                } catch (Exception e) {
                                    //Do nothing
                                }
                                return d;
                            });
                        }
                    }
                }
            }
            if (!Object.class.equals(destination.getSuperclass())) {
                matchTupleSetters(accessors, (Class) destination.getSuperclass());
            }
        }

        private void matchTupleWithers(Map<String, TriFunction> accessors, Class<T> destination) {
            try {
                var wither = destination.getDeclaredMethod("with");
                wither.setAccessible(true);
                var witherAdded = false;

                var getters = tuple.getElements().stream()
                        .collect(Collectors.toMap(TupleElement::getAlias, v -> (Supplier) () -> tuple.get(v.getAlias())));
                var withers = Arrays.stream(wither.getReturnType().getMethods())
                        .filter(m -> Modifier.isPublic(m.getModifiers()))
                        .filter(m -> m.getParameterCount() == 1)
                        .filter(this::shouldNotSkip)
                        .collect(Collectors.toMap(Method::getName, v -> v));

                if (!withers.isEmpty()) {
                    for (var entry : getters.entrySet()) {
                        if (!accessors.containsKey(entry.getKey())) {
                            var setter = withers.get(entry.getKey());
                            var getter = entry.getValue();
                            if (nonNull(setter)) {
                                var name = entry.getKey();

                                var destType = setter.getParameterTypes()[0];
                                if (!witherAdded) {
                                    addWither(accessors, wither);
                                    witherAdded = true;
                                }
                                accessors.put(name, (s, d, w) -> {
                                    try {
                                        var value = getter.get();

                                        if (nonNull(value)) {
                                            if (destType.isAssignableFrom(value.getClass())) {
                                                setter.invoke(w, value);
                                            } else {
                                                setter.invoke(w, Mapper.convert(value, destType));
                                            }
                                        }
                                    } catch (Exception e) {
                                        //Do nothing
                                    }
                                    return d;
                                });
                            }
                        }
                    }
                }

                if (!Object.class.equals(destination.getSuperclass())) {
                    matchGettersWithers(accessors, source, destination.getSuperclass());
                }
            } catch (Exception e) {
                //Do nothing
            }
        }

        private void matchTupleModifier(Map<String, TriFunction> accessors, Class<T> destination) {
            var getters = tuple.getElements().stream()
                    .collect(Collectors.toMap(TupleElement::getAlias, v -> (Supplier) () -> tuple.get(v.getAlias())));
            var withers = Arrays.stream(destination.getMethods())
                    .filter(m -> m.getParameterCount() == 1)
                    .filter(m -> m.getReturnType().isInterface())
                    .filter(m -> m.getReturnType().isAssignableFrom(destination))
                    .filter(this::shouldNotSkip)
                    .collect(Collectors.toMap(Method::getName, v -> v));

            if (!withers.isEmpty()) {
                for (var entry : getters.entrySet()) {
                    if (!accessors.containsKey(entry.getKey())) {
                        var setter = withers.get(entry.getKey());
                        var getter = entry.getValue();
                        if (nonNull(setter)) {
                            var name = entry.getKey();
                            var destType = setter.getParameterTypes()[0];
                            accessors.put(name, (s, d, w) -> {
                                try {
                                    var value = getter.get();

                                    if (nonNull(value)) {
                                        if (destType.isAssignableFrom(value.getClass())) {
                                            setter.invoke(d, value);
                                        } else {
                                            setter.invoke(d, Mapper.convert(value, destType));
                                        }
                                    }
                                } catch (Exception e) {
                                    //Do nothing
                                }
                                return d;
                            });
                        }
                    }
                }
            }
            if (!Object.class.equals(destination.getSuperclass())) {
                matchGettersModifier(accessors, source, destination.getSuperclass());
            }
        }
    }

    private CodeHibernate() {
        //Do nothing
    }

}
