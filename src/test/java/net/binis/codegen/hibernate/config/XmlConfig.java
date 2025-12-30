package net.binis.codegen.hibernate.config;

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

import net.binis.codegen.jackson.CodeBeanDeserializerModifier;
import net.binis.codegen.jackson.CodeProxyTypeFactory;
import net.binis.codegen.jackson.serialize.CodeEnumStringSerializer;
import org.springframework.boot.jackson.autoconfigure.XmlMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlWriteFeature;

@Configuration
public class XmlConfig {

    @Bean
    public XmlMapperBuilderCustomizer xmlCustomizer() {
        var module = new SimpleModule();
        module.setDeserializerModifier(new CodeBeanDeserializerModifier());
        module.addSerializer(new CodeEnumStringSerializer());
        return builder -> builder.typeFactory(new CodeProxyTypeFactory(builder.typeFactory())).addModule(module).configure(XmlWriteFeature.WRITE_NULLS_AS_XSI_NIL, false);
    }

    @Bean
    public XmlMapper xmlMapper(XmlMapperBuilderCustomizer customizer) {
        var builder = XmlMapper.builder();
        customizer.customize(builder);
        return builder.build();
    }

}
