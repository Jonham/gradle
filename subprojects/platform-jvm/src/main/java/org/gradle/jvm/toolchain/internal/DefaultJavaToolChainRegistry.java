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

package org.gradle.jvm.toolchain.internal;

import org.gradle.jvm.internal.toolchain.JavaToolChainInternal;
import org.gradle.jvm.platform.JvmPlatform;
import org.gradle.jvm.toolchain.JavaToolChain;
import org.gradle.jvm.toolchain.JavaToolChainRegistry;

public class DefaultJavaToolChainRegistry implements JavaToolChainRegistry {
    private final JavaToolChainInternal toolChain;

    public DefaultJavaToolChainRegistry(JavaToolChainInternal toolChain) {
        this.toolChain = toolChain;
    }

    public JavaToolChain getForPlatform(JvmPlatform targetPlatform) {
        return toolChain;
    }
}