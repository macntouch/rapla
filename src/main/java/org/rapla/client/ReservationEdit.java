package org.rapla.client;

import org.rapla.components.util.undo.CommandHistory;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.ModificationEvent;
import org.rapla.framework.RaplaException;

import java.util.Collection;
import java.util.Date;

public interface ReservationEdit<T> extends RaplaWidget<T>
{
    void addAppointment( Date start, Date end) throws RaplaException;
	
    Reservation getReservation();

    void addAppointmentListener(AppointmentListener listener);
    void removeAppointmentListener(AppointmentListener listener);
   
    Collection<Appointment> getSelectedAppointments();

    void editReservation(Reservation reservation, Reservation original,AppointmentBlock appointmentBlock, Runnable saveCmd, Runnable closeCmd, Runnable deleteCmd) throws RaplaException;

    Reservation getOriginal();

    boolean hasChanged();

    void setReservation(Reservation reservation, Appointment appointment) throws RaplaException;
    //void updateReservation(Reservation persistant) throws RaplaException;

    //void deleteReservation() throws RaplaException;

    CommandHistory getCommandHistory();

    void updateView(ModificationEvent evt);

    void fireChange();

    boolean isNew();

    void setHasChanged(boolean b);
}