/*--------------------------------------------------------------------------*
 | Copyright (C) 2014 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.plugin.monthview.client.swing;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.Icon;

import org.rapla.RaplaResources;
import org.rapla.client.ReservationController;
import org.rapla.client.dialog.DialogUiFactoryInterface;
import org.rapla.client.extensionpoints.ObjectMenuFactory;
import org.rapla.client.internal.RaplaClipboard;
import org.rapla.client.swing.InfoFactory;
import org.rapla.client.swing.MenuFactory;
import org.rapla.client.swing.SwingCalendarView;
import org.rapla.client.swing.extensionpoints.SwingViewFactory;
import org.rapla.client.swing.images.RaplaImages;
import org.rapla.components.calendar.DateRenderer;
import org.rapla.components.iolayer.IOInterface;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.entities.domain.AppointmentFormater;
import org.rapla.facade.CalendarModel;
import org.rapla.facade.CalendarSelectionModel;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaInitializationException;
import org.rapla.framework.RaplaLocale;
import org.rapla.framework.logger.Logger;
import org.rapla.inject.Extension;
import org.rapla.plugin.monthview.MonthViewPlugin;

@Singleton
@Extension(provides = SwingViewFactory.class, id = MonthViewPlugin.MONTH_VIEW)
public class MonthViewFactory implements SwingViewFactory
{
    private final Set<ObjectMenuFactory> objectMenuFactories;
    private final MenuFactory menuFactory;
    private final Provider<DateRenderer> dateRendererProvider;
    private final CalendarSelectionModel calendarSelectionModel;
    private final RaplaClipboard clipboard;
    private final ReservationController reservationController;
    private final InfoFactory infoFactory;
    private final RaplaImages raplaImages;
    private final DateRenderer dateRenderer;
    private final DialogUiFactoryInterface dialogUiFactory;
    private final ClientFacade facade;
    private final RaplaResources i18n;
    private final RaplaLocale raplaLocale;
    private final Logger logger;
    private final IOInterface ioInterface;
    private final AppointmentFormater appointmentFormater;
    private RaplaConfiguration config;

    @Inject
    public MonthViewFactory(ClientFacade facade, RaplaResources i18n, RaplaLocale raplaLocale, Logger logger, Set<ObjectMenuFactory> objectMenuFactories,
            MenuFactory menuFactory, Provider<DateRenderer> dateRendererProvider, CalendarSelectionModel calendarSelectionModel, RaplaClipboard clipboard,
            ReservationController reservationController, InfoFactory infoFactory, RaplaImages raplaImages, DateRenderer dateRenderer,
            DialogUiFactoryInterface dialogUiFactory, IOInterface ioInterface, AppointmentFormater appointmentFormater) throws RaplaInitializationException
    {
        this.facade = facade;
        this.i18n = i18n;
        this.raplaLocale = raplaLocale;
        this.logger = logger;
        this.objectMenuFactories = objectMenuFactories;
        this.menuFactory = menuFactory;
        this.dateRendererProvider = dateRendererProvider;
        this.calendarSelectionModel = calendarSelectionModel;
        this.clipboard = clipboard;
        this.reservationController = reservationController;
        this.infoFactory = infoFactory;
        this.raplaImages = raplaImages;
        this.dateRenderer = dateRenderer;
        this.dialogUiFactory = dialogUiFactory;
        this.ioInterface = ioInterface;
        this.appointmentFormater = appointmentFormater;
        try
        {
            config = facade.getRaplaFacade().getSystemPreferences().getEntry(MonthViewPlugin.CONFIG, new RaplaConfiguration());
        }
        catch (RaplaException e)
        {
            throw new RaplaInitializationException(e.getMessage(), e);
        }
    }

    public SwingCalendarView createSwingView(CalendarModel model, boolean editable, boolean printing) throws RaplaException
    {
        return new SwingMonthCalendar(facade, i18n, raplaLocale, logger, model, editable, printing, objectMenuFactories, menuFactory, dateRendererProvider, calendarSelectionModel, clipboard,
                reservationController, infoFactory, raplaImages, dateRenderer, dialogUiFactory, ioInterface, appointmentFormater);
    }
    
    @Override
    public boolean isEnabled()
    {
        return config.getAttributeAsBoolean("enabled", true);
    }

    public String getViewId()
    {
        return MonthViewPlugin.MONTH_VIEW;
    }

    public String getName()
    {
        return i18n.getString(MonthViewPlugin.MONTH_VIEW);
    }

    Icon icon;

    public Icon getIcon()
    {
        if (icon == null)
        {
            icon = RaplaImages.getIcon("/org/rapla/plugin/monthview/images/month.png");
        }
        return icon;
    }

    public String getMenuSortKey()
    {
        return "C";
    }

}
