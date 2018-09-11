/*
 * Copyright (c) 2018 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.buildship.core;

/**
 * Represents a Gradle distribution.
 * <p>
 * Currently four different Gradle distribution types are supported.
 * <ul>
 * <li>{@link WrapperGradleDistribution}</li>
 * <li>{@link LocalGradleDistribution}</li>
 * <li>{@link RemoteGradleDistribution}</li>
 * <li>{@link FixedVersionGradleDistribution}</li>
 * </ul>
 * New instances can be created with the factory methods in {@link GradleDistributions}.
 *
 * @author Donat Csikos
 * @since 3.0
 * @noimplement this interface is not intended to be implemented by clients
 */
public interface GradleDistribution {

}
