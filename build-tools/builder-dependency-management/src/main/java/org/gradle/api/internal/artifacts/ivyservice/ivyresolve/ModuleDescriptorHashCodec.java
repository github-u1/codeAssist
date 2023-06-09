/*
 * Copyright 2019 the original author or authors.
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
package org.gradle.api.internal.artifacts.ivyservice.ivyresolve;

import org.gradle.internal.component.model.PersistentModuleSource;
import com.google.common.hash.HashCode;
import org.gradle.internal.serialize.Decoder;
import org.gradle.internal.serialize.Encoder;

import java.io.IOException;

public class ModuleDescriptorHashCodec implements PersistentModuleSource.Codec<ModuleDescriptorHashModuleSource> {
    @Override
    public void encode(ModuleDescriptorHashModuleSource moduleSource, Encoder encoder) throws IOException {
        encoder.writeBinary(moduleSource.getDescriptorHash().asBytes());
        encoder.writeBoolean(moduleSource.isChangingModule());
    }

    @Override
    public ModuleDescriptorHashModuleSource decode(Decoder decoder) throws IOException {
        return new ModuleDescriptorHashModuleSource(
            HashCode.fromBytes(decoder.readBinary()),
            decoder.readBoolean()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
