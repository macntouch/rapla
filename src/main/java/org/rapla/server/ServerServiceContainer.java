package org.rapla.server;

import org.rapla.framework.Disposable;
import org.rapla.framework.RaplaException;
import org.rapla.server.extensionpoints.ServletRequestPreprocessor;
import org.rapla.storage.StorageOperator;

import java.util.Collection;

public interface ServerServiceContainer extends Disposable
{
    Collection<ServletRequestPreprocessor> getServletRequestPreprocessors();

    StorageOperator getOperator();

    String getFirstAdmin() throws RaplaException;

}
