package org.rapla.plugin.eventimport;

import org.rapla.framework.RaplaException;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path( "templateimport" )
public interface TemplateImport
{
    public static final String BEGIN_KEY = "DatumVon";
    public static final String STORNO_KEY = "StorniertAm";
    public static final String PRIMARY_KEY = "Seminarnummer";
    public static final String TEMPLATE_KEY = "TitelName";

    @POST
    @Path("importFromServer")
    ParsedTemplateResult importFromServer() throws RaplaException;
}
