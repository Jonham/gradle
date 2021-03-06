/*
 * Copyright 2014 the original author or authors.
 *
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
 */

package org.gradle.platform.base.internal.registry;

import org.gradle.language.base.FunctionalSourceSet;
import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.model.InvalidModelRuleDeclarationException;
import org.gradle.model.collection.CollectionBuilder;
import org.gradle.model.internal.core.DefaultCollectionBuilder;
import org.gradle.model.internal.core.NamedEntityInstantiator;
import org.gradle.model.internal.core.*;
import org.gradle.model.internal.core.rule.describe.SimpleModelRuleDescriptor;
import org.gradle.model.internal.inspect.MethodRuleDefinition;
import org.gradle.model.internal.inspect.RuleSourceDependencies;
import org.gradle.model.internal.registry.ModelRegistry;
import org.gradle.model.internal.type.ModelType;
import org.gradle.platform.base.*;
import org.gradle.platform.base.internal.BinarySpecInternal;
import org.gradle.platform.base.internal.ComponentSpecInternal;

public class ComponentBinariesRuleDefinitionHandler extends AbstractAnnotationDrivenMethodComponentRuleDefinitionHandler<ComponentBinaries> {

    public <R> void register(final MethodRuleDefinition<R> ruleDefinition, final ModelRegistry modelRegistry, RuleSourceDependencies dependencies) {
        doRegister(ruleDefinition, modelRegistry, dependencies);
    }

    private <R, S extends BinarySpec> void doRegister(MethodRuleDefinition<R> ruleDefinition, ModelRegistry modelRegistry, RuleSourceDependencies dependencies) {
        try {
            RuleMethodDataCollector dataCollector = new RuleMethodDataCollector();
            visitAndVerifyMethodSignature(dataCollector, ruleDefinition);

            final Class<S> binaryType = dataCollector.getParameterType(BinarySpec.class);
            final Class<? extends ComponentSpec> componentType = dataCollector.getParameterType(ComponentSpec.class);
            dependencies.add(ComponentModelBasePlugin.class);
            final ModelReference<BinaryContainer> subject = ModelReference.of(ModelPath.path("binaries"), new ModelType<BinaryContainer>() {
            });

            configureMutationRule(modelRegistry, subject, componentType, binaryType, ruleDefinition);
        } catch (InvalidModelException e) {
            invalidModelRule(ruleDefinition, e);
        }
    }

    private <S extends BinarySpec, R> void configureMutationRule(ModelRegistry modelRegistry, ModelReference<BinaryContainer> subject, Class<? extends ComponentSpec> componentType, Class<S> binaryType, MethodRuleDefinition<R> ruleDefinition) {
        modelRegistry.mutate(new ComponentBinariesRule<R, S>(subject, componentType, binaryType, ruleDefinition));
    }

    private <R> void visitAndVerifyMethodSignature(RuleMethodDataCollector dataCollector, MethodRuleDefinition<R> ruleDefinition) {
        assertIsVoidMethod(ruleDefinition);
        visitCollectionBuilderSubject(dataCollector, ruleDefinition, BinarySpec.class);
        visitDependency(dataCollector, ruleDefinition, ModelType.of(ComponentSpec.class));
    }

    private class ComponentBinariesRule<R, S extends BinarySpec> extends CollectionBuilderBasedRule<R, S, ComponentSpec, BinaryContainer> {

        private final Class<? extends ComponentSpec> componentType;
        private final Class<S> binaryType;

        public ComponentBinariesRule(ModelReference<BinaryContainer> subject, final Class<? extends ComponentSpec> componentType, final Class<S> binaryType, MethodRuleDefinition<R> ruleDefinition) {
            super(subject, componentType, ruleDefinition, ModelReference.of(ComponentSpecContainer.class));
            this.componentType = componentType;
            this.binaryType = binaryType;
        }

        public void mutate(ModelNode modelNode, final BinaryContainer binaries, final Inputs inputs) {
            ComponentSpecContainer componentSpecs = inputs.get(0, ModelType.of(ComponentSpecContainer.class)).getInstance();

            for (final ComponentSpec componentSpec : componentSpecs.withType(componentType)) {
                NamedEntityInstantiator<S> namedEntityInstantiator = new Instantiator<S>(binaryType, componentSpec, binaries);
                CollectionBuilder<S> collectionBuilder = new DefaultCollectionBuilder<S>(
                        namedEntityInstantiator,
                        new SimpleModelRuleDescriptor("Project.<init>.binaries()"),
                        modelNode);
                invoke(inputs, collectionBuilder, componentSpec, componentSpecs);
            }
        }
    }

    protected <R> void invalidModelRule(MethodRuleDefinition<R> ruleDefinition, InvalidModelException e) {
        StringBuilder sb = new StringBuilder();
        ruleDefinition.getDescriptor().describeTo(sb);
        sb.append(" is not a valid ComponentBinaries model rule method.");
        throw new InvalidModelRuleDeclarationException(sb.toString(), e);
    }

    private class Instantiator<S extends BinarySpec> implements NamedEntityInstantiator<S> {
        private final Class<S> binaryType;
        private final ComponentSpec componentSpec;
        private final BinaryContainer container;

        public Instantiator(Class<S> binaryType, ComponentSpec componentSpec, BinaryContainer container) {
            this.binaryType = binaryType;
            this.componentSpec = componentSpec;
            this.container = container;
        }

        public ModelType<S> getType() {
            return ModelType.of(binaryType);
        }

        public S create(String name) {
            S binary = container.create(name, binaryType);
            bindBinaryToComponent(componentSpec, binary, name);
            return binary;
        }

        public <U extends S> U create(String name, Class<U> type) {
            U binary = container.create(name, type);
            bindBinaryToComponent(componentSpec, binary, name);
            return binary;
        }

        private <U extends S> void bindBinaryToComponent(ComponentSpec componentSpec, U binary, String name) {
            componentSpec.getBinaries().add(binary);
            BinarySpecInternal binaryInternal = (BinarySpecInternal) binary;
            FunctionalSourceSet binarySourceSet = ((ComponentSpecInternal) componentSpec).getSources().copy(name);
            binaryInternal.setBinarySources(binarySourceSet);
        }
    }
}
