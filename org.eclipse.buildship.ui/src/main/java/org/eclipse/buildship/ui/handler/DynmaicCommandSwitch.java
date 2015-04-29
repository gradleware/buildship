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

package org.eclipse.buildship.ui.handler;

import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

import org.eclipse.buildship.ui.view.executionview.AbstractPagePart;
import org.eclipse.buildship.ui.view.pages.IPage;

public class DynmaicCommandSwitch extends CompoundContributionItem {

    @Override
    protected IContributionItem[] getContributionItems() {

        IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
        if (activePart instanceof AbstractPagePart) {
            List<IContributionItem> items = Lists.newArrayList();

            List<IPage> pages = ((AbstractPagePart) activePart).getPages();

            for (int i = 0; i < pages.size(); i++) {
                IPage page = pages.get(i);
                CommandContributionItemParameter contributionItemParameter = new CommandContributionItemParameter(PlatformUI.getWorkbench(), "",
                        SwitchPageHandler.SWITCH_PAGE_COMMAND_ID, SWT.PUSH);
                contributionItemParameter.label = page.getDisplayName();

                HashMap<String, String> commandParameters = Maps.newHashMap();
                commandParameters.put(SwitchPageHandler.PAGE_ID_PARAM, String.valueOf(i));
                contributionItemParameter.parameters = commandParameters;

                CommandContributionItem commandContributionItem = new CommandContributionItem(contributionItemParameter);
                commandContributionItem.setVisible(true);

                items.add(commandContributionItem);
            }
            return items.toArray(new IContributionItem[items.size()]);
        }

        return new IContributionItem[0];
    }

}
