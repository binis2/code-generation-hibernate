package net.binis.codegen.hibernate;

/*-
 * #%L
 * code-generator-hibernate
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

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.annotation.CodePrototype;
import net.binis.codegen.annotation.builder.CodeBuilder;
import net.binis.codegen.hibernate.objects.TestEnum;
import net.binis.codegen.hibernate.objects.TestEnumPrototype;
import net.binis.codegen.hibernate.objects.TestEnums;
import net.binis.codegen.hibernate.objects.TestMixEnum;
import net.binis.codegen.map.Mapper;
import net.binis.codegen.jackson.mapping.keys.MappingKeys;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.AutoConfigureDataJpa;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@DataJpaTest
@AutoConfigureDataJpa
@ContextConfiguration(initializers = {HibernateIntegrationTest.Initializer.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class HibernateIntegrationTest {

    @ClassRule
    public static PostgreSQLContainer<?> postgreSQLContainer = prepareDatabase();

    public static PostgreSQLContainer<?> prepareDatabase(){
        var result = new PostgreSQLContainer<>("postgres:17")
                .withDatabaseName("integration-tests-db")
                .withUsername("sa")
                .withPassword("sa")
                .withReuse(true)
                .withExposedPorts(5432)
                .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                        new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(10000), new ExposedPort(5432)))
                ));
        result.start();
        return result;
    }

    @AfterAll
    public static void tearDown() {
        try {
            postgreSQLContainer.stop();
        } finally {
            postgreSQLContainer.close();
        }
    }

    @Test
    void test() {
        TestEnums.create()
                .testEnum(TestEnum.ONE)
                .testMixEnum(TestMixEnum.FOUR)
                .testList(List.of(TestEnum.TWO))
                .testMixList(List.of(TestMixEnum.FOUR)).save();
        var load = TestEnums.find().by().testEnum(TestEnum.ONE).and().testList().contains(TestEnum.TWO).get().orElse(null);

        assertNotNull(load);
        assertEquals(TestEnum.ONE, load.getTestEnum());
        assertEquals(TestMixEnum.FOUR, load.getTestMixEnum());
        assertTrue(load.getTestList().contains(TestEnum.TWO));
        assertTrue(load.getTestMixList().contains(TestMixEnum.FOUR));
    }

    @Test
    void testCollections() {
        TestEnums.create()
                .testList(List.of(TestEnum.TWO, TestEnum.THREE))
                .save();

        var load = TestEnums.find().by().testList().containsOne(List.of(TestEnum.TWO)).get().orElse(null);

        assertNotNull(load);
    }

    @Test
    void testSerialization() {
        var obj = TestEnums.create()
                .done();

        var json = Mapper.convert(obj, String.class, MappingKeys.JSON);
        assertEquals("{\"id\":null,\"testEnum\":null,\"testEnumNumber\":null,\"testList\":null,\"testMixEnum\":null,\"testMixList\":null}", json);

        var xml = Mapper.convert(obj, String.class, MappingKeys.XML);
        assertEquals("<TestEnumsEntity><id/><testEnum/><testEnumNumber/><testMixEnum/></TestEnumsEntity>", xml);

    }

    @Test
    void testQueries() {
        var obj = TestEnums.create()
                .testEnum(TestEnum.ONE)
                .testMixEnum(TestMixEnum.FOUR)
                .testList(List.of(TestEnum.TWO))
                .testMixList(List.of(TestMixEnum.FOUR)).save();

        var refs = TestEnums.find().by().references();
        assertEquals(UUID.class, refs.get(0).getId().getClass());

        var ref = TestEnums.find().by().id(obj.getId()).reference();
        assertEquals(UUID.class, ref.get().getId().getClass());
        var ids = TestEnums.find().select().id().tuples(UUID.class);
        assertEquals(UUID.class, ids.get(0).getClass());

        var plain = TestEnums.find().select().id().tuples(TestMapDestinationPlain.class);
        assertEquals(obj.getId(), plain.get(0).getId());

        var modif = TestEnums.find().select().id().testEnum().tuples(TestMapDestinationImpl.class);
        assertEquals(obj.getId(), modif.get(0).getId());
        assertEquals(TestEnum.ONE, modif.get(0).getTestEnum());
    }

    @Test
    void testEnumAsString() {
        var obj = TestEnums.create()
                .testEnum(TestEnum.ONE)
                .testEnumNumber(TestEnum.TWO)
                .testMixEnum(TestMixEnum.FOUR)
                .testList(List.of(TestEnum.TWO))
                .testMixList(List.of(TestMixEnum.FOUR)).save();

        var values = TestEnums.find().nativeQuery("select testEnum, testMixEnum, testEnumNumber from testenumsentity where id = ?1").param(obj.getId()).tuple().get();
        assertEquals(String.class, values.get(0).getClass());
        assertEquals(String.class, values.get(1).getClass());
        assertEquals(Short.class, values.get(2).getClass());

    }

    @Data
    public static class TestMapDestinationPlain {

        private UUID id;

    }

    @Test
    void testTuples() {
        TestEnums.create()
                .testList(List.of(TestEnum.TWO, TestEnum.THREE))
                .save();

        var load = TestEnums.find().select().id().list();

        assertEquals(1, load.size());
    }


    @CodePrototype(base = true)
    public interface TestMapBaseDestinationPrototype {
        UUID id();
    }

    @CodeBuilder
    public interface TestMapDestinationPrototype extends TestMapBaseDestinationPrototype{
        TestEnumPrototype testEnum();
    }

    static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                    "spring.datasource.password=" + postgreSQLContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

}
