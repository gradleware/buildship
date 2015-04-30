/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Simon Scholz (vogella GmbH) - initial API and implementation and initial documentation
 */

package org.eclipse.buildship.core.event;


/**
 * This is supposed to be the common interface for events, which are propagated by the
 * {@link org.eclipse.buildship.core.CorePlugin#eventBroker()}.
 *
 * @param <T> is the type of the element, which is passed within the event
 *
 * @see BuildLaunchRequestEvent
 * @see org.eclipse.buildship.core.event.internal.DefaultGradleEvent
 * @see org.eclipse.buildship.core.event.internal.DefaultBuildLaunchRequestEvent
 */
public interface GradleEvent<T> {

    public Object getSource();

    public T getElement();
}
