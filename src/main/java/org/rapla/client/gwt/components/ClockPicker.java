package org.rapla.client.gwt.components;

import java.util.Date;

import org.gwtbootstrap3.client.ui.html.Div;
import org.rapla.client.gwt.components.util.JQueryElement;
import org.rapla.client.gwt.components.util.JS;
import org.rapla.components.i18n.BundleManager;
import org.rapla.components.i18n.I18nLocaleFormats;

import com.google.gwt.core.client.js.JsFunction;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.TextBox;

public class ClockPicker extends Div
{
    public interface TimeChangeListener
    {
        void timeChanged(final Date newDate);
    }

    @JsType(prototype = "jQuery")
    public interface ClockPickerJquery extends JQueryElement
    {
        ClockPickerElement clockpicker(ClockPickerOptions options);
        void remove();
    }

    @JsType(prototype = "jQuery")
    public interface ClockPickerElement extends JQueryElement
    {
        /*
         * clockpicker is the key
         */
        ClockPickerI data(String key);
    }

    @JsType
    public interface ClockPickerI extends JQueryElement
    {

        void clockpicker(String action);
    }
    
    @JsFunction
    public interface Callback{
        void handleAction();
    }

    @JsType
    public interface ClockPickerOptions
    {
        @JsProperty
        void setAutoclose(Boolean autoclose);

        @JsProperty
        Boolean getAutoclose();

        @JsProperty
        void setTwelvehour(Boolean twelvehour);

        @JsProperty
        Boolean getTwelvehour();

        @JsProperty
        void setAfterDone(Callback listener);

        @JsProperty
        Callback getAfterDone();
    }

    private ClockPickerI clockPicker;
    private final TimeChangeListener changeListener;
    private final DateTimeFormat format;
    private final TextBox input = new TextBox();
    private final boolean amPmFormat;

    public ClockPicker(final Date initDate, final TimeChangeListener changeListener, final BundleManager bundleManager)
    {
        this.changeListener = changeListener;
        amPmFormat = bundleManager.getFormats().isAmPmFormat();
        setStyleName("raplaClockPicker input-group clockpicker");
        final I18nLocaleFormats formats = bundleManager.getFormats();
        final String formatHour = formats.getFormatHour();
        format = DateTimeFormat.getFormat(formatHour);
        input.setStyleName("form-control");
        input.addChangeHandler(new ChangeHandler()
        {
            @Override
            public void onChange(ChangeEvent event)
            {
                timeChanged();
            }
        });
        setTime(initDate);
        add(input);
        input.addFocusHandler(new FocusHandler()
        {
            @Override
            public void onFocus(FocusEvent event)
            {
                clockPicker.clockpicker("show");
            }
        });
        final Element span = DOM.createSpan();
        span.setClassName("input-group-addon");
        getElement().appendChild(span);
        final Element innerSpan = DOM.createSpan();
        innerSpan.setClassName("glyphicon glyphicon-time");
        span.appendChild(innerSpan);
        addDomHandler(new ClickHandler()
        {
            @Override
            public void onClick(ClickEvent event)
            {
                event.stopPropagation();
                clockPicker.clockpicker("show");
            }
        }, ClickEvent.getType());
    }

    public void setTime(final Date time)
    {
        input.setValue(format.format(time));
    }

    @Override
    protected void onAttach()
    {
        super.onAttach();
        ClockPickerJquery jqe = (ClockPickerJquery) JQueryElement.Static.$(input.getElement());
        ClockPickerOptions options = JS.createObject();
        options.setAutoclose(true);
        options.setTwelvehour(amPmFormat);
        options.setAfterDone(new Callback()
        {
            @Override
            public void handleAction()
            {
                timeChanged();
            }
        });
        ClockPickerElement clockPickerElement = jqe.clockpicker(options);
        clockPicker = clockPickerElement.data("clockpicker");
    }

    private void timeChanged()
    {
        final String value = input.getValue();
        final Date time = format.parse(value);
        changeListener.timeChanged(time);
    }

    public Date getTime()
    {
        final String value = input.getValue();
        final Date time = format.parse(value);
        return time;
    }

}