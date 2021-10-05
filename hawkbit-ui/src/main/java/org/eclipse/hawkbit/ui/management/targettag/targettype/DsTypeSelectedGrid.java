/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.targettype;

import com.vaadin.ui.Grid;
import com.vaadin.ui.themes.ValoTheme;
import org.eclipse.hawkbit.ui.common.builder.GridComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.grid.selection.RangeSelectionModel;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * Distribution Set Selected Type grid which is shown on the Target Type
 * Create/Update popup layout.
 */
public class DsTypeSelectedGrid extends Grid<ProxyType> {
    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;

    /**
     * Constructor for DsTypeSelectedGrid
     *
     * @param i18n
     *            VaadinMessageSource
     */
    public DsTypeSelectedGrid(final VaadinMessageSource i18n) {
        this.i18n = i18n;

        init();
    }

    private void init() {
        setSizeFull();
        setHeightUndefined();
        addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_SMALL);
        // used to deactivate cell text selection by user
        addStyleName(AbstractGrid.MULTI_SELECT_STYLE);

        setId(SPUIDefinitions.TWIN_TABLE_SELECTED_ID);
        setSelectionModel(new RangeSelectionModel<>(i18n));

        addColumns();
    }

    private void addColumns() {
        GridComponentBuilder.addColumn(this, ProxyType::getName).setId(UIComponentIdProvider.DIST_TYPE_TABLE_SELECTED_ID)
                .setCaption(i18n.getMessage("header.dt.twintable.selected"))
                .setDescriptionGenerator(ProxyType::getDescription);
    }
}
